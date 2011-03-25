/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.query.algebra;

/**
 * The IF function, as defined in SPARQL 1.1 Query.
 * 
 * @author Jeen Broekstra
 */
public class If extends QueryModelNodeBase implements ValueExpr {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The operator's arguments.
	 */
	private ValueExpr condition;

	private ValueExpr result;

	private ValueExpr alternative;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public If() {
	}

	public If(ValueExpr condition) {
		setCondition(condition);
	}
	
	public If(ValueExpr condition, ValueExpr result) {
		setCondition(condition);
		setResult(result);
	}
	
	public If(ValueExpr condition, ValueExpr result, ValueExpr alternative) {
		setCondition(condition);
		setResult(result);
		setAlternative(alternative);
	}


	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Gets the argument of this unary value operator.
	 * 
	 * @return The operator's argument.
	 */
	public ValueExpr getCondition() {
		return condition;
	}

	/**
	 * Sets the condition argument of this unary value operator.
	 * 
	 * @param condition
	 *        The (new) condition argument for this operator, must not be
	 *        <tt>null</tt>.
	 */
	public void setCondition(ValueExpr condition) {
		assert condition != null : "arg must not be null";
		condition.setParentNode(this);
		this.condition = condition;
	}

	public <X extends Exception> void visit(QueryModelVisitor<X> visitor)
		throws X
	{
		visitor.meet(this);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor)
		throws X
	{
		condition.visit(visitor);
	}

	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {
		if (condition == current) {
			setCondition((ValueExpr)replacement);
		}
		else {
			super.replaceChildNode(current, replacement);
		}
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof If) {
			If o = (If)other;
			return (condition.equals(o.getCondition()) && result.equals(o.getResult()) && alternative.equals(o.getAlternative()));
		}
		return false;
	}

	@Override
	public int hashCode() {
		return condition.hashCode() ^ result.hashCode() ^ "If".hashCode();
	}

	@Override
	public If clone() {
		If clone = (If)super.clone();
		clone.setCondition(getCondition().clone());
		clone.setResult(getResult().clone());
		clone.setAlternative(getAlternative().clone());
		return clone;
	}

	/**
	 * @param result
	 *        The result to set.
	 */
	public void setResult(ValueExpr result) {
		this.result = result;
	}

	/**
	 * @return Returns the result.
	 */
	public ValueExpr getResult() {
		return result;
	}

	/**
	 * @param alternative
	 *        The alternative to set.
	 */
	public void setAlternative(ValueExpr alternative) {
		this.alternative = alternative;
	}

	/**
	 * @return Returns the alternative.
	 */
	public ValueExpr getAlternative() {
		return alternative;
	}
}
