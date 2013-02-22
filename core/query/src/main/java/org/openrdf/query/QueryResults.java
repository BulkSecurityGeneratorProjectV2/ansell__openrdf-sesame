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
package org.openrdf.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.datatype.XMLGregorianCalendar;

import info.aduna.iteration.DistinctIteration;
import info.aduna.iteration.Iterations;

import org.openrdf.model.BNode;
import org.openrdf.model.Graph;
import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.datatypes.XMLDatatypeUtil;
import org.openrdf.model.impl.GraphImpl;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.util.ModelUtil;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;

/**
 * Utility methods related to query results.
 * 
 * @since 2.7.0
 * @author Jeen Broekstra
 */
public class QueryResults extends Iterations {

	/**
	 * Get a {@link Model} containing all elements obtained from the specified
	 * query result.
	 * 
	 * @since 2.7.0
	 * @param gqr
	 *        the {@link GraphQueryResult} to get the statements from
	 * @return a {@link Model} containing all statements obtained from the
	 *         specified query result.
	 */
	public static Model asModel(GraphQueryResult gqr)
		throws QueryEvaluationException
	{
		Model model = new LinkedHashModel();
		addAll(gqr, model);
		return model;
	}

	/**
	 * Returns a single element from the query result. The QueryResult is
	 * automatically closed by this method.
	 * 
	 * @since 2.7.0
	 * @return a single query result element.
	 * @throws QueryEvaluationException
	 */
	public static Statement singleResult(GraphQueryResult result)
		throws QueryEvaluationException
	{
		Statement singleResult = null;
		if (result.hasNext()) {
			singleResult = result.next();
		}
		result.close();
		return singleResult;
	}

	/**
	 * Returns a single element from the query result. The QueryResult is
	 * automatically closed by this method.
	 * 
	 * @since 2.7.0
	 * @return a single query result element.
	 * @throws QueryEvaluationException
	 */
	public static BindingSet singleResult(TupleQueryResult result)
		throws QueryEvaluationException
	{
		BindingSet singleResult = null;
		if (result.hasNext()) {
			singleResult = result.next();
		}
		result.close();
		return singleResult;
	}

	/**
	 * Returns a {@link GraphQueryResult} that filters out any duplicate
	 * solutions from the supplied queryResult.
	 * 
	 * @param queryResult
	 *        a queryResult containing possible duplicate statements.
	 * @return a {@link GraphQueryResult} with any duplicates filtered out.
	 * @since 2.7.0
	 */
	public static GraphQueryResult distinctResults(GraphQueryResult queryResult) {
		return new GraphQueryResultFilter(queryResult);
	}

	/**
	 * Returns a {@link TupleQueryResult} that filters out any duplicate
	 * solutions from the supplied queryResult.
	 * 
	 * @param queryResult
	 *        a queryResult containing possible duplicate solutions.
	 * @return a {@link TupleQueryResult} with any duplicates filtered out.
	 * @since 2.7.0
	 */
	public static TupleQueryResult distinctResults(TupleQueryResult queryResult) {
		return new TupleQueryResultFilter(queryResult);
	}

	/**
	 * Reports a tuple query result to a {@link TupleQueryResultHandler}.
	 * 
	 * @param tqr
	 *        The query result to report.
	 * @param handler
	 *        The handler to report the query result to.
	 * @throws TupleQueryResultHandlerException
	 *         If such an exception is thrown by the used query result writer.
	 */
	public static void report(TupleQueryResult tqr, TupleQueryResultHandler handler)
		throws TupleQueryResultHandlerException, QueryEvaluationException
	{
		handler.startQueryResult(tqr.getBindingNames());
		try {
			while (tqr.hasNext()) {
				BindingSet bindingSet = tqr.next();
				handler.handleSolution(bindingSet);
			}
		}
		finally {
			tqr.close();
		}
		handler.endQueryResult();
	}

	/**
	 * Reports a graph query result to an {@link RDFHandler}.
	 * 
	 * @param gqr
	 *        The query result to report.
	 * @param rdfHandler
	 *        The handler to report the query result to.
	 * @throws RDFHandlerException
	 *         If such an exception is thrown by the used RDF writer.
	 * @throws QueryEvaluationException
	 */
	public static void report(GraphQueryResult gqr, RDFHandler rdfHandler)
		throws RDFHandlerException, QueryEvaluationException
	{
		try {
			rdfHandler.startRDF();

			for (Map.Entry<String, String> entry : gqr.getNamespaces().entrySet()) {
				String prefix = entry.getKey();
				String namespace = entry.getValue();
				rdfHandler.handleNamespace(prefix, namespace);
			}

			while (gqr.hasNext()) {
				Statement st = gqr.next();
				rdfHandler.handleStatement(st);
			}

			rdfHandler.endRDF();
		}
		finally {
			gqr.close();
		}
	}

	/**
	 * Compares the two query results by converting them to graphs and returns
	 * true if they are equal. QueryResults are equal if they contain the same
	 * set of BindingSet and have the headers. Blank nodes identifiers are not
	 * relevant for equality, they are mapped from one model to the other by
	 * using the attached properties. Note that the method consumes both query
	 * results fully.
	 * 
	 * @throws QueryEvaluationException
	 */
	public static boolean equals(TupleQueryResult tqr1, TupleQueryResult tqr2)
		throws QueryEvaluationException
	{
		List<BindingSet> list1 = Iterations.asList(tqr1);
		List<BindingSet> list2 = Iterations.asList(tqr2);

		// Compare the number of statements in both sets
		if (list1.size() != list2.size()) {
			return false;
		}

		return matchBindingSets(list1, list2);
	}

	public static boolean isSubset(TupleQueryResult tqr1, TupleQueryResult tqr2)
		throws QueryEvaluationException
	{
		List<BindingSet> list1 = Iterations.asList(tqr1);
		List<BindingSet> list2 = Iterations.asList(tqr2);

		// Compare the number of statements in both sets
		if (list1.size() > list2.size()) {
			return false;
		}

		return matchBindingSets(list1, list2);
	}

	public static boolean equals(GraphQueryResult result1, GraphQueryResult result2)
		throws QueryEvaluationException
	{
		Set<? extends Statement> graph1 = Iterations.asSet(result1);
		Set<? extends Statement> graph2 = Iterations.asSet(result1);

		return ModelUtil.equals(graph1, graph2);
	}

	private static boolean matchBindingSets(List<? extends BindingSet> queryResult1,
			Iterable<? extends BindingSet> queryResult2)
	{
		return matchBindingSets(queryResult1, queryResult2, new HashMap<BNode, BNode>(), 0);
	}

	/**
	 * A recursive method for finding a complete mapping between blank nodes in
	 * queryResult1 and blank nodes in queryResult2. The algorithm does a
	 * depth-first search trying to establish a mapping for each blank node
	 * occurring in queryResult1.
	 * 
	 * @return true if a complete mapping has been found, false otherwise.
	 */
	private static boolean matchBindingSets(List<? extends BindingSet> queryResult1,
			Iterable<? extends BindingSet> queryResult2, Map<BNode, BNode> bNodeMapping, int idx)
	{
		boolean result = false;

		if (idx < queryResult1.size()) {
			BindingSet bs1 = queryResult1.get(idx);

			List<BindingSet> matchingBindingSets = findMatchingBindingSets(bs1, queryResult2, bNodeMapping);

			for (BindingSet bs2 : matchingBindingSets) {
				// Map bNodes in bs1 to bNodes in bs2
				Map<BNode, BNode> newBNodeMapping = new HashMap<BNode, BNode>(bNodeMapping);

				for (Binding binding : bs1) {
					if (binding.getValue() instanceof BNode) {
						newBNodeMapping.put((BNode)binding.getValue(), (BNode)bs2.getValue(binding.getName()));
					}
				}

				// FIXME: this recursive implementation has a high risk of
				// triggering a stack overflow

				// Enter recursion
				result = matchBindingSets(queryResult1, queryResult2, newBNodeMapping, idx + 1);

				if (result == true) {
					// models match, look no further
					break;
				}
			}
		}
		else {
			// All statements have been mapped successfully
			result = true;
		}

		return result;
	}

	private static List<BindingSet> findMatchingBindingSets(BindingSet st,
			Iterable<? extends BindingSet> model, Map<BNode, BNode> bNodeMapping)
	{
		List<BindingSet> result = new ArrayList<BindingSet>();

		for (BindingSet modelSt : model) {
			if (bindingSetsMatch(st, modelSt, bNodeMapping)) {
				// All components possibly match
				result.add(modelSt);
			}
		}

		return result;
	}

	private static boolean bindingSetsMatch(BindingSet bs1, BindingSet bs2, Map<BNode, BNode> bNodeMapping) {

		if (bs1.size() != bs2.size()) {
			return false;
		}

		for (Binding binding1 : bs1) {
			Value value1 = binding1.getValue();
			Value value2 = bs2.getValue(binding1.getName());

			if (value1 instanceof BNode && value2 instanceof BNode) {
				BNode mappedBNode = bNodeMapping.get(value1);

				if (mappedBNode != null) {
					// bNode 'value1' was already mapped to some other bNode
					if (!value2.equals(mappedBNode)) {
						// 'value1' and 'value2' do not match
						return false;
					}
				}
				else {
					// 'value1' was not yet mapped, we need to check if 'value2' is a
					// possible mapping candidate
					if (bNodeMapping.containsValue(value2)) {
						// 'value2' is already mapped to some other value.
						return false;
					}
				}
			}
			else {
				// values are not (both) bNodes
				if (value1 instanceof Literal && value2 instanceof Literal) {
					// do literal value-based comparison for supported datatypes
					Literal leftLit = (Literal)value1;
					Literal rightLit = (Literal)value2;

					URI dt1 = leftLit.getDatatype();
					URI dt2 = rightLit.getDatatype();

					if (dt1 != null && dt2 != null && dt1.equals(dt2)
							&& XMLDatatypeUtil.isValidValue(leftLit.getLabel(), dt1)
							&& XMLDatatypeUtil.isValidValue(rightLit.getLabel(), dt2))
					{
						Integer compareResult = null;
						if (dt1.equals(XMLSchema.DOUBLE)) {
							compareResult = Double.compare(leftLit.doubleValue(), rightLit.doubleValue());
						}
						else if (dt1.equals(XMLSchema.FLOAT)) {
							compareResult = Float.compare(leftLit.floatValue(), rightLit.floatValue());
						}
						else if (dt1.equals(XMLSchema.DECIMAL)) {
							compareResult = leftLit.decimalValue().compareTo(rightLit.decimalValue());
						}
						else if (XMLDatatypeUtil.isIntegerDatatype(dt1)) {
							compareResult = leftLit.integerValue().compareTo(rightLit.integerValue());
						}
						else if (dt1.equals(XMLSchema.BOOLEAN)) {
							Boolean leftBool = Boolean.valueOf(leftLit.booleanValue());
							Boolean rightBool = Boolean.valueOf(rightLit.booleanValue());
							compareResult = leftBool.compareTo(rightBool);
						}
						else if (XMLDatatypeUtil.isCalendarDatatype(dt1)) {
							XMLGregorianCalendar left = leftLit.calendarValue();
							XMLGregorianCalendar right = rightLit.calendarValue();

							compareResult = left.compare(right);
						}

						if (compareResult != null) {
							if (compareResult.intValue() != 0) {
								return false;
							}
						}
						else if (!value1.equals(value2)) {
							return false;
						}
					}
					else if (!value1.equals(value2)) {
						return false;
					}
				}
				else if (!value1.equals(value2)) {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Check whether two {@link BindingSet}s are compatible. Two binding sets are
	 * compatible if they have equal values for each binding name that occurs in
	 * both binding sets.
	 */
	public static boolean bindingSetsCompatible(BindingSet bs1, BindingSet bs2) {
		Set<String> sharedBindings = new HashSet<String>(bs1.getBindingNames());
		sharedBindings.retainAll(bs2.getBindingNames());

		for (String bindingName : sharedBindings) {
			Value value1 = bs1.getValue(bindingName);
			Value value2 = bs2.getValue(bindingName);

			if (!value1.equals(value2)) {
				return false;
			}
		}

		return true;
	}

	private static class GraphQueryResultFilter implements GraphQueryResult {

		private DistinctIteration<Statement, QueryEvaluationException> filter;

		private GraphQueryResult unfiltered;

		public GraphQueryResultFilter(GraphQueryResult wrappedResult) {
			this.filter = new DistinctIteration<Statement, QueryEvaluationException>(wrappedResult);
			this.unfiltered = wrappedResult;
		}

		@Override
		public void close()
			throws QueryEvaluationException
		{
			filter.close();
		}

		@Override
		public boolean hasNext()
			throws QueryEvaluationException
		{
			return filter.hasNext();
		}

		@Override
		public Statement next()
			throws QueryEvaluationException
		{
			return filter.next();
		}

		@Override
		public void remove()
			throws QueryEvaluationException
		{
			filter.remove();
		}

		@Override
		public Map<String, String> getNamespaces()
			throws QueryEvaluationException
		{
			return unfiltered.getNamespaces();
		}
	}

	private static class TupleQueryResultFilter implements TupleQueryResult {

		private DistinctIteration<BindingSet, QueryEvaluationException> filter;

		private TupleQueryResult unfiltered;

		public TupleQueryResultFilter(TupleQueryResult wrappedResult) {
			this.filter = new DistinctIteration<BindingSet, QueryEvaluationException>(wrappedResult);
			this.unfiltered = wrappedResult;
		}

		@Override
		public void close()
			throws QueryEvaluationException
		{
			filter.close();
		}

		@Override
		public boolean hasNext()
			throws QueryEvaluationException
		{
			return filter.hasNext();
		}

		@Override
		public BindingSet next()
			throws QueryEvaluationException
		{
			return filter.next();
		}

		@Override
		public void remove()
			throws QueryEvaluationException
		{
			filter.remove();
		}

		@Override
		public List<String> getBindingNames()
			throws QueryEvaluationException
		{
			return unfiltered.getBindingNames();
		}

	}
}