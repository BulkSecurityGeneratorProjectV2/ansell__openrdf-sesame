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
package org.openrdf.model.impl;

import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import org.openrdf.model.Model;
import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.IRI;
import org.openrdf.model.Value;

/**
 * A Model wrapper that prevents modification to the underlying model.
 */
class UnmodifiableModel extends AbstractModel {

	private static final long serialVersionUID = 6335569454318096059L;

	private final Model model;

	public UnmodifiableModel(Model delegate) {
		this.model = delegate;
	}

	@Override
	public Set<Namespace> getNamespaces() {
		return Collections.unmodifiableSet(model.getNamespaces());
	}

	@Override
	public Optional<Namespace> getNamespace(String prefix) {
		return model.getNamespace(prefix);
	}

	@Override
	public Namespace setNamespace(String prefix, String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setNamespace(Namespace name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<Namespace> removeNamespace(String prefix) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean contains(Resource subj, IRI pred, Value obj, Resource... contexts) {
		return model.contains(subj, pred, obj, contexts);
	}

	@Override
	public boolean add(Resource subj, IRI pred, Value obj, Resource... contexts) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Resource subj, IRI pred, Value obj, Resource... contexts) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Model filter(Resource subj, IRI pred, Value obj, Resource... contexts) {
		return model.filter(subj, pred, obj, contexts).unmodifiable();
	}

	@Override
	public Iterator<Statement> iterator() {
		return Collections.unmodifiableSet(model).iterator();
	}

	@Override
	public int size() {
		return model.size();
	}

	@Override
	public void removeTermIteration(Iterator<Statement> iter, Resource subj, IRI pred, Value obj,
			Resource... contexts)
	{
		throw new UnsupportedOperationException();
	}

}
