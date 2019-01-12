package com.ak.uobtimetable;

import android.app.Application;

import com.ak.uobtimetable.Utilities.AndroidUtilities;
import com.ak.uobtimetable.Utilities.Logging.AndroidLogger;
import com.ak.uobtimetable.Utilities.Logging.BugsnagLogger;
import com.ak.uobtimetable.Utilities.Logging.Logger;
import com.ak.uobtimetable.Utilities.Logging.MemoryLogger;
import com.ak.uobtimetable.Notifications.SessionReminderNotifier;
import com.ak.uobtimetable.Utilities.SettingsManager;
import com.bugsnag.android.Configuration;
import com.jakewharton.threetenabp.AndroidThreeTen;

import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;

/**
 * Extended Application class which performs logging on startup.
 */
public class MyApplication extends Application {

    private boolean hadPrefDataOnLaunch;

    public MyApplication(){

        Logger.getInstance().info("Application", "Startup");
    }

    public void onCreate(){

        super.onCreate();

        SettingsManager settings = SettingsManager.getInstance(this);
        hadPrefDataOnLaunch = settings.isEmpty() == false;
        settings.clearOldData();

        String loggerKey = "Application";

        AndroidThreeTen.init(this);

        // Init logger
        Logger.getInstance()
            .addLogger("android", new AndroidLogger())
            .addLogger("memory", new MemoryLogger());

        // Init bugsnag
        String bugsnagKey = BuildConfig.BUGSNAG_KEY;
        // Key is always inserted in to BuildConfig as a string
        if (bugsnagKey.equals("null")){
            Logger.getInstance().error(loggerKey, "Can't init Bugsnag - No key");
        } else {
            Configuration config = new Configuration(bugsnagKey);
            config.setAppVersion(BuildConfig.VERSION_NAME);
            config.setReleaseStage(getBuildTypeString());

            Logger.getInstance()
                .addLogger("bugsnag", new BugsnagLogger(this, config))
                .info(loggerKey, "Bugsnag initialised");
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm dd/MM/yy");

        Logger.getInstance()
            .info(loggerKey, "API level: " + AndroidUtilities.apiLevel())
            .info(loggerKey, "API level name: " + AndroidUtilities.apiLevelName())
            .info(loggerKey, "Target API level: " + AndroidUtilities.targetApiLevel(this))
            .info(loggerKey, "Git commit hash: " + BuildConfig.GIT_COMMIT_HASH)
            .info(loggerKey, "Git branch: " + BuildConfig.GIT_BRANCH)
            .info(loggerKey, "Build type: " + getBuildTypeString())
            .info(loggerKey, "Build keys: " + (AndroidUtilities.isReleaseSigned(this) ? "Release" : "Debug"))
            .info(loggerKey, "Version code: " + AndroidUtilities.buildVersionCode(this))
            .info(loggerKey, "Version name: " + AndroidUtilities.buildVersionName(this))
            .info(loggerKey, "Build date: " + dateFormat.format(AndroidUtilities.buildDate()))
            .info(loggerKey, "Package update date: " + dateFormat.format(AndroidUtilities.packageUpdateDate(this)))
            .info(loggerKey, "Launch count: " +  settings.incrementLaunchCount())
            .info(loggerKey, "Network: " + AndroidUtilities.getNetworkRaw(this))
            .info(loggerKey, "Tablet layout: " + AndroidUtilities.isTabletLayout(this));

        // Init notification channels
        SessionReminderNotifier notifier = new SessionReminderNotifier(this);
        notifier.setup();
    }

    private String getBuildTypeString(){

        return BuildConfig.IS_DEBUG ? "Debug" : "Release";
    }

    public boolean hadPrefDataOnLaunch(){

        return hadPrefDataOnLaunch;
    }
}
