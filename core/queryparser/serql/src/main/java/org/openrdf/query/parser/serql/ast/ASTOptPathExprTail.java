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
package org.openrdf.query.parser.serql.ast;

public class ASTOptPathExprTail extends ASTPathExprTail {

	public ASTOptPathExprTail(int id) {
		super(id);
	}

	public ASTOptPathExprTail(SyntaxTreeBuilder p, int id) {
		super(p, id);
	}

	@Override
	public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data)
		throws VisitorException
	{
		return visitor.visit(this, data);
	}

	/**
	 * Gets the optional tail part of the path expression.
	 * 
	 * @return The optional tail part of the path expression.
	 */
	public ASTBasicPathExprTail getOptionalTail() {
		return (ASTBasicPathExprTail)children.get(0);
	}

	public boolean hasWhereClause() {
		return getWhereClause() != null;
	}

	/**
	 * Gets the where-clause that constrains the results of the optional path
	 * expression tail, if any.
	 * 
	 * @return The where-clause, or <tt>null</tt> if not available.
	 */
	public ASTWhere getWhereClause() {
		if (children.size() >= 2) {
			Node node = children.get(1);

			if (node instanceof ASTWhere) {
				return (ASTWhere)node;
			}
		}

		return null;
	}

	@Override
	public ASTPathExprTail getNextTail() {
		if (children.size() >= 2) {
			Node node = children.get(children.size() - 1);

			if (node instanceof ASTPathExprTail) {
				return (ASTPathExprTail)node;
			}
		}

		return null;
	}
}
