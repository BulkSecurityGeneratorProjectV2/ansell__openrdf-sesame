/* Generated By:JJTree: Do not edit this line. ASTAskQuery.java */

package org.openrdf.query.parser.sparql.ast;

public class ASTAskQuery extends ASTQuery {

	public ASTAskQuery(int id) {
		super(id);
	}

	public ASTAskQuery(SyntaxTreeBuilder p, int id) {
		super(p, id);
	}

	@Override
	public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data)
		throws VisitorException
	{
		return visitor.visit(this, data);
	}
}
