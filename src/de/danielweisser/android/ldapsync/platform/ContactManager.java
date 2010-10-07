package de.danielweisser.android.ldapsync.platform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.text.TextUtils;
import android.util.Log;
import de.danielweisser.android.ldapsync.Constants;
import de.danielweisser.android.ldapsync.client.Contact;

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
	public static synchronized void syncContacts(Context context, String accountName, List<Contact> contacts) {

		// TODO Check when LDAP connection fails!!!!
		final ContentResolver resolver = context.getContentResolver();

		// Get all phone contacts for the LDAP account
		HashMap<String, Integer> contactsOnPhone = getAllContactsOnPhone(resolver, accountName);

		// Update and create new contacts
		for (final Contact contact : contacts) {
			if (contactsOnPhone.containsKey(contact.getDN())) {
				Integer contactId = contactsOnPhone.get(contact.getDN());
				Log.d(TAG, "Update contact: " + contact.getDN());
				updateContact(resolver, contactId, contact);
				contactsOnPhone.remove(contact.getDN());
			} else {
				Log.d(TAG, "Add contact: " + contact.getFirstName() + " " + contact.getLastName());
				addContact(resolver, accountName, contact);
			}
		}

		// Delete contacts
		for (Entry<String, Integer> contact : contactsOnPhone.entrySet()) {
			Log.d(TAG, "Delete contact: " + contact.getKey());
			deleteContact(resolver, contact.getValue());
		}
	}

	private static void updateContact(ContentResolver resolver, Integer contactId, Contact contact) {
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

		checkAndUpdateName(resolver, contactId, contact, ops);
		updateWorkMobileNumber(resolver, contactId, contact, ops);
		updateWorkNumber(resolver, contactId, contact, ops);
		updatePicture(resolver, contactId, contact, ops);
		updateWorkEmails(resolver, contactId, contact, ops);

		try {
			resolver.applyBatch(ContactsContract.AUTHORITY, ops);
		} catch (RemoteException e) {
			Log.e(TAG, e.getMessage(), e);
		} catch (OperationApplicationException e) {
			Log.e(TAG, e.getMessage(), e);
		}
	}

	private static void updateWorkEmails(ContentResolver resolver, Integer contactId, Contact contact, ArrayList<ContentProviderOperation> ops) {
		// Get all e-mail addresses for the contact
		final String selection = Data.CONTACT_ID + "=? AND " + Data.MIMETYPE + "=? AND " + Email.TYPE + "=?";
		final String[] projection = new String[] { Data._ID, Data.CONTACT_ID, Email.DATA };
		final Cursor c = resolver.query(Data.CONTENT_URI, projection, selection,
				new String[] { contactId + "", Email.CONTENT_ITEM_TYPE, Email.TYPE_WORK + "" }, null);
		HashMap<String, Integer> mailsForContact = new HashMap<String, Integer>();

		while (c.moveToNext()) {
			mailsForContact.put(c.getString(c.getColumnIndex(Email.DATA)), c.getInt(c.getColumnIndex(Data._ID)));
		}
		c.close();

		// Insert mail addresses
		if (contact.getEmails() != null) {
			for (final String mail : contact.getEmails()) {
				if (mailsForContact.containsKey(mail)) {
					mailsForContact.remove(mail);
				} else {
					Log.d(TAG, "Add mail: " + mail);
					ContentValues cv = new ContentValues();
					cv.put(Email.DATA, mail);
					cv.put(Email.TYPE, Email.TYPE_WORK);
					cv.put(Email.MIMETYPE, Email.CONTENT_ITEM_TYPE);
					ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValue(Data.RAW_CONTACT_ID, contactId).withValues(cv).build());
				}
			}
		}

		// Delete mail addresses
		for (Entry<String, Integer> mail : mailsForContact.entrySet()) {
			Log.d(TAG, "Delete mail: " + mail.getKey());
			ops.add(ContentProviderOperation.newDelete(Data.CONTENT_URI).withSelection(Data._ID + "=?", new String[] { mail.getValue() + "" }).build());
		}
	}

	private static void updatePicture(ContentResolver resolver, Integer contactId, Contact contact, ArrayList<ContentProviderOperation> ops) {
		final String selection = Data.CONTACT_ID + "=? AND " + Data.MIMETYPE + "=?";
		final String[] projection = new String[] { Data._ID, Data.CONTACT_ID, Photo.PHOTO };
		final Cursor c = resolver.query(Data.CONTENT_URI, projection, selection, new String[] { contactId + "", Photo.CONTENT_ITEM_TYPE }, null);

		if (c.moveToFirst()) {
			String id = c.getString(c.getColumnIndex(Data._ID));
			if (contact.getImage() == null) {
				Log.d(TAG, "Delete photo");
				ops.add(ContentProviderOperation.newDelete(Data.CONTENT_URI).withSelection(Data._ID + "=?", new String[] { id }).build());
			} else if (!Arrays.equals(c.getBlob(c.getColumnIndex(Photo.PHOTO)), contact.getImage())) {
				// Update
				Log.d(TAG, "Update photo");
				ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI).withSelection(Data._ID + "=?", new String[] { id }).withValue(Photo.PHOTO,
						contact.getImage()).build());
			}
		} else if (contact.getImage() != null) {
			// Add
			Log.d(TAG, "Add photo");
			ContentValues cv = new ContentValues();
			cv.put(Photo.PHOTO, contact.getImage());
			cv.put(Photo.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
			ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValue(Data.RAW_CONTACT_ID, contactId).withValues(cv).build());
		}
		c.close();
	}

	private static void updateWorkMobileNumber(ContentResolver resolver, Integer contactId, Contact contact, ArrayList<ContentProviderOperation> ops) {
		final String selection = Data.CONTACT_ID + "=? AND " + Data.MIMETYPE + "=? AND " + Phone.TYPE + "=?";
		final String[] projection = new String[] { Data._ID, Data.CONTACT_ID, Phone.NUMBER };
		final Cursor c = resolver.query(Data.CONTENT_URI, projection, selection, new String[] { contactId + "", Phone.CONTENT_ITEM_TYPE,
				Phone.TYPE_WORK_MOBILE + "" }, null);

		if (c.moveToFirst()) {
			String id = c.getString(c.getColumnIndex(Data._ID));
			if (TextUtils.isEmpty(contact.getCellPhone())) {
				Log.d(TAG, "Delete work mobile");
				ops.add(ContentProviderOperation.newDelete(Data.CONTENT_URI).withSelection(Data._ID + "=?", new String[] { id }).build());
			} else if (!contact.getCellPhone().equals(c.getString(c.getColumnIndex(Phone.NUMBER)))) {
				// Update
				Log.d(TAG, "Update work mobile");
				ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI).withSelection(Data._ID + "=?", new String[] { id }).withValue(Phone.NUMBER,
						contact.getCellPhone()).build());
			}
		} else if (!TextUtils.isEmpty(contact.getCellPhone())) {
			// Add
			Log.d(TAG, "Add work mobile");
			ContentValues cv = new ContentValues();
			cv.put(Phone.NUMBER, contact.getCellPhone());
			cv.put(Phone.TYPE, Phone.TYPE_WORK_MOBILE);
			cv.put(Phone.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
			ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValue(Data.RAW_CONTACT_ID, contactId).withValues(cv).build());
		}
		c.close();
	}

	private static void updateWorkNumber(ContentResolver resolver, Integer contactId, Contact contact, ArrayList<ContentProviderOperation> ops) {
		final String selection = Data.CONTACT_ID + "=? AND " + Data.MIMETYPE + "=? AND " + Phone.TYPE + "=?";
		final String[] projection = new String[] { Data._ID, Data.CONTACT_ID, Phone.NUMBER };
		final Cursor c = resolver.query(Data.CONTENT_URI, projection, selection,
				new String[] { contactId + "", Phone.CONTENT_ITEM_TYPE, Phone.TYPE_WORK + "" }, null);

		if (c.moveToFirst()) {
			String id = c.getString(c.getColumnIndex(Data._ID));
			if (TextUtils.isEmpty(contact.getOfficePhone())) {
				Log.d(TAG, "Delete work");
				ops.add(ContentProviderOperation.newDelete(Data.CONTENT_URI).withSelection(Data._ID + "=?", new String[] { id }).build());
			} else if (!contact.getOfficePhone().equals(c.getString(c.getColumnIndex(Phone.NUMBER)))) {
				// Update
				Log.d(TAG, "Update work");
				ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI).withSelection(Data._ID + "=?", new String[] { id }).withValue(Phone.NUMBER,
						contact.getOfficePhone()).build());
			}
		} else if (!TextUtils.isEmpty(contact.getOfficePhone())) {
			// Add
			Log.d(TAG, "Add work");
			ContentValues cv = new ContentValues();
			cv.put(Phone.NUMBER, contact.getOfficePhone());
			cv.put(Phone.TYPE, Phone.TYPE_WORK);
			cv.put(Phone.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
			ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValue(Data.RAW_CONTACT_ID, contactId).withValues(cv).build());
		}
		c.close();
	}

	private static void checkAndUpdateName(ContentResolver resolver, Integer contactId, Contact contact, ArrayList<ContentProviderOperation> ops) {
		final String selection = Data.CONTACT_ID + "=? AND " + Data.MIMETYPE + "=?";
		final String[] projection = new String[] { Data._ID, Data.DATA2, Data.DATA3 };
		final Cursor c = resolver.query(Data.CONTENT_URI, projection, selection, new String[] { contactId + "", StructuredName.CONTENT_ITEM_TYPE }, null);

		if (c.moveToFirst()) {
			if (!(contact.getFirstName().equals(c.getString(c.getColumnIndex(StructuredName.DATA2))) && contact.getLastName().equals(
					c.getString(c.getColumnIndex(StructuredName.DATA3))))) {
				String id = c.getString(c.getColumnIndex(Data._ID));
				// Update name
				ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI).withSelection(Data._ID + "=?", new String[] { id }).withValue(
						StructuredName.DATA2, contact.getFirstName()).withValue(StructuredName.DATA3, contact.getLastName()).build());
			}
		}
		c.close();
	}

	private static void deleteContact(ContentResolver resolver, Integer rawContactId) {
		resolver.delete(RawContacts.CONTENT_URI, RawContacts.CONTACT_ID + "=?", new String[] { "" + rawContactId });
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
		c.close();
		return contactsOnPhone;
	}

	private static void addContact(ContentResolver resolver, String accountName, Contact contact) {
		// Put the data in the contacts provider
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
		int rawContactInsertIndex = ops.size();

		ContentValues cv = new ContentValues();
		cv.put(RawContacts.ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
		cv.put(RawContacts.ACCOUNT_NAME, accountName);
		cv.put(RawContacts.SYNC1, contact.getDN());
		ops.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI).withValues(cv).build());

		// Store name of the account
		cv.clear();
		cv.put(StructuredName.GIVEN_NAME, contact.getFirstName());
		cv.put(StructuredName.FAMILY_NAME, contact.getLastName());
		cv.put(StructuredName.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
		ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex).withValues(cv).build());

		// E-Mail
		if (contact.getEmails() != null) {
			for (String mail : contact.getEmails()) {
				cv.clear();
				cv.put(Email.DATA, mail);
				cv.put(Email.TYPE, Email.TYPE_WORK);
				cv.put(Email.MIMETYPE, Email.CONTENT_ITEM_TYPE);
				ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex).withValues(cv)
						.build());
			}
		}

		// Cellphone
		if (!TextUtils.isEmpty(contact.getCellPhone())) {
			cv.clear();
			cv.put(Phone.NUMBER, contact.getCellPhone());
			cv.put(Phone.TYPE, Phone.TYPE_WORK_MOBILE);
			cv.put(Phone.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
			ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex).withValues(cv)
					.build());
		}

		// Office phone
		if (!TextUtils.isEmpty(contact.getOfficePhone())) {
			cv.clear();
			cv.put(Phone.NUMBER, contact.getOfficePhone());
			cv.put(Phone.TYPE, Phone.TYPE_WORK);
			cv.put(Phone.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
			ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex).withValues(cv)
					.build());
		}

		// Image
		if (contact.getImage() != null) {
			cv.clear();
			cv.put(ContactsContract.CommonDataKinds.Photo.PHOTO, contact.getImage());
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
