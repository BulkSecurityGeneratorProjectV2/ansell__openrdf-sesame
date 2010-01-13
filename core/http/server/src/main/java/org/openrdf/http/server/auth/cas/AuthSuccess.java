/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2009.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.http.server.auth.cas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Arjohn Kampman
 */
class AuthSuccess implements ServiceResponse {

	final String user;

	final String proxyGrantingTicket;

	final List<String> proxies;

	AuthSuccess(String user, String proxyGrantingTicket, List<String> proxies) {
		this.user = user;
		this.proxyGrantingTicket = proxyGrantingTicket;
		this.proxies = Collections.unmodifiableList(new ArrayList<String>(proxies));
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("user=").append(user);
		if (proxyGrantingTicket != null) {
			sb.append("; pgt=").append(proxyGrantingTicket);
		}
		if (!proxies.isEmpty()) {
			sb.append("; proxyList=").append(proxies);
		}
		return sb.toString();
	}
}