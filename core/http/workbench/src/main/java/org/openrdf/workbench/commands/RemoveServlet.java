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
package org.openrdf.workbench.commands;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openrdf.http.protocol.Protocol;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.IRI;
import org.openrdf.model.Value;
import org.openrdf.query.QueryResultHandlerException;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.workbench.base.TransformationServlet;
import org.openrdf.workbench.exceptions.BadRequestException;
import org.openrdf.workbench.util.TupleResultBuilder;
import org.openrdf.workbench.util.WorkbenchRequest;

public class RemoveServlet extends TransformationServlet {

	private final Logger logger = LoggerFactory.getLogger(RemoveServlet.class);

	@Override
	protected void doPost(WorkbenchRequest req, HttpServletResponse resp, String xslPath)
		throws IOException, RepositoryException, QueryResultHandlerException
	{
		String objectParameter = req.getParameter("obj");
		try {
			RepositoryConnection con = repository.getConnection();
			try {
				Resource subj = req.getResource("subj");
				IRI pred = req.getURI("pred");
				Value obj = req.getValue("obj");
				if (subj == null && pred == null && obj == null && !req.isParameterPresent(CONTEXT)) {
					throw new BadRequestException("No values");
				}
				remove(con, subj, pred, obj, req);
				// HACK: HTML sends \r\n, but SAX strips out the \r, try both ways
				if (obj instanceof Literal && obj.stringValue().contains("\r\n")) {
					obj = Protocol.decodeValue(objectParameter.replace("\r\n", "\n"), con.getValueFactory());
					remove(con, subj, pred, obj, req);
				}
			}
			catch (ClassCastException exc) {
				throw new BadRequestException(exc.getMessage(), exc);
			}
			finally {
				con.close();
			}
			resp.sendRedirect("summary");
		}
		catch (BadRequestException exc) {
			logger.warn(exc.toString(), exc);
			TupleResultBuilder builder = getTupleResultBuilder(req, resp, resp.getOutputStream());
			builder.transform(xslPath, "remove.xsl");
			builder.start("error-message", "subj", "pred", "obj", CONTEXT);
			builder.link(Arrays.asList(INFO));
			builder.result(exc.getMessage(), req.getParameter("subj"), req.getParameter("pred"),
					objectParameter, req.getParameter(CONTEXT));
			builder.end();
		}
	}

	private void remove(RepositoryConnection con, Resource subj, IRI pred, Value obj, WorkbenchRequest req)
		throws BadRequestException, RepositoryException
	{
		if (req.isParameterPresent(CONTEXT)) {
			Resource ctx = req.getResource(CONTEXT);
			if (subj == null && pred == null && obj == null) {
				con.clear(ctx);
			}
			else {
				con.remove(subj, pred, obj, ctx);
			}
		}
		else {
			con.remove(subj, pred, obj);
		}
	}

	@Override
	public void service(TupleResultBuilder builder, String xslPath)
		throws RepositoryException, QueryResultHandlerException
	{
		builder.transform(xslPath, "remove.xsl");
		builder.start();
		builder.link(Arrays.asList(INFO));
		builder.end();
	}

}