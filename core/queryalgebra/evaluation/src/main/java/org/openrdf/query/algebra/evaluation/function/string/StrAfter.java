/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2011.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.query.algebra.evaluation.function.string;

import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.FN;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.algebra.evaluation.ValueExprEvaluationException;
import org.openrdf.query.algebra.evaluation.function.Function;

/**
 * The SPARQL built-in {@link Function} STRAFTER, as defined in <a
 * href="http://www.w3.org/TR/sparql11-query/#func-substr">SPARQL Query Language
 * for RDF</a>.
 * 
 * @author Jeen Broekstra
 */
public class StrAfter implements Function {

	public String getURI() {
		return FN.SUBSTRING_AFTER.toString();
	}

	public Literal evaluate(ValueFactory valueFactory, Value... args)
		throws ValueExprEvaluationException
	{
		if (args.length != 2) {
			throw new ValueExprEvaluationException("Incorrect number of arguments for STRAFTER: " + args.length);
		}

		Value leftArg = args[0];
		Value rightArg = args[1];

		if (leftArg instanceof Literal && rightArg instanceof Literal) {
			Literal leftLit = (Literal)leftArg;
			Literal rightLit = (Literal)rightArg;

			String leftLanguage = leftLit.getLanguage();
			String rightLanguage = rightLit.getLanguage();
			
			URI leftDt = leftLit.getDatatype();
			URI rightDt = rightLit.getDatatype();
			
			// STRAFTER function accepts only plain literals (optionally
			// language-tagged) or string-typed literals.
			if ((leftLanguage != null
					|| (leftDt == null || XMLSchema.STRING.equals(leftDt)))
					&& (rightLanguage == null || rightLanguage.equals(leftLanguage))
					&& (rightDt == null || XMLSchema.STRING.equals(rightDt)))
			{
				String lexicalValue = leftLit.getLabel();
				String substring = rightLit.getLabel();
				
				int index = lexicalValue.indexOf(substring);

				String substringAfter = "";
				if (index > -1) {
					index += substring.length() - 1;
					substringAfter = lexicalValue.substring(index + 1, lexicalValue.length());
				}
				
				if (leftLanguage != null) {
					return valueFactory.createLiteral(substringAfter, leftLanguage);
				}
				else if (leftDt != null) {
					return valueFactory.createLiteral(substringAfter, leftDt);
				}
				else {
					return valueFactory.createLiteral(substringAfter);
				}
			}
			else {
				throw new ValueExprEvaluationException("incompatible operands for STRAFTER: " + leftArg + ", " + rightArg);
			}
		}
		else {
			throw new ValueExprEvaluationException("incompatible operands for STRAFTER: " + leftArg + ", " + rightArg);
		}
	}
}
