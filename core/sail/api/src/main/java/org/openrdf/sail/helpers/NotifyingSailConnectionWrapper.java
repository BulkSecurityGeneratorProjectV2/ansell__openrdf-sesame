/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 1997-2008.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.sail.helpers;

import org.openrdf.sail.NotifyingSailConnection;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailConnectionListener;

/**
 * An implementation of the Transaction interface that wraps another Transaction
 * object and forwards any method calls to the wrapped transaction.
 * 
 * @author jeen
 * @author James Leigh
 */
public class NotifyingSailConnectionWrapper extends SailConnectionWrapper implements NotifyingSailConnection {

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new wrapper object that wraps the supplied connection.
	 */
	public NotifyingSailConnectionWrapper(SailConnection wrappedCon) {
		super(wrappedCon);
	}

	/**
	 * Creates a new wrapper object that wraps the supplied connection and
	 * ensures the connection supports notifying.
	 */
	public NotifyingSailConnectionWrapper(NotifyingSailConnection wrappedCon) {
		super(wrappedCon);
		if (!wrappedCon.isNotifyingSupported())
			throw new IllegalArgumentException("Sail does not support notifying");
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected NotifyingSailConnection getWrappedConnection() {
		return (NotifyingSailConnection)super.getWrappedConnection();
	}

	public void addConnectionListener(SailConnectionListener listener) {
		getWrappedConnection().addConnectionListener(listener);
	}

	public void removeConnectionListener(SailConnectionListener listener) {
		getWrappedConnection().addConnectionListener(listener);
	}
}
