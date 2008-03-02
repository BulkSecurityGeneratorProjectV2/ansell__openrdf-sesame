/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2008.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.sail.rdbms.schema;

import static java.sql.Types.BIGINT;
import static java.sql.Types.DOUBLE;
import static java.sql.Types.VARCHAR;
import static java.sql.Types.LONGVARCHAR;

import java.sql.Connection;
import java.util.concurrent.BlockingQueue;

/**
 * Factory class used to create or load the database tables.
 * 
 * @author James Leigh
 * 
 */
public class RdbmsTableFactory {
	private static final int VCS = 127;
	private static final int VCL = 255;
	protected static final String LANGUAGES = "LANGUAGES";
	protected static final String NAMESPACES = "NAMESPACE_PREFIXES";
	protected static final String RESOURCES = "RESOURCES";
	protected static final String BNODE_VALUES = "BNODE_VALUES";
	protected static final String URI_VALUES = "URI_VALUES";
	protected static final String LURI_VALUES = "LONG_URI_VALUES";
	protected static final String LBS = "LABEL_VALUES";
	protected static final String LLBS = "LONG_LABEL_VALUES";
	protected static final String LANGS = "LANGUAGE_VALUES";
	protected static final String DTS = "DATATYPE_VALUES";
	protected static final String NUM_VALUES = "NUMERIC_VALUES";
	protected static final String TIMES = "DATETIME_VALUES";
	protected static final String TRANS_STATEMENTS = "TRANSACTION_STATEMENTS";

	public NamespacesTable createNamespacesTable(Connection conn) {
		return new NamespacesTable(createTable(conn, NAMESPACES));
	}

	public ResourceTable createBNodeTable(Connection conn, BlockingQueue<ValueBatch> queue) {
		ValueTable table = createValueTable(conn, queue, BNODE_VALUES, VARCHAR, VCS);
		return new ResourceTable(table);
	}

	public ResourceTable createURITable(Connection conn, BlockingQueue<ValueBatch> queue) {
		ValueTable table = createValueTable(conn, queue, URI_VALUES, VARCHAR, VCL);
		return new ResourceTable(table);
	}

	public ResourceTable createLongURITable(Connection conn, BlockingQueue<ValueBatch> queue) {
		ValueTable table = createValueTable(conn, queue, LURI_VALUES, LONGVARCHAR);
		return new ResourceTable(table);
	}

	public LiteralTable createLiteralTable(Connection conn, BlockingQueue<ValueBatch> queue) {
		ValueTable lbs = createValueTable(conn, queue, LBS, VARCHAR, VCL);
		ValueTable llbs = createValueTable(conn, queue, LLBS, LONGVARCHAR);
		ValueTable lgs = createValueTable(conn, queue, LANGS, VARCHAR, VCS);
		ValueTable dt = createValueTable(conn, queue, DTS, VARCHAR, VCL);
		ValueTable num = createValueTable(conn, queue, NUM_VALUES, DOUBLE);
		ValueTable dateTime = createValueTable(conn, queue, TIMES, BIGINT);
		LiteralTable literals = new LiteralTable();
		literals.setLabelTable(lbs);
		literals.setLongLabelTable(llbs);
		literals.setLanguageTable(lgs);
		literals.setDatatypeTable(dt);
		literals.setNumericTable(num);
		literals.setDateTimeTable(dateTime);
		return literals;
	}

	public TripleTable createTripleTable(Connection conn, String tableName) {
		RdbmsTable table = newTable(tableName);
		table.setConnection(conn);
		return new TripleTable(table);
	}

	public RdbmsTable createTemporaryTable(Connection conn) {
		return createTemporaryTable(conn, TRANS_STATEMENTS);
	}

	protected RdbmsTable createTemporaryTable(Connection conn, String name) {
		return createTable(conn, name);
	}

	protected RdbmsTable createTable(Connection conn, String name) {
		RdbmsTable table = newTable(name);
		table.setConnection(conn);
		return table;
	}

	protected RdbmsTable newTable(String name) {
		return new RdbmsTable(name);
	}

	protected ValueTable createValueTable(Connection conn, BlockingQueue<ValueBatch> queue, String name, int sqlType) {
		return createValueTable(conn, queue, name, sqlType, -1);
	}

	protected ValueTable createValueTable(Connection conn, BlockingQueue<ValueBatch> queue, String name, int sqlType,
			int length) {
		ValueTable table = newValueTable();
		table.setRdbmsTable(createTable(conn, name));
		table.setTemporaryTable(createTemporaryTable(conn, "INSERT_" + name));
		table.setQueue(queue);
		table.setSqlType(sqlType);
		table.setLength(length);
		return table;
	}

	protected ValueTable newValueTable() {
		return new ValueTable();
	}
}
