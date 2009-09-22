/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 1997-2009.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.query.algebra;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A natural join between two tuple expressions.
 */
public class Join extends NaryTupleOperator {

	private static final long serialVersionUID = -1501013589230065874L;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public Join() {
	}

	/**
	 * Creates a new natural join operator.
	 */
	public Join(TupleExpr... args) {
		super(args);
	}

	/**
	 * Creates a new natural join operator.
	 */
	public Join(Iterable<? extends TupleExpr> args) {
		super(args);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public Set<String> getAssuredBindingNames() {
		Set<String> bindingNames = new LinkedHashSet<String>(16);
		for (TupleExpr arg : getArgs()) {
			bindingNames.addAll(arg.getAssuredBindingNames());
		}
		return bindingNames;
	}

	public <X extends Exception> void visit(QueryModelVisitor<X> visitor)
		throws X
	{
		visitor.meet(this);
	}

	@Override
	public Join clone() {
		return (Join)super.clone();
	}
}
