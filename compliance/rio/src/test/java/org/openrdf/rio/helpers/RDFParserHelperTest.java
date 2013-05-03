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
package org.openrdf.rio.helpers;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.openrdf.model.Literal;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.rio.ParserConfig;

/**
 * Tests for {@link RDFParserHelper} methods.
 * 
 * @author Peter Ansell
 */
public class RDFParserHelperTest {

	/**
	 * 
	 */
	private static final String LABEL_TESTA = "test-a";

	private ParserConfig parserConfig;

	private ParseErrorCollector errListener;

	private ValueFactory valueFactory;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp()
		throws Exception
	{
		parserConfig = new ParserConfig();
		errListener = new ParseErrorCollector();
		valueFactory = new ValueFactoryImpl();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown()
		throws Exception
	{
	}

	/**
	 * Test method for
	 * {@link org.openrdf.rio.helpers.RDFParserHelper#createLiteral(java.lang.String, java.lang.String, org.openrdf.model.URI, org.openrdf.rio.ParserConfig, org.openrdf.rio.ParseErrorListener, org.openrdf.model.ValueFactory)}
	 * .
	 */
	@Test
	public final void testCreateLiteralLabelOnly()
		throws Exception
	{
		Literal literal = RDFParserHelper.createLiteral(LABEL_TESTA, null, null, parserConfig, errListener,
				valueFactory);

		assertEquals(LABEL_TESTA, literal.getLabel());
		assertNull(literal.getLanguage());
		assertNull(literal.getDatatype());
	}

	/**
	 * Test method for
	 * {@link org.openrdf.rio.helpers.RDFParserHelper#reportError(java.lang.String, org.openrdf.rio.RioSetting, org.openrdf.rio.ParserConfig, org.openrdf.rio.ParseErrorListener)}
	 * .
	 */
	@Ignore
	@Test
	public final void testReportErrorStringRioSettingOfBooleanParserConfigParseErrorListener()
		throws Exception
	{
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.openrdf.rio.helpers.RDFParserHelper#reportError(java.lang.String, int, int, org.openrdf.rio.RioSetting, org.openrdf.rio.ParserConfig, org.openrdf.rio.ParseErrorListener)}
	 * .
	 */
	@Ignore
	@Test
	public final void testReportErrorStringIntIntRioSettingOfBooleanParserConfigParseErrorListener()
		throws Exception
	{
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.openrdf.rio.helpers.RDFParserHelper#reportError(java.lang.Exception, int, int, org.openrdf.rio.RioSetting, org.openrdf.rio.ParserConfig, org.openrdf.rio.ParseErrorListener)}
	 * .
	 */
	@Ignore
	@Test
	public final void testReportErrorExceptionIntIntRioSettingOfBooleanParserConfigParseErrorListener()
		throws Exception
	{
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.openrdf.rio.helpers.RDFParserHelper#reportFatalError(java.lang.String, org.openrdf.rio.ParseErrorListener)}
	 * .
	 */
	@Ignore
	@Test
	public final void testReportFatalErrorStringParseErrorListener()
		throws Exception
	{
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.openrdf.rio.helpers.RDFParserHelper#reportFatalError(java.lang.String, int, int, org.openrdf.rio.ParseErrorListener)}
	 * .
	 */
	@Ignore
	@Test
	public final void testReportFatalErrorStringIntIntParseErrorListener()
		throws Exception
	{
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.openrdf.rio.helpers.RDFParserHelper#reportFatalError(java.lang.Exception, org.openrdf.rio.ParseErrorListener)}
	 * .
	 */
	@Ignore
	@Test
	public final void testReportFatalErrorExceptionParseErrorListener()
		throws Exception
	{
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.openrdf.rio.helpers.RDFParserHelper#reportFatalError(java.lang.Exception, int, int, org.openrdf.rio.ParseErrorListener)}
	 * .
	 */
	@Ignore
	@Test
	public final void testReportFatalErrorExceptionIntIntParseErrorListener()
		throws Exception
	{
		fail("Not yet implemented"); // TODO
	}

}
