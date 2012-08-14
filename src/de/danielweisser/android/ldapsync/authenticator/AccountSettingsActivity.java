package de.danielweisser.android.ldapsync.authenticator;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;
import de.danielweisser.android.ldapsync.R;

public class AccountSettingsActivity extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_resources);
        // this.getIntent().getExtras()) and the key "account
        
        String preferencesName = getIntent().getExtras().getString("account");
        Log.i("TTTT", preferencesName + " ---------------");
        // set the preferences file name
        getPreferenceManager().setSharedPreferencesName(preferencesName);
    }
}

// TODO Save