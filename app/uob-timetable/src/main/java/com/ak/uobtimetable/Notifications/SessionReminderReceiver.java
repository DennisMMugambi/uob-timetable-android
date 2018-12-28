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

        //
        if (intent.getAction() != null && intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            Logger.getInstance().info("Notifications", "SessionReminderReceiver BOOT_COMPLETED received");
        } else {
            singleEvent(context, intent);
        }

    }

    private void bootEvent(Context context, Intent intent){

    }

    private void singleEvent(Context context, Intent intent){

        SettingsManager settings = new SettingsManager(context);
        Logger.getInstance().info("Notifications", "Received session reminder notification event");

        // Stop if sessions are no longer enabled
        if (settings.getNotificationSessionRemindersEnabled() == false) {
            Logger.getInstance().error("Notifications", "Preventing notification - notifications disabled");
            return;
        }

        // Get session hash from intent
        String sessionHash = intent.getData().getQueryParameter("session_hash");

        if (sessionHash == null || sessionHash.length() == 0){
            Logger.getInstance().error("Notifications", "Empty session_hash");
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
            Logger.getInstance().info("Notifications", "No session found for notification");
            return;
        }

        // Ignore if session is no longer visible and hidden sessions are not visible
        if (selectedSession.visible == false && settings.getShowHiddenSessions() == false){
            Map<String, String> meta = new HashMap<>();
            Logger.getInstance().info("Notifications", "Session for notification is no longer visible");
            return;
        }

        // Show notification
        SessionReminderNotifier notifier = new SessionReminderNotifier(context);
        notifier.showSessionReminder(selectedSession);

        // Reschedule for next time
        Logger.getInstance().info("Notifications", "Rescheduling session reminder alarm");
        notifier.scheduleAlarm(selectedSession, settings.getNotificationSessionRemindersMinutes());
    }
}
