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
package org.openrdf.query.impl;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.openrdf.model.Value;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;

/**
 * An immutable empty BindingSet.
 * 
 * @author Arjohn Kampman
 */
public class EmptyBindingSet implements BindingSet {

	private static final long serialVersionUID = -6010968140688315954L;

	private static final EmptyBindingSet singleton = new EmptyBindingSet();

	public static BindingSet getInstance() {
		return singleton;
	}

	private EmptyBindingIterator iter = new EmptyBindingIterator();

	public Iterator<Binding> iterator() {
		return iter;
	}

	public Set<String> getBindingNames() {
		return Collections.emptySet();
	}

	public Binding getBinding(String bindingName) {
		return null;
	}

	public boolean hasBinding(String bindingName) {
		return false;
	}

	public Value getValue(String bindingName) {
		return null;
	}

	public int size() {
		return 0;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof BindingSet) {
			return ((BindingSet)o).size() == 0;
		}

		return false;
	}

	@Override
	public int hashCode() {
		return 0;
	}

	@Override
	public String toString() {
		return "[]";
	}

	/*----------------------------------*
	 * Inner class EmptyBindingIterator *
	 *----------------------------------*/

	private static class EmptyBindingIterator implements Iterator<Binding> {

		public boolean hasNext() {
			return false;
		}

		public Binding next() {
			throw new NoSuchElementException();
		}

		public void remove() {
			throw new IllegalStateException();
		}
	}
}
