package com.ak.uobtimetable.Notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.ak.uobtimetable.API.Models;
import com.ak.uobtimetable.Utilities.Logging.Logger;
import com.ak.uobtimetable.Utilities.SettingsManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SessionReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction() != null && intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            bootEvent(context, intent);
        } else {
            singleEvent(context, intent);
        }

    }

    private void bootEvent(Context context, Intent intent){

        // Register here on boot, as these are not persisted when the device reboots
        Logger.getInstance().info("SessionReminderReceiver", "SessionReminderReceiver BOOT_COMPLETED received");

        SettingsManager settings = new SettingsManager(context);

        // Register if enabled and we have sessions (session list may be null if application was never setup)
        if (settings.getNotificationSessionRemindersEnabled() && settings.hasSessions()) {
            SessionReminderNotifier notifier = new SessionReminderNotifier(context);
            notifier.setAlarms(settings.getSessions(), settings.getNotificationSessionRemindersMinutes());
        }
    }

    private void singleEvent(Context context, Intent intent){

        SettingsManager settings = new SettingsManager(context);
        Logger.getInstance().info("SessionReminderReceiver", "Received session reminder notification event");

        // Stop if sessions are no longer enabled. The alarm has not been rescheduled at this point
        // so this should only happen once per alarm after disabling notifications.
        if (settings.getNotificationSessionRemindersEnabled() == false)
            return;

        // Get session hash from intent
        String sessionHash = intent.getData().getQueryParameter(SessionReminderNotifier.SESSION_HASH_PARAM);

        if (sessionHash == null || sessionHash.length() == 0){
            Logger.getInstance().error("SessionReminderReceiver", "Empty session_hash");
            return;
        }

        // Get session
        List<Models.DisplaySession> sessions = settings.getSessions();
        Models.DisplaySession selectedSession = null;
        for (Models.DisplaySession session : sessions){
            if (session.hash.equals(sessionHash)){
                selectedSession = session;
                break;
            }
        }

        // Check whether session is found
        if (selectedSession == null) {
            Map<String, String> meta = new HashMap<>();
            Logger.getInstance().info("SessionReminderReceiver", "No session found for notification");
            return;
        }

        // Ignore if session is no longer visible and hidden sessions are not visible
        if (selectedSession.visible == false && settings.getShowHiddenSessions() == false){
            Map<String, String> meta = new HashMap<>();
            Logger.getInstance().info("SessionReminderReceiver", "Session for notification is no longer visible");
            return;
        }

        // Show notification
        SessionReminderNotifier notifier = new SessionReminderNotifier(context);
        notifier.showSessionReminder(selectedSession);

        // Reschedule for next time
        Logger.getInstance().info("SessionReminderReceiver", "Rescheduling session reminder alarm");
        notifier.scheduleAlarm(selectedSession, settings.getNotificationSessionRemindersMinutes());
    }
}
