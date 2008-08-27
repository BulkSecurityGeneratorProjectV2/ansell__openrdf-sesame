/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 1997-2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.sail.nativerdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import info.aduna.concurrent.locks.Lock;
import info.aduna.iteration.CloseableIteration;
import info.aduna.iteration.ExceptionConvertingIteration;
import info.aduna.iteration.Iterations;
import info.aduna.iteration.IteratorIteration;
import info.aduna.iteration.LockingIteration;

import org.openrdf.OpenRDFUtil;
import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.NamespaceImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.algebra.QueryRoot;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.Var;
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
import org.openrdf.query.algebra.helpers.QueryModelVisitorBase;
import org.openrdf.query.impl.EmptyBindingSet;
import org.openrdf.StoreException;
import org.openrdf.sail.helpers.DefaultSailChangedEvent;
import org.openrdf.sail.helpers.SailConnectionBase;
import org.openrdf.sail.inferencer.InferencerConnection;
import org.openrdf.sail.nativerdf.btree.RecordIterator;
import org.openrdf.sail.nativerdf.model.NativeValue;

/**
 * @author Arjohn Kampman
 */
public class NativeStoreConnection extends SailConnectionBase implements InferencerConnection {

	/*-----------*
	 * Constants *
	 *-----------*/

	protected final NativeStore nativeStore;

	/*-----------*
	 * Variables *
	 *-----------*/

	private DefaultSailChangedEvent sailChangedEvent;

	/**
	 * The exclusive transaction lock held by this connection during
	 * transactions.
	 */
	private Lock txnLock;

	/*--------------*
	 * Constructors *
	 *--------------*/

	protected NativeStoreConnection(NativeStore nativeStore)
		throws IOException
	{
		super(nativeStore);
		this.nativeStore = nativeStore;
		sailChangedEvent = new DefaultSailChangedEvent(nativeStore);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected void closeInternal() {
		// FIXME we should check for open iteration objects.
	}

	@Override
	protected CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluateInternal(
			TupleExpr tupleExpr, Dataset dataset, BindingSet bindings, boolean includeInferred)
		throws StoreException
	{
		logger.trace("Incoming query model:\n{}", tupleExpr.toString());

		// Clone the tuple expression to allow for more aggressive optimizations
		tupleExpr = tupleExpr.clone();

		if (!(tupleExpr instanceof QueryRoot)) {
			// Add a dummy root node to the tuple expressions to allow the
			// optimizers to modify the actual root node
			tupleExpr = new QueryRoot(tupleExpr);
		}

		Lock readLock = nativeStore.getReadLock();

		try {
			replaceValues(tupleExpr);

			NativeTripleSource tripleSource = new NativeTripleSource(nativeStore, includeInferred,
					transactionActive());
			EvaluationStrategyImpl strategy = new EvaluationStrategyImpl(tripleSource, dataset);

			QueryOptimizerList optimizerList = new QueryOptimizerList();
			optimizerList.add(new BindingAssigner());
			optimizerList.add(new ConstantOptimizer(strategy));
			optimizerList.add(new CompareOptimizer());
			optimizerList.add(new ConjunctiveConstraintSplitter());
			optimizerList.add(new DisjunctiveConstraintOptimizer());
			optimizerList.add(new SameTermFilterOptimizer());
			optimizerList.add(new QueryModelPruner());
			optimizerList.add(new QueryJoinOptimizer(new NativeEvaluationStatistics(nativeStore)));
			optimizerList.add(new FilterOptimizer());

			optimizerList.optimize(tupleExpr, dataset, bindings);
			logger.trace("Optimized query model:\n{}", tupleExpr.toString());

			CloseableIteration<BindingSet, QueryEvaluationException> iter;
			iter = strategy.evaluate(tupleExpr, EmptyBindingSet.getInstance());
			return new LockingIteration<BindingSet, QueryEvaluationException>(readLock, iter);
		}
		catch (QueryEvaluationException e) {
			readLock.release();
			throw new StoreException(e);
		}
		catch (RuntimeException e) {
			readLock.release();
			throw e;
		}
	}

	protected void replaceValues(TupleExpr tupleExpr)
		throws StoreException
	{
		// Replace all Value objects stored in variables with NativeValue objects,
		// which cache internal IDs
		tupleExpr.visit(new QueryModelVisitorBase<StoreException>() {

			@Override
			public void meet(Var var) {
				if (var.hasValue()) {
					var.setValue(nativeStore.getValueStore().getNativeValue(var.getValue()));
				}
			}
		});
	}

	@Override
	protected CloseableIteration<? extends Resource, StoreException> getContextIDsInternal()
		throws StoreException
	{
		// Which resources are used as context identifiers is not stored
		// separately. Iterate over all statements and extract their context.
		Lock readLock = nativeStore.getReadLock();
		try {
			CloseableIteration<? extends Resource, IOException> contextIter;
			contextIter = nativeStore.getContextIDs(transactionActive());
			// releasing the read lock when the iterator is closed
			contextIter = new LockingIteration<Resource, IOException>(readLock, contextIter);

			return new ExceptionConvertingIteration<Resource, StoreException>(contextIter) {

				@Override
				protected StoreException convert(Exception e) {
					if (e instanceof IOException) {
						return new StoreException(e);
					}
					else if (e instanceof RuntimeException) {
						throw (RuntimeException)e;
					}
					else if (e == null) {
						throw new IllegalArgumentException("e must not be null");
					}
					else {
						throw new IllegalArgumentException("Unexpected exception type: " + e.getClass());
					}
				}
			};
		}
		catch (IOException e) {
			readLock.release();
			throw new StoreException(e);
		}
		catch (RuntimeException e) {
			readLock.release();
			throw e;
		}
	}

	@Override
	protected CloseableIteration<? extends Statement, StoreException> getStatementsInternal(Resource subj,
			URI pred, Value obj, boolean includeInferred, Resource... contexts)
		throws StoreException
	{
		Lock readLock = nativeStore.getReadLock();
		try {
			CloseableIteration<? extends Statement, IOException> iter;
			iter = nativeStore.createStatementIterator(subj, pred, obj, includeInferred, transactionActive(),
					contexts);
			iter = new LockingIteration<Statement, IOException>(readLock, iter);

			return new ExceptionConvertingIteration<Statement, StoreException>(iter) {

				@Override
				protected StoreException convert(Exception e) {
					if (e instanceof IOException) {
						return new StoreException(e);
					}
					else if (e instanceof RuntimeException) {
						throw (RuntimeException)e;
					}
					else if (e == null) {
						throw new IllegalArgumentException("e must not be null");
					}
					else {
						throw new IllegalArgumentException("Unexpected exception type: " + e.getClass());
					}
				}
			};
		}
		catch (IOException e) {
			readLock.release();
			throw new StoreException("Unable to get statements", e);
		}
		catch (RuntimeException e) {
			readLock.release();
			throw e;
		}
	}

	@Override
	protected long sizeInternal(Resource... contexts)
		throws StoreException
	{
		OpenRDFUtil.verifyContextNotNull(contexts);

		Lock readLock = nativeStore.getReadLock();

		try {
			List<Integer> contextIDs;
			if (contexts.length == 0) {
				contextIDs = Arrays.asList(NativeValue.UNKNOWN_ID);
			}
			else {
				contextIDs = nativeStore.getContextIDs(contexts);
			}

			long size = 0L;

			for (int contextID : contextIDs) {
				// Iterate over all explicit statements
				RecordIterator iter = nativeStore.getTripleStore().getTriples(-1, -1, -1, contextID, true,
						transactionActive());
				try {
					while (iter.next() != null) {
						size++;
					}
				}
				finally {
					iter.close();
				}
			}

			return size;
		}
		catch (IOException e) {
			throw new StoreException(e);
		}
		finally {
			readLock.release();
		}
	}

	@Override
	protected CloseableIteration<? extends Namespace, StoreException> getNamespacesInternal()
		throws StoreException
	{
		Lock readLock = nativeStore.getReadLock();
		try {
			return new LockingIteration<NamespaceImpl, StoreException>(
					readLock,
					new IteratorIteration<NamespaceImpl, StoreException>(nativeStore.getNamespaceStore().iterator()));
		}
		catch (RuntimeException e) {
			readLock.release();
			throw e;
		}
	}

	@Override
	protected String getNamespaceInternal(String prefix)
		throws StoreException
	{
		Lock readLock = nativeStore.getReadLock();
		try {
			return nativeStore.getNamespaceStore().getNamespace(prefix);
		}
		finally {
			readLock.release();
		}
	}

	@Override
	protected void startTransactionInternal()
		throws StoreException
	{
		txnLock = nativeStore.getTransactionLock();

		try {
			nativeStore.getTripleStore().startTransaction();
		}
		catch (IOException e) {
			throw new StoreException(e);
		}
	}

	@Override
	protected void commitInternal()
		throws StoreException
	{
		Lock storeReadLock = nativeStore.getReadLock();

		try {
			nativeStore.getValueStore().sync();
			nativeStore.getTripleStore().commit();
			nativeStore.getNamespaceStore().sync();

			txnLock.release();
		}
		catch (IOException e) {
			throw new StoreException(e);
		}
		finally {
			storeReadLock.release();
		}

		nativeStore.notifySailChanged(sailChangedEvent);

		// create a fresh event object.
		sailChangedEvent = new DefaultSailChangedEvent(nativeStore);
	}

	@Override
	protected void rollbackInternal()
		throws StoreException
	{
		Lock storeReadLock = nativeStore.getReadLock();

		try {
			nativeStore.getValueStore().sync();
			nativeStore.getTripleStore().rollback();
		}
		catch (IOException e) {
			throw new StoreException(e);
		}
		finally {
			txnLock.release();
			storeReadLock.release();
		}
	}

	@Override
	protected void addStatementInternal(Resource subj, URI pred, Value obj, Resource... contexts)
		throws StoreException
	{
		addStatement(subj, pred, obj, true, contexts);
	}

	public boolean addInferredStatement(Resource subj, URI pred, Value obj, Resource... contexts)
		throws StoreException
	{
		Lock conLock = getSharedConnectionLock();
		try {
			verifyIsOpen();

			Lock txnLock = getTransactionLock();
			try {
				autoStartTransaction();
				return addStatement(subj, pred, obj, false, contexts);
			}
			finally {
				txnLock.release();
			}
		}
		finally {
			conLock.release();
		}
	}

	private boolean addStatement(Resource subj, URI pred, Value obj, boolean explicit, Resource... contexts)
		throws StoreException
	{
		OpenRDFUtil.verifyContextNotNull(contexts);

		boolean result = false;

		try {
			ValueStore valueStore = nativeStore.getValueStore();
			int subjID = valueStore.storeValue(subj);
			int predID = valueStore.storeValue(pred);
			int objID = valueStore.storeValue(obj);

			if (contexts.length == 0) {
				contexts = new Resource[] { null };
			}

			for (Resource context : contexts) {
				int contextID = 0;
				if (context != null) {
					contextID = valueStore.storeValue(context);
				}

				boolean wasNew = nativeStore.getTripleStore().storeTriple(subjID, predID, objID, contextID,
						explicit);
				result |= wasNew;

				if (wasNew) {
					// The triple was not yet present in the triple store
					sailChangedEvent.setStatementsAdded(true);

					if (hasConnectionListeners()) {
						Statement st;

						if (context != null) {
							st = valueStore.createStatement(subj, pred, obj, context);
						}
						else {
							st = valueStore.createStatement(subj, pred, obj);
						}

						notifyStatementAdded(st);
					}
				}
			}
		}
		catch (IOException e) {
			throw new StoreException(e);
		}

		return result;
	}

	@Override
	protected void removeStatementsInternal(Resource subj, URI pred, Value obj, Resource... contexts)
		throws StoreException
	{
		removeStatements(subj, pred, obj, true, contexts);
	}

	public boolean removeInferredStatement(Resource subj, URI pred, Value obj, Resource... contexts)
		throws StoreException
	{
		Lock conLock = getSharedConnectionLock();
		try {
			verifyIsOpen();

			Lock txnLock = getTransactionLock();
			try {
				autoStartTransaction();
				int removeCount = removeStatements(subj, pred, obj, false, contexts);
				return removeCount > 0;
			}
			finally {
				txnLock.release();
			}
		}
		finally {
			conLock.release();
		}
	}

	private int removeStatements(Resource subj, URI pred, Value obj, boolean explicit, Resource... contexts)
		throws StoreException
	{
		OpenRDFUtil.verifyContextNotNull(contexts);

		try {
			TripleStore tripleStore = nativeStore.getTripleStore();
			ValueStore valueStore = nativeStore.getValueStore();

			int subjID = NativeValue.UNKNOWN_ID;
			if (subj != null) {
				subjID = valueStore.getID(subj);
				if (subjID == NativeValue.UNKNOWN_ID) {
					return 0;
				}
			}
			int predID = NativeValue.UNKNOWN_ID;
			if (pred != null) {
				predID = valueStore.getID(pred);
				if (predID == NativeValue.UNKNOWN_ID) {
					return 0;
				}
			}
			int objID = NativeValue.UNKNOWN_ID;
			if (obj != null) {
				objID = valueStore.getID(obj);
				if (objID == NativeValue.UNKNOWN_ID) {
					return 0;
				}
			}

			List<Integer> contextIDList = new ArrayList<Integer>(contexts.length);
			if (contexts.length == 0) {
				contextIDList.add(NativeValue.UNKNOWN_ID);
			}
			else {
				for (Resource context : contexts) {
					if (context == null) {
						contextIDList.add(0);
					}
					else {
						int contextID = valueStore.getID(context);
						if (contextID != NativeValue.UNKNOWN_ID) {
							contextIDList.add(contextID);
						}
					}
				}
			}

			int removeCount = 0;

			for (int i = 0; i < contextIDList.size(); i++) {
				int contextID = contextIDList.get(i);

				List<Statement> removedStatements = Collections.emptyList();

				if (hasConnectionListeners()) {
					// We need to iterate over all matching triples so that they can
					// be reported
					RecordIterator btreeIter = tripleStore.getTriples(subjID, predID, objID, contextID, explicit,
							true);

					NativeStatementIterator iter = new NativeStatementIterator(btreeIter, valueStore);

					removedStatements = Iterations.asList(iter);
				}

				removeCount += tripleStore.removeTriples(subjID, predID, objID, contextID, explicit);

				for (Statement st : removedStatements) {
					notifyStatementRemoved(st);
				}
			}

			if (removeCount > 0) {
				sailChangedEvent.setStatementsRemoved(true);
			}

			return removeCount;
		}
		catch (IOException e) {
			throw new StoreException(e);
		}
	}

	@Override
	protected void clearInternal(Resource... contexts)
		throws StoreException
	{
		removeStatements(null, null, null, true, contexts);
	}

	public void clearInferred(Resource... contexts)
		throws StoreException
	{
		Lock conLock = getSharedConnectionLock();
		try {
			verifyIsOpen();

			Lock txnLock = getTransactionLock();
			try {
				autoStartTransaction();
				removeStatements(null, null, null, false, contexts);
			}
			finally {
				txnLock.release();
			}
		}
		finally {
			conLock.release();
		}
	}

	public void flushUpdates() {
		// no-op; changes are reported as soon as they come in
	}

	@Override
	protected void setNamespaceInternal(String prefix, String name)
		throws StoreException
	{
		nativeStore.getNamespaceStore().setNamespace(prefix, name);
	}

	@Override
	protected void removeNamespaceInternal(String prefix)
		throws StoreException
	{
		nativeStore.getNamespaceStore().removeNamespace(prefix);
	}

	@Override
	protected void clearNamespacesInternal()
		throws StoreException
	{
		nativeStore.getNamespaceStore().clear();
	}
}