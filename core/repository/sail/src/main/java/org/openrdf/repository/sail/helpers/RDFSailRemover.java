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
package org.openrdf.repository.sail.helpers;

import java.util.Set;

import org.openrdf.OpenRDFUtil;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.IRI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.helpers.AbstractRDFHandler;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailException;
import org.openrdf.sail.UpdateContext;

/**
 * An Sail-specific RDFHandler that removes RDF data from a repository. To be
 * used in combination with SPARQL DELETE DATA only.
 * 
 * @author jeen
 */
class RDFSailRemover extends AbstractRDFHandler {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The connection to use for the remove operations.
	 */
	private final SailConnection con;

	private final ValueFactory vf;

	private final UpdateContext uc;

	/**
	 * The contexts to remove the statements from. If this variable is a
	 * non-empty array, statements will be removed from the corresponding
	 * contexts.
	 */
	private Resource[] contexts = new Resource[0];

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new RDFSailRemover object.
	 * 
	 * @param con
	 *        The connection to use for the remove operations.
	 */
	public RDFSailRemover(SailConnection con, ValueFactory vf, UpdateContext uc) {
		this.con = con;
		this.vf = vf;
		this.uc = uc;

	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Enforces the supplied contexts upon all statements that are reported to
	 * this RDFSailRemover.
	 * 
	 * @param contexts
	 *        the contexts to use. Use an empty array (not null!) to indicate no
	 *        context(s) should be enforced.
	 */
	public void enforceContext(Resource... contexts) {
		OpenRDFUtil.verifyContextNotNull(contexts);
		this.contexts = contexts;
	}

	/**
	 * Checks whether this RDFRemover enforces its contexts upon all statements
	 * that are reported to it.
	 * 
	 * @return <tt>true</tt> if it enforces its contexts, <tt>false</tt>
	 *         otherwise.
	 */
	public boolean enforcesContext() {
		return contexts.length != 0;
	}

	/**
	 * Gets the contexts that this RDFRemover enforces upon all statements that
	 * are reported to it (in case <tt>enforcesContext()</tt> returns
	 * <tt>true</tt>).
	 * 
	 * @return A Resource[] identifying the contexts, or <tt>null</tt> if no
	 *         contexts is enforced.
	 */
	public Resource[] getContexts() {
		return contexts;
	}

	@Override
	public void handleStatement(Statement st)
		throws RDFHandlerException
	{
		Resource subj = st.getSubject();
		IRI pred = st.getPredicate();
		Value obj = st.getObject();
		Resource ctxt = st.getContext();

		try {
			if (enforcesContext()) {
				con.removeStatement(uc, subj, pred, obj, contexts);
			}
			else {
				if (ctxt == null) {
					final Set<IRI> removeGraphs = uc.getDataset().getDefaultRemoveGraphs();
					if (!removeGraphs.isEmpty()) {
						con.removeStatement(uc, subj, pred, obj, new IRI[removeGraphs.size()]);
					}
					else {
						con.removeStatement(uc, subj, pred, obj);
					}
				}
				else {
					con.removeStatement(uc, subj, pred, obj, ctxt);
				}
			}
		}
		catch (SailException e) {
			throw new RDFHandlerException(e);
		}
	}
}
