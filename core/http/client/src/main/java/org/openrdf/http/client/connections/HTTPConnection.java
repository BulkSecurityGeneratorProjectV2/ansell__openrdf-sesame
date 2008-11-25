/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2002-2008.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.http.client.connections;

import static org.openrdf.http.protocol.Protocol.ACCEPT_PARAM_NAME;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Set;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HeaderElement;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.aduna.net.http.HttpClientUtil;

import org.openrdf.http.protocol.Protocol;
import org.openrdf.http.protocol.exceptions.HTTPException;
import org.openrdf.http.protocol.exceptions.NoCompatibleMediaType;
import org.openrdf.query.TupleQueryResultHandler;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.resultio.BooleanQueryResultFormat;
import org.openrdf.query.resultio.BooleanQueryResultParser;
import org.openrdf.query.resultio.BooleanQueryResultParserRegistry;
import org.openrdf.query.resultio.QueryResultIO;
import org.openrdf.query.resultio.QueryResultParseException;
import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.openrdf.query.resultio.TupleQueryResultParser;
import org.openrdf.query.resultio.TupleQueryResultParserRegistry;
import org.openrdf.query.resultio.UnsupportedQueryResultFormatException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFParserRegistry;
import org.openrdf.rio.Rio;
import org.openrdf.rio.UnsupportedRDFormatException;

/**
 * Serialises Java Objects over an HTTP connection.
 * 
 * @author Herko ter Horst
 * @author Arjohn Kampman
 * @author James Leigh
 */
public class HTTPConnection {

	private Logger logger = LoggerFactory.getLogger(HTTPConnection.class);

	private HTTPConnectionPool pool;

	private HttpMethod method;

	public HTTPConnection(HTTPConnectionPool pool, HttpMethod method) {
		this.pool = pool;
		this.method = method;
	}

	public void accept(Class<?> type)
		throws NoCompatibleMediaType
	{
		if (String.class.isAssignableFrom(type)) {
			acceptString();
		} else {
			throw new NoCompatibleMediaType("No parsers are available for " + type);
		}
	}

	public void acceptString() {
		method.addRequestHeader(ACCEPT_PARAM_NAME, "text/plain");
	}

	public void acceptBoolean()
		throws NoCompatibleMediaType
	{
		// Specify which formats we support using Accept headers
		Set<BooleanQueryResultFormat> booleanFormats = BooleanQueryResultParserRegistry.getInstance().getKeys();
		if (booleanFormats.isEmpty()) {
			throw new NoCompatibleMediaType("No boolean query result parsers have been registered");
		}

		for (BooleanQueryResultFormat format : booleanFormats) {
			// Determine a q-value that reflects the user specified preference
			int qValue = 10;

			TupleQueryResultFormat preferredBQRFormat = pool.getPreferredTupleQueryResultFormat();
			if (preferredBQRFormat != null && !preferredBQRFormat.equals(format)) {
				// Prefer specified format over other formats
				qValue -= 2;
			}

			for (String mimeType : format.getMIMETypes()) {
				String acceptParam = mimeType;

				if (qValue < 10) {
					acceptParam += ";q=0." + qValue;
				}

				method.addRequestHeader(ACCEPT_PARAM_NAME, acceptParam);
			}
		}
	}

	public void acceptTuple()
		throws NoCompatibleMediaType
	{
		// Specify which formats we support using Accept headers
		Set<TupleQueryResultFormat> tqrFormats = TupleQueryResultParserRegistry.getInstance().getKeys();
		if (tqrFormats.isEmpty()) {
			throw new NoCompatibleMediaType("No tuple query result parsers have been registered");
		}

		for (TupleQueryResultFormat format : tqrFormats) {
			// Determine a q-value that reflects the user specified preference
			int qValue = 10;

			TupleQueryResultFormat preferredTQRFormat = pool.getPreferredTupleQueryResultFormat();
			if (preferredTQRFormat != null && !preferredTQRFormat.equals(format)) {
				// Prefer specified format over other formats
				qValue -= 2;
			}

			for (String mimeType : format.getMIMETypes()) {
				String acceptParam = mimeType;

				if (qValue < 10) {
					acceptParam += ";q=0." + qValue;
				}

				method.addRequestHeader(ACCEPT_PARAM_NAME, acceptParam);
			}
		}
	}

	public void acceptRDF(boolean requireContext)
		throws NoCompatibleMediaType
	{
		// Specify which formats we support using Accept headers
		Set<RDFFormat> rdfFormats = RDFParserRegistry.getInstance().getKeys();
		if (rdfFormats.isEmpty()) {
			throw new NoCompatibleMediaType("No tuple RDF parsers have been registered");
		}

		for (RDFFormat format : rdfFormats) {
			// Determine a q-value that reflects the necessity of context
			// support and the user specified preference
			int qValue = 10;

			if (requireContext && !format.supportsContexts()) {
				// Prefer context-supporting formats over pure triple-formats
				qValue -= 5;
			}

			RDFFormat preferredRDFFormat = pool.getPreferredRDFFormat();
			if (preferredRDFFormat != null && !preferredRDFFormat.equals(format)) {
				// Prefer specified format over other formats
				qValue -= 2;
			}

			if (!format.supportsNamespaces()) {
				// We like reusing namespace prefixes
				qValue -= 1;
			}

			for (String mimeType : format.getMIMETypes()) {
				String acceptParam = mimeType;

				if (qValue < 10) {
					acceptParam += ";q=0." + qValue;
				}

				method.addRequestHeader(ACCEPT_PARAM_NAME, acceptParam);
			}
		}
	}

	public void send(Object instance) {
		if (instance instanceof String) {
			sendString((String) instance);
		} else {
			throw new IllegalArgumentException();
		}
	}

	public void sendString(String plain) {
		try {
			((PutMethod)method).setRequestEntity(new StringRequestEntity(plain, "text/plain", "UTF-8"));
		}
		catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
	}

	public void sendForm(List<NameValuePair> queryParams) {
		method.setRequestHeader("Content-Type", Protocol.FORM_MIME_TYPE + "; charset=utf-8");
		((PostMethod)method).setRequestBody(queryParams.toArray(new NameValuePair[queryParams.size()]));
	}

	public void sendQueryString(List<NameValuePair> params) {
		method.setQueryString(params.toArray(new NameValuePair[params.size()]));
	}

	public void sendEntity(RequestEntity requestEntity) {
		((EntityEnclosingMethod)method).setRequestEntity(requestEntity);
	}

	public void execute()
		throws IOException, HTTPException
	{
		int statusCode = pool.executeMethod(method);
		if (!HttpClientUtil.is2xx(statusCode)) {
			throw HTTPException.create(statusCode, method.getResponseBodyAsString());
		}
	}

	public <T> T read(Class<T> type)
		throws IOException, NumberFormatException, QueryResultParseException, RDFParseException,
		NoCompatibleMediaType
	{
		if (String.class.isAssignableFrom(type)) {
			return type.cast(readString());
		} else {
			throw new NoCompatibleMediaType("Cannot read " + type);
		}
	}

	public String readString()
		throws IOException
	{
		if (method.getStatusCode() == HttpURLConnection.HTTP_NOT_FOUND)
			return null;
		return method.getResponseBodyAsString();
	}

	public long readLong()
		throws IOException, NumberFormatException
	{
		return Long.parseLong(method.getResponseBodyAsString());
	}

	public boolean readBoolean()
		throws IOException, QueryResultParseException, NoCompatibleMediaType
	{
		String mimeType = readContentType();
		try {
			Set<BooleanQueryResultFormat> booleanFormats = BooleanQueryResultParserRegistry.getInstance().getKeys();
			BooleanQueryResultFormat format = BooleanQueryResultFormat.matchMIMEType(mimeType, booleanFormats);
			BooleanQueryResultParser parser = QueryResultIO.createParser(format);
			return parser.parse(method.getResponseBodyAsStream());
		}
		catch (UnsupportedQueryResultFormatException e) {
			logger.warn(e.toString(), e);
			throw new NoCompatibleMediaType("Server responded with an unsupported file format: "
					+ mimeType);
		}
	}

	public void readTuple(TupleQueryResultHandler handler)
		throws TupleQueryResultHandlerException, IOException, QueryResultParseException,
		NoCompatibleMediaType
	{
		String mimeType = readContentType();
		try {
			Set<TupleQueryResultFormat> tqrFormats = TupleQueryResultParserRegistry.getInstance().getKeys();
			TupleQueryResultFormat format = TupleQueryResultFormat.matchMIMEType(mimeType, tqrFormats);
			TupleQueryResultParser parser = QueryResultIO.createParser(format, pool.getValueFactory());
			parser.setTupleQueryResultHandler(handler);
			parser.parse(method.getResponseBodyAsStream());
		}
		catch (UnsupportedQueryResultFormatException e) {
			logger.warn(e.toString(), e);
			throw new NoCompatibleMediaType("Server responded with an unsupported file format: "
					+ mimeType);
		}
	}

	public void readRDF(RDFHandler handler)
		throws RDFHandlerException, IOException, RDFParseException, NoCompatibleMediaType
	{
		String mimeType = readContentType();
		try {
			Set<RDFFormat> rdfFormats = RDFParserRegistry.getInstance().getKeys();
			RDFFormat format = RDFFormat.matchMIMEType(mimeType, rdfFormats);
			RDFParser parser = Rio.createParser(format, pool.getValueFactory());
			parser.setPreserveBNodeIDs(true);
			parser.setRDFHandler(handler);
			parser.parse(method.getResponseBodyAsStream(), method.getURI().getURI());
		}
		catch (UnsupportedRDFormatException e) {
			logger.warn(e.toString(), e);
			throw new NoCompatibleMediaType("Server responded with an unsupported file format: "
					+ mimeType);
		}
	}

	public void release() {
		try {
			// Read the entire response body to enable the reuse of the connection
			InputStream responseStream = method.getResponseBodyAsStream();
			if (responseStream != null) {
				while (responseStream.read() >= 0) {
					// do nothing
				}
			}

			method.releaseConnection();
		}
		catch (IOException e) {
			logger.warn("I/O error upon releasing connection", e);
		}
	}

	/**
	 * Gets the MIME type specified in the response headers of the supplied
	 * method, if any. For example, if the response headers contain
	 * <tt>Content-Type: application/xml;charset=UTF-8</tt>, this method will
	 * return <tt>application/xml</tt> as the MIME type.
	 * 
	 * @param method
	 *        The method to get the reponse MIME type from.
	 * @return The response MIME type, or <tt>null</tt> if not available.
	 */
	protected String readContentType()
		throws IOException
	{
		Header[] headers = method.getResponseHeaders("Content-Type");

		for (Header header : headers) {
			HeaderElement[] headerElements = header.getElements();

			for (HeaderElement headerEl : headerElements) {
				String mimeType = headerEl.getName();
				if (mimeType != null) {
					logger.debug("reponse MIME type is {}", mimeType);
					return mimeType;
				}
			}
		}

		return null;
	}

}
