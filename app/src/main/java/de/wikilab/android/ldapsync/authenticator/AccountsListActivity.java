package de.wikilab.android.ldapsync.authenticator;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import de.wikilab.android.ldapsync.Constants;
import de.wikilab.android.ldapsync.R;
import de.wikilab.android.ldapsync.activity.SettingsActivity;

public class AccountsListActivity extends ListActivity {

	private static final String TAG = "AccountsListActivity";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onStart() {
		super.onStart();
		AccountManager accountManager = AccountManager.get(this);
		Account[] accountsList = accountManager.getAccountsByType(Constants.ACCOUNT_TYPE);
		String[] values = new String[accountsList.length];
		for (int i = 0; i < accountsList.length; i++) {
			values[i] = accountsList[i].name;
		}
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, values);
		setListAdapter(adapter);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		String item = (String) getListAdapter().getItem(position);
		Intent intent = new Intent(this, AccountSettingsActivity.class);
		intent.putExtra("accountname", item);
		startActivity(intent);
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	public void addAccount(MenuItem item) {
		startActivity(new Intent(this, LDAPAuthenticatorActivity.class));
	}

	public void scanQr(MenuItem item) {
		IntentIntegrator integrator = new IntentIntegrator(this);
		integrator.initiateScan(IntentIntegrator.QR_CODE_AND_DATA_MATRIX_TYPES);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
		if (scanResult != null) {
			// handle scan result
			try {
				Log.i(TAG, "handling QR code contents: "+scanResult.getContents());
				Uri uri = Uri.parse(scanResult.getContents());
				if (uri.getScheme().equals("ldap") || uri.getScheme().equals("ldaps")) {
					Intent starter = new Intent(this, LDAPAuthenticatorActivity.class);
					starter.setData(uri);
					starter.setAction(Intent.ACTION_VIEW);
					startActivity(starter);
				}
			} catch(Exception ex){
				Log.e(TAG, "error with activity result", ex);
				Toast.makeText(this, "Could not handle this code. Please try again.", Toast.LENGTH_LONG).show();
			}
		}
	}

	public void settings(MenuItem item) {
		startActivity(new Intent(this, SettingsActivity.class));
	}
}
