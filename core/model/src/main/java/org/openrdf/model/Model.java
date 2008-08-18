/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.model;

import java.io.Serializable;
import java.util.Set;

import org.openrdf.OpenRDFUtil;

/**
 * An RDF model, represented as a set of {@link Statement}s.
 * 
 * @author James Leigh
 */
public interface Model extends Set<Statement>, Serializable {

	/**
	 * Determines if statements with the specified subject, predicate, object and
	 * (optionally) context exist in this model. The <tt>subject</tt>,
	 * <tt>predicate</tt> and <tt>object</tt> parameters can be <tt>null</tt>
	 * to indicate wildcards. The <tt>contexts</tt> parameter is a wildcard and
	 * accepts zero or more values. If no contexts are specified, statements will
	 * match disregarding their context. If one or more contexts are specified,
	 * statements with a context matching one of these will match. Note: to match
	 * statements without an associated context, specify the value <tt>null</tt>
	 * and explicitly cast it to type <tt>Resource</tt>.
	 * <p>
	 * Examples: <tt>model.contains(s1, null, null)</tt> is true if any
	 * statements in this model have subject <tt>s1</tt>,<br>
	 * <tt>model.contains(null, null, null, c1)</tt> is true if any statements
	 * in this model have context <tt>c1</tt>,<br>
	 * <tt>model.contains(null, null, null, (Resource)null)</tt> is true if any
	 * statements in this model have no associated context,<br>
	 * <tt>model.contains(null, null, null, c1, c2, c3)</tt> is true if any
	 * statements in this model have context <tt>c1</tt>, <tt>c2</tt> or
	 * <tt>c3</tt>.
	 * 
	 * @param subj
	 *        The subject of the statements to match, <tt>null</tt> to match
	 *        statements with any subject.
	 * @param pred
	 *        The predicate of the statements to match, <tt>null</tt> to match
	 *        statements with any predicate.
	 * @param obj
	 *        The object of the statements to match, <tt>null</tt> to match
	 *        statements with any object.
	 * @param contexts
	 *        The contexts of the statements to match. If no contexts are
	 *        specified, statements will match disregarding their context. If one
	 *        or more contexts are specified, statements with a context matching
	 *        one of these will match.
	 * @return <code>true</code> if statements match the specified pattern.
	 * @throws IllegalArgumentException
	 *         If a <tt>null</tt>-array is specified as the value for
	 *         <tt>contexts</tt>. See
	 *         {@link OpenRDFUtil#verifyContextNotNull(Resource[])} for more
	 *         info.
	 */
	public boolean contains(Resource subj, URI pred, Value obj, Resource... contexts);

	/**
	 * Adds one or more statements to the model. This method creates a statement
	 * for each specified context and adds those to the model. If no contexts are
	 * specified, a single statement with no associated context is added.
	 * 
	 * @param subj
	 *        The statement's subject, must not be <tt>null</tt>.
	 * @param pred
	 *        The statement's predicate, must not be <tt>null</tt>.
	 * @param obj
	 *        The statement's object, must not be <tt>null</tt>.
	 * @param contexts
	 *        The contexts to add statements to.
	 */
	public boolean add(Resource subj, URI pred, Value obj, Resource... contexts);

	/**
	 * Removes statements with the specified context exist in this model.
	 * 
	 * @param context
	 *        The context of the statements to remove.
	 * @return <code>true</code> if one or more statements have been removed.
	 * @throws IllegalArgumentException
	 *         If a <tt>null</tt>-array is specified as the value for
	 *         <tt>contexts</tt>. See
	 *         {@link OpenRDFUtil#verifyContextNotNull(Resource[])} for more
	 *         info.
	 */
	public boolean clear(Resource context);

	/**
	 * Removes statements with the specified subject, predicate, object and
	 * (optionally) context exist in this model. The <tt>subject</tt>,
	 * <tt>predicate</tt> and <tt>object</tt> parameters can be <tt>null</tt>
	 * to indicate wildcards. The <tt>contexts</tt> parameter is a wildcard and
	 * accepts zero or more values. If no contexts are specified, statements will
	 * be removed disregarding their context. If one or more contexts are
	 * specified, statements with a context matching one of these will be
	 * removed. Note: to remove statements without an associated context, specify
	 * the value <tt>null</tt> and explicitly cast it to type <tt>Resource</tt>.
	 * <p>
	 * Examples: <tt>model.remove(s1, null, null)</tt> removes any statements
	 * in this model have subject <tt>s1</tt>,<br>
	 * <tt>model.remove(null, null, null, c1)</tt> removes any statements in
	 * this model have context <tt>c1</tt>,<br>
	 * <tt>model.remove(null, null, null, (Resource)null)</tt> removes any
	 * statements in this model have no associated context,<br>
	 * <tt>model.remove(null, null, null, c1, c2, c3)</tt> removes any
	 * statements in this model have context <tt>c1</tt>, <tt>c2</tt> or
	 * <tt>c3</tt>.
	 * 
	 * @param subj
	 *        The subject of the statements to remove, <tt>null</tt> to remove
	 *        statements with any subject.
	 * @param pred
	 *        The predicate of the statements to remove, <tt>null</tt> to
	 *        remove statements with any predicate.
	 * @param obj
	 *        The object of the statements to remove, <tt>null</tt> to remove
	 *        statements with any object.
	 * @param contexts
	 *        The contexts of the statements to remove. If no contexts are
	 *        specified, statements will be removed disregarding their context.
	 *        If one or more contexts are specified, statements with a context
	 *        matching one of these will be removed.
	 * @return <code>true</code> if one or more statements have been removed.
	 * @throws IllegalArgumentException
	 *         If a <tt>null</tt>-array is specified as the value for
	 *         <tt>contexts</tt>. See
	 *         {@link OpenRDFUtil#verifyContextNotNull(Resource[])} for more
	 *         info.
	 */
	public boolean remove(Resource subj, URI pred, Value obj, Resource... contexts);

	// Views

	/**
	 * Returns a {@link Set} view of the statements with the specified subject,
	 * predicate, object and (optionally) context. The <tt>subject</tt>,
	 * <tt>predicate</tt> and <tt>object</tt> parameters can be <tt>null</tt>
	 * to indicate wildcards. The <tt>contexts</tt> parameter is a wildcard and
	 * accepts zero or more values. If no contexts are specified, statements will
	 * match disregarding their context. If one or more contexts are specified,
	 * statements with a context matching one of these will match. Note: to match
	 * statements without an associated context, specify the value <tt>null</tt>
	 * and explicitly cast it to type <tt>Resource</tt>.
	 * <p>
	 * The set is backed by the model, so changes to the model are reflected in
	 * the set, and vice-versa. If the model is modified while an iteration over
	 * the set is in progress (except through the iterator's own <tt>remove</tt>
	 * operation), the results of the iteration are undefined. The set supports
	 * element removal, which removes the corresponding statement from the model,
	 * via the <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
	 * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt>
	 * operations. The statements passed to the <tt>add</tt> and
	 * <tt>addAll</tt> operations must match the parameter pattern. Gets the
	 * <p>
	 * Examples: <tt>model.filter(s1, null, null)</tt> matches all statements
	 * that have subject <tt>s1</tt>,<br>
	 * <tt>model.filter(null, null, null, c1)</tt> matches all statements that
	 * have context <tt>c1</tt>,<br>
	 * <tt>model.filter(null, null, null, (Resource)null)</tt> matches all
	 * statements that have no associated context,<br>
	 * <tt>model.filter(null, null, null, c1, c2, c3)</tt> matches all
	 * statements that have context <tt>c1</tt>, <tt>c2</tt> or <tt>c3</tt>.
	 * 
	 * @param subj
	 *        The subject of the statements to match, <tt>null</tt> to match
	 *        statements with any subject.
	 * @param pred
	 *        The predicate of the statements to match, <tt>null</tt> to match
	 *        statements with any predicate.
	 * @param obj
	 *        The object of the statements to match, <tt>null</tt> to match
	 *        statements with any object.
	 * @param contexts
	 *        The contexts of the statements to match. If no contexts are
	 *        specified, statements will match disregarding their context. If one
	 *        or more contexts are specified, statements with a context matching
	 *        one of these will match.
	 * @return The statements that match the specified pattern.
	 * @throws IllegalArgumentException
	 *         If a <tt>null</tt>-array is specified as the value for
	 *         <tt>contexts</tt>. See
	 *         {@link OpenRDFUtil#verifyContextNotNull(Resource[])} for more
	 *         info.
	 */
	public Set<Statement> filter(Resource subj, URI pred, Value obj, Resource... contexts);

	/**
	 * Returns a {@link Set} view of the subjects contained in this model. The
	 * set is backed by the model, so changes to the model are reflected in the
	 * set, and vice-versa. If the model is modified while an iteration over the
	 * set is in progress (except through the iterator's own <tt>remove</tt>
	 * operation), the results of the iteration are undefined. The set supports
	 * element removal, which removes the corresponding statement from the model,
	 * via the <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
	 * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt>
	 * operations. It does not support the <tt>add</tt> or <tt>addAll</tt>
	 * operations if the parameters <tt>pred</tt> or <tt>obj</tt> are null.
	 * 
	 * @return a set view of the subjects contained in this model
	 */
	public Set<Resource> subjects(URI pred, Value obj, Resource... contexts);

	/**
	 * Returns a {@link Set} view of the predicates contained in this model. The
	 * set is backed by the model, so changes to the model are reflected in the
	 * set, and vice-versa. If the model is modified while an iteration over the
	 * set is in progress (except through the iterator's own <tt>remove</tt>
	 * operation), the results of the iteration are undefined. The set supports
	 * element removal, which removes the corresponding statement from the model,
	 * via the <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
	 * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt>
	 * operations. It does not support the <tt>add</tt> or <tt>addAll</tt>
	 * operations if the parameters <tt>subj</tt> or <tt>obj</tt> are null.
	 * 
	 * @return a set view of the predicates contained in this model
	 */
	public Set<URI> predicates(Resource subj, Value obj, Resource... contexts);

	/**
	 * Returns a {@link Set} view of the objects contained in this model. The
	 * set is backed by the model, so changes to the model are reflected in the
	 * set, and vice-versa. If the model is modified while an iteration over the
	 * set is in progress (except through the iterator's own <tt>remove</tt>
	 * operation), the results of the iteration are undefined. The set supports
	 * element removal, which removes the corresponding statement from the model,
	 * via the <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
	 * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt>
	 * operations. It does not support the <tt>add</tt> or <tt>addAll</tt>
	 * operations if the parameters <tt>subj</tt> or <tt>pred</tt> are null.
	 * 
	 * @return a set view of the objects contained in this model
	 */
	public Set<Value> objects(Resource subj, URI pred, Resource... contexts);

	/**
	 * Returns a {@link Set} view of the contexts contained in this model. The
	 * set is backed by the model, so changes to the model are reflected in the
	 * set, and vice-versa. If the model is modified while an iteration over the
	 * set is in progress (except through the iterator's own <tt>remove</tt>
	 * operation), the results of the iteration are undefined. The set supports
	 * element removal, which removes the corresponding statement from the model,
	 * via the <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
	 * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt>
	 * operations. It does not support the <tt>add</tt> or <tt>addAll</tt>
	 * operations if the parameters <tt>subj</tt>, <tt>pred</tt> or <tt>obj</tt> are null.
	 * 
	 * @return a set view of the contexts contained in this model
	 */
	public Set<Resource> contexts(Resource subj, URI pred, Value obj);
}
