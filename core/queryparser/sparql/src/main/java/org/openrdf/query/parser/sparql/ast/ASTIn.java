/* Generated By:JJTree: Do not edit this line. ASTIn.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package org.openrdf.query.parser.sparql.ast;

public
class ASTIn extends SimpleNode {
  public ASTIn(int id) {
    super(id);
  }

  public ASTIn(SyntaxTreeBuilder p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data) throws VisitorException {
    return visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=0b09d303dab8bf639d612338f9375011 (do not edit this line) */
