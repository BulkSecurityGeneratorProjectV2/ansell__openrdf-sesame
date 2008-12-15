/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 1997-2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.query.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.openrdf.query.BindingSet;
import org.openrdf.query.Cursor;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.base.CursorWrapper;
import org.openrdf.store.StoreException;

/**
 * A generic implementation of the {@link TupleQueryResult} interface.
 */
public class TupleQueryResultImpl extends CursorWrapper<BindingSet> implements TupleQueryResult {

	/*-----------*
	 * Variables *
	 *-----------*/

	private List<String> bindingNames;

	private BindingSet next;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a query result object with the supplied binding names.
	 * <em>The supplied list of binding names is assumed to be constant</em>;
	 * care should be taken that the contents of this list doesn't change after
	 * supplying it to this solution.
	 * 
	 * @param bindingNames
	 *        The binding names, in order of projection.
	 */
	public TupleQueryResultImpl(List<String> bindingNames, Iterable<? extends BindingSet> bindingSets) {
		this(bindingNames, bindingSets.iterator());
	}

	public TupleQueryResultImpl(List<String> bindingNames, Iterator<? extends BindingSet> bindingSetIter) {
		this(bindingNames, new IteratorCursor<BindingSet>(bindingSetIter));
	}

	/**
	 * Creates a query result object with the supplied binding names.
	 * <em>The supplied list of binding names is assumed to be constant</em>;
	 * care should be taken that the contents of this list doesn't change after
	 * supplying it to this solution.
	 * 
	 * @param bindingNames
	 *        The binding names, in order of projection.
	 */
	public TupleQueryResultImpl(List<String> bindingNames, Cursor<? extends BindingSet> bindingSetIter) {
		super(bindingSetIter);
		// Don't allow modifications to the binding names when it is accessed
		// through getBindingNames:
		this.bindingNames = Collections.unmodifiableList(bindingNames);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public List<String> getBindingNames() {
		return bindingNames;
	}

	@Override
	public BindingSet next()
		throws StoreException
	{
		BindingSet result = next;
		if (result == null)
			return super.next();
		next = null;
		return result;
	}

	public boolean hasNext()
		throws StoreException
	{
		return next != null || (next = next()) != null;
	}

	public <C extends Collection<? super BindingSet>> C addTo(C collection)
		throws StoreException
	{
		BindingSet bindings;
		while ((bindings = next()) != null) {
			collection.add(bindings);
		}
		return collection;
	}

	public List<BindingSet> asList()
		throws StoreException
	{
		return addTo(new ArrayList<BindingSet>());
	}

	public Set<BindingSet> asSet()
		throws StoreException
	{
		return addTo(new HashSet<BindingSet>());
	}
}
