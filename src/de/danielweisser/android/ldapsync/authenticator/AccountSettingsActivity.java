package de.danielweisser.android.ldapsync.authenticator;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import de.danielweisser.android.ldapsync.R;

public class AccountSettingsActivity extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_resources);
    }
}