/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2008.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.sail.federation.config;

import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;

/**
 * RDF Schema used by the federation configuration.
 * 
 * @author James Leigh
 */
public class FederationSchema {

	/** http://www.openrdf.org/config/sail/federation# */
	public static String NAMESPACE = "http://www.openrdf.org/config/sail/federation#";

	public static URI MEMBER = new URIImpl(NAMESPACE + "member");

	/**
	 * For all triples with a predicate in this space, the container RDF store
	 * contains all triples with that subject and any predicate in this space.
	 */
	public static URI LOCALPROPERTYSPACE = new URIImpl(NAMESPACE + "localPropertySpace");

	private FederationSchema() {
		// no constructor
	}
}
