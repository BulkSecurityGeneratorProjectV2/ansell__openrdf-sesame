/*******************************************************************************
 * Copyright (c) 2015, Eclipse Foundation, Inc. and its licensors.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * - Neither the name of the Eclipse Foundation, Inc. nor the names of its
 *   contributors may be used to endorse or promote products derived from this
 *   software without specific prior written permission. 
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/
package org.openrdf.query.algebra.evaluation.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openrdf.model.Value;
import org.openrdf.model.impl.BooleanLiteral;
import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.algebra.And;
import org.openrdf.query.algebra.BinaryValueOperator;
import org.openrdf.query.algebra.Bound;
import org.openrdf.query.algebra.Extension;
import org.openrdf.query.algebra.ExtensionElem;
import org.openrdf.query.algebra.FunctionCall;
import org.openrdf.query.algebra.If;
import org.openrdf.query.algebra.Or;
import org.openrdf.query.algebra.ProjectionElem;
import org.openrdf.query.algebra.ProjectionElemList;
import org.openrdf.query.algebra.Regex;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.UnaryTupleOperator;
import org.openrdf.query.algebra.UnaryValueOperator;
import org.openrdf.query.algebra.ValueConstant;
import org.openrdf.query.algebra.ValueExpr;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.algebra.evaluation.EvaluationStrategy;
import org.openrdf.query.algebra.evaluation.QueryOptimizer;
import org.openrdf.query.algebra.evaluation.ValueExprEvaluationException;
import org.openrdf.query.algebra.evaluation.function.Function;
import org.openrdf.query.algebra.evaluation.function.FunctionRegistry;
import org.openrdf.query.algebra.evaluation.function.numeric.Rand;
import org.openrdf.query.algebra.evaluation.function.rdfterm.STRUUID;
import org.openrdf.query.algebra.evaluation.function.rdfterm.UUID;
import org.openrdf.query.algebra.helpers.AbstractQueryModelVisitor;
import org.openrdf.query.impl.EmptyBindingSet;

/**
 * A query optimizer that optimizes constant value expressions.
 * 
 * @author James Leigh
 * @author Arjohn Kampman
 */
public class ConstantOptimizer implements QueryOptimizer {

	protected final Logger logger = LoggerFactory.getLogger(this.getClass());

	protected final EvaluationStrategy strategy;

	public ConstantOptimizer(EvaluationStrategy strategy) {
		this.strategy = strategy;
	}

	/**
	 * Applies generally applicable optimizations to the supplied query: variable
	 * assignments are inlined.
	 */
	@Override
	public void optimize(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings) {
		ConstantVisitor visitor = new ConstantVisitor();
		tupleExpr.visit(visitor);
		Set<String> varsBefore = visitor.varNames;

		VarNameCollector varCollector = new VarNameCollector();
		tupleExpr.visit(varCollector);
		Set<String> varsAfter = varCollector.varNames;

		if (varsAfter.size() < varsBefore.size()) {
			varsBefore.removeAll(varsAfter);
			for (ProjectionElemList projElems : visitor.projElemLists) {
				for (ProjectionElem projElem : projElems.getElements()) {
					String name = projElem.getSourceName();
					if (varsBefore.contains(name)) {
						UnaryTupleOperator proj = (UnaryTupleOperator)projElems.getParentNode();
						Extension ext = new Extension(proj.getArg());
						proj.setArg(ext);
						Var lostVar = new Var(name);
						Value value = bindings.getValue(name);
						if (value != null) {
							lostVar.setValue(value);
						}
						ext.addElement(new ExtensionElem(lostVar, name));
					}
				}
			}
		}
	}

	protected class ConstantVisitor extends VarNameCollector {

		final List<ProjectionElemList> projElemLists = new ArrayList<ProjectionElemList>();

		@Override
		public void meet(ProjectionElemList projElems) {
			super.meet(projElems);
			projElemLists.add(projElems);
		}

		@Override
		public void meet(Or or) {
			or.visitChildren(this);

			try {
				if (isConstant(or.getLeftArg()) && isConstant(or.getRightArg())) {
					boolean value = strategy.isTrue(or, EmptyBindingSet.getInstance());
					or.replaceWith(new ValueConstant(BooleanLiteral.valueOf(value)));
				}
				else if (isConstant(or.getLeftArg())) {
					boolean leftIsTrue = strategy.isTrue(or.getLeftArg(), EmptyBindingSet.getInstance());
					if (leftIsTrue) {
						or.replaceWith(new ValueConstant(BooleanLiteral.TRUE));
					}
					else {
						or.replaceWith(or.getRightArg());
					}
				}
				else if (isConstant(or.getRightArg())) {
					boolean rightIsTrue = strategy.isTrue(or.getRightArg(), EmptyBindingSet.getInstance());
					if (rightIsTrue) {
						or.replaceWith(new ValueConstant(BooleanLiteral.TRUE));
					}
					else {
						or.replaceWith(or.getLeftArg());
					}
				}
			}
			catch (ValueExprEvaluationException e) {
				// TODO: incompatible values types(?), remove the affected part of
				// the query tree
				logger.debug("Failed to evaluate BinaryValueOperator with two constant arguments", e);
			}
			catch (QueryEvaluationException e) {
				logger.error("Query evaluation exception caught", e);
			}
		}

		@Override
		public void meet(And and) {
			and.visitChildren(this);

			try {
				if (isConstant(and.getLeftArg()) && isConstant(and.getRightArg())) {
					boolean value = strategy.isTrue(and, EmptyBindingSet.getInstance());
					and.replaceWith(new ValueConstant(BooleanLiteral.valueOf(value)));
				}
				else if (isConstant(and.getLeftArg())) {
					boolean leftIsTrue = strategy.isTrue(and.getLeftArg(), EmptyBindingSet.getInstance());
					if (leftIsTrue) {
						and.replaceWith(and.getRightArg());
					}
					else {
						and.replaceWith(new ValueConstant(BooleanLiteral.FALSE));
					}
				}
				else if (isConstant(and.getRightArg())) {
					boolean rightIsTrue = strategy.isTrue(and.getRightArg(), EmptyBindingSet.getInstance());
					if (rightIsTrue) {
						and.replaceWith(and.getLeftArg());
					}
					else {
						and.replaceWith(new ValueConstant(BooleanLiteral.FALSE));
					}
				}
			}
			catch (ValueExprEvaluationException e) {
				// TODO: incompatible values types(?), remove the affected part of
				// the query tree
				logger.debug("Failed to evaluate BinaryValueOperator with two constant arguments", e);
			}
			catch (QueryEvaluationException e) {
				logger.error("Query evaluation exception caught", e);
			}
		}

		@Override
		protected void meetBinaryValueOperator(BinaryValueOperator binaryValueOp) {
			super.meetBinaryValueOperator(binaryValueOp);

			if (isConstant(binaryValueOp.getLeftArg()) && isConstant(binaryValueOp.getRightArg())) {
				try {
					Value value = strategy.evaluate(binaryValueOp, EmptyBindingSet.getInstance());
					binaryValueOp.replaceWith(new ValueConstant(value));
				}
				catch (ValueExprEvaluationException e) {
					// TODO: incompatible values types(?), remove the affected part
					// of the query tree
					logger.debug("Failed to evaluate BinaryValueOperator with two constant arguments", e);
				}
				catch (QueryEvaluationException e) {
					logger.error("Query evaluation exception caught", e);
				}
			}
		}

		@Override
		protected void meetUnaryValueOperator(UnaryValueOperator unaryValueOp) {
			super.meetUnaryValueOperator(unaryValueOp);

			if (isConstant(unaryValueOp.getArg())) {
				try {
					Value value = strategy.evaluate(unaryValueOp, EmptyBindingSet.getInstance());
					unaryValueOp.replaceWith(new ValueConstant(value));
				}
				catch (ValueExprEvaluationException e) {
					// TODO: incompatible values types(?), remove the affected part
					// of the query tree
					logger.debug("Failed to evaluate UnaryValueOperator with a constant argument", e);
				}
				catch (QueryEvaluationException e) {
					logger.error("Query evaluation exception caught", e);
				}
			}
		}

		@Override
		public void meet(FunctionCall functionCall) {
			super.meet(functionCall);

			List<ValueExpr> args = functionCall.getArgs();

			if (args.size() == 0) {
				/* SPARQL has two types of zero-arg function. One are proper 'constant' functions like NOW() which generate a single value
				 *  for the entire query and which can be safely optimized to a constant. Other functions, like RAND(), UUID() and STRUUID(), 
				 *  are a special case: they are expected to yield a new value on every call, and can therefore not be replaced by a constant.
				 */
				if (!isConstantZeroArgFunction(functionCall)) {
					return;
				}
			}
			else {
				for (ValueExpr arg : args) {
					if (!isConstant(arg)) {
						return;
					}
				}
			}

			// All arguments are constant

			try {
				Value value = strategy.evaluate(functionCall, EmptyBindingSet.getInstance());
				functionCall.replaceWith(new ValueConstant(value));
			}
			catch (ValueExprEvaluationException e) {
				// TODO: incompatible values types(?), remove the affected part of
				// the query tree
				logger.debug("Failed to evaluate BinaryValueOperator with two constant arguments", e);
			}
			catch (QueryEvaluationException e) {
				logger.error("Query evaluation exception caught", e);
			}
		}

		/**
		 * Determines if the provided zero-arg function is a function that should
		 * return a constant value for the entire query execution (e.g NOW()), or
		 * if it should generate a new value for every call (e.g. RAND()).
		 * 
		 * @param functionCall
		 *        a zero-arg function call.
		 * @return <code>true<code> iff the provided function returns a constant value for the query execution, <code>false</code>
		 *         otherwise.
		 */
		private boolean isConstantZeroArgFunction(FunctionCall functionCall) {
			Function function = FunctionRegistry.getInstance().get(functionCall.getURI()).orElseThrow(
					() -> new QueryEvaluationException(
							"Unable to find function with the URI: " + functionCall.getURI()));

			// we treat constant functions as the 'regular case' and make
			// exceptions for specific SPARQL built-in functions that require
			// different treatment.
			if (function instanceof Rand || function instanceof UUID || function instanceof STRUUID) {
				return false;
			}

			return true;
		}

		@Override
		public void meet(Bound bound) {
			super.meet(bound);

			if (bound.getArg().hasValue()) {
				// variable is always bound
				bound.replaceWith(new ValueConstant(BooleanLiteral.TRUE));
			}
		}

		@Override
		public void meet(If node) {
			super.meet(node);

			if (isConstant(node.getCondition())) {
				try {
					if (strategy.isTrue(node.getCondition(), EmptyBindingSet.getInstance())) {
						node.replaceWith(node.getResult());
					}
					else {
						node.replaceWith(node.getAlternative());
					}
				}
				catch (ValueExprEvaluationException e) {
					logger.debug("Failed to evaluate UnaryValueOperator with a constant argument", e);
				}
				catch (QueryEvaluationException e) {
					logger.error("Query evaluation exception caught", e);
				}
			}
		}

		/**
		 * Override meetBinaryValueOperator
		 */
		@Override
		public void meet(Regex node) {
			super.meetNode(node);

			if (isConstant(node.getArg()) && isConstant(node.getPatternArg())
					&& isConstant(node.getFlagsArg()))
			{
				try {
					Value value = strategy.evaluate(node, EmptyBindingSet.getInstance());
					node.replaceWith(new ValueConstant(value));
				}
				catch (ValueExprEvaluationException e) {
					logger.debug("Failed to evaluate BinaryValueOperator with two constant arguments", e);
				}
				catch (QueryEvaluationException e) {
					logger.error("Query evaluation exception caught", e);
				}
			}
		}

		private boolean isConstant(ValueExpr expr) {
			return expr instanceof ValueConstant || expr instanceof Var && ((Var)expr).hasValue();
		}
	}

	protected class VarNameCollector extends AbstractQueryModelVisitor<RuntimeException> {

		final Set<String> varNames = new HashSet<String>();

		@Override
		public void meet(Var var) {
			if (!var.isAnonymous()) {
				varNames.add(var.getName());
			}
		}
	}
}
