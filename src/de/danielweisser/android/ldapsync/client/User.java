package de.danielweisser.android.ldapsync.client;

import java.io.ByteArrayOutputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.unboundid.ldap.sdk.ReadOnlyEntry;

/**
 * Represents a sample LDAPSyncAdapter user
 */
public class User {
	private final String mFirstName;
	private final String mLastName;
	private final String mCellPhone;
	private final String mOfficePhone;
	private final String mEmail;
	private final byte[] mImage;

	public String getFirstName() {
		return mFirstName;
	}

	public String getLastName() {
		return mLastName;
	}

	public String getCellPhone() {
		return mCellPhone;
	}

	public String getOfficePhone() {
		return mOfficePhone;
	}

	public String getEmail() {
		return mEmail;
	}

	public byte[] getImage() {
		return mImage;
	}

	public User(String firstName, String lastName, String cellPhone, String officePhone, String email, byte[] image) {
		mFirstName = firstName;
		mLastName = lastName;
		mCellPhone = cellPhone;
		mOfficePhone = officePhone;
		mEmail = email;
		mImage = image;
	}

	/**
	 * Creates and returns an instance of the user from the provided LDAP data.
	 * 
	 * @param user
	 *            The LDAPObject containing user data
	 * @return user The new instance of LDAP user created from the LDAP data.
	 */
	public static User valueOf(ReadOnlyEntry user) {
		try {
			final String firstName = user.hasAttribute("givenname") ? user.getAttributeValue("givenname") : null;
			final String lastName = user.hasAttribute("sn") ? user.getAttributeValue("sn") : null;
			final String officePhone = user.hasAttribute("telephonenumber") ? user.getAttributeValue("telephonenumber")
					: null;
			final String cellPhone = user.hasAttribute("mobile") ? user.getAttributeValue("mobile") : null;
			final String email = user.hasAttribute("mail") ? user.getAttributeValue("mail") : null;
			byte[] image = null;
			if (user.hasAttribute("thumbnailphoto")) {
				byte[] array = user.getAttributeValueBytes("thumbnailphoto");

				Bitmap myBitmap = BitmapFactory.decodeByteArray(array, 0, array.length);
				if (myBitmap != null) {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					myBitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
					image = baos.toByteArray();
				}
			}
			return new User(firstName, lastName, cellPhone, officePhone, email, image);
		} catch (final Exception ex) {
			Log.i("User", "Error parsing LDAP user object" + ex.toString());
		}
		return null;
	}

}
