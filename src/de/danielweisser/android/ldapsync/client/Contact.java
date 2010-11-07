package de.danielweisser.android.ldapsync.client;

import java.io.ByteArrayOutputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;

import com.unboundid.ldap.sdk.ReadOnlyEntry;

/**
 * Represents a LDAPSyncAdapter contact
 */
public class Contact {
	public static String FIRSTNAME = "FIRSTNAME";
	public static String LASTNAME = "LASTNAME";
	public static String TELEPHONE = "TELEPHONE";
	public static String MOBILE = "MOBILE";
	public static String HOMEPHONE = "HOMEPHONE";
	public static String MAIL = "MAIL";
	public static String PHOTO = "PHOTO";

	private String dn = "";
	private String firstName = "";
	private String lastName = "";
	private String cellWorkPhone = "";
	private String workPhone = "";
	private String homePhone = "";
	private String[] emails = null;
	private byte[] image = null;

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

	/**
	 * Creates and returns an instance of the user from the provided LDAP data.
	 * 
	 * @param user
	 *            The LDAPObject containing user data
	 * @param mB
	 * @return user The new instance of LDAP user created from the LDAP data.
	 */
	public static Contact valueOf(ReadOnlyEntry user, Bundle mB) {
		Contact c = new Contact();
		try {
			c.setDn(user.getDN());
			c.setFirstName(user.hasAttribute(mB.getString(FIRSTNAME)) ? user.getAttributeValue(mB.getString(FIRSTNAME)) : null);
			c.setLastName(user.hasAttribute(mB.getString(LASTNAME)) ? user.getAttributeValue(mB.getString(LASTNAME)) : null);
			if ((user.hasAttribute(mB.getString(FIRSTNAME)) ? user.getAttributeValue(mB.getString(FIRSTNAME)) : null) == null
					|| (user.hasAttribute(mB.getString(LASTNAME)) ? user.getAttributeValue(mB.getString(LASTNAME)) : null) == null) {
				return null;
			}
			c.setWorkPhone(user.hasAttribute(mB.getString(TELEPHONE)) ? user.getAttributeValue(mB.getString(TELEPHONE)) : null);
			c.setCellWorkPhone(user.hasAttribute(mB.getString(MOBILE)) ? user.getAttributeValue(mB.getString(MOBILE)) : null);
			c.setHomePhone(user.hasAttribute(mB.getString(HOMEPHONE)) ? user.getAttributeValue(mB.getString(HOMEPHONE)) : null);
			c.setEmails(user.hasAttribute(mB.getString(MAIL)) ? user.getAttributeValues(mB.getString(MAIL)) : null);
			byte[] image = null;
			if (user.hasAttribute(mB.getString(PHOTO))) {
				byte[] array = user.getAttributeValueBytes(mB.getString(PHOTO));

				Bitmap myBitmap = BitmapFactory.decodeByteArray(array, 0, array.length);
				if (myBitmap != null) {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					myBitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
					image = baos.toByteArray();
				}
			}
			c.setImage(image);
		} catch (final Exception ex) {
			Log.i("User", "Error parsing LDAP user object" + ex.toString());
		}
		return c;
	}
}
