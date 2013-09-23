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
package org.openrdf.rio.nquads;

import java.io.OutputStream;
import java.io.Writer;

import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.RDFWriterFactory;

/**
 * An {@link RDFWriterFactory} for {@link RDFFormat#NQUADS N-Quads} writers.
 * 
 * @since 2.7.0
 * @author Peter Ansell
 */
public class NQuadsWriterFactory implements RDFWriterFactory {

	/**
	 * Returns {@link RDFFormat#NQUADS}.
	 */
	public RDFFormat getRDFFormat() {
		return RDFFormat.NQUADS;
	}

	@Override
	public RDFWriter getWriter(OutputStream out) {
		return new NQuadsWriter(out);
	}

	@Override
	public RDFWriter getWriter(OutputStream out, String defaultBaseURI) {
		return this.getWriter(out);
	}

	@Override
	public RDFWriter getWriter(Writer writer) {
		return new NQuadsWriter(writer);
	}

	@Override
	public RDFWriter getWriter(Writer writer, String defaultBaseURI) {
		return this.getWriter(writer);
	}
}
