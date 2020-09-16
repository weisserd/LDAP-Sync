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

package de.wikilab.android.ldapsync.syncadapter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import de.wikilab.android.ldapsync.Constants;
import de.wikilab.android.ldapsync.client.Contact;
import de.wikilab.android.ldapsync.client.LDAPServerInstance;
import de.wikilab.android.ldapsync.client.LDAPUtilities;
import de.wikilab.android.ldapsync.platform.ContactManager;

/**
 * SyncAdapter implementation for synchronizing LDAP contacts to the platform ContactOperations provider.
 * 
 * @author <a href="mailto:daniel.weisser@gmx.de">Daniel Weisser</a>
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
	public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
		Logger l = new Logger();

		if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("enable_debug_logging", false))
			l.startLogging();
		l.d("Start the sync");
		Log.d(TAG, "Start the sync.");
		List<Contact> users = new ArrayList<Contact>();
		// use the account manager to request the credentials
		final String password = mAccountManager.getPassword(account);
		final String host = mAccountManager.getUserData(account, Constants.PARAM_HOST);
		final String username = mAccountManager.getUserData(account, Constants.PARAM_USERNAME);
		final int port = Integer.parseInt(mAccountManager.getUserData(account, Constants.PARAM_PORT));
		final String sEnc = mAccountManager.getUserData(account, Constants.PARAM_ENCRYPTION);
		int encryption = 0;
		if (!TextUtils.isEmpty(sEnc)) {
			encryption = Integer.parseInt(sEnc);
		}
		LDAPServerInstance ldapServer = new LDAPServerInstance(host, port, encryption, username, password);

		SharedPreferences preferences = getContext().getSharedPreferences(account.name, Activity.MODE_PRIVATE);
		final String searchFilter = preferences.getString("searchFilter", "");
		final String baseDN = preferences.getString("baseDN", "");

		users = LDAPUtilities.fetchContacts(ldapServer, baseDN, searchFilter, preferences, mLastUpdated, this.getContext());
		if (users == null) {
			syncResult.stats.numIoExceptions++;
			return;
		}
		// update the last synced date.
		mLastUpdated = new Date();
		// update platform contacts.
		Log.d(TAG, "Calling contactManager's sync contacts");
		l.d("Calling contactManager's sync contacts");
		ContactManager cm = new ContactManager(l);
		cm.syncContacts(mContext, account.name, users, syncResult);
		l.stopLogging();
	}
}
