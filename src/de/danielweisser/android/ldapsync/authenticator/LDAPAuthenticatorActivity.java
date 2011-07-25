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

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ViewFlipper;
import de.danielweisser.android.ldapsync.Constants;
import de.danielweisser.android.ldapsync.R;
import de.danielweisser.android.ldapsync.client.Contact;
import de.danielweisser.android.ldapsync.client.LDAPServerInstance;
import de.danielweisser.android.ldapsync.client.LDAPUtilities;
import de.danielweisser.android.ldapsync.platform.ContactManager;

/**
 * Activity which displays login screen to the user.
 * 
 * @author <a href="mailto:daniel.weisser@gmx.de">Daniel Weisser</a>
 */
public class LDAPAuthenticatorActivity extends AccountAuthenticatorActivity {

	private static final int ERROR_DIALOG = 1;
	private static final int PROGRESS_DIALOG = 0;
	public static final String PARAM_CONFIRMCREDENTIALS = "confirmCredentials";
	public static final String PARAM_USERNAME = "username";
	public static final String PARAM_PASSWORD = "password";
	public static final String PARAM_HOST = "host";
	public static final String PARAM_PORT = "port";
	public static final String PARAM_ENCRYPTION = "encryption";
	public static final String PARAM_AUTHTOKEN_TYPE = "authtokenType";
	public static final String PARAM_SEARCHFILTER = "searchFilter";
	public static final String PARAM_BASEDN = "baseDN";
	public static final String PARAM_MAPPING = "map_";

	private static final String TAG = "LDAPAuthActivity";

	private String message;

	/** Was the original caller asking for an entirely new account? */
	protected boolean mRequestNewAccount = true;

	/**
	 * If set we are just checking that the user knows their credentials, this doesn't cause the user's password to be changed on the device.
	 */
	private Boolean mConfirmCredentials = false;

	/** for posting authentication attempts back to UI thread */
	private final Handler mHandler = new Handler();

	private AccountManager mAccountManager;
	private Thread mAuthThread;
	private String mAuthtoken;
	private String mAuthtokenType;

	private String mPassword;
	private EditText mPasswordEdit;
	private String mUsername;
	private EditText mUsernameEdit;
	private String mHost;
	private EditText mHostEdit;
	private int mEncryption;
	private Spinner mEncryptionSpinner;
	private String mSearchFilter;
	private EditText mSearchFilterEdit;
	private String mBaseDN;
	private AutoCompleteTextView mBaseDNSpinner;
	private int mPort;
	private EditText mPortEdit;

	private String mFirstName;
	private EditText mFirstNameEdit;
	private String mLastName;
	private EditText mLastNameEdit;
	private String mCellPhone;
	private EditText mCellPhoneEdit;
	private String mHomePhone;
	private EditText mHomePhoneEdit;
	private String mOfficePhone;
	private EditText mOfficePhoneEdit;
	private String mEmail;
	private EditText mEmailEdit;
	private String mStreet;
	private EditText mStreetEdit;
	private String mCity;
	private EditText mCityEdit;
	private String mState;
	private EditText mStateEdit;
	private String mZip;
	private EditText mZipEdit;
	private String mCountry;
	private EditText mCountryEdit;
	private String mImage;
	private EditText mImageEdit;
	private Dialog dialog;

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		mAccountManager = AccountManager.get(this);

		getDataFromIntent();
		setLDAPMappings();

		setContentView(R.layout.login_activity);

		mEncryptionSpinner = (Spinner) findViewById(R.id.encryption_spinner);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.encryption_methods, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mEncryptionSpinner.setAdapter(adapter);
		mEncryptionSpinner.setSelection(mEncryption);
		mEncryptionSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				mEncryption = position;
				if (position == 1) {
					mPortEdit.setText("636");
				} else {
					mPortEdit.setText("389");
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// Do nothing.
			}
		});

		// Find controls
		mUsernameEdit = (EditText) findViewById(R.id.username_edit);
		mPasswordEdit = (EditText) findViewById(R.id.password_edit);
		mHostEdit = (EditText) findViewById(R.id.host_edit);
		mPortEdit = (EditText) findViewById(R.id.port_edit);
		mSearchFilterEdit = (EditText) findViewById(R.id.searchfilter_edit);
		mBaseDNSpinner = (AutoCompleteTextView) findViewById(R.id.basedn_spinner);

		// Set values from the intent
		mUsernameEdit.setText(mUsername);
		mPasswordEdit.setText(mAuthtokenType);
		mHostEdit.setText(mHost);
		mPortEdit.setText(Integer.toString(mPort));
		mSearchFilterEdit.setText(mSearchFilter);

		// Set values for LDAP mapping
		mFirstNameEdit = (EditText) findViewById(R.id.firstname_edit);
		mFirstNameEdit.setText(mFirstName);
		mLastNameEdit = (EditText) findViewById(R.id.lastname_edit);
		mLastNameEdit.setText(mLastName);
		mOfficePhoneEdit = (EditText) findViewById(R.id.officephone_edit);
		mOfficePhoneEdit.setText(mOfficePhone);
		mCellPhoneEdit = (EditText) findViewById(R.id.cellphone_edit);
		mCellPhoneEdit.setText(mCellPhone);
		mHomePhoneEdit = (EditText) findViewById(R.id.homephone_edit);
		mHomePhoneEdit.setText(mHomePhone);
		mEmailEdit = (EditText) findViewById(R.id.mail_edit);
		mEmailEdit.setText(mEmail);
		mImageEdit = (EditText) findViewById(R.id.image_edit);
		mImageEdit.setText(mImage);
		mStreetEdit = (EditText) findViewById(R.id.street_edit);
		mStreetEdit.setText(mStreet);
		mCityEdit = (EditText) findViewById(R.id.city_edit);
		mCityEdit.setText(mCity);
		mZipEdit = (EditText) findViewById(R.id.zip_edit);
		mZipEdit.setText(mZip);
		mStateEdit = (EditText) findViewById(R.id.state_edit);
		mStateEdit.setText(mState);
		mCountryEdit = (EditText) findViewById(R.id.country_edit);
		mCountryEdit.setText(mCountry);
	}

	/**
	 * Sets the default LDAP mapping attributes
	 */
	private void setLDAPMappings() {
		if (mRequestNewAccount) {
			// mSearchFilter = "(objectClass=inetOrgPerson)";
			mSearchFilter = "(objectClass=organizationalPerson)";
			mFirstName = "givenName";
			mLastName = "sn";
			mOfficePhone = "telephonenumber";
			mCellPhone = "mobile";
			mHomePhone = "homephone";
			mEmail = "mail";
			mImage = "jpegphoto";
			mStreet = "street";
			mCity = "l";
			mZip = "postalCode";
			mState = "st";
			mCountry = "co";
			// mImage = "thumbnailphoto";
		}
	}

	/**
	 * Obtains data from an intent that was provided for the activity. If no intent was provided some default values are set.
	 */
	private void getDataFromIntent() {
		final Intent intent = getIntent();
		mUsername = intent.getStringExtra(PARAM_USERNAME);
		mPassword = intent.getStringExtra(PARAM_PASSWORD);
		mHost = intent.getStringExtra(PARAM_HOST);
		mPort = intent.getIntExtra(PARAM_PORT, 389);
		mEncryption = intent.getIntExtra(PARAM_ENCRYPTION, 0);
		mRequestNewAccount = (mUsername == null);
		mConfirmCredentials = intent.getBooleanExtra(PARAM_CONFIRMCREDENTIALS, false);
	}

	/**
	 * Called when response is received from the server for confirm credentials request. See onAuthenticationResult(). Sets the AccountAuthenticatorResult which
	 * is sent back to the caller.
	 * 
	 * @param the
	 *            confirmCredentials result.
	 */
	protected void finishConfirmCredentials(boolean result) {
		Log.i(TAG, "finishConfirmCredentials()");
		final Account account = new Account(mHost, Constants.ACCOUNT_TYPE);
		mAccountManager.setPassword(account, mPassword);
		final Intent intent = new Intent();
		intent.putExtra(AccountManager.KEY_BOOLEAN_RESULT, result);
		setAccountAuthenticatorResult(intent.getExtras());
		setResult(RESULT_OK, intent);
		finish();
	}

	/**
	 * Called when response is received from the server for authentication request. See onAuthenticationResult(). Sets the AccountAuthenticatorResult which is
	 * sent back to the caller. Also sets the authToken in AccountManager for this account.
	 */
	protected void finishLogin() {
		Log.i(TAG, "finishLogin()");
		final Account account = new Account(mHost, Constants.ACCOUNT_TYPE);

		if (mRequestNewAccount) {
			Bundle userData = new Bundle();
			userData.putString(PARAM_USERNAME, mUsername);
			userData.putString(PARAM_PORT, mPort + "");
			userData.putString(PARAM_HOST, mHost);
			userData.putString(PARAM_ENCRYPTION, mEncryption + "");
			userData.putString(PARAM_SEARCHFILTER, mSearchFilter);
			userData.putString(PARAM_BASEDN, mBaseDN);
			// Mappings for LDAP data
			userData.putString(PARAM_MAPPING + Contact.FIRSTNAME, mFirstName);
			userData.putString(PARAM_MAPPING + Contact.LASTNAME, mLastName);
			userData.putString(PARAM_MAPPING + Contact.TELEPHONE, mOfficePhone);
			userData.putString(PARAM_MAPPING + Contact.MOBILE, mCellPhone);
			userData.putString(PARAM_MAPPING + Contact.HOMEPHONE, mHomePhone);
			userData.putString(PARAM_MAPPING + Contact.MAIL, mEmail);
			userData.putString(PARAM_MAPPING + Contact.PHOTO, mImage);
			userData.putString(PARAM_MAPPING + Contact.STREET, mStreet);
			userData.putString(PARAM_MAPPING + Contact.CITY, mCity);
			userData.putString(PARAM_MAPPING + Contact.ZIP, mZip);
			userData.putString(PARAM_MAPPING + Contact.STATE, mState);
			userData.putString(PARAM_MAPPING + Contact.COUNTRY, mCountry);
			mAccountManager.addAccountExplicitly(account, mPassword, userData);

			// Set contacts sync for this account.
			ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, true);
			ContactManager.makeGroupVisible(account.name, getContentResolver());
		} else {
			mAccountManager.setPassword(account, mPassword);
		}
		final Intent intent = new Intent();
		mAuthtoken = mPassword;
		intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, account.name);
		intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
		if (mAuthtokenType != null && mAuthtokenType.equals(Constants.AUTHTOKEN_TYPE)) {
			intent.putExtra(AccountManager.KEY_AUTHTOKEN, mAuthtoken);
		}
		setAccountAuthenticatorResult(intent.getExtras());
		setResult(RESULT_OK, intent);
		finish();
	}

	/**
	 * Handles onClick event on the Next button. Sends username/password to the server for authentication.
	 * 
	 * @param view
	 *            The Next button for which this method is invoked
	 */
	public void getLDAPServerDetails(View view) {
		Log.i(TAG, "handleLogin");
		if (mRequestNewAccount) {
			mUsername = mUsernameEdit.getText().toString();
		}
		mPassword = mPasswordEdit.getText().toString();
		mHost = mHostEdit.getText().toString();
		try {
			mPort = Integer.parseInt(mPortEdit.getText().toString());
		} catch (NumberFormatException nfe) {
			Log.i(TAG, "No port given. Set port to 389");
			mPort = 389;
		}
		LDAPServerInstance ldapServer = new LDAPServerInstance(mHost, mPort, mEncryption, mUsername, mPassword);

		showDialog(PROGRESS_DIALOG);
		// Start authenticating...
		mAuthThread = LDAPUtilities.attemptAuth(ldapServer, mHandler, LDAPAuthenticatorActivity.this);
	}

	/**
	 * Call back for the authentication process. When the authentication attempt is finished this method is called.
	 * 
	 * @param baseDNs
	 *            List of baseDNs from the LDAP server
	 * @param result
	 *            result of the authentication process
	 * @param message
	 *            Possible error message
	 */
	public void onAuthenticationResult(String[] baseDNs, boolean result, String message) {
		Log.i(TAG, "onAuthenticationResult(" + result + ")");
		if (dialog != null) {
			dialog.dismiss();
		}
		if (result) {
			if (baseDNs != null) {
				ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, baseDNs);
				adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				mBaseDNSpinner.setAdapter(adapter);
			}
			ViewFlipper vf = (ViewFlipper) findViewById(R.id.server);
			vf.showNext();
		} else {
			this.message = message;
			showDialog(ERROR_DIALOG);
			Log.e(TAG, "onAuthenticationResult: failed to authenticate");
		}
	}

	/**
	 * Handles onClick event on the Done button. Saves the account with the account manager.
	 * 
	 * @param view
	 *            The Done button for which this method is invoked
	 */
	public void saveAccount(View view) {
		mSearchFilter = mSearchFilterEdit.getText().toString();
		mBaseDN = mBaseDNSpinner.getText().toString();
		mFirstName = mFirstNameEdit.getText().toString();
		mLastName = mLastNameEdit.getText().toString();
		mOfficePhone = mOfficePhoneEdit.getText().toString();
		mCellPhone = mCellPhoneEdit.getText().toString();
		mHomePhone = mHomePhoneEdit.getText().toString();
		mEmail = mEmailEdit.getText().toString();
		mImage = mImageEdit.getText().toString();
		mStreet = mStreetEdit.getText().toString();
		mCity = mCityEdit.getText().toString();
		mZip = mZipEdit.getText().toString();
		mState = mStateEdit.getText().toString();
		mCountry = mCountryEdit.getText().toString();

		if (!mConfirmCredentials) {
			finishLogin();
		} else {
			finishConfirmCredentials(true);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == PROGRESS_DIALOG) {
			final ProgressDialog dialog = new ProgressDialog(this);
			dialog.setMessage(getText(R.string.ui_activity_authenticating));
			dialog.setIndeterminate(true);
			dialog.setCancelable(true);
			dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					Log.i(TAG, "dialog cancel has been invoked");
					if (mAuthThread != null) {
						mAuthThread.interrupt();
						finish();
					}
				}
			});
			this.dialog = dialog;
			return dialog;
		} else if (id == ERROR_DIALOG) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Connection error").setMessage("Could not connect to the server:\n" + message).setCancelable(false);
			builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			});
			AlertDialog alert = builder.create();
			return alert;
		}
		return null;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		if (id == ERROR_DIALOG) {
			((AlertDialog) dialog).setMessage("Could not connect to the server:\n" + message);
		}
	}
}
