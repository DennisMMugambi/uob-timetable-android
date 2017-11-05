package com.ak.uobtimetable.Utilities.Logging;

import java.util.HashMap;

/**
 * Extendable logger
 */
public class Logger implements Loggable {

    private static HashMap<String, Loggable> loggers = new HashMap<>();
    private static Logger instance;

    public Logger(){

    }

    public static Logger getInstance() {

        if (instance == null)
            instance = new Logger();

        return instance;
    }

    public Logger addLogger(String id, Loggable logger){
        loggers.put(id, logger);
        return this;
    }

    public Loggable getLogger(String id){

        return loggers.get(id);
    }

    public Logger verbose(String tag, String message){

        for (Loggable logger : loggers.values())
            logger.verbose(tag, message);

        return this;
    }

    public Logger debug(String tag, String message){

        for (Loggable logger : loggers.values())
            logger.debug(tag, message);

        return this;
    }

    public Logger info(String tag, String message){

        for (Loggable logger : loggers.values())
            logger.info(tag, message);

        return this;
    }

    public Logger warn(String tag, String message){

        for (Loggable logger : loggers.values())
            logger.warn(tag, message);

        return this;
    }

    public Logger error(String tag, String message){

        for (Loggable logger : loggers.values())
            logger.error(tag, new Exception(message));

        return this;
    }

    public Logger error(String tag, Exception exception){

        for (Loggable logger : loggers.values())
            logger.error(tag, exception);

        return this;
    }
}
