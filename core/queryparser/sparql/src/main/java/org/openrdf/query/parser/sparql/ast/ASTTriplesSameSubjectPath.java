/* Generated By:JJTree: Do not edit this line. ASTTriplesSameSubjectPath.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package org.openrdf.query.parser.sparql.ast;

public
class ASTTriplesSameSubjectPath extends SimpleNode {
  public ASTTriplesSameSubjectPath(int id) {
    super(id);
  }

  public ASTTriplesSameSubjectPath(SyntaxTreeBuilder p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data) throws VisitorException {
    return visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=0e23bca3cbb073c810fd90cb9fe305d2 (do not edit this line) */
