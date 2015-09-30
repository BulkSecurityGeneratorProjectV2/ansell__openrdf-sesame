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
package org.openrdf.model.impl;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.openrdf.model.IRI;
import org.openrdf.model.vocabulary.XMLSchema;

/**
 * An extension of {@link SimpleLiteral} that stores an integer value using a
 * {@link BigDecimal} object.
 * 
 * @author Arjohn Kampman
 */
public class DecimalLiteral extends SimpleLiteral {

	private static final long serialVersionUID = -3310213093222314380L;
	
	private final BigDecimal value;

	/**
	 * Creates an xsd:decimal literal with the specified value.
	 */
	protected DecimalLiteral(BigDecimal value) {
		this(value, XMLSchema.DECIMAL);
	}

	/**
	 * Creates a literal with the specified value and datatype.
	 */
	protected DecimalLiteral(BigDecimal value, IRI datatype) {
		// TODO: maybe DecimalLiteral should not extend SimpleLiteral?
		super(value.toPlainString(), datatype);
		this.value = value;
	}

	@Override
	public byte byteValue()
	{
		return value.byteValue();
	}

	@Override
	public short shortValue()
	{
		return value.shortValue();
	}

	@Override
	public int intValue()
	{
		return value.intValue();
	}

	@Override
	public long longValue()
	{
		return value.longValue();
	}

	@Override
	public float floatValue()
	{
		return value.floatValue();
	}

	@Override
	public double doubleValue()
	{
		return value.doubleValue();
	}

	@Override
	public BigInteger integerValue()
	{
		return value.toBigInteger();
	}

	@Override
	public BigDecimal decimalValue()
	{
		return value;
	}
}
