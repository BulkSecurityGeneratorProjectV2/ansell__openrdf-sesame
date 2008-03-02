/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2008.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.sail.rdbms.schema;

import static org.openrdf.sail.rdbms.schema.TripleTableManager.OTHER_PRED;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


/**
 * Manages and delegates to a collection of {@link TransactionTable}s.
 * 
 * @author James Leigh
 * 
 */
public class TransTableManager {
	public static final int POPULAR = 64;
	public static int BATCH_SIZE = 256;
	public static final boolean TEMPORARY_TABLE_USED = TripleTable.UNIQUE_INDEX_TRIPLES;
	private RdbmsTableFactory factory;
	private TripleTableManager triples;
	private RdbmsTable temporaryTable;
	private Map<Long, TransactionTable> tables = new HashMap<Long, TransactionTable>();
	private List<TransactionTable> list;
	private LinkedHashMap<TransactionTable, Boolean> popular = new LinkedHashMap<TransactionTable, Boolean>(POPULAR) {
		private static final long serialVersionUID = 4679235006378767952L;

		@Override
		protected boolean removeEldestEntry(Entry<TransactionTable, Boolean> eldest) {
			return size() > POPULAR;
		}};
	private int removedCount;
	private String fromDummy;
	private Connection conn;

	public void setConnection(Connection conn) {
		this.conn = conn;
	}

	public void setRdbmsTableFactory(RdbmsTableFactory factory) {
		this.factory = factory;
	}

	public void setStatementsTable(TripleTableManager predicateTableManager) {
		this.triples = predicateTableManager;
	}

	public void setFromDummyTable(String fromDummy) {
		this.fromDummy = fromDummy;
	}

	public int getBatchSize() {
		return BATCH_SIZE;
	}

	public int getAvgUncommittedRowCount() throws SQLException {
		List<TransactionTable> tables = getTables();
		if (tables.size() == 0)
			return 0;
		long sum = 0;
		for (TransactionTable table : tables) {
			sum += table.getUncommittedRowCount();
		}
		return (int) (sum / tables.size());
	}

	public void initialize() throws SQLException {
	}

	public void insert(long ctx, long subj, long pred, long obj)
			throws SQLException {
		getTable(pred).insert(ctx, subj, pred, obj);
	}

	public int flush() throws SQLException {
		int count = 0;
		for (TransactionTable table : getTables()) {
			count += table.flush();
		}
		return count;
	}

	public int flushUnpopular() throws SQLException {
		int count = 0;
		for (TransactionTable table : getTables()) {
			synchronized (popular) {
				if (popular.containsKey(table))
					continue;
			}
			if (!table.isReady())
				continue;
			count += table.flush();
		}
		return count;
	}

	public void close() throws SQLException {
		// allow subclasses to override
	}

	public String findTableName(long pred) throws SQLException {
		return triples.findTableName(pred);
	}

	public String getCombinedTableName() throws SQLException {
		String union = " UNION ALL ";
		StringBuilder sb = new StringBuilder(1024);
		sb.append("(");
		for (Long pred : triples.getPredicateIds()) {
			TripleTable predicate;
			try {
				predicate = triples.getPredicateTable(pred);
			} catch (SQLException e) {
				throw new AssertionError(e);
			}
			TransactionTable table = findTable(pred);
			if ((table == null || table.isEmpty()) && predicate.isEmpty())
				continue;
			sb.append("SELECT ctx, subj, ");
			if (predicate.isPredColumnPresent()) {
				sb.append(" pred,");
			} else {
				sb.append(pred).append(" AS pred,");
			}
			sb.append(" obj");
			sb.append("\nFROM ");
			sb.append(predicate.getName());
			sb.append(union);
		}
		if (sb.length() < union.length())
			return getEmptyTableName();
		sb.delete(sb.length() - union.length(), sb.length());
		sb.append(")");
		return sb.toString();
	}

	public String getTableName(long pred) throws SQLException {
		if (pred == ValueTable.NIL_ID)
			return getCombinedTableName();
		String tableName = triples.getTableName(pred);
		if (tableName == null)
			return getEmptyTableName();
		return tableName;
	}

	private String getEmptyTableName() {
		StringBuilder sb = new StringBuilder(256);
		sb.append("(");
		sb.append("SELECT ");
		sb.append(getZeroBigInt()).append(" AS ctx, ");
		sb.append(getZeroBigInt()).append(" AS subj, ");
		sb.append(getZeroBigInt()).append(" AS pred, ");
		sb.append(getZeroBigInt()).append(" AS obj ");
		sb.append(fromDummy);
		sb.append("\nWHERE 1=0");
		sb.append(")");
		return sb.toString();
	}

	protected String getZeroBigInt() {
		return "0";
	}

	public void committed(boolean locked) throws SQLException {
		synchronized (tables) {
			for (TransactionTable table : tables.values()) {
				table.committed();
			}
			list = null;
			tables.clear();
		}
		if (removedCount > 0) {
			triples.removed(removedCount, locked);
		}
	}

	public void removed(Long pred, int count) throws SQLException {
		getTable(pred).removed(count);
		removedCount += count;
	}

	protected TransactionTable getTable(long pred) throws SQLException {
		synchronized (tables) {
			TransactionTable table = tables.get(pred);
			if (table == null) {
				TripleTable predicate = triples.getPredicateTable(pred);
				Long key = pred;
				if (predicate.isPredColumnPresent()) {
					key = OTHER_PRED;
					table = tables.get(key);
					if (table != null)
						return table;
				}
				table = createTransactionTable(predicate);
				tables.put(key, table);
				list = null;
			}
			synchronized (popular) {
				popular.remove(table);
				popular.put(table, Boolean.TRUE);
			}
			return table;
		}
	}

	public Collection<Long> getPredicateIds() {
		return triples.getPredicateIds();
	}

	public boolean isPredColumnPresent(Long id) throws SQLException {
		if (id == ValueTable.NIL_ID)
			return true;
		return triples.getPredicateTable(id).isPredColumnPresent();
	}

	public ValueTypes getObjTypes(long pred) {
		TripleTable table = triples.getExistingTable(pred);
		if (table == null)
			return ValueTypes.UNKNOWN;
		return table.getObjTypes();
	}

	public ValueTypes getSubjTypes(long pred) {
		TripleTable table = triples.getExistingTable(pred);
		if (table == null)
			return ValueTypes.RESOURCE;
		return table.getSubjTypes();
	}

	public boolean isEmpty() throws SQLException {
		for (Long pred : triples.getPredicateIds()) {
			TripleTable predicate;
			try {
				predicate = triples.getPredicateTable(pred);
			} catch (SQLException e) {
				throw new AssertionError(e);
			}
			TransactionTable table = findTable(pred);
			if (table != null && !table.isEmpty() || !predicate.isEmpty())
				return false;
		}
		return true;
	}

	protected TransactionTable createTransactionTable(TripleTable predicate)
			throws SQLException {
		if (temporaryTable == null && TEMPORARY_TABLE_USED) {
			temporaryTable = createTemporaryTable(conn);
			if (!temporaryTable.isCreated()) {
				createTemporaryTable(temporaryTable);
			}
		}
		TransactionTable table = createTransactionTable();
		table.setPredicateTable(predicate);
		table.setTemporaryTable(temporaryTable);
		table.setConnection(conn);
		table.setBatchSize(getBatchSize());
		table.initialize();
		return table;
	}

	protected RdbmsTable createTemporaryTable(Connection conn) {
		return factory.createTemporaryTable(conn);
	}

	protected TransactionTable createTransactionTable() {
		return new TransactionTable();
	}

	protected void createTemporaryTable(RdbmsTable table) throws SQLException {
		StringBuilder sb = new StringBuilder();
		sb.append("  ctx BIGINT NOT NULL,\n");
		sb.append("  subj BIGINT NOT NULL,\n");
		sb.append("  pred BIGINT NOT NULL,\n");
		sb.append("  obj BIGINT NOT NULL\n");
		table.createTemporaryTable(sb);
	}

	private List<TransactionTable> getTables() {
		List<TransactionTable> list = this.list;
		synchronized (tables) {
			if (list == null || list.size() != tables.size()) {
				this.list = list = new ArrayList(tables.values());
			}
		}
		return list;
	}

	private TransactionTable findTable(Long pred) {
		synchronized (tables) {
			return tables.get(pred);
		}
	}

}
