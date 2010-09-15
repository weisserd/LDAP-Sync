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
import de.danielweisser.android.ldapsync.client.LDAPUtilities;

/**
 * This class is an implementation of AbstractAccountAuthenticator for
 * authenticating accounts in the de.danielweisser.android.ldapsync domain.
 */
class LDAPAuthenticator extends AbstractAccountAuthenticator {
	/** Authentication Service context */
	private final Context mContext;

	private static final String TAG = "LDAPAuthenticator";

	public LDAPAuthenticator(Context context) {
		super(context);
		mContext = context;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType,
			String[] requiredFeatures, Bundle options) {
		Log.i(TAG, "addAccount()");
		final Intent intent = new Intent(mContext, LDAPAuthenticatorActivity.class);
		intent.putExtra(LDAPAuthenticatorActivity.PARAM_AUTHTOKEN_TYPE, authTokenType);
		intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
		final Bundle bundle = new Bundle();
		bundle.putParcelable(AccountManager.KEY_INTENT, intent);
		return bundle;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) {
		if (options != null && options.containsKey(AccountManager.KEY_PASSWORD)) {
			final String password = options.getString(AccountManager.KEY_PASSWORD);
			final AccountManager am = AccountManager.get(mContext);
			final String host = am.getUserData(account, LDAPAuthenticatorActivity.PARAM_HOST);
			final int port = Integer.parseInt(am.getUserData(account, LDAPAuthenticatorActivity.PARAM_PORT));
			final boolean verified = onlineConfirmPassword(host, port, account.name, password);
			final Bundle result = new Bundle();
			result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, verified);
			return result;
		}
		// Launch AuthenticatorActivity to confirm credentials
		final Intent intent = new Intent(mContext, LDAPAuthenticatorActivity.class);
		intent.putExtra(LDAPAuthenticatorActivity.PARAM_USERNAME, account.name);
		intent.putExtra(LDAPAuthenticatorActivity.PARAM_CONFIRMCREDENTIALS, true);
		intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
		final Bundle bundle = new Bundle();
		bundle.putParcelable(AccountManager.KEY_INTENT, intent);
		return bundle;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType,
			Bundle loginOptions) {
		if (!authTokenType.equals(Constants.AUTHTOKEN_TYPE)) {
			final Bundle result = new Bundle();
			result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType");
			return result;
		}
		final AccountManager am = AccountManager.get(mContext);
		final String password = am.getPassword(account);
		final String host = am.getUserData(account, LDAPAuthenticatorActivity.PARAM_HOST);
		final int port = Integer.parseInt(am.getUserData(account, LDAPAuthenticatorActivity.PARAM_PORT));
		if (password != null) {
			final boolean verified = onlineConfirmPassword(host, port, account.name, password);
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
		intent.putExtra(LDAPAuthenticatorActivity.PARAM_USERNAME, account.name);
		intent.putExtra(LDAPAuthenticatorActivity.PARAM_AUTHTOKEN_TYPE, authTokenType);
		intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
		final Bundle bundle = new Bundle();
		bundle.putParcelable(AccountManager.KEY_INTENT, intent);
		return bundle;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getAuthTokenLabel(String authTokenType) {
		Log.i(TAG, "getAuthTokenLabel()");
		return null;

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) {
		final Bundle result = new Bundle();
		result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType,
			Bundle loginOptions) {
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
	 */
	private boolean onlineConfirmPassword(String host, int port, String username, String password) {
		return LDAPUtilities.authenticate(host, port, username, password, null/* Handler */, null/* Context */);
	}
}
