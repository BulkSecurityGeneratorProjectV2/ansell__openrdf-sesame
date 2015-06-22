/* Generated By:JJTree: Do not edit this line. ASTGroupClause.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package org.openrdf.query.parser.sparql.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public
class ASTGroupClause extends SimpleNode {
  public ASTGroupClause(int id) {
    super(id);
  }

  public ASTGroupClause(SyntaxTreeBuilder p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data) throws VisitorException {
    return visitor.visit(this, data);
  }
  
  public List<String> getBindingNames() {
	  
	  List<String> bindingNames = new ArrayList<String>();
	  
	  for(ASTGroupCondition condition: getGroupConditions()) {
		  bindingNames.add(condition.getName());
	  }
	  
	  return bindingNames;
  }
  
  public List<ASTGroupCondition> getGroupConditions() {
	  return jjtGetChildren(ASTGroupCondition.class);
  }
}
/* JavaCC - OriginalChecksum=160933dbaf7175f4d32bb39163c160f5 (do not edit this line) */
