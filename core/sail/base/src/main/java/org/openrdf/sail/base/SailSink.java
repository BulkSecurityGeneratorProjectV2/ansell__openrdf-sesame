/* 
 * Licensed to Aduna under one or more contributor license agreements.  
 * See the NOTICE.txt file distributed with this work for additional 
 * information regarding copyright ownership. 
 *
 * Aduna licenses this file to you under the terms of the Aduna BSD 
 * License (the "License"); you may not use this file except in compliance 
 * with the License. See the LICENSE.txt file distributed with this work 
 * for the full License.
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or 
 * implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package org.openrdf.sail.base;

import org.openrdf.IsolationLevels;
import org.openrdf.model.Resource;
import org.openrdf.model.IRI;
import org.openrdf.model.Value;
import org.openrdf.sail.SailConflictException;
import org.openrdf.sail.SailException;

/**
 * A mutable source of RDF graphs. The life cycle follows that of a write
 * operation.
 * 
 * @author James Leigh
 */
public interface SailSink extends SailClosable {

	/**
	 * Checks if this {@link SailSink} is consistent with the isolation level it
	 * was created with. If this Sink was created with a
	 * {@link IsolationLevels#SERIALIZABLE} and another conflicting
	 * {@link SailSink} has already been {@link #flush()}ed, this method will
	 * throw a {@link SailConflictException}.
	 * 
	 * @return <code>false</code> if this sink has a conflict
	 */
	void prepare()
		throws SailException;

	/**
	 * Once this method returns successfully, changes that were made to this
	 * {@link SailSink} will be visible to subsequent
	 * {@link SailSource#dataset(org.openrdf.IsolationLevel)}.
	 * 
	 * @throws SailException
	 */
	void flush()
		throws SailException;

	/**
	 * Sets the prefix for a namespace.
	 * 
	 * @param prefix
	 *        The new prefix, or an empty string in case of the default
	 *        namespace.
	 * @param name
	 *        The namespace name that the prefix maps to.
	 * @throws SailException
	 *         If the Sail object encountered an error or unexpected situation
	 *         internally.
	 * @throws NullPointerException
	 *         In case <tt>prefix</tt> or <tt>name</tt> is <tt>null</tt>.
	 */
	void setNamespace(String prefix, String name)
		throws SailException;

	/**
	 * Removes a namespace declaration by removing the association between a
	 * prefix and a namespace name.
	 * 
	 * @param prefix
	 *        The namespace prefix, or an empty string in case of the default
	 *        namespace.
	 * @throws SailException
	 * @throws NullPointerException
	 *         In case <tt>prefix</tt> is <tt>null</tt>.
	 */
	void removeNamespace(String prefix)
		throws SailException;

	/**
	 * Removes all namespace declarations from this {@link SailSource}.
	 * 
	 * @throws SailException
	 */
	void clearNamespaces()
		throws SailException;

	/**
	 * Removes all statements from the specified/all contexts. If no contexts are
	 * specified the method operates on the entire repository.
	 * 
	 * @param contexts
	 *        The context(s) from which to remove the statements. Note that this
	 *        parameter is a vararg and as such is optional. If no contexts are
	 *        specified the method operates on the entire repository. A
	 *        <tt>null</tt> value can be used to match context-less statements.
	 * @throws SailException
	 *         If the statements could not be removed.
	 */
	public void clear(Resource... contexts)
		throws SailException;

	/**
	 * Called to indicate matching statements have been observed and must not
	 * change their state until after this {@link SailSink} is committed, iff this
	 * was opened in an isolation level compatible with
	 * {@link IsolationLevels#SERIALIZABLE}.
	 * 
	 * @param subj
	 *        A Resource specifying the subject, or <tt>null</tt> for a wildcard.
	 * @param pred
	 *        A IRI specifying the predicate, or <tt>null</tt> for a wildcard.
	 * @param obj
	 *        A Value specifying the object, or <tt>null</tt> for a wildcard.
	 * @param contexts
	 *        The context(s) of the observed statements. Note that this parameter
	 *        is a vararg and as such is optional. If no contexts are supplied
	 *        the method operates on all contexts.
	 * @throws SailException
	 *         If the triple source failed to observe these statements.
	 */
	public void observe(Resource subj, IRI pred, Value obj, Resource... contexts)
		throws SailException;

	/**
	 * Adds a statement to the store.
	 * 
	 * @param subj
	 *        The subject of the statement to add.
	 * @param pred
	 *        The predicate of the statement to add.
	 * @param obj
	 *        The object of the statement to add.
	 * @param ctx
	 *        The context to add the statement to.
	 * @throws SailException
	 *         If the statement could not be added, for example because no
	 *         transaction is active.
	 */
	void approve(Resource subj, IRI pred, Value obj, Resource ctx)
		throws SailException;

	/**
	 * Removes a statement with the specified subject, predicate, object, and
	 * context. All four parameters may be non-null.
	 * 
	 * @param subj
	 *        The subject of the statement that should be removed
	 * @param pred
	 *        The predicate of the statement that should be removed
	 * @param obj
	 *        The object of the statement that should be removed
	 * @param ctx
	 *        The context from which to remove the statement
	 * @throws SailException
	 *         If the statement could not be removed, for example because no
	 *         transaction is active.
	 */
	void deprecate(Resource subj, IRI pred, Value obj, Resource ctx)
		throws SailException;

}
