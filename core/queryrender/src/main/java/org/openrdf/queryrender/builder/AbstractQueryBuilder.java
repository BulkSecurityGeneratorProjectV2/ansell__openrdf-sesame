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
package org.openrdf.queryrender.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openrdf.model.IRI;
import org.openrdf.model.Value;
import org.openrdf.query.algebra.And;
import org.openrdf.query.algebra.BinaryTupleOperator;
import org.openrdf.query.algebra.Distinct;
import org.openrdf.query.algebra.EmptySet;
import org.openrdf.query.algebra.Extension;
import org.openrdf.query.algebra.ExtensionElem;
import org.openrdf.query.algebra.Filter;
import org.openrdf.query.algebra.Join;
import org.openrdf.query.algebra.LeftJoin;
import org.openrdf.query.algebra.MultiProjection;
import org.openrdf.query.algebra.Order;
import org.openrdf.query.algebra.OrderElem;
import org.openrdf.query.algebra.Projection;
import org.openrdf.query.algebra.ProjectionElem;
import org.openrdf.query.algebra.ProjectionElemList;
import org.openrdf.query.algebra.Reduced;
import org.openrdf.query.algebra.Slice;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.UnaryTupleOperator;
import org.openrdf.query.algebra.ValueConstant;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.algebra.helpers.StatementPatternCollector;
import org.openrdf.query.algebra.helpers.VarNameCollector;
import org.openrdf.query.impl.SimpleDataset;
import org.openrdf.query.parser.ParsedGraphQuery;
import org.openrdf.query.parser.ParsedQuery;
import org.openrdf.query.parser.ParsedTupleQuery;

/**
 * <p>
 * Base implementation of a QueryBuilder.
 * </p>
 * 
 * @author Michael Grove
 * @since 2.7.0
 */
public class AbstractQueryBuilder<T extends ParsedQuery> implements QueryBuilder<T> {

	// this is a bit of a hack making these protected so the select/construct
	// query impl can access it.
	// would be better to encapsulate building the projection element up so the
	// subclasses just handle it.
	protected List<StatementPattern> mProjectionPatterns = new ArrayList<StatementPattern>();

	protected List<String> mProjectionVars = new ArrayList<String>();

	private List<Group> mQueryAtoms = new ArrayList<Group>();

	private List<OrderElem> mOrderByElems = new ArrayList<OrderElem>();

	/**
	 * the current limit on the number of results
	 */
	private int mLimit = -1;

	/**
	 * The current result offset
	 */
	private int mOffset = -1;

	private boolean mDistinct = false;

	private boolean mReduced = false;

	/**
	 * the from clauses in the query
	 */
	private Set<IRI> mFrom = new HashSet<IRI>();

	/**
	 * The from named clauses of the query
	 */
	private Set<IRI> mFromNamed = new HashSet<IRI>();

	/**
	 * The query to be built
	 */
	private T mQuery;

	AbstractQueryBuilder(T theQuery) {
		mQuery = theQuery;
	}

	/**
	 * @inheritDoc
	 */
	public void reset() {
		mDistinct = mReduced = false;
		mLimit = mOffset = -1;
		mProjectionVars.clear();
		mQueryAtoms.clear();
		mProjectionPatterns.clear();
	}

	/**
	 * @inheritDoc
	 */
	public T query() {
		UnaryTupleOperator aRoot = null;
		UnaryTupleOperator aCurr = null;

		if (mLimit != -1 || mOffset != -1) {
			Slice aSlice = new Slice();

			if (mLimit != -1) {
				aSlice.setLimit(mLimit);
			}
			if (mOffset != -1) {
				aSlice.setOffset(mOffset);
			}

			aRoot = aCurr = aSlice;
		}

		if (mOrderByElems != null && !mOrderByElems.isEmpty()) {
			Order aOrder = new Order();

			aOrder.addElements(mOrderByElems);

			if (aRoot == null) {
				aRoot = aCurr = aOrder;
			}
			else {
				aCurr.setArg(aOrder);
				aCurr = aOrder;
			}
		}

		if (mDistinct) {
			Distinct aDistinct = new Distinct();

			if (aRoot == null) {
				aRoot = aCurr = aDistinct;
			}
			else {
				aCurr.setArg(aDistinct);
				aCurr = aDistinct;
			}
		}

		if (mReduced) {
			Reduced aReduced = new Reduced();

			if (aRoot == null) {
				aRoot = aCurr = aReduced;
			}
			else {
				aCurr.setArg(aReduced);
				aCurr = aReduced;
			}
		}

		TupleExpr aJoin = join();

		if (mQuery instanceof ParsedTupleQuery && mProjectionVars.isEmpty()) {
			VarNameCollector aCollector = new VarNameCollector();

			aJoin.visit(aCollector);

			mProjectionVars.addAll(aCollector.getVarNames());
		}
		else if (mQuery instanceof ParsedGraphQuery && mProjectionPatterns.isEmpty()) {
			StatementPatternCollector aCollector = new StatementPatternCollector();

			aJoin.visit(aCollector);

			mProjectionPatterns.addAll(aCollector.getStatementPatterns());
		}

		UnaryTupleOperator aProjection = projection();

		if (aRoot == null) {
			aRoot = aCurr = aProjection;
		}
		else {
			aCurr.setArg(aProjection);
		}

		if (aProjection.getArg() == null) {
			aCurr = aProjection;
		}
		else {
			// I think this is always a safe cast
			aCurr = (UnaryTupleOperator)aProjection.getArg();
		}

		if (aJoin != null) {
			aCurr.setArg(aJoin);
		}

		mQuery.setTupleExpr(aRoot);

		if (!mFrom.isEmpty() || !mFromNamed.isEmpty()) {
			SimpleDataset aDataset = new SimpleDataset();

			for (IRI aFrom : mFrom) {
				aDataset.addDefaultGraph(aFrom);
			}

			for (IRI aFrom : mFromNamed) {
				aDataset.addNamedGraph(aFrom);
			}

			mQuery.setDataset(aDataset);
		}

		return mQuery;
	}

	/**
	 * @inheritDoc
	 */
	public QueryBuilder<T> fromNamed(final IRI theURI) {
		mFromNamed.add(theURI);
		return this;
	}

	/**
	 * @inheritDoc
	 */
	public QueryBuilder<T> from(final IRI theURI) {
		mFrom.add(theURI);
		return this;
	}

	/**
	 * @inheritDoc
	 */
	public QueryBuilder<T> distinct() {
		// crappy way to only let this be set for select queries
		if (isSelect()) {
			mDistinct = true;
		}

		return this;
	}

	/**
	 * @inheritDoc
	 */
	public QueryBuilder<T> reduced() {
		mReduced = true;
		return this;
	}

	/**
	 * @inheritDoc
	 */
	public QueryBuilder<T> addProjectionVar(String... theNames) {
		if (isSelect()) {
			mProjectionVars.addAll(Arrays.asList(theNames));
		}

		return this;
	}

	private boolean isConstruct() {
		// crappy way to only let this be set for select queries
		return (mQuery instanceof ParsedGraphQuery);
	}

	private boolean isSelect() {
		// crappy way to only let this be set for select queries
		return (mQuery instanceof ParsedTupleQuery);
	}

	/**
	 * @inheritDoc
	 */
	public QueryBuilder<T> addProjectionStatement(final String theSubj, final String thePred,
			final String theObj)
	{
		if (isConstruct()) {
			mProjectionPatterns.add(new StatementPattern(new Var(theSubj), new Var(thePred), new Var(theObj)));
		}

		return this;
	}

	/**
	 * @inheritDoc
	 */
	public QueryBuilder<T> addProjectionStatement(final String theSubj, final Value thePred, final Value theObj)
	{
		if (isConstruct()) {
			mProjectionPatterns.add(new StatementPattern(new Var(theSubj), GroupBuilder.valueToVar(thePred),
					GroupBuilder.valueToVar(theObj)));
		}

		return this;
	}

	/**
	 * @inheritDoc
	 */
	public QueryBuilder<T> addProjectionStatement(final String theSubj, final String thePred,
			final Value theObj)
	{
		if (isConstruct()) {
			mProjectionPatterns.add(new StatementPattern(new Var(theSubj), new Var(thePred),
					GroupBuilder.valueToVar(theObj)));
		}

		return this;
	}

	/**
	 * @inheritDoc
	 */
	public QueryBuilder<T> addProjectionStatement(String theSubj, IRI thePred, Value theObj) {
		if (isConstruct()) {
			mProjectionPatterns.add(new StatementPattern(new Var(theSubj), GroupBuilder.valueToVar(thePred),
					GroupBuilder.valueToVar(theObj)));
		}

		return this;
	}

	/**
	 * @inheritDoc
	 */
	public QueryBuilder<T> addProjectionStatement(IRI theSubj, String thePred, String theObj) {
		if (isConstruct()) {
			mProjectionPatterns.add(new StatementPattern(GroupBuilder.valueToVar(theSubj), new Var(thePred),
					new Var(theObj)));
		}

		return this;
	}

	/**
	 * @inheritDoc
	 */
	public QueryBuilder<T> addProjectionStatement(IRI theSubj, IRI thePred, String theObj) {
		if (isConstruct()) {
			mProjectionPatterns.add(new StatementPattern(GroupBuilder.valueToVar(theSubj),
					GroupBuilder.valueToVar(thePred), new Var(theObj)));
		}

		return this;
	}

	/**
	 * @inheritDoc
	 */
	public QueryBuilder<T> addProjectionStatement(String theSubj, IRI thePred, String theObj) {
		if (isConstruct()) {
			mProjectionPatterns.add(new StatementPattern(new Var(theSubj), GroupBuilder.valueToVar(thePred),
					new Var(theObj)));
		}

		return this;
	}

	private TupleExpr join() {
		if (mQueryAtoms.isEmpty()) {
			throw new RuntimeException("Can't have an empty or missing join.");
		}
		else if (mQueryAtoms.size() == 1) {
			return mQueryAtoms.get(0).expr();
		}
		else {
			return groupAsJoin(mQueryAtoms);
		}
	}

	private UnaryTupleOperator projection() {
		if (!mProjectionPatterns.isEmpty()) {
			return multiProjection();
		}
		else {
			Extension aExt = null;

			ProjectionElemList aList = new ProjectionElemList();

			for (String aVar : mProjectionVars) {
				aList.addElement(new ProjectionElem(aVar));
			}

			Projection aProjection = new Projection();
			aProjection.setProjectionElemList(aList);

			if (aExt != null) {
				aProjection.setArg(aExt);
			}

			return aProjection;
		}
	}

	private UnaryTupleOperator multiProjection() {
		MultiProjection aProjection = new MultiProjection();

		Extension aExt = null;

		for (StatementPattern aPattern : mProjectionPatterns) {
			ProjectionElemList aList = new ProjectionElemList();

			aList.addElement(new ProjectionElem(aPattern.getSubjectVar().getName(), "subject"));
			aList.addElement(new ProjectionElem(aPattern.getPredicateVar().getName(), "predicate"));
			aList.addElement(new ProjectionElem(aPattern.getObjectVar().getName(), "object"));

			if (aPattern.getSubjectVar().hasValue()) {
				if (aExt == null) {
					aExt = new Extension();
				}

				aExt.addElements(new ExtensionElem(new ValueConstant(aPattern.getSubjectVar().getValue()),
						aPattern.getSubjectVar().getName()));
			}

			if (aPattern.getPredicateVar().hasValue()) {
				if (aExt == null) {
					aExt = new Extension();
				}

				aExt.addElements(new ExtensionElem(new ValueConstant(aPattern.getPredicateVar().getValue()),
						aPattern.getPredicateVar().getName()));
			}

			if (aPattern.getObjectVar().hasValue()) {
				if (aExt == null) {
					aExt = new Extension();
				}

				aExt.addElements(new ExtensionElem(new ValueConstant(aPattern.getObjectVar().getValue()),
						aPattern.getObjectVar().getName()));
			}

			aProjection.addProjection(aList);
		}

		if (aExt != null) {
			aProjection.setArg(aExt);
		}

		return aProjection;
	}

	/**
	 * @inheritDoc
	 */
	public GroupBuilder<T, QueryBuilder<T>> group() {
		return new GroupBuilder<T, QueryBuilder<T>>(this, false, null);
	}

	/**
	 * @inheritDoc
	 */
	public GroupBuilder<T, QueryBuilder<T>> optional() {
		return new GroupBuilder<T, QueryBuilder<T>>(this, true, null);
	}

	/**
	 * @inheritDoc
	 */
	public QueryBuilder<T> limit(int theLimit) {
		mLimit = theLimit;
		return this;
	}

	/**
	 * @inheritDoc
	 */
	public QueryBuilder<T> offset(int theOffset) {
		mOffset = theOffset;
		return this;
	}

	/**
	 * @inheritDoc
	 */
	public QueryBuilder<T> addGroup(Group theGroup) {
		mQueryAtoms.add(theGroup);
		return this;
	}

	/**
	 * @inheritDoc
	 */
	public QueryBuilder<T> removeGroup(Group theGroup) {
		mQueryAtoms.remove(theGroup);
		return this;
	}

	private TupleExpr groupAsJoin(List<Group> theList) {
		BinaryTupleOperator aJoin = new Join();

		Filter aFilter = null;
		for (Group aGroup : theList) {
			TupleExpr aExpr = aGroup.expr();

			if (aExpr == null) {
				continue;
			}

			if (aExpr instanceof Filter
					&& (((Filter)aExpr).getArg() == null || ((Filter)aExpr).getArg() instanceof EmptySet))
			{
				if (aFilter == null) {
					aFilter = (Filter)aExpr;
				}
				else {
					// if we already have a filter w/ an empty arg, let's And the
					// conditions together.
					aFilter.setCondition(new And(aFilter.getCondition(), ((Filter)aExpr).getCondition()));
				}

				continue;
			}

			if (aFilter != null) {
				aFilter.setArg(aExpr);
				aExpr = aFilter;

				aFilter = null;
			}

			if (aGroup.isOptional()) {
				LeftJoin lj = new LeftJoin();

				TupleExpr aLeft = joinOrExpr(aJoin);

				if (aLeft != null) {
					lj.setLeftArg(aLeft);
					lj.setRightArg(aExpr);

					aJoin = lj;

					continue;
				}
			}

			if (aJoin.getLeftArg() == null) {
				aJoin.setLeftArg(aExpr);
			}
			else if (aJoin.getRightArg() == null) {
				aJoin.setRightArg(aExpr);
			}
			else {
				Join aNewJoin = new Join();

				aNewJoin.setLeftArg(aJoin);
				aNewJoin.setRightArg(aExpr);

				aJoin = aNewJoin;
			}
		}

		TupleExpr aExpr = joinOrExpr(aJoin);

		if (aFilter != null) {
			aFilter.setArg(aExpr);
			aExpr = aFilter;
		}

		return aExpr;
	}

	private TupleExpr joinOrExpr(BinaryTupleOperator theExpr) {
		if (theExpr.getLeftArg() != null && theExpr.getRightArg() == null) {
			return theExpr.getLeftArg();
		}
		else if (theExpr.getLeftArg() == null && theExpr.getRightArg() != null) {
			return theExpr.getRightArg();
		}
		else if (theExpr.getLeftArg() == null && theExpr.getRightArg() == null) {
			return null;
		}
		else {
			return theExpr;
		}
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public QueryBuilder<T> orderBy(String... theNames) {
		return this.orderByAsc(theNames);
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public QueryBuilder<T> orderByAsc(String... theNames) {
		// null safe
		if (theNames != null) {
			for (String aName : theNames) {
				mOrderByElems.add(new OrderElem(new Var(aName), true));
			}
		}
		return this;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public QueryBuilder<T> orderByDesc(String... theNames) {
		// null safe
		if (theNames != null) {
			for (String aName : theNames) {
				mOrderByElems.add(new OrderElem(new Var(aName), false));
			}
		}
		return this;
	}

}
