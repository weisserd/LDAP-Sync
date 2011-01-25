/*
 * Copyright 2010 Daniel Weisser
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

package de.danielweisser.android.ldapsync.authenticator;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import de.danielweisser.android.ldapsync.Constants;
import de.danielweisser.android.ldapsync.client.LDAPServerInstance;
import de.danielweisser.android.ldapsync.client.LDAPUtilities;

/**
 * This class is an implementation of AbstractAccountAuthenticator for authenticating accounts in the de.danielweisser.android.ldapsync domain.
 * 
 * @author <a href="mailto:daniel.weisser@gmx.de">Daniel Weisser</a>
 */
class LDAPAuthenticator extends AbstractAccountAuthenticator {
	/** Authentication Service context */
	private final Context mContext;

	private static final String TAG = "LDAPAuthenticator";

	public LDAPAuthenticator(Context context) {
		super(context);
		mContext = context;
	}

	@Override
	public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) {
		Log.i(TAG, "addAccount()");
		final Intent intent = new Intent(mContext, LDAPAuthenticatorActivity.class);
		intent.putExtra(LDAPAuthenticatorActivity.PARAM_AUTHTOKEN_TYPE, authTokenType);
		intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
		final Bundle bundle = new Bundle();
		bundle.putParcelable(AccountManager.KEY_INTENT, intent);
		return bundle;
	}

	@Override
	public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) {
		if (options != null && options.containsKey(AccountManager.KEY_PASSWORD)) {
			final String password = options.getString(AccountManager.KEY_PASSWORD);
			final AccountManager am = AccountManager.get(mContext);
			final String host = am.getUserData(account, LDAPAuthenticatorActivity.PARAM_HOST);
			final String username = am.getUserData(account, LDAPAuthenticatorActivity.PARAM_USERNAME);
			final int port = Integer.parseInt(am.getUserData(account, LDAPAuthenticatorActivity.PARAM_PORT));
			final int encryption = Integer.parseInt(am.getUserData(account, LDAPAuthenticatorActivity.PARAM_ENCRYPTION));
			LDAPServerInstance ldapServer = new LDAPServerInstance(host, port, encryption, username, password);

			final boolean verified = onlineConfirmPassword(ldapServer);
			final Bundle result = new Bundle();
			result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, verified);
			return result;
		}
		// Launch AuthenticatorActivity to confirm credentials
		final Intent intent = new Intent(mContext, LDAPAuthenticatorActivity.class);
		// intent.putExtra(LDAPAuthenticatorActivity.PARAM_USERNAME, account.name);
		intent.putExtra(LDAPAuthenticatorActivity.PARAM_CONFIRMCREDENTIALS, true);
		intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
		final Bundle bundle = new Bundle();
		bundle.putParcelable(AccountManager.KEY_INTENT, intent);
		return bundle;
	}

	@Override
	public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle loginOptions) {
		if (!authTokenType.equals(Constants.AUTHTOKEN_TYPE)) {
			final Bundle result = new Bundle();
			result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType");
			return result;
		}
		final AccountManager am = AccountManager.get(mContext);
		final String password = am.getPassword(account);
		final String host = am.getUserData(account, LDAPAuthenticatorActivity.PARAM_HOST);
		final String username = am.getUserData(account, LDAPAuthenticatorActivity.PARAM_USERNAME);
		final int port = Integer.parseInt(am.getUserData(account, LDAPAuthenticatorActivity.PARAM_PORT));
		final int encryption = Integer.parseInt(am.getUserData(account, LDAPAuthenticatorActivity.PARAM_ENCRYPTION));
		LDAPServerInstance ldapServer = new LDAPServerInstance(host, port, encryption, username, password);
		if (password != null) {
			final boolean verified = onlineConfirmPassword(ldapServer);
			if (verified) {
				final Bundle result = new Bundle();
				result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
				result.putString(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
				result.putString(AccountManager.KEY_AUTHTOKEN, password);
				return result;
			}
		}
		// the password was missing or incorrect, return an Intent to an
		// Activity that will prompt the user for the password.
		final Intent intent = new Intent(mContext, LDAPAuthenticatorActivity.class);
		intent.putExtra(LDAPAuthenticatorActivity.PARAM_USERNAME, username);
		intent.putExtra(LDAPAuthenticatorActivity.PARAM_AUTHTOKEN_TYPE, authTokenType);
		intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
		final Bundle bundle = new Bundle();
		bundle.putParcelable(AccountManager.KEY_INTENT, intent);
		return bundle;
	}

	@Override
	public String getAuthTokenLabel(String authTokenType) {
		Log.i(TAG, "getAuthTokenLabel()");
		return null;

	}

	@Override
	public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) {
		final Bundle result = new Bundle();
		result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
		return result;
	}

	@Override
	public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle loginOptions) {
		final Intent intent = new Intent(mContext, LDAPAuthenticatorActivity.class);
		intent.putExtra(LDAPAuthenticatorActivity.PARAM_USERNAME, account.name);
		intent.putExtra(LDAPAuthenticatorActivity.PARAM_AUTHTOKEN_TYPE, authTokenType);
		intent.putExtra(LDAPAuthenticatorActivity.PARAM_CONFIRMCREDENTIALS, false);
		final Bundle bundle = new Bundle();
		bundle.putParcelable(AccountManager.KEY_INTENT, intent);
		return bundle;
	}

	/**
	 * Validates user's password on the server
	 * 
	 * @param ldapServer
	 *            Configuration data of the LDAP server.
	 * @return <code>true</code> if the authentication data is valid, <code>false</code> otherwise.
	 */
	private boolean onlineConfirmPassword(LDAPServerInstance ldapServer) {
		return LDAPUtilities.authenticate(ldapServer, null, null);
	}
}
