package android.util;

public class Log {

    public static final int VERBOSE = 2;
    public static final int DEBUG = 3;
    public static final int INFO = 4;
    public static final int WARN = 5;
    public static final int ERROR = 6;
    public static final int ASSERT = 7;

    public static int v(String tag, String msg) { return println("VERBOSE", tag, msg); }
    public static int d(String tag, String msg) { return println("DEBUG", tag, msg); }
    public static int i(String tag, String msg) { return println("INFO", tag, msg); }
    public static int w(String tag, String msg) { return println("WARN", tag, msg); }
    public static int e(String tag, String msg) { return println("ERROR", tag, msg); }

    public static int v(String tag, String msg, Throwable tr) { return println("VERBOSE", tag, msg + '\n' + tr); }
    public static int d(String tag, String msg, Throwable tr) { return println("DEBUG", tag, msg + '\n' + tr); }
    public static int i(String tag, String msg, Throwable tr) { return println("INFO", tag, msg + '\n' + tr); }
    public static int w(String tag, String msg, Throwable tr) { return println("WARN", tag, msg + '\n' + tr); }
    public static int e(String tag, String msg, Throwable tr) { return println("ERROR", tag, msg + '\n' + tr); }

    private static int println(String level, String tag, String msg) {
        System.out.println(level + "/" + tag + ": " + msg);
        return 0;
    }
}
