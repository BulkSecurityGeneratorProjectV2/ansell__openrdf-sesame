/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.sail.config;

import static org.openrdf.sail.config.SailConfigSchema.DELEGATE;

import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Value;

/**
 * @author Herko ter Horst
 */
public class DelegatingSailImplConfigBase extends SailImplConfigBase implements DelegatingSailImplConfig {

	private SailImplConfig delegate;

	/**
	 * Create a new RepositoryConfigImpl.
	 */
	public DelegatingSailImplConfigBase() {
		super();
	}

	/**
	 * Create a new RepositoryConfigImpl.
	 */
	public DelegatingSailImplConfigBase(String type) {
		super(type);
	}

	/**
	 * Create a new RepositoryConfigImpl.
	 */
	public DelegatingSailImplConfigBase(String type, SailImplConfig delegate) {
		this(type);
		setDelegate(delegate);
	}

	public SailImplConfig getDelegate() {
		return delegate;
	}

	public void setDelegate(SailImplConfig delegate) {
		this.delegate = delegate;
	}

	@Override
	public void validate()
		throws SailConfigException
	{
		super.validate();
		if (delegate == null) {
			throw new SailConfigException("No delegate specified for " + getType() + " Sail");
		}
		delegate.validate();
	}

	@Override
	public Resource export(Model model)
	{
		Resource implNode = super.export(model);

		if (delegate != null) {
			Resource delegateNode = delegate.export(model);
			model.add(implNode, DELEGATE, delegateNode);
		}

		return implNode;
	}

	@Override
	public void parse(Model model, Resource implNode)
		throws SailConfigException
	{
		super.parse(model, implNode);

		try {
			for (Value obj : model.objects(implNode, DELEGATE)) {
				Resource delegateNode = (Resource)obj;
				setDelegate(SailConfigUtil.parseRepositoryImpl(model, delegateNode));
			}
		}
		catch (Exception e) {
			throw new SailConfigException(e.getMessage(), e);
		}
	}
}
