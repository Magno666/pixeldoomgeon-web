/* PixelDoomgeon — android.graphics.Bitmap sobre canvas 2D. GPL-3.0-or-later */
package android.graphics;

import org.teavm.jso.browser.Window;
import org.teavm.jso.canvas.CanvasRenderingContext2D;
import org.teavm.jso.canvas.ImageData;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import java.nio.ByteBuffer;

/**
 * Un canvas del navegador con cara de Bitmap de Android.
 *
 * El juego lo usa para dos cosas: cargar los PNG del atlas y generar
 * texturas a mano (el degradado del cielo, los circulos del HUD). Las dos
 * caben en un canvas 2D.
 */
public class Bitmap {

    public enum Config { ALPHA_8, RGB_565, ARGB_4444, ARGB_8888 }

    final HTMLCanvasElement lienzo;
    private CanvasRenderingContext2D ctx;
    private ImageData cache;

    Bitmap(HTMLCanvasElement lienzo) { this.lienzo = lienzo; }

    public static Bitmap createBitmap(int w, int h, Config config) {
        HTMLCanvasElement c = (HTMLCanvasElement)
            Window.current().getDocument().createElement("canvas");
        c.setWidth(Math.max(w, 1));
        c.setHeight(Math.max(h, 1));
        return new Bitmap(c);
    }

    public int getWidth()  { return lienzo.getWidth(); }
    public int getHeight() { return lienzo.getHeight(); }

    CanvasRenderingContext2D ctx() {
        if (ctx == null) ctx = (CanvasRenderingContext2D) lienzo.getContext("2d");
        return ctx;
    }

    private ImageData datos() {
        if (cache == null) cache = ctx().getImageData(0, 0, getWidth(), getHeight());
        return cache;
    }

    public void eraseColor(int color) {
        cache = null;
        CanvasRenderingContext2D g = ctx();
        g.setFillStyle(css(color));
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    public int getPixel(int x, int y) {
        ImageData d = datos();
        int i = (y * getWidth() + x) * 4;
        int r = d.getData().get(i), gg = d.getData().get(i + 1);
        int b = d.getData().get(i + 2), a = d.getData().get(i + 3);
        return (a << 24) | (r << 16) | (gg << 8) | b;
    }

    public void setPixel(int x, int y, int color) {
        cache = null;
        CanvasRenderingContext2D g = ctx();
        g.setFillStyle(css(color));
        g.fillRect(x, y, 1, 1);
    }

    public void getPixels(int[] pixels, int offset, int stride,
                          int x, int y, int w, int h) {
        ImageData d = ctx().getImageData(x, y, w, h);
        for (int fy = 0; fy < h; fy++) {
            for (int fx = 0; fx < w; fx++) {
                int i = (fy * w + fx) * 4;
                int r = d.getData().get(i), gg = d.getData().get(i + 1);
                int b = d.getData().get(i + 2), a = d.getData().get(i + 3);
                pixels[offset + fy * stride + fx] = (a << 24) | (r << 16) | (gg << 8) | b;
            }
        }
    }

    public boolean isRecycled() { return false; }
    public void recycle() {}

    /** Los pixeles tal cual los quiere glTexImage2D: RGBA, fila por fila. */
    public ByteBuffer pixelesRGBA() {
        ImageData d = ctx().getImageData(0, 0, getWidth(), getHeight());
        int n = getWidth() * getHeight() * 4;
        ByteBuffer b = ByteBuffer.allocate(n);
        for (int i = 0; i < n; i++) {
            b.put((byte) d.getData().get(i));
        }
        b.position(0);
        return b;
    }

    private static String css(int argb) {
        int a = (argb >>> 24) & 0xFF, r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        return "rgba(" + r + "," + g + "," + b + "," + (a / 255.0) + ")";
    }
}
