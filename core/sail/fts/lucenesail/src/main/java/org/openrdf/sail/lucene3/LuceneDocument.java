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
package org.openrdf.sail.lucene3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.spatial.tier.projections.CartesianTierPlotter;
import org.apache.lucene.spatial.tier.projections.IProjector;
import org.apache.lucene.util.NumericUtils;
import org.openrdf.sail.lucene.LuceneSail;
import org.openrdf.sail.lucene.SearchDocument;
import org.openrdf.sail.lucene.SearchFields;

import com.spatial4j.core.shape.Point;
import com.spatial4j.core.shape.Shape;

/**
 *
 * @author MJAHale
 */
public class LuceneDocument implements SearchDocument
{
	private static final IProjector projector = new FixedSinusoidalProjector();
	private static final CartesianTierPlotter[] plotters;
	private static final double maxMiles = 25.0;
	private static final double minMiles = 1.0;

	static {
		String fieldPrefix = CartesianTierPlotter.DEFALT_FIELD_PREFIX;
		CartesianTierPlotter ctp = new CartesianTierPlotter(0, projector, fieldPrefix);
		int startTier = ctp.bestFit(maxMiles);
		int endTier = ctp.bestFit(minMiles);
		plotters = new CartesianTierPlotter[endTier-startTier+1];
		for(int tier = startTier; tier <= endTier; tier++)
		{
			plotters[tier-startTier] = new CartesianTierPlotter(tier, projector, fieldPrefix);
		}
	}

	private final Document doc;

	public LuceneDocument()
	{
		this(new Document());
	}

	public LuceneDocument(Document doc)
	{
		this.doc = doc;
	}

	public LuceneDocument(String id, String resourceId, String context)
	{
		this();
		setId(id);
		setResource(resourceId);
		setContext(context);
	}

	private void setId(String id) {
		LuceneIndex.addIDField(id, doc);
	}

	private void setContext(String context) {
		LuceneIndex.addContextField(context, doc);
	}

	private void setResource(String resourceId) {
		LuceneIndex.addResourceField(resourceId, doc);
	}

	public Document getDocument() {
		return doc;
	}

	@Override
	public String getId() {
		return doc.get(SearchFields.ID_FIELD_NAME);
	}

	@Override
	public String getResource() {
		return doc.get(SearchFields.URI_FIELD_NAME);
	}

	@Override
	public String getContext() {
		return doc.get(SearchFields.CONTEXT_FIELD_NAME);
	}

	@Override
	public void addProperty(String name) {
		// don't need to do anything
	}

	@Override
	public Collection<String> getPropertyNames() {
		List<Fieldable> fields = doc.getFields();
		List<String> names = new ArrayList<String>(fields.size());
		for(Fieldable field : fields) {
			String name = field.name();
			if (SearchFields.isPropertyField(name))
				names.add(name);
		}
		return names;
	}


	/**
	 * Stores and indexes a property in a Document. We don't have to recalculate
	 * the concatenated text: just add another TEXT field and Lucene will take
	 * care of this. Additional advantage: Lucene may be able to handle the
	 * invididual strings in a way that may affect e.g. phrase and proximity
	 * searches (concatenation basically means loss of information). NOTE: The
	 * TEXT_FIELD_NAME has to be stored, see in LuceneSail
	 * 
	 * @see LuceneSail
	 */
	@Override
	public void addProperty(String name, String text) {
		LuceneIndex.addPredicateField(name, text, doc);
		LuceneIndex.addTextField(text, doc);
	}

	/**
	 * Checks whether a field occurs with a specified value in a Document.
	 */
	@Override
	public boolean hasProperty(String fieldName, String value) {
		String[] fields = doc.getValues(fieldName);
		if (fields != null) {
			for (String field : fields) {
				if (value.equals(field)) {
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public List<String> getProperty(String name) {
		return Arrays.asList(doc.getValues(name));
	}

	@Override
	public void addShape(String field, Shape shape) {
		if(shape instanceof Point)
		{
			Point p = (Point) shape;
			for(CartesianTierPlotter ctp : plotters) {
				double boxId = ctp.getTierBoxId(p.getY(), p.getX());
				doc.add(new Field(ctp.getTierFieldName(), NumericUtils.doubleToPrefixCoded(boxId), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
			}
		}
	}
}
