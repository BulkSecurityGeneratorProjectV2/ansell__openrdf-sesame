/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2008.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.sail.rdbms.schema;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.concurrent.BlockingQueue;

/**
 * Manages the rows in a value table. These tables have two columns: an internal
 * id column and a value column.
 * 
 * @author James Leigh
 * 
 */
public class ValueTable {
	public static int BATCH_SIZE = 8 * 1024;
	public static final long NIL_ID = 0;
	private static final String[] PKEY = { "id" };
	private static final String[] VALUE_INDEX = { "value" };
	private int length = -1;
	private int sqlType;
	private String INSERT;
	private String INSERT_SELECT;
	private String EXPUNGE;
	private RdbmsTable table;
	private RdbmsTable temporary;
	private int removedStatementsSinceExpunge;
	private ValueBatch batch;
	private BlockingQueue<Batch> queue;
	private boolean indexingValues;

	public void setQueue(BlockingQueue<Batch> queue) {
		this.queue = queue;
	}

	public boolean isIndexingValues() {
		return indexingValues;
	}

	public void setIndexingValues(boolean indexingValues) {
		this.indexingValues = indexingValues;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public int getSqlType() {
		return sqlType;
	}

	public void setSqlType(int sqlType) {
		this.sqlType = sqlType;
	}

	public RdbmsTable getRdbmsTable() {
		return table;
	}

	public void setRdbmsTable(RdbmsTable table) {
		this.table = table;
	}

	public RdbmsTable getTemporaryTable() {
		return temporary;
	}

	public void setTemporaryTable(RdbmsTable temporary) {
		this.temporary = temporary;
	}

	public String getName() {
		return table.getName();
	}

	public long size() {
		return table.size();
	}

	public int getBatchSize() {
		return BATCH_SIZE;
	}

	public void initialize() throws SQLException {
		StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO ").append(temporary.getName());
		sb.append(" (id, value) VALUES (?, ?)");
		INSERT = sb.toString();
		sb.delete(0, sb.length());
		sb.append("INSERT INTO ").append(table.getName());
		sb.append(" (id, value) SELECT DISTINCT id, value FROM ");
		sb.append(temporary.getName()).append(" tmp\n");
		sb.append("WHERE NOT EXISTS (SELECT id FROM ").append(table.getName());
		sb.append(" val WHERE val.id = tmp.id)");
		INSERT_SELECT = sb.toString();
		sb.delete(0, sb.length());
		sb.append("DELETE FROM ").append(table.getName()).append("\n");
		sb.append("WHERE 1=1 ");
		EXPUNGE = sb.toString();
		if (!table.isCreated()) {
			createTable(table);
			table.index(PKEY);
			if (isIndexingValues()) {
				table.index(VALUE_INDEX);
			}
		} else {
			table.count();
		}
		if (!temporary.isCreated()) {
			createTemporaryTable(temporary);
		}
	}

	public void close() throws SQLException {
		// allow subclasses to override
	}

	public synchronized void insert(long id, Object value) throws SQLException, InterruptedException {
		ValueBatch batch = getValueBatch();
		if (isExpired(batch)) {
			batch = newValueBatch();
			initBatch(batch);
		}
		batch.setLong(1, id);
		batch.setObject(2, value);
		batch.addBatch();
		queue(batch);
	}

	public ValueBatch getValueBatch() {
		return this.batch;
	}

	public boolean isExpired(ValueBatch batch) {
		if (batch == null || batch.isFull())
			return true;
		return queue == null || !queue.remove(batch);
	}

	public ValueBatch newValueBatch() {
		return new ValueBatch();
	}

	public void initBatch(ValueBatch batch)
		throws SQLException
	{
		batch.setTable(table);
		batch.setTemporary(temporary);
		batch.setBatchStatement(prepareInsert(INSERT));
		batch.setMaxBatchSize(getBatchSize());
		batch.setInsertStatement(prepareInsertSelect(INSERT_SELECT));
	}

	public void queue(ValueBatch batch)
		throws SQLException, InterruptedException
	{
		if (queue == null) {
			batch.flush();
		} else {
			queue.put(batch);
		}
	}

	public void optimize() throws SQLException {
		table.optimize();
	}

	public boolean expungeRemovedStatements(int count, String condition)
			throws SQLException {
		removedStatementsSinceExpunge += count;
		if (condition != null && timeToExpunge()) {
			expunge(condition);
			removedStatementsSinceExpunge = 0;
			return true;
		}
		return false;
	}

	public long[] maxIds() throws SQLException {
		String column = "id";
		StringBuilder shift = new StringBuilder();
		shift.append("MOD((").append(column);
		shift.append(" >> ").append(IdCode.SHIFT);
		shift.append(") + ").append(IdCode.MOD).append(", ");
		shift.append(IdCode.MOD);
		shift.append(")");
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT ").append(shift);
		sb.append(", MAX(").append(column);
		sb.append("), COUNT(*)\n");
		sb.append("FROM ").append(getName());
		sb.append("\nGROUP BY ").append(shift);
		String query = sb.toString();
		PreparedStatement st = table.prepareStatement(query);
		try {
			ResultSet rs = st.executeQuery();
			try {
				long[] result = new long[IdCode.values().length];
				while (rs.next()) {
					int idx = rs.getInt(1);
					result[idx] = rs.getLong(2);
					assert IdCode.valueOf(result[idx]).equals(IdCode.values()[idx]);
				}
				return result;
			} finally {
				rs.close();
			}
		} finally {
			st.close();
		}
	}

	@Override
	public String toString() {
		return getName();
	}

	protected void expunge(String condition) throws SQLException {
		synchronized (table) {
			int count = table.executeUpdate(EXPUNGE + condition);
			table.modified(0, count);
		}
	}

	protected boolean timeToExpunge() {
		return removedStatementsSinceExpunge > table.size() / 4;
	}

	protected PreparedStatement prepareInsert(String sql) throws SQLException {
		return temporary.prepareStatement(sql);
	}

	protected PreparedStatement prepareInsertSelect(String sql) throws SQLException {
		return temporary.prepareStatement(sql);
	}

	protected void createTable(RdbmsTable table) throws SQLException {
		StringBuilder sb = new StringBuilder();
		sb.append("  id BIGINT NOT NULL,\n");
		sb.append("  value ").append(getDeclaredSqlType(sqlType, length));
		sb.append(" NOT NULL\n");
		table.createTable(sb);
	}

	protected void createTemporaryTable(RdbmsTable table) throws SQLException {
		StringBuilder sb = new StringBuilder();
		sb.append("  id BIGINT NOT NULL,\n");
		sb.append("  value ").append(getDeclaredSqlType(sqlType, length));
		sb.append(" NOT NULL\n");
		table.createTemporaryTable(sb);
	}

	protected String getDeclaredSqlType(int type, int length) {
		switch (sqlType) {
		case Types.VARCHAR:
			if (length > 0)
				return "VARCHAR(" + length + ")";
			return "TEXT";
		case Types.LONGVARCHAR:
			if (length > 0)
				return "LONGVARCHAR(" + length + ")";
			return "TEXT";
		case Types.BIGINT:
			return "BIGINT";
		case Types.INTEGER:
			return "INTEGER";
		case Types.SMALLINT:
			return "SMALLINT";
		case Types.FLOAT:
			return "FLOAT";
		case Types.DOUBLE:
			return "DOUBLE";
		case Types.DECIMAL:
			return "DECIMAL";
		case Types.BOOLEAN:
			return "BOOLEAN";
		case Types.TIMESTAMP:
			return "TIMESTAMP";
		default:
			throw new AssertionError("Unsupported SQL Type: " + type);
		}
	}
}
