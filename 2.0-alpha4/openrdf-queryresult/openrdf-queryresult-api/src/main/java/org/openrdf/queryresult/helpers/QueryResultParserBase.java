/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 1997-2006.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.queryresult.helpers;

import org.openrdf.queryresult.TupleQueryResultHandler;
import org.openrdf.queryresult.TupleQueryResultParser;

import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;


/**
 * Base class for {@link TupleQueryResultParser}s offering common functionality for
 * query result parsers.
 */
public abstract class QueryResultParserBase implements TupleQueryResultParser {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The ValueFactory to use for creating RDF model objects.
	 */
	protected ValueFactory _valueFactory;

	/**
	 * The TupleQueryResultHandler that will handle the parsed query results.
	 */
	protected TupleQueryResultHandler _handler;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new parser base that, by default, will use an instance of
	 * {@link ValueFactoryImpl} to create Value objects.
	 */
	public QueryResultParserBase() {
		this(new ValueFactoryImpl());
	}

	/**
	 * Creates a new parser base that will use the supplied ValueFactory to
	 * create Value objects.
	 */
	public QueryResultParserBase(ValueFactory valueFactory) {
		setValueFactory(valueFactory);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public void setValueFactory(ValueFactory valueFactory) {
		_valueFactory = valueFactory;
	}

	public void setTupleQueryResultHandler(TupleQueryResultHandler handler) {
		_handler = handler;
	}
}
