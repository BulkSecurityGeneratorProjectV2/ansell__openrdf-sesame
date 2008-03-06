/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 1997-2006.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.querylanguage.serql.ast;

public class ASTNot extends ASTBooleanExpr {

	public ASTNot(int id) {
		super(id);
	}

	public ASTNot(SyntaxTreeBuilder p, int id) {
		super(p, id);
	}

	/** Accept the visitor. */
	public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data)
		throws VisitorException
	{
		return visitor.visit(this, data);
	}
	
	public ASTBooleanExpr getOperand() {
		return (ASTBooleanExpr)children.get(0);
	}
}
