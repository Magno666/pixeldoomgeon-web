/* PixelDoomgeon — android.util.Log a la consola. GPL-3.0-or-later */
package android.util;

import org.teavm.jso.JSBody;

/**
 * TeaVM no expone console como objeto Java, asi que se llama al de
 * JavaScript directo. @JSBody incrusta el codigo tal cual.
 */
public final class Log {

    private Log() {}

    @JSBody(params = "msg", script = "console.log(msg);")
    private static native void cLog(String msg);

    @JSBody(params = "msg", script = "console.warn(msg);")
    private static native void cWarn(String msg);

    @JSBody(params = "msg", script = "console.error(msg);")
    private static native void cError(String msg);

    public static int v(String t, String m) { cLog(t + ": " + m); return 0; }
    public static int d(String t, String m) { cLog(t + ": " + m); return 0; }
    public static int i(String t, String m) { cLog(t + ": " + m); return 0; }
    public static int w(String t, String m) { cWarn(t + ": " + m); return 0; }
    public static int e(String t, String m) { cError(t + ": " + m); return 0; }
    public static int e(String t, String m, Throwable x) { return e(t, m + " " + x); }
    public static int w(String t, String m, Throwable x) { return w(t, m + " " + x); }

    public static String getStackTraceString(Throwable x) {
        return x == null ? "" : String.valueOf(x);
    }
}
