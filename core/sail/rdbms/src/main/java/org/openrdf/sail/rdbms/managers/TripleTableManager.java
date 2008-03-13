/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2008.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.sail.rdbms.managers;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openrdf.sail.rdbms.schema.TripleTable;
import org.openrdf.sail.rdbms.schema.ValueTableFactory;

/**
 * Manages and delegates to the collection of {@link TripleTable}.
 * 
 * @author James Leigh
 * 
 */
public class TripleTableManager {
	private static final String DEFAULT_TABLE_PREFIX = "TRIPLES";
	private static final String OTHER_TRIPLES_TABLE = "TRIPLES";
	public static int MAX_TABLES = Integer.MAX_VALUE;//1000;
	public static final boolean INDEX_TRIPLES = true;
	public static Long OTHER_PRED = Long.valueOf(-1);
	private BNodeManager bnodes;
	private boolean closed;
	private Connection conn;
	private ValueTableFactory factory;
	private Thread initThread;
	private LiteralManager literals;
	private Logger logger = LoggerFactory.getLogger(TripleTableManager.class);
	private PredicateManager predicates;
	private LinkedList<TripleTable> queue = new LinkedList<TripleTable>();
	private Pattern tablePrefix = Pattern.compile("\\W(\\w*)\\W*$");
	private Map<Long, TripleTable> tables = new HashMap<Long, TripleTable>();
	private UriManager uris;
	private int maxTables = MAX_TABLES;
	private boolean indexingTriples = INDEX_TRIPLES;
	Exception exc;

	public TripleTableManager(ValueTableFactory factory) {
		this.factory = factory;
	}

	public void setBNodeManager(BNodeManager bnodeTable) {
		this.bnodes = bnodeTable;
	}

	public void setConnection(Connection conn) {
		this.conn = conn;
	}

	public void setLiteralManager(LiteralManager literalTable) {
		this.literals = literalTable;
	}

	public void setPredicateManager(PredicateManager predicates) {
		this.predicates = predicates;
	}

	public void setUriManager(UriManager uriTable) {
		this.uris = uriTable;
	}

	public int getMaxNumberOfTripleTables() {
		if (maxTables == Integer.MAX_VALUE)
			return 0;
		return maxTables + 1;
	}

	public void setMaxNumberOfTripleTables(int max) {
		if (max < 1) {
			maxTables = MAX_TABLES;
		} else {
			maxTables = max - 1;
		}
	}

	public boolean isIndexingTriples() {
		return indexingTriples;
	}

	public void setIndexingTriples(boolean indexingTriples) {
		this.indexingTriples = indexingTriples;
	}

	public void initialize() throws SQLException {
		putAll(findPredicateTables());
		initThread = new Thread(new Runnable() {
			public void run() {
				try {
					initThread();
				} catch (Exception e) {
					exc = e;
					logger.error(e.toString(), e);
				}
			}
		}, "table-initialize");
		initThread.start();
	}

	public void close() throws SQLException {
		closed = true;
		synchronized (queue) {
			queue.notify();
		}
		Iterator<Entry<Long, TripleTable>> iter;
		iter = tables.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<Long, TripleTable> next = iter.next();
			TripleTable table = next.getValue();
			if (table.isEmpty()) {
				predicates.remove(next.getKey());
				table.drop();
				iter.remove();
			}
		}
	}

	public void createTripleIndexes() throws SQLException {
		indexingTriples = true;
		for (TripleTable table : tables.values()) {
			if (!table.isIndexed()) {
				table.createIndex();
			}
		}
	}

	public void dropTripleIndexes() throws SQLException {
		indexingTriples = false;
		for (TripleTable table : tables.values()) {
			if (table.isIndexed()) {
				table.dropIndex();
			}
		}
	}

	public String findTableName(long pred) throws SQLException {
		return getPredicateTable(pred).getNameWhenReady();
	}

	public synchronized TripleTable getExistingTable(long pred) {
		if (tables.containsKey(pred))
			return tables.get(pred);
		return tables.get(OTHER_PRED);
	}

	public synchronized Collection<Long> getPredicateIds() {
		return new ArrayList<Long>(tables.keySet());
	}

	public synchronized TripleTable getPredicateTable(long pred) throws SQLException {
		assert pred != 0;
			if (tables.containsKey(pred))
				return tables.get(pred);
			if (tables.containsKey(OTHER_PRED))
				return tables.get(OTHER_PRED);
			String tableName = getNewTableName(pred);
			if (tables.size() >= maxTables) {
				tableName = OTHER_TRIPLES_TABLE;
			}
			TripleTable table = factory.createTripleTable(conn, tableName);
			if (tables.size() >= maxTables) {
				table.setPredColumnPresent(true);
				initTable(table);
				tables.put(OTHER_PRED, table);
			} else {
				initTable(table);
				tables.put(pred, table);
			}
			return table;
	}

	public synchronized String getTableName(long pred) throws SQLException {
		if (tables.containsKey(pred))
			return tables.get(pred).getNameWhenReady();
		if (tables.containsKey(OTHER_PRED))
			return tables.get(OTHER_PRED).getNameWhenReady();
		return null;
	}

	public void removed(int count, boolean locked) throws SQLException {
		String condition = null;
		if (locked) {
			condition = getExpungeCondition();
		}
		bnodes.removedStatements(count, condition);
		uris.removedStatements(count, condition);
		literals.removedStatements(count, condition);
	}

	protected Set<String> findAllTables() throws SQLException {
		Set<String> tables = new HashSet<String>();
		DatabaseMetaData metaData = conn.getMetaData();
		String c = null;
		String s = null;
		String n = null;
		String[] TYPE_TABLE = new String[] { "TABLE" };
		ResultSet rs = metaData.getTables(c, s, n, TYPE_TABLE);
		try {
			while (rs.next()) {
				String tableName = rs.getString(3);
				tables.add(tableName);
			}
			return tables;
		} finally {
			rs.close();
		}
	}

	protected Map<Long, TripleTable> findPredicateTables()
			throws SQLException {
		Map<Long, TripleTable> tables = new HashMap<Long, TripleTable>();
		Set<String> names = findPredicateTableNames();
		for (String tableName : names) {
			TripleTable table = factory.createTripleTable(conn, tableName);
			if (tableName.equalsIgnoreCase(OTHER_TRIPLES_TABLE)) {
				table.setPredColumnPresent(true);
			}
			if (indexingTriples && !table.isIndexed()) {
				table.createIndex();
			}
			table.reload();
			tables.put(key(tableName), table);
		}
		return tables;
	}

	protected Set<String> findTablesWithColumn(String column)
			throws SQLException {
		Set<String> tables = findTablesWithExactColumn(column.toUpperCase());
		if (tables.isEmpty())
			return findTablesWithExactColumn(column.toLowerCase());
		return tables;
	}

	protected Set<String> findTablesWithExactColumn(String column)
			throws SQLException {
		Set<String> tables = new HashSet<String>();
		DatabaseMetaData metaData = conn.getMetaData();
		String c = null;
		String s = null;
		String n = null;
		ResultSet rs = metaData.getColumns(c, s, n, column);
		try {
			while (rs.next()) {
				String tableName = rs.getString(3);
				tables.add(tableName);
			}
			return tables;
		} finally {
			rs.close();
		}
	}

	protected synchronized String getExpungeCondition() throws SQLException {
		StringBuilder sb = new StringBuilder(1024);
		for (Map.Entry<Long, TripleTable> e : tables.entrySet()) {
			sb.append("\nAND id != ").append(e.getKey());
			if (e.getValue().isEmpty())
				continue;
			sb.append(" AND NOT EXISTS (SELECT * FROM ");
			sb.append(e.getValue().getNameWhenReady());
			sb.append(" WHERE ctx = id OR subj = id OR obj = id");
			if (e.getValue().isPredColumnPresent()) {
				sb.append(" OR pred = id");
			}
			sb.append(")");
		}
		return sb.toString();
	}

	protected String getNewTableName(long pred) throws SQLException {
		String prefix = getTableNamePrefix(pred);
		String tableName = prefix + "_" + pred;
		return tableName;
	}

	protected long key(String tn) {
		if (tn.equalsIgnoreCase(OTHER_TRIPLES_TABLE))
			return OTHER_PRED;
		Long id = Long.valueOf(tn.substring(tn.lastIndexOf('_') + 1));
		assert id != 0;
		return id;
	}

	protected String getTableNamePrefix(long pred) throws SQLException {
		String uri = predicates.getPredicateUri(pred);
		if (uri == null)
			return DEFAULT_TABLE_PREFIX;
		Matcher m = tablePrefix.matcher(uri);
		if (!m.find())
			return DEFAULT_TABLE_PREFIX;
		String localName = m.group(1).replaceAll("^[^a-zA-Z]*", "");
		if (localName.length() == 0)
			return DEFAULT_TABLE_PREFIX;
		if (localName.length() > 16)
			return localName.substring(0, 16);
		return localName;
	}

	void initThread() throws SQLException, InterruptedException {
		logger.debug("Starting helper thread {}", initThread.getName());
		while (!closed) {
			TripleTable table = null;
			synchronized (queue) {
				if (queue.isEmpty()) {
					queue.wait();
				}
				if (!queue.isEmpty()) {
					table = queue.removeFirst();
				}
			}
			if (table != null) {
				table.initTable();
				table = null;
			}
		}
		logger.debug("Closing helper thread {}", initThread.getName());
	}

	private Set<String> findPredicateTableNames() throws SQLException {
		Set<String> names = findAllTables();
		names.retainAll(findTablesWithColumn("ctx"));
		names.retainAll(findTablesWithColumn("subj"));
		names.retainAll(findTablesWithColumn("obj"));
		return names;
	}

	private void initTable(TripleTable table) throws SQLException {
		if (exc != null)
			throwException();
		table.setIndexed(indexingTriples);
		synchronized (queue) {
			queue.add(table);
			queue.notify();
		}
	}

	private synchronized void putAll(Map<Long, TripleTable> t) {
		tables.putAll(t);
	}

	private void throwException() throws SQLException {
		if (exc instanceof SQLException) {
			SQLException e = (SQLException) exc;
			exc = null;
			throw e;
		} else if (exc instanceof RuntimeException) {
			RuntimeException e = (RuntimeException) exc;
			exc = null;
			throw e;
		}
	}
}
