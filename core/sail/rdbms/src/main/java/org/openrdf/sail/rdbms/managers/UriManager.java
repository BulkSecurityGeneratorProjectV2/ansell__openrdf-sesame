/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2008.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.sail.rdbms.managers;

import java.sql.SQLException;

import org.openrdf.sail.rdbms.managers.base.ValueManagerBase;
import org.openrdf.sail.rdbms.model.RdbmsURI;
import org.openrdf.sail.rdbms.schema.IdCode;
import org.openrdf.sail.rdbms.schema.URITable;

/**
 * Manages URIs. Including creating, inserting, and looking up their
 * IDs.
 * 
 * @author James Leigh
 * 
 */
public class UriManager extends ValueManagerBase<RdbmsURI> {
	public static UriManager instance;
	private URITable table;

	public UriManager() {
		instance = this;
	}

	public void setUriTable(URITable shorter) {
		this.table = shorter;
	}

	@Override
	public void close()
		throws SQLException
	{
		super.close();
		table.close();
	}

	@Override
	public void removedStatements(int count, String condition)
		throws SQLException
	{
		super.removedStatements(count, condition);
		table.removedStatements(count, condition);
	}

	@Override
	protected int getTableVersion() {
		return table.getVersion();
	}

	@Override
	protected int getBatchSize() {
		return table.getBatchSize();
	}

	@Override
	protected String key(RdbmsURI value) {
		return value.stringValue();
	}

	@Override
	protected void insert(long id, RdbmsURI resource) throws SQLException, InterruptedException {
		String uri = resource.stringValue();
		if (IdCode.valueOf(id).isLong()) {
			table.insertLong(id, uri);
		} else {
			table.insertShort(id, uri);
		}
	}

	@Override
	protected long getMissingId(RdbmsURI value) {
		return IdCode.valueOf(value).hash(value);
	}

	@Override
	protected void optimize() throws SQLException {
		super.optimize();
		table.optimize();
	}

}
