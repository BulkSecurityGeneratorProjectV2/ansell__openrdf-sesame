/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 1997-2008.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.sail.helpers;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openrdf.StoreException;
import org.openrdf.sail.Sail;
import org.openrdf.sail.SailConnection;

/**
 * SailBase is an abstract Sail implementation that takes care of common sail
 * tasks, including proper closing of active connections and a grace period for
 * active connections during shutdown of the store.
 * 
 * @author Herko ter Horst
 * @author jeen
 * @author Arjohn Kampman
 */
public abstract class SailBase implements Sail {

	/*-----------*
	 * Constants *
	 *-----------*/

	protected final Logger logger = LoggerFactory.getLogger(this.getClass());

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * Directory to store information related to this sail in.
	 */
	private File dataDir;

	/**
	 * Flag indicating whether the Sail is shutting down.
	 */
	private boolean shutDownInProgress = false;

	private SailConnectionTracker tracker = new SailConnectionTracker(); 

	/*---------*
	 * Methods *
	 *---------*/

	public void setDataDir(File dataDir) {
		this.dataDir = dataDir;
	}

	public File getDataDir() {
		return dataDir;
	}

	public SailConnection getConnection()
		throws StoreException
	{
		if (shutDownInProgress) {
			throw new IllegalStateException("shut down in progress");
		}

		return tracker.track(getConnectionInternal());
	}

	/**
	 * returns a store-specific SailConnection object.
	 * 
	 * @return a SailConnection
	 * @throws StoreException
	 */
	protected abstract SailConnection getConnectionInternal()
		throws StoreException;

	public void shutDown()
		throws StoreException
	{
		// indicate no more new connections should be given out.
		shutDownInProgress = true;

		try {
			tracker.closeAll();
			
			shutDownInternal();
		}
		finally {
			shutDownInProgress = false;
		}
	}

	/**
	 * Do store-specific operations to ensure proper shutdown of the store.
	 * 
	 * @throws StoreException
	 */
	protected abstract void shutDownInternal()
		throws StoreException;
}
