package de.danielweisser.android.ldapsync.syncadapter;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;
import de.danielweisser.android.ldapsync.Constants;
import de.danielweisser.android.ldapsync.client.LDAPUtilities;
import de.danielweisser.android.ldapsync.client.User;
import de.danielweisser.android.ldapsync.platform.ContactManager;

/**
 * SyncAdapter implementation for syncing LDAP contacts to the
 * platform ContactOperations provider.
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
    public void onPerformSync(Account account, Bundle extras, String authority,
        ContentProviderClient provider, SyncResult syncResult) {
    	Log.d(TAG, "Start the sync.");
        List<User> users;
        String authtoken = null;
         try {
             // use the account manager to request the credentials
             authtoken =
                mAccountManager.blockingGetAuthToken(account,
                    Constants.AUTHTOKEN_TYPE, true /* notifyAuthFailure */);
             // fetch updates from the sample service over the cloud
             users =
                LDAPUtilities.fetchContacts(account, authtoken,
                    mLastUpdated);
            // update the last synced date.
            mLastUpdated = new Date();
            // update platform contacts.
            Log.d(TAG, "Calling contactManager's sync contacts");
            ContactManager.syncContacts(mContext, account.name, users);
        } catch (final AuthenticatorException e) {
            syncResult.stats.numParseExceptions++;
            Log.e(TAG, "AuthenticatorException", e);
        } catch (final OperationCanceledException e) {
            Log.e(TAG, "OperationCanceledExcetpion", e);
        } catch (final IOException e) {
            Log.e(TAG, "IOException", e);
            syncResult.stats.numIoExceptions++;
        }
    }
}
