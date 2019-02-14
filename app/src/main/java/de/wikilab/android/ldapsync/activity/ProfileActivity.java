package de.wikilab.android.ldapsync.activity;

//import de.wikilab.android.ldapsync.client.Organization;

/*
public class ProfileActivity extends Activity implements OnClickListener {

	final String TAG = "Profile";
	
	private Contact currentContact = null;
	
	private TextView mDisplayName;
	private TextView mPhone;
	private TextView mGeneral;
	private TextView mAddr;
	private TextView mStaffInfo;
	
	private Button mButtonWebview;
	private Button mButtonAddcontact;

	private static final int DIALOG_RESYNC = 1;
	
	@Override
	protected void onStart() {
		Log.i(TAG, "Started the profile activity: " + getIntent());
		super.onStart();
		
		Intent intent = getIntent();
		
		if(intent != null && intent.getAction().equals(Intent.ACTION_VIEW) && intent.getData() != null) {
			Cursor cursor = managedQuery(getIntent().getData(), null, null, null, null);
			if(cursor.moveToNext()) {
				Log.i(TAG, "DATA1: " + cursor.getString(cursor.getColumnIndex("DATA1")));
				Log.i(TAG, "DATA2: " + cursor.getString(cursor.getColumnIndex("DATA2")));
				Log.i(TAG, "DATA3: " + cursor.getString(cursor.getColumnIndex("DATA3")));
				Log.i(TAG, "DATA4: " + cursor.getString(cursor.getColumnIndex("DATA4")));
				Logger l = new Logger();
				ContactManager cm = new ContactManager();
				//String ufid = cursor.getString(cursor.getColumnIndex("DATA4"));
				
				String dn = cursor.getString(cursor.getColumnIndex("DATA1"));
				//currentContact = cm.getContactByDn(getApplicationContext(), Constants.ACCOUNT_NAME, dn);
				currentContact.setDn(dn);
				//currentContact.setUfid(ufid);
				mButtonAddcontact.setVisibility(View.INVISIBLE);
			}
			cursor.close();
		}
		else if(intent != null && intent.getAction().equals(Intent.ACTION_VIEW) && intent.getSerializableExtra(Constants.CUSTOM_CONTACT_DATA) != null) {
			currentContact = (Contact) intent.getSerializableExtra(Constants.CUSTOM_CONTACT_DATA);
			mButtonAddcontact.setVisibility(View.VISIBLE);
		}
		
		if(currentContact != null) {
			Log.i(TAG, "Contact found for profile activity: " + currentContact);

			// these aren't in the regular phone structured data

			//Address waddr = currentContact.getWorkAddress();
			
			//map everything from contact c into textviews
			//mDisplayName.setText(currentContact.getDisplayName());
			
			String general = "";
			if(currentContact.getEmails() != null) {
				for(String email : currentContact.getEmails())
					general += "Email: " + email + "\n";
			}

			//Organization org = currentContact.getWorkOrganization();
			//if(org != null && org.getPrimaryAffiliation() != null && !org.getPrimaryAffiliation().equals(""))
			//	general += "Affiliation: " + org.getPrimaryAffiliation() + "\n";
			
			
			mGeneral.setText(general);
			Linkify.addLinks(mGeneral, Linkify.EMAIL_ADDRESSES);
			
			
			String phone = "";
			if(currentContact.getCellWorkPhone() != null && !currentContact.getCellWorkPhone().equals("")) phone += "Work cell: " + currentContact.getCellWorkPhone() + "\n";
			if(currentContact.getHomePhone() != null && !currentContact.getHomePhone().equals("")) phone += "Home phone: " + currentContact.getHomePhone() + "\n";
			if(currentContact.getWorkPhone() != null && !currentContact.getWorkPhone().equals("")) phone += "Work phone: " + currentContact.getWorkPhone() + "\n";
			mPhone.setText(phone);
			Linkify.addLinks(mPhone, Linkify.PHONE_NUMBERS);

			String addr = "";
			//if(waddr != null && !waddr.toFancyString().equals(""))
			//	addr += "Preferred address: " + waddr.toFancyString() + "\n";
			//if(org != null && org.getOfficeLocation() != null && !org.getOfficeLocation().equals(""))
			//	addr += (!addr.equals("") ? "\n" : "") + "Office Location: " + org.getOfficeLocation() + "\n";
			mAddr.setText(addr);
			Linkify.addLinks(mAddr, Linkify.MAP_ADDRESSES);
			
			String staffInfo = "";
			//if(org != null && org.getCompany() != null && !org.getCompany().equals(""))
			//	staffInfo += "Unit: " + org.getCompany() + "\n";
			//if(org != null && org.getTitle() != null && !org.getTitle().equals(""))
			//	staffInfo += "Title: " + org.getTitle() + "\n";
			mStaffInfo.setText(staffInfo);
		}
		else {
			Log.w(TAG, "Contact NOT found for profile activity");
			showDialog(DIALOG_RESYNC);
		}
	}
	
	
	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
	    switch(id) {
	    case DIALOG_RESYNC:
	    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    	builder.setMessage("Profile not found. Please wait for another sync if this is an existing contact.\n\nIf this problem persists, remove and re-add the UF Phonebook Sync account, or remove and re-add the whole application (no data loss).")
	    	       .setCancelable(true)
	    	       .setNegativeButton("OK", new DialogInterface.OnClickListener() {
	    	           public void onClick(DialogInterface dialog, int id) {
	    	                dialog.cancel();
	    	                finish();
	    	           }
	    	       });
	    	dialog = builder.create();
	        break;
	    default:
	        dialog = null;
	    }
	    return dialog;
	}

	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "Created the profile activity");
		super.onCreate(savedInstanceState);
		
	    setContentView(R.layout.fullcontactview);
	    
	    mDisplayName = (TextView) findViewById(R.id.profile_text_name);
	    
	    mGeneral = (TextView) findViewById(R.id.profile_text_general);
	    mPhone = (TextView) findViewById(R.id.profile_text_phone);
	    mAddr = (TextView) findViewById(R.id.profile_text_addresses);
	    mStaffInfo = (TextView) findViewById(R.id.profile_text_staffinfo);
	    
	    mButtonWebview = (Button)findViewById(R.id.profile_button_webview);
	    mButtonWebview.setOnClickListener(this);
	    
	    mButtonAddcontact = (Button)findViewById(R.id.profile_button_addcontact);
	    mButtonAddcontact.setOnClickListener(this);
	}
	
	@Override
	protected void onResume() {
		Log.i(TAG, "Resumed the profile activity");
		super.onResume();
	}

	final private long UFID_MASK = 56347812;

	
	public String convertUFIDToTag(String ufid) {
		
			long lUfid = Long.valueOf(ufid);
		
			String filter = "TSJWHEVN";
			
			String encoded = String.format("%09o", lUfid ^ UFID_MASK);

			String result = "";
			for(int i = 0; i < encoded.length(); i++) {
				char c = encoded.charAt(i);
				int v = Integer.valueOf(""+c);
				result += filter.charAt(v);
			}
			
			return result;
	}

	public void onClick(View v) {
		Log.i(TAG, "Button click received");
		if(currentContact == null || currentContact.getUfid() == null || currentContact.getUfid().equals(""))
			return;
		
		if(v.getId() == R.id.profile_button_webview) {
			Log.i(TAG, "WebView starting");
			String ufid = currentContact.getUfid();
			String tag = convertUFIDToTag(ufid);
			if(tag != null && !tag.equals("")) {
				String url = "http://phonebook.ufl.edu/people/" + tag + "/";
				
				Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				startActivity(myIntent);
			}
		}
		else if(v.getId() == R.id.profile_button_addcontact) {
			Log.i(TAG, "AddContact staring");
			Intent in = new Intent(Insert.ACTION, ContactsContract.Contacts.CONTENT_URI);
			
			in.putExtra(Insert.NAME, currentContact.getDisplayName());
			if(currentContact.getEmails() != null) {
				
				if(currentContact.getEmails().length > 0) {
					in.putExtra(Insert.EMAIL, currentContact.getEmails()[0]);
				    in.putExtra(Insert.EMAIL_TYPE, CommonDataKinds.Email.TYPE_WORK);
				}
				if(currentContact.getEmails().length > 1) {
					in.putExtra(Insert.SECONDARY_EMAIL, currentContact.getEmails()[1]);
				    in.putExtra(Insert.SECONDARY_EMAIL_TYPE, CommonDataKinds.Email.TYPE_WORK);
				}
				if(currentContact.getEmails().length > 2) {
					in.putExtra(Insert.TERTIARY_EMAIL, currentContact.getEmails()[2]);
				    in.putExtra(Insert.TERTIARY_EMAIL_TYPE, CommonDataKinds.Email.TYPE_WORK);
				}
			}
		    
		    startActivity(in);
		}
	}
	
}
*/