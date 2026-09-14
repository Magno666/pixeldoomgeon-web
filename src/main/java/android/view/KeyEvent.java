/* PixelDoomgeon — KeyEvent. GPL-3.0-or-later */
package android.view;

public class KeyEvent {
    public static final int ACTION_DOWN = 0;
    public static final int ACTION_UP   = 1;
    public static final int KEYCODE_BACK   = 4;
    public static final int KEYCODE_MENU   = 82;
    public static final int KEYCODE_VOLUME_UP   = 24;
    public static final int KEYCODE_VOLUME_DOWN = 25;

    private final int accion, codigo;
    public KeyEvent(int accion, int codigo) { this.accion = accion; this.codigo = codigo; }
    public int getAction()  { return accion; }
    public int getKeyCode() { return codigo; }
}
