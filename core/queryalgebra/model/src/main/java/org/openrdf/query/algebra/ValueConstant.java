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

import org.openrdf.model.Value;

/**
 * A ValueExpr with a constant value.
 */
public class ValueConstant extends AbstractQueryModelNode implements ValueExpr {

	/*-----------*
	 * Variables *
	 *-----------*/

	private Value value;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public ValueConstant() {
	}

	public ValueConstant(Value value) {
		setValue(value);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public Value getValue() {
		return value;
	}

	public void setValue(Value value) {
		assert value != null : "value must not be null";
		this.value = value;
	}

	public <X extends Exception> void visit(QueryModelVisitor<X> visitor)
		throws X
	{
		visitor.meet(this);
	}

	@Override
	public String getSignature() {
		StringBuilder sb = new StringBuilder(64);

		sb.append(super.getSignature());
		sb.append(" (value=");
		sb.append(value.toString());
		sb.append(")");

		return sb.toString();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof ValueConstant) {
			ValueConstant o = (ValueConstant)other;
			return value.equals(o.getValue());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public ValueConstant clone() {
		return (ValueConstant)super.clone();
	}
}
