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

package de.danielweisser.android.ldapsync.platform;

import java.util.ArrayList;
import java.util.Arrays;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.ContentProviderOperation.Builder;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.text.TextUtils;
import de.danielweisser.android.ldapsync.client.Address;
import de.danielweisser.android.ldapsync.client.Contact;
import de.danielweisser.android.ldapsync.syncadapter.Logger;

/**
 * A helper class that merges the fields of existing contacts with the fields of new contacts.
 * 
 * @author <a href="mailto:daniel.weisser@gmx.de">Daniel Weisser</a>
 */
public class ContactMerger {

	private final long rawContactId;
	private final Contact newC;
	private final Contact existingC;
	private final ArrayList<ContentProviderOperation> ops;
	private final Logger l;

	public ContactMerger(long rawContactId, Contact newContact, Contact existingContact, ArrayList<ContentProviderOperation> ops, Logger l) {
		this.rawContactId = rawContactId;
		this.newC = newContact;
		this.existingC = existingContact;
		this.ops = ops;
		this.l = l;
	}

	public void updateName() {
		if (TextUtils.isEmpty(existingC.getFirstName()) || TextUtils.isEmpty(existingC.getLastName())) {
			l.d("Set name to: " + newC.getFirstName() + " " + newC.getLastName());
			ContentValues cv = new ContentValues();
			cv.put(StructuredName.GIVEN_NAME, newC.getFirstName());
			cv.put(StructuredName.FAMILY_NAME, newC.getLastName());
			cv.put(StructuredName.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
			Builder insertOp = createInsert(rawContactId, cv);
			ops.add(insertOp.build());
		} else if (!newC.getFirstName().equals(existingC.getFirstName()) || !newC.getLastName().equals(existingC.getLastName())) {
			l.d("Update name to: " + newC.getFirstName() + " " + newC.getLastName());
			ContentValues cv = new ContentValues();
			cv.put(StructuredName.GIVEN_NAME, newC.getFirstName());
			cv.put(StructuredName.FAMILY_NAME, newC.getLastName());
			Builder updateOp = ContentProviderOperation.newUpdate(addCallerIsSyncAdapterFlag(Data.CONTENT_URI)).withSelection(
					Data.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE + "=?", new String[] { rawContactId + "", StructuredName.CONTENT_ITEM_TYPE })
					.withValues(cv);
			ops.add(updateOp.build());
		}
	}

	private Builder createInsert(long rawContactId, ContentValues cv) {
		Builder insertOp = ContentProviderOperation.newInsert(addCallerIsSyncAdapterFlag(Data.CONTENT_URI)).withValues(cv);
		if (rawContactId == -1) {
			insertOp.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0);
		} else {
			insertOp.withValue(Data.RAW_CONTACT_ID, rawContactId);
		}
		return insertOp;
	}

	private Uri addCallerIsSyncAdapterFlag(Uri uri) {
		Uri.Builder b = uri.buildUpon();
		b.appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true");
		return b.build();
	}

	public void updateMail(int mailType) {
		String newMail = null;
		String existingMail = null;
		if (mailType == Email.TYPE_WORK) {
			if (newC.getEmails() != null && newC.getEmails().length > 0) {
				newMail = newC.getEmails()[0];
			}
			if (existingC.getEmails() != null && existingC.getEmails().length > 0) {
				existingMail = existingC.getEmails()[0];
			}
		}
		updateMail(newMail, existingMail, mailType);
	}

	private void updateMail(String newMail, String existingMail, int mailType) {
		String selection = Data.RAW_CONTACT_ID + "=? AND " + Email.MIMETYPE + "=? AND " + Email.TYPE + "=?";
		if (TextUtils.isEmpty(newMail) && !TextUtils.isEmpty(existingMail)) {
			l.d("Delete mail data " + mailType + " (" + existingMail + ")");
			ops.add(ContentProviderOperation.newDelete(addCallerIsSyncAdapterFlag(Data.CONTENT_URI)).withSelection(selection,
					new String[] { rawContactId + "", Email.CONTENT_ITEM_TYPE, mailType + "" }).build());
		} else if (!TextUtils.isEmpty(newMail) && TextUtils.isEmpty(existingMail)) {
			l.d("Add mail data " + mailType + " (" + newMail + ")");
			ContentValues cv = new ContentValues();
			cv.put(Email.DATA, newMail);
			cv.put(Email.TYPE, mailType);
			cv.put(Email.MIMETYPE, Email.CONTENT_ITEM_TYPE);
			Builder insertOp = createInsert(rawContactId, cv);
			ops.add(insertOp.build());
		} else if (!TextUtils.isEmpty(newMail) && !newMail.equals(existingMail)) {
			l.d("Update mail data " + mailType + " (" + existingMail + " => " + newMail + ")");
			Builder updateOp = ContentProviderOperation.newUpdate(addCallerIsSyncAdapterFlag(Data.CONTENT_URI)).withSelection(selection,
					new String[] { rawContactId + "", Email.CONTENT_ITEM_TYPE, mailType + "" }).withValue(Email.DATA, newMail);
			ops.add(updateOp.build());
		}
	}

	public void updatePhone(int phoneType) {
		String newPhone = null;
		String existingPhone = null;
		if (phoneType == Phone.TYPE_WORK_MOBILE) {
			newPhone = newC.getCellWorkPhone();
			existingPhone = existingC.getCellWorkPhone();
		} else if (phoneType == Phone.TYPE_WORK) {
			newPhone = newC.getWorkPhone();
			existingPhone = existingC.getWorkPhone();
		} else if (phoneType == Phone.TYPE_HOME) {
			newPhone = newC.getHomePhone();
			existingPhone = existingC.getHomePhone();
		}
		updatePhone(newPhone, existingPhone, phoneType);
	}

	private void updatePhone(String newPhone, String existingPhone, int phoneType) {
		String selection = Data.RAW_CONTACT_ID + "=? AND " + Phone.MIMETYPE + "=? AND " + Phone.TYPE + "=?";
		if (TextUtils.isEmpty(newPhone) && !TextUtils.isEmpty(existingPhone)) {
			l.d("Delete phone data " + phoneType + " (" + existingPhone + ")");
			ops.add(ContentProviderOperation.newDelete(addCallerIsSyncAdapterFlag(Data.CONTENT_URI)).withSelection(selection,
					new String[] { rawContactId + "", Phone.CONTENT_ITEM_TYPE, phoneType + "" }).build());
		} else if (!TextUtils.isEmpty(newPhone) && TextUtils.isEmpty(existingPhone)) {
			l.d("Add phone data " + phoneType + " (" + newPhone + ")");
			ContentValues cv = new ContentValues();
			cv.put(Phone.DATA, newPhone);
			cv.put(Phone.TYPE, phoneType);
			cv.put(Phone.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
			Builder insertOp = createInsert(rawContactId, cv);
			ops.add(insertOp.build());
		} else if (!TextUtils.isEmpty(newPhone) && !newPhone.equals(existingPhone)) {
			l.d("Update phone data " + phoneType + " (" + existingPhone + " => " + newPhone + ")");
			Builder updateOp = ContentProviderOperation.newUpdate(addCallerIsSyncAdapterFlag(Data.CONTENT_URI)).withSelection(selection,
					new String[] { rawContactId + "", Phone.CONTENT_ITEM_TYPE, phoneType + "" }).withValue(Phone.DATA, newPhone);
			ops.add(updateOp.build());
		}
	}

	public void updatePicture() {
		String selection = Data.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE + "=?";
		if (newC.getImage() == null && existingC.getImage() != null) {
			l.d("Delete image");
			ops.add(ContentProviderOperation.newDelete(addCallerIsSyncAdapterFlag(Data.CONTENT_URI)).withSelection(selection,
					new String[] { rawContactId + "", Photo.CONTENT_ITEM_TYPE }).build());
		} else if (newC.getImage() != null && existingC.getImage() == null) {
			l.d("Add image");
			ContentValues cv = new ContentValues();
			cv.put(Photo.PHOTO, newC.getImage());
			cv.put(Photo.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
			Builder insertOp = createInsert(rawContactId, cv);
			ops.add(insertOp.build());
		} else if (!Arrays.equals(newC.getImage(), existingC.getImage())) {
			l.d("Update image");
			Builder updateOp = ContentProviderOperation.newUpdate(addCallerIsSyncAdapterFlag(Data.CONTENT_URI)).withSelection(selection,
					new String[] { rawContactId + "", Photo.CONTENT_ITEM_TYPE }).withValue(Photo.PHOTO, newC.getImage());
			ops.add(updateOp.build());
		}
	}

	public void updateAddress(int adressType) {
		if (adressType == StructuredPostal.TYPE_WORK) {
			updateAddress(newC.getAddress(), existingC.getAddress(), adressType);
		}
	}

	private void updateAddress(de.danielweisser.android.ldapsync.client.Address newAddress, Address existingAddress, int adressType) {
		final String selection = Data.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE + "=? AND " + StructuredPostal.TYPE + "=?";
		if ((newAddress == null || newAddress.isEmpty()) && existingAddress != null) {
			l.d("Delete address " + adressType + "(" + existingC.getFirstName() + " " + existingC.getLastName() + ")");
			ops.add(ContentProviderOperation.newDelete(addCallerIsSyncAdapterFlag(Data.CONTENT_URI)).withSelection(selection,
					new String[] { rawContactId + "", StructuredPostal.CONTENT_ITEM_TYPE, adressType + "" }).build());
		} else if (existingAddress == null && newAddress != null && !newAddress.isEmpty()) {
			l.d("Add address " + adressType + "(" + existingC.getFirstName() + " " + existingC.getLastName() + ")");
			ContentValues cv = new ContentValues();
			cv.put(StructuredPostal.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE);
			cv.put(StructuredPostal.TYPE, adressType);
			cv.put(StructuredPostal.STREET, newAddress.getStreet());
			cv.put(StructuredPostal.CITY, newAddress.getCity());
			cv.put(StructuredPostal.COUNTRY, newAddress.getCountry());
			cv.put(StructuredPostal.POSTCODE, newAddress.getZip());
			cv.put(StructuredPostal.REGION, newAddress.getState());
			Builder insertOp = createInsert(rawContactId, cv);
			ops.add(insertOp.build());
		} else if (newAddress != null && !newAddress.isEmpty() && !newAddress.equals(existingAddress)) {
			l.d("Update address " + adressType + "(" + existingC.getFirstName() + " " + existingC.getLastName() + ")");
			ContentValues cv = new ContentValues();
			cv.put(StructuredPostal.STREET, newAddress.getStreet());
			cv.put(StructuredPostal.CITY, newAddress.getCity());
			cv.put(StructuredPostal.COUNTRY, newAddress.getCountry());
			cv.put(StructuredPostal.POSTCODE, newAddress.getZip());
			cv.put(StructuredPostal.REGION, newAddress.getState());
			Builder updateOp = ContentProviderOperation.newUpdate(addCallerIsSyncAdapterFlag(Data.CONTENT_URI)).withSelection(selection,
					new String[] { rawContactId + "", StructuredPostal.CONTENT_ITEM_TYPE, adressType + "" }).withValues(cv);
			ops.add(updateOp.build());
		}
	}
}
