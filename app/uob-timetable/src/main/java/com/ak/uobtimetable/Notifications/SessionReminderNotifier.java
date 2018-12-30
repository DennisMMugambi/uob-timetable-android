package com.ak.uobtimetable.Notifications;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;

import com.ak.uobtimetable.API.Models;
import com.ak.uobtimetable.MainActivity;
import com.ak.uobtimetable.R;
import com.ak.uobtimetable.Utilities.Logging.Logger;
import com.ak.uobtimetable.Utilities.SettingsManager;

import org.apache.commons.lang3.StringUtils;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.List;

public class SessionReminderNotifier {

    public static final String SESSION_HASH_PARAM = "session_hash";
    public static final String CHANNEL_ID_SESSION_REMINDERS = "session_reminders";
    protected Context context;

    public SessionReminderNotifier(Context context){
        this.context = context;
    }

    public void setup(){
        // Notification channel only needs to be set on Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelName = context.getString(R.string.notify_channel_session_reminders_name);
            String channelDesc = context.getString(R.string.notify_channel_session_reminders_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_SESSION_REMINDERS, channelName, importance);
            channel.setDescription(channelDesc);
            channel.enableLights(true);
            channel.setLightColor(context.getColor(R.color.colorPrimary));
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void showSessionReminder(Models.DisplaySession session){

        // Get incrementing notification ID
        int id = SettingsManager.getInstance(context).getNotificationId();

        String title = session.isValid ? session.moduleName : "Unknown module";
        String description = String.format(
            "%s from %s to %s in %s",
            session.type,
            session.start,
            session.end,
            StringUtils.join(session.roomsShort, ", ")
        );

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("notification_session_hash", session.hash);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, CHANNEL_ID_SESSION_REMINDERS)
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setChannelId(CHANNEL_ID_SESSION_REMINDERS);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        notificationManager.notify(id, mBuilder.build());

        Logger.getInstance().debug("SessionReminderNotifier", "Created session reminder notification " + id);
    }

    public void setAlarms(List<Models.DisplaySession> sessions, int minutesBefore){

        boolean includeHidden = SettingsManager.getInstance(context).getShowHiddenSessions();

        // Schedule alarms if visible, or hidden sessions are being displayed
        for (Models.DisplaySession session : sessions){
            if (includeHidden || session.visible)
                scheduleAlarm(session, minutesBefore);
        }
    }

    public void scheduleAlarm(Models.DisplaySession session, int minutesBefore){

        // Get start time
        String[] startParts = session.start.split(":");
        int startHour = Integer.parseInt(startParts[0]);
        int startMinutes = Integer.parseInt(startParts[1]);

        // Build time for notification
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Europe/London"));

        ZonedDateTime date = now
            .withHour(startHour)
            .withMinute(startMinutes)
            .withSecond(0)
            .withNano(0)
            .with(session.getDayOfWeek())
            .minusMinutes(minutesBefore);

        // If time has elapsed this week, set it for next week
        if (date.isBefore(now))
            date = date.plusWeeks(1);

        // Set alarm
        PendingIntent pendingIntent = getAlarmPendingIntent(session);

        long alarmTime = date.toInstant().toEpochMilli();
        int alarmType = AlarmManager.RTC_WAKEUP;

        AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= 23)
            alarmMgr.setExactAndAllowWhileIdle(alarmType, alarmTime, pendingIntent);
        else if (Build.VERSION.SDK_INT >= 19)
            alarmMgr.setExact(alarmType, alarmTime, pendingIntent);
        else
            alarmMgr.set(alarmType, alarmTime, pendingIntent);

        StringBuilder logMessage = new StringBuilder();
        logMessage.append("Created session reminder alarm for ");
        logMessage.append(session.hash);
        logMessage.append(" at ");
        logMessage.append(date.format(DateTimeFormatter.ofPattern("E HH:mm:ss dd/MM/yy")));
        Logger.getInstance().info("SessionReminderNotifier", logMessage.toString());
    }

    private PendingIntent getAlarmPendingIntent(Models.DisplaySession session){

        // When building a pending intent for the alarm, we can't set the session hash as an intent
        // extra because this will not make the intent "unique". We'd end up with each session
        // notification overwriting the last. Instead, encode the session hash in a URI set as the
        // intent data which is used when checking the intent equality. It is easy to retrieve the
        // session hash from the intent by parsing the URI parameters when the intent is unpacked.

        Uri sessionHashUri = new Uri.Builder()
            .scheme("com.ak.uobtimetable")
            .authority("session")
            .appendQueryParameter(SESSION_HASH_PARAM, session.hash)
            .build();

        Intent cancelIntent = new Intent(context, SessionReminderReceiver.class).setData(sessionHashUri);
        return PendingIntent.getBroadcast(context, 0, cancelIntent, 0);
    }
}
