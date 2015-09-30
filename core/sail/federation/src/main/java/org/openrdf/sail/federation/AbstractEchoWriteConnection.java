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
package org.openrdf.sail.federation;

import java.util.List;

import org.openrdf.model.Resource;
import org.openrdf.model.IRI;
import org.openrdf.model.Value;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.sail.SailException;

/**
 * Echos all write operations to all members.
 * 
 * @author James Leigh
 */
abstract class AbstractEchoWriteConnection extends AbstractFederationConnection {

	public AbstractEchoWriteConnection(Federation federation, List<RepositoryConnection> members) {
		super(federation, members);
	}

	@Override
	public void startTransactionInternal()
		throws SailException
	{
		excute(new Procedure() {

			public void run(RepositoryConnection con)
				throws RepositoryException
			{
				con.begin();
			}
		});
	}

	@Override
	public void rollbackInternal()
		throws SailException
	{
		excute(new Procedure() {

			public void run(RepositoryConnection con)
				throws RepositoryException
			{
				con.rollback();
			}
		});
	}

	@Override
	public void commitInternal()
		throws SailException
	{
		excute(new Procedure() {

			public void run(RepositoryConnection con)
				throws RepositoryException
			{
				con.commit();
			}
		});
	}

	public void setNamespaceInternal(final String prefix, final String name)
		throws SailException
	{
		excute(new Procedure() {

			public void run(RepositoryConnection con)
				throws RepositoryException
			{
				con.setNamespace(prefix, name);
			}
		});
	}

	@Override
	public void clearNamespacesInternal()
		throws SailException
	{
		excute(new Procedure() {

			public void run(RepositoryConnection con)
				throws RepositoryException
			{
				con.clearNamespaces();
			}
		});
	}

	@Override
	public void removeNamespaceInternal(final String prefix)
		throws SailException
	{
		excute(new Procedure() {

			public void run(RepositoryConnection con)
				throws RepositoryException
			{
				con.removeNamespace(prefix);
			}
		});
	}

	@Override
	public void removeStatementsInternal(final Resource subj, final IRI pred, final Value obj,
			final Resource... contexts)
		throws SailException
	{
		excute(new Procedure() {

			public void run(RepositoryConnection con)
				throws RepositoryException
			{
				con.remove(subj, pred, obj, contexts);
			}
		});
	}
}
