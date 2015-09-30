/*******************************************************************************
 * Copyright (c) 2015, Eclipse Foundation, Inc. and its licensors.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * - Neither the name of the Eclipse Foundation, Inc. nor the names of its
 *   contributors may be used to endorse or promote products derived from this
 *   software without specific prior written permission. 
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/
package org.openrdf.sail.helpers;

import org.openrdf.sail.NotifyingSailConnection;
import org.openrdf.sail.SailConnectionListener;

/**
 * An implementation of the {@link org.openrdf.sail.NotifyingSailConnection}
 * interface that wraps another {@link org.openrdf.sail.NotifyingSailConnection}
 * object and forwards any method calls to the wrapped transaction.
 * 
 * @author Jeen Broekstra
 */
public class NotifyingSailConnectionWrapper extends SailConnectionWrapper implements NotifyingSailConnection {

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new {@link NotifyingSailConnectionWrapper} object that wraps the
	 * supplied connection.
	 */
	public NotifyingSailConnectionWrapper(NotifyingSailConnection wrappedCon) {
		super(wrappedCon);
	}

	/*-----------------------*
	 * SailConnectionWrapper *
	 *-----------------------*/

	@Override
	public NotifyingSailConnection getWrappedConnection() {
		return (NotifyingSailConnection)super.getWrappedConnection();
	}

	/*-------------------------*
	 * NotifyingSailConnection *
	 *-------------------------*/

	/**
	 * Adds the given listener to the wrapped connection.
	 */
	@Override
	public void addConnectionListener(SailConnectionListener listener) {
		getWrappedConnection().addConnectionListener(listener);
	}

	/**
	 * Removes the given listener from the wrapped connection.
	 */
	@Override
	public void removeConnectionListener(SailConnectionListener listener) {
		getWrappedConnection().removeConnectionListener(listener);
	}
}
