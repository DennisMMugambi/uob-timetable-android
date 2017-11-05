package com.ak.uobtimetable.Fragments;


import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.ak.uobtimetable.R;
import com.ak.uobtimetable.Utilities.Logging.Logger;
import com.ak.uobtimetable.Utilities.SettingsManager;

public class PreferencesFragment extends PreferenceFragment {

    CheckBoxPreference cbLongRoomNames;
    CheckBoxPreference cbRefreshWiFi;
    CheckBoxPreference cbRefreshCellular;

    SettingsManager settings;

    private enum settingsList {
        longRoomNames,
        refreshWiFi,
        refreshCellular
    }

    public PreferencesFragment() {

        settings = SettingsManager.getInstance(getActivity());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        // We've already abstracted the preferences that we want to save in SettingsManager, so
        // we'll manually get and save the preference values instead of using the built in
        // auto-binding magic.

        cbLongRoomNames = (CheckBoxPreference)findPreference(settingsList.longRoomNames.name());
        cbRefreshWiFi = (CheckBoxPreference)findPreference(settingsList.refreshWiFi.name());
        cbRefreshCellular = (CheckBoxPreference)findPreference(settingsList.refreshCellular.name());

        // Set values
        cbLongRoomNames.setChecked(settings.getLongRoomNames());
        cbRefreshWiFi.setChecked(settings.getRefreshWiFi());
        cbRefreshCellular.setChecked(settings.getRefreshCellular());

        // Set listeners
        Preference.OnPreferenceChangeListener listener = new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {

                CheckBoxPreference pref = (CheckBoxPreference) preference;

                String key = pref.getKey();
                boolean value = (boolean)o;

                Logger.getInstance()
                    .debug("PreferenceFragment", "Changed: " + key)
                    .debug("PreferenceFragment", "Value: " + value);

                if (key.equals(settingsList.longRoomNames.name()))
                    settings.setLongRoomNames(value);
                else if (key.equals(settingsList.refreshWiFi.name()))
                    settings.setRefreshWiFi(value);
                else if (key.equals(settingsList.refreshCellular.name()))
                    settings.setRefreshCellular(value);

                return true;
            }
        };

        cbLongRoomNames.setOnPreferenceChangeListener(listener);
        cbRefreshWiFi.setOnPreferenceChangeListener(listener);
        cbRefreshCellular.setOnPreferenceChangeListener(listener);
    }
}