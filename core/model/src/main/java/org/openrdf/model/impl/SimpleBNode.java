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

import org.openrdf.model.BNode;

/**
 * An simple default implementation of the {@link BNode} interface.
 * 
 * @author Arjohn Kampman
 */
public class SimpleBNode implements BNode {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final long serialVersionUID = 5273570771022125970L;

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The blank node's identifier.
	 */
	private String id;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new, unitialized blank node. This blank node's ID needs to be
	 * {@link #setID(String) set} before the normal methods can be used.
	 */
	protected SimpleBNode() {
	}

	/**
	 * Creates a new blank node with the supplied identifier.
	 * 
	 * @param id
	 *        The identifier for this blank node, must not be <tt>null</tt>.
	 */
	protected SimpleBNode(String id) {
		this();
		setID(id);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public String getID() {
		return id;
	}

	protected void setID(String id) {
		this.id = id;
	}

	public String stringValue() {
		return id;
	}

	// Overrides Object.equals(Object), implements BNode.equals(Object)
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o instanceof BNode) {
			BNode otherNode = (BNode)o;
			return this.getID().equals(otherNode.getID());
		}

		return false;
	}

	// Overrides Object.hashCode(), implements BNode.hashCode()
	@Override
	public int hashCode() {
		return id.hashCode();
	}

	// Overrides Object.toString()
	@Override
	public String toString() {
		return "_:" + id;
	}
}
