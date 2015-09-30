/*******************************************************************************
 * Copyright (c) 2015, Eclipse Foundation, Inc. and its licensors.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * - Neither the name of the Eclipse Foundation, Inc. nor the names of its
 *   contributors may be used to endorse or promote products derived from this
 *   software without specific prior written permission. 
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/
package org.openrdf.sail.base;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openrdf.IsolationLevels;
import org.openrdf.model.Model;
import org.openrdf.model.ModelFactory;
import org.openrdf.model.Resource;
import org.openrdf.model.IRI;
import org.openrdf.model.Value;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.Var;
import org.openrdf.sail.SailConflictException;
import org.openrdf.sail.SailException;

/**
 * Set of changes applied to an {@link SailSourceBranch} awaiting to be flushed
 * into its backing {@link SailSource}.
 * 
 * @author James Leigh
 */
abstract class Changeset implements SailSink, ModelFactory {

	/**
	 * Set of {@link SailDataset}s that are currently using this {@link Changeset}
	 * to derive the state of the {@link SailSource}.
	 */
	private Set<SailDatasetImpl> refbacks;

	/**
	 * {@link Changeset}s that have been {@link #flush()}ed to the same
	 * {@link SailSourceBranch}, since this object was {@link #flush()}ed.
	 */
	private Set<Changeset> prepend;

	/**
	 * When in {@link IsolationLevels#SERIALIZABLE} this contains all the
	 * observed {@link StatementPattern}s that were observed by
	 * {@link ObservingSailDataset}.
	 */
	private Set<StatementPattern> observations;

	/**
	 * Statements that have been added as part of a transaction, but has not yet
	 * been committed.
	 */
	private Model approved;

	/**
	 * Explicit statements that have been removed as part of a transaction, but
	 * have not yet been committed.
	 */
	private Model deprecated;

	/**
	 * Set of contexts of the {@link #approved} statements.
	 */
	private Set<Resource> approvedContexts;

	/**
	 * Set of contexts that were passed to {@link #clear(Resource...)}.
	 */
	private Set<Resource> deprecatedContexts;

	/**
	 * Additional namespaces added.
	 */
	private Map<String, String> addedNamespaces;

	/**
	 * Namespace prefixes that were removed.
	 */
	private Set<String> removedPrefixes;

	/**
	 * If all namespaces were removed, other than {@link #addedNamespaces}.
	 */
	private boolean namespaceCleared;

	/**
	 * If all statements were removed, other than {@link #approved}.
	 */
	private boolean statementCleared;

	@Override
	public void close()
		throws SailException
	{
		// no-op
	}

	@Override
	public void prepare()
		throws SailException
	{
		if (prepend != null && observations != null) {
			for (StatementPattern p : observations) {
				Resource subj = (Resource)p.getSubjectVar().getValue();
				IRI pred = (IRI)p.getPredicateVar().getValue();
				Value obj = p.getObjectVar().getValue();
				Var ctxVar = p.getContextVar();
				Resource[] contexts;
				if (ctxVar == null) {
					contexts = new Resource[0];
				}
				else {
					contexts = new Resource[] { (Resource)ctxVar.getValue() };
				}
				for (Changeset changeset : prepend) {
					Model approved = changeset.getApproved();
					Model deprecated = changeset.getDeprecated();
					if (approved != null && approved.contains(subj, pred, obj, contexts) || deprecated != null
							&& deprecated.contains(subj, pred, obj, contexts))
					{
						throw new SailConflictException("Observed State has Changed");
					}
				}
			}
		}
	}

	public synchronized void addRefback(SailDatasetImpl dataset) {
		if (refbacks == null) {
			refbacks = new HashSet<SailDatasetImpl>();
		}
		refbacks.add(dataset);
	}

	public synchronized void removeRefback(SailDatasetImpl dataset) {
		if (refbacks != null) {
			refbacks.remove(dataset);
		}
	}

	public synchronized boolean isRefback() {
		return refbacks != null && !refbacks.isEmpty();
	}

	public synchronized void prepend(Changeset changeset) {
		if (prepend == null) {
			prepend = new HashSet<Changeset>();
		}
		prepend.add(changeset);
	}

	@Override
	public synchronized void setNamespace(String prefix, String name) {
		if (removedPrefixes == null) {
			removedPrefixes = new HashSet<String>();
		}
		removedPrefixes.add(prefix);
		if (addedNamespaces == null) {
			addedNamespaces = new HashMap<String, String>();
		}
		addedNamespaces.put(prefix, name);
	}

	@Override
	public synchronized void removeNamespace(String prefix) {
		if (addedNamespaces != null) {
			addedNamespaces.remove(prefix);
		}
		if (removedPrefixes == null) {
			removedPrefixes = new HashSet<String>();
		}
		removedPrefixes.add(prefix);
	}

	@Override
	public synchronized void clearNamespaces() {
		if (removedPrefixes != null) {
			removedPrefixes.clear();
		}
		if (addedNamespaces != null) {
			addedNamespaces.clear();
		}
		namespaceCleared = true;
	}

	@Override
	public synchronized void observe(Resource subj, IRI pred, Value obj, Resource... contexts)
		throws SailConflictException
	{
		if (observations == null) {
			observations = new HashSet<StatementPattern>();
		}
		if (contexts == null) {
			observations.add(new StatementPattern(new Var("s", subj), new Var("p", pred), new Var("o", obj),
					new Var("g", null)));
		}
		else if (contexts.length == 0) {
			observations.add(new StatementPattern(new Var("s", subj), new Var("p", pred), new Var("o", obj)));
		}
		else {
			for (Resource ctx : contexts) {
				observations.add(new StatementPattern(new Var("s", subj), new Var("p", pred), new Var("o", obj),
						new Var("g", ctx)));
			}
		}
	}

	@Override
	public synchronized void clear(Resource... contexts) {
		if (contexts != null && contexts.length == 0) {
			if (approved != null) {
				approved.remove(null, null, null);
			}
			if (approvedContexts != null) {
				approvedContexts.clear();
			}
			statementCleared = true;
		}
		else {
			if (approved != null) {
				approved.remove(null, null, null, contexts);
			}
			if (approvedContexts != null) {
				approvedContexts.removeAll(Arrays.asList(contexts));
			}
			if (deprecatedContexts == null) {
				deprecatedContexts = new HashSet<Resource>();
			}
			deprecatedContexts.addAll(Arrays.asList(contexts));
		}
	}

	@Override
	public synchronized void approve(Resource subj, IRI pred, Value obj, Resource ctx) {
		if (deprecated != null) {
			deprecated.remove(subj, pred, obj, ctx);
		}
		if (approved == null) {
			approved = createEmptyModel();
		}
		approved.add(subj, pred, obj, ctx);
		if (ctx != null) {
			if (approvedContexts == null) {
				approvedContexts = new HashSet<Resource>();
			}
			approvedContexts.add(ctx);
		}
	}

	@Override
	public synchronized void deprecate(Resource subj, IRI pred, Value obj, Resource ctx) {
		if (approved != null) {
			approved.remove(subj, pred, obj, ctx);
		}
		if (deprecated == null) {
			deprecated = createEmptyModel();
		}
		deprecated.add(subj, pred, obj, ctx);
		if (approvedContexts != null && approvedContexts.contains(ctx)
				&& !approved.contains(null, null, null, ctx))
		{
			approvedContexts.remove(ctx);
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (observations != null) {
			sb.append(observations.size());
			sb.append(" observations, ");
		}
		if (namespaceCleared) {
			sb.append("namespaceCleared, ");
		}
		if (removedPrefixes != null) {
			sb.append(removedPrefixes.size());
			sb.append(" removedPrefixes, ");
		}
		if (addedNamespaces != null) {
			sb.append(addedNamespaces.size());
			sb.append(" addedNamespaces, ");
		}
		if (statementCleared) {
			sb.append("statementCleared, ");
		}
		if (deprecatedContexts != null && !deprecatedContexts.isEmpty()) {
			sb.append(deprecatedContexts.size());
			sb.append(" deprecatedContexts, ");
		}
		if (deprecated != null) {
			sb.append(deprecated.size());
			sb.append(" deprecated, ");
		}
		if (approved != null) {
			sb.append(approved.size());
			sb.append(" approved, ");
		}
		if (sb.length() > 0) {
			return sb.substring(0, sb.length() - 2);
		}
		else {
			return super.toString();
		}
	}

	protected void setChangeset(Changeset from) {
		this.observations = from.observations;
		this.approved = from.approved;
		this.deprecated = from.deprecated;
		this.approvedContexts = from.approvedContexts;
		this.deprecatedContexts = from.deprecatedContexts;
		this.addedNamespaces = from.addedNamespaces;
		this.removedPrefixes = from.removedPrefixes;
		this.namespaceCleared = from.namespaceCleared;
		this.statementCleared = from.statementCleared;
	}

	public synchronized Set<StatementPattern> getObservations() {
		return observations;
	}

	public synchronized Model getApproved() {
		return approved;
	}

	public synchronized Model getDeprecated() {
		return deprecated;
	}

	public synchronized Set<Resource> getApprovedContexts() {
		return approvedContexts;
	}

	public synchronized Set<Resource> getDeprecatedContexts() {
		return deprecatedContexts;
	}

	public synchronized boolean isStatementCleared() {
		return statementCleared;
	}

	public synchronized Map<String, String> getAddedNamespaces() {
		return addedNamespaces;
	}

	public synchronized Set<String> getRemovedPrefixes() {
		return removedPrefixes;
	}

	public synchronized boolean isNamespaceCleared() {
		return namespaceCleared;
	}
}
