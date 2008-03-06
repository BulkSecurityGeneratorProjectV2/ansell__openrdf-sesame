/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 1997-2006.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.querylanguage.serql.ast;

/*
 * All AST nodes must implement this interface. It provides basic machinery for
 * constructing the parent and child relationships between nodes.
 */

public interface Node {

	/**
	 * This method is called after the node has been made the current node. It
	 * indicates that child nodes can now be added to it.
	 */
	public void jjtOpen();

	/**
	 * This method is called after all the child nodes have been added.
	 */
	public void jjtClose();

	/**
	 * This pair of methods are used to inform the node of its parent.
	 */
	public void jjtSetParent(Node n);

	public Node jjtGetParent();

	/**
	 * This method tells the node to add its argument to the node's list of
	 * children.
	 */
	public void jjtAddChild(Node n, int i);

	/**
	 * Adds the supplied node as the last child node to this node.
	 */
	public void jjtAppendChild(Node n);

	/**
	 * Adds the supplied node as the <tt>i</tt>'th child node to this node.
	 */
	public void jjtInsertChild(Node n, int i);

	/**
	 * Replaces a child node with a new node.
	 */
	public void jjtReplaceChild(Node oldNode, Node newNode);

	/**
	 * This method returns a child node. The children are numbered from zero,
	 * left to right.
	 */
	public Node jjtGetChild(int i);

	/** Return the number of children the node has. */
	public int jjtGetNumChildren();

	/**
	 * Accept the visitor.
	 */
	public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data)
		throws VisitorException;
}
