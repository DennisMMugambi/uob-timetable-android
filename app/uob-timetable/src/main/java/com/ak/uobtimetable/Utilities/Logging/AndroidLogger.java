package com.ak.uobtimetable.Utilities.Logging;

import android.util.Log;

public class AndroidLogger implements Loggable {

    @Override
    public AndroidLogger verbose(String tag, String message) {
        Log.v(tag, message);
        return this;
    }

    @Override
    public AndroidLogger debug(String tag, String message) {
        Log.d(tag, message);
        return this;
    }

    @Override
    public AndroidLogger info(String tag, String message) {
        Log.i(tag, message);
        return this;
    }

    @Override
    public AndroidLogger warn(String tag, String message) {
        Log.w(tag, message);
        return this;
    }

    @Override
    public AndroidLogger error(String tag, Exception exception) {
        Log.e(tag, exception.getMessage(), exception);
        return this;
    }
}
