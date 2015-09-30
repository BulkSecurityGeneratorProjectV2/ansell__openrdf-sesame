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
package org.openrdf.rio.datatypes;

import org.openrdf.model.Literal;
import org.openrdf.model.IRI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.SimpleValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.rio.DatatypeHandler;

/**
 * Test for {@link VirtuosoDatatypeHandler} with
 * http://www.openlinksw.com/schemas/virtrdf#Geometry .
 * 
 * @author Peter Ansell
 */
public class VirtuosoGeometryPointDatatypeHandlerTest extends AbstractDatatypeHandlerTest {

	// -------------------------------------
	// XMLSchema Double specific methods
	// -------------------------------------

	@Override
	protected IRI getRecognisedDatatypeUri() {
		return SimpleValueFactory.getInstance().createIRI("http://www.openlinksw.com/schemas/virtrdf#",
				"Geometry");
	}

	@Override
	protected String getValueMatchingRecognisedDatatypeUri() {
		return "POINT(123.0000 143.000)";
	}

	@Override
	protected String getValueNotMatchingRecognisedDatatypeUri() {
		return "POINT(This is not a point)";
	}

	@Override
	protected Literal getNormalisedLiteralForRecognisedDatatypeAndValue() {
		return SimpleValueFactory.getInstance().createLiteral(
				"POINT(123.0000 143.000)",
				SimpleValueFactory.getInstance().createIRI("http://www.openlinksw.com/schemas/virtrdf#", "Geometry"));
	}

	// -------------------------------------
	// Common methods
	// -------------------------------------

	@Override
	protected DatatypeHandler getNewDatatypeHandler() {
		return new VirtuosoGeometryDatatypeHandler();
	}

	@Override
	protected ValueFactory getValueFactory() {
		return SimpleValueFactory.getInstance();
	}

	@Override
	protected IRI getUnrecognisedDatatypeUri() {
		return RDF.LANGSTRING;
	}

	@Override
	protected String getExpectedKey() {
		return DatatypeHandler.VIRTUOSOGEOMETRY;
	}

}
