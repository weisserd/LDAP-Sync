package de.danielweisser.android.ldapsync.platform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.Settings;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.text.TextUtils;
import android.util.Log;
import de.danielweisser.android.ldapsync.Constants;
import de.danielweisser.android.ldapsync.client.User;

/**
 * Class for managing contacts sync related operations
 */
public class ContactManager {
	private static final String TAG = "ContactManager";

	/**
	 * Synchronize raw contacts
	 * 
	 * @param context
	 *            The context of Authenticator Activity
	 * @param accountName
	 *            The account name
	 * @param contacts
	 *            The list of retrieved LDAP contacts
	 */
	public static synchronized void syncContacts(Context context, String accountName, List<User> contacts) {
		final ContentResolver resolver = context.getContentResolver();

		// Check in a first step which contacts must be deleted, updated and
		// created
		HashMap<String, Integer> contactsOnPhone = getAllContactsOnPhone(resolver, accountName);
		logMap(contactsOnPhone);

		// Update and create new contacts
		for (final User user : contacts) {
			if (contactsOnPhone.containsKey(user.getDN())) {
				Integer contactId = contactsOnPhone.get(user.getDN());
				Log.d(TAG, "Update contact: " + user.getFirstName() + " " + user.getLastName() + " (" + contactId + ")");
				// TODO Update
				contactsOnPhone.remove(user.getDN());
			} else {
				Log.d(TAG, "Add contact: " + user.getFirstName() + " " + user.getLastName());
				addContact(context, accountName, user, resolver);
			}
		}

		// Delete contacts
		logMap(contactsOnPhone);
		for (Entry<String, Integer> contact : contactsOnPhone.entrySet()) {
			Log.d(TAG, "Delete contact: " + contact.getKey() + " (" + contact.getValue() + ")");
			// TODO Delete contact
			// deleteContacts(resolver, account);
		}
	}

	private static void logMap(HashMap<String, Integer> contactsOnPhone) {
		Log.d(TAG, "Contacts in Map: " + contactsOnPhone.size());
		for (Entry<String, Integer> entry : contactsOnPhone.entrySet()) {
			Log.d(TAG, "  ID: " + entry.getValue() + ", DN: " + entry.getKey());
		}
	}

	/**
	 * Retrieves all contacts that are on the phone for this account.
	 * 
	 * @return
	 */
	private static HashMap<String, Integer> getAllContactsOnPhone(ContentResolver resolver, String accountName) {
		final String[] projection = new String[] { RawContacts.CONTACT_ID, RawContacts.SYNC1 };
		final String selection = RawContacts.ACCOUNT_NAME + "=?";

		final Cursor c = resolver.query(RawContacts.CONTENT_URI, projection, selection, new String[] { accountName }, null);
		HashMap<String, Integer> contactsOnPhone = new HashMap<String, Integer>(c.getCount());
		while (c.moveToNext()) {
			contactsOnPhone.put(c.getString(c.getColumnIndex(RawContacts.SYNC1)), c.getInt(c.getColumnIndex(Data.CONTACT_ID)));
		}
		return contactsOnPhone;
	}

	private static void deleteContacts(ContentResolver resolver, String accountName) {
		String[] projection = new String[] { Data.CONTACT_ID, RawContacts.ACCOUNT_NAME };
		final String selection = RawContacts.ACCOUNT_NAME + "=?";

		final Cursor c = resolver.query(Data.CONTENT_URI, projection, selection, new String[] { accountName }, null);
		HashSet<String> contactIds = new HashSet<String>();
		try {
			while (c.moveToNext()) {
				contactIds.add(c.getString(c.getColumnIndex(Data.CONTACT_ID)));
			}
			Log.d(TAG, "Ids: " + contactIds);
		} finally {
			c.close();
		}

		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
		for (String rawContactId : contactIds) {
			Log.v(TAG, "Deleting contact: " + rawContactId);
			ops.add(ContentProviderOperation.newDelete(RawContacts.CONTENT_URI).withSelection(RawContacts._ID + "=?", new String[] { rawContactId }).build());
			ops.add(ContentProviderOperation.newDelete(Data.CONTENT_URI).withSelection(Data.RAW_CONTACT_ID + "=?", new String[] { rawContactId }).build());
		}

		try {
			resolver.applyBatch(ContactsContract.AUTHORITY, ops);
		} catch (RemoteException e) {
			Log.e(TAG, e.getMessage(), e);
		} catch (OperationApplicationException e) {
			Log.e(TAG, e.getMessage(), e);
		}
	}

	private static void addContact(Context context, String accountName, User user, ContentResolver resolver) {
		// Put the data in the contacts provider
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
		int rawContactInsertIndex = ops.size();

		ContentValues cv = new ContentValues();
		cv.put(RawContacts.ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
		cv.put(RawContacts.ACCOUNT_NAME, accountName);
		cv.put(RawContacts.SYNC1, user.getDN());
		ops.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI).withValues(cv).build());

		// Store name of the account
		cv.clear();
		cv.put(StructuredName.GIVEN_NAME, user.getFirstName());
		cv.put(StructuredName.FAMILY_NAME, user.getLastName());
		cv.put(StructuredName.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
		ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex).withValues(cv).build());

		// E-Mail
		if (user.getEmails() != null) {
			for (String mail : user.getEmails()) {
				cv.clear();
				cv.put(Email.DATA, mail);
				cv.put(Email.TYPE, Email.TYPE_WORK);
				cv.put(Email.MIMETYPE, Email.CONTENT_ITEM_TYPE);
				ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex).withValues(cv)
						.build());
			}
		}

		// Cellphone
		if (!TextUtils.isEmpty(user.getCellPhone())) {
			cv.clear();
			cv.put(Phone.NUMBER, user.getCellPhone());
			cv.put(Phone.TYPE, Phone.TYPE_WORK_MOBILE);
			cv.put(Phone.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
			ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex).withValues(cv)
					.build());
		}

		// Office phone
		if (!TextUtils.isEmpty(user.getOfficePhone())) {
			cv.clear();
			cv.put(Phone.NUMBER, user.getOfficePhone());
			cv.put(Phone.TYPE, Phone.TYPE_WORK);
			cv.put(Phone.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
			ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex).withValues(cv)
					.build());
		}

		// Image
		if (user.getImage() != null) {
			cv.clear();
			cv.put(ContactsContract.CommonDataKinds.Photo.PHOTO, user.getImage());
			cv.put(ContactsContract.CommonDataKinds.Photo.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE);
			ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex).withValues(cv)
					.build());
		}

		try {
			resolver.applyBatch(ContactsContract.AUTHORITY, ops);
		} catch (RemoteException e) {
			Log.e(TAG, e.getMessage(), e);
		} catch (OperationApplicationException e) {
			Log.e(TAG, e.getMessage(), e);
		}
	}

	public static void makeGroupVisible(String accountName, ContentResolver resolver) {
		try {
			ContentProviderClient client = resolver.acquireContentProviderClient(ContactsContract.AUTHORITY_URI);
			ContentValues cv = new ContentValues();
			cv.put(Groups.ACCOUNT_NAME, accountName);
			cv.put(Groups.ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
			cv.put(Settings.UNGROUPED_VISIBLE, true);
			client.insert(Settings.CONTENT_URI.buildUpon().appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true").build(), cv);
		} catch (RemoteException e) {
			Log.d(TAG, "Cannot make the Group Visible");
		}
	}
}
