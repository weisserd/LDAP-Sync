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

package de.wikilab.android.ldapsync.client;

import java.io.ByteArrayOutputStream;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.unboundid.ldap.sdk.ReadOnlyEntry;

/**
 * Represents a LDAPSyncAdapter contact.
 * 
 * @author <a href="mailto:daniel.weisser@gmx.de">Daniel Weisser</a>
 */
public class Contact {

	private String dn = "";
	private String firstName = "";
	private String lastName = "";
	private String cellWorkPhone = "";
	private String workPhone = "";
	private String homePhone = "";
	private String[] emails = null;
	private byte[] image = null;
	private Address address = null;

	public String getDn() {
		return dn;
	}

	public void setDn(String dn) {
		this.dn = dn;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public void setCellWorkPhone(String cellWorkPhone) {
		this.cellWorkPhone = cellWorkPhone;
	}

	public String getCellWorkPhone() {
		return cellWorkPhone;
	}

	public String getWorkPhone() {
		return workPhone;
	}

	public void setWorkPhone(String workPhone) {
		this.workPhone = workPhone;
	}

	public void setHomePhone(String homePhone) {
		this.homePhone = homePhone;
	}

	public String getHomePhone() {
		return homePhone;
	}

	public String[] getEmails() {
		return emails;
	}

	public void setEmails(String[] emails) {
		this.emails = emails;
	}

	public byte[] getImage() {
		return image;
	}

	public void setImage(byte[] image) {
		this.image = image;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	public Address getAddress() {
		return address;
	}

	/**
	 * Creates and returns an instance of the user from the provided LDAP data.
	 * 
	 * @param user
	 *            The LDAPObject containing user data
	 * @param preferences
	 *            Mapping bundle for the LDAP attribute names.
	 * @return user The new instance of LDAP user created from the LDAP data.
	 */
	public static Contact valueOf(ReadOnlyEntry user, SharedPreferences preferences) {
		Contact c = new Contact();
		try {
			c.setDn(user.getDN());
			c.setFirstName(getAttributevalue(user, preferences, "first_name"));
			c.setLastName(getAttributevalue(user, preferences, "last_name"));
			if (getAttributevalue(user, preferences, "last_name") == null || getAttributevalue(user, preferences, "first_name") == null) {
				return null;
			}
			c.setWorkPhone(getAttributevalue(user, preferences, "office_phone"));
			c.setCellWorkPhone(getAttributevalue(user, preferences, "cell_phone"));
			c.setHomePhone(getAttributevalue(user, preferences, "home_phone"));
			c.setEmails(
                    user.hasAttribute(preferences.getString("email", ""))
                            ? user.getAttributeValues(preferences.getString("email", ""))
                            : null);

			byte[] image = null;
			if (user.hasAttribute(preferences.getString("photo", ""))) {
				byte[] array = user.getAttributeValueBytes(preferences.getString("photo", ""));

				try {
					Bitmap myBitmap = BitmapFactory.decodeByteArray(array, 0, array.length);
					if (myBitmap != null) {
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						myBitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
						image = baos.toByteArray();
					}
				} catch (OutOfMemoryError e) {
					// Do not set an image, when an OutOfMemoryError occurs
					image = null;
					array = null;
				}
			}
			c.setImage(image);
			// Get address
			if (user.hasAttribute(preferences.getString("street", "")) || user.hasAttribute(preferences.getString("city", ""))
					|| user.hasAttribute(preferences.getString("state", "")) || user.hasAttribute(preferences.getString("postalCode", ""))
					|| user.hasAttribute(preferences.getString("country", ""))) {
				Address a = new Address();
				a.setStreet(getAttributevalue(user, preferences, "street"));
				a.setCity(getAttributevalue(user, preferences, "city"));
				a.setState(getAttributevalue(user, preferences, "state"));
				a.setZip(getAttributevalue(user, preferences, "postalCode"));
				a.setCountry(getAttributevalue(user, preferences, "country"));
				c.setAddress(a);
			}
		} catch (final Exception ex) {
			Log.i("User", "Error parsing LDAP user object" + ex.toString());
		}
		return c;
	}

	private static String getAttributevalue(ReadOnlyEntry user, SharedPreferences preferences, String field) {
		return user.hasAttribute(preferences.getString(field, "")) ? user.getAttributeValue(preferences.getString(field, "")) : null;
	}
}
