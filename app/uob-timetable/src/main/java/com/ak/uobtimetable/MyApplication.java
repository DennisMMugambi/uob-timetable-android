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

        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm dd/MM/yy");

        Logger.getInstance()
            .info("Application", "API Level: " + AndroidUtilities.apiLevel())
            .info("Application", "API Level Name: " + AndroidUtilities.apiLevelName())
            .info("Application", "Git Commit Hash: " + BuildConfig.GIT_COMMIT_HASH)
            .info("Application", "Git Branch: " + BuildConfig.GIT_BRANCH)
            .info("Application", "Build type: " + getBuildTypeString())
            .info("Application", "Build keys: " + (AndroidUtilities.isReleaseSigned(this) ? "Release" : "Debug"))
            .info("Application", "Version code: " + AndroidUtilities.buildVersionCode(this))
            .info("Application", "Version name: " + AndroidUtilities.buildVersionName(this))
            .info("Application", "Build date: " + dateFormat.format(AndroidUtilities.buildDate()))
            .info("Application", "Package update date: " + dateFormat.format(AndroidUtilities.packageUpdateDate(this)))
            .info("Application", "Launch count: " +  settings.incrementLaunchCount())
            .info("Application", "Network: " + AndroidUtilities.getNetworkRaw(this))
            .info("Application", "Tablet layout: " + AndroidUtilities.isTabletLayout(this));

        // Init bugsnag
        String bugsnagKey = BuildConfig.BUGSNAG_KEY;
        // Key is always inserted in to BuildConfig as a string
        if (bugsnagKey.equals("null")){
            Logger.getInstance().error("Application", "Can't init Bugsnag - No key");
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
            Logger.getInstance().info("Application", "Bugsnag initialised");
        }
    }

    private String getBuildTypeString(){

        return BuildConfig.IS_DEBUG ? "Debug" : "Release";
    }

    public boolean hadPrefDataOnLaunch(){

        return hadPrefDataOnLaunch;
    }
}
