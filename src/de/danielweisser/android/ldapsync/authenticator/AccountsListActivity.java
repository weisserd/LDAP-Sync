package de.danielweisser.android.ldapsync.authenticator;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import de.danielweisser.android.ldapsync.Constants;

public class AccountsListActivity extends ListActivity {

	// TODO Add title
	private AccountManager accountManager;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		accountManager = AccountManager.get(this);
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

}
