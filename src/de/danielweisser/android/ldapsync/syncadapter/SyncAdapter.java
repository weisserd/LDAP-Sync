package de.danielweisser.android.ldapsync.syncadapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;
import de.danielweisser.android.ldapsync.Constants;
import de.danielweisser.android.ldapsync.authenticator.LDAPAuthenticatorActivity;
import de.danielweisser.android.ldapsync.client.LDAPUtilities;
import de.danielweisser.android.ldapsync.client.User;
import de.danielweisser.android.ldapsync.platform.ContactManager;

/**
 * SyncAdapter implementation for synchronizing LDAP contacts to the platform
 * ContactOperations provider.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {
	private static final String TAG = "LDAPSyncAdapter";

	private final AccountManager mAccountManager;
	private final Context mContext;

	private Date mLastUpdated;

	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		mContext = context;
		mAccountManager = AccountManager.get(context);
	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider,
			SyncResult syncResult) {
		Log.d(TAG, "Start the sync.");
		List<User> users = new ArrayList<User>();
		String authtoken = null;
		try {
			// use the account manager to request the credentials
			authtoken = mAccountManager
					.blockingGetAuthToken(account, Constants.AUTHTOKEN_TYPE, true /* notifyAuthFailure */);
			final String host = mAccountManager.getUserData(account, LDAPAuthenticatorActivity.PARAM_HOST);
			final String username = mAccountManager.getUserData(account, LDAPAuthenticatorActivity.PARAM_USERNAME);
			final int port = Integer.parseInt(mAccountManager.getUserData(account, LDAPAuthenticatorActivity.PARAM_PORT));
			final String searchFilter = mAccountManager.getUserData(account, LDAPAuthenticatorActivity.PARAM_SEARCHFILTER);
			final String baseDN = mAccountManager.getUserData(account, LDAPAuthenticatorActivity.PARAM_BASEDN);
			
			// LDAP name mappings
			final Bundle mappingBundle = new Bundle();
			mappingBundle.putString(User.FIRSTNAME, mAccountManager.getUserData(account, LDAPAuthenticatorActivity.PARAM_MAPPING + User.FIRSTNAME));
			mappingBundle.putString(User.LASTNAME, mAccountManager.getUserData(account, LDAPAuthenticatorActivity.PARAM_MAPPING + User.LASTNAME));
			mappingBundle.putString(User.TELEPHONE, mAccountManager.getUserData(account, LDAPAuthenticatorActivity.PARAM_MAPPING + User.TELEPHONE));
			mappingBundle.putString(User.MOBILE, mAccountManager.getUserData(account, LDAPAuthenticatorActivity.PARAM_MAPPING + User.MOBILE));
			mappingBundle.putString(User.MAIL, mAccountManager.getUserData(account, LDAPAuthenticatorActivity.PARAM_MAPPING + User.MAIL));
			mappingBundle.putString(User.PHOTO, mAccountManager.getUserData(account, LDAPAuthenticatorActivity.PARAM_MAPPING + User.PHOTO));
			users = LDAPUtilities.fetchContacts(host, port, username, authtoken, baseDN, searchFilter, mappingBundle, mLastUpdated);
			// update the last synced date.
			mLastUpdated = new Date();
			// update platform contacts.
			Log.d(TAG, "Calling contactManager's sync contacts");
			ContactManager.syncContacts(mContext, account.name, users);
		} catch (final AuthenticatorException e) {
			syncResult.stats.numParseExceptions++;
			Log.e(TAG, "AuthenticatorException", e);
		} catch (final OperationCanceledException e) {
			Log.e(TAG, "OperationCanceledExcetpion", e);
		} catch (final IOException e) {
			Log.e(TAG, "IOException", e);
			syncResult.stats.numIoExceptions++;
		}
	}
}
