package com.ak.uobtimetable.Utilities.Logging;

public interface Loggable {

    Loggable verbose(String tag, String message);

    Loggable debug(String tag, String message);

    Loggable info(String tag, String message);

    Loggable warn(String tag, String message);

    Loggable error(String tag, Exception exception);
}
