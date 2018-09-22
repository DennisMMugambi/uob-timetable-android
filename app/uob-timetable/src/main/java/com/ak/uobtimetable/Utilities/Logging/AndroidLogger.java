package com.ak.uobtimetable.Utilities.Logging;

import android.util.Log;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;

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
    public AndroidLogger warn(String tag, String message, Map<String, String> metadata) {

        if (metadata != null && metadata.isEmpty() == false)
            message += " " + this.serialiseMetadata(metadata);

        Log.w(tag, message);
        return this;
    }

    @Override
    public AndroidLogger error(String tag, Exception exception, Map<String, String> metadata) {

        String message = exception.getMessage();
        if (metadata != null && metadata.isEmpty() == false)
            message += " " + this.serialiseMetadata(metadata);

        Log.e(tag, message, exception);
        return this;
    }

    private String serialiseMetadata(Map<String, String> metadata) {

        Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

        String json = gson.toJson(metadata);

        // Truncate metadata if too long
        json = StringUtils.abbreviate(json, 300);

        return json;
    }
}
