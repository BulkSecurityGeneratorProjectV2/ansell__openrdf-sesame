/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2008.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.query.base;

import org.openrdf.StoreException;
import org.openrdf.query.Cursor;

/**
 * A Cursor that wraps another Cursor, applying a filter on the objects that are
 * returned. Subclasses must implement the <tt>accept</tt> method to indicate
 * which objects should be returned.
 * 
 * @author James Leigh
 */
public abstract class FilteringCursor<E> extends CursorWrapper<E> {

	public FilteringCursor(Cursor<? extends E> delegate) {
		super(delegate);
	}

	public E next()
		throws StoreException
	{
		E next = super.next();
		if (next == null)
			return null;
		if (accept(next))
			return next;
		return next();
	}

	protected abstract boolean accept(E next)
		throws StoreException;

}
