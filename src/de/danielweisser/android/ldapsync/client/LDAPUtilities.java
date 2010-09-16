package de.danielweisser.android.ldapsync.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.RootDSE;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;

import de.danielweisser.android.ldapsync.authenticator.LDAPAuthenticatorActivity;

/**
 * Provides utility methods for communicating with the server.
 */
public class LDAPUtilities {
	private static final String TAG = "LDAPUtilities";

	/**
	 * Executes the network requests on a separate thread.
	 * 
	 * @param runnable
	 *            The runnable instance containing network operations to be
	 *            executed.
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
	 * Connects to the LDAP server, authenticates the provided username and
	 * password.
	 * 
	 * @param username
	 *            The user's username
	 * @param password
	 *            The user's password
	 * @param handler
	 *            The handler instance from the calling UI thread.
	 * @param context
	 *            The context of the calling Activity.
	 * @return boolean The boolean result indicating whether the user was
	 *         successfully authenticated.
	 */
	public static boolean authenticate(String host, int port, String username, String password, Handler handler,
			final Context context) {
		LDAPConnection connection = new LDAPConnection();
		try {
			connection.connect(host, port);
			BindResult bindResult = connection.bind(username, password);

			ResultCode resultCode = bindResult.getResultCode();
			Log.i(TAG, "Bind result: " + resultCode);

			if (resultCode.isConnectionUsable()) {
				if (Log.isLoggable(TAG, Log.VERBOSE)) {
					Log.v(TAG, "Successful authentication");
				}
				RootDSE s = connection.getRootDSE();
				String[] baseDNs = s.getNamingContextDNs();

				sendResult(baseDNs, true, handler, context);
				return true;
			} else {
				if (Log.isLoggable(TAG, Log.VERBOSE)) {
					Log.v(TAG, "Error authenticating");
				}
				sendResult(null, false, handler, context);
				return false;
			}
		} catch (LDAPException e) {
			if (Log.isLoggable(TAG, Log.VERBOSE)) {
				Log.v(TAG, "LDAPException when getting authtoken", e);
			}
			sendResult(null, false, handler, context);
			return false;
		} finally {
			connection.close();
			if (Log.isLoggable(TAG, Log.VERBOSE)) {
				Log.v(TAG, "getAuthtoken completing");
			}
		}
	}

	/**
	 * Sends the authentication response from server back to the caller main UI
	 * thread through its handler.
	 * 
	 * @param result
	 *            The boolean holding authentication result
	 * @param handler
	 *            The main UI thread's handler instance.
	 * @param context
	 *            The caller Activity's context.
	 */
	private static void sendResult(final String[] baseDNs, final Boolean result, final Handler handler,
			final Context context) {
		if (handler == null || context == null) {
			return;
		}
		handler.post(new Runnable() {
			public void run() {
				((LDAPAuthenticatorActivity) context).onAuthenticationResult(baseDNs, result);
			}
		});
	}

	/**
	 * Attempts to authenticate the user credentials on the server.
	 * 
	 * @param username
	 *            The user's username
	 * @param mPort
	 * @param password
	 *            The user's password to be authenticated
	 * @param mPassword
	 * @param handler
	 *            The main UI thread's handler instance.
	 * @param context
	 *            The caller Activity's context
	 * @return Thread The thread on which the network mOperations are executed.
	 */
	public static Thread attemptAuth(final String host, final int port, final String username, final String password,
			final Handler handler, final Context context) {
		final Runnable runnable = new Runnable() {
			public void run() {
				authenticate(host, port, username, password, handler, context);
			}
		};
		// run on background thread.
		return LDAPUtilities.performOnBackgroundThread(runnable);
	}

	public static List<User> fetchContacts(String host, int port, String username, String authtoken, String baseDN,
			String searchFilter, Bundle mappingBundle, Date mLastUpdated) {
		final ArrayList<User> friendList = new ArrayList<User>();
		LDAPConnection connection = new LDAPConnection();

		ArrayList<String> ldapAttributes = new ArrayList<String>();
		String[] ldapArray = new String[mappingBundle.size()];
		for (String key : mappingBundle.keySet()) {
			ldapAttributes.add(mappingBundle.getString(key));
		}
		ldapArray = ldapAttributes.toArray(ldapArray);
		try {
			connection.connect(host, port);
			connection.bind(username, authtoken);
			SearchResult searchResult = connection.search(baseDN, SearchScope.SUB, searchFilter, ldapArray);
			Log.i(TAG, searchResult.getEntryCount() + " entries returned.");
			for (SearchResultEntry e : searchResult.getSearchEntries()) {
				User u = User.valueOf(e, mappingBundle);
				if (u != null) {
					friendList.add(u);
				}
			}
		} catch (LDAPException e) {
			Log.v(TAG, "LDAPException on fetching contacts", e);
		} finally {
			connection.close();
		}

		return friendList;
	}
}
