/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 1997-2007.
 * Copyright James Leigh (c) 2006.
 *
 * Licensed under the Aduna BSD-style license.
 */

package org.openrdf.sail.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import info.aduna.concurrent.locks.Lock;
import info.aduna.iteration.CloseableIteration;
import info.aduna.iteration.CloseableIteratorIteration;
import info.aduna.iteration.LockingIteration;

import org.openrdf.StoreException;
import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.algebra.QueryRoot;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.algebra.evaluation.TripleSource;
import org.openrdf.query.algebra.evaluation.impl.BindingAssigner;
import org.openrdf.query.algebra.evaluation.impl.CompareOptimizer;
import org.openrdf.query.algebra.evaluation.impl.ConjunctiveConstraintSplitter;
import org.openrdf.query.algebra.evaluation.impl.ConstantOptimizer;
import org.openrdf.query.algebra.evaluation.impl.DisjunctiveConstraintOptimizer;
import org.openrdf.query.algebra.evaluation.impl.EvaluationStatistics;
import org.openrdf.query.algebra.evaluation.impl.EvaluationStrategyImpl;
import org.openrdf.query.algebra.evaluation.impl.FilterOptimizer;
import org.openrdf.query.algebra.evaluation.impl.QueryJoinOptimizer;
import org.openrdf.query.algebra.evaluation.impl.QueryModelPruner;
import org.openrdf.query.algebra.evaluation.impl.SameTermFilterOptimizer;
import org.openrdf.query.algebra.evaluation.util.QueryOptimizerList;
import org.openrdf.query.impl.EmptyBindingSet;
import org.openrdf.sail.SailReadOnlyException;
import org.openrdf.sail.helpers.NotifyingSailConnectionBase;
import org.openrdf.sail.inferencer.InferencerConnection;
import org.openrdf.sail.memory.model.MemResource;
import org.openrdf.sail.memory.model.MemStatement;
import org.openrdf.sail.memory.model.MemStatementIterator;
import org.openrdf.sail.memory.model.MemStatementList;
import org.openrdf.sail.memory.model.MemURI;
import org.openrdf.sail.memory.model.MemValue;
import org.openrdf.sail.memory.model.MemValueFactory;
import org.openrdf.sail.memory.model.ReadMode;

/**
 * Implementation of a Sail Connection for memory stores.
 * 
 * @author Arjohn Kampman
 * @author jeen
 */
public class MemoryStoreConnection extends NotifyingSailConnectionBase implements InferencerConnection {

	/*-----------*
	 * Variables *
	 *-----------*/

	protected final MemoryStore store;

	/**
	 * The exclusive transaction lock held by this connection during
	 * transactions.
	 */
	private Lock txnLock;

	/**
	 * A statement list read lock held by this connection during transactions.
	 * Keeping this lock prevents statements from being removed from the main
	 * statement list during transactions.
	 */
	private Lock txnStLock;

	/*--------------*
	 * Constructors *
	 *--------------*/

	protected MemoryStoreConnection(MemoryStore store) {
		this.store = store;
	}

	/*---------*
	 * Methods *
	 *---------*/

	public CloseableIteration<? extends BindingSet, StoreException> evaluate(
			TupleExpr tupleExpr, Dataset dataset, BindingSet bindings, boolean includeInferred)
		throws StoreException
	{
		logger.trace("Incoming query model:\n{}", tupleExpr.toString());

		// Clone the tuple expression to allow for more aggresive optimizations
		tupleExpr = tupleExpr.clone();

		if (!(tupleExpr instanceof QueryRoot)) {
			// Add a dummy root node to the tuple expressions to allow the
			// optimizers to modify the actual root node
			tupleExpr = new QueryRoot(tupleExpr);
		}

		Lock stLock = store.getStatementsReadLock();

		try {
			int snapshot = store.getCurrentSnapshot();
			ReadMode readMode = ReadMode.COMMITTED;

			if (transactionActive()) {
				snapshot++;
				readMode = ReadMode.TRANSACTION;
			}

			TripleSource tripleSource = new MemTripleSource(includeInferred, snapshot, readMode);
			EvaluationStrategyImpl strategy = new EvaluationStrategyImpl(tripleSource, dataset);

			QueryOptimizerList optimizerList = new QueryOptimizerList();
			optimizerList.add(new BindingAssigner());
			optimizerList.add(new ConstantOptimizer(strategy));
			optimizerList.add(new CompareOptimizer());
			optimizerList.add(new ConjunctiveConstraintSplitter());
			optimizerList.add(new DisjunctiveConstraintOptimizer());
			optimizerList.add(new SameTermFilterOptimizer());
			optimizerList.add(new QueryModelPruner());
			optimizerList.add(new QueryJoinOptimizer(new MemEvaluationStatistics()));
			optimizerList.add(new FilterOptimizer());

			optimizerList.optimize(tupleExpr, dataset, bindings);

			logger.trace("Optimized query model:\n{}", tupleExpr.toString());

			CloseableIteration<BindingSet, StoreException> iter;
			iter = strategy.evaluate(tupleExpr, EmptyBindingSet.getInstance());
			return new LockingIteration<BindingSet, StoreException>(stLock, iter);
		}
		catch (StoreException e) {
			stLock.release();
			throw e;
		}
		catch (RuntimeException e) {
			stLock.release();
			throw e;
		}
	}

	public CloseableIteration<? extends Resource, StoreException> getContextIDs()
		throws StoreException
	{
		// Note: we can't do this in a streaming fashion due to concurrency
		// issues; iterating over the set of URIs or bnodes while another thread
		// adds statements with new resources would result in
		// ConcurrentModificationException's (issue SES-544).

		// Create a list of all resources that are used as contexts
		ArrayList<MemResource> contextIDs = new ArrayList<MemResource>(32);

		Lock stLock = store.getStatementsReadLock();

		try {
			final int snapshot = transactionActive() ? store.getCurrentSnapshot() + 1
					: store.getCurrentSnapshot();
			final ReadMode readMode = transactionActive() ? ReadMode.TRANSACTION : ReadMode.COMMITTED;

			MemValueFactory valueFactory = store.getValueFactory();

			synchronized (valueFactory) {
				for (MemResource memResource : valueFactory.getMemURIs()) {
					if (isContextResource(memResource, snapshot, readMode)) {
						contextIDs.add(memResource);
					}
				}

				for (MemResource memResource : valueFactory.getMemBNodes()) {
					if (isContextResource(memResource, snapshot, readMode)) {
						contextIDs.add(memResource);
					}
				}
			}
		}
		finally {
			stLock.release();
		}

		return new CloseableIteratorIteration<MemResource, StoreException>(contextIDs.iterator());
	}

	private boolean isContextResource(MemResource memResource, int snapshot, ReadMode readMode)
		throws StoreException
	{
		MemStatementList contextStatements = memResource.getContextStatementList();

		// Filter resources that are not used as context identifier
		if (contextStatements.size() == 0) {
			return false;
		}

		// Filter more thoroughly by considering snapshot and read-mode parameters
		MemStatementIterator<StoreException> iter = new MemStatementIterator<StoreException>(contextStatements,
				null, null, null, false, snapshot, readMode);
		try {
			return iter.hasNext();
		}
		finally {
			iter.close();
		}
	}

	public CloseableIteration<? extends Statement, StoreException> getStatements(Resource subj,
			URI pred, Value obj, boolean includeInferred, Resource... contexts)
		throws StoreException
	{
		Lock stLock = store.getStatementsReadLock();

		try {
			int snapshot = store.getCurrentSnapshot();
			ReadMode readMode = ReadMode.COMMITTED;

			if (transactionActive()) {
				snapshot++;
				readMode = ReadMode.TRANSACTION;
			}

			return new LockingIteration<MemStatement, StoreException>(stLock, store.createStatementIterator(
					StoreException.class, subj, pred, obj, !includeInferred, snapshot, readMode, contexts));
		}
		catch (RuntimeException e) {
			stLock.release();
			throw e;
		}
	}

	public long size(Resource... contexts)
		throws StoreException
	{
		Lock stLock = store.getStatementsReadLock();

		try {
			CloseableIteration<? extends Statement, StoreException> iter = getStatements(null, null,
					null, false, contexts);

			try {
				long size = 0L;

				while (iter.hasNext()) {
					iter.next();
					size++;
				}

				return size;
			}
			finally {
				iter.close();
			}
		}
		finally {
			stLock.release();
		}
	}

	public CloseableIteration<? extends Namespace, StoreException> getNamespaces()
		throws StoreException
	{
		return new CloseableIteratorIteration<Namespace, StoreException>(store.getNamespaceStore().iterator());
	}

	public String getNamespace(String prefix)
		throws StoreException
	{
		return store.getNamespaceStore().getNamespace(prefix);
	}

	@Override
	public void begin()
		throws StoreException
	{
		if (!store.isWritable()) {
			throw new SailReadOnlyException("Unable to start transaction: data file is locked or read-only");
		}

		txnStLock = store.getStatementsReadLock();

		// Prevent concurrent transactions by acquiring an exclusive txn lock
		txnLock = store.getTransactionLock();
		store.startTransaction();
		super.begin();
	}

	@Override
	public void commit()
		throws StoreException
	{
		store.commit();
		txnLock.release();
		txnStLock.release();
		super.commit();
	}

	@Override
	public void rollback()
		throws StoreException
	{
		try {
			store.rollback();
			super.rollback();
		}
		finally {
			txnLock.release();
			txnStLock.release();
		}
	}

	public void addStatement(Resource subj, URI pred, Value obj, Resource... contexts)
		throws StoreException
	{
		addStatementInternal(subj, pred, obj, true, contexts);
	}

	public boolean addInferredStatement(Resource subj, URI pred, Value obj, Resource... contexts)
		throws StoreException
	{
		return addStatementInternal(subj, pred, obj, false, contexts);
	}

	/**
	 * Adds the specified statement to this MemoryStore.
	 * 
	 * @throws StoreException
	 */
	private boolean addStatementInternal(Resource subj, URI pred, Value obj, boolean explicit,
			Resource... contexts)
		throws StoreException
	{
		Statement st = null;

		if (contexts.length == 0) {
			st = store.addStatement(subj, pred, obj, null, explicit);
			if (st != null) {
				notifyStatementAdded(st);
			}
		}
		else {
			for (Resource context : contexts) {
				st = store.addStatement(subj, pred, obj, context, explicit);
				if (st != null) {
					notifyStatementAdded(st);
				}
			}
		}

		// FIXME: this return type is invalid in case multiple contexts were
		// specified
		return st != null;
	}

	public void removeStatements(Resource subj, URI pred, Value obj, Resource... contexts)
		throws StoreException
	{
		removeStatementsInternal(subj, pred, obj, true, contexts);
	}

	public boolean removeInferredStatement(Resource subj, URI pred, Value obj, Resource... contexts)
		throws StoreException
	{
		return removeStatementsInternal(subj, pred, obj, false, contexts);
	}

	public void clear(Resource... contexts)
		throws StoreException
	{
		removeStatementsInternal(null, null, null, true, contexts);
	}

	public void clearInferred(Resource... contexts)
		throws StoreException
	{
		removeStatementsInternal(null, null, null, false, contexts);
	}

	public void flushUpdates() {
		// no-op; changes are reported as soon as they come in
	}

	/**
	 * Removes the statements that match the specified pattern of subject,
	 * predicate, object and context.
	 * 
	 * @param subj
	 *        The subject for the pattern, or <tt>null</tt> for a wildcard.
	 * @param pred
	 *        The predicate for the pattern, or <tt>null</tt> for a wildcard.
	 * @param obj
	 *        The object for the pattern, or <tt>null</tt> for a wildcard.
	 * @param explicit
	 *        Flag indicating whether explicit or inferred statements should be
	 *        removed; <tt>true</tt> removes explicit statements that match the
	 *        pattern, <tt>false</tt> removes inferred statements that match the
	 *        pattern.
	 * @throws StoreException
	 */
	private boolean removeStatementsInternal(Resource subj, URI pred, Value obj, boolean explicit,
			Resource... contexts)
		throws StoreException
	{
		CloseableIteration<MemStatement, StoreException> stIter = store.createStatementIterator(
				StoreException.class, subj, pred, obj, explicit, store.getCurrentSnapshot() + 1,
				ReadMode.TRANSACTION, contexts);

		return removeIteratorStatements(stIter, explicit);
	}

	protected boolean removeIteratorStatements(CloseableIteration<MemStatement, StoreException> stIter,
			boolean explicit)
		throws StoreException
	{
		boolean statementsRemoved = false;

		try {
			while (stIter.hasNext()) {
				MemStatement st = stIter.next();

				if (store.removeStatement(st, explicit)) {
					statementsRemoved = true;
					notifyStatementRemoved(st);
				}
			}
		}
		finally {
			stIter.close();
		}

		return statementsRemoved;

	}

	public void setNamespace(String prefix, String name)
		throws StoreException
	{
		// FIXME: changes to namespace prefixes not isolated in transactions yet
		try {
			store.getNamespaceStore().setNamespace(prefix, name);
		}
		catch (IllegalArgumentException e) {
			throw new StoreException(e.getMessage());
		}
	}

	public void removeNamespace(String prefix)
		throws StoreException
	{
		// FIXME: changes to namespace prefixes not isolated in transactions yet
		store.getNamespaceStore().removeNamespace(prefix);
	}

	public void clearNamespaces()
		throws StoreException
	{
		// FIXME: changes to namespace prefixes not isolated in transactions yet
		store.getNamespaceStore().clear();
	}

	/*-----------------------------*
	 * Inner class MemTripleSource *
	 *-----------------------------*/

	/**
	 * Implementation of the TripleSource interface from the Sail Query Model
	 */
	protected class MemTripleSource implements TripleSource {

		protected final int snapshot;

		protected final ReadMode readMode;

		protected final boolean includeInferred;

		public MemTripleSource(boolean includeInferred, int snapshot, ReadMode readMode) {
			this.includeInferred = includeInferred;
			this.snapshot = snapshot;
			this.readMode = readMode;
		}

		public CloseableIteration<MemStatement, StoreException> getStatements(Resource subj,
				URI pred, Value obj, Resource... contexts)
		{
			return store.createStatementIterator(StoreException.class, subj, pred, obj,
					!includeInferred, snapshot, readMode, contexts);
		}

		public MemValueFactory getValueFactory() {
			return store.getValueFactory();
		}
	} // end inner class MemTripleSource

	/*-------------------------------------*
	 * Inner class MemEvaluationStatistics *
	 *-------------------------------------*/

	/**
	 * Uses the MemoryStore's statement sizes to give cost estimates based on the
	 * size of the expected results. This process could be improved with
	 * repository statistics about size and distribution of statements.
	 * 
	 * @author Arjohn Kampman
	 * @author James Leigh
	 */
	protected class MemEvaluationStatistics extends EvaluationStatistics {

		@Override
		protected CardinalityCalculator createCardinalityCalculator() {
			return new MemCardinalityCalculator();
		}

		protected class MemCardinalityCalculator extends CardinalityCalculator {

			@Override
			public double getCardinality(StatementPattern sp) {
				Resource subj = (Resource)getConstantValue(sp.getSubjectVar());
				URI pred = (URI)getConstantValue(sp.getPredicateVar());
				Value obj = getConstantValue(sp.getObjectVar());
				Resource context = (Resource)getConstantValue(sp.getContextVar());

				MemValueFactory valueFactory = store.getValueFactory();

				// Perform look-ups for value-equivalents of the specified values
				MemResource memSubj = valueFactory.getMemResource(subj);
				MemURI memPred = valueFactory.getMemURI(pred);
				MemValue memObj = valueFactory.getMemValue(obj);
				MemResource memContext = valueFactory.getMemResource(context);

				if (subj != null && memSubj == null || pred != null && memPred == null || obj != null
						&& memObj == null || context != null && memContext == null)
				{
					// non-existent subject, predicate, object or context
					return 0.0;
				}

				// Search for the smallest list that can be used by the iterator
				List<Integer> listSizes = new ArrayList<Integer>(4);
				if (memSubj != null) {
					listSizes.add(memSubj.getSubjectStatementCount());
				}
				if (memPred != null) {
					listSizes.add(memPred.getPredicateStatementCount());
				}
				if (memObj != null) {
					listSizes.add(memObj.getObjectStatementCount());
				}
				if (memContext != null) {
					listSizes.add(memContext.getContextStatementCount());
				}

				double cardinality;

				if (listSizes.isEmpty()) {
					// all wildcards
					cardinality = store.size();
				}
				else {
					cardinality = Collections.min(listSizes);

					// List<Var> vars = getVariables(sp);
					// int constantVarCount = countConstantVars(vars);
					//
					// // Subtract 1 from var count as this was used for the list
					// size
					// double unboundVarFactor = (double)(vars.size() -
					// constantVarCount) / (vars.size() - 1);
					//
					// cardinality = Math.pow(cardinality, unboundVarFactor);
				}

				return cardinality;
			}

			protected Value getConstantValue(Var var) {
				if (var != null) {
					return var.getValue();
				}

				return null;
			}
		}
	} // end inner class MemCardinalityCalculator
}
