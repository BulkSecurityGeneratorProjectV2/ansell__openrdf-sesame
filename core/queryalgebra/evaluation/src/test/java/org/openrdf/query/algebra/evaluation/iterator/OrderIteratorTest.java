/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 1997-2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.query.algebra.evaluation.iterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import info.aduna.iteration.CloseableIteratorIteration;

import org.openrdf.StoreException;
import org.openrdf.model.Value;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;


/**
 * 
 * @author james
 * 
 */
public class OrderIteratorTest extends TestCase {
	class IterationStub extends
			CloseableIteratorIteration<BindingSet, StoreException> {
		int hasNextCount = 0;

		int nextCount = 0;

		int removeCount = 0;

		@Override
		public void setIterator(Iterator<? extends BindingSet> iter) {
			super.setIterator(iter);
		}

		@Override
		public boolean hasNext() {
			hasNextCount++;
			return super.hasNext();
		}

		@Override
		public BindingSet next() {
			nextCount++;
			return super.next();
		}

		@Override
		public void remove() {
			removeCount++;
		}
	}

	class SizeComparator implements Comparator<BindingSet> {
		public int compare(BindingSet o1, BindingSet o2) {
			return Integer.valueOf(o1.size()).compareTo(
					Integer.valueOf(o2.size()));
		}
	}

	class BindingSetSize implements BindingSet {
		private int size;

		public BindingSetSize(int size) {
			super();
			this.size = size;
		}

		public Binding getBinding(String bindingName) {
			throw new UnsupportedOperationException();
		}

		public Set<String> getBindingNames() {
			throw new UnsupportedOperationException();
		}

		public Value getValue(String bindingName) {
			throw new UnsupportedOperationException();
		}

		public boolean hasBinding(String bindingName) {
			throw new UnsupportedOperationException();
		}

		public Iterator<Binding> iterator() {
			throw new UnsupportedOperationException();
		}

		public int size() {
			return size;
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "#" + size;
		}
	}

	private IterationStub iteration;

	private OrderIterator order;

	private List<BindingSet> list;

	private BindingSet b1 = new BindingSetSize(1);

	private BindingSet b2 = new BindingSetSize(2);

	private BindingSet b3 = new BindingSetSize(3);

	private BindingSet b4 = new BindingSetSize(4);

	private BindingSet b5 = new BindingSetSize(5);

	private SizeComparator cmp;

	public void testFirstHasNext() throws Exception {
		order.hasNext();
		assertEquals(list.size() + 1, iteration.hasNextCount);
		assertEquals(list.size(), iteration.nextCount);
		assertEquals(0, iteration.removeCount);
	}

	public void testHasNext() throws Exception {
		order.hasNext();
		order.next();
		order.hasNext();
		assertEquals(list.size() + 1, iteration.hasNextCount);
		assertEquals(list.size(), iteration.nextCount);
		assertEquals(0, iteration.removeCount);
	}

	public void testFirstNext() throws Exception {
		order.next();
		assertEquals(list.size() + 1, iteration.hasNextCount);
		assertEquals(list.size(), iteration.nextCount);
		assertEquals(0, iteration.removeCount);
	}

	public void testNext() throws Exception {
		order.next();
		order.next();
		assertEquals(list.size() + 1, iteration.hasNextCount);
		assertEquals(list.size(), iteration.nextCount);
		assertEquals(0, iteration.removeCount);
	}

	public void testRemove() throws Exception {
		try {
			order.remove();
			fail();
		} catch (UnsupportedOperationException e) {
		}

	}

	public void testSorting() throws Exception {
		List<BindingSet> sorted = new ArrayList<BindingSet>(list);
		Collections.sort(sorted, cmp);
		for (BindingSet b : sorted) {
			assertEquals(b, order.next());
		}
		assertFalse(order.hasNext());
	}

	@Override
	protected void setUp() throws Exception {
		list = Arrays.asList(b3, b5, b2, b1, b4, b2);
		cmp = new SizeComparator();
		iteration = new IterationStub();
		iteration.setIterator(list.iterator());
		order = new OrderIterator(iteration, cmp);
	}

}
