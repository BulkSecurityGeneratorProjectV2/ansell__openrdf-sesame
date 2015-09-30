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
package org.openrdf.workbench.proxy;

import javax.servlet.ServletConfig;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Handles cookies for proxy servlets.
 */
public class CookieHandler {
	protected static final String COOKIE_AGE_PARAM = "cookie-max-age";

	private final String maxAge;

	protected CookieHandler(final String maxAge) {
   	 this.maxAge = maxAge;
    }
	
	protected CookieHandler(final ServletConfig config) {
		this(config.getInitParameter(COOKIE_AGE_PARAM));
	}
	
	protected String getCookieNullIfEmpty(final HttpServletRequest req, 
			final HttpServletResponse resp, final String name){
		String value = this.getCookie(req, resp, name);
		if (null !=value && value.isEmpty()){
			value = null;
		}
		return value;
	}
	
	protected String getCookie(final HttpServletRequest req, 
			final HttpServletResponse resp, final String name) {
		String value = null;
		final Cookie[] cookies = req.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (name.equals(cookie.getName())) {
					resp.addHeader("Vary", "Cookie");
					initCookie(cookie, req);
					resp.addCookie(cookie);
					value = cookie.getValue();
					break;
				}
			}
		}
		return value;
	}
	
	private void initCookie(final Cookie cookie, 
			final HttpServletRequest req) {
		final String context = req.getContextPath();
		cookie.setPath(null == context ? "/" : context);
		if (maxAge != null) {
			cookie.setMaxAge(Integer.parseInt(maxAge));
		}
	}
	
	/**
	 * @param req servlet request
	 * @param resp servlet response
	 * @param name cookie name
	 * @param value cookie value
	 */
	protected void addNewCookie(final HttpServletRequest req, final HttpServletResponse resp, final String name, final String value)
	{
		final Cookie cookie = new Cookie(name, value);
		initCookie(cookie, req);
		resp.addCookie(cookie);
	}

	/**
	 * @return the maximum age allowed for cookies
	 */
	public String getMaxAge() {
		return maxAge;
	}
}