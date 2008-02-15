package org.openrdf.sail.rdbms.schema;

import java.util.Arrays;

import junit.framework.TestCase;

public class IdCodeTest extends TestCase {
	private static final long STEP = IdCode.values()[1].minId();

	public void testStep() throws Exception {
		assertEquals("0", Long.toBinaryString(0l));
		assertEquals(x60('1'), Long.toBinaryString(STEP - 1));
		assertEquals("1" + x60('0'), Long.toBinaryString(STEP));
		assertEquals("10" + x60('0'), Long.toBinaryString(STEP * 2));
	}

	public void testMinMax() throws Exception {
		for (IdCode code : IdCode.values()) {
			assertTrue(code.minId() < code.maxId());
		}
	}

	public void testDecode() throws Exception {
		for (IdCode code : IdCode.values()) {
			assertEquals(code, IdCode.decode(code.minId()));
			assertEquals(code, IdCode.decode(code.minId() + 1));
			assertEquals(code, IdCode.decode(code.maxId()));
			assertEquals(code, IdCode.decode(code.maxId() - 1));
		}
	}

	public void testMin() throws Exception {
		for (IdCode code : IdCode.values()) {
			String min = Long.toBinaryString(code.minId());
			if (code.minId() == 0) {
				assertEquals("0", min);
			} else {
				assertEquals(x60('0'), min.substring(min.length() - 60));
			}
		}
	}

	public void testMax() throws Exception {
		for (IdCode code : IdCode.values()) {
			String max = Long.toBinaryString(code.maxId());
			assertEquals(x60('1'), max.substring(max.length() - 60));
		}
	}

	private String x60(char c) {
		char[] a = new char[60];
		Arrays.fill(a, 0, a.length, c);
		return new String(a);
	}
}
