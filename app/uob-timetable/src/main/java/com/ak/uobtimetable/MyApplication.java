package com.ak.uobtimetable;

import android.app.Application;

import com.ak.uobtimetable.Utilities.AndroidUtilities;
import com.ak.uobtimetable.Utilities.Logger;
import com.ak.uobtimetable.Utilities.SettingsManager;

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
            .info("Application", "Build type: " + (BuildConfig.IS_DEBUG ? "Debug" : "Release"))
            .info("Application", "Build keys: " + (AndroidUtilities.isReleaseSigned(this) ? "Release" : "Debug"))
            .info("Application", "Version code: " + AndroidUtilities.buildVersionCode(this))
            .info("Application", "Version name: " + AndroidUtilities.buildVersionName(this))
            .info("Application", "Build date: " + dateFormat.format(AndroidUtilities.buildDate()))
            .info("Application", "Package update date: " + dateFormat.format(AndroidUtilities.packageUpdateDate(this)))
            .info("Application", "Launch count: " +  settings.incrementLaunchCount())
            .info("Application", "Network: " + AndroidUtilities.getNetworkRaw(this))
            .info("Application", "Tablet layout: " + AndroidUtilities.isTabletLayout(this));
    }

    public boolean hadPrefDataOnLaunch(){

        return hadPrefDataOnLaunch;
    }
}
