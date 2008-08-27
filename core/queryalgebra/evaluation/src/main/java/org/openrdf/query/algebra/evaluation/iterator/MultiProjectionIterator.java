/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 1997-2006.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.query.algebra.evaluation.iterator;

import java.util.List;

import info.aduna.iteration.CloseableIteration;
import info.aduna.iteration.CloseableIterationBase;
import info.aduna.iteration.Iteration;

import org.openrdf.StoreException;
import org.openrdf.query.BindingSet;
import org.openrdf.query.algebra.MultiProjection;
import org.openrdf.query.algebra.ProjectionElemList;

public class MultiProjectionIterator extends CloseableIterationBase<BindingSet, StoreException> {

	/*-----------*
	 * Constants *
	 *-----------*/

	private final List<ProjectionElemList> projections;

	private final Iteration<BindingSet, StoreException> iter;

	private final BindingSet parentBindings;

	/*-----------*
	 * Variables *
	 *-----------*/

	private BindingSet currentBindings;

	private int nextProjectionIdx;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public MultiProjectionIterator(MultiProjection multiProjection,
			CloseableIteration<BindingSet, StoreException> iter, BindingSet bindings)
	{
		this.projections = multiProjection.getProjections();
		this.iter = iter;
		this.parentBindings = bindings;
	}

	/*---------*
	 * Methods *
	 *---------*/

	public boolean hasNext()
		throws StoreException
	{
		return currentBindings != null && nextProjectionIdx < projections.size() || iter.hasNext();
	}

	public BindingSet next()
		throws StoreException
	{
		if (currentBindings == null || nextProjectionIdx >= projections.size()) {
			currentBindings = iter.next();
			nextProjectionIdx = 0;
		}

		ProjectionElemList nextProjection = projections.get(nextProjectionIdx++);
		return ProjectionIterator.project(nextProjection, currentBindings, parentBindings);
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void handleClose()
		throws StoreException
	{
		nextProjectionIdx = projections.size();
		currentBindings = null;
		super.handleClose();
	}
}
