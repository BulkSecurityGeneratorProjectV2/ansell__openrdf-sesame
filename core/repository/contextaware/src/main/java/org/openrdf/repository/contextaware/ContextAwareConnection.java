/*
 * Copyright James Leigh (c) 2007-2008.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.repository.contextaware;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Method;
import java.net.URL;

import info.aduna.iteration.CloseableIteration;
import info.aduna.iteration.Iteration;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.Query;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.impl.DatasetImpl;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.base.RepositoryConnectionWrapper;
import org.openrdf.repository.util.RDFInserter;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;

/**
 * Allows contexts to be specified at the connection level or the method level.
 * 
 * @author James Leigh
 */
public class ContextAwareConnection extends RepositoryConnectionWrapper {

	private static final URI[] ALL_CONTEXTS = new URI[0];

	private boolean includeInferred = true;

	private long maxQueryTime;

	private QueryLanguage ql = QueryLanguage.SPARQL;

	private URI[] readContexts = ALL_CONTEXTS;

	private URI[] addContexts = ALL_CONTEXTS;

	private URI[] removeContexts = ALL_CONTEXTS;

	private URI[] archiveContexts = ALL_CONTEXTS;

	public ContextAwareConnection(Repository repository)
		throws RepositoryException
	{
		this(repository, repository.getConnection());
	}

	public ContextAwareConnection(Repository repository, RepositoryConnection connection) {
		super(repository, connection);
	}

	@Override
	protected boolean isDelegatingRemove() {
		return archiveContexts.length == 0;
	}

	/**
	 * if false, no inferred statements are considered; if true, inferred
	 * statements are considered if available
	 */
	public boolean isIncludeInferred() {
		return includeInferred;
	}

	/**
	 * if false, no inferred statements are considered; if true, inferred
	 * statements are considered if available
	 */
	public void setIncludeInferred(boolean includeInferred) {
		this.includeInferred = includeInferred;
	}

	public long getMaxQueryTime() {
		return maxQueryTime;
	}

	public void setMaxQueryTime(long maxQueryTime) {
		this.maxQueryTime = maxQueryTime;
	}

	public QueryLanguage getQueryLanguage() {
		return ql;
	}

	public void setQueryLanguage(QueryLanguage ql) {
		this.ql = ql;
	}

	/**
	 * The context(s) to get the data from. Note that this parameter is a vararg
	 * and as such is optional. If no contexts are supplied the method operates
	 * on the entire repository.
	 */
	public URI[] getReadContexts() {
		return readContexts;
	}

	/**
	 * The context(s) to get the data from. Note that this parameter is a vararg
	 * and as such is optional. If no contexts are supplied the method operates
	 * on the entire repository.
	 */
	public void setReadContexts(URI... readContexts) {
		this.readContexts = readContexts;
	}

	/**
	 * The contexts to add the statements to. Note that this parameter is a
	 * vararg and as such is optional. If no contexts are specified, each
	 * statement is added to any context specified in the statement, or if the
	 * statement contains no context, it is added without a context. If one or
	 * more contexts are specified each statement is added to these contexts,
	 * ignoring any context information in the statement itself.
	 */
	public URI[] getAddContexts() {
		return addContexts;
	}

	/**
	 * The contexts to add the statements to. Note that this parameter is a
	 * vararg and as such is optional. If no contexts are specified, each
	 * statement is added to any context specified in the statement, or if the
	 * statement contains no context, it is added without a context. If one or
	 * more contexts are specified each statement is added to these contexts,
	 * ignoring any context information in the statement itself.
	 */
	public void setAddContexts(URI... addContexts) {
		this.addContexts = addContexts;
	}

	/**
	 * The context(s) to remove the data from. Note that this parameter is a
	 * vararg and as such is optional. If no contexts are supplied the method
	 * operates on the contexts associated with the statement itself, and if no
	 * context is associated with the statement, on the entire repository.
	 */
	public URI[] getRemoveContexts() {
		return removeContexts;
	}

	/**
	 * The context(s) to remove the data from. Note that this parameter is a
	 * vararg and as such is optional. If no contexts are supplied the method
	 * operates on the contexts associated with the statement itself, and if no
	 * context is associated with the statement, on the entire repository.
	 */
	public void setRemoveContexts(URI... removeContexts) {
		this.removeContexts = removeContexts;
	}

	/**
	 * Before Statements are removed, they are first copied to these contexts.
	 */
	public URI[] getArchiveContexts() {
		return archiveContexts;
	}

	/**
	 * Before Statements are removed, they are first copied to these contexts.
	 */
	public void setArchiveContexts(URI... archiveContexts) {
		this.archiveContexts = archiveContexts;
	}

	@Override
	public void add(File file, String baseURI, RDFFormat dataFormat, Resource... contexts)
		throws IOException, RDFParseException, RepositoryException
	{
		if (contexts == null || contexts.length < 1) {
			super.add(file, baseURI, dataFormat, addContexts);
		}
		else {
			super.add(file, baseURI, dataFormat, contexts);
		}
	}

	@Override
	public void add(InputStream in, String baseURI, RDFFormat dataFormat, Resource... contexts)
		throws IOException, RDFParseException, RepositoryException
	{
		if (contexts == null || contexts.length < 1) {
			super.add(in, baseURI, dataFormat, addContexts);
		}
		else {
			super.add(in, baseURI, dataFormat, contexts);
		}
	}

	@Override
	public void add(Iterable<? extends Statement> statements, Resource... contexts)
		throws RepositoryException
	{
		if (contexts == null || contexts.length < 1) {
			super.add(statements, addContexts);
		}
		else {
			super.add(statements, contexts);
		}
	}

	@Override
	public <E extends Exception> void add(Iteration<? extends Statement, E> statementIter,
			Resource... contexts)
		throws RepositoryException, E
	{
		if (contexts == null || contexts.length < 1) {
			super.add(statementIter, addContexts);
		}
		else {
			super.add(statementIter, contexts);
		}
	}

	@Override
	public void add(Reader reader, String baseURI, RDFFormat dataFormat, Resource... contexts)
		throws IOException, RDFParseException, RepositoryException
	{
		if (contexts == null || contexts.length < 1) {
			super.add(reader, baseURI, dataFormat, addContexts);
		}
		else {
			super.add(reader, baseURI, dataFormat, contexts);
		}
	}

	@Override
	public void add(Resource subject, URI predicate, Value object, Resource... contexts)
		throws RepositoryException
	{
		if (contexts == null || contexts.length < 1) {
			super.add(subject, predicate, object, addContexts);
		}
		else {
			super.add(subject, predicate, object, contexts);
		}
	}

	@Override
	public void add(Statement st, Resource... contexts)
		throws RepositoryException
	{
		if (contexts == null || contexts.length < 1) {
			super.add(st, addContexts);
		}
		else {
			super.add(st, contexts);
		}
	}

	@Override
	public void add(URL url, String baseURI, RDFFormat dataFormat, Resource... contexts)
		throws IOException, RDFParseException, RepositoryException
	{
		if (contexts == null || contexts.length < 1) {
			super.add(url, baseURI, dataFormat, addContexts);
		}
		else {
			super.add(url, baseURI, dataFormat, contexts);
		}
	}

	@Override
	public void clear(Resource... contexts)
		throws RepositoryException
	{
		if (contexts == null || contexts.length < 1) {
			super.clear(removeContexts);
		}
		else {
			super.clear(contexts);
		}
	}

	@Override
	public void export(RDFHandler handler, Resource... contexts)
		throws RepositoryException, RDFHandlerException
	{
		if (contexts == null || contexts.length < 1) {
			super.export(handler, readContexts);
		}
		else {
			super.export(handler, contexts);
		}
	}

	/**
	 * Exports all statements with a specific subject, predicate and/or object
	 * from the repository, optionally from the specified contexts.
	 * 
	 * @param subj
	 *        The subject, or null if the subject doesn't matter.
	 * @param pred
	 *        The predicate, or null if the predicate doesn't matter.
	 * @param obj
	 *        The object, or null if the object doesn't matter.
	 * @param handler
	 *        The handler that will handle the RDF data.
	 * @throws RDFHandlerException
	 *         If the handler encounters an unrecoverable error.
	 * @see #getReadContexts()
	 * @see #isIncludeInferred()
	 */
	public void exportStatements(Resource subj, URI pred, Value obj, RDFHandler hander, Resource... contexts)
		throws RepositoryException, RDFHandlerException
	{
		if (contexts == null || contexts.length < 1) {
			super.exportStatements(subj, pred, obj, includeInferred, hander, readContexts);
		}
		else {
			super.exportStatements(subj, pred, obj, includeInferred, hander, contexts);
		}
	}

	/**
	 * Gets all statements with a specific subject, predicate and/or object from
	 * the repository. The result is optionally restricted to the specified set
	 * of named contexts.
	 * 
	 * @param subj
	 *        A Resource specifying the subject, or <tt>null</tt> for a wildcard.
	 * @param pred
	 *        A URI specifying the predicate, or <tt>null</tt> for a wildcard.
	 * @param obj
	 *        A Value specifying the object, or <tt>null</tt> for a wildcard.
	 * @return The statements matching the specified pattern. The result object
	 *         is a {@link RepositoryResult} object, a lazy Iterator-like object
	 *         containing {@link Statement}s and optionally throwing a
	 *         {@link RepositoryException} when an error when a problem occurs
	 *         during retrieval.
	 * @see #getReadContexts()
	 * @see #isIncludeInferred()
	 */
	public RepositoryResult<Statement> getStatements(Resource subj, URI pred, Value obj, Resource... contexts)
		throws RepositoryException
	{
		if (contexts == null || contexts.length < 1) {
			return super.getStatements(subj, pred, obj, includeInferred, readContexts);
		}
		else {
			return super.getStatements(subj, pred, obj, includeInferred, contexts);
		}
	}

	/**
	 * Checks whether the repository contains statements with a specific subject,
	 * predicate and/or object, optionally in the specified contexts.
	 * 
	 * @param subj
	 *        A Resource specifying the subject, or <tt>null</tt> for a wildcard.
	 * @param pred
	 *        A URI specifying the predicate, or <tt>null</tt> for a wildcard.
	 * @param obj
	 *        A Value specifying the object, or <tt>null</tt> for a wildcard.
	 * @return true If a matching statement is in the repository in the specified
	 *         context, false otherwise.
	 * @see #getReadContexts()
	 * @see #isIncludeInferred()
	 */
	public boolean hasStatement(Resource subj, URI pred, Value obj, Resource... contexts)
		throws RepositoryException
	{
		if (contexts == null || contexts.length < 1) {
			return super.hasStatement(subj, pred, obj, includeInferred, readContexts);
		}
		else {
			return super.hasStatement(subj, pred, obj, includeInferred, contexts);
		}
	}

	/**
	 * Checks whether the repository contains the specified statement, optionally
	 * in the specified contexts.
	 * 
	 * @param st
	 *        The statement to look for. Context information in the statement is
	 *        ignored.
	 * @return true If the repository contains the specified statement, false
	 *         otherwise.
	 * @see #getReadContexts()
	 * @see #isIncludeInferred()
	 */
	public boolean hasStatement(Statement st, Resource... contexts)
		throws RepositoryException
	{
		if (contexts == null || contexts.length < 1) {
			return super.hasStatement(st, includeInferred, readContexts);
		}
		else {
			return super.hasStatement(st, includeInferred, contexts);
		}
	}

	public GraphQuery prepareGraphQuery(String query)
		throws MalformedQueryException, RepositoryException
	{
		return initQuery(super.prepareGraphQuery(ql, query));
	}

	public Query prepareQuery(String query)
		throws MalformedQueryException, RepositoryException
	{
		return initQuery(super.prepareQuery(ql, query));
	}

	public TupleQuery prepareTupleQuery(String query)
		throws MalformedQueryException, RepositoryException
	{
		return initQuery(super.prepareTupleQuery(ql, query));
	}

	@Override
	public GraphQuery prepareGraphQuery(QueryLanguage ql, String query)
		throws MalformedQueryException, RepositoryException
	{
		return initQuery(super.prepareGraphQuery(ql, query));
	}

	@Override
	public Query prepareQuery(QueryLanguage ql, String query)
		throws MalformedQueryException, RepositoryException
	{
		return initQuery(super.prepareQuery(ql, query));
	}

	@Override
	public TupleQuery prepareTupleQuery(QueryLanguage ql, String query)
		throws MalformedQueryException, RepositoryException
	{
		return initQuery(super.prepareTupleQuery(ql, query));
	}

	@Override
	public BooleanQuery prepareBooleanQuery(QueryLanguage ql, String query)
		throws MalformedQueryException, RepositoryException
	{
		return initQuery(super.prepareBooleanQuery(ql, query));
	}

	@Override
	public GraphQuery prepareGraphQuery(QueryLanguage ql, String query, String baseURI)
		throws MalformedQueryException, RepositoryException
	{
		return initQuery(super.prepareGraphQuery(ql, query, baseURI));
	}

	@Override
	public Query prepareQuery(QueryLanguage ql, String query, String baseURI)
		throws MalformedQueryException, RepositoryException
	{
		return initQuery(super.prepareQuery(ql, query, baseURI));
	}

	@Override
	public TupleQuery prepareTupleQuery(QueryLanguage ql, String query, String baseURI)
		throws MalformedQueryException, RepositoryException
	{
		return initQuery(super.prepareTupleQuery(ql, query, baseURI));
	}

	@Override
	public BooleanQuery prepareBooleanQuery(QueryLanguage ql, String query, String baseURI)
		throws MalformedQueryException, RepositoryException
	{
		return initQuery(super.prepareBooleanQuery(ql, query, baseURI));
	}

	@Override
	public void remove(Iterable<? extends Statement> statements, Resource... contexts)
		throws RepositoryException
	{
		if (contexts == null || contexts.length < 1) {
			super.remove(statements, removeContexts);
		}
		else {
			super.remove(statements, contexts);
		}
	}

	/**
	 * Removes the supplied statements from a specific context in this
	 * repository, ignoring any context information carried by the statements
	 * themselves.
	 * 
	 * @param statementIter
	 *        The statements to remove. In case the iterator is a
	 *        {@link CloseableIteration}, it will be closed before this method
	 *        returns.
	 * @throws RepositoryException
	 *         If the statements could not be removed from the repository, for
	 *         example because the repository is not writable.
	 * @see #getRemoveContexts()
	 */
	@Override
	public <E extends Exception> void remove(Iteration<? extends Statement, E> statementIter,
			Resource... contexts)
		throws RepositoryException, E
	{
		if (contexts == null || contexts.length < 1) {
			super.remove(statementIter, removeContexts);
		}
		else {
			super.remove(statementIter, contexts);
		}
	}

	/**
	 * Removes the statement with the specified subject, predicate and object
	 * from the repository, optionally restricted to the specified contexts.
	 * 
	 * @param subject
	 *        The statement's subject.
	 * @param predicate
	 *        The statement's predicate.
	 * @param object
	 *        The statement's object.
	 * @throws RepositoryException
	 *         If the statement could not be removed from the repository, for
	 *         example because the repository is not writable.
	 * @see #getRemoveContexts()
	 */
	@Override
	public void remove(Resource subject, URI predicate, Value object, Resource... contexts)
		throws RepositoryException
	{
		if (contexts == null || contexts.length < 1) {
			super.remove(subject, predicate, object, removeContexts);
		}
		else {
			super.remove(subject, predicate, object, contexts);
		}
	}

	/**
	 * Removes the supplied statement from the specified contexts in the
	 * repository.
	 * 
	 * @param st
	 *        The statement to remove.
	 * @throws RepositoryException
	 *         If the statement could not be removed from the repository, for
	 *         example because the repository is not writable.
	 * @see #getRemoveContexts()
	 */
	@Override
	public void remove(Statement st, Resource... contexts)
		throws RepositoryException
	{
		if (contexts == null || contexts.length < 1) {
			super.remove(st, removeContexts);
		}
		else {
			super.remove(st, contexts);
		}
	}

	/**
	 * Returns the number of (explicit) statements that are in the specified
	 * contexts in this repository.
	 * 
	 * @return The number of explicit statements from the specified contexts in
	 *         this repository.
	 * @see #getReadContexts()
	 */
	@Override
	public long size(Resource... contexts)
		throws RepositoryException
	{
		if (contexts == null || contexts.length < 1) {
			return super.size(readContexts);
		}
		else {
			return super.size(contexts);
		}
	}

	@Override
	protected void removeWithoutCommit(Resource subject, URI predicate, Value object, Resource... contexts)
		throws RepositoryException
	{
		RDFHandler handler = new RDFInserter(getDelegate());
		try {
			getDelegate().exportStatements(subject, predicate, object, true, handler, archiveContexts);
		}
		catch (RDFHandlerException e) {
			if (e.getCause() instanceof RepositoryException) {
				throw (RepositoryException)e.getCause();
			}
			throw new AssertionError(e);
		}
		getDelegate().remove(subject, predicate, object, contexts);
	}

	private <Q extends Query> Q initQuery(Q query) {
		if (readContexts.length > 0) {
			DatasetImpl ds = new DatasetImpl();
			for (URI graph : readContexts) {
				ds.addDefaultGraph(graph);
			}
			query.setDataset(ds);
		}

		query.setIncludeInferred(includeInferred);
		// TODO preparedQuery.setMaxQueryTime(maxQueryTime);
		try {
			Class<? extends Query> c = query.getClass();
			Method setMaxQueryTime = c.getMethod("setMaxQueryTime", Long.TYPE);
			setMaxQueryTime.invoke(query, maxQueryTime);
		}
		catch (Exception e) {
			// TODO remove this reflection
		}
		return query;
	}

}
