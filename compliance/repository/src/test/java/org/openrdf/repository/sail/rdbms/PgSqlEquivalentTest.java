/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2008.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.repository.sail.rdbms;

import junit.framework.TestSuite;

import org.openrdf.repository.EquivalentTest;
import org.openrdf.repository.Repository;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.rdbms.postgresql.PgSqlStore;


public class PgSqlEquivalentTest extends EquivalentTest {

	public static TestSuite suite() throws Exception {
		return EquivalentTest.suite(PgSqlEquivalentTest.class);
	}

	public PgSqlEquivalentTest() {
		super();
	}

	public PgSqlEquivalentTest(String name) {
		super(name);
	}

	@Override
	protected Repository newRepository() {
		return new SailRepository(new PgSqlStore("sesame_test"));
	}

}
