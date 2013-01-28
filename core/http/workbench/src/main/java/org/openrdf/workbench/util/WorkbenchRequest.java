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
package org.openrdf.workbench.util;

import static java.net.URLDecoder.decode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.aduna.iteration.Iterations;

import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.workbench.exceptions.BadRequestException;

public class WorkbenchRequest extends HttpServletRequestWrapper {

	private static final String UTF_8 = "UTF-8";

	private Logger logger = LoggerFactory.getLogger(WorkbenchRequest.class);

	private Map<String, String> parameters;

	private Map<String, String> defaults;

	private Repository repository;

	private ValueFactory vf;

	private InputStream content;

	private String contentFileName;
	
	public WorkbenchRequest(Repository repository, HttpServletRequest request, Map<String, String> defaults)
		throws RepositoryException, IOException, FileUploadException
	{
		super(request);
		this.defaults = defaults;
		if (repository == null) {
			this.vf = new ValueFactoryImpl();
		}
		else {
			this.repository = repository;
			this.vf = repository.getValueFactory();
		}
		String url = request.getRequestURL().toString();
		if (ServletFileUpload.isMultipartContent(this)) {
			parameters = getMultipartParameterMap();
		}
		else if (request.getQueryString() == null && url.contains(";")) {
			parameters = getUrlParameterMap(url);
		}
	}

	public InputStream getContentParameter()
		throws RepositoryException, BadRequestException, IOException, FileUploadException
	{
		return content;
	}
	
	public String getContentFileName() {
		return contentFileName;
	}

	/***
	 * Get the integer value associated with the given parameter name. 
	 * Internally uses getParameter(String), so looks in this order:
	 * 1. the query parameters that were parsed at construction, using the 
	 * last value if multiple exist. 2. Request cookies. 3. The defaults. 
	 * 
	 * @returns the value of the parameter, or zero if it is not present
	 * @throws BadRequestException if the parameter is present but does not
	 * parse as an integer
	 */
	public int getInt(String name)
		throws BadRequestException
	{
		String limit = getParameter(name);
		if (limit == null || limit.length() == 0)
			return 0;
		try {
			return Integer.parseInt(limit);
		}
		catch (NumberFormatException exc) {
			throw new BadRequestException(exc.getMessage(), exc);
		}
	}

	@Override
	public String getParameter(String name) {
		if (parameters != null && parameters.containsKey(name))
			return parameters.get(name);
		String[] values = super.getParameterValues(name);
		if (values != null && values.length > 0)
			// use the last one as it maybe appended in js
			return values[values.length - 1];
		Cookie[] cookies = getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (name.equals(cookie.getName())) {
					return cookie.getValue();
				}
			}
		}
		if (defaults != null && defaults.containsKey(name))
			return defaults.get(name);
		return null;
	}

	@Override
	public String[] getParameterValues(String name) {
		if (parameters != null && parameters.containsKey(name))
			return new String[] { parameters.get(name) };
		return super.getParameterValues(name);
	}

	public boolean isParameterPresent(String name) {
		if (parameters != null && parameters.get(name) != null)
			return parameters.get(name).length() > 0;
		String[] values = super.getParameterValues(name);
		if (values != null && values.length > 0)
			// use the last one as it maybe appended in js
			return values[values.length - 1].length() > 0;
		return false;
	}

	public Resource getResource(String name)
		throws BadRequestException, RepositoryException
	{
		Value value = decodeValue(getParameter(name));
		if (value == null || value instanceof Resource)
			return (Resource)value;
		throw new BadRequestException("Not a BNode or URI: " + value);
	}

	@SuppressWarnings("unchecked")
	public Map<String, String> getSingleParameterMap() {
		Map<String, String[]> map = super.getParameterMap();
		Map<String, String> parameters = new HashMap<String, String>(map.size());
		for (String name : map.keySet()) {
			if (isParameterPresent(name)) {
				parameters.put(name, getParameter(name));
			}
		}
		if (this.parameters != null) {
			parameters.putAll(this.parameters);
		}
		return parameters;
	}

	public String getTypeParameter() {
		return getParameter("type");
	}

	public URI getURI(String name)
		throws BadRequestException, RepositoryException
	{
		Value value = decodeValue(getParameter(name));
		if (value == null || value instanceof URI)
			return (URI)value;
		throw new BadRequestException("Not a URI: " + value);
	}

	public URL getUrl(String name)
		throws RepositoryException, BadRequestException, IOException, FileUploadException
	{
		String url = getParameter(name);
		try {
			return new URL(url);
		}
		catch (MalformedURLException exc) {
			throw new BadRequestException(exc.getMessage());
		}
	}

	public Value getValue(String name)
		throws BadRequestException, RepositoryException
	{
		return decodeValue(getParameter(name));
	}

	private Value decodeValue(String string)
		throws RepositoryException, BadRequestException
	{
		try {
			if (string == null)
				return null;
			String value = string.trim();
			if (value.length() == 0 || value.equals("null"))
				return null;
			if (value.startsWith("_:")) {
				String label = value.substring("_:".length());
				return vf.createBNode(label);
			}
			else if (value.startsWith("<") && value.endsWith(">")) {
				String label = value.substring(1, value.length() - 1);
				return vf.createURI(label);
			}
			else if (value.charAt(0) == '"') {
				String label = value.substring(1, value.lastIndexOf('"'));
				String rest = value.substring(label.length() + 2);
				if (rest.startsWith("^^")) {
					Value datatype = decodeValue(rest.substring(2));
					if (datatype instanceof URI)
						return vf.createLiteral(label, (URI)datatype);
					throw new BadRequestException("Malformed datatype: " + value);
				}
				else if (rest.startsWith("@")) {
					return vf.createLiteral(label, rest.substring(1));
				}
				else {
					return vf.createLiteral(label);
				}
			}
			else {
				String prefix = value.substring(0, value.indexOf(':'));
				String localPart = value.substring(prefix.length() + 1);
				String ns = getNamespace(prefix);
				if (ns == null)
					throw new BadRequestException("Undefined prefix: " + value);
				return vf.createURI(ns, localPart);
			}
		}
		catch (Exception exc) {
			logger.warn(exc.toString(), exc);
			throw new BadRequestException("Malformed value: " + string, exc);
		}
	}

	private String firstLine(FileItemStream item)
		throws IOException
	{
		InputStream in = item.openStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		try {
			return reader.readLine();
		}
		finally {
			reader.close();
		}
	}

	private String getNamespace(String prefix)
		throws RepositoryException
	{
		RepositoryConnection con = repository.getConnection();
		try {
			String ns = con.getNamespace(prefix);
			if (ns != null)
				return ns;
			for (Namespace n : Iterations.asList(con.getNamespaces())) {
				if (prefix.equals(n.getPrefix()))
					ns = n.getName();
			}
			if (ns != null) {
				logger.error("Namespace could not be found, but it does exist");
			}
			return ns;
		}
		finally {
			con.close();
		}
	}

	private Map<String, String> getMultipartParameterMap()
		throws RepositoryException, IOException, FileUploadException
	{
		Map<String, String> parameters = new HashMap<String, String>();
		ServletFileUpload upload = new ServletFileUpload();
		FileItemIterator iter = upload.getItemIterator(this);
		while (iter.hasNext()) {
			FileItemStream item = iter.next();
			String name = item.getFieldName();
			if ("content".equals(name)) {
				content = item.openStream();
				contentFileName = item.getName();
				return parameters;
			}
			else {
				String firstLine = firstLine(item);
				parameters.put(name, firstLine);
			}
		}
		return parameters;
	}

	private Map<String, String> getUrlParameterMap(String url)
		throws UnsupportedEncodingException
	{
		String qry = url.substring(url.indexOf(';') + 1);
		Map<String, String> parameters = new HashMap<String, String>();
		for (String param : qry.split("&")) {
			int idx = param.indexOf('=');
			String name = decode(param.substring(0, idx), UTF_8);
			String value = decode(param.substring(idx + 1), UTF_8);
			parameters.put(name, value);
		}
		return parameters;
	}

}
