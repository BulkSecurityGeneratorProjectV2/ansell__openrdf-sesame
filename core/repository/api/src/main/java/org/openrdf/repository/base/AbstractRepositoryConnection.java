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
package org.openrdf.repository.base;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.aduna.iteration.Iteration;
import info.aduna.iteration.Iterations;

import org.openrdf.IsolationLevel;
import org.openrdf.OpenRDFUtil;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.IRI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.Query;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.Update;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.UnknownTransactionStateException;
import org.openrdf.repository.util.RDFInserter;
import org.openrdf.repository.util.RDFLoader;
import org.openrdf.rio.ParserConfig;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;

/**
 * Abstract class implementing most 'convenience' methods in the
 * {@link RepositoryConnection} interface by transforming parameters and mapping
 * the methods to the basic (abstractly declared) methods.
 * <p>
 * Open connections are automatically closed when being garbage collected. A
 * warning message will be logged when the system property
 * <tt>org.openrdf.repository.debug</tt> has been set to a non-<tt>null</tt>
 * value.
 * 
 * @author Jeen Broekstra
 * @author Arjohn Kampman
 */
public abstract class AbstractRepositoryConnection implements RepositoryConnection {

	protected final Logger logger = LoggerFactory.getLogger(this.getClass());

	private final Repository repository;

	private volatile ParserConfig parserConfig = new ParserConfig();

	private volatile boolean isOpen;

	private IsolationLevel isolationLevel;

	// private volatile boolean active;

	protected AbstractRepositoryConnection(Repository repository) {
		this.repository = repository;
		this.isOpen = true;
	}

	public void setParserConfig(ParserConfig parserConfig) {
		this.parserConfig = parserConfig;
	}

	public ParserConfig getParserConfig() {
		return parserConfig;
	}

	public Repository getRepository() {
		return repository;
	}

	public ValueFactory getValueFactory() {
		return getRepository().getValueFactory();
	}

	@Override
	public void begin(IsolationLevel level)
		throws RepositoryException
	{
		setIsolationLevel(level);
		begin();
	}

	@Override
	public void setIsolationLevel(IsolationLevel level)
		throws IllegalStateException
	{
		try {
			if (isActive()) {
				throw new IllegalStateException(
						"Transaction isolation level can not be modified while transaction is active");
			}
			this.isolationLevel = level;
		}
		catch (UnknownTransactionStateException e) {
			throw new IllegalStateException(
					"Transaction isolation level can not be modified while transaction state is unknown", e);

		}
		catch (RepositoryException e) {
			throw new IllegalStateException(
					"Transaction isolation level can not be modified due to repository error", e);
		}
	}

	@Override
	public IsolationLevel getIsolationLevel() {
		return this.isolationLevel;
	}

	public boolean isOpen()
		throws RepositoryException
	{
		return isOpen;
	}

	public void close()
		throws RepositoryException
	{
		isOpen = false;
	}

	public Query prepareQuery(QueryLanguage ql, String query)
		throws MalformedQueryException, RepositoryException
	{
		return prepareQuery(ql, query, null);
	}

	public TupleQuery prepareTupleQuery(QueryLanguage ql, String query)
		throws MalformedQueryException, RepositoryException
	{
		return prepareTupleQuery(ql, query, null);
	}

	public GraphQuery prepareGraphQuery(QueryLanguage ql, String query)
		throws MalformedQueryException, RepositoryException
	{
		return prepareGraphQuery(ql, query, null);
	}

	public BooleanQuery prepareBooleanQuery(QueryLanguage ql, String query)
		throws MalformedQueryException, RepositoryException
	{
		return prepareBooleanQuery(ql, query, null);
	}

	public Update prepareUpdate(QueryLanguage ql, String update)
		throws MalformedQueryException, RepositoryException
	{
		return prepareUpdate(ql, update, null);
	}

	public boolean hasStatement(Resource subj, IRI pred, Value obj, boolean includeInferred,
			Resource... contexts)
		throws RepositoryException
	{
		RepositoryResult<Statement> stIter = getStatements(subj, pred, obj, includeInferred, contexts);
		try {
			return stIter.hasNext();
		}
		finally {
			stIter.close();
		}
	}

	public boolean hasStatement(Statement st, boolean includeInferred, Resource... contexts)
		throws RepositoryException
	{
		return hasStatement(st.getSubject(), st.getPredicate(), st.getObject(), includeInferred, contexts);
	}

	public boolean isEmpty()
		throws RepositoryException
	{
		return size() == 0;
	}

	public void export(RDFHandler handler, Resource... contexts)
		throws RepositoryException, RDFHandlerException
	{
		exportStatements(null, null, null, false, handler, contexts);
	}

	/**
	 * @deprecated since 2.7.0. Use {@link #begin()} instead.
	 */
	@Deprecated
	public void setAutoCommit(boolean autoCommit)
		throws RepositoryException
	{
		if (isActive()) {
			if (autoCommit) {
				// we are switching to autocommit mode from an active transaction.
				commit();
			}
		}
		else if (!autoCommit) {
			// begin a transaction
			begin();
		}
	}

	/**
	 * @deprecated since 2.7.0. Use {@link #isActive()} instead.
	 */
	@Deprecated
	public boolean isAutoCommit()
		throws RepositoryException
	{
		return !isActive();
	}

	public void add(File file, String baseURI, RDFFormat dataFormat, Resource... contexts)
		throws IOException, RDFParseException, RepositoryException
	{
		OpenRDFUtil.verifyContextNotNull(contexts);

		RDFInserter rdfInserter = new RDFInserter(this);
		rdfInserter.enforceContext(contexts);

		boolean localTransaction = startLocalTransaction();

		try {
			RDFLoader loader = new RDFLoader(getParserConfig(), getValueFactory());
			loader.load(file, baseURI, dataFormat, rdfInserter);

			conditionalCommit(localTransaction);
		}
		catch (RDFHandlerException e) {
			conditionalRollback(localTransaction);

			// RDFInserter only throws wrapped RepositoryExceptions
			throw (RepositoryException)e.getCause();
		}
		catch (RDFParseException e) {
			conditionalRollback(localTransaction);
			throw e;
		}
		catch (IOException e) {
			conditionalRollback(localTransaction);
			throw e;
		}
		catch (RuntimeException e) {
			conditionalRollback(localTransaction);
			throw e;
		}
	}

	public void add(URL url, String baseURI, RDFFormat dataFormat, Resource... contexts)
		throws IOException, RDFParseException, RepositoryException
	{
		OpenRDFUtil.verifyContextNotNull(contexts);

		RDFInserter rdfInserter = new RDFInserter(this);
		rdfInserter.enforceContext(contexts);

		boolean localTransaction = startLocalTransaction();

		try {
			RDFLoader loader = new RDFLoader(getParserConfig(), getValueFactory());
			loader.load(url, baseURI, dataFormat, rdfInserter);

			conditionalCommit(localTransaction);
		}
		catch (RDFHandlerException e) {
			conditionalRollback(localTransaction);

			// RDFInserter only throws wrapped RepositoryExceptions
			throw (RepositoryException)e.getCause();
		}
		catch (RDFParseException e) {
			conditionalRollback(localTransaction);
			throw e;
		}
		catch (IOException e) {
			conditionalRollback(localTransaction);
			throw e;
		}
		catch (RuntimeException e) {
			conditionalRollback(localTransaction);
			throw e;
		}
	}

	public void add(InputStream in, String baseURI, RDFFormat dataFormat, Resource... contexts)
		throws IOException, RDFParseException, RepositoryException
	{
		OpenRDFUtil.verifyContextNotNull(contexts);

		RDFInserter rdfInserter = new RDFInserter(this);
		rdfInserter.enforceContext(contexts);

		boolean localTransaction = startLocalTransaction();

		try {
			RDFLoader loader = new RDFLoader(getParserConfig(), getValueFactory());
			loader.load(in, baseURI, dataFormat, rdfInserter);

			conditionalCommit(localTransaction);
		}
		catch (RDFHandlerException e) {
			conditionalRollback(localTransaction);

			// RDFInserter only throws wrapped RepositoryExceptions
			throw (RepositoryException)e.getCause();
		}
		catch (RDFParseException e) {
			conditionalRollback(localTransaction);
			throw e;
		}
		catch (IOException e) {
			conditionalRollback(localTransaction);
			throw e;
		}
		catch (RuntimeException e) {
			conditionalRollback(localTransaction);
			throw e;
		}
	}

	/**
	 * Starts a new transaction if one is not already active.
	 * 
	 * @since 2.7.0
	 * @return <code>true</code> if a new transaction was started,
	 *         <code>false</code> if a transaction was already active.
	 * @throws RepositoryException
	 */
	protected final boolean startLocalTransaction()
		throws RepositoryException
	{
		if (!isActive()) {
			begin();
			return true;
		}
		return false;
	}

	/**
	 * Invokes {@link #commit()} if supplied boolean condition is
	 * <code>true</code>.
	 * 
	 * @since 2.7.0
	 * @param condition
	 *        a boolean condition.
	 * @throws RepositoryException
	 */
	protected final void conditionalCommit(boolean condition)
		throws RepositoryException
	{
		if (condition) {
			commit();
		}
	}

	/**
	 * Invokes {@link #rollback()} if supplied boolean condition is
	 * <code>true</code>.
	 * 
	 * @since 2.7.0
	 * @param condition
	 *        a boolean condition.
	 * @throws RepositoryException
	 */
	protected final void conditionalRollback(boolean condition)
		throws RepositoryException
	{
		if (condition) {
			rollback();
		}
	}

	public void add(Reader reader, String baseURI, RDFFormat dataFormat, Resource... contexts)
		throws IOException, RDFParseException, RepositoryException
	{
		OpenRDFUtil.verifyContextNotNull(contexts);

		RDFInserter rdfInserter = new RDFInserter(this);
		rdfInserter.enforceContext(contexts);

		boolean localTransaction = startLocalTransaction();

		try {
			RDFLoader loader = new RDFLoader(getParserConfig(), getValueFactory());
			loader.load(reader, baseURI, dataFormat, rdfInserter);

			conditionalCommit(localTransaction);
		}
		catch (RDFHandlerException e) {
			conditionalRollback(localTransaction);

			// RDFInserter only throws wrapped RepositoryExceptions
			throw (RepositoryException)e.getCause();
		}
		catch (RDFParseException e) {
			conditionalRollback(localTransaction);
			throw e;
		}
		catch (IOException e) {
			conditionalRollback(localTransaction);
			throw e;
		}
		catch (RuntimeException e) {
			conditionalRollback(localTransaction);
			throw e;
		}
	}

	public void add(Iterable<? extends Statement> statements, Resource... contexts)
		throws RepositoryException
	{
		OpenRDFUtil.verifyContextNotNull(contexts);

		boolean localTransaction = startLocalTransaction();
		try {
			for (Statement st : statements) {
				addWithoutCommit(st, contexts);
			}
			conditionalCommit(localTransaction);
		}
		catch (RepositoryException e) {
			conditionalRollback(localTransaction);
			throw e;
		}
		catch (RuntimeException e) {
			conditionalRollback(localTransaction);
			throw e;
		}
	}

	public <E extends Exception> void add(Iteration<? extends Statement, E> statements, Resource... contexts)
		throws RepositoryException, E
	{
		try {
			OpenRDFUtil.verifyContextNotNull(contexts);

			boolean localTransaction = startLocalTransaction();

			try {
				while (statements.hasNext()) {
					addWithoutCommit(statements.next(), contexts);
				}

				conditionalCommit(localTransaction);
			}
			catch (RepositoryException e) {
				conditionalRollback(localTransaction);
				throw e;
			}
			catch (RuntimeException e) {
				conditionalRollback(localTransaction);
				throw e;
			}
		}
		finally {
			Iterations.closeCloseable(statements);
		}
	}

	public void add(Statement st, Resource... contexts)
		throws RepositoryException
	{
		boolean localTransaction = startLocalTransaction();

		OpenRDFUtil.verifyContextNotNull(contexts);
		addWithoutCommit(st, contexts);

		conditionalCommit(localTransaction);
	}

	public void add(Resource subject, IRI predicate, Value object, Resource... contexts)
		throws RepositoryException
	{
		boolean localTransaction = startLocalTransaction();

		OpenRDFUtil.verifyContextNotNull(contexts);
		addWithoutCommit(subject, predicate, object, contexts);

		conditionalCommit(localTransaction);
	}

	public void remove(Iterable<? extends Statement> statements, Resource... contexts)
		throws RepositoryException
	{
		OpenRDFUtil.verifyContextNotNull(contexts);

		boolean localTransaction = startLocalTransaction();

		try {
			for (Statement st : statements) {
				remove(st, contexts);
			}

			conditionalCommit(localTransaction);
		}
		catch (RepositoryException e) {
			conditionalRollback(localTransaction);
			throw e;
		}
		catch (RuntimeException e) {
			conditionalRollback(localTransaction);
			throw e;
		}
	}

	public <E extends Exception> void remove(Iteration<? extends Statement, E> statements,
			Resource... contexts)
		throws RepositoryException, E
	{
		try {
			boolean localTransaction = startLocalTransaction();

			try {
				while (statements.hasNext()) {
					remove(statements.next(), contexts);
				}

				conditionalCommit(localTransaction);
			}
			catch (RepositoryException e) {
				conditionalRollback(localTransaction);
				throw e;
			}
			catch (RuntimeException e) {
				conditionalRollback(localTransaction);
				throw e;
			}
		}
		finally {
			Iterations.closeCloseable(statements);
		}
	}

	public void remove(Statement st, Resource... contexts)
		throws RepositoryException
	{
		boolean localTransaction = startLocalTransaction();

		OpenRDFUtil.verifyContextNotNull(contexts);
		removeWithoutCommit(st, contexts);

		conditionalCommit(localTransaction);
	}

	public void remove(Resource subject, IRI predicate, Value object, Resource... contexts)
		throws RepositoryException
	{
		boolean localTransaction = startLocalTransaction();

		OpenRDFUtil.verifyContextNotNull(contexts);
		removeWithoutCommit(subject, predicate, object, contexts);

		conditionalCommit(localTransaction);
	}

	public void clear(Resource... contexts)
		throws RepositoryException
	{
		remove(null, null, null, contexts);
	}

	protected void addWithoutCommit(Statement st, Resource... contexts)
		throws RepositoryException
	{
		if (contexts.length == 0 && st.getContext() != null) {
			contexts = new Resource[] { st.getContext() };
		}

		addWithoutCommit(st.getSubject(), st.getPredicate(), st.getObject(), contexts);
	}

	protected abstract void addWithoutCommit(Resource subject, IRI predicate, Value object,
			Resource... contexts)
		throws RepositoryException;

	protected void removeWithoutCommit(Statement st, Resource... contexts)
		throws RepositoryException
	{
		if (contexts.length == 0 && st.getContext() != null) {
			contexts = new Resource[] { st.getContext() };
		}

		removeWithoutCommit(st.getSubject(), st.getPredicate(), st.getObject(), contexts);
	}

	protected abstract void removeWithoutCommit(Resource subject, IRI predicate, Value object,
			Resource... contexts)
		throws RepositoryException;
}
