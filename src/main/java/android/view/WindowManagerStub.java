/* PixelDoomgeon — lo minimo de WindowManager: el tamano de la pantalla.
   GPL-3.0-or-later */
package android.view;

import android.util.DisplayMetrics;
import org.teavm.jso.browser.Window;

public class WindowManagerStub {
    public Pantalla getDefaultDisplay() { return new Pantalla(); }

    public static class Pantalla {
        public void getMetrics(DisplayMetrics m) {
            Window w = Window.current();
            double dpr = w.getDevicePixelRatio();
            if (dpr <= 0) dpr = 1;
            m.widthPixels  = (int) (w.getInnerWidth()  * dpr);
            m.heightPixels = (int) (w.getInnerHeight() * dpr);
            m.density = (float) dpr;
        }
        public int getWidth()  { return Window.current().getInnerWidth(); }
        public int getHeight() { return Window.current().getInnerHeight(); }
    }
}
