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
// SES-1071 disabling rdbms-based tests
//package org.openrdf.repository.sail.rdbms;
//
//import junit.framework.Test;
//
//import org.openrdf.query.Dataset;
//import org.openrdf.query.parser.sparql.SPARQL11ManifestTest;
//import org.openrdf.query.parser.sparql.SPARQLQueryTest;
//import org.openrdf.repository.Repository;
//import org.openrdf.repository.dataset.DatasetRepository;
//import org.openrdf.repository.sail.SailRepository;
//import org.openrdf.sail.memory.MemoryStore;
//import org.openrdf.sail.rdbms.postgresql.PgSqlStore;
//
//public class PgSqlSPARQL11QueryTest extends SPARQLQueryTest {
//
//	public static Test suite()
//		throws Exception
//	{
//		return SPARQL11ManifestTest.suite(new Factory() {
//
//			public PgSqlSPARQL11QueryTest createSPARQLQueryTest(String testURI, String name,
//					String queryFileURL, String resultFileURL, Dataset dataSet, boolean laxCardinality)
//			{
//				return createSPARQLQueryTest(testURI, name, queryFileURL, resultFileURL, dataSet, laxCardinality, false);
//			}
//			
//			public PgSqlSPARQL11QueryTest createSPARQLQueryTest(String testURI, String name,
//					String queryFileURL, String resultFileURL, Dataset dataSet, boolean laxCardinality, boolean checkOrder)
//			{
//				return new PgSqlSPARQL11QueryTest(testURI, name, queryFileURL, resultFileURL, dataSet,
//						laxCardinality, checkOrder);
//			}
//		});
//	}
//
//	protected PgSqlSPARQL11QueryTest(String testURI, String name, String queryFileURL, String resultFileURL,
//			Dataset dataSet, boolean laxCardinality)
//	{
//		this(testURI, name, queryFileURL, resultFileURL, dataSet, laxCardinality, false);
//	}
//
//	protected PgSqlSPARQL11QueryTest(String testURI, String name, String queryFileURL, String resultFileURL,
//			Dataset dataSet, boolean laxCardinality, boolean checkOrder)
//	{
//		super(testURI, name, queryFileURL, resultFileURL, dataSet, laxCardinality, checkOrder);
//	}
//	
//	protected Repository newRepository() {
//		PgSqlStore sail = new PgSqlStore("sesame_test");
//		sail.setUser("sesame");
//		sail.setPassword("opensesame");
//		return new DatasetRepository(new SailRepository(sail));
//	}
//}
