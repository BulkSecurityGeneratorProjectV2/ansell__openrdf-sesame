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
package org.openrdf.sail.lucene;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.openrdf.sail.lucene.LuceneSailSchema.MATCHES;
import static org.openrdf.sail.lucene.LuceneSailSchema.PROPERTY;
import static org.openrdf.sail.lucene.LuceneSailSchema.QUERY;
import static org.openrdf.sail.lucene.LuceneSailSchema.SCORE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.openrdf.model.IRI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.SimpleLiteral;
import org.openrdf.model.impl.SimpleValueFactory;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

public abstract class AbstractLuceneSailIndexedPropertiesTest {

	private static ValueFactory vf = SimpleValueFactory.getInstance();

	protected LuceneSail sail;

	protected Repository repository;

	protected RepositoryConnection connection;

	public static final IRI SUBJECT_1 = vf.createIRI("urn:subject1");

	public static final IRI SUBJECT_2 = vf.createIRI("urn:subject2");

	public static final IRI SUBJECT_3 = vf.createIRI("urn:subject3");

	public static final IRI SUBJECT_4 = vf.createIRI("urn:subject4");

	public static final IRI SUBJECT_5 = vf.createIRI("urn:subject5");

	public static final IRI CONTEXT_1 = vf.createIRI("urn:context1");

	public static final IRI CONTEXT_2 = vf.createIRI("urn:context2");

	public static final IRI CONTEXT_3 = vf.createIRI("urn:context3");

	public static final IRI RDFSLABEL = RDFS.LABEL;

	public static final IRI RDFSCOMMENT = RDFS.COMMENT;

	public static final IRI FOAFNAME = vf.createIRI("http://xmlns.com/foaf/0.1/name");

	public static final IRI FOAFPLAN = vf.createIRI("http://xmlns.com/foaf/0.1/plan");

	protected abstract void configure(LuceneSail sail);

	@Before
	public void setUp()
		throws IOException, RepositoryException
	{
		// setup a LuceneSail
		MemoryStore memoryStore = new MemoryStore();
		// enable lock tracking
		info.aduna.concurrent.locks.Properties.setLockTrackingEnabled(true);
		sail = new LuceneSail();
		configure(sail);
		Properties indexedFields = new Properties();
		indexedFields.setProperty("index.1", RDFSLABEL.toString());
		indexedFields.setProperty("index.2", RDFSCOMMENT.toString());
		indexedFields.setProperty(FOAFNAME.toString(), RDFS.LABEL.toString());
		ByteArrayOutputStream indexedFieldsString = new ByteArrayOutputStream();
		indexedFields.store(indexedFieldsString, "For testing");
		sail.setParameter(LuceneSail.INDEXEDFIELDS, indexedFieldsString.toString());
		sail.setBaseSail(memoryStore);

		// create a Repository wrapping the LuceneSail
		repository = new SailRepository(sail);
		repository.initialize();

		// add some statements to it
		connection = repository.getConnection();
		connection.begin();
		connection.add(SUBJECT_1, RDFSLABEL, vf.createLiteral("the first resource"));
		connection.add(SUBJECT_1, RDFSCOMMENT, vf.createLiteral(
				"Groucho Marx is going to cut away the first part of the first party of the contract."));
		connection.add(SUBJECT_1, FOAFNAME, vf.createLiteral("groucho and harpo"));

		connection.add(SUBJECT_2, FOAFNAME, vf.createLiteral("the second resource"));
		connection.add(SUBJECT_2, RDFSCOMMENT,
				vf.createLiteral("in the night at the opera, groucho is in a cabin on a ship."));

		connection.add(SUBJECT_3, RDFSLABEL, vf.createLiteral("the third resource"));
		connection.add(SUBJECT_3, RDFSCOMMENT,
				vf.createLiteral("a not well known fact, groucho marx was not a smoker"));
		// this should not be indexed
		connection.add(SUBJECT_3, FOAFPLAN, vf.createLiteral("groucho did not smoke cigars nor cigarillos"));
		connection.commit();
	}

	@After
	public void tearDown()
		throws IOException, RepositoryException
	{
		connection.close();
		repository.shutDown();
	}

	@Test
	public void testTriplesStored()
		throws Exception
	{
		// are the triples stored in the underlying sail?

		assertTrue(
				connection.hasStatement(SUBJECT_1, RDFSLABEL, vf.createLiteral("the first resource"), false));
		assertTrue(connection.hasStatement(SUBJECT_1, RDFSCOMMENT,
				vf.createLiteral(
						"Groucho Marx is going to cut away the first part of the first party of the contract."),
				false));
		assertTrue(connection.hasStatement(SUBJECT_1, FOAFNAME, vf.createLiteral("groucho and harpo"), false));

		assertTrue(
				connection.hasStatement(SUBJECT_2, FOAFNAME, vf.createLiteral("the second resource"), false));
		assertTrue(connection.hasStatement(SUBJECT_2, RDFSCOMMENT,
				vf.createLiteral("in the night at the opera, groucho is in a cabin on a ship."), false));

		assertTrue(
				connection.hasStatement(SUBJECT_3, RDFSLABEL, vf.createLiteral("the third resource"), false));
		assertTrue(connection.hasStatement(SUBJECT_3, RDFSCOMMENT,
				vf.createLiteral("a not well known fact, groucho marx was not a smoker"), false));
		// this should not be indexed
		assertTrue(connection.hasStatement(SUBJECT_3, FOAFPLAN,
				vf.createLiteral("groucho did not smoke cigars nor cigarillos"), false));
	}

	@Test
	public void testRegularQuery()
		throws RepositoryException, MalformedQueryException, QueryEvaluationException
	{
		// fire a query for all subjects with a given term
		String queryString = "SELECT Subject, Score " + "FROM {Subject} <" + MATCHES + "> {} " + " <" + QUERY
				+ "> {Query}; " + " <" + PROPERTY + "> {Property}; " + " <" + SCORE + "> {Score} ";
		{
			TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SERQL, queryString);
			query.setBinding("Query", vf.createLiteral("resource"));
			query.setBinding("Property", RDFSLABEL);
			TupleQueryResult result = query.evaluate();
			// check the results
			ArrayList<IRI> uris = new ArrayList<IRI>();

			BindingSet bindings = null;
			while (result.hasNext()) {
				bindings = result.next();
				uris.add((IRI)bindings.getValue("Subject"));
				assertNotNull(bindings.getValue("Score"));
			}
			result.close();
			assertEquals(3, uris.size());
			assertTrue(uris.contains(SUBJECT_1));
			assertTrue(uris.contains(SUBJECT_2));
			assertTrue(uris.contains(SUBJECT_3));
		}
		{
			TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SERQL, queryString);
			query.setBinding("Query", vf.createLiteral("groucho"));
			query.setBinding("Property", RDFSLABEL);
			TupleQueryResult result = query.evaluate();
			// check the results
			ArrayList<IRI> uris = new ArrayList<IRI>();

			BindingSet bindings = null;
			while (result.hasNext()) {
				bindings = result.next();
				uris.add((IRI)bindings.getValue("Subject"));
				assertNotNull(bindings.getValue("Score"));
			}
			result.close();
			assertEquals(1, uris.size());
			assertTrue(uris.contains(SUBJECT_1));
		}
		{
			TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SERQL, queryString);
			query.setBinding("Query", vf.createLiteral("cigarillos"));
			query.setBinding("Property", FOAFPLAN);
			TupleQueryResult result = query.evaluate();
			// check the results
			assertFalse(result.hasNext());
			result.close();
		}
	}

}
