/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2007-2008.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.repository.sail;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BindingSet;
import org.openrdf.query.Cursor;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryResultUtil;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.base.ConvertingCursor;
import org.openrdf.query.base.FilteringCursor;
import org.openrdf.query.impl.GraphQueryResultImpl;
import org.openrdf.query.parser.ParsedGraphQuery;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.sail.SailConnection;
import org.openrdf.store.StoreException;

/**
 * @author Arjohn Kampman
 */
public class SailGraphQuery extends SailQuery implements GraphQuery {

	protected SailGraphQuery(ParsedGraphQuery tupleQuery, SailRepositoryConnection con) {
		super(tupleQuery, con);
	}

	@Override
	public ParsedGraphQuery getParsedQuery() {
		return (ParsedGraphQuery)super.getParsedQuery();
	}

	public GraphQueryResult evaluate()
		throws StoreException
	{
		TupleExpr tupleExpr = getParsedQuery().getTupleExpr();


		Cursor<? extends BindingSet> bindingsIter;

		SailConnection sailCon = getConnection().getSailConnection();
		bindingsIter = sailCon.evaluate(tupleExpr, getActiveDataset(), getBindings(), getIncludeInferred());

		// Filters out all partial and invalid matches
		bindingsIter = new FilteringCursor<BindingSet>(bindingsIter) {

			@Override
			protected boolean accept(BindingSet bindingSet) {
				Value context = bindingSet.getValue("context");

				return bindingSet.getValue("subject") instanceof Resource
				&& bindingSet.getValue("predicate") instanceof URI
				&& bindingSet.getValue("object") instanceof Value
				&& (context == null || context instanceof Resource);
			}

			@Override
			public String getName() {
				return "FilterOutPartialMatches";
			}
		};

		bindingsIter = enforceMaxQueryTime(bindingsIter);

		// Convert the BindingSet objects to actual RDF statements
		final ValueFactory vf = getConnection().getValueFactory();
		Cursor<Statement> stIter;
		stIter = new ConvertingCursor<BindingSet, Statement>(bindingsIter) {

			@Override
			protected Statement convert(BindingSet bindingSet) {
				Resource subject = (Resource)bindingSet.getValue("subject");
				URI predicate = (URI)bindingSet.getValue("predicate");
				Value object = bindingSet.getValue("object");
				Resource context = (Resource)bindingSet.getValue("context");

				if (context == null) {
					return vf.createStatement(subject, predicate, object);
				}
				else {
					return vf.createStatement(subject, predicate, object, context);
				}
			}

			@Override
			protected String getName() {
				return "CreateStatement";
			}
		};

		return new GraphQueryResultImpl(getParsedQuery().getQueryNamespaces(), stIter);
	}

	public void evaluate(RDFHandler handler)
		throws StoreException, RDFHandlerException
	{
		GraphQueryResult queryResult = evaluate();
		QueryResultUtil.report(queryResult, handler);
	}
}
