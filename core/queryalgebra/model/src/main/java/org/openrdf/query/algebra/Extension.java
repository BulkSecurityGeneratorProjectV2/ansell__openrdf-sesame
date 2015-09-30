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
package org.openrdf.query.algebra;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * An extension operator that can be used to add bindings to solutions whose
 * values are defined by {@link ValueExpr value expressions}.
 */
public class Extension extends UnaryTupleOperator {

	/*-----------*
	 * Variables *
	 *-----------*/

	private List<ExtensionElem> elements = new ArrayList<ExtensionElem>();

	/*--------------*
	 * Constructors *
	 *--------------*/

	public Extension() {
	}

	public Extension(TupleExpr arg) {
		super(arg);
	}

	public Extension(TupleExpr arg, ExtensionElem... elements) {
		this(arg);
		addElements(elements);
	}

	public Extension(TupleExpr arg, Iterable<ExtensionElem> elements) {
		this(arg);
		addElements(elements);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public List<ExtensionElem> getElements() {
		return elements;
	}

	public void setElements(Iterable<ExtensionElem> elements) {
		this.elements.clear();
		addElements(elements);
	}

	public void addElements(ExtensionElem... elements) {
		for (ExtensionElem pe : elements) {
			addElement(pe);
		}
	}

	public void addElements(Iterable<ExtensionElem> elements) {
		for (ExtensionElem pe : elements) {
			addElement(pe);
		}
	}

	public void addElement(ExtensionElem pe) {
		elements.add(pe);
		pe.setParentNode(this);
	}

	@Override
	public Set<String> getBindingNames() {
		Set<String> bindingNames = new LinkedHashSet<String>(arg.getBindingNames());

		for (ExtensionElem pe : elements) {
			bindingNames.add(pe.getName());
		}

		return bindingNames;
	}

	public <X extends Exception> void visit(QueryModelVisitor<X> visitor)
		throws X
	{
		visitor.meet(this);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor)
		throws X
	{
		for (ExtensionElem elem : elements) {
			elem.visit(visitor);
		}

		super.visitChildren(visitor);
	}

	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {
		if (replaceNodeInList(elements, current, replacement)) {
			return;
		}
		super.replaceChildNode(current, replacement);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Extension && super.equals(other)) {
			Extension o = (Extension)other;
			return elements.equals(o.getElements());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ elements.hashCode();
	}

	@Override
	public Extension clone() {
		Extension clone = (Extension)super.clone();

		clone.elements = new ArrayList<ExtensionElem>(getElements().size());
		for (ExtensionElem elem : getElements()) {
			clone.addElement(elem.clone());
		}

		return clone;
	}
}
