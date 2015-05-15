package ru.terra.tproxy;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Date: 03.04.15
 * Time: 16:56
 */
public class PrefsActivity extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

    }
}