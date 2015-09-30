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
package org.openrdf.http.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.openrdf.http.client.BackgroundGraphResult;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.helpers.AbstractRDFParser;

/**
 * @author Damyan Ognyanov
 */
public class BackgroundGraphResultHangTest {

	static class DummyParser extends AbstractRDFParser {

		@Override
		public RDFFormat getRDFFormat() {
			return null;
		}

		@Override
		public void parse(InputStream in, String baseURI)
			throws IOException, RDFParseException, RDFHandlerException
		{
			throw new RDFParseException("invalid RDF ");
		}

		@Override
		public void parse(Reader reader, String baseURI)
			throws IOException, RDFParseException, RDFHandlerException
		{
			throw new RDFParseException("invalid RDF ");
		}

	}

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test(timeout = 1000)
	public void testBGRHang()
		throws Exception
	{
		String data = "@not-rdf";

		BackgroundGraphResult gRes = new BackgroundGraphResult(new DummyParser(), new ByteArrayInputStream(
				data.getBytes(Charset.forName("UTF-8"))), Charset.forName("UTF-8"), "http://base.org");

		gRes.run();

		gRes.getNamespaces();

		thrown.expect(QueryEvaluationException.class);
		gRes.hasNext();
	}
}
