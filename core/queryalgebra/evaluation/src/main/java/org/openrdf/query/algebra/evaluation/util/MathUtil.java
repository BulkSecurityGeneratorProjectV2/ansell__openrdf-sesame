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
package org.openrdf.query.algebra.evaluation.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

import org.openrdf.model.IRI;
import org.openrdf.model.Literal;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.datatypes.XMLDatatypeUtil;
import org.openrdf.model.impl.DecimalLiteral;
import org.openrdf.model.impl.IntegerLiteral;
import org.openrdf.model.impl.NumericLiteral;
import org.openrdf.model.impl.SimpleValueFactory;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.algebra.MathExpr.MathOp;
import org.openrdf.query.algebra.evaluation.ValueExprEvaluationException;

/**
 * A utility class for evaluation of mathematical expressions on RDF literals.
 * 
 * @author Jeen Broekstra
 */
public class MathUtil {

	/**
	 * The default expansion scale used in division operations resulting
	 * in a decimal value with non-terminating decimal expansion. The OpenRDF
	 * default is 24 digits, a value used in various other SPARQL
	 * implementations, to make comparison between these systems easy.
	 */
	public static final int DEFAULT_DECIMAL_EXPANSION_SCALE = 24;

	private static int decimalExpansionScale = DEFAULT_DECIMAL_EXPANSION_SCALE;

	/**
	 * Computes the result of applying the supplied math operator on the supplied
	 * left and right operand.
	 * 
	 * @param leftLit
	 *        a numeric datatype literal
	 * @param rightLit
	 *        a numeric datatype literal
	 * @param op
	 *        a mathematical operator, as definied by MathExpr.MathOp.
	 * @return a numeric datatype literal containing the result of the operation.
	 *         The result will be datatype according to the most specific data
	 *         type the two operands have in common per the SPARQL/XPath spec.
	 * @throws ValueExprEvaluationException
	 */
	public static Literal compute(Literal leftLit, Literal rightLit, MathOp op)
		throws ValueExprEvaluationException
	{
		final ValueFactory vf = SimpleValueFactory.getInstance();
		
		IRI leftDatatype = leftLit.getDatatype();
		IRI rightDatatype = rightLit.getDatatype();

		// Only numeric value can be used in math expressions
		if (!XMLDatatypeUtil.isNumericDatatype(leftDatatype)) {
			throw new ValueExprEvaluationException("Not a number: " + leftLit);
		}
		if (!XMLDatatypeUtil.isNumericDatatype(rightDatatype)) {
			throw new ValueExprEvaluationException("Not a number: " + rightLit);
		}

		// Determine most specific datatype that the arguments have in common,
		// choosing from xsd:integer, xsd:decimal, xsd:float and xsd:double as
		// per the SPARQL/XPATH spec
		IRI commonDatatype;

		if (leftDatatype.equals(XMLSchema.DOUBLE) || rightDatatype.equals(XMLSchema.DOUBLE)) {
			commonDatatype = XMLSchema.DOUBLE;
		}
		else if (leftDatatype.equals(XMLSchema.FLOAT) || rightDatatype.equals(XMLSchema.FLOAT)) {
			commonDatatype = XMLSchema.FLOAT;
		}
		else if (leftDatatype.equals(XMLSchema.DECIMAL) || rightDatatype.equals(XMLSchema.DECIMAL)) {
			commonDatatype = XMLSchema.DECIMAL;
		}
		else if (op == MathOp.DIVIDE) {
			// Result of integer divide is decimal and requires the arguments to
			// be handled as such, see for details:
			// http://www.w3.org/TR/xpath-functions/#func-numeric-divide
			commonDatatype = XMLSchema.DECIMAL;
		}
		else {
			commonDatatype = XMLSchema.INTEGER;
		}

		// Note: Java already handles cases like divide-by-zero appropriately
		// for floats and doubles, see:
		// http://www.particle.kth.se/~lindsey/JavaCourse/Book/Part1/Tech/
		// Chapter02/floatingPt2.html

		try {
			if (commonDatatype.equals(XMLSchema.DOUBLE)) {
				double left = leftLit.doubleValue();
				double right = rightLit.doubleValue();

				switch (op) {
					case PLUS:
						return vf.createLiteral(left + right);
					case MINUS:
						return vf.createLiteral(left - right);
					case MULTIPLY:
						return vf.createLiteral(left * right);
					case DIVIDE:
						return vf.createLiteral(left / right);
					default:
						throw new IllegalArgumentException("Unknown operator: " + op);
				}
			}
			else if (commonDatatype.equals(XMLSchema.FLOAT)) {
				float left = leftLit.floatValue();
				float right = rightLit.floatValue();

				switch (op) {
					case PLUS:
						return vf.createLiteral(left + right);
					case MINUS:
						return vf.createLiteral(left - right);
					case MULTIPLY:
						return vf.createLiteral(left * right);
					case DIVIDE:
						return vf.createLiteral(left / right);
					default:
						throw new IllegalArgumentException("Unknown operator: " + op);
				}
			}
			else if (commonDatatype.equals(XMLSchema.DECIMAL)) {
				BigDecimal left = leftLit.decimalValue();
				BigDecimal right = rightLit.decimalValue();

				switch (op) {
					case PLUS:
						return vf.createLiteral(left.add(right));
					case MINUS:
						return vf.createLiteral(left.subtract(right));
					case MULTIPLY:
						return vf.createLiteral(left.multiply(right));
					case DIVIDE:
						// Divide by zero handled through NumberFormatException
						BigDecimal result = null;
						try {
							// try to return the exact quotient if possible.
							result = left.divide(right, MathContext.UNLIMITED);
						}
						catch (ArithmeticException e) {
							// non-terminating decimal expansion in quotient, using
							// scaling and rounding.
							result = left.setScale(getDecimalExpansionScale(), RoundingMode.HALF_UP).divide(right,
									RoundingMode.HALF_UP);
						}

						return vf.createLiteral(result);
					default:
						throw new IllegalArgumentException("Unknown operator: " + op);
				}
			}
			else { // XMLSchema.INTEGER
				BigInteger left = leftLit.integerValue();
				BigInteger right = rightLit.integerValue();

				switch (op) {
					case PLUS:
						return vf.createLiteral(left.add(right));
					case MINUS:
						return vf.createLiteral(left.subtract(right));
					case MULTIPLY:
						return vf.createLiteral(left.multiply(right));
					case DIVIDE:
						throw new RuntimeException("Integer divisions should be processed as decimal divisions");
					default:
						throw new IllegalArgumentException("Unknown operator: " + op);
				}
			}
		}
		catch (NumberFormatException e) {
			throw new ValueExprEvaluationException(e);
		}
		catch (ArithmeticException e) {
			throw new ValueExprEvaluationException(e);
		}
	}

	/**
	 * Returns the decimal expansion scale used in division operations resulting
	 * in a decimal value with non-terminating decimal expansion. By default,
	 * this value is set to 24.
	 * 
	 * @return The decimal expansion scale.
	 */
	public static int getDecimalExpansionScale() {
		return decimalExpansionScale;
	}

	/**
	 * Sets the decimal expansion scale used in divisions resulting in a decimal
	 * value with non-terminating decimal expansion.
	 * 
	 * @param decimalExpansionScale
	 *        The decimal expansion scale to set. Note that a mimimum of 18 is
	 *        required to stay compliant with the XPath specification of
	 *        xsd:decimal operations.
	 */
	public static void setDecimalExpansionScale(int decimalExpansionScale) {
		MathUtil.decimalExpansionScale = decimalExpansionScale;
	}

}
