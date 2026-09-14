/* PixelDoomgeon — puerta al canvas de Bitmap desde fuera del paquete.
   GPL-3.0-or-later */
package android.graphics;

import org.teavm.jso.dom.html.HTMLCanvasElement;

public final class BitmapPuente {
    private BitmapPuente() {}
    public static Bitmap envolver(HTMLCanvasElement c) { return new Bitmap(c); }
    public static HTMLCanvasElement lienzo(Bitmap b)   { return b.lienzo; }
}
