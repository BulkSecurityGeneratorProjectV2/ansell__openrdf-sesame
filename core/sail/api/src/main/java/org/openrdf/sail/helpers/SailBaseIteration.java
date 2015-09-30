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

import info.aduna.iteration.CloseableIteration;
import info.aduna.iteration.IterationWrapper;

/**
 * An iteration extension that keeps a reference to the AbstractSailConnection from
 * which it originates and signals when it is closed.
 * 
 * @author Jeen Broekstra
 */
class SailBaseIteration<T, E extends Exception> extends IterationWrapper<T, E> {

	private final AbstractSailConnection connection;

	/**
	 * Creates a new memory-store specific iteration object.
	 * 
	 * @param lock
	 *        a query lock
	 * @param iter
	 *        the wrapped iteration over sail objects.
	 * @param connection
	 *        the connection from which this iteration originates.
	 */
	public SailBaseIteration(CloseableIteration<? extends T, ? extends E> iter, AbstractSailConnection connection)
	{
		super(iter);
		this.connection = connection;
	}

	@Override
	public boolean hasNext()
		throws E
	{
		if (super.hasNext()) {
			return true;
		}
		else {
			// auto-close when exhausted
			close();
			return false;
		}
	}

	@Override
	protected void handleClose()
		throws E
	{
		super.handleClose();
		connection.iterationClosed(this);
	}

	@Deprecated
	protected void forceClose()
		throws E
	{
		close();
	}
}
