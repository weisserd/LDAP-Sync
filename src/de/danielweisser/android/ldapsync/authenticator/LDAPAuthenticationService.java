package de.danielweisser.android.ldapsync.authenticator;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Service to handle Account authentication. It instantiates the authenticator and returns its IBinder.
 * 
 * @author <a href="mailto:daniel.weisser@gmx.de">Daniel Weisser</a>
 */
public class LDAPAuthenticationService extends Service {
	private LDAPAuthenticator mAuthenticator;

	@Override
	public void onCreate() {
		mAuthenticator = new LDAPAuthenticator(this);
	}

	@Override
	public void onDestroy() {
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mAuthenticator.getIBinder();
	}
}
