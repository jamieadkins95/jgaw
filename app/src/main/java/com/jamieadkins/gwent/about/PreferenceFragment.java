package com.jamieadkins.gwent.about;

import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.jamieadkins.gwent.R;

/**
 * Shows information about the app.
 */

public class PreferenceFragment extends PreferenceFragmentCompat {
    private int layout;

    public static PreferenceFragment newInstance(int layout) {
        PreferenceFragment fragment = new PreferenceFragment();
        fragment.layout = layout;
        return fragment;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource.
        addPreferencesFromResource(layout);
    }
}
