/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.repository.http;

import java.io.IOException;

import org.openrdf.http.client.HTTPClient;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;

/**
 * GraphQuery implementation specific to the HTTP protocol. Methods in this
 * class may throw the specific RepositoryException subclass
 * UnautorizedException, the semantics of which is defined by the HTTP protocol.
 * 
 * @see org.openrdf.http.protocol.UnauthorizedException
 * @author Arjohn Kampman
 * @author Herko ter Horst
 */
public class HTTPGraphQuery extends HTTPQuery implements GraphQuery {

	public HTTPGraphQuery(HTTPRepositoryConnection con, QueryLanguage ql, String queryString, String baseURI) {
		super(con, ql, queryString, baseURI);
	}

	public GraphQueryResult evaluate()
			throws QueryEvaluationException
		{
			HTTPClient client = httpCon.getRepository().getHTTPClient();

			HTTPGraphQueryResult result = new HTTPGraphQueryResult(client, queryLanguage, queryString, baseURI,
					dataset, includeInferred, getBindingsArray());
			execute(result);
			return result;
		}
	
	/*
	public GraphQueryResult evaluate()
		throws QueryEvaluationException
	{
		HTTPClient client = httpCon.getRepository().getHTTPClient();

		
		try {
			return client.sendGraphQuery(queryLanguage, queryString, baseURI, dataset, includeInferred, maxQueryTime,
					getBindingsArray());
		}
		catch (IOException e) {
			throw new HTTPQueryEvaluationException(e.getMessage(), e);
		}
		catch (RepositoryException e) {
			throw new HTTPQueryEvaluationException(e.getMessage(), e);
		}
		catch (MalformedQueryException e) {
			throw new HTTPQueryEvaluationException(e.getMessage(), e);
		}
	}
	*/

	public void evaluate(RDFHandler handler)
		throws QueryEvaluationException, RDFHandlerException
	{
		HTTPClient client = httpCon.getRepository().getHTTPClient();
		try {
			client.sendGraphQuery(queryLanguage, queryString, baseURI, dataset, includeInferred, maxQueryTime, handler,
					getBindingsArray());
		}
		catch (IOException e) {
			throw new HTTPQueryEvaluationException(e.getMessage(), e);
		}
		catch (RepositoryException e) {
			throw new HTTPQueryEvaluationException(e.getMessage(), e);
		}
		catch (MalformedQueryException e) {
			throw new HTTPQueryEvaluationException(e.getMessage(), e);
		}
	}
}
