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
package org.openrdf.query.resultio.binary;

import static org.openrdf.query.resultio.binary.BinaryQueryResultConstants.BNODE_RECORD_MARKER;
import static org.openrdf.query.resultio.binary.BinaryQueryResultConstants.DATATYPE_LITERAL_RECORD_MARKER;
import static org.openrdf.query.resultio.binary.BinaryQueryResultConstants.EMPTY_ROW_RECORD_MARKER;
import static org.openrdf.query.resultio.binary.BinaryQueryResultConstants.ERROR_RECORD_MARKER;
import static org.openrdf.query.resultio.binary.BinaryQueryResultConstants.FORMAT_VERSION;
import static org.openrdf.query.resultio.binary.BinaryQueryResultConstants.LANG_LITERAL_RECORD_MARKER;
import static org.openrdf.query.resultio.binary.BinaryQueryResultConstants.MAGIC_NUMBER;
import static org.openrdf.query.resultio.binary.BinaryQueryResultConstants.MALFORMED_QUERY_ERROR;
import static org.openrdf.query.resultio.binary.BinaryQueryResultConstants.NAMESPACE_RECORD_MARKER;
import static org.openrdf.query.resultio.binary.BinaryQueryResultConstants.NULL_RECORD_MARKER;
import static org.openrdf.query.resultio.binary.BinaryQueryResultConstants.PLAIN_LITERAL_RECORD_MARKER;
import static org.openrdf.query.resultio.binary.BinaryQueryResultConstants.QNAME_RECORD_MARKER;
import static org.openrdf.query.resultio.binary.BinaryQueryResultConstants.QUERY_EVALUATION_ERROR;
import static org.openrdf.query.resultio.binary.BinaryQueryResultConstants.REPEAT_RECORD_MARKER;
import static org.openrdf.query.resultio.binary.BinaryQueryResultConstants.TABLE_END_RECORD_MARKER;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.IRI;
import org.openrdf.model.Value;
import org.openrdf.model.util.Literals;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryResultHandlerException;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.impl.ListBindingSet;
import org.openrdf.query.resultio.AbstractQueryResultWriter;
import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.openrdf.query.resultio.TupleQueryResultWriter;

/**
 * Writer for the binary tuple result format. The format is explained in
 * {@link BinaryQueryResultConstants}.
 * 
 * @author Arjohn Kampman
 */
public class BinaryQueryResultWriter extends AbstractQueryResultWriter implements TupleQueryResultWriter {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The output stream to write the results table to.
	 */
	private DataOutputStream out;

	private CharsetEncoder charsetEncoder = Charset.forName("UTF-8").newEncoder();

	/**
	 * Map containing the namespace IDs (Integer objects) that have been defined
	 * in the document, stored using the concerning namespace (Strings).
	 */
	private Map<String, Integer> namespaceTable = new HashMap<String, Integer>(32);

	private int nextNamespaceID;

	private BindingSet previousBindings;

	private List<String> bindingNames;

	private boolean documentStarted = false;

	protected boolean tupleVariablesFound = false;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public BinaryQueryResultWriter(OutputStream out) {
		this.out = new DataOutputStream(out);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public final TupleQueryResultFormat getTupleQueryResultFormat() {
		return TupleQueryResultFormat.BINARY;
	}

	@Override
	public final TupleQueryResultFormat getQueryResultFormat() {
		return getTupleQueryResultFormat();
	}

	@Override
	public void startDocument()
		throws TupleQueryResultHandlerException
	{
		documentStarted = true;
		try {
			out.write(MAGIC_NUMBER);
			out.writeInt(FORMAT_VERSION);
		}
		catch (IOException e) {
			throw new TupleQueryResultHandlerException(e);
		}
	}

	@Override
	public void startQueryResult(List<String> bindingNames)
		throws TupleQueryResultHandlerException
	{
		tupleVariablesFound = true;

		if (!documentStarted) {
			startDocument();
		}

		// Copy supplied column headers list and make it unmodifiable
		bindingNames = new ArrayList<String>(bindingNames);
		this.bindingNames = Collections.unmodifiableList(bindingNames);

		try {
			out.writeInt(this.bindingNames.size());

			for (String bindingName : this.bindingNames) {
				writeString(bindingName);
			}

			List<Value> nullTuple = Collections.nCopies(this.bindingNames.size(), (Value)null);
			previousBindings = new ListBindingSet(this.bindingNames, nullTuple);
			nextNamespaceID = 0;
		}
		catch (IOException e) {
			throw new TupleQueryResultHandlerException(e);
		}
	}

	@Override
	public void endQueryResult()
		throws TupleQueryResultHandlerException
	{
		if (!tupleVariablesFound) {
			throw new IllegalStateException(
					"Could not end query result as startQueryResult was not called first.");
		}

		try {
			out.writeByte(TABLE_END_RECORD_MARKER);
			endDocument();
		}
		catch (IOException e) {
			throw new TupleQueryResultHandlerException(e);
		}
	}

	@Override
	public void handleSolution(BindingSet bindingSet)
		throws TupleQueryResultHandlerException
	{
		if (!tupleVariablesFound) {
			throw new IllegalStateException("Must call startQueryResult before handleSolution");
		}

		try {
			if (bindingSet.size() == 0) {
				writeEmptyRow();
			}
			else {
				for (String bindingName : bindingNames) {
					Value value = bindingSet.getValue(bindingName);

					if (value == null) {
						writeNull();
					}
					else if (value.equals(previousBindings.getValue(bindingName))) {
						writeRepeat();
					}
					else if (value instanceof IRI) {
						writeQName((IRI)value);
					}
					else if (value instanceof BNode) {
						writeBNode((BNode)value);
					}
					else if (value instanceof Literal) {
						writeLiteral((Literal)value);
					}
					else {
						throw new TupleQueryResultHandlerException("Unknown Value object type: " + value.getClass());
					}
				}

				previousBindings = bindingSet;
			}
		}
		catch (IOException e) {
			throw new TupleQueryResultHandlerException(e);
		}
	}

	private void writeNull()
		throws IOException
	{
		out.writeByte(NULL_RECORD_MARKER);
	}

	private void writeRepeat()
		throws IOException
	{
		out.writeByte(REPEAT_RECORD_MARKER);
	}

	private void writeEmptyRow()
		throws IOException
	{
		out.writeByte(EMPTY_ROW_RECORD_MARKER);
	}

	@Override
	public void handleNamespace(String prefix, String uri)
		throws QueryResultHandlerException
	{
		// Binary format does not support explicit setting of namespace prefixes.
	}

	private void writeQName(IRI uri)
		throws IOException
	{
		// Check if the URI has a new namespace
		String namespace = uri.getNamespace();

		Integer nsID = namespaceTable.get(namespace);

		if (nsID == null) {
			// Generate a ID for this new namespace
			nsID = writeNamespace(namespace);
		}

		out.writeByte(QNAME_RECORD_MARKER);
		out.writeInt(nsID.intValue());
		writeString(uri.getLocalName());
	}

	private void writeBNode(BNode bnode)
		throws IOException
	{
		out.writeByte(BNODE_RECORD_MARKER);
		writeString(bnode.getID());
	}

	private void writeLiteral(Literal literal)
		throws IOException
	{
		String label = literal.getLabel();
		IRI datatype = literal.getDatatype();

		int marker = PLAIN_LITERAL_RECORD_MARKER;

		if (Literals.isLanguageLiteral(literal)) {
			marker = LANG_LITERAL_RECORD_MARKER;
		}
		else {
			String namespace = datatype.getNamespace();

			if (!namespaceTable.containsKey(namespace)) {
				// Assign an ID to this new namespace
				writeNamespace(namespace);
			}

			marker = DATATYPE_LITERAL_RECORD_MARKER;
		}

		out.writeByte(marker);
		writeString(label);

		if (Literals.isLanguageLiteral(literal)) {
			writeString(literal.getLanguage().get());
		}
		else {
			writeQName(datatype);
		}
	}

	/**
	 * Writes an error msg to the stream.
	 * 
	 * @param errType
	 *        The error type.
	 * @param msg
	 *        The error message.
	 * @throws IOException
	 *         When the error could not be written to the stream.
	 */
	public void error(QueryErrorType errType, String msg)
		throws IOException
	{
		out.writeByte(ERROR_RECORD_MARKER);

		if (errType == QueryErrorType.MALFORMED_QUERY_ERROR) {
			out.writeByte(MALFORMED_QUERY_ERROR);
		}
		else {
			out.writeByte(QUERY_EVALUATION_ERROR);
		}

		writeString(msg);
	}

	private Integer writeNamespace(String namespace)
		throws IOException
	{
		out.writeByte(NAMESPACE_RECORD_MARKER);
		out.writeInt(nextNamespaceID);
		writeString(namespace);

		Integer result = new Integer(nextNamespaceID);
		namespaceTable.put(namespace, result);

		nextNamespaceID++;

		return result;
	}

	private void writeString(String s)
		throws IOException
	{
		ByteBuffer byteBuf = charsetEncoder.encode(CharBuffer.wrap(s));
		out.writeInt(byteBuf.remaining());
		out.write(byteBuf.array(), 0, byteBuf.remaining());
	}

	@Override
	public void handleStylesheet(String stylesheetUrl)
		throws QueryResultHandlerException
	{
		// Ignored by Binary Query Results format
	}

	@Override
	public void startHeader()
		throws QueryResultHandlerException
	{
		// Ignored by Binary Query Results format
	}

	@Override
	public void handleLinks(List<String> linkUrls)
		throws QueryResultHandlerException
	{
		// Ignored by Binary Query Results format
	}

	@Override
	public void endHeader()
		throws QueryResultHandlerException
	{
		// Ignored by Binary Query Results format
	}

	private void endDocument()
		throws IOException
	{
		out.flush();
		documentStarted = false;
	}

	@Override
	public void handleBoolean(boolean value)
		throws QueryResultHandlerException
	{
		throw new UnsupportedOperationException("Cannot handle boolean results");
	}
}
