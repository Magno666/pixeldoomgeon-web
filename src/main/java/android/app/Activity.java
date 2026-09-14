/* PixelDoomgeon — Activity. GPL-3.0-or-later */
package android.app;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManagerStub;

/**
 * En Android es el ciclo de vida de la app. En el navegador no hay tal
 * cosa: la pagina se abre y ya. Queda lo que el juego llama de verdad.
 */
public class Activity extends Context {

    protected void onCreate(Bundle estado) {}
    protected void onResume()  {}
    protected void onPause()   {}
    protected void onDestroy() {}

    public void setContentView(View v) {}
    public void setRequestedOrientation(int orientacion) {}
    public Ventana getWindow() { return new Ventana(this); }

    /** Lo poco de Window que el juego toca: la vista para el modo inmersivo. */
    public static class Ventana {
        private final View decor;
        Ventana(Context c) { decor = new View(c); }
        public View getDecorView() { return decor; }
        public void addFlags(int f) {}
        public void clearFlags(int f) {}
    }

    public void onWindowFocusChanged(boolean tieneFoco) {}

    /** Game las sobreescribe con @Override, asi que tienen que existir
     *  aqui aunque en el navegador las teclas lleguen por otro camino
     *  (web.Entrada). */
    public boolean onKeyDown(int codigo, android.view.KeyEvent e) { return false; }
    public boolean onKeyUp(int codigo, android.view.KeyEvent e)   { return false; }
    public void setVolumeControlStream(int stream) {}
    public WindowManagerStub getWindowManager() { return new WindowManagerStub(); }
    public void finish() {}
    public void runOnUiThread(Runnable r) { r.run(); }
}
