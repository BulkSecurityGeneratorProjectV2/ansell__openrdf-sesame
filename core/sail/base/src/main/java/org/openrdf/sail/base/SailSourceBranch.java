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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.openrdf.IsolationLevel;
import org.openrdf.IsolationLevels;
import org.openrdf.model.Model;
import org.openrdf.model.ModelFactory;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.IRI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.TreeModelFactory;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.Var;
import org.openrdf.sail.SailException;

/**
 * An {@link SailSource} that keeps a delta of its state from a backing
 * {@link SailSource}.
 * 
 * @author James Leigh
 */
class SailSourceBranch implements SailSource {

	/**
	 * Used to prevent changes to this object's field from multiple threads.
	 */
	private final ReentrantLock semaphore = new ReentrantLock();

	/**
	 * The difference between this {@link SailSource} and the backing
	 * {@link SailSource}.
	 */
	private final LinkedList<Changeset> changes = new LinkedList<Changeset>();

	/**
	 * {@link SailSink} that have been created, but not yet
	 * {@link SailSink#flush()}ed to this {@link SailSource}.
	 */
	private final Collection<Changeset> pending = new LinkedList<Changeset>();

	/**
	 * Set of open {@link SailDataset} for this {@link SailSource}.
	 */
	private final Collection<SailDataset> observers = new LinkedList<SailDataset>();

	/**
	 * The underly {@link SailSource} this {@link SailSource} is derived from.
	 */
	private final SailSource backingSource;

	/**
	 * The {@link Model} instances that should be used to store
	 * {@link SailSink#approve(Resource, IRI, Value, Resource)} and
	 * {@link SailSink#deprecate(Resource, IRI, Value, Resource)} statements.
	 */
	private final ModelFactory modelFactory;

	/**
	 * If this {@link SailSource} should be flushed to the backing
	 * {@link SailSource} when it is not in use.
	 */
	private final boolean autoFlush;

	/**
	 * Non-null when in {@link IsolationLevels#SNAPSHOT} (or higher) mode.
	 */
	private SailDataset snapshot;

	/**
	 * Non-null when in {@link IsolationLevels#SERIALIZABLE} (or higher) mode.
	 */
	private SailSink serializable;

	/**
	 * Non-null after {@link #prepare()}, but before {@link #flush()}.
	 */
	private SailSink prepared;

	/**
	 * Creates a new in-memory {@link SailSource} derived from the given
	 * {@link SailSource}.
	 * 
	 * @param backingSource
	 */
	public SailSourceBranch(SailSource backingSource) {
		this(backingSource, new TreeModelFactory(), false);
	}

	/**
	 * Creates a new {@link SailSource} derived from the given {@link SailSource}.
	 * 
	 * @param backingSource
	 * @param modelFactory
	 */
	public SailSourceBranch(SailSource backingSource, ModelFactory modelFactory) {
		this(backingSource, modelFactory, false);
	}

	/**
	 * Creates a new {@link SailSource} derived from the given {@link SailSource}
	 * and if <code>autoFlush</code> is true, will automatically call
	 * {@link #flush()} when not in use.
	 * 
	 * @param backingSource
	 * @param modelFactory
	 * @param autoFlush
	 */
	public SailSourceBranch(SailSource backingSource, ModelFactory modelFactory, boolean autoFlush) {
		this.backingSource = backingSource;
		this.modelFactory = modelFactory;
		this.autoFlush = autoFlush;
	}

	@Override
	public void close()
		throws SailException
	{
		try {
			semaphore.lock();
			if (snapshot != null) {
				try {
					snapshot.close();
				}
				finally {
					snapshot = null;
				}
			}
			if (serializable != null) {
				try {
					serializable.close();
				}
				finally {
					serializable = null;
				}
			}
		}
		finally {
			semaphore.unlock();
		}
	}

	@Override
	public SailSink sink(IsolationLevel level)
		throws SailException
	{
		Changeset changeset = new Changeset() {

			private boolean prepared;

			@Override
			public void prepare()
				throws SailException
			{
				if (!prepared) {
					preparedChangeset(this);
					prepared = true;
				}
				super.prepare();
			}

			@Override
			public void flush()
				throws SailException
			{
				merge(this);
			}

			@Override
			public void close()
				throws SailException
			{
				try {
					super.close();
				}
				finally {
					if (prepared) {
						closeChangeset(this);
						prepared = false;
					}
					autoFlush();
				}
			}

			@Override
			public Model createEmptyModel() {
				return modelFactory.createEmptyModel();
			}
		};
		try {
			semaphore.lock();
			pending.add(changeset);
		}
		finally {
			semaphore.unlock();
		}
		return changeset;
	}

	@Override
	public SailDataset dataset(IsolationLevel level)
		throws SailException
	{
		SailDataset dataset = new DelegatingSailDataset(derivedFromSerializable(level)) {

			@Override
			public void close()
				throws SailException
			{
				super.close();
				try {
					semaphore.lock();
					observers.remove(this);
					compressChanges();
					autoFlush();
				}
				finally {
					semaphore.unlock();
				}
			}
		};
		try {
			semaphore.lock();
			observers.add(dataset);
		}
		finally {
			semaphore.unlock();
		}
		return dataset;
	}

	@Override
	public SailSource fork() {
		return new SailSourceBranch(this, modelFactory);
	}

	@Override
	public void prepare()
		throws SailException
	{
		try {
			semaphore.lock();
			if (!changes.isEmpty()) {
				if (prepared == null && serializable == null) {
					prepared = backingSource.sink(IsolationLevels.NONE);
				}
				else if (prepared == null) {
					prepared = serializable;
				}
				prepare(prepared);
				prepared.prepare();
			}
		}
		finally {
			semaphore.unlock();
		}
	}

	@Override
	public void flush()
		throws SailException
	{
		try {
			semaphore.lock();
			if (!changes.isEmpty()) {
				if (prepared == null) {
					prepare();
				}
				flush(prepared);
				prepared.flush();
				try {
					if (prepared != serializable) {
						prepared.close();
					}
				}
				finally {
					prepared = null;
				}
			}
		}
		finally {
			semaphore.unlock();
		}
	}

	public boolean isChanged() {
		try {
			semaphore.lock();
			return !changes.isEmpty();
		}
		finally {
			semaphore.unlock();
		}
	}

	public String toString() {
		return backingSource.toString() + "\n" + changes.toString();
	}

	void preparedChangeset(Changeset changeset) {
		semaphore.lock();
	}

	void merge(Changeset change) {
		try {
			semaphore.lock();
			pending.remove(change);
			if (isChanged(change)) {
				Changeset merged;
				changes.add(change);
				compressChanges();
				merged = changes.getLast();
				for (Changeset c : pending) {
					c.prepend(merged);
				}
			}
		}
		finally {
			semaphore.unlock();
		}
	}

	void compressChanges() {
		try {
			semaphore.lock();
			while (changes.size() > 1 && !changes.get(changes.size() - 2).isRefback()) {
				try {
					Changeset pop = changes.removeLast();
					prepare(pop, changes.getLast());
					flush(pop, changes.getLast());
				}
				catch (SailException e) {
					// Changeset does not throw SailException
					throw new AssertionError(e);
				}
			}
		}
		finally {
			semaphore.unlock();
		}
	}

	void closeChangeset(Changeset changeset) {
		semaphore.unlock();
	}

	void autoFlush()
		throws SailException
	{
		if (autoFlush && semaphore.tryLock()) {
			try {
				if (serializable == null && observers.isEmpty()) {
					flush();
				}
			}
			finally {
				semaphore.unlock();
			}
		}
	}

	private boolean isChanged(Changeset change) {
		return change.getApproved() != null || change.getDeprecated() != null
				|| change.getApprovedContexts() != null || change.getDeprecatedContexts() != null
				|| change.getAddedNamespaces() != null || change.getRemovedPrefixes() != null
				|| change.isStatementCleared() || change.isNamespaceCleared() || change.getObservations() != null;
	}

	private SailDataset derivedFromSerializable(IsolationLevel level)
		throws SailException
	{
		try {
			semaphore.lock();
			if (serializable == null && level.isCompatibleWith(IsolationLevels.SERIALIZABLE)) {
				serializable = backingSource.sink(level);
			}
			SailDataset derivedFrom = derivedFromSnapshot(level);
			if (serializable == null) {
				return derivedFrom;
			}
			else {
				return new ObservingSailDataset(derivedFrom, sink(level));
			}
		}
		finally {
			semaphore.unlock();
		}
	}

	private SailDataset derivedFromSnapshot(IsolationLevel level)
		throws SailException
	{
		try {
			semaphore.lock();
			SailDataset derivedFrom;
			if (this.snapshot != null) {
				// this object is already has at least snapshot isolation
				derivedFrom = new DelegatingSailDataset(this.snapshot) {

					@Override
					public void close()
						throws SailException
					{
						// don't close snapshot yet
					}
				};
			}
			else {
				derivedFrom = backingSource.dataset(level);
				if (level.isCompatibleWith(IsolationLevels.SNAPSHOT)) {
					this.snapshot = derivedFrom;
					// don't release snapshot until this SailSource is released
					derivedFrom = new DelegatingSailDataset(derivedFrom) {

						@Override
						public void close()
							throws SailException
						{
							// don't close snapshot yet
						}
					};
				}
			}
			Iterator<Changeset> iter = changes.iterator();
			while (iter.hasNext()) {
				derivedFrom = new SailDatasetImpl(derivedFrom, iter.next());
			}
			return derivedFrom;
		}
		finally {
			semaphore.unlock();
		}
	}

	private void prepare(SailSink sink)
		throws SailException
	{
		try {
			semaphore.lock();
			Iterator<Changeset> iter = changes.iterator();
			while (iter.hasNext()) {
				prepare(iter.next(), sink);
			}
		}
		finally {
			semaphore.unlock();
		}
	}

	private void prepare(Changeset change, SailSink sink)
		throws SailException
	{
		Set<StatementPattern> observations = change.getObservations();
		if (observations != null) {
			for (StatementPattern p : observations) {
				Resource subj = (Resource)p.getSubjectVar().getValue();
				IRI pred = (IRI)p.getPredicateVar().getValue();
				Value obj = p.getObjectVar().getValue();
				Var ctxVar = p.getContextVar();
				if (ctxVar == null) {
					sink.observe(subj, pred, obj);
				}
				else {
					sink.observe(subj, pred, obj, (Resource)ctxVar.getValue());
				}
			}
		}
	}

	private void flush(SailSink sink)
		throws SailException
	{
		try {
			semaphore.lock();
			if (changes.size() == 1 && !changes.getFirst().isRefback() && sink instanceof Changeset
					&& !isChanged((Changeset)sink))
			{
				// one change to apply that is not in use to an empty Changeset
				Changeset dst = (Changeset)sink;
				dst.setChangeset(changes.pop());
			}
			else {
				Iterator<Changeset> iter = changes.iterator();
				while (iter.hasNext()) {
					flush(iter.next(), sink);
					iter.remove();
				}
			}
		}
		finally {
			semaphore.unlock();
		}
	}

	private void flush(Changeset change, SailSink sink)
		throws SailException
	{
		prepare(change, sink);
		if (change.isNamespaceCleared()) {
			sink.clearNamespaces();
		}
		Set<String> removedPrefixes = change.getRemovedPrefixes();
		if (removedPrefixes != null) {
			for (String prefix : removedPrefixes) {
				sink.removeNamespace(prefix);
			}
		}
		Map<String, String> addedNamespaces = change.getAddedNamespaces();
		if (addedNamespaces != null) {
			for (Map.Entry<String, String> e : addedNamespaces.entrySet()) {
				sink.setNamespace(e.getKey(), e.getValue());
			}
		}
		if (change.isStatementCleared()) {
			sink.clear();
		}
		Set<Resource> deprecatedContexts = change.getDeprecatedContexts();
		if (deprecatedContexts != null && !deprecatedContexts.isEmpty()) {
			sink.clear(deprecatedContexts.toArray(new Resource[deprecatedContexts.size()]));
		}
		Model deprecated = change.getDeprecated();
		if (deprecated != null) {
			for (Statement st : deprecated) {
				sink.deprecate(st.getSubject(), st.getPredicate(), st.getObject(), st.getContext());
			}
		}
		Model approved = change.getApproved();
		if (approved != null) {
			for (Statement st : approved) {
				sink.approve(st.getSubject(), st.getPredicate(), st.getObject(), st.getContext());
			}
		}
	}

}
