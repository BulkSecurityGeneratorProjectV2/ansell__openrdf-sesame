/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2008.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.sail.rdbms.schema;

import java.sql.SQLException;

/**
 * A Facade to the five literal value tables. Which are labels, languages,
 * datatypes, numeric values, and dateTime values.
 * 
 * @author James Leigh
 * 
 */
public class LiteralTable {
	public static final boolean ONLY_INSERT_LABEL = false;

	private ValueTable labels;
	private ValueTable longLabels;
	private ValueTable languages;
	private ValueTable datatypes;
	private ValueTable numeric;
	private ValueTable dateTime;
	private HashTable hashTable;
	private int version;

	public ValueTable getLabelTable() {
		return labels;
	}

	public void setLabelTable(ValueTable labels) {
		this.labels = labels;
	}

	public ValueTable getLongLabelTable() {
		return longLabels;
	}

	public void setLongLabelTable(ValueTable longLabels) {
		this.longLabels = longLabels;
	}

	public ValueTable getLanguageTable() {
		return languages;
	}

	public void setLanguageTable(ValueTable languages) {
		this.languages = languages;
	}

	public ValueTable getDatatypeTable() {
		return datatypes;
	}

	public void setDatatypeTable(ValueTable datatypes) {
		this.datatypes = datatypes;
	}

	public ValueTable getNumericTable() {
		return numeric;
	}

	public void setNumericTable(ValueTable numeric) {
		this.numeric = numeric;
	}

	public ValueTable getDateTimeTable() {
		return dateTime;
	}

	public void setDateTimeTable(ValueTable dateTime) {
		this.dateTime = dateTime;
	}

	public void setHashTable(HashTable hash) {
		this.hashTable = hash;
	}

	public void close() throws SQLException {
		labels.close();
		longLabels.close();
		languages.close();
		datatypes.close();
		numeric.close();
		dateTime.close();
	}

	public int getBatchSize() {
		return labels.getBatchSize();
	}

	public int getIdVersion() {
		return version;
	}

	public void insertSimple(long id, long hash, String label) throws SQLException, InterruptedException {
		hashTable.insert(id, hash);
		if (IdCode.valueOf(id).isLong()) {
			longLabels.insert(id, label);
		} else {
			labels.insert(id, label);
		}
	}

	public void insertLanguage(long id, long hash, String label, String language)
			throws SQLException, InterruptedException {
		insertSimple(id, hash, label);
		languages.insert(id, language);
	}

	public void insertDatatype(long id, long hash, String label, String datatype)
			throws SQLException, InterruptedException {
		insertSimple(id, hash, label);
		datatypes.insert(id, datatype);
	}

	public void insertNumeric(long id, long hash, String label, String datatype,
			double value) throws SQLException, InterruptedException {
		hashTable.insert(id, hash);
		labels.insert(id, label);
		datatypes.insert(id, datatype);
		numeric.insert(id, value);
	}

	public void insertDateTime(long id, long hash, String label, String datatype,
			long value) throws SQLException, InterruptedException {
		hashTable.insert(id, hash);
		labels.insert(id, label);
		datatypes.insert(id, datatype);
		dateTime.insert(id, value);
	}

	public void optimize() throws SQLException {
		hashTable.optimize();
		labels.optimize();
		longLabels.optimize();
		languages.optimize();
		datatypes.optimize();
		numeric.optimize();
		dateTime.optimize();
	}

	public void removedStatements(int count, String condition)
			throws SQLException {
		hashTable.expungeRemovedStatements(count, condition);
		boolean bool = false;
		bool |= labels.expungeRemovedStatements(count, condition);
		bool |= longLabels.expungeRemovedStatements(count, condition);
		bool |= languages.expungeRemovedStatements(count, condition);
		bool |= datatypes.expungeRemovedStatements(count, condition);
		bool |= numeric.expungeRemovedStatements(count, condition);
		bool |= dateTime.expungeRemovedStatements(count, condition);
		if (bool) {
			version++;
		}
	}
}
