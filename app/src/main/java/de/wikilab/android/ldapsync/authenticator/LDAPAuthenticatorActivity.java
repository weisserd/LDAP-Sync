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

package de.wikilab.android.ldapsync.authenticator;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import de.wikilab.android.ldapsync.R;
import de.wikilab.android.ldapsync.client.LDAPServerInstance;
import de.wikilab.android.ldapsync.client.LDAPUtilities;

/**
 * Activity which displays login screen to the user.
 * 
 * @author <a href="mailto:daniel.weisser@gmx.de">Daniel Weisser</a>
 */
public class LDAPAuthenticatorActivity extends Activity {

	private static final int ERROR_DIALOG = 1;
	private static final int PROGRESS_DIALOG = 0;
	public static final String PARAM_CONFIRMCREDENTIALS = "confirmCredentials";

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

	
	private Thread mAuthThread;
	private int mEncryption = 1;
	private Dialog dialog;
	private EditText username;
	private EditText host;
	private String accountName;
	private LDAPServerInstance ldapServer;

	private Uri configUri = null;

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);

		// getDataFromIntent();
		// setLDAPMappings();
		setContentView(R.layout.login_activity);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LDAPAuthenticatorActivity.this);

		username = (EditText) findViewById(R.id.username);
		host = (EditText) findViewById(R.id.host);

		// fetch last used values
		username.setText(prefs.getString("lastUsedUsername",""));
		host.setText(prefs.getString("lastUsedHostname",""));

		// Enable the next button only if the hostname is set - additionally update account name to username + host
		final Button next = (Button) findViewById(R.id.next);
		if(host.getText().toString().equals(""))
			next.setEnabled(false);

		host.addTextChangedListener(new TextWatcher() {

			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s.toString().equals("")) {
					next.setEnabled(false);
				} else {
					next.setEnabled(true);
				}
				updateAccountName();
				SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(LDAPAuthenticatorActivity.this).edit();
				editor.putString("lastUsedHostname", s.toString());
				editor.commit();
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			public void afterTextChanged(Editable s) {
			}
		});

		username.addTextChangedListener(new TextWatcher() {

			public void onTextChanged(CharSequence s, int start, int before, int count) {
				updateAccountName();
				SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(LDAPAuthenticatorActivity.this).edit();
				editor.putString("lastUsedUsername", s.toString());
				editor.commit();
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			public void afterTextChanged(Editable s) {
			}
		});

		// Fill the encryption spinner and change port according to selected encryption
		Spinner mEncryptionSpinner = (Spinner) findViewById(R.id.encryption);
		final EditText mPortEdit = (EditText) findViewById(R.id.port);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.encryption_methods, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mEncryptionSpinner.setAdapter(adapter);
		mEncryption = prefs.getInt("lastUsedEncryption", 1);
		mEncryptionSpinner.setSelection(mEncryption);
		if (mEncryption == 1) {
			mPortEdit.setText("636");
		} else {
			mPortEdit.setText("389");
		}
		mEncryptionSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				mEncryption = position;
				if (position == 1) {
					mPortEdit.setText("636");
				} else {
					mPortEdit.setText("389");
				}
			}

			public void onNothingSelected(AdapterView<?> parent) {
				// Do nothing.
			}
		});


		// Handle "ldaps://" URL links, e.g. from barcode scanner or website
		Intent intent = getIntent();
		if (Intent.ACTION_VIEW.equals(intent.getAction())) {
			Uri uri = intent.getData();
			configUri = uri;
			host.setText(uri.getHost());

			if (uri.getScheme().equals("ldap")) {
				mEncryption = 0;
				mEncryptionSpinner.setSelection(mEncryption);
				mPortEdit.setText("389");
			}

			if (uri.getPort() > -1) mPortEdit.setText(String.valueOf(uri.getPort()));

			if (uri.getUserInfo() != null) {
				String userInfo[] = uri.getUserInfo().split(":", 2);
				if (userInfo.length == 1) {
					username.setText(userInfo[0]);
				} else if (userInfo.length == 2) {
					username.setText(userInfo[0]);
					((EditText) findViewById(R.id.password)).setText(userInfo[0]);
				}
			}

			if (uri.getQueryParameter("user") != null)
				username.setText(uri.getQueryParameter("user"));
			if (uri.getQueryParameter("password") != null)
				((EditText) findViewById(R.id.password)).setText(uri.getQueryParameter("password"));

			next.setEnabled(true);
			updateAccountName();

			if (uri.getQueryParameter("accountName") != null) {
				final EditText accountName = (EditText) findViewById(R.id.account_name);
				accountName.setText(uri.getQueryParameter("accountName"));
			}
		}
	}


	private void updateAccountName() {
		final EditText accountName = (EditText) findViewById(R.id.account_name);
		if (TextUtils.isEmpty(username.getText().toString())) {
			accountName.setText(host.getText().toString());
		} else {
			accountName.setText(username.getText().toString() + "@" + host.getText().toString());
		}
	}

	/**
	 * Obtains data from an intent that was provided for the activity. If no intent was provided some default values are set.
	 */
	private void getDataFromIntent() {
		final Intent intent = getIntent();
		// mRequestNewAccount = (mUsername == null);
		mConfirmCredentials = intent.getBooleanExtra(PARAM_CONFIRMCREDENTIALS, false);
	}

	/**
	 * Called when the user touches the next button. Sends username/password to the server for authentication.
	 * 
	 * @param view
	 *            The Next button for which this method is invoked
	 */
	public void next(View view) {
		accountName = ((EditText) findViewById(R.id.account_name)).getText().toString();

		// if (mRequestNewAccount) {
		String username = ((EditText) findViewById(R.id.username)).getText().toString();
		// }
		String password = ((EditText) findViewById(R.id.password)).getText().toString();
		String host = ((EditText) findViewById(R.id.host)).getText().toString();
		int port;
		try {
			port = Integer.parseInt(((EditText) findViewById(R.id.port)).getText().toString());
		} catch (NumberFormatException nfe) {
			Log.i(TAG, "No port given. Set port to 389");
			port = 389;
		}
		Log.i(TAG, "Now trying to login to server" + host + " for user " + username);
		ldapServer = new LDAPServerInstance(host, port, mEncryption, username, password);

		showDialog(PROGRESS_DIALOG);
		// Start authenticating...
		mAuthThread = LDAPUtilities.attemptAuth(ldapServer, mHandler, LDAPAuthenticatorActivity.this);
	}

	/**
	 * Call back for the authentication process. When the authentication attempt is finished this method is called.
	 * Called by LDAPUtilities.sendResult
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
			// Build intent with baseDNs and mappings (auto-detect)
			Intent intent = new Intent(this, AccountSettingsActivity.class);
			intent.putExtra("accountname", accountName);
			if (baseDNs != null) {
				intent.putExtra("baseDNs", baseDNs);
			}
			intent.putExtra("ldapServer", ldapServer);

			if (configUri != null) {
				intent.putExtra("configUri", configUri);
			}

			startActivity(intent);
			finish();
		} else {
			this.message = message;
			showDialog(ERROR_DIALOG);
			Log.e(TAG, "Error during authentication: " + message);
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
			builder.setTitle("Connection error").setMessage(message).setCancelable(false);
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
