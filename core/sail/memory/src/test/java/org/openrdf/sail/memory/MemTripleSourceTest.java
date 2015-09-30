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
package org.openrdf.sail.memory;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.aduna.iteration.CloseableIteration;
import info.aduna.iteration.ExceptionConvertingIteration;
import info.aduna.iteration.Iterations;

import org.openrdf.IsolationLevel;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.IRI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.algebra.evaluation.TripleSource;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.Rio;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailException;
import org.openrdf.sail.base.SailDataset;
import org.openrdf.sail.base.SailSource;

/**
 * Unit Test for {@link TripleSource}
 * 
 * @author Peter Ansell
 */
public class MemTripleSourceTest {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private MemoryStore store;

	protected static final String EX_NS = "http://example.org/";

	private IRI bob;

	private IRI alice;

	private IRI mary;

	private ValueFactory f;

	private SailDataset snapshot;

	private SailSource source;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp()
		throws Exception
	{
		store = new MemoryStore();
		store.initialize();
		f = store.getValueFactory();

		bob = f.createIRI(EX_NS, "bob");
		alice = f.createIRI(EX_NS, "alice");
		mary = f.createIRI(EX_NS, "mary");
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown()
		throws Exception
	{
		if (snapshot != null) {
			snapshot.close();
		}
		if (source != null) {
			source.close();
		}
		store.shutDown();
	}

	/**
	 * Test method for
	 * {@link org.openrdf.sail.memory.MemTripleSource#getStatements(org.openrdf.model.Resource, org.openrdf.model.IRI, org.openrdf.model.Value, org.openrdf.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsNoContextsAllNull()
		throws Exception
	{
		loadTestData("/alp-testdata.ttl");
		TripleSource source = getTripleSourceCommitted();

		CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				null, null);

		try {
			List<Statement> list = Iterations.asList(statements);

			assertEquals(8, list.size());
		}
		finally {
			statements.close();
		}
	}

	/**
	 * Test method for
	 * {@link org.openrdf.sail.memory.MemTripleSource#getStatements(org.openrdf.model.Resource, org.openrdf.model.IRI, org.openrdf.model.Value, org.openrdf.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsOneContextAllNull()
		throws Exception
	{
		loadTestData("/alp-testdata.ttl", this.alice);
		TripleSource source = getTripleSourceCommitted();

		CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				null, null);

		try {
			List<Statement> list = Iterations.asList(statements);

			assertEquals(8, list.size());
		}
		finally {
			statements.close();
		}
	}

	/**
	 * Test method for
	 * {@link org.openrdf.sail.memory.MemTripleSource#getStatements(org.openrdf.model.Resource, org.openrdf.model.IRI, org.openrdf.model.Value, org.openrdf.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsTwoContextsAllNull()
		throws Exception
	{
		loadTestData("/alp-testdata.ttl", this.alice, this.bob);
		TripleSource source = getTripleSourceCommitted();

		CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				null, null);

		try {
			List<Statement> list = Iterations.asList(statements);

			assertEquals(16, list.size());
		}
		finally {
			statements.close();
		}
	}

	/**
	 * Test method for
	 * {@link org.openrdf.sail.memory.MemTripleSource#getStatements(org.openrdf.model.Resource, org.openrdf.model.IRI, org.openrdf.model.Value, org.openrdf.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsNoContextsOnePredicate()
		throws Exception
	{
		loadTestData("/alp-testdata.ttl");
		TripleSource source = getTripleSourceCommitted();

		CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, null);

		try {
			List<Statement> list = Iterations.asList(statements);

			assertEquals(4, list.size());
		}
		finally {
			statements.close();
		}
	}

	/**
	 * Test method for
	 * {@link org.openrdf.sail.memory.MemTripleSource#getStatements(org.openrdf.model.Resource, org.openrdf.model.IRI, org.openrdf.model.Value, org.openrdf.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsOneContextOnePredicate()
		throws Exception
	{
		loadTestData("/alp-testdata.ttl", this.alice);
		TripleSource source = getTripleSourceCommitted();

		CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, null);

		try {
			List<Statement> list = Iterations.asList(statements);

			assertEquals(4, list.size());
		}
		finally {
			statements.close();
		}
	}

	/**
	 * Test method for
	 * {@link org.openrdf.sail.memory.MemTripleSource#getStatements(org.openrdf.model.Resource, org.openrdf.model.IRI, org.openrdf.model.Value, org.openrdf.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsTwoContextsOnePredicate()
		throws Exception
	{
		loadTestData("/alp-testdata.ttl", this.alice, this.bob);
		TripleSource source = getTripleSourceCommitted();

		CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, null);

		try {
			List<Statement> list = Iterations.asList(statements);

			assertEquals(8, list.size());
		}
		finally {
			statements.close();
		}
	}

	/**
	 * Test method for
	 * {@link org.openrdf.sail.memory.MemTripleSource#getStatements(org.openrdf.model.Resource, org.openrdf.model.IRI, org.openrdf.model.Value, org.openrdf.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsNoContextsOnePredicateOneContext()
		throws Exception
	{
		loadTestData("/alp-testdata.ttl");
		TripleSource source = getTripleSourceCommitted();

		CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, null, this.alice);

		try {
			List<Statement> list = Iterations.asList(statements);

			assertEquals(0, list.size());
		}
		finally {
			statements.close();
		}
	}

	/**
	 * Test method for
	 * {@link org.openrdf.sail.memory.MemTripleSource#getStatements(org.openrdf.model.Resource, org.openrdf.model.IRI, org.openrdf.model.Value, org.openrdf.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsOneContextOnePredicateOneContext()
		throws Exception
	{
		loadTestData("/alp-testdata.ttl", this.alice);
		TripleSource source = getTripleSourceCommitted();

		CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, null, this.alice);

		try {
			List<Statement> list = Iterations.asList(statements);

			assertEquals(4, list.size());
		}
		finally {
			statements.close();
		}
	}

	/**
	 * Test method for
	 * {@link org.openrdf.sail.memory.MemTripleSource#getStatements(org.openrdf.model.Resource, org.openrdf.model.IRI, org.openrdf.model.Value, org.openrdf.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsTwoContextsOnePredicateOneContext()
		throws Exception
	{
		loadTestData("/alp-testdata.ttl", this.alice, this.bob);
		TripleSource source = getTripleSourceCommitted();

		CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, null, this.alice);

		try {
			List<Statement> list = Iterations.asList(statements);

			assertEquals(4, list.size());
		}
		finally {
			statements.close();
		}
	}

	/**
	 * Test method for
	 * {@link org.openrdf.sail.memory.MemTripleSource#getStatements(org.openrdf.model.Resource, org.openrdf.model.IRI, org.openrdf.model.Value, org.openrdf.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsNoContextsOnePredicateTwoContexts()
		throws Exception
	{
		loadTestData("/alp-testdata.ttl");
		TripleSource source = getTripleSourceCommitted();

		CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, null, this.alice, this.bob);

		try {
			List<Statement> list = Iterations.asList(statements);

			assertEquals(0, list.size());
		}
		finally {
			statements.close();
		}
	}

	/**
	 * Test method for
	 * {@link org.openrdf.sail.memory.MemTripleSource#getStatements(org.openrdf.model.Resource, org.openrdf.model.IRI, org.openrdf.model.Value, org.openrdf.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsOneContextOnePredicateTwoContexts()
		throws Exception
	{
		loadTestData("/alp-testdata.ttl", this.alice);
		TripleSource source = getTripleSourceCommitted();

		CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, null, this.alice, this.bob);

		try {
			List<Statement> list = Iterations.asList(statements);

			assertEquals(4, list.size());
		}
		finally {
			statements.close();
		}
	}

	/**
	 * Test method for
	 * {@link org.openrdf.sail.memory.MemTripleSource#getStatements(org.openrdf.model.Resource, org.openrdf.model.IRI, org.openrdf.model.Value, org.openrdf.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsTwoContextsOnePredicateTwoContexts()
		throws Exception
	{
		loadTestData("/alp-testdata.ttl", this.alice, this.bob);
		TripleSource source = getTripleSourceCommitted();

		CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, null, this.alice, this.bob);

		try {
			List<Statement> list = Iterations.asList(statements);

			assertEquals(8, list.size());
		}
		finally {
			statements.close();
		}
	}

	/**
	 * Test method for
	 * {@link org.openrdf.sail.memory.MemTripleSource#getStatements(org.openrdf.model.Resource, org.openrdf.model.IRI, org.openrdf.model.Value, org.openrdf.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsNoContextsPredicateOwlThingTwoContexts()
		throws Exception
	{
		loadTestData("/alp-testdata.ttl");
		TripleSource source = getTripleSourceCommitted();

		CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, OWL.THING, this.alice, this.bob);

		try {
			List<Statement> list = Iterations.asList(statements);

			assertEquals(0, list.size());
		}
		finally {
			statements.close();
		}
	}

	/**
	 * Test method for
	 * {@link org.openrdf.sail.memory.MemTripleSource#getStatements(org.openrdf.model.Resource, org.openrdf.model.IRI, org.openrdf.model.Value, org.openrdf.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsOneContextPredicateOwlThingTwoContexts()
		throws Exception
	{
		loadTestData("/alp-testdata.ttl", this.alice);
		TripleSource source = getTripleSourceCommitted();

		CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, OWL.THING, this.alice, this.bob);

		try {
			List<Statement> list = Iterations.asList(statements);

			assertEquals(1, list.size());
		}
		finally {
			statements.close();
		}
	}

	/**
	 * Test method for
	 * {@link org.openrdf.sail.memory.MemTripleSource#getStatements(org.openrdf.model.Resource, org.openrdf.model.IRI, org.openrdf.model.Value, org.openrdf.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsTwoContextsPredicateOwlThingTwoContexts()
		throws Exception
	{
		loadTestData("/alp-testdata.ttl", this.alice, this.bob);
		TripleSource source = getTripleSourceCommitted();

		CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, OWL.THING, this.alice, this.bob);

		try {
			List<Statement> list = Iterations.asList(statements);

			assertEquals(2, list.size());
		}
		finally {
			statements.close();
		}
	}

	/**
	 * Test method for
	 * {@link org.openrdf.sail.memory.MemTripleSource#getStatements(org.openrdf.model.Resource, org.openrdf.model.IRI, org.openrdf.model.Value, org.openrdf.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsNoContextsPredicateOwlClassTwoContexts()
		throws Exception
	{
		loadTestData("/alp-testdata.ttl");
		TripleSource source = getTripleSourceCommitted();

		CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDF.TYPE, OWL.CLASS, this.alice, this.bob);

		try {
			List<Statement> list = Iterations.asList(statements);

			assertEquals(0, list.size());
		}
		finally {
			statements.close();
		}
	}

	/**
	 * Test method for
	 * {@link org.openrdf.sail.memory.MemTripleSource#getStatements(org.openrdf.model.Resource, org.openrdf.model.IRI, org.openrdf.model.Value, org.openrdf.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsOneContextPredicateOwlClassTwoContexts()
		throws Exception
	{
		loadTestData("/alp-testdata.ttl", this.alice);
		TripleSource source = getTripleSourceCommitted();

		CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDF.TYPE, OWL.CLASS, this.alice, this.bob);

		try {
			List<Statement> list = Iterations.asList(statements);

			assertEquals(4, list.size());
		}
		finally {
			statements.close();
		}
	}

	/**
	 * Test method for
	 * {@link org.openrdf.sail.memory.MemTripleSource#getStatements(org.openrdf.model.Resource, org.openrdf.model.IRI, org.openrdf.model.Value, org.openrdf.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsTwoContextsPredicateOwlClassTwoContexts()
		throws Exception
	{
		loadTestData("/alp-testdata.ttl", this.alice, this.bob);
		TripleSource source = getTripleSourceCommitted();

		CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDF.TYPE, OWL.CLASS, this.alice, this.bob);

		try {
			List<Statement> list = Iterations.asList(statements);

			assertEquals(8, list.size());
		}
		finally {
			statements.close();
		}
	}

	/**
	 * Test method for
	 * {@link org.openrdf.sail.memory.MemTripleSource#getStatements(org.openrdf.model.Resource, org.openrdf.model.IRI, org.openrdf.model.Value, org.openrdf.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsNoContextsPredicateOwlClassNoContexts()
		throws Exception
	{
		loadTestData("/alp-testdata.ttl");
		TripleSource source = getTripleSourceCommitted();

		CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDF.TYPE, OWL.CLASS);

		try {
			List<Statement> list = Iterations.asList(statements);

			assertEquals(4, list.size());
		}
		finally {
			statements.close();
		}
	}

	/**
	 * Test method for
	 * {@link org.openrdf.sail.memory.MemTripleSource#getStatements(org.openrdf.model.Resource, org.openrdf.model.IRI, org.openrdf.model.Value, org.openrdf.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsOneContextPredicateOwlClassNoContexts()
		throws Exception
	{
		loadTestData("/alp-testdata.ttl", this.alice);
		TripleSource source = getTripleSourceCommitted();

		CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDF.TYPE, OWL.CLASS);

		try {
			List<Statement> list = Iterations.asList(statements);

			assertEquals(4, list.size());
		}
		finally {
			statements.close();
		}
	}

	/**
	 * Test method for
	 * {@link org.openrdf.sail.memory.MemTripleSource#getStatements(org.openrdf.model.Resource, org.openrdf.model.IRI, org.openrdf.model.Value, org.openrdf.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsTwoContextsPredicateOwlClassNoContexts()
		throws Exception
	{
		loadTestData("/alp-testdata.ttl", this.alice, this.bob);
		TripleSource source = getTripleSourceCommitted();

		CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDF.TYPE, OWL.CLASS);

		try {
			List<Statement> list = Iterations.asList(statements);

			assertEquals(8, list.size());
		}
		finally {
			statements.close();
		}
	}

	/**
	 * Test method for
	 * {@link org.openrdf.sail.memory.MemTripleSource#getStatements(org.openrdf.model.Resource, org.openrdf.model.IRI, org.openrdf.model.Value, org.openrdf.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsNoContextsPredicateExClassNoContexts()
		throws Exception
	{
		loadTestData("/alp-testdata.ttl");
		TripleSource source = getTripleSourceCommitted();

		CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, f.createIRI(EX_NS, "A"));

		try {
			List<Statement> list = Iterations.asList(statements);

			assertEquals(3, list.size());
		}
		finally {
			statements.close();
		}
	}

	/**
	 * Test method for
	 * {@link org.openrdf.sail.memory.MemTripleSource#getStatements(org.openrdf.model.Resource, org.openrdf.model.IRI, org.openrdf.model.Value, org.openrdf.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsOneContextPredicateExClassNoContexts()
		throws Exception
	{
		loadTestData("/alp-testdata.ttl", this.alice);
		TripleSource source = getTripleSourceCommitted();

		CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, f.createIRI(EX_NS, "A"));

		try {
			List<Statement> list = Iterations.asList(statements);

			assertEquals(3, list.size());
		}
		finally {
			statements.close();
		}
	}

	/**
	 * Test method for
	 * {@link org.openrdf.sail.memory.MemTripleSource#getStatements(org.openrdf.model.Resource, org.openrdf.model.IRI, org.openrdf.model.Value, org.openrdf.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsTwoContextsPredicateExClassNoContexts()
		throws Exception
	{
		loadTestData("/alp-testdata.ttl", this.alice, this.bob);
		TripleSource source = getTripleSourceCommitted();

		CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, f.createIRI(EX_NS, "A"));

		try {
			List<Statement> list = Iterations.asList(statements);

			assertEquals(6, list.size());
		}
		finally {
			statements.close();
		}
	}

	/**
	 * Test method for
	 * {@link org.openrdf.sail.memory.MemTripleSource#getStatements(org.openrdf.model.Resource, org.openrdf.model.IRI, org.openrdf.model.Value, org.openrdf.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsNoContextsPredicateExClassOneContext()
		throws Exception
	{
		loadTestData("/alp-testdata.ttl");
		TripleSource source = getTripleSourceCommitted();

		CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, f.createIRI(EX_NS, "A"), this.alice);

		try {
			List<Statement> list = Iterations.asList(statements);

			assertEquals(0, list.size());
		}
		finally {
			statements.close();
		}
	}

	/**
	 * Test method for
	 * {@link org.openrdf.sail.memory.MemTripleSource#getStatements(org.openrdf.model.Resource, org.openrdf.model.IRI, org.openrdf.model.Value, org.openrdf.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsOneContextPredicateExClassOneContext()
		throws Exception
	{
		loadTestData("/alp-testdata.ttl", this.alice);
		TripleSource source = getTripleSourceCommitted();

		CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, f.createIRI(EX_NS, "A"), this.alice);

		try {
			List<Statement> list = Iterations.asList(statements);

			assertEquals(3, list.size());
		}
		finally {
			statements.close();
		}
	}

	/**
	 * Test method for
	 * {@link org.openrdf.sail.memory.MemTripleSource#getStatements(org.openrdf.model.Resource, org.openrdf.model.IRI, org.openrdf.model.Value, org.openrdf.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsTwoContextsPredicateExClassOneContext()
		throws Exception
	{
		loadTestData("/alp-testdata.ttl", this.alice, this.bob);
		TripleSource source = getTripleSourceCommitted();

		CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, f.createIRI(EX_NS, "A"), this.alice);

		try {
			List<Statement> list = Iterations.asList(statements);

			assertEquals(3, list.size());
		}
		finally {
			statements.close();
		}
	}

	/**
	 * Test method for
	 * {@link org.openrdf.sail.memory.MemTripleSource#getStatements(org.openrdf.model.Resource, org.openrdf.model.IRI, org.openrdf.model.Value, org.openrdf.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsNoContextsPredicateExClassTwoContexts()
		throws Exception
	{
		loadTestData("/alp-testdata.ttl");
		TripleSource source = getTripleSourceCommitted();

		CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, f.createIRI(EX_NS, "A"), this.alice, this.bob);

		try {
			List<Statement> list = Iterations.asList(statements);

			assertEquals(0, list.size());
		}
		finally {
			statements.close();
		}
	}

	/**
	 * Test method for
	 * {@link org.openrdf.sail.memory.MemTripleSource#getStatements(org.openrdf.model.Resource, org.openrdf.model.IRI, org.openrdf.model.Value, org.openrdf.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsOneContextPredicateExClassTwoContexts()
		throws Exception
	{
		loadTestData("/alp-testdata.ttl", this.alice);
		TripleSource source = getTripleSourceCommitted();

		CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, f.createIRI(EX_NS, "A"), this.alice, this.bob);

		try {
			List<Statement> list = Iterations.asList(statements);

			assertEquals(3, list.size());
		}
		finally {
			statements.close();
		}
	}

	/**
	 * Test method for
	 * {@link org.openrdf.sail.memory.MemTripleSource#getStatements(org.openrdf.model.Resource, org.openrdf.model.IRI, org.openrdf.model.Value, org.openrdf.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsTwoContextsPredicateExClassTwoContexts()
		throws Exception
	{
		loadTestData("/alp-testdata.ttl", this.alice, this.bob);
		TripleSource source = getTripleSourceCommitted();

		CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				RDFS.SUBCLASSOF, f.createIRI(EX_NS, "A"), this.alice, this.bob);

		try {
			List<Statement> list = Iterations.asList(statements);

			assertEquals(6, list.size());
		}
		finally {
			statements.close();
		}
	}

	/**
	 * Test method for
	 * {@link org.openrdf.sail.memory.MemTripleSource#getStatements(org.openrdf.model.Resource, org.openrdf.model.IRI, org.openrdf.model.Value, org.openrdf.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsNoContextsExClassPredicateTwoContexts()
		throws Exception
	{
		loadTestData("/alp-testdata.ttl");
		TripleSource source = getTripleSourceCommitted();

		CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(
				f.createIRI(EX_NS, "C"), RDFS.SUBCLASSOF, null, this.alice, this.bob);

		try {
			List<Statement> list = Iterations.asList(statements);

			assertEquals(0, list.size());
		}
		finally {
			statements.close();
		}
	}

	/**
	 * Test method for
	 * {@link org.openrdf.sail.memory.MemTripleSource#getStatements(org.openrdf.model.Resource, org.openrdf.model.IRI, org.openrdf.model.Value, org.openrdf.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsOneContextExClassPredicateTwoContexts()
		throws Exception
	{
		loadTestData("/alp-testdata.ttl", this.alice);
		TripleSource source = getTripleSourceCommitted();

		CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(
				f.createIRI(EX_NS, "C"), RDFS.SUBCLASSOF, null, this.alice, this.bob);

		try {
			List<Statement> list = Iterations.asList(statements);

			assertEquals(1, list.size());
		}
		finally {
			statements.close();
		}
	}

	/**
	 * Test method for
	 * {@link org.openrdf.sail.memory.MemTripleSource#getStatements(org.openrdf.model.Resource, org.openrdf.model.IRI, org.openrdf.model.Value, org.openrdf.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsTwoContextsExClassPredicateTwoContexts()
		throws Exception
	{
		loadTestData("/alp-testdata.ttl", this.alice, this.bob);
		TripleSource source = getTripleSourceCommitted();

		CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(
				f.createIRI(EX_NS, "C"), RDFS.SUBCLASSOF, null, this.alice, this.bob);

		try {
			List<Statement> list = Iterations.asList(statements);

			assertEquals(2, list.size());
		}
		finally {
			statements.close();
		}
	}

	/**
	 * Test method for
	 * {@link org.openrdf.sail.memory.MemTripleSource#getStatements(org.openrdf.model.Resource, org.openrdf.model.IRI, org.openrdf.model.Value, org.openrdf.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsNoContextsExClassPredicateNoContexts()
		throws Exception
	{
		loadTestData("/alp-testdata.ttl");
		TripleSource source = getTripleSourceCommitted();

		CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(
				f.createIRI(EX_NS, "C"), RDFS.SUBCLASSOF, null);

		try {
			List<Statement> list = Iterations.asList(statements);
			
			assertEquals(1, list.size());
		}
		finally {
			statements.close();
		}
	}

	/**
	 * Test method for
	 * {@link org.openrdf.sail.memory.MemTripleSource#getStatements(org.openrdf.model.Resource, org.openrdf.model.IRI, org.openrdf.model.Value, org.openrdf.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsOneContextExClassPredicateNoContexts()
		throws Exception
	{
		loadTestData("/alp-testdata.ttl", this.alice);
		TripleSource source = getTripleSourceCommitted();

		CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(
				f.createIRI(EX_NS, "C"), RDFS.SUBCLASSOF, null);

		try {
			List<Statement> list = Iterations.asList(statements);

			assertEquals(1, list.size());
		}
		finally {
			statements.close();
		}
	}

	/**
	 * Test method for
	 * {@link org.openrdf.sail.memory.MemTripleSource#getStatements(org.openrdf.model.Resource, org.openrdf.model.IRI, org.openrdf.model.Value, org.openrdf.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsTwoContextsExClassPredicateNoContexts()
		throws Exception
	{
		loadTestData("/alp-testdata.ttl", this.alice, this.bob);
		TripleSource source = getTripleSourceCommitted();

		CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(
				f.createIRI(EX_NS, "C"), RDFS.SUBCLASSOF, null);

		try {
			List<Statement> list = Iterations.asList(statements);

			assertEquals(2, list.size());
		}
		finally {
			statements.close();
		}
	}

	/**
	 * Test method for
	 * {@link org.openrdf.sail.memory.MemTripleSource#getStatements(org.openrdf.model.Resource, org.openrdf.model.IRI, org.openrdf.model.Value, org.openrdf.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsThreeContextsAllNull()
		throws Exception
	{
		loadTestData("/alp-testdata.ttl", this.alice, this.bob, this.mary);
		TripleSource source = getTripleSourceCommitted();

		CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				null, null);

		try {
			List<Statement> list = Iterations.asList(statements);

			assertEquals(24, list.size());
		}
		finally {
			statements.close();
		}
	}

	/**
	 * Test method for
	 * {@link org.openrdf.sail.memory.MemTripleSource#getStatements(org.openrdf.model.Resource, org.openrdf.model.IRI, org.openrdf.model.Value, org.openrdf.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsThreeContextsOneContext()
		throws Exception
	{
		loadTestData("/alp-testdata.ttl", this.alice, this.bob, this.mary);
		TripleSource source = getTripleSourceCommitted();

		CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				null, null, this.alice);

		try {
			List<Statement> list = Iterations.asList(statements);

			assertEquals(8, list.size());
		}
		finally {
			statements.close();
		}
	}

	/**
	 * Test method for
	 * {@link org.openrdf.sail.memory.MemTripleSource#getStatements(org.openrdf.model.Resource, org.openrdf.model.IRI, org.openrdf.model.Value, org.openrdf.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsThreeContextsTwoContexts()
		throws Exception
	{
		loadTestData("/alp-testdata.ttl", this.alice, this.bob, this.mary);
		TripleSource source = getTripleSourceCommitted();

		CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				null, null, this.alice, this.bob);

		try {
			List<Statement> list = Iterations.asList(statements);

			assertEquals(16, list.size());
		}
		finally {
			statements.close();
		}
	}

	/**
	 * Test method for
	 * {@link org.openrdf.sail.memory.MemTripleSource#getStatements(org.openrdf.model.Resource, org.openrdf.model.IRI, org.openrdf.model.Value, org.openrdf.model.Resource[])}
	 * .
	 */
	@Test
	public final void testGetStatementsThreeContextsThreeContexts()
		throws Exception
	{
		loadTestData("/alp-testdata.ttl", this.alice, this.bob, this.mary);
		TripleSource source = getTripleSourceCommitted();

		CloseableIteration<? extends Statement, QueryEvaluationException> statements = source.getStatements(null,
				null, null, this.alice, this.bob, this.mary);

		try {
			List<Statement> list = Iterations.asList(statements);

			assertEquals(24, list.size());
		}
		finally {
			statements.close();
		}
	}

	protected void loadTestData(String dataFile, Resource... contexts)
		throws RDFParseException, IOException, SailException
	{
		logger.debug("loading dataset {}", dataFile);
		InputStream dataset = this.getClass().getResourceAsStream(dataFile);
		SailConnection con = store.getConnection();
		try {
			con.begin();
			for (Statement nextStatement : Rio.parse(dataset, "", RDFFormat.TURTLE, contexts)) {
				con.addStatement(nextStatement.getSubject(), nextStatement.getPredicate(),
						nextStatement.getObject(), nextStatement.getContext());
			}
		}
		finally {
			con.commit();
			con.close();
			dataset.close();
		}
		logger.debug("dataset loaded.");
	}

	/**
	 * Helper method to avoid writing this constructor multiple times. It needs
	 * to be created after statements are added and committed.
	 * 
	 * @return
	 * @throws SailException 
	 */
	private TripleSource getTripleSourceCommitted()
		throws SailException
	{
		IsolationLevel level = store.getDefaultIsolationLevel();
		source = store.getSailStore().getExplicitSailSource().fork();
		snapshot = source.dataset(level);
		final ValueFactory vf = store.getValueFactory();
		return new TripleSource() {

			public CloseableIteration<? extends Statement, QueryEvaluationException> getStatements(
					Resource subj, IRI pred, Value obj, Resource... contexts)
				throws QueryEvaluationException
			{
				try {
					return new ExceptionConvertingIteration<Statement, QueryEvaluationException>(
							snapshot.getStatements(subj, pred, obj, contexts))
					{

						protected QueryEvaluationException convert(Exception e) {
							return new QueryEvaluationException(e);
						}
					};
				}
				catch (SailException e) {
					throw new QueryEvaluationException(e);
				}
			}

			public ValueFactory getValueFactory() {
				return vf;
			}
		};
	}

}
