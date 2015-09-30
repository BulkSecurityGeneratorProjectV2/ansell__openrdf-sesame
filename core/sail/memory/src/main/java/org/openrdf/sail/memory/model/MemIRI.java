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
package org.openrdf.sail.memory.model;

import org.openrdf.model.IRI;

/**
 * A MemoryStore-specific implementation of URI that stores separated namespace
 * and local name information to enable reuse of namespace String objects
 * (reducing memory usage) and that gives it node properties.
 */
public class MemIRI implements IRI, MemResource {

	private static final long serialVersionUID = 9118488004995852467L;

	/*------------*
	 * Attributes *
	 *------------*/

	/**
	 * The URI's namespace.
	 */
	private final String namespace;

	/**
	 * The URI's local name.
	 */
	private final String localName;

	/**
	 * The object that created this MemURI.
	 */
	transient private final Object creator;

	/**
	 * The MemURI's hash code, 0 if not yet initialized.
	 */
	private int hashCode = 0;

	/**
	 * The list of statements for which this MemURI is the subject.
	 */
	transient private volatile MemStatementList subjectStatements = null;

	/**
	 * The list of statements for which this MemURI is the predicate.
	 */
	transient private volatile MemStatementList predicateStatements = null;

	/**
	 * The list of statements for which this MemURI is the object.
	 */
	transient private volatile MemStatementList objectStatements = null;

	/**
	 * The list of statements for which this MemURI represents the context.
	 */
	transient private volatile MemStatementList contextStatements = null;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new MemURI for a URI.
	 * 
	 * @param creator
	 *        The object that is creating this MemURI.
	 * @param namespace
	 *        namespace part of URI.
	 * @param localName
	 *        localname part of URI.
	 */
	public MemIRI(Object creator, String namespace, String localName) {
		this.creator = creator;
		this.namespace = namespace;
		this.localName = localName;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public String toString() {
		return namespace + localName;
	}

	public String stringValue() {
		return toString();
	}

	public String getNamespace() {
		return namespace;
	}

	public String getLocalName() {
		return localName;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}

		if (other instanceof MemIRI) {
			MemIRI o = (MemIRI)other;
			return namespace.equals(o.getNamespace()) && localName.equals(o.getLocalName());
		}
		else if (other instanceof IRI) {
			String otherStr = other.toString();

			return namespace.length() + localName.length() == otherStr.length() && otherStr.endsWith(localName)
					&& otherStr.startsWith(namespace);
		}

		return false;
	}

	@Override
	public int hashCode() {
		if (hashCode == 0) {
			hashCode = toString().hashCode();
		}

		return hashCode;
	}

	public Object getCreator() {
		return creator;
	}

	public boolean hasStatements() {
		return subjectStatements != null || predicateStatements != null || objectStatements != null
				|| contextStatements != null;
	}

	public MemStatementList getSubjectStatementList() {
		if (subjectStatements == null) {
			return EMPTY_LIST;
		}
		else {
			return subjectStatements;
		}
	}

	public int getSubjectStatementCount() {
		if (subjectStatements == null) {
			return 0;
		}
		else {
			return subjectStatements.size();
		}
	}

	public void addSubjectStatement(MemStatement st) {
		if (subjectStatements == null) {
			subjectStatements = new MemStatementList(4);
		}

		subjectStatements.add(st);
	}

	public void removeSubjectStatement(MemStatement st) {
		subjectStatements.remove(st);

		if (subjectStatements.isEmpty()) {
			subjectStatements = null;
		}
	}

	public void cleanSnapshotsFromSubjectStatements(int currentSnapshot) {
		if (subjectStatements != null) {
			subjectStatements.cleanSnapshots(currentSnapshot);

			if (subjectStatements.isEmpty()) {
				subjectStatements = null;
			}
		}
	}

	/**
	 * Gets the list of statements for which this MemURI is the predicate.
	 * 
	 * @return a MemStatementList containing the statements.
	 */
	public MemStatementList getPredicateStatementList() {
		if (predicateStatements == null) {
			return EMPTY_LIST;
		}
		else {
			return predicateStatements;
		}
	}

	/**
	 * Gets the number of Statements for which this MemURI is the predicate.
	 * 
	 * @return An integer larger than or equal to 0.
	 */
	public int getPredicateStatementCount() {
		if (predicateStatements == null) {
			return 0;
		}
		else {
			return predicateStatements.size();
		}
	}

	/**
	 * Adds a statement to this MemURI's list of statements for which it is the
	 * predicate.
	 */
	public void addPredicateStatement(MemStatement st) {
		if (predicateStatements == null) {
			predicateStatements = new MemStatementList(4);
		}

		predicateStatements.add(st);
	}

	/**
	 * Removes a statement from this MemURI's list of statements for which it is
	 * the predicate.
	 */
	public void removePredicateStatement(MemStatement st) {
		predicateStatements.remove(st);

		if (predicateStatements.isEmpty()) {
			predicateStatements = null;
		}
	}

	/**
	 * Removes statements from old snapshots (those that have expired at or
	 * before the specified snapshot version) from this MemValue's list of
	 * statements for which it is the predicate.
	 * 
	 * @param currentSnapshot
	 *        The current snapshot version.
	 */
	public void cleanSnapshotsFromPredicateStatements(int currentSnapshot) {
		if (predicateStatements != null) {
			predicateStatements.cleanSnapshots(currentSnapshot);

			if (predicateStatements.isEmpty()) {
				predicateStatements = null;
			}
		}
	}

	public MemStatementList getObjectStatementList() {
		if (objectStatements == null) {
			return EMPTY_LIST;
		}
		else {
			return objectStatements;
		}
	}

	public int getObjectStatementCount() {
		if (objectStatements == null) {
			return 0;
		}
		else {
			return objectStatements.size();
		}
	}

	public void addObjectStatement(MemStatement st) {
		if (objectStatements == null) {
			objectStatements = new MemStatementList(4);
		}
		objectStatements.add(st);
	}

	public void removeObjectStatement(MemStatement st) {
		objectStatements.remove(st);
		if (objectStatements.isEmpty()) {
			objectStatements = null;
		}
	}

	public void cleanSnapshotsFromObjectStatements(int currentSnapshot) {
		if (objectStatements != null) {
			objectStatements.cleanSnapshots(currentSnapshot);

			if (objectStatements.isEmpty()) {
				objectStatements = null;
			}
		}
	}

	public MemStatementList getContextStatementList() {
		if (contextStatements == null) {
			return EMPTY_LIST;
		}
		else {
			return contextStatements;
		}
	}

	public int getContextStatementCount() {
		if (contextStatements == null) {
			return 0;
		}
		else {
			return contextStatements.size();
		}
	}

	public void addContextStatement(MemStatement st) {
		if (contextStatements == null) {
			contextStatements = new MemStatementList(4);
		}

		contextStatements.add(st);
	}

	public void removeContextStatement(MemStatement st) {
		contextStatements.remove(st);

		if (contextStatements.isEmpty()) {
			contextStatements = null;
		}
	}

	public void cleanSnapshotsFromContextStatements(int currentSnapshot) {
		if (contextStatements != null) {
			contextStatements.cleanSnapshots(currentSnapshot);

			if (contextStatements.isEmpty()) {
				contextStatements = null;
			}
		}
	}
}
