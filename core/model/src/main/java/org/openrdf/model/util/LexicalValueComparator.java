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
package org.openrdf.model.util;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Optional;

import info.aduna.lang.ObjectUtil;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.IRI;
import org.openrdf.model.Value;
import org.openrdf.model.datatypes.XMLDatatypeUtil;

/**
 * A lexical rdf term Comparator, this class does not compare numerically and is
 * therefore a bit faster than a SPARQL compliant comparator.
 * 
 * @author james
 * @author Arjohn Kampman
 */
public class LexicalValueComparator implements Serializable, Comparator<Value> {

	private static final long serialVersionUID = -7055973992568220217L;

	public int compare(Value o1, Value o2) {
		// check equality
		if (ObjectUtil.nullEquals(o1, o2)) {
			return 0;
		}

		// 1. (Lowest) no value assigned to the variable
		if (o1 == null) {
			return -1;
		}
		if (o2 == null) {
			return 1;
		}

		// 2. Blank nodes
		boolean b1 = o1 instanceof BNode;
		boolean b2 = o2 instanceof BNode;
		if (b1 && b2) {
			return compareBNodes((BNode)o1, (BNode)o2);
		}
		if (b1) {
			return -1;
		}
		if (b2) {
			return 1;
		}

		// 3. IRIs
		boolean u1 = o1 instanceof IRI;
		boolean u2 = o2 instanceof IRI;
		if (u1 && u2) {
			return compareURIs((IRI)o1, (IRI)o2);
		}
		if (u1) {
			return -1;
		}
		if (u2) {
			return 1;
		}

		// 4. RDF literals
		return compareLiterals((Literal)o1, (Literal)o2);
	}

	private int compareBNodes(BNode leftBNode, BNode rightBNode) {
		return leftBNode.getID().compareTo(rightBNode.getID());
	}

	private int compareURIs(IRI leftURI, IRI rightURI) {
		return leftURI.toString().compareTo(rightURI.toString());
	}

	private int compareLiterals(Literal leftLit, Literal rightLit) {
		// Additional constraint for ORDER BY: "A plain literal is lower
		// than an RDF literal with type xsd:string of the same lexical
		// form."
		int result = 0;
		// FIXME: Confirm these rules work with RDF-1.1
		// Sort by datatype first, plain literals come before datatyped literals
		IRI leftDatatype = leftLit.getDatatype();
		IRI rightDatatype = rightLit.getDatatype();

		if (leftDatatype != null) {
			if (rightDatatype != null) {
				// Both literals have datatypes
				result = compareDatatypes(leftDatatype, rightDatatype);
			}
			else {
				result = 1;
			}
		}
		else if (rightDatatype != null) {
			result = -1;
		}

		if (result == 0) {
			// datatypes are equal or both literals are untyped; sort by language
			// tags, simple literals come before literals with language tags
			Optional<String> leftLanguage = leftLit.getLanguage();
			Optional<String> rightLanguage = rightLit.getLanguage();

			if (leftLanguage.isPresent()) {
				if (rightLanguage.isPresent()) {
					result = leftLanguage.get().compareTo(rightLanguage.get());
				}
				else {
					result = 1;
				}
			}
			else if (rightLanguage.isPresent()) {
				result = -1;
			}
		}

		if (result == 0) {
			// Literals are equal as fas as their datatypes and language tags are
			// concerned, compare their labels
			result = leftLit.getLabel().compareTo(rightLit.getLabel());
		}

		return result;
	}

	/**
	 * Compares two literal datatypes and indicates if one should be ordered
	 * after the other. This algorithm ensures that compatible ordered datatypes
	 * (numeric and date/time) are grouped together so that
	 * {@link QueryEvaluationUtil#compareLiterals(Literal, Literal, CompareOp)}
	 * is used in consecutive ordering steps.
	 */
	private int compareDatatypes(IRI leftDatatype, IRI rightDatatype) {
		if (XMLDatatypeUtil.isNumericDatatype(leftDatatype)) {
			if (XMLDatatypeUtil.isNumericDatatype(rightDatatype)) {
				// both are numeric datatypes
				return compareURIs(leftDatatype, rightDatatype);
			}
			else {
				return -1;
			}
		}
		else if (XMLDatatypeUtil.isNumericDatatype(rightDatatype)) {
			return 1;
		}
		else if (XMLDatatypeUtil.isCalendarDatatype(leftDatatype)) {
			if (XMLDatatypeUtil.isCalendarDatatype(rightDatatype)) {
				// both are calendar datatypes
				return compareURIs(leftDatatype, rightDatatype);
			}
			else {
				return -1;
			}
		}
		else if (XMLDatatypeUtil.isCalendarDatatype(rightDatatype)) {
			return 1;
		}
		else {
			// incompatible or unordered datatypes
			return compareURIs(leftDatatype, rightDatatype);
		}
	}
}