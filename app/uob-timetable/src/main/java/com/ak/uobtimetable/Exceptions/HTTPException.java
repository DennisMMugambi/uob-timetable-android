package com.ak.uobtimetable.Exceptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Exception class for errors that occur for HTTP communication, with or
 * without a response.
 */
public class HTTPException extends Exception {

    protected String url;
    protected String body;
    protected int statusCode = -1;

    public HTTPException(String message, String url) {
        super(message);
        this.url = url;
    }

    public HTTPException(String message, String url, Throwable throwable) {
        super(message, throwable);
        this.url = url;
    }

    public HTTPException(String message, String url, int statusCode, String body) {
        super(message);
        this.url = url;
        this.statusCode = statusCode;
        this.body = body;
    }

    public HTTPException(String message, String url, int statusCode, String body, Throwable throwable) {
        super(message, throwable);
        this.url = url;
        this.statusCode = statusCode;
        this.body = body;
    }

    public String getUrl() {
        return url;
    }

    public String getBody() {
        return body;
    }

    public boolean hasResponse() {
        return getStatusCode() > -1;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Map<String, String> toMap(){
        HashMap<String, String> entries = new HashMap<>();
        entries.put("request_url", getUrl());
        entries.put("has_response", String.valueOf(hasResponse()));

        if (hasResponse()) {
            entries.put("response_body", getBody());
            entries.put("response_code", Integer.valueOf(getStatusCode()).toString());
        }

        return entries;
    }
}
