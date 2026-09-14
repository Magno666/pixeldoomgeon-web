/* PixelDoomgeon — Intent. Abrir enlaces. GPL-3.0-or-later */
package android.content;

public class Intent {
    public static final String ACTION_VIEW = "android.intent.action.VIEW";
    private String url;
    public Intent() {}
    public Intent(String accion, android.net.Uri uri) { url = uri == null ? null : uri.texto; }
    public String url() { return url; }
}
