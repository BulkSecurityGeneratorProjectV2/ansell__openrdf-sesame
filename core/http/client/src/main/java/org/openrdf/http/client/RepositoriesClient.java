/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2002-2008.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.http.client;

import org.openrdf.http.client.connections.HTTPConnectionPool;
import org.openrdf.http.client.helpers.StoreClient;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.TupleQueryResultHandler;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.store.StoreException;

/**
 * @author Herko ter Horst
 * @author Arjohn Kampman
 * @author James Leigh
 */
public class RepositoriesClient {

	private HTTPConnectionPool repositories;
	private StoreClient client;

	public RepositoriesClient(HTTPConnectionPool repositroies) {
		this.repositories = repositroies;
		this.client = new StoreClient(repositories);
	}

	public TupleQueryResult get()
		throws StoreException
	{
		return client.list();
	}

	public void get(TupleQueryResultHandler handler)
		throws TupleQueryResultHandlerException, StoreException
	{
		client.list(handler);
	}

	public RepositoryClient slash(String id) {
		return new RepositoryClient(repositories.slash(id));
	}

}
