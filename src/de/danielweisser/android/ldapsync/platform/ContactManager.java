package de.danielweisser.android.ldapsync.platform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
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
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.CommonDataKinds.Website;
import android.util.Log;
import de.danielweisser.android.ldapsync.Constants;
import de.danielweisser.android.ldapsync.client.Contact;
import de.danielweisser.android.ldapsync.syncadapter.Logger;

/**
 * Class for managing contacts sync related operations
 */
public class ContactManager {
	private static final String TAG = "ContactManager";
	private Logger l;

	public ContactManager(Logger l) {
		this.l = l;
	}

	/**
	 * Synchronize raw contacts
	 * 
	 * @param context
	 *            The context of Authenticator Activity
	 * @param accountName
	 *            The account name
	 * @param contacts
	 *            The list of retrieved LDAP contacts
	 * @param syncResult
	 *            SyncResults for tracking the sync
	 */
	public synchronized void syncContacts(Context context, String accountName, List<Contact> contacts, SyncResult syncResult) {
		final ContentResolver resolver = context.getContentResolver();

		// Get all phone contacts for the LDAP account
		HashMap<String, Integer> contactsOnPhone = getAllContactsOnPhone(resolver, accountName);

		// Update and create new contacts
		for (final Contact contact : contacts) {
			if (contactsOnPhone.containsKey(contact.getDn())) {
				Integer contactId = contactsOnPhone.get(contact.getDn());
				Log.d(TAG, "Update contact: " + contact.getDn());
				l.d("Update contact: " + contact.getDn() + " " + contact.getFirstName() + " " + contact.getLastName() + " (" + contactId + ")");
				updateContact(resolver, contactId, contact);
				syncResult.stats.numUpdates++;
				contactsOnPhone.remove(contact.getDn());
			} else {
				Log.d(TAG, "Add contact: " + contact.getFirstName() + " " + contact.getLastName());
				l.d("Add contact: " + contact.getFirstName() + " " + contact.getLastName());
				addContact(resolver, accountName, contact);
				syncResult.stats.numInserts++;
			}
		}

		// Delete contacts
		for (Entry<String, Integer> contact : contactsOnPhone.entrySet()) {
			Log.d(TAG, "Delete contact: " + contact.getKey());
			deleteContact(resolver, contact.getValue());
			l.d("Delete contact: " + contact.getKey() + "(" + contact.getValue() + ")");
			syncResult.stats.numDeletes++;
		}
	}

	
	private void updateContact(ContentResolver resolver, long rawContactId, Contact contact) {
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

		Contact existingContact = new Contact();

		final String selection = Data.RAW_CONTACT_ID + "=?";
		final String[] projection = new String[] { Data.MIMETYPE, Data.DATA1, Data.DATA2, Data.DATA3, Data.DATA4, Data.DATA7, Data.DATA8, Data.DATA9,
				Data.DATA10, Data.DATA15 };
		final Cursor c = resolver.query(Data.CONTENT_URI, projection, selection, new String[] { rawContactId + "" }, null);

		if (c != null) {
			while (c.moveToNext()) {
				String mimetype = c.getString(c.getColumnIndex(Data.MIMETYPE));
				if (mimetype.equals(StructuredName.CONTENT_ITEM_TYPE)) {
					existingContact.setFirstName(c.getString(c.getColumnIndex(Data.DATA2)));
					existingContact.setLastName(c.getString(c.getColumnIndex(Data.DATA3)));
				} else if (mimetype.equals(Email.CONTENT_ITEM_TYPE)) {
					// TODO Mails
//					int type = c.getInt(c.getColumnIndex(Data.DATA2));
//					if (type == Email.TYPE_HOME) {
//						existingContact.setHomeEmail(c.getString(c.getColumnIndex(Data.DATA1)));
//					} else if (type == Email.TYPE_WORK) {
//						existingContact.setWorkEmail(c.getString(c.getColumnIndex(Data.DATA1)));
//					}
				} else if (mimetype.equals(Phone.CONTENT_ITEM_TYPE)) {
					int type = c.getInt(c.getColumnIndex(Data.DATA2));
					if (type == Phone.TYPE_WORK_MOBILE) {
						existingContact.setCellWorkPhone(c.getString(c.getColumnIndex(Data.DATA1)));
					} else if (type == Phone.TYPE_WORK) {
						existingContact.setWorkPhone(c.getString(c.getColumnIndex(Data.DATA1)));
					}
				} else if (mimetype.equals(Photo.CONTENT_ITEM_TYPE)) {
					existingContact.setImage(c.getBlob(c.getColumnIndex(Photo.PHOTO)));
				}
			}
		}

		prepareFields(rawContactId, contact, existingContact, ops, false);

		try {
			if (ops.size() > 0) {
				resolver.applyBatch(ContactsContract.AUTHORITY, ops);
			}
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
	
	private Uri addCallerIsSyncAdapterFlag(Uri uri) {
		Uri.Builder b = uri.buildUpon();
		b.appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true");
		return b.build();
	}
	
	/**
	 * Add a new contact to the RawContacts table.
	 * 
	 * @param resolver
	 * @param accountName
	 * @param contact
	 */
	private void addContact(ContentResolver resolver, String accountName, Contact contact) {
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

		Uri uri = addCallerIsSyncAdapterFlag(RawContacts.CONTENT_URI);

		ContentValues cv = new ContentValues();
		cv.put(RawContacts.ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
		cv.put(RawContacts.ACCOUNT_NAME, accountName);
		cv.put(RawContacts.SOURCE_ID, contact.getDn());

		// This is the first insert into the raw contacts table
		ContentProviderOperation i1 = ContentProviderOperation.newInsert(uri).withValues(cv).build();
		ops.add(i1);

		prepareFields(-1, contact, new Contact(), ops, true);

		// Now create the contact with a single batch operation
		try {
			ContentProviderResult[] res = resolver.applyBatch(ContactsContract.AUTHORITY, ops);
			// The first insert is the one generating the ID for this contact
			long id = ContentUris.parseId(res[0].uri);
			l.d("The new contact has id: " + id);
		} catch (Exception e) {
			Log.e(TAG, "Cannot create contact ", e);
		}
	}
	
	private void prepareFields(long rawContactId, Contact newC, Contact existingC, ArrayList<ContentProviderOperation> ops, boolean isNew) {
		ContactMerger contactMerger = new ContactMerger(rawContactId, newC, existingC, ops, l);
		contactMerger.updateName();
//		contactMerger.updateMail(Email.TYPE_WORK);
//		contactMerger.updateMail(Email.TYPE_HOME);

		contactMerger.updatePhone(Phone.TYPE_WORK_MOBILE);
		contactMerger.updatePhone(Phone.TYPE_WORK);

//		contactMerger.updateURL(Website.TYPE_HOME);
//		contactMerger.updateURL(Website.TYPE_WORK);
//		contactMerger.updateURL(Website.TYPE_PROFILE);

		contactMerger.updatePicture();
//		contactMerger.updateCompanyInformation();

//		contactMerger.updateAddress(StructuredPostal.TYPE_WORK);
//		contactMerger.updateAddress(StructuredPostal.TYPE_HOME);
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
