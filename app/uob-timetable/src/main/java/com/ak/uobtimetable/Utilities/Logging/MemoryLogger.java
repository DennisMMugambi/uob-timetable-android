package com.ak.uobtimetable.Utilities.Logging;

import com.ak.uobtimetable.Utilities.GeneralUtilities;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MemoryLogger implements Loggable {

    public class Entry {
        public Date dateTime;
        public MemoryLogger.Type type;
        public String tag;
        public String message;
        public Map<String, String> metadata;

        public Entry(MemoryLogger.Type type, String tag, String message, Map<String, String> metadata){
            this.type = type;
            this.tag = tag;
            this.message = message;
            this.dateTime = new Date();
            this.metadata = metadata;
        }

        public String toString(){
            DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
            String text = dateFormat.format(dateTime) + " > " + type + " - " + tag + " - " + message;

            if (metadata != null && metadata.isEmpty() == false)
                text += " - " + this.serialiseMetadata(metadata);

            return text;
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

            if (metadata != null && metadata.isEmpty() == false) {
                String metaText = "<br/>";
                metaText += this.serialiseMetadata(metadata)
                    .replace("\n", "<br/>")
                    .replace(" ", "&nbsp;");
                text +=  "<font face=\"monospace\">" + metaText + "</font>";
            }

            return "<font color=\"" + colour + "\">" + text + "</font>";
        }

        private String serialiseMetadata(Map<String, String> metadata) {

            Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();

            return gson.toJson(metadata);
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
        entries.add(new Entry(Type.verbose, tag, message, null));
        return this;
    }

    @Override
    public MemoryLogger debug(String tag, String message) {
        entries.add(new Entry(Type.debug, tag, message, null));
        return this;
    }

    @Override
    public MemoryLogger info(String tag, String message) {
        entries.add(new Entry(Type.info, tag, message, null));
        return this;
    }

    @Override
    public MemoryLogger warn(String tag, String message, Map<String, String> metadata) {
        entries.add(new Entry(Type.warn, tag, message, metadata));
        return this;
    }

    @Override
    public MemoryLogger error(String tag, Exception exception, Map<String, String> metadata) {
        String message =  GeneralUtilities.nestedThrowableToString(exception);
        entries.add(new Entry(Type.error, tag, message, metadata));
        return this;
    }

    public List<Entry> getEntries(){

        return new ArrayList<>(entries);
    }

    public void clearEntries(){

        entries.clear();
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
