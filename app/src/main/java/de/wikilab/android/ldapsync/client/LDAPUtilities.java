/*
 * Copyright 2010 Daniel Weisser
 * Copyright 2018 Mira Weller
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.wikilab.android.ldapsync.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.RootDSE;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;

import de.wikilab.android.ldapsync.R;
import de.wikilab.android.ldapsync.activity.SyncErrorActivity;
import de.wikilab.android.ldapsync.authenticator.LDAPAuthenticatorActivity;

/**
 * Provides utility methods for communicating with the LDAP server.
 * 
 * @author <a href="mailto:daniel.weisser@gmx.de">Daniel Weisser</a>
 */
public class LDAPUtilities {
	
	private static final String TAG = "LDAPUtilities";

	/**
	 * Executes the network requests on a separate thread.
	 * 
	 * @param runnable
	 *            The runnable instance containing network operations to be executed.
	 */
	public static Thread performOnBackgroundThread(final Runnable runnable) {
		final Thread t = new Thread() {
			@Override
			public void run() {
				try {
					runnable.run();
				} finally {
				}
			}
		};
		t.start();
		return t;
	}

	/**
	 * Sends the authentication response from server back to the caller main UI thread through its handler.
	 * 
	 * @param baseDNs
	 *            An array containing the baseDNs of the LDAP server
	 * @param result
	 *            The boolean holding authentication result
	 * @param handler
	 *            The main UI thread's handler instance.
	 * @param context
	 *            The caller Activity's context.
	 * @param message
	 *            A message if applicable
	 */
	private static void sendResult(final String[] baseDNs, final Boolean result, final Handler handler, final Context context, final String message) {
		if (handler == null || context == null) {
			return;
		}
		handler.post(new Runnable() {
			public void run() {
				((LDAPAuthenticatorActivity) context).onAuthenticationResult(baseDNs, result, message);
			}
		});
	}

	/**
	 * Obtains a list of all contacts from the LDAP Server.
	 * 
	 * @param ldapServer
	 *            The LDAP server data
	 * @param baseDN
	 *            The baseDN that will be used for the search
	 * @param searchFilter
	 *            The search filter
	 * @param preferences
	 *            A bundle of all LDAP attributes that are queried
	 * @param mLastUpdated
	 *            Date of the last update
	 * @param context
	 *            The caller Activity's context
	 * @return List of all LDAP contacts
	 */
	public static List<Contact> fetchContacts(final LDAPServerInstance ldapServer, final String baseDN, final String searchFilter, final SharedPreferences preferences,
			final Date mLastUpdated, final Context context) {
		final ArrayList<Contact> friendList = new ArrayList<Contact>();
		LDAPConnection connection = null;
		try {
			connection = ldapServer.getConnection();
			SearchResult searchResult = connection.search(baseDN, SearchScope.SUB, searchFilter, getUsedAttributes(preferences));
			Log.i(TAG, searchResult.getEntryCount() + " entries returned.");

			for (SearchResultEntry e : searchResult.getSearchEntries()) {
				Contact u = Contact.valueOf(e, preferences);
				if (u != null) {
					friendList.add(u);
				}
			}
		} catch (LDAPException e) {
			Log.v(TAG, "LDAPException on fetching contacts", e);
			showErrorNotification(context, e);
			return null;
		} finally {
			if (connection != null) {
				connection.close();
			}
		}

		return friendList;
	}

	private static void showErrorNotification(Context context, Throwable e) {
		NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		CharSequence tickerText = "Error on LDAP Sync";

		Intent notificationIntent = new Intent(context, SyncErrorActivity.class);
		notificationIntent.putExtra("throwable", e);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

		Notification noti = new Notification.Builder(context)
				.setSmallIcon(R.drawable.icon)
				.setContentTitle(tickerText)
				.setContentText(e.getMessage().replace("\\n", " "))
				.setContentIntent(contentIntent)
				.setAutoCancel(true)
				.getNotification();
		mNotificationManager.notify(0, noti);
	}

	private static String[] getUsedAttributes(SharedPreferences preferences) {
		ArrayList<String> ldapAttributes = new ArrayList<String>();
		String[] ldapArray = new String[preferences.getAll().size()];
		for (String key : preferences.getAll().keySet()) {
			if (!(key.equals("baseDN") || key.equals("searchFilter")))
			ldapAttributes.add(preferences.getString(key, ""));
		}
		ldapArray = ldapAttributes.toArray(ldapArray);
		return ldapArray;
	}

	/**
	 * Attempts to authenticate the user credentials on the server.
	 * 
	 * @param ldapServer
	 *            The LDAP server data
	 * @param handler
	 *            The main UI thread's handler instance.
	 * @param context
	 *            The caller Activity's context
	 * @return Thread The thread on which the network mOperations are executed.
	 */
	public static Thread attemptAuth(final LDAPServerInstance ldapServer, final Handler handler, final Context context) {
		final Runnable runnable = new Runnable() {
			public void run() {
				authenticate(ldapServer, handler, context);
			}
		};
		// run on background thread.
		return LDAPUtilities.performOnBackgroundThread(runnable);
	}

	/**
	 * Tries to authenticate against the LDAP server and
	 * 
	 * @param ldapServer
	 *            The LDAP server data
	 * @param handler
	 *            The handler instance from the calling UI thread.
	 * @param context
	 *            The context of the calling Activity.
	 * @return {code false} if the authentication fails, {code true} otherwise
	 */
	public static boolean authenticate(LDAPServerInstance ldapServer, Handler handler, final Context context) {
		LDAPConnection connection = null;
		try {
			connection = ldapServer.getConnection();
			if (connection != null) {
				Log.d(TAG, "authenticate: connection est. " + connection.isConnected());
				Log.d(TAG, "authenticate: connection name " + connection.getConnectionName());
				Log.d(TAG, "authenticate: connection address " + connection.getConnectedAddress());

				RootDSE s = connection.getRootDSE();
//				TODO Check vendor name : s.getVendorName()
				String[] baseDNs = null;
				if (s != null) {
					baseDNs = s.getNamingContextDNs();
					if (baseDNs != null)
						Log.d(TAG, "authenticate: base DNs: "+ TextUtils.join("; ", baseDNs));
				}

				sendResult(baseDNs, true, handler, context, null);
				return true;
			}
		} catch (LDAPException e) {
			Log.e(TAG, "Error authenticating", e);
			sendResult(null, false, handler, context, e.getMessage());
			return false;
		} finally {
			if (connection != null) {
				connection.close();
			}
		}
		return false;
	}
}
