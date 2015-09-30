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
package org.openrdf.sail.federation;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openrdf.IsolationLevel;
import org.openrdf.IsolationLevels;
import org.openrdf.http.client.HttpClientDependent;
import org.openrdf.http.client.SesameClient;
import org.openrdf.http.client.SesameClientDependent;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.SimpleValueFactory;
import org.openrdf.query.Dataset;
import org.openrdf.query.algebra.evaluation.EvaluationStrategy;
import org.openrdf.query.algebra.evaluation.TripleSource;
import org.openrdf.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.openrdf.query.algebra.evaluation.federation.FederatedServiceResolverClient;
import org.openrdf.query.algebra.evaluation.federation.FederatedServiceResolverImpl;
import org.openrdf.query.algebra.evaluation.impl.SimpleEvaluationStrategy;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.config.RepositoryResolver;
import org.openrdf.repository.sail.config.RepositoryResolverClient;
import org.openrdf.sail.Sail;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailException;
import org.openrdf.sail.federation.evaluation.FederationStrategy;

/**
 * Union multiple (possibly remote) Repositories into a single RDF store.
 * 
 * @author James Leigh
 * @author Arjohn Kampman
 */
public class Federation implements Sail, Executor, FederatedServiceResolverClient, RepositoryResolverClient,
		HttpClientDependent, SesameClientDependent
{

	private static final Logger LOGGER = LoggerFactory.getLogger(Federation.class);

	private final List<Repository> members = new ArrayList<Repository>();

	private final ExecutorService executor = Executors.newCachedThreadPool();

	private PrefixHashSet localPropertySpace; // NOPMD

	private boolean distinct;

	private boolean readOnly;

	private File dataDir;

	/** independent life cycle */
	private FederatedServiceResolver serviceResolver;

	/** dependent life cycle */
	private FederatedServiceResolverImpl dependentServiceResolver;

	public File getDataDir() {
		return dataDir;
	}

	public void setDataDir(File dataDir) {
		this.dataDir = dataDir;
	}

	public ValueFactory getValueFactory() {
		return SimpleValueFactory.getInstance();
	}

	public boolean isWritable()
		throws SailException
	{
		return !isReadOnly();
	}

	public void addMember(Repository member) {
		members.add(member);
	}

	/**
	 * @return PrefixHashSet or null
	 */
	public PrefixHashSet getLocalPropertySpace() {
		return localPropertySpace;
	}

	public void setLocalPropertySpace(Collection<String> localPropertySpace) { // NOPMD
		if (localPropertySpace.isEmpty()) {
			this.localPropertySpace = null; // NOPMD
		}
		else {
			this.localPropertySpace = new PrefixHashSet(localPropertySpace);
		}
	}

	public boolean isDistinct() {
		return distinct;
	}

	public void setDistinct(boolean distinct) {
		this.distinct = distinct;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	/**
	 * @return Returns the SERVICE resolver.
	 */
	public synchronized FederatedServiceResolver getFederatedServiceResolver() {
		if (serviceResolver == null) {
			if (dependentServiceResolver == null) {
				dependentServiceResolver = new FederatedServiceResolverImpl();
			}
			return serviceResolver = dependentServiceResolver;
		}
		return serviceResolver;
	}

	/**
	 * Overrides the {@link FederatedServiceResolver} used by this instance, but
	 * the given resolver is not shutDown when this instance is.
	 * 
	 * @param reslover
	 *        The SERVICE resolver to set.
	 */
	public synchronized void setFederatedServiceResolver(FederatedServiceResolver resolver) {
		this.serviceResolver = resolver;
		for (Repository member : members) {
			if (member instanceof FederatedServiceResolverClient) {
				((FederatedServiceResolverClient) member).setFederatedServiceResolver(resolver);
			}
		}
	}

	@Override
	public void setRepositoryResolver(RepositoryResolver resolver) {
		for (Repository member : members) {
			if (member instanceof RepositoryResolverClient) {
				((RepositoryResolverClient) member).setRepositoryResolver(resolver);
			}
		}
	}

	@Override
	public SesameClient getSesameClient() {
		for (Repository member : members) {
			if (member instanceof SesameClientDependent) {
				SesameClient client = ((SesameClientDependent) member).getSesameClient();
				if (client != null) {
					return client;
				}
			}
		}
		return null;
	}

	@Override
	public void setSesameClient(SesameClient client) {
		for (Repository member : members) {
			if (member instanceof SesameClientDependent) {
				((SesameClientDependent) member).setSesameClient(client);
			}
		}
	}

	@Override
	public HttpClient getHttpClient() {
		for (Repository member : members) {
			if (member instanceof HttpClientDependent) {
				HttpClient client = ((HttpClientDependent) member).getHttpClient();
				if (client != null) {
					return client;
				}
			}
		}
		return null;
	}

	@Override
	public void setHttpClient(HttpClient client) {
		for (Repository member : members) {
			if (member instanceof HttpClientDependent) {
				((HttpClientDependent) member).setHttpClient(client);
			}
		}
	}

	@Override
	public void initialize()
		throws SailException
	{
		for (Repository member : members) {
			try {
				member.initialize();
			}
			catch (RepositoryException e) {
				throw new SailException(e);
			}
		}
	}

	public void shutDown()
		throws SailException
	{
		for (Repository member : members) {
			try {
				member.shutDown();
			}
			catch (RepositoryException e) {
				throw new SailException(e);
			}
		}
		executor.shutdown();
		if (dependentServiceResolver != null) {
			dependentServiceResolver.shutDown();
		}
	}

	/**
	 * Required by {@link java.util.concurrent.Executor Executor} interface.
	 */
	public void execute(Runnable command) {
		executor.execute(command);
	}

	public SailConnection getConnection()
		throws SailException
	{
		List<RepositoryConnection> connections = new ArrayList<RepositoryConnection>(members.size());
		try {
			for (Repository member : members) {
				connections.add(member.getConnection());
			}
			return readOnly ? new ReadOnlyConnection(this, connections) : new WritableConnection(this,
					connections);
		}
		catch (RepositoryException e) {
			closeAll(connections);
			throw new SailException(e);
		}
		catch (RuntimeException e) {
			closeAll(connections);
			throw e;
		}
	}

	protected EvaluationStrategy createEvaluationStrategy(TripleSource tripleSource, Dataset dataset, FederatedServiceResolver resolver) {
		return new FederationStrategy(this, tripleSource, dataset, getFederatedServiceResolver());
	}

	private void closeAll(Iterable<RepositoryConnection> connections) {
		for (RepositoryConnection con : connections) {
			try {
				con.close();
			}
			catch (RepositoryException e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
	}

	@Override
	public List<IsolationLevel> getSupportedIsolationLevels() {
		return Arrays.asList(new IsolationLevel[] { IsolationLevels.NONE });
	}

	@Override
	public IsolationLevel getDefaultIsolationLevel() {
		return IsolationLevels.NONE;
	}
}
