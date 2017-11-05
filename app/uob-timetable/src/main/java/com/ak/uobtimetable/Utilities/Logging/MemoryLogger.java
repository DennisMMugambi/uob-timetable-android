package com.ak.uobtimetable.Utilities.Logging;

import com.ak.uobtimetable.Utilities.GeneralUtilities;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class MemoryLogger implements Loggable {

    public class Entry {
        public Date dateTime;
        public MemoryLogger.Type type;
        public String tag;
        public String message;

        public Entry(MemoryLogger.Type type, String tag, String message){
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

            HashMap<MemoryLogger.Type, String> typeColours = new HashMap<>();
            typeColours.put(MemoryLogger.Type.info, "#008000"); // LimeGreen
            typeColours.put(MemoryLogger.Type.debug, "blue");
            typeColours.put(MemoryLogger.Type.warn, "orange");
            typeColours.put(MemoryLogger.Type.error, "red");

            String colour = "";
            if (typeColours.containsKey(type))
                colour = typeColours.get(type);

            message = message.replace("\n", "<br/>");

            DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
            String text = dateFormat.format(dateTime) + " > " + tag + " - " + message;

            return "<font color='" + colour + "'>" + text + "</font>";
        }
    }

    public enum Type {
        verbose,
        info,
        debug,
        warn,
        error
    }

    private List<Entry> entries;

    public MemoryLogger(){
        entries = new ArrayList<>();
    }

    @Override
    public MemoryLogger verbose(String tag, String message) {
        entries.add(new Entry(Type.verbose, tag, message));
        return this;
    }

    @Override
    public MemoryLogger debug(String tag, String message) {
        entries.add(new Entry(Type.debug, tag, message));
        return this;
    }

    @Override
    public MemoryLogger info(String tag, String message) {
        entries.add(new Entry(Type.info, tag, message));
        return this;
    }

    @Override
    public MemoryLogger warn(String tag, String message) {
        entries.add(new Entry(Type.warn, tag, message));
        return this;
    }

    @Override
    public MemoryLogger error(String tag, Exception exception) {
        entries.add(new Entry(Type.error, tag, GeneralUtilities.nestedThrowableToString(exception)));
        return this;
    }

    public List<Entry> getEntries(){

        return new ArrayList<>(entries);
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
