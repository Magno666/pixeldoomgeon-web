/* PixelDoomgeon — View. GPL-3.0-or-later */
package android.view;

import android.content.Context;

public class View {
    public interface OnTouchListener { boolean onTouch(View v, MotionEvent e); }
    protected Context contexto;
    public View() {}
    public View(Context c) { contexto = c; }
    private OnTouchListener escucha;
    public static int registrados, recibidos, aceptados;

    // El juego registra su listener aqui y espera recibir los dedos por el.
    // Guardarlo era lo unico que faltaba: el puente del navegador ya
    // entregaba los eventos, pero desembocaban en un onTouchEvent que
    // devolvia false y no llamaba a nadie.
    public void setOnTouchListener(OnTouchListener l) { escucha = l; registrados++; }

    public boolean onTouchEvent(MotionEvent e) {
        recibidos++;
        if (escucha == null) return false;
        aceptados++;
        return escucha.onTouch(this, e);
    }
    public Context getContext() { return contexto; }
    public void requestFocus() {}
    public void setFocusable(boolean f) {}
    public void setFocusableInTouchMode(boolean f) {}
    public int getWidth()  { return 0; }
    public int getHeight() { return 0; }
    public void setSystemUiVisibility(int v) {}
    public int getSystemUiVisibility() { return 0; }
    public static final int SYSTEM_UI_FLAG_HIDE_NAVIGATION = 0x00000002;
    public static final int SYSTEM_UI_FLAG_FULLSCREEN      = 0x00000004;
    public static final int SYSTEM_UI_FLAG_IMMERSIVE_STICKY= 0x00001000;
    public static final int SYSTEM_UI_FLAG_LAYOUT_STABLE   = 0x00000100;
    public static final int SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION = 0x00000200;
    public static final int SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN      = 0x00000400;
    public static final int SYSTEM_UI_FLAG_IMMERSIVE              = 0x00000800;
    public static final int SYSTEM_UI_FLAG_LOW_PROFILE            = 0x00000001;
}
