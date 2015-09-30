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
package org.openrdf.sail.memory.model;

import java.io.IOException;

import javax.xml.datatype.XMLGregorianCalendar;

import org.openrdf.model.IRI;
import org.openrdf.model.datatypes.XMLDatatypeUtil;

/**
 * An extension of MemLiteral that stores a Calendar value to avoid parsing.
 * 
 * @author David Huynh
 * @author Arjohn Kampman
 */
public class CalendarMemLiteral extends MemLiteral {

	private static final long serialVersionUID = -7903843639313451580L;

	/*-----------*
	 * Variables *
	 *-----------*/

	transient private XMLGregorianCalendar calendar;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public CalendarMemLiteral(Object creator, XMLGregorianCalendar calendar) {
		this(creator, calendar.toXMLFormat(), calendar);
	}

	public CalendarMemLiteral(Object creator, String label, XMLGregorianCalendar calendar) {
		this(creator, label, XMLDatatypeUtil.qnameToURI(calendar.getXMLSchemaType()), calendar);
	}

	public CalendarMemLiteral(Object creator, String label, IRI datatype, XMLGregorianCalendar calendar) {
		super(creator, label, datatype);
		this.calendar = calendar;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public XMLGregorianCalendar calendarValue() {
		return calendar;
	}

	private void readObject(java.io.ObjectInputStream in)
		throws IOException
	{
		try {
			in.defaultReadObject();
			calendar = XMLDatatypeUtil.parseCalendar(this.getLabel());
		}
		catch (ClassNotFoundException e) {
			throw new IOException(e.getMessage());
		}
	}
}
