/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2008.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.sail.rdbms.managers;

import java.sql.SQLException;

import org.openrdf.model.impl.URIImpl;
import org.openrdf.sail.rdbms.managers.base.ValueManagerBase;
import org.openrdf.sail.rdbms.model.RdbmsURI;
import org.openrdf.sail.rdbms.schema.IdCode;
import org.openrdf.sail.rdbms.schema.ResourceTable;

/**
 * Manages URIs. Including creating, inserting, and looking up their
 * IDs.
 * 
 * @author James Leigh
 * 
 */
public class UriManager extends ValueManagerBase<String, RdbmsURI> {
	public static UriManager instance;
	private ResourceTable shorter;
	private ResourceTable longer;

	public UriManager(ResourceTable shorter, ResourceTable longer) {
		this.shorter = shorter;
		this.longer = longer;
		instance = this;
	}

	@Override
	public int getIdVersion() {
		return shorter.getIdVersion() + longer.getIdVersion();
	}

	@Override
	protected void flushTable() throws SQLException {
		shorter.flush();
		longer.flush();
	}

	@Override
	protected int getBatchSize() {
		return shorter.getBatchSize();
	}

	@Override
	protected String key(RdbmsURI value) {
		return value.stringValue();
	}

	@Override
	protected void insert(long id, RdbmsURI resource) throws SQLException {
		String uri = resource.stringValue();
		if (IdCode.decode(id).isLong()) {
			longer.insert(id, uri);
		} else {
			shorter.insert(id, uri);
		}
	}

	@Override
	protected long getMissingId(RdbmsURI value) {
		String uri = value.stringValue();
		if (uri.length() > IdCode.LONG)
			return IdCode.URI_LONG.getId(uri);
		return IdCode.URI.getId(uri);
	}

	@Override
	protected void optimize() throws SQLException {
		shorter.optimize();
		longer.optimize();
	}

	@Override
	protected RdbmsURI createClosedSignal() {
		return new RdbmsURI(new URIImpl("urn:closedSignal"));
	}

}
