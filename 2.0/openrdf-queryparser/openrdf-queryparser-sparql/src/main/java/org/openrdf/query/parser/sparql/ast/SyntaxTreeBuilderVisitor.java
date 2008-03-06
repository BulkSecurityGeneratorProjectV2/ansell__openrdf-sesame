/* Generated By:JJTree: Do not edit this line. .\SyntaxTreeBuilderVisitor.java */

package org.openrdf.query.parser.sparql.ast;

public interface SyntaxTreeBuilderVisitor
{
  public Object visit(SimpleNode node, Object data) throws VisitorException;
  public Object visit(ASTQueryContainer node, Object data) throws VisitorException;
  public Object visit(ASTBaseDecl node, Object data) throws VisitorException;
  public Object visit(ASTPrefixDecl node, Object data) throws VisitorException;
  public Object visit(ASTSelectQuery node, Object data) throws VisitorException;
  public Object visit(ASTSelect node, Object data) throws VisitorException;
  public Object visit(ASTConstructQuery node, Object data) throws VisitorException;
  public Object visit(ASTConstruct node, Object data) throws VisitorException;
  public Object visit(ASTDescribeQuery node, Object data) throws VisitorException;
  public Object visit(ASTDescribe node, Object data) throws VisitorException;
  public Object visit(ASTAskQuery node, Object data) throws VisitorException;
  public Object visit(ASTDatasetClause node, Object data) throws VisitorException;
  public Object visit(ASTWhereClause node, Object data) throws VisitorException;
  public Object visit(ASTOrderClause node, Object data) throws VisitorException;
  public Object visit(ASTOrderCondition node, Object data) throws VisitorException;
  public Object visit(ASTLimit node, Object data) throws VisitorException;
  public Object visit(ASTOffset node, Object data) throws VisitorException;
  public Object visit(ASTGraphPatternGroup node, Object data) throws VisitorException;
  public Object visit(ASTBasicGraphPattern node, Object data) throws VisitorException;
  public Object visit(ASTOptionalGraphPattern node, Object data) throws VisitorException;
  public Object visit(ASTGraphGraphPattern node, Object data) throws VisitorException;
  public Object visit(ASTUnionGraphPattern node, Object data) throws VisitorException;
  public Object visit(ASTConstraint node, Object data) throws VisitorException;
  public Object visit(ASTFunctionCall node, Object data) throws VisitorException;
  public Object visit(ASTTriplesSameSubject node, Object data) throws VisitorException;
  public Object visit(ASTPropertyList node, Object data) throws VisitorException;
  public Object visit(ASTObjectList node, Object data) throws VisitorException;
  public Object visit(ASTIRI node, Object data) throws VisitorException;
  public Object visit(ASTBlankNodePropertyList node, Object data) throws VisitorException;
  public Object visit(ASTCollection node, Object data) throws VisitorException;
  public Object visit(ASTVar node, Object data) throws VisitorException;
  public Object visit(ASTOr node, Object data) throws VisitorException;
  public Object visit(ASTAnd node, Object data) throws VisitorException;
  public Object visit(ASTCompare node, Object data) throws VisitorException;
  public Object visit(ASTMath node, Object data) throws VisitorException;
  public Object visit(ASTNot node, Object data) throws VisitorException;
  public Object visit(ASTNumericLiteral node, Object data) throws VisitorException;
  public Object visit(ASTStr node, Object data) throws VisitorException;
  public Object visit(ASTLang node, Object data) throws VisitorException;
  public Object visit(ASTLangMatches node, Object data) throws VisitorException;
  public Object visit(ASTDatatype node, Object data) throws VisitorException;
  public Object visit(ASTBound node, Object data) throws VisitorException;
  public Object visit(ASTSameTerm node, Object data) throws VisitorException;
  public Object visit(ASTIsIRI node, Object data) throws VisitorException;
  public Object visit(ASTIsBlank node, Object data) throws VisitorException;
  public Object visit(ASTIsLiteral node, Object data) throws VisitorException;
  public Object visit(ASTRegexExpression node, Object data) throws VisitorException;
  public Object visit(ASTRDFLiteral node, Object data) throws VisitorException;
  public Object visit(ASTTrue node, Object data) throws VisitorException;
  public Object visit(ASTFalse node, Object data) throws VisitorException;
  public Object visit(ASTString node, Object data) throws VisitorException;
  public Object visit(ASTQName node, Object data) throws VisitorException;
  public Object visit(ASTBlankNode node, Object data) throws VisitorException;
}
