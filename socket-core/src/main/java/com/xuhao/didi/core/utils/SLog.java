package com.xuhao.didi.core.utils;

/**
 * Created by xuhao on 2017/6/9.
 */
public class SLog {
    private static final String TAG = "OkSocket";
    private static final LogPrinter DEFAULT_LOG_PRINTER = new JdkLogPrinter();

    private static boolean isDebug;
    private static volatile LogPrinter sLogPrinter = DEFAULT_LOG_PRINTER;

    public static void setIsDebug(boolean isDebug) {
        SLog.isDebug = isDebug;
    }

    public static void setLogPrinter(LogPrinter logPrinter) {
        sLogPrinter = logPrinter == null ? DEFAULT_LOG_PRINTER : logPrinter;
    }

    public static boolean isDebug() {
        return isDebug;
    }

    public static void e(String msg) {
        log(LogLevel.ERROR, msg, null);
    }

    public static void e(String msg, Throwable throwable) {
        log(LogLevel.ERROR, msg, throwable);
    }

    public static void i(String msg) {
        log(LogLevel.INFO, msg, null);
    }

    public static void w(String msg) {
        log(LogLevel.WARN, msg, null);
    }

    private static void log(LogLevel level, String msg, Throwable throwable) {
        if (!isDebug || sLogPrinter == null) {
            return;
        }
        sLogPrinter.print(level, TAG, msg, throwable);
    }

    public interface LogPrinter {
        void print(LogLevel level, String tag, String msg, Throwable throwable);
    }

    public enum LogLevel {
        ERROR,
        WARN,
        INFO
    }

    private static final class JdkLogPrinter implements LogPrinter {
        private final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(TAG);

        @Override
        public void print(LogLevel level, String tag, String msg, Throwable throwable) {
            java.util.logging.Level jdkLevel;
            switch (level) {
                case ERROR:
                    jdkLevel = java.util.logging.Level.SEVERE;
                    break;
                case WARN:
                    jdkLevel = java.util.logging.Level.WARNING;
                    break;
                default:
                    jdkLevel = java.util.logging.Level.INFO;
                    break;
            }

            String message = tag + ", " + msg;
            if (throwable == null) {
                logger.log(jdkLevel, message);
            } else {
                logger.log(jdkLevel, message, throwable);
            }
        }
    }
}
