/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.sail.nativerdf.config;

import static org.openrdf.sail.nativerdf.config.NativeStoreSchema.FORCE_SYNC;
import static org.openrdf.sail.nativerdf.config.NativeStoreSchema.TRIPLE_INDEXES;

import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.util.ModelUtil;
import org.openrdf.model.util.ModelUtilException;
import org.openrdf.sail.config.SailConfigException;
import org.openrdf.sail.config.SailImplConfigBase;

/**
 * @author Arjohn Kampman
 */
public class NativeStoreConfig extends SailImplConfigBase {

	/*-----------*
	 * Variables *
	 *-----------*/

	private String tripleIndexes;

	private boolean forceSync = false;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public NativeStoreConfig() {
		super(NativeStoreFactory.SAIL_TYPE);
	}

	public NativeStoreConfig(String tripleIndexes) {
		this();
		setTripleIndexes(tripleIndexes);
	}

	public NativeStoreConfig(String tripleIndexes, boolean forceSync) {
		this(tripleIndexes);
		setForceSync(forceSync);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public String getTripleIndexes() {
		return tripleIndexes;
	}

	public void setTripleIndexes(String tripleIndexes) {
		this.tripleIndexes = tripleIndexes;
	}

	public boolean getForceSync() {
		return forceSync;
	}

	public void setForceSync(boolean forceSync) {
		this.forceSync = forceSync;
	}

	@Override
	public Resource export(Model model) {
		Resource implNode = super.export(model);

		ValueFactoryImpl vf = ValueFactoryImpl.getInstance();

		if (tripleIndexes != null) {
			model.add(implNode, TRIPLE_INDEXES, vf.createLiteral(tripleIndexes));
		}
		if (forceSync) {
			model.add(implNode, FORCE_SYNC, vf.createLiteral(forceSync));
		}

		return implNode;
	}

	@Override
	public void parse(Model model, Resource implNode)
		throws SailConfigException
	{
		super.parse(model, implNode);

		try {
			Literal tripleIndexLit = ModelUtil.getOptionalObjectLiteral(model, implNode, TRIPLE_INDEXES);
			if (tripleIndexLit != null) {
				setTripleIndexes((tripleIndexLit).getLabel());
			}

			Literal forceSyncLit = ModelUtil.getOptionalObjectLiteral(model, implNode, FORCE_SYNC);
			if (forceSyncLit != null) {
				try {
					setForceSync(forceSyncLit.booleanValue());
				}
				catch (IllegalArgumentException e) {
					throw new SailConfigException("Boolean value required for " + FORCE_SYNC + " property, found "
							+ forceSyncLit);
				}
			}
		}
		catch (ModelUtilException e) {
			throw new SailConfigException(e.getMessage(), e);
		}
	}
}
