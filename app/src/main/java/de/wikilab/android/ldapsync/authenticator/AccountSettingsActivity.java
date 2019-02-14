package de.wikilab.android.ldapsync.authenticator;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import de.wikilab.android.ldapsync.Constants;
import de.wikilab.android.ldapsync.R;
import de.wikilab.android.ldapsync.client.LDAPServerInstance;
import de.wikilab.android.ldapsync.platform.ContactManager;

public class AccountSettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

	private static final String TAG = "AccountSettingsActivity";
	private String accountName;
	private LDAPServerInstance ldapServerInstance;

	// TODO Add title
	// TODO Remove Done if not in create mode
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Account account = (Account) getIntent().getParcelableExtra("account");
		if (account != null) {
			accountName = account.name;
		} else {
			accountName = getIntent().getStringExtra("accountname");
			ldapServerInstance = (LDAPServerInstance) getIntent().getSerializableExtra("ldapServer");
		}
		getPreferenceManager().setSharedPreferencesName(accountName);
		final SharedPreferences sharedPrefs = getPreferenceManager().getSharedPreferences();
		sharedPrefs.registerOnSharedPreferenceChangeListener(this);
		Log.i(TAG, "Get preferences for " + accountName);

		addPreferencesFromResource(R.xml.preference_resources);
		setContentView(R.layout.preference_layout);



		if (getIntent().hasExtra("configUri")) {
			Uri configUri = getIntent().getParcelableExtra("configUri");
			Log.i(TAG, "config URI found: "+configUri.toString());


			SharedPreferences.Editor editor = sharedPrefs.edit();
			for (String key: sharedPrefs.getAll().keySet()) {
				Log.i(TAG, "config URI: checking param "+key);

				String value = configUri.getQueryParameter("cfg_" + key);
				if (value != null) {
					Log.i(TAG, "config URI: OK - param "+key+" has value "+value);
					editor.putString(key, value);
				}
			}
			editor.commit();

			// allow skipping of details config
			if ("1".equals(configUri.getQueryParameter("skip"))) {
				createAccount(null);
			}
		}
		if (getIntent().hasExtra("baseDNs") && sharedPrefs.getString("baseDN","").equals("")) {
			final String []baseDNs = getIntent().getStringArrayExtra("baseDNs");
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Select Base DN")
					.setItems(baseDNs, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							SharedPreferences.Editor editor = sharedPrefs.edit();
							editor.putString("baseDN", baseDNs[which]);
							editor.commit();
							onSharedPreferenceChanged(sharedPrefs, "baseDN");
						}
					});
			builder.show();
		}

		// Initialize all summaries to values
		for (String key: sharedPrefs.getAll().keySet()) {
			onSharedPreferenceChanged(sharedPrefs, key);
		}



		// this.getIntent().getExtras()) and the key "account

		// ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, baseDNs);
		// adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// mBaseDNSpinner.setAdapter(adapter);
		// set the preferences file name
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.preference_menu, menu);

		MenuItem saveItem = menu.findItem(R.id.action_save);

		return super.onCreateOptionsMenu(menu);
	}

    /**
	 * Called when the user touches the done button.
	 * 
	 * @param view
	 *            The Next button for which this method is invoked
	 */
	public void createAccount(View view) {
		Log.i(TAG, "finishLogin()");
		if (ldapServerInstance != null) {
			// Only create new account while in the new account flow, but not when editing existing account
			final Account account = new Account(accountName, Constants.ACCOUNT_TYPE);

			Bundle userData = new Bundle();
			userData.putString(Constants.PARAM_USERNAME, ldapServerInstance.bindDN);
			userData.putString(Constants.PARAM_PORT, ldapServerInstance.port + "");
			userData.putString(Constants.PARAM_HOST, ldapServerInstance.host);
			userData.putString(Constants.PARAM_ENCRYPTION, ldapServerInstance.encryption + "");
			AccountManager mAccountManager = AccountManager.get(this);
			mAccountManager.addAccountExplicitly(account, ldapServerInstance.bindPW, userData);

			// Set contacts sync for this account.
			ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, true);
			ContactManager.makeGroupVisible(account.name, getContentResolver());
		}
		setResult(RESULT_OK, new Intent());
		finish();
	}

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        Log.d(TAG, "onContentChanged for " + s);
        Preference p = findPreference(s);
        if (p != null) {
            Object value = sharedPreferences.getAll().get(s);
            if (value != null) {
                p.setSummary(value.toString());
            }
        }
    }

	public void createAccount2(MenuItem item) {
		createAccount(null);
	}
}
