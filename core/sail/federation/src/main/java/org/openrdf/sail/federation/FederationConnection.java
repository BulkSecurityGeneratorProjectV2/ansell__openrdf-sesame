package org.openrdf.sail.federation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BindingSet;
import org.openrdf.query.Cursor;
import org.openrdf.query.algebra.QueryModel;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.evaluation.TripleSource;
import org.openrdf.query.algebra.evaluation.cursors.DistinctCursor;
import org.openrdf.query.algebra.evaluation.cursors.UnionCursor;
import org.openrdf.query.algebra.evaluation.impl.BindingAssigner;
import org.openrdf.query.algebra.evaluation.impl.CompareOptimizer;
import org.openrdf.query.algebra.evaluation.impl.ConjunctiveConstraintSplitter;
import org.openrdf.query.algebra.evaluation.impl.ConstantOptimizer;
import org.openrdf.query.algebra.evaluation.impl.DisjunctiveConstraintOptimizer;
import org.openrdf.query.algebra.evaluation.impl.EvaluationStrategyImpl;
import org.openrdf.query.algebra.evaluation.impl.FilterOptimizer;
import org.openrdf.query.algebra.evaluation.impl.QueryJoinOptimizer;
import org.openrdf.query.algebra.evaluation.impl.QueryModelPruner;
import org.openrdf.query.algebra.evaluation.impl.SameTermFilterOptimizer;
import org.openrdf.query.algebra.evaluation.util.QueryOptimizerList;
import org.openrdf.query.impl.EmptyBindingSet;
import org.openrdf.query.impl.IteratorCursor;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.sail.SailConnection;
import org.openrdf.store.StoreException;

abstract class FederationConnection implements SailConnection, TripleSource {

	private Logger logger = LoggerFactory.getLogger(FederationConnection.class);

	private Federation federation;

	private Collection<RepositoryConnection> members;

	private FederationStatistics statistics;

	public FederationConnection(Federation federation, Collection<RepositoryConnection> members) {
		this.federation = federation;
		this.members = members;
		this.statistics = new FederationStatistics(members);
	}

	public ValueFactory getValueFactory() {
		return federation.getValueFactory();
	}

	public void close()
		throws StoreException
	{
		excute(new Procedure() {

			public void run(RepositoryConnection member)
				throws StoreException
			{
				member.close();
			}
		});
	}

	public Cursor<? extends Resource> getContextIDs()
		throws StoreException
	{
		return union(new Function<Resource>() {

			public RepositoryResult<? extends Resource> call(RepositoryConnection member)
				throws StoreException
			{
				return member.getContextIDs();
			}
		});
	}

	public String getNamespace(String prefix)
		throws StoreException
	{
		String namespace = null;
		for (RepositoryConnection member : members) {
			String ns = member.getNamespace(prefix);
			if (namespace == null) {
				namespace = ns;
			}
			else if (ns != null && !ns.equals(namespace)) {
				return null;
			}
		}
		return namespace;
	}

	public Cursor<? extends Namespace> getNamespaces()
		throws StoreException
	{
		Map<String, Namespace> namespaces = new HashMap<String, Namespace>();
		Set<String> prefixes = new HashSet<String>();
		for (RepositoryConnection member : members) {
			RepositoryResult<Namespace> ns = member.getNamespaces();
			while (ns.hasNext()) {
				Namespace next = ns.next();
				String prefix = next.getPrefix();
				if (prefixes.add(prefix)) {
					namespaces.put(prefix, next);
				}
				else if (!next.equals(namespaces.get(prefix))) {
					namespaces.remove(prefix);
				}
			}
		}
		return new IteratorCursor<Namespace>(namespaces.values().iterator());
	}

	public long size(Resource subj, URI pred, Value obj, boolean includeInferred, Resource... contexts)
		throws StoreException
	{
		// TODO Does size have to be exact?
		Cursor<? extends Statement> cursor;
		cursor = getStatements(subj, pred, obj, includeInferred, contexts);
		long size = 0;
		while (cursor.next() != null) {
			size++;
		}
		return size;
	}

	public Cursor<? extends Statement> getStatements(final Resource subj, final URI pred, final Value obj,
			final boolean includeInferred, final Resource... contexts)
		throws StoreException
	{
		return getStatements(subj, pred, obj, contexts);
	}

	public Cursor<? extends Statement> getStatements(final Resource subj, final URI pred, final Value obj,
			final Resource... contexts)
		throws StoreException
	{
		return union(new Function<Statement>() {

			public RepositoryResult<? extends Statement> call(RepositoryConnection member)
				throws StoreException
			{
				return member.getStatements(subj, pred, obj, true, contexts);
			}
		});
	}

	public Cursor<? extends BindingSet> evaluate(QueryModel query, BindingSet bindings,
			boolean includeInferred)
		throws StoreException
	{
		EvaluationStrategyImpl strategy;
		strategy = new EvaluationStrategyImpl(this, query);
		TupleExpr qry = optimize(query, bindings, strategy);
		return strategy.evaluate(qry, EmptyBindingSet.getInstance());
	}

	interface Procedure {

		void run(RepositoryConnection member)
			throws StoreException;
	}

	void excute(Procedure operation)
		throws StoreException
	{
		StoreException store = null;
		RuntimeException runtime = null;
		for (RepositoryConnection member : members) {
			try {
				operation.run(member);
			}
			catch (StoreException e) {
				logger.error(e.toString(), e);
				if (store != null) {
					store = e;
				}
			}
			catch (RuntimeException e) {
				logger.error(e.toString(), e);
				if (runtime != null) {
					runtime = e;
				}

			}
		}
		if (store != null)
			throw store;
		if (runtime != null)
			throw runtime;
	}

	private interface Function<E> {

		public abstract RepositoryResult<? extends E> call(RepositoryConnection member)
			throws StoreException;
	}

	private class ResultCursor<E> implements Cursor<E> {

		private RepositoryResult<? extends E> result;

		public ResultCursor(RepositoryResult<? extends E> result) {
			this.result = result;
		}

		public E next()
			throws StoreException
		{
			if (result.hasNext())
				return result.next();
			return null;
		}

		public void close()
			throws StoreException
		{
			result.close();
		}

		public String toString() {
			return result.toString();
		}
	}

	private <E> Cursor<? extends E> union(Function<E> converter)
		throws StoreException
	{
		List<Cursor<? extends E>> cursors = new ArrayList<Cursor<? extends E>>(members.size());
		try {
			for (RepositoryConnection member : members) {
				cursors.add(new ResultCursor<E>(converter.call(member)));
			}
			return new DistinctCursor<E>(new UnionCursor<E>(cursors));
		}
		catch (StoreException e) {
			closeAll(cursors);
			throw e;
		}
		catch (RuntimeException e) {
			closeAll(cursors);
			throw e;
		}
	}

	private <E> void closeAll(Iterable<? extends Cursor<? extends E>> cursors) {
		for (Cursor<? extends E> cursor : cursors) {
			try {
				cursor.close();
			}
			catch (StoreException e) {
				logger.error(e.toString(), e);
			}
		}
	}

	private QueryModel optimize(QueryModel query, BindingSet bindings,
			EvaluationStrategyImpl strategy)
		throws StoreException
	{
		logger.trace("Incoming query model:\n{}", query.toString());

		// Clone the tuple expression to allow for more aggresive optimizations
		query = query.clone();

		QueryOptimizerList optimizerList = new QueryOptimizerList();
		optimizerList.add(new BindingAssigner());
		optimizerList.add(new ConstantOptimizer(strategy));
		optimizerList.add(new CompareOptimizer());
		optimizerList.add(new ConjunctiveConstraintSplitter());
		optimizerList.add(new DisjunctiveConstraintOptimizer());
		optimizerList.add(new SameTermFilterOptimizer());
		optimizerList.add(new QueryModelPruner());
		optimizerList.add(new QueryJoinOptimizer(statistics));
		optimizerList.add(new FilterOptimizer());

		optimizerList.optimize(query, bindings);

		logger.trace("Optimized query model:\n{}", query.toString());
		return query;
	}

}
