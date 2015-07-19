package uk.co.jaymehta.csafeedback;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by Jay on 19/07/2015.
 */
public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.prefs);
    }
}