/* 
 * Licensed to Aduna under one or more contributor license agreements.  
 * See the NOTICE.txt file distributed with this work for additional 
 * information regarding copyright ownership. 
 *
 * Aduna licenses this file to you under the terms of the Aduna BSD 
 * License (the "License"); you may not use this file except in compliance 
 * with the License. See the LICENSE.txt file distributed with this work 
 * for the full License.
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or 
 * implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package org.openrdf.http.server;

import static org.junit.Assert.*;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import info.aduna.io.IOUtil;

import org.openrdf.http.protocol.Protocol;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.resultio.QueryResultIO;
import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.openrdf.rio.RDFFormat;

public class ProtocolTest {

	@Rule
	public TemporaryFolder tempDir = new TemporaryFolder();

	private TestServer server;

	@Before
	public void setUp()
		throws Exception
	{
		server = new TestServer();
		File testFolder = tempDir.newFolder("sesame-http-compliance-datadir");
		testFolder.mkdirs();
		server.start(testFolder);
	}

	@After
	public void tearDown()
		throws Exception
	{
		server.stop();
	}

	/**
	 * Tests the server's methods for updating all data in a repository.
	 */
	@Test
	public void testRepository_PUT()
		throws Exception
	{
		putFile(Protocol.getStatementsLocation(server.getRepositoryUrl()), "/testcases/default-graph-1.ttl");
	}

	/**
	 * Tests the server's methods for deleting all data in a repository.
	 */
	@Test
	public void testRepository_DELETE()
		throws Exception
	{
		delete(Protocol.getStatementsLocation(server.getRepositoryUrl()));
	}

	/**
	 * Tests the server's methods for updating the data in the default context of
	 * a repository.
	 */
	@Test
	public void testNullContext_PUT()
		throws Exception
	{
		String location = Protocol.getStatementsLocation(server.getRepositoryUrl());
		location += "?" + Protocol.CONTEXT_PARAM_NAME + "=" + Protocol.NULL_PARAM_VALUE;
		putFile(location, "/testcases/default-graph-1.ttl");
	}

	/**
	 * Tests the server's methods for deleting the data from the default context
	 * of a repository.
	 */
	@Test
	public void testNullContext_DELETE()
		throws Exception
	{
		String location = Protocol.getStatementsLocation(server.getRepositoryUrl());
		location += "?" + Protocol.CONTEXT_PARAM_NAME + "=" + Protocol.NULL_PARAM_VALUE;
		delete(location);
	}

	/**
	 * Tests the server's methods for updating the data in a named context of a
	 * repository.
	 */
	@Test
	public void testNamedContext_PUT()
		throws Exception
	{
		String location = Protocol.getStatementsLocation(server.getRepositoryUrl());
		String encContext = Protocol.encodeValue(new URIImpl("urn:x-local:graph1"));
		location += "?" + Protocol.CONTEXT_PARAM_NAME + "=" + encContext;
		putFile(location, "/testcases/named-graph-1.ttl");
	}

	/**
	 * Tests the server's methods for deleting the data from a named context of a
	 * repository.
	 */
	@Test
	public void testNamedContext_DELETE()
		throws Exception
	{
		String location = Protocol.getStatementsLocation(server.getRepositoryUrl());
		String encContext = Protocol.encodeValue(new URIImpl("urn:x-local:graph1"));
		location += "?" + Protocol.CONTEXT_PARAM_NAME + "=" + encContext;
		delete(location);
	}

	/**
	 * Tests the server's methods for quering a repository using GET requests to
	 * send SeRQL-select queries.
	 */
	@Test
	public void testSeRQLselect()
		throws Exception
	{
		TupleQueryResult queryResult = evaluate(server.getRepositoryUrl(), "select * from {X} P {Y}",
				QueryLanguage.SERQL);
		QueryResultIO.write(queryResult, TupleQueryResultFormat.SPARQL, System.out);
	}

	private void putFile(String location, String file)
		throws Exception
	{
		System.out.println("Put file to " + location);

		URL url = new URL(location);
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setRequestMethod("PUT");
		conn.setDoOutput(true);

		RDFFormat dataFormat = RDFFormat.forFileName(file, RDFFormat.RDFXML);
		conn.setRequestProperty("Content-Type", dataFormat.getDefaultMIMEType());

		InputStream dataStream = ProtocolTest.class.getResourceAsStream(file);
		try {
			OutputStream connOut = conn.getOutputStream();

			try {
				IOUtil.transfer(dataStream, connOut);
			}
			finally {
				connOut.close();
			}
		}
		finally {
			dataStream.close();
		}

		conn.connect();

		int responseCode = conn.getResponseCode();

		if (responseCode != HttpURLConnection.HTTP_OK && // 200 OK
				responseCode != HttpURLConnection.HTTP_NO_CONTENT) // 204 NO CONTENT
		{
			String response = "location " + location + " responded: " + conn.getResponseMessage() + " ("
					+ responseCode + ")";
			fail(response);
		}
	}

	private void delete(String location)
		throws Exception
	{
		URL url = new URL(location);
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setRequestMethod("DELETE");

		conn.connect();

		int responseCode = conn.getResponseCode();

		if (responseCode != HttpURLConnection.HTTP_OK && // 200 OK
				responseCode != HttpURLConnection.HTTP_NO_CONTENT) // 204 NO CONTENT
		{
			String response = "location " + location + " responded: " + conn.getResponseMessage() + " ("
					+ responseCode + ")";
			fail(response);
		}
	}

	private TupleQueryResult evaluate(String location, String query, QueryLanguage queryLn)
		throws Exception
	{
		location += "?query=" + URLEncoder.encode(query, "UTF-8") + "&queryLn=" + queryLn.getName();

		URL url = new URL(location);

		HttpURLConnection conn = (HttpURLConnection)url.openConnection();

		// Request SPARQL-XML formatted results:
		conn.setRequestProperty("Accept", TupleQueryResultFormat.SPARQL.getDefaultMIMEType());

		conn.connect();

		try {
			int responseCode = conn.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				// Process query results
				return QueryResultIO.parse(conn.getInputStream(), TupleQueryResultFormat.SPARQL);
			}
			else {
				String response = "location " + location + " responded: " + conn.getResponseMessage() + " ("
						+ responseCode + ")";
				fail(response);
				throw new RuntimeException(response);
			}
		}
		finally {
			conn.disconnect();
		}
	}
}
