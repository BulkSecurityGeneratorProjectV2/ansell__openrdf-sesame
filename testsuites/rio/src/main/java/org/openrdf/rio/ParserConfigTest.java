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
package org.openrdf.rio;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashSet;

import org.junit.Ignore;
import org.junit.Test;

import org.openrdf.rio.RDFParser.DatatypeHandling;
import org.openrdf.rio.helpers.BasicParserSettings;
import org.openrdf.rio.helpers.NTriplesParserSettings;

/**
 * Test for ParserConfig to verify that the core operations succeed and are
 * consistent.
 * 
 * @author Peter Ansell
 */
public class ParserConfigTest {

	/**
	 * Test the default constructor does not set any settings, but still returns
	 * the default values for basic settings.
	 */
	@Test
	public final void testParserConfig() {
		ParserConfig testConfig = new ParserConfig();

		// check that the basic settings are not set
		assertFalse(testConfig.isSet(BasicParserSettings.PRESERVE_BNODE_IDS));

		// check that the basic settings all return their expected default values
		assertFalse(testConfig.get(BasicParserSettings.PRESERVE_BNODE_IDS));
		assertFalse(testConfig.isPreserveBNodeIDs());

		// then set to check that changes occur
		testConfig.set(BasicParserSettings.PRESERVE_BNODE_IDS, true);

		// check that the basic settings are now explicitly set
		assertTrue(testConfig.isSet(BasicParserSettings.PRESERVE_BNODE_IDS));

		// check that the basic settings all return their set values
		assertTrue(testConfig.get(BasicParserSettings.PRESERVE_BNODE_IDS));
		assertTrue(testConfig.isPreserveBNodeIDs());

		// reset the values
		testConfig.set(BasicParserSettings.PRESERVE_BNODE_IDS, null);

		// check again that the basic settings all return their expected default
		// values
		assertFalse(testConfig.get(BasicParserSettings.PRESERVE_BNODE_IDS));
		assertFalse(testConfig.isPreserveBNodeIDs());
	}

	/**
	 * Test that the explicit constructor sets all of the basic settings using
	 * the default values.
	 */
	@Test
	public final void testParserConfigSameAsDefaults() {
		ParserConfig testConfig = new ParserConfig(true, true, false, DatatypeHandling.VERIFY);

		// check that the basic settings are explicitly set
		assertTrue(testConfig.isSet(BasicParserSettings.PRESERVE_BNODE_IDS));

		// check that the basic settings all return their expected default values
		assertFalse(testConfig.get(BasicParserSettings.PRESERVE_BNODE_IDS));
		assertFalse(testConfig.isPreserveBNodeIDs());
	}

	/**
	 * Test that the explicit constructor sets all of the basic settings using
	 * non-default values.
	 */
	@Test
	public final void testParserConfigNonDefaults() {
		ParserConfig testConfig = new ParserConfig(false, false, true, DatatypeHandling.IGNORE);

		// check that the basic settings are explicitly set
		assertTrue(testConfig.isSet(BasicParserSettings.PRESERVE_BNODE_IDS));

		// check that the basic settings all return their set values
		assertTrue(testConfig.get(BasicParserSettings.PRESERVE_BNODE_IDS));
		assertTrue(testConfig.isPreserveBNodeIDs());
	}

	/**
	 * Test method for
	 * {@link org.openrdf.rio.ParserConfig#ParserConfig(boolean, boolean, boolean, org.openrdf.rio.RDFParser.DatatypeHandling)}
	 * .
	 */
	@Ignore("TODO: Implement me")
	@Test
	public final void testParserConfigBooleanBooleanBooleanDatatypeHandling() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.openrdf.rio.ParserConfig#useDefaults()}.
	 */
	@Test
	public final void testUseDefaults() {
		ParserConfig testConfig = new ParserConfig();

		// Test the initial state and add a non-fatal error first
		assertNotNull(testConfig.getNonFatalErrors());
		assertTrue(testConfig.getNonFatalErrors().isEmpty());
		testConfig.addNonFatalError(BasicParserSettings.PRESERVE_BNODE_IDS);
		assertFalse(testConfig.getNonFatalErrors().isEmpty());
		assertTrue(testConfig.isNonFatalError(BasicParserSettings.PRESERVE_BNODE_IDS));

		// Test useDefaults
		testConfig.useDefaults();

		// Verify that the non fatal errors are empty again
		assertTrue(testConfig.getNonFatalErrors().isEmpty());
		assertFalse(testConfig.isNonFatalError(BasicParserSettings.PRESERVE_BNODE_IDS));
	}

	/**
	 * Test method for
	 * {@link org.openrdf.rio.ParserConfig#setNonFatalErrors(java.util.Set)}.
	 */
	@Test
	public final void testSetNonFatalErrors() {
		ParserConfig testConfig = new ParserConfig();

		// Test that the defaults exist and are empty
		assertNotNull(testConfig.getNonFatalErrors());
		assertTrue(testConfig.getNonFatalErrors().isEmpty());

		// Test that we can add to the default before calling setNonFatalErrors
		// (SES-1801)
		testConfig.addNonFatalError(BasicParserSettings.PRESERVE_BNODE_IDS);
		assertFalse(testConfig.getNonFatalErrors().isEmpty());
		assertTrue(testConfig.getNonFatalErrors().contains(BasicParserSettings.PRESERVE_BNODE_IDS));
		assertFalse(testConfig.getNonFatalErrors().contains(BasicParserSettings.VERIFY_DATATYPE_VALUES));

		// Test with a non-empty set that we remove the previous setting
		testConfig.setNonFatalErrors(Collections.<RioSetting<?>> singleton(BasicParserSettings.VERIFY_DATATYPE_VALUES));
		assertNotNull(testConfig.getNonFatalErrors());
		assertFalse(testConfig.getNonFatalErrors().isEmpty());
		assertFalse(testConfig.getNonFatalErrors().contains(BasicParserSettings.PRESERVE_BNODE_IDS));
		assertTrue(testConfig.getNonFatalErrors().contains(BasicParserSettings.VERIFY_DATATYPE_VALUES));

		// Test with an empty set
		testConfig.setNonFatalErrors(new HashSet<RioSetting<?>>());
		assertNotNull(testConfig.getNonFatalErrors());
		assertTrue(testConfig.getNonFatalErrors().isEmpty());
	}

	/**
	 * Test method for
	 * {@link org.openrdf.rio.ParserConfig#addNonFatalError(org.openrdf.rio.RioSetting)}
	 * .
	 */
	@Test
	public final void testAddNonFatalError() {
		ParserConfig testConfig = new ParserConfig();

		assertTrue(testConfig.getNonFatalErrors().isEmpty());
		testConfig.addNonFatalError(BasicParserSettings.PRESERVE_BNODE_IDS);
		assertTrue(testConfig.getNonFatalErrors().contains(BasicParserSettings.PRESERVE_BNODE_IDS));
	}

	/**
	 * Test method for
	 * {@link org.openrdf.rio.ParserConfig#isNonFatalError(org.openrdf.rio.RioSetting)}
	 * .
	 */
	@Test
	public final void testIsNonFatalError() {
		ParserConfig testConfig = new ParserConfig();

		assertTrue(testConfig.getNonFatalErrors().isEmpty());

		assertFalse(testConfig.isNonFatalError(BasicParserSettings.PRESERVE_BNODE_IDS));

		testConfig.addNonFatalError(BasicParserSettings.PRESERVE_BNODE_IDS);

		assertTrue(testConfig.isNonFatalError(BasicParserSettings.PRESERVE_BNODE_IDS));
	}

	/**
	 * Test method for {@link org.openrdf.rio.ParserConfig#getNonFatalErrors()}.
	 */
	@Test
	public final void testGetNonFatalErrors() {
		ParserConfig testConfig = new ParserConfig();

		assertTrue(testConfig.getNonFatalErrors().isEmpty());

		testConfig.addNonFatalError(BasicParserSettings.PRESERVE_BNODE_IDS);

		assertFalse(testConfig.getNonFatalErrors().isEmpty());
	}

	/**
	 * Test method for {@link org.openrdf.rio.ParserConfig#verifyData()}.
	 */
	@Test
	public final void testVerifyData() {
		ParserConfig testConfig = new ParserConfig();

		assertTrue(testConfig.verifyData());

		testConfig.set(BasicParserSettings.VERIFY_RELATIVE_URIS, false);

		assertFalse(testConfig.verifyData());
	}

	/**
	 * Test method for {@link org.openrdf.rio.ParserConfig#stopAtFirstError()}.
	 * Test specifically for SES-1947
	 */
	@Test
	public final void testStopAtFirstError() {
		ParserConfig testConfig = new ParserConfig();

		assertTrue(testConfig.stopAtFirstError());

		testConfig.addNonFatalError(NTriplesParserSettings.FAIL_ON_NTRIPLES_INVALID_LINES);

		assertFalse(testConfig.stopAtFirstError());
	}

	/**
	 * Test method for {@link org.openrdf.rio.ParserConfig#isPreserveBNodeIDs()}.
	 */
	@Test
	public final void testIsPreserveBNodeIDs() {
		ParserConfig testConfig = new ParserConfig();

		assertFalse(testConfig.isPreserveBNodeIDs());

		testConfig.set(BasicParserSettings.PRESERVE_BNODE_IDS, true);

		assertTrue(testConfig.isPreserveBNodeIDs());
	}

	/**
	 * Test method for {@link org.openrdf.rio.ParserConfig#datatypeHandling()}.
	 */
	@Test
	public final void testDatatypeHandling() {
		ParserConfig testConfig = new ParserConfig();

		try {
			testConfig.datatypeHandling();
			fail("Did not receive expected exception");
		}
		catch (Exception e) {
		}
	}

	/**
	 * Test method for
	 * {@link org.openrdf.rio.ParserConfig#get(org.openrdf.rio.RioSetting)}.
	 */
	@Test
	public final void testGet() {
		ParserConfig testConfig = new ParserConfig();

		assertTrue(testConfig.get(BasicParserSettings.VERIFY_RELATIVE_URIS));

		testConfig.set(BasicParserSettings.VERIFY_RELATIVE_URIS, false);

		assertFalse(testConfig.get(BasicParserSettings.VERIFY_RELATIVE_URIS));
	}

	/**
	 * Test method for
	 * {@link org.openrdf.rio.ParserConfig#set(org.openrdf.rio.RioSetting, java.lang.Object)}
	 * .
	 */
	@Test
	public final void testSet() {
		ParserConfig testConfig = new ParserConfig();

		assertFalse(testConfig.isSet(BasicParserSettings.VERIFY_RELATIVE_URIS));

		testConfig.set(BasicParserSettings.VERIFY_RELATIVE_URIS, false);

		assertTrue(testConfig.isSet(BasicParserSettings.VERIFY_RELATIVE_URIS));
	}

	/**
	 * Test method for
	 * {@link org.openrdf.rio.ParserConfig#isSet(org.openrdf.rio.RioSetting)}.
	 */
	@Test
	public final void testIsSet() {
		ParserConfig testConfig = new ParserConfig();

		assertFalse(testConfig.isSet(BasicParserSettings.PRESERVE_BNODE_IDS));

		testConfig.set(BasicParserSettings.PRESERVE_BNODE_IDS, true);

		assertTrue(testConfig.isSet(BasicParserSettings.PRESERVE_BNODE_IDS));
	}
}
