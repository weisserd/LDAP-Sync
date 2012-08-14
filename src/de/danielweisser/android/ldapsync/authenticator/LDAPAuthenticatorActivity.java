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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
import de.danielweisser.android.ldapsync.R;
import de.danielweisser.android.ldapsync.client.LDAPServerInstance;
import de.danielweisser.android.ldapsync.client.LDAPUtilities;

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
	private int mEncryption = 0;
	private Dialog dialog;
	private EditText username;
	private EditText host;
	private String accountName;
	private LDAPServerInstance ldapServer;

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);

		// getDataFromIntent();
		// setLDAPMappings();
		setContentView(R.layout.login_activity);

		// Enable the next button only if the hostname is set - additionally update account name to username + host
		final Button next = (Button) findViewById(R.id.next);
		next.setEnabled(false);
		username = (EditText) findViewById(R.id.username);
		host = (EditText) findViewById(R.id.host);
		host.addTextChangedListener(new TextWatcher() {

			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s.toString().equals("")) {
					next.setEnabled(false);
				} else {
					next.setEnabled(true);
				}
				updateAccountName();
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			public void afterTextChanged(Editable s) {
			}
		});

		username.addTextChangedListener(new TextWatcher() {

			public void onTextChanged(CharSequence s, int start, int before, int count) {
				updateAccountName();
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
		mEncryptionSpinner.setSelection(mEncryption);
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
			// Build intent with baseDNs and mappings (auto-detect)
			Intent intent = new Intent(this, AccountSettingsActivity.class);
			intent.putExtra("accountname", accountName);
			if (baseDNs != null) {
				intent.putExtra("baseDNs", baseDNs);
			}
			intent.putExtra("ldapServer", ldapServer);
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
