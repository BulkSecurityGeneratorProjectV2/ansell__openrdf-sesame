/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2011.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.rio.binary;

import java.io.DataInput;
import java.io.DataOutput;
import java.nio.charset.Charset;

/**
 * Interface defining constants for the binary table result format. Files in
 * this format consist of a header followed by zero or more records. Data fields
 * are encoded as specified in the interfaces {@link DataInput} and
 * {@link DataOutput}, except for the encoding of string values. String values
 * are encoded in UTF-8 and are preceeded by a 32-bit integer specifying the
 * length in bytes of this UTF-8 encoded string.
 * <p>
 * The file header is 13 bytes long:
 * <ul>
 * <li>Bytes 1-4 contain the ASCII codes for the string "BRTR", which stands for
 * Binary RDF Table Result.
 * <li>Bytes 5-8 specify the format version (an integer).
 * <li>Byte 9 specifies some flags, specifically 'distinct' and 'ordered'.
 * <li>Bytes 10-13 specify the number of columns of the query result that will
 * follow (an integer).
 * </ul>
 * Following this are the column headers, which are encoded as UTF-8 strings.
 * There are as many column headers as the number of columns that has been
 * specified in the header.
 * <p>
 * Zero or more records follow after the column headers. This can be a mixture
 * of records describing a result and supporting records. The results table is
 * described by the result records which are written from left to right, from
 * top to bottom. Each record starts with a record type marker (a single byte).
 * The following records are defined in the current format:
 * <ul>
 * <li><tt>NULL</tt> (byte value: 0):<br>
 * This indicates a NULL value in the table and consists of nothing more than
 * the record type marker.
 * <li><tt>REPEAT</tt> (byte value: 1):<br>
 * This indicates that the next value is identical to the value in the same
 * column in the previous row. The REPEAT record consists of nothing more than
 * the record type marker.
 * <li><tt>NAMESPACE</tt> (byte value: 2):<br>
 * This is a supporting record that assigns an ID (non-negative integer) to a
 * namespace. This ID can later be used in in a QNAME record to combine it with
 * a local name to form a full URI. The record type marker is followed by a
 * non-negative integer for the ID and an UTF-8 encoded string for the
 * namespace.
 * <li><tt>QNAME </tt>(byte value: 3):<br>
 * This indicates a URI value, the value of which is encoded as a namespace ID
 * and a local name. The namespace ID is required to be mapped to a namespace in
 * a previous NAMESPACE record. The record type marker is followed by a
 * non-negative integer (the namespace ID) and an UTF-8 encoded string for the
 * local name.
 * <li><tt>URI</tt> (byte value: 4):<br>
 * This also indicates a URI value, but one that does not use a namespace ID.
 * This record type marker is simply followed by an UTF-8 encoded string for the
 * full URI.
 * <li><tt>BNODE</tt> (byte value: 5):<br>
 * This indicates a blank node. The record type marker is followed by an UTF-8
 * encoded string for the bnode ID.
 * <li><tt>PLAIN_LITERAL</tt> (byte value: 6):<br>
 * This indicates a plain literal value. The record type marker is followed by
 * an UTF-8 encoded string for the literal's label.
 * <li><tt>LANG_LITERAL</tt> (byte value: 7):<br>
 * This indicates a literal value with a language attribute. The record type
 * marker is followed by an UTF-8 encoded string for the literal's label,
 * followed by an UTF-8 encoded string for the language attribute.
 * <li><tt>DATATYPE_LITERAL</tt> (byte value: 8):<br>
 * This indicates a datatyped literal. The record type marker is followed by an
 * UTF-8 encoded string for the literal's label. Following this label is either
 * a QNAME or URI record for the literal's datatype.
 * <li><tt>ERROR</tt> (byte value: 126):<br>
 * This record indicates a error. The type of error is indicates by the byte
 * directly following the record type marker: <tt>1</tt> for a malformed query
 * error, <tt>2</tt> for a query evaluation error. The error type byte is
 * followed by an UTF-8 string for the error message.
 * <li><tt>TABLE_END</tt> (byte value: 127):<br>
 * This is a special record that indicates the end of the results table and
 * consists of nothing more than the record type marker. Any data following this
 * record should be ignored.
 * </ul>
 * 
 * @author Arjohn Kampman
 */
class BinaryRDFConstants {

	/**
	 * Magic number for Binary RDF Table Result files.
	 */
	static final byte[] MAGIC_NUMBER = new byte[] { 'B', 'R', 'D', 'F' };

	/**
	 * The version number of the current format.
	 */
	static final int FORMAT_VERSION = 1;

	/* RECORD TYPES */

	static final int NAMESPACE_DECL = 0;

	static final int STATEMENT = 1;

	static final int COMMENT = 2;

	// public static final int ERROR = 126;

	static final int END_OF_DATA = 127;

	/* VALUE TYPES */

	static final int NULL_VALUE = 0;

	static final int URI_VALUE = 1;

	static final int BNODE_VALUE = 2;

	static final int PLAIN_LITERAL_VALUE = 3;

	static final int LANG_LITERAL_VALUE = 4;

	static final int DATATYPE_LITERAL_VALUE = 5;
	
	static final Charset CHARSET = Charset.forName("UTF-8");
}
