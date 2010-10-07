package de.danielweisser.android.ldapsync.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.net.SocketFactory;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.ExtendedResult;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.RootDSE;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.extensions.StartTLSExtendedRequest;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;

import de.danielweisser.android.ldapsync.authenticator.LDAPAuthenticatorActivity;

/**
 * Provides utility methods for communicating with the server.
 * 
 * TODO Enable secure connect 
 * 
 * TODO Cleanup code! => getConnection! => ServerInstance.getConnection
 * 
 * TODO Encapsulate LDAP ServerInstance
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
	 * Connects to the LDAP server, authenticates the provided username and password.
	 * 
	 * @param host
	 *            The hostname of the LDAP server
	 * @param port
	 *            The port number of the LDAP server
	 * @param encryption
	 *            The encryption method (0 - no encryption, 1 - SSL, 2 - StartTLS)
	 * @param username
	 *            The user's username
	 * @param password
	 *            The user's password
	 * @param handler
	 *            The handler instance from the calling UI thread.
	 * @param context
	 *            The context of the calling Activity.
	 * 
	 * @return boolean The boolean result indicating whether the user was successfully authenticated.
	 */
	public static boolean authenticate(String host, int port, int encryption, String username, String password, Handler handler, final Context context) {
		SocketFactory socketFactory = null;
		if (encryption == 1) {
			final SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
			try {
				socketFactory = sslUtil.createSSLSocketFactory();
			} catch (Exception e) {
				Log.v(TAG, "Exception on secure connect", e);
			}
		}
		LDAPConnection connection = null;
		final LDAPConnectionOptions options = new LDAPConnectionOptions();
		options.setAutoReconnect(true);
		options.setConnectTimeoutMillis(30000);
		options.setFollowReferrals(false);
		options.setMaxMessageSize(1024 * 1024);

		try {
			connection = new LDAPConnection(socketFactory, options, host, port);
			
			if (encryption == 2) {
				final SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
				try {
					final ExtendedResult r = connection.processExtendedOperation(new StartTLSExtendedRequest(sslUtil.createSSLContext()));
					if (r.getResultCode() != ResultCode.SUCCESS) {
						throw new LDAPException(r);
					}
				} catch (LDAPException le) {
					Log.e(TAG, "getConnection", le);
					connection.close();
				} catch (Exception e) {
					Log.e(TAG, "getConnection", e);
					connection.close();
				}
			}

			BindResult bindResult = connection.bind(username, password);

			ResultCode resultCode = bindResult.getResultCode();
			Log.i(TAG, "Bind result: " + resultCode);

			if (resultCode.isConnectionUsable()) {
				Log.v(TAG, "Successful authentication");
				RootDSE s = connection.getRootDSE();
				String[] baseDNs = s.getNamingContextDNs();

				sendResult(baseDNs, true, handler, context);
				return true;
			} else {
				Log.v(TAG, "Error authenticating");
				sendResult(null, false, handler, context);
				return false;
			}
		} catch (LDAPException e) {
			Log.v(TAG, "LDAPException when getting authtoken", e);
			sendResult(null, false, handler, context);
			return false;
		} finally {
			if (connection != null) {
				connection.close();
			}
		}
	}

	/**
	 * Sends the authentication response from server back to the caller main UI thread through its handler.
	 * 
	 * @param result
	 *            The boolean holding authentication result
	 * @param handler
	 *            The main UI thread's handler instance.
	 * @param context
	 *            The caller Activity's context.
	 */
	private static void sendResult(final String[] baseDNs, final Boolean result, final Handler handler, final Context context) {
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
	public static Thread attemptAuth(final String host, final int port, final String username, final String password, final Handler handler,
			final Context context) {
		final Runnable runnable = new Runnable() {
			public void run() {
				authenticate(host, port, 0, username, password, handler, context);
			}
		};
		// run on background thread.
		return LDAPUtilities.performOnBackgroundThread(runnable);
	}

	public static List<Contact> fetchContacts(String host, int port, String username, String authtoken, String baseDN, String searchFilter,
			Bundle mappingBundle, Date mLastUpdated) {
		final ArrayList<Contact> friendList = new ArrayList<Contact>();
		LDAPConnection connection = null;
		try {
			connection = new LDAPConnection(host, port);
			// connection.connect(host, port);
			connection.bind(username, authtoken);
			SearchResult searchResult = connection.search(baseDN, SearchScope.SUB, searchFilter, getUsedAttributes(mappingBundle));
			Log.i(TAG, searchResult.getEntryCount() + " entries returned.");
			for (SearchResultEntry e : searchResult.getSearchEntries()) {
				Contact u = Contact.valueOf(e, mappingBundle);
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

	private static String[] getUsedAttributes(Bundle mappingBundle) {
		ArrayList<String> ldapAttributes = new ArrayList<String>();
		String[] ldapArray = new String[mappingBundle.size()];
		for (String key : mappingBundle.keySet()) {
			ldapAttributes.add(mappingBundle.getString(key));
		}
		ldapArray = ldapAttributes.toArray(ldapArray);
		return ldapArray;
	}
}
