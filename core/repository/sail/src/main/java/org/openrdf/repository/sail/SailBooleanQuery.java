/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2007-2008.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.repository.sail;

import org.openrdf.StoreException;
import org.openrdf.query.BindingSet;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.Cursor;
import org.openrdf.query.Dataset;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.parser.ParsedBooleanQuery;
import org.openrdf.sail.SailConnection;

/**
 * @author Arjohn Kampman
 */
public class SailBooleanQuery extends SailQuery implements BooleanQuery {

	protected SailBooleanQuery(ParsedBooleanQuery tupleQuery, SailRepositoryConnection sailConnection) {
		super(tupleQuery, sailConnection);
	}

	@Override
	public ParsedBooleanQuery getParsedQuery() {
		return (ParsedBooleanQuery)super.getParsedQuery();
	}

	public boolean evaluate()
		throws StoreException
	{
		ParsedBooleanQuery parsedBooleanQuery = getParsedQuery();
		TupleExpr tupleExpr = parsedBooleanQuery.getTupleExpr();
		Dataset dataset = getDataset();
		if (dataset == null) {
			// No external dataset specified, use query's own dataset (if any)
			dataset = parsedBooleanQuery.getDataset();
		}

		SailConnection sailCon = getConnection().getSailConnection();

		Cursor<? extends BindingSet> bindingsIter;
		bindingsIter = sailCon.evaluate(tupleExpr, dataset, getBindings(), getIncludeInferred());

		bindingsIter = enforceMaxQueryTime(bindingsIter);

		try {
			return bindingsIter.next() != null;
		}
		finally {
			bindingsIter.close();
		}
	}
}
