package com.ak.uobtimetable.Utilities.Logging;

import java.util.Map;

public interface Loggable {

    Loggable verbose(String tag, String message);

    Loggable debug(String tag, String message);

    Loggable info(String tag, String message);

    Loggable warn(String tag, String message, Map<String, String> metadata);

    Loggable error(String tag, Exception exception, Map<String, String> metadata);
}
