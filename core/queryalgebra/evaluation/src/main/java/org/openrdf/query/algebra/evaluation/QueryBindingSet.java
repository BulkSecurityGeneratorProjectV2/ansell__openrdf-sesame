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
package org.openrdf.query.algebra.evaluation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.Value;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.impl.SimpleBinding;
import org.openrdf.query.impl.MapBindingSet;
import org.openrdf.util.iterators.ConvertingIterator;

/**
 * An implementation of the {@link BindingSet} interface that is used to evalate
 * query object models. This implementations differs from {@link MapBindingSet}
 * in that it maps variable names to Value objects and that the Binding objects
 * are created lazily.
 */
public class QueryBindingSet implements BindingSet {

	private static final long serialVersionUID = -2010715346095527301L;

	private final Map<String, Value> bindings;

	public QueryBindingSet() {
		this(8);
	}

	public QueryBindingSet(int capacity) {
		// Create bindings map with some extra space for new bindings and
		// compensating for HashMap's load factor
		bindings = new HashMap<String, Value>(capacity * 2);
	}

	public QueryBindingSet(BindingSet bindingSet) {
		this(bindingSet.size());
		addAll(bindingSet);
	}

	public void addAll(BindingSet bindingSet) {
		if (bindingSet instanceof QueryBindingSet) {
			bindings.putAll(((QueryBindingSet)bindingSet).bindings);
		}
		else {
			for (Binding binding : bindingSet) {
				this.addBinding(binding);
			}
		}
	}

	/**
	 * Adds a new binding to the binding set. The binding's name must not already
	 * be part of this binding set.
	 * 
	 * @param binding
	 *        The binding to add this this BindingSet.
	 */
	public void addBinding(Binding binding) {
		addBinding(binding.getName(), binding.getValue());
	}

	/**
	 * Adds a new binding to the binding set. The binding's name must not already
	 * be part of this binding set.
	 * 
	 * @param name
	 *        The binding's name, must not be bound in this binding set already.
	 * @param value
	 *        The binding's value.
	 */
	public void addBinding(String name, Value value) {
		assert !bindings.containsKey(name) : "variable already bound: " + name;
		setBinding(name, value);
	}

	public void setBinding(Binding binding) {
		setBinding(binding.getName(), binding.getValue());
	}

	public void setBinding(String name, Value value) {
		assert value != null : "null value for variable " + name;
		bindings.put(name, value);
	}

	public void removeBinding(String name) {
		bindings.remove(name);
	}

	public void removeAll(Collection<String> bindingNames) {
		bindings.keySet().removeAll(bindingNames);
	}

	public void retainAll(Collection<String> bindingNames) {
		bindings.keySet().retainAll(bindingNames);
	}

	public Set<String> getBindingNames() {
		return bindings.keySet();
	}

	public Value getValue(String bindingName) {
		return bindings.get(bindingName);
	}

	public Binding getBinding(String bindingName) {
		Value value = getValue(bindingName);

		if (value != null) {
			return new SimpleBinding(bindingName, value);
		}

		return null;
	}

	public boolean hasBinding(String bindingName) {
		return bindings.containsKey(bindingName);
	}

	public Iterator<Binding> iterator() {
		Iterator<Map.Entry<String, Value>> entries = bindings.entrySet().iterator();

		return new ConvertingIterator<Map.Entry<String, Value>, Binding>(entries) {

			@Override
			protected Binding convert(Map.Entry<String, Value> entry) {
				return new SimpleBinding(entry.getKey(), entry.getValue());
			}
		};
	}

	public int size() {
		return bindings.size();
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		else if (other instanceof QueryBindingSet) {
			return bindings.equals(((QueryBindingSet)other).bindings);
		}
		else if (other instanceof BindingSet) {
			int otherSize = 0;

			// Compare other's bindings to own
			for (Binding binding : (BindingSet)other) {
				Value ownValue = getValue(binding.getName());

				if (!binding.getValue().equals(ownValue)) {
					// Unequal bindings for this name
					return false;
				}

				otherSize++;
			}

			// All bindings have been matched, sets are equal if this binding set
			// doesn't have any additional bindings.
			return otherSize == bindings.size();
		}

		return false;
	}

	@Override
	public int hashCode() {
		int hashCode = 5;

		for (Map.Entry<String, Value> entry : bindings.entrySet()) {
			hashCode = 37 * hashCode + entry.hashCode();
		}

		return hashCode;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(32 * size());

		sb.append('[');

		Iterator<Binding> iter = iterator();
		while (iter.hasNext()) {
			sb.append(iter.next().toString());
			if (iter.hasNext()) {
				sb.append(';');
			}
		}

		sb.append(']');

		return sb.toString();
	}
}
