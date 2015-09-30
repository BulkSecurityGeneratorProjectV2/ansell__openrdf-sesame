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
package org.openrdf.query.resultio;

import org.openrdf.rio.RioSetting;
import org.openrdf.rio.helpers.RioSettingImpl;

/**
 * {@link RioSetting} constants to use with {@link QueryResultWriter}s.
 * 
 * @author Peter Ansell
 * @since 2.7.0
 */
public class BasicQueryWriterSettings {

	/**
	 * Specifies whether the writer should add the proprietary
	 * "http://www.openrdf.org/schema/qname#qname" annotations to output.
	 * <p>
	 * Defaults to false.
	 * 
	 * @since 2.7.0
	 */
	public final static RioSetting<Boolean> ADD_SESAME_QNAME = new RioSettingImpl<Boolean>(
			"org.openrdf.query.resultio.addsesameqname", "Add Sesame QName", false);

	/**
	 * Specifies a callback function name for wrapping JSON results to support
	 * the JSONP cross-origin request methodology.
	 * <p>
	 * Defaults to "sesamecallback".
	 * 
	 * @since 2.7.0
	 */
	public static final RioSetting<String> JSONP_CALLBACK = new RioSettingImpl<String>(
			"org.openrdf.query.resultio.jsonpcallback", "JSONP callback function", "sesamecallback");

	/**
	 * Private default constructor
	 */
	private BasicQueryWriterSettings() {
	}

}
