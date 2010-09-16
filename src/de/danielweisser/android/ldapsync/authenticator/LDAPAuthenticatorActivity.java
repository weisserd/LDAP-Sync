package de.danielweisser.android.ldapsync.authenticator;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ViewFlipper;
import android.widget.AdapterView.OnItemSelectedListener;
import de.danielweisser.android.ldapsync.Constants;
import de.danielweisser.android.ldapsync.R;
import de.danielweisser.android.ldapsync.client.LDAPUtilities;
import de.danielweisser.android.ldapsync.client.User;

/**
 * Activity which displays login screen to the user.
 */
public class LDAPAuthenticatorActivity extends AccountAuthenticatorActivity {

	public static final String PARAM_CONFIRMCREDENTIALS = "confirmCredentials";
	public static final String PARAM_USERNAME = "username";
	public static final String PARAM_PASSWORD = "password";
	public static final String PARAM_HOST = "host";
	public static final String PARAM_PORT = "port";
	public static final String PARAM_AUTHTOKEN_TYPE = "authtokenType";
	public static final String PARAM_SEARCHFILTER = "searchFilter";
	public static final String PARAM_BASEDN = "baseDN";
	public static final String PARAM_MAPPING = "map_";

	private static final String TAG = "LDAPAuthActivity";

	/** Was the original caller asking for an entirely new account? */
	protected boolean mRequestNewAccount = true;

	/**
	 * If set we are just checking that the user knows their credentials, this
	 * doesn't cause the user's password to be changed on the device.
	 */
	private Boolean mConfirmCredentials = false;

	/** for posting authentication attempts back to UI thread */
	private final Handler mHandler = new Handler();

	private AccountManager mAccountManager;
	private Thread mAuthThread;
	private String mAuthtoken;
	private String mAuthtokenType;

	private TextView mMessage;
	private String mPassword;
	private EditText mPasswordEdit;
	private String mUsername;
	private EditText mUsernameEdit;
	private String mHost;
	private EditText mHostEdit;
	private String mSearchFilter;
	private EditText mSearchFilterEdit;
	private String mBaseDN;
	private Spinner mBaseDNSpinner;
	private int mPort;
	private EditText mPortEdit;
	
	private String mFirstName;
	private EditText mFirstNameEdit;
	private String mLastName;
	private EditText mLastNameEdit;
	private String mCellPhone;
	private EditText mCellPhoneEdit;
	private String mOfficePhone;
	private EditText mOfficePhoneEdit;
	private String mEmail;
	private EditText mEmailEdit;
	private String mImage;
	private EditText mImageEdit;

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
//		android.os.Debug.waitForDebugger();
		mAccountManager = AccountManager.get(this);
	
		// Get data from Intent
		final Intent intent = getIntent();
		mUsername = intent.getStringExtra(PARAM_USERNAME);
		mPassword = intent.getStringExtra(PARAM_PASSWORD);
		mHost = intent.getStringExtra(PARAM_HOST);
		mPort = intent.getIntExtra(PARAM_PORT, 389);
		mRequestNewAccount = (mUsername == null);
		mConfirmCredentials = intent.getBooleanExtra(PARAM_CONFIRMCREDENTIALS, false);
	
		if (mRequestNewAccount) {
			mSearchFilter = "(objectClass=person)";
			mFirstName = "givenName";
			mLastName = "sn";
			mOfficePhone = "telephonenumber";
			mCellPhone = "mobile";
			mEmail = "mail";
			mImage = "jpegphoto";
		}
	
		setContentView(R.layout.login_activity);
	
		// Find controls
		mMessage = (TextView) findViewById(R.id.message);
		mUsernameEdit = (EditText) findViewById(R.id.username_edit);
		mPasswordEdit = (EditText) findViewById(R.id.password_edit);
		mHostEdit = (EditText) findViewById(R.id.host_edit);
		mPortEdit = (EditText) findViewById(R.id.port_edit);
		mSearchFilterEdit = (EditText) findViewById(R.id.searchfilter_edit);
		mBaseDNSpinner = (Spinner) findViewById(R.id.basedn_spinner);
		mBaseDNSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
	
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				mBaseDN = parent.getItemAtPosition(position).toString();
			}
	
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// Do nothing.
			}
		});
	
		// Set values from the intent
		mUsernameEdit.setText(mUsername);
		mPasswordEdit.setText(mAuthtokenType);
		mHostEdit.setText(mHost);
		mPortEdit.setText(Integer.toString(mPort));
		mSearchFilterEdit.setText(mSearchFilter);
		mMessage.setText(getMessage());
		
		// Set values for LDAP mapping
		mFirstNameEdit = (EditText) findViewById(R.id.firstname_edit);
		mFirstNameEdit.setText(mFirstName);
		mLastNameEdit = (EditText) findViewById(R.id.lastname_edit);
		mLastNameEdit.setText(mLastName);
		mOfficePhoneEdit = (EditText) findViewById(R.id.officephone_edit);
		mOfficePhoneEdit.setText(mOfficePhone);
		mCellPhoneEdit= (EditText) findViewById(R.id.cellphone_edit);
		mCellPhoneEdit.setText(mCellPhone);
		mEmailEdit = (EditText) findViewById(R.id.mail_edit);
		mEmailEdit.setText(mEmail);
		mImageEdit = (EditText) findViewById(R.id.image_edit);
		mImageEdit.setText(mImage);
	}

	/**
	 * Called when response is received from the server for confirm credentials
	 * request. See onAuthenticationResult(). Sets the
	 * AccountAuthenticatorResult which is sent back to the caller.
	 * 
	 * @param the
	 *            confirmCredentials result.
	 */
	protected void finishConfirmCredentials(boolean result) {
		Log.i(TAG, "finishConfirmCredentials()");
		final Account account = new Account(mUsername, Constants.ACCOUNT_TYPE);
		mAccountManager.setPassword(account, mPassword);
		final Intent intent = new Intent();
		intent.putExtra(AccountManager.KEY_BOOLEAN_RESULT, result);
		setAccountAuthenticatorResult(intent.getExtras());
		setResult(RESULT_OK, intent);
		finish();
	}

	/**
	 * Called when response is received from the server for authentication
	 * request. See onAuthenticationResult(). Sets the
	 * AccountAuthenticatorResult which is sent back to the caller. Also sets
	 * the authToken in AccountManager for this account.
	 * 
	 * @param the
	 *            confirmCredentials result.
	 */
	protected void finishLogin() {
		Log.i(TAG, "finishLogin()");
		final Account account = new Account(mUsername, Constants.ACCOUNT_TYPE);

		if (mRequestNewAccount) {
			Bundle userData = new Bundle();
			userData.putString(PARAM_PORT, mPort + "");
			userData.putString(PARAM_HOST, mHost);
			userData.putString(PARAM_SEARCHFILTER, mSearchFilter);
			userData.putString(PARAM_BASEDN, mBaseDN);
			// Mappings for LDAP data
			userData.putString(PARAM_MAPPING + User.FIRSTNAME, mFirstName);
			userData.putString(PARAM_MAPPING + User.LASTNAME, mLastName);
			userData.putString(PARAM_MAPPING + User.TELEPHONE, mOfficePhone);
			userData.putString(PARAM_MAPPING + User.MOBILE, mCellPhone);
			userData.putString(PARAM_MAPPING + User.MAIL, mEmail);
			userData.putString(PARAM_MAPPING + User.PHOTO, mImage);
			mAccountManager.addAccountExplicitly(account, mPassword, userData);

			// Set contacts sync for this account.
			ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, true);
		} else {
			mAccountManager.setPassword(account, mPassword);
		}
		final Intent intent = new Intent();
		mAuthtoken = mPassword;
		intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, mUsername);
		intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
		if (mAuthtokenType != null && mAuthtokenType.equals(Constants.AUTHTOKEN_TYPE)) {
			intent.putExtra(AccountManager.KEY_AUTHTOKEN, mAuthtoken);
		}
		setAccountAuthenticatorResult(intent.getExtras());
		setResult(RESULT_OK, intent);
		finish();
	}

	/**
	 * Returns the message to be displayed at the top of the login dialog box.
	 */
	private CharSequence getMessage() {
		getString(R.string.label);
		if (TextUtils.isEmpty(mUsername)) {
			return getText(R.string.login_activity_newaccount_text);
		}
		return null;
	}

	/**
	 * Handles onClick event on the Next button. Sends username/password to the
	 * server for authentication.
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
		mPort = Integer.parseInt(mPortEdit.getText().toString());

		if (TextUtils.isEmpty(mUsername) || TextUtils.isEmpty(mPassword)) {
			mMessage.setText(getMessage());
		} else {
			showProgress();
			// Start authenticating...
			mAuthThread = LDAPUtilities.attemptAuth(mHost, mPort, mUsername, mPassword, mHandler,
					LDAPAuthenticatorActivity.this);
		}
	}

	/**
	 * Call back for the authentication process. When the authentication attempt
	 * is finished this method is called.
	 */
	public void onAuthenticationResult(String[] baseDNs, boolean result) {
		Log.i(TAG, "onAuthenticationResult(" + result + ")");
		hideProgress();
		if (result) {
			ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this,
					android.R.layout.simple_spinner_item, baseDNs);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mBaseDNSpinner.setAdapter(adapter);
			ViewFlipper vf = (ViewFlipper) findViewById(R.id.server);
			vf.showNext();
		} else {
			Log.e(TAG, "onAuthenticationResult: failed to authenticate");
			mMessage.setText(getText(R.string.login_activity_loginfail));
		}
	}

	/**
	 * Handles onClick event on the Done button. Saves the account with the
	 * acount manager.
	 * 
	 * @param view
	 *            The Done button for which this method is invoked
	 */
	public void saveAccount(View view) {
		mSearchFilter = mSearchFilterEdit.getText().toString();
		mFirstName = mFirstNameEdit.getText().toString();
		mLastName = mLastNameEdit.getText().toString();
		mOfficePhone = mOfficePhoneEdit.getText().toString();
		mCellPhone = mCellPhoneEdit.getText().toString();
		mEmail = mEmailEdit.getText().toString();
		mImage = mImageEdit.getText().toString();

		if (!mConfirmCredentials) {
			finishLogin();
		} else {
			finishConfirmCredentials(true);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
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
		return dialog;
	}

	/**
	 * Shows the progress UI for a lengthy operation.
	 */
	protected void showProgress() {
		showDialog(0);
	}

	/**
	 * Hides the progress UI for a lengthy operation.
	 */
	protected void hideProgress() {
		dismissDialog(0);
	}
}
