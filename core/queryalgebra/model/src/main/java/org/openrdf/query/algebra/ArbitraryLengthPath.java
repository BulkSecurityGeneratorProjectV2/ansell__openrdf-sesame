/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 1997-2009.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.query.algebra;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openrdf.query.algebra.StatementPattern.Scope;

/**
 * A tuple expression that matches a path of arbitrary length against an RDF graph.
 * They can can be targeted at one of three context scopes: all
 * contexts, null context only, or named contexts only.
 */
public class ArbitraryLengthPath extends QueryModelNodeBase implements TupleExpr {

	/*-----------*
	 * Variables *
	 *-----------*/

	private Scope scope;

	private Var subjectVar;

	private Var predicateVar;

	private Var objectVar;

	private Var contextVar;

	private long minLength;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public ArbitraryLengthPath() {
	}

	/**
	 * Creates a arbitrary-length path that matches a subject-, predicate- and object
	 * variable against statements from all contexts.
	 */
	public ArbitraryLengthPath(Var subject, Var predicate, Var object, long minLength) {
		this(Scope.DEFAULT_CONTEXTS, subject, predicate, object, minLength);
	}

	/**
	 * Creates a arbitrary-length path that matches a subject-, predicate- and object
	 * variable against statements from the specified context scope.
	 */
	public ArbitraryLengthPath(Scope scope, Var subject, Var predicate, Var object, long minLength) {
		this(scope, subject, predicate, object, null, minLength);
	}

	/**
	 * Creates a arbitrary-length path that matches a subject-, predicate-, object-
	 * and context variable against statements from all contexts.
	 */
	public ArbitraryLengthPath(Var subject, Var predicate, Var object, Var context, long minLength) {
		this(Scope.DEFAULT_CONTEXTS, subject, predicate, object, context, minLength);
	}

	/**
	 * Creates a arbitrary-length path that matches a subject-, predicate-, object-
	 * and context variable against statements from the specified context scope.
	 */
	public ArbitraryLengthPath(Scope scope, Var subjVar, Var predVar, Var objVar, Var conVar, long minLength) {
		setScope(scope);
		setSubjectVar(subjVar);
		setPredicateVar(predVar);
		setObjectVar(objVar);
		setContextVar(conVar);
		setMinLength(minLength);
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Gets the context scope for the arbitrary-length path.
	 */
	public Scope getScope() {
		return scope;
	}

	/**
	 * Sets the context scope for the arbitrary-length path
	 */
	public void setScope(Scope scope) {
		assert scope != null : "scope must not be null";
		this.scope = scope;
	}

	public Var getSubjectVar() {
		return subjectVar;
	}

	public void setSubjectVar(Var subject) {
		assert subject != null : "subject must not be null";
		subject.setParentNode(this);
		subjectVar = subject;
	}

	public Var getPredicateVar() {
		return predicateVar;
	}

	public void setPredicateVar(Var predicate) {
		assert predicate != null : "predicate must not be null";
		predicate.setParentNode(this);
		predicateVar = predicate;
	}

	public Var getObjectVar() {
		return objectVar;
	}

	public void setObjectVar(Var object) {
		assert object != null : "object must not be null";
		object.setParentNode(this);
		objectVar = object;
	}

	public void setMinLength(long minLength) {
		this.minLength = minLength;
	}
	
	public long getMinLength() {
		return minLength;
	}
	
	/**
	 * Returns the context variable, if available.
	 */
	public Var getContextVar() {
		return contextVar;
	}

	public void setContextVar(Var context) {
		if (context != null) {
			context.setParentNode(this);
		}
		contextVar = context;
	}

	public Set<String> getBindingNames() {
		return getAssuredBindingNames();
	}

	public Set<String> getAssuredBindingNames() {
		Set<String> bindingNames = new HashSet<String>(8);

		if (subjectVar != null) {
			bindingNames.add(subjectVar.getName());
		}
		if (predicateVar != null) {
			bindingNames.add(predicateVar.getName());
		}
		if (objectVar != null) {
			bindingNames.add(objectVar.getName());
		}
		if (contextVar != null) {
			bindingNames.add(contextVar.getName());
		}

		return bindingNames;
	}

	public List<Var> getVarList() {
		return getVars(new ArrayList<Var>(4));
	}

	/**
	 * Adds the variables of this statement pattern to the supplied collection.
	 */
	public <L extends Collection<Var>> L getVars(L varCollection) {
		if (subjectVar != null) {
			varCollection.add(subjectVar);
		}
		if (predicateVar != null) {
			varCollection.add(predicateVar);
		}
		if (objectVar != null) {
			varCollection.add(objectVar);
		}
		if (contextVar != null) {
			varCollection.add(contextVar);
		}

		return varCollection;
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
		if (subjectVar != null) {
			subjectVar.visit(visitor);
		}
		if (predicateVar != null) {
			predicateVar.visit(visitor);
		}
		if (objectVar != null) {
			objectVar.visit(visitor);
		}
		if (contextVar != null) {
			contextVar.visit(visitor);
		}

		super.visitChildren(visitor);
	}

	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {
		if (subjectVar == current) {
			setSubjectVar((Var)replacement);
		}
		else if (predicateVar == current) {
			setPredicateVar((Var)replacement);
		}
		else if (objectVar == current) {
			setObjectVar((Var)replacement);
		}
		else if (contextVar == current) {
			setContextVar((Var)replacement);
		}
		else {
			super.replaceChildNode(current, replacement);
		}
	}

	@Override
	public String getSignature() {
		StringBuilder sb = new StringBuilder(128);

		sb.append(super.getSignature());

		if (scope == Scope.NAMED_CONTEXTS) {
			sb.append(" FROM NAMED CONTEXT");
		}

		return sb.toString();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof ArbitraryLengthPath) {
			ArbitraryLengthPath o = (ArbitraryLengthPath)other;
			return subjectVar.equals(o.getSubjectVar()) && predicateVar.equals(o.getPredicateVar())
					&& objectVar.equals(o.getObjectVar()) && nullEquals(contextVar, o.getContextVar())
					&& scope.equals(o.getScope());
		}
		return false;
	}

	@Override
	public int hashCode() {
		int result = subjectVar.hashCode();
		result ^= predicateVar.hashCode();
		result ^= objectVar.hashCode();
		if (contextVar != null) {
			result ^= contextVar.hashCode();
		}
		if (scope == Scope.NAMED_CONTEXTS) {
			result = ~result;
		}
		return result;
	}

	@Override
	public ArbitraryLengthPath clone() {
		ArbitraryLengthPath clone = (ArbitraryLengthPath)super.clone();
		clone.setSubjectVar(getSubjectVar().clone());
		clone.setPredicateVar(getPredicateVar().clone());
		clone.setObjectVar(getObjectVar().clone());

		if (getContextVar() != null) {
			clone.setContextVar(getContextVar().clone());
		}

		return clone;
	}
}
