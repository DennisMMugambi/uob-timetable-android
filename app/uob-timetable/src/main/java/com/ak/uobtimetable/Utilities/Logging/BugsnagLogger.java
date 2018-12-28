package com.ak.uobtimetable.Utilities.Logging;

import android.content.Context;
import android.support.annotation.NonNull;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Callback;
import com.bugsnag.android.Configuration;
import com.bugsnag.android.Report;
import com.bugsnag.android.Severity;

import java.util.Map;

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
        Bugsnag.leaveBreadcrumb(tag + " - " + message);
        return this;
    }

    @Override
    public BugsnagLogger warn(final String tag, String message, final Map<String, String> metadata) {
        Exception e = new Exception(message);
        notify(Severity.WARNING, tag, e, metadata);
        return this;
    }

    @Override
    public BugsnagLogger error(final String tag, Exception exception, final Map<String, String> metadata) {
        notify(Severity.ERROR, tag, exception, metadata);
        return this;
    }

    private void notify(final Severity severity, final String tag, Exception exception, final Map<String, String> metadata){
        Bugsnag.notify(exception, new Callback() {
            @Override
            public void beforeNotify(@NonNull Report report) {
                report.getError().setSeverity(severity);
                report.getError().addToTab("Error", "tag", tag);
                if (metadata != null) {
                    for (Map.Entry<String, String> metadataEntry : metadata.entrySet())
                        report.getError().getMetaData().addToTab("Custom", metadataEntry.getKey(), metadataEntry.getValue());
                }
            }
        });
    }
}
