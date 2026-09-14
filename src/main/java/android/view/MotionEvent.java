/* PixelDoomgeon — MotionEvent desde eventos del navegador. GPL-3.0-or-later */
package android.view;

/**
 * El juego lee acciones, indices de dedo y coordenadas. El navegador da
 * eventos de raton y de tacto; web.Entrada los convierte a esto.
 *
 * Los valores de ACTION_* son los de Android de verdad: el juego compara
 * contra ellos con mascaras de bits.
 */
public class MotionEvent {

    public static final int ACTION_DOWN          = 0;
    public static final int ACTION_UP            = 1;
    public static final int ACTION_MOVE          = 2;
    public static final int ACTION_CANCEL        = 3;
    public static final int ACTION_POINTER_DOWN  = 5;
    public static final int ACTION_POINTER_UP    = 6;
    public static final int ACTION_MASK          = 0xFF;
    public static final int ACTION_POINTER_INDEX_MASK  = 0xFF00;
    public static final int ACTION_POINTER_INDEX_SHIFT = 8;

    private final int accion, indice;
    private final int[] ids;
    private final float[] xs, ys;

    public MotionEvent(int accion, int indice, int[] ids, float[] xs, float[] ys) {
        this.accion = accion; this.indice = indice;
        this.ids = ids; this.xs = xs; this.ys = ys;
    }

    public int getAction()       { return accion | (indice << ACTION_POINTER_INDEX_SHIFT); }
    public int getActionMasked() { return accion; }
    public int getActionIndex()  { return indice; }
    public int getPointerCount() { return ids.length; }
    public int getPointerId(int i) { return ids[i]; }
    public float getX(int i)     { return xs[i]; }
    public float getY(int i)     { return ys[i]; }
    public float getX()          { return xs.length > 0 ? xs[0] : 0; }
    public float getY()          { return ys.length > 0 ? ys[0] : 0; }

    /** En Android estos eventos se reciclan para no crear basura. Aqui el
     *  recolector se encarga, asi que no hacen nada -- pero el juego los
     *  llama y tienen que existir. */
    public void recycle() {}
    public static MotionEvent obtain(MotionEvent o) {
        return new MotionEvent(o.accion, o.indice, o.ids, o.xs, o.ys);
    }
}
