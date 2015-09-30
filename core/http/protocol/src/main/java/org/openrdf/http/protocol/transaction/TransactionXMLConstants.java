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
package org.openrdf.http.protocol.transaction;

/**
 * Interface defining tags and attribute names for the XML serialization of
 * transactions.
 * 
 * @author Arjohn Kampman
 * @author Leo Sauermann
 */
interface TransactionXMLConstants {

	public static final String TRANSACTION_TAG = "transaction";

	public static final String ADD_STATEMENT_TAG = "add";

	public static final String REMOVE_STATEMENTS_TAG = "remove";

	public static final String REMOVE_NAMED_CONTEXT_STATEMENTS_TAG = "removeFromNamedContext";

	public static final String CLEAR_TAG = "clear";

	public static final String NULL_TAG = "null";

	public static final String URI_TAG = "uri";

	public static final String BNODE_TAG = "bnode";

	public static final String LITERAL_TAG = "literal";

	public static final String ENCODING_ATT = "encoding";

	public static final String LANG_ATT = "xml:lang";

	public static final String DATATYPE_ATT = "datatype";

	public static final String SET_NAMESPACE_TAG = "setNamespace";

	public static final String REMOVE_NAMESPACE_TAG = "removeNamespace";

	public static final String PREFIX_ATT = "prefix";

	public static final String NAME_ATT = "name";

	public static final String CLEAR_NAMESPACES_TAG = "clearNamespaces";

	public static final String CONTEXTS_TAG = "contexts";

	/**
	 * @since 2.7.0
	 */
	public static final String SPARQL_UPDATE_TAG = "sparql";

	/**
	 * @since 2.7.0
	 */
	public static final String UPDATE_STRING_TAG = "updateString";

	/**
	 * @since 2.7.0
	 */
	public static final String BASE_URI_ATT = "baseURI";

	/**
	 * @since 2.7.0
	 */
	public static final String INCLUDE_INFERRED_ATT = "includeInferred";

	/**
	 * @since 2.7.0
	 */
	public static final String DATASET_TAG = "dataset";

	/**
	 * @since 2.7.0
	 */
	public static final String GRAPH_TAG = "graph";

	/**
	 * @since 2.7.0
	 */
	public static final String DEFAULT_GRAPHS_TAG = "defaultGraphs";

	/**
	 * @since 2.7.0
	 */
	public static final String NAMED_GRAPHS_TAG = "namedGraphs";

	/**
	 * @since 2.7.0
	 */
	public static final String DEFAULT_REMOVE_GRAPHS_TAG = "defaultRemoveGraphs";

	/**
	 * @since 2.7.0
	 */
	public static final String DEFAULT_INSERT_GRAPH = "defaultInsertGraph";

	public static final String BINDINGS = "bindings";

	public static final String BINDING_URI = "binding_uri";

	public static final String BINDING_BNODE = "binding_bnode";

	public static final String BINDING_LITERAL = "binding_literal";

	public static final String LANGUAGE_ATT = "language";

	public static final String DATA_TYPE_ATT = "dataType";
}
