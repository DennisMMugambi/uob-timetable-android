package com.ak.uobtimetable;

import android.app.Application;

import com.ak.uobtimetable.Utilities.AndroidUtilities;
import com.ak.uobtimetable.Utilities.Logger;
import com.ak.uobtimetable.Utilities.SettingsManager;
import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;

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

        // Init bugsnag
        String bugsnagKey = BuildConfig.BUGSNAG_KEY;
        // Key is always inserted in to BuildConfig as a string
        if (bugsnagKey.equals("null")){
            Logger.getInstance().error(loggerKey, "Can't init Bugsnag - No key");
        } else {
            String[] bugsnagVersionParts = {
                    BuildConfig.VERSION_NAME,
                    Integer.valueOf(BuildConfig.VERSION_CODE).toString(),
                    BuildConfig.GIT_COMMIT_HASH
            };
            String bugsnagVersion = StringUtils.join(bugsnagVersionParts, ":");

            Configuration config = new Configuration(bugsnagKey);
            config.setAppVersion(bugsnagVersion);
            config.setReleaseStage(getBuildTypeString());

            Bugsnag.init(this, config);
            Logger.getInstance()
                .info(loggerKey, "Bugsnag initialised")
                .setCanLog(true);
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
    }

    private String getBuildTypeString(){

        return BuildConfig.IS_DEBUG ? "Debug" : "Release";
    }

    public boolean hadPrefDataOnLaunch(){

        return hadPrefDataOnLaunch;
    }
}
