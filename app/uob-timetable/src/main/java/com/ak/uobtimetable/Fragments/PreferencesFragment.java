package com.ak.uobtimetable.Fragments;


import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.ak.uobtimetable.R;
import com.ak.uobtimetable.Utilities.Logging.Logger;
import com.ak.uobtimetable.Notifications.SessionReminderNotifier;
import com.ak.uobtimetable.Utilities.SettingsManager;

public class PreferencesFragment extends PreferenceFragment {

    CheckBoxPreference cbNotifySessionRemindersEnable;
    ListPreference ltNotifySessionRemindersMinutes;
    CheckBoxPreference cbLongRoomNames;
    CheckBoxPreference cbRefreshWiFi;
    CheckBoxPreference cbRefreshCellular;

    SettingsManager settings;

    private enum settingsList {
        notifySessionRemindersEnabled,
        notifySessionRemindersMinutes,
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
        cbNotifySessionRemindersEnable = (CheckBoxPreference)findPreference(settingsList.notifySessionRemindersEnabled.name());
        ltNotifySessionRemindersMinutes = (ListPreference)findPreference(settingsList.notifySessionRemindersMinutes.name());
        cbLongRoomNames = (CheckBoxPreference)findPreference(settingsList.longRoomNames.name());
        cbRefreshWiFi = (CheckBoxPreference)findPreference(settingsList.refreshWiFi.name());
        cbRefreshCellular = (CheckBoxPreference)findPreference(settingsList.refreshCellular.name());

        // Set values
        cbNotifySessionRemindersEnable.setChecked(settings.getNotificationSessionRemindersEnabled());
        ltNotifySessionRemindersMinutes.setValue(Integer.toString(settings.getNotificationSessionRemindersMinutes()));
        cbLongRoomNames.setChecked(settings.getLongRoomNames());
        cbRefreshWiFi.setChecked(settings.getRefreshWiFi());
        cbRefreshCellular.setChecked(settings.getRefreshCellular());

        // Set listeners
        Preference.OnPreferenceChangeListener listener = new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {

                String key = preference.getKey();

                String logMessage = String.format("Changed %s to %s", key, value);
                Logger.getInstance().debug("PreferenceFragment", logMessage.toString());

                // Save preference change
                if (key.equals(settingsList.notifySessionRemindersEnabled.name()))
                    settings.setNotificationSessionRemindersEnabled((boolean)value);
                else if (key.equals(settingsList.notifySessionRemindersMinutes.name()))
                    settings.setNotificationSessionRemindersMinutes(Integer.parseInt((String)value));
                else if (key.equals(settingsList.longRoomNames.name()))
                    settings.setLongRoomNames((boolean)value);
                else if (key.equals(settingsList.refreshWiFi.name()))
                    settings.setRefreshWiFi((boolean)value);
                else if (key.equals(settingsList.refreshCellular.name()))
                    settings.setRefreshCellular((boolean)value);
                else
                    Logger.getInstance().error("PreferenceFragment", "Unknown preference " + key);

                // Reschedule notification alarms if any notification preferences have changed
                boolean notificationPrefChanged = key.equals(settingsList.notifySessionRemindersEnabled.name()) ||
                    key.equals(settingsList.notifySessionRemindersMinutes.name());

                if (notificationPrefChanged && settings.getNotificationSessionRemindersEnabled()){
                    Logger.getInstance().info("PreferenceFragment", "Notification preferences changed - rescheduling alarams");
                    SessionReminderNotifier notifier = new SessionReminderNotifier(getActivity());
                    notifier.setAlarms(settings.getSessions(), settings.getNotificationSessionRemindersMinutes());
                }

                return true;
            }
        };

        cbNotifySessionRemindersEnable.setOnPreferenceChangeListener(listener);
        ltNotifySessionRemindersMinutes.setOnPreferenceChangeListener(listener);
        cbLongRoomNames.setOnPreferenceChangeListener(listener);
        cbRefreshWiFi.setOnPreferenceChangeListener(listener);
        cbRefreshCellular.setOnPreferenceChangeListener(listener);
    }
}