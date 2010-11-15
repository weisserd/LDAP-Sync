package de.danielweisser.android.ldapsync.syncadapter;

import com.nullwire.trace.ExceptionHandler;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Service to handle Account sync. This is invoked with an intent with action ACTION_AUTHENTICATOR_INTENT. It instantiates the sync adapter and returns its
 * IBinder.
 * 
 * @author <a href="mailto:daniel.weisser@gmx.de">Daniel Weisser</a>
 */
public class SyncService extends Service {
	private static final String TAG = "LDAPSyncService";

	private static final Object sSyncAdapterLock = new Object();
	private static SyncAdapter sSyncAdapter = null;

	@Override
	public void onCreate() {
		Log.v(TAG, "onCreate");
		synchronized (sSyncAdapterLock) {
			ExceptionHandler.register(this, "http://www.danielweisser.de/android/server.php");
			if (sSyncAdapter == null) {
				sSyncAdapter = new SyncAdapter(getApplicationContext(), true);
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.v(TAG, "onBind");
		return sSyncAdapter.getSyncAdapterBinder();
	}
}
