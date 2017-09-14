package com.ak.uobtimetable.Utilities;

import android.util.Log;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Severity;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Wraps the standard Android Log methods and keeps an in memory list of all logged data
 */
public class Logger {

    public class Entry {
        public Date dateTime;
        public Type type;
        public String tag;
        public String message;

        public Entry(Type type, String tag, String message){
            this.type = type;
            this.tag = tag;
            this.message = message;
            this.dateTime = new Date();
        }

        public String toString(){
            DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
            return dateFormat.format(dateTime) + " > " + type + " - " + tag + " - " + message;
        }

        public String toHtml(){

            HashMap<Type, String> typeColours = new HashMap<>();
            typeColours.put(Type.info, "#008000"); // LimeGreen
            typeColours.put(Type.debug, "blue");
            typeColours.put(Type.warn, "orange");
            typeColours.put(Type.error, "red");

            String colour = "";
            if (typeColours.containsKey(type))
                colour = typeColours.get(type);

            message = message.replace("\n", "<br/>");

            DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
            String text = dateFormat.format(dateTime) + " > " + tag + " - " + message;

            return "<font color='" + colour + "'>" + text + "</font>";
        }
    }

    private List<Entry> entries;
    private static Logger instance;

    public enum Type {
        verbose,
        info,
        debug,
        warn,
        error
    }

    public Logger(){

        entries = new ArrayList<>();
        info("Logger", "Initialised");
    }

    public static Logger getInstance(){

        if (instance == null)
            instance = new Logger();

        return instance;
    }

    public Logger verbose(String tag, String message){

        entries.add(new Entry(Type.verbose, tag, message));
        Log.v(tag, message);

        return this;
    }

    public Logger debug(String tag, String message){

        entries.add(new Entry(Type.debug, tag, message));
        Log.e(tag, message);

        return this;
    }

    public Logger info(String tag, String message){

        entries.add(new Entry(Type.info, tag, message));
        Log.i(tag, message);

        return this;
    }

    public Logger warn(String tag, String message){

        entries.add(new Entry(Type.warn, tag, message));
        Log.w(tag, message);

        this.logWarning(message);

        return this;
    }

    public Logger error(String tag, String message){

        entries.add(new Entry(Type.error, tag, message));
        Log.e(tag, message);

        return this;
    }

    public Logger error(String tag, Exception exception){

        this.error(tag, GeneralUtilities.nestedThrowableToString(exception));
        this.logException(exception);

        return this;
    }

    private void logException(Exception exception){

        Bugsnag.notify(exception, Severity.ERROR);
    }

    private void logWarning(String message){

        Bugsnag.notify(new Exception(message), Severity.WARNING);
    }

    public String toString(){

        StringBuilder sb = new StringBuilder();
        for (Entry e : entries)
            sb.append(e.toString() + "\n");
        return sb.toString();
    }

    public String toHtml(){

        StringBuilder sb = new StringBuilder();
        for (Entry e : entries)
            sb.append(e.toHtml() + "<br/>");
        return sb.toString();
    }
}
