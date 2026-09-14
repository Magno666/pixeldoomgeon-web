/* PixelDoomgeon — Uri. GPL-3.0-or-later */
package android.net;

public class Uri {
    public final String texto;
    private Uri(String t) { texto = t; }
    public static Uri parse(String s) { return new Uri(s); }
    @Override public String toString() { return texto; }
}
