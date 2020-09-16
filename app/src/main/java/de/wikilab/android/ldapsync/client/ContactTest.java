package de.wikilab.android.ldapsync.client;

import static org.junit.Assert.*;

import org.junit.Test;

public class ContactTest {
	
	private Contact c = new Contact();

	@Test
	public void testDn() {
		c.setDn("testDn");
		assertEquals("testDn", c.getDn());
	}

	@Test
	public void testGetFirstName() {
		c.setFirstName("testFirstName");
		assertEquals("testFirstName", c.getFirstName());
	}
}
