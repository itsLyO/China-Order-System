package common.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LoggerUtil {
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public static void info(String tag, String message) {
        log("INFO", tag, message);
    }

    public static void warn(String tag, String message) {
        log("WARN", tag, message);
    }

    public static void error(String tag, String message, Throwable t) {
        log("ERROR", tag, message + (t != null ? " - " + t.getMessage() : ""));
        if (t != null) {
            t.printStackTrace();
        }
    }

    private static void log(String level, String tag, String message) {
        String time = SDF.format(new Date());
        String thread = Thread.currentThread().getName();
        System.out.printf("[%s] [%s] [%s] [%s]: %s%n", time, level, thread, tag, message);
    }
}
