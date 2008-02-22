/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2008.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.sail.rdbms.postgresql;

import java.sql.Connection;

import org.openrdf.sail.rdbms.schema.PredicateTable;
import org.openrdf.sail.rdbms.schema.RdbmsTable;
import org.openrdf.sail.rdbms.schema.RdbmsTableFactory;

/**
 * Overrides PostgreSQL specific table commands.
 * 
 * @author James Leigh
 * 
 */
public class PgSqlTableFactory extends RdbmsTableFactory {

	@Override
	public PgSqlValueTable createValueTable(RdbmsTable table, int sqlType,
			int length) {
		return new PgSqlValueTable(table, sqlType, length);
	}

	@Override
	protected RdbmsTable newTable(String name) {
		return new PgSqlTable(name);
	}

	@Override
	public PredicateTable createPredicateTable(Connection conn, String tableName) {
		return super.createPredicateTable(conn, tableName.toLowerCase());
	}
}
