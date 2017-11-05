package com.ak.uobtimetable.Utilities.Logging;

import android.content.Context;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;
import com.bugsnag.android.Severity;

public class BugsnagLogger implements Loggable {

    public BugsnagLogger(Context context, Configuration config){
        Bugsnag.init(context, config);
    }

    @Override
    public BugsnagLogger verbose(String tag, String message) {
        return this;
    }

    @Override
    public BugsnagLogger debug(String tag, String message) {
        Bugsnag.leaveBreadcrumb(tag + " - " + message);
        return this;
    }

    @Override
    public BugsnagLogger info(String tag, String message) {
        return this;
    }

    @Override
    public BugsnagLogger warn(String tag, String message) {
        Bugsnag.notify(new Exception(message), Severity.WARNING);
        return this;
    }

    @Override
    public BugsnagLogger error(String tag, Exception exception) {
        Bugsnag.notify(exception, Severity.ERROR);
        return this;
    }
}
