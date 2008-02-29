package org.openrdf.sail.rdbms.mysql;

import java.sql.SQLException;

import org.openrdf.sail.rdbms.schema.RdbmsTable;

public class MySqlTable extends RdbmsTable {

	public MySqlTable(String name) {
		super(name);
	}

	@Override
	protected String buildCreateTransactionalTable(CharSequence columns) {
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE TABLE ").append(getName());
		sb.append(" (\n").append(columns).append(")");
		sb.append(" type = InnoDB");
		return sb.toString();
	}

	@Override
	protected String buildLongIndex(String... columns) {
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE INDEX ").append(buildIndexName(columns));
		sb.append(" ON ").append(getName());
		sb.append(" (value(1024))");
		return sb.toString();
	}

	@Override
	protected String buildOptimize() throws SQLException {
		return "OPTIMIZE TABLE " + getName();
	}

}
