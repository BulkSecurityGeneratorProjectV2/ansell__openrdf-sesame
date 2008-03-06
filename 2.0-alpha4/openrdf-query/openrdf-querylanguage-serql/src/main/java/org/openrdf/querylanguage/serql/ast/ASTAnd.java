/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 1997-2006.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.querylanguage.serql.ast;

import java.util.List;

import org.openrdf.util.collections.CastingList;

public class ASTAnd extends ASTBooleanExpr {

	public ASTAnd(int id) {
		super(id);
	}

	public ASTAnd(SyntaxTreeBuilder p, int id) {
		super(p, id);
	}

	/** Accept the visitor. */
	public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data)
		throws VisitorException
	{
		return visitor.visit(this, data);
	}
	
	public List<ASTBooleanExpr> getOperandList() {
		return new CastingList<ASTBooleanExpr>(children);
	}
}
