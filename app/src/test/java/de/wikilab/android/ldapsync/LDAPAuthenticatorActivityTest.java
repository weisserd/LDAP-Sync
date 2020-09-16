package de.wikilab.android.ldapsync;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import de.wikilab.android.ldapsync.authenticator.LDAPAuthenticatorActivity;

@RunWith(RobolectricTestRunner.class)
public class LDAPAuthenticatorActivityTest {

	@Test
	public void test() {
		String appName = new LDAPAuthenticatorActivity().getResources().getString(R.string.app_name);
		assertEquals("LDAP Sync", appName);
	}

}
