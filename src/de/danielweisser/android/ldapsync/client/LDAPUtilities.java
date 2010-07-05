package de.danielweisser.android.ldapsync.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.accounts.Account;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;

import de.danielweisser.android.ldapsync.authenticator.LDAPAuthenticatorActivity;

/**
 * Provides utility methods for communicating with the server.
 */
public class LDAPUtilities {
	private static final String LDAP_FILTER = "(|(objectClass=contact)(objectClass=user))";
	private static final String LDAP_BASE_DN = "OU=Organisation,DC=exxeta-de,DC=local";
	private static final String LDAP_PASSWORD = "ads4711";
	private static final String LDAP_USER = "exxeta-de\\ads";
	private static final int LDAP_PORT = 389;
	private static final String LDAP_SERVER = "192.168.0.10";
	
	private static final String TAG = "LDAPUtilities";
	public static final String PARAM_USERNAME = "username";
	public static final String PARAM_PASSWORD = "password";
	public static final String PARAM_UPDATED = "timestamp";

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
	 *            The hander instance from the calling UI thread.
	 * @param context
	 *            The context of the calling Activity.
	 * @return boolean The boolean result indicating whether the user was
	 *         successfully authenticated.
	 */
	public static boolean authenticate(String username, String password, Handler handler, final Context context) {
		try {
			// android.os.Debug.waitForDebugger();
			// Log.i(TAG, "Trying to connect to LDAP server.");
			// sendResult(true, handler, context);
			// return true;
			LDAPConnection connection = new LDAPConnection();
			connection.connect(LDAP_SERVER, LDAP_PORT);
			BindResult bindResult = connection.bind(LDAP_USER, LDAP_PASSWORD);

			ResultCode resultCode = bindResult.getResultCode();
			Log.i(TAG, "Bind result: " + resultCode);

			if (resultCode.isConnectionUsable()) {
				if (Log.isLoggable(TAG, Log.VERBOSE)) {
					Log.v(TAG, "Successful authentication");
				}
				sendResult(true, handler, context);
				return true;
			} else {
				if (Log.isLoggable(TAG, Log.VERBOSE)) {
					Log.v(TAG, "Error authenticating");
				}
				sendResult(false, handler, context);
				return false;
			}
		} catch (LDAPException e) {
			if (Log.isLoggable(TAG, Log.VERBOSE)) {
				Log.v(TAG, "LDAPException when getting authtoken", e);
			}
			sendResult(false, handler, context);
			return false;
		} finally {
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
	private static void sendResult(final Boolean result, final Handler handler, final Context context) {
		if (handler == null || context == null) {
			return;
		}
		handler.post(new Runnable() {
			public void run() {
				((LDAPAuthenticatorActivity) context).onAuthenticationResult(result);
			}
		});
	}

	/**
	 * Attempts to authenticate the user credentials on the server.
	 * 
	 * @param username
	 *            The user's username
	 * @param password
	 *            The user's password to be authenticated
	 * @param handler
	 *            The main UI thread's handler instance.
	 * @param context
	 *            The caller Activity's context
	 * @return Thread The thread on which the network mOperations are executed.
	 */
	public static Thread attemptAuth(final String username, final String password, final Handler handler,
			final Context context) {
		final Runnable runnable = new Runnable() {
			public void run() {
				authenticate(username, password, handler, context);
			}
		};
		// run on background thread.
		return LDAPUtilities.performOnBackgroundThread(runnable);
	}

	public static List<User> fetchContacts(Account account, String authtoken, Date mLastUpdated) {
		android.os.Debug.waitForDebugger();
		final ArrayList<User> friendList = new ArrayList<User>();
		LDAPConnection connection = new LDAPConnection();
		try {
			connection.connect(LDAP_SERVER, LDAP_PORT);
			connection.bind(LDAP_USER, LDAP_PASSWORD);
			SearchResult searchResult = connection.search(LDAP_BASE_DN, SearchScope.SUB,
					LDAP_FILTER);
			Log.w(TAG, searchResult.getEntryCount() + " entries returned.");
			for (SearchResultEntry e : searchResult.getSearchEntries()) {
				friendList.add(User.valueOf(e));
			}
		} catch (LDAPException e) {
			Log.v(TAG, "LDAPException on fetching contacts", e);
		}

		// User u1 = new User("AA Vorname", "AA Nachname", "+49 (162) 2581943",
		// "office D", "vorname@gmx.de", null);
		// friendList.add(u1);
		return friendList;
	}
}
