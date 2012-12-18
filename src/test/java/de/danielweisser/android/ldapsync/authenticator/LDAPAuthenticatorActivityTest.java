package de.danielweisser.android.ldapsync.authenticator;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.xtremelabs.robolectric.RobolectricTestRunner;

import de.danielweisser.android.ldapsync.R;

@RunWith(RobolectricTestRunner.class)
public class LDAPAuthenticatorActivityTest {

	@Test
	public void test() {
		String appName = new LDAPAuthenticatorActivity().getResources().getString(R.string.app_name);
		assertEquals("LDAP Sync", appName);
	}

}
