/* PixelDoomgeon — android.graphics.Path. GPL-3.0-or-later */
package android.graphics;

import org.teavm.jso.canvas.CanvasRenderingContext2D;
import java.util.ArrayList;
import java.util.List;

/** Guarda los trazos y los repite sobre el canvas cuando toca dibujar. */
public class Path {

    private static final int MOVE = 0, LINE = 1;
    private final List<float[]> pasos = new ArrayList<>();

    public void reset() { pasos.clear(); }
    public void moveTo(float x, float y) { pasos.add(new float[] { MOVE, x, y }); }
    public void lineTo(float x, float y) { pasos.add(new float[] { LINE, x, y }); }
    public void close() {}

    void reproducir(CanvasRenderingContext2D g) {
        for (float[] p : pasos) {
            if (p[0] == MOVE) g.moveTo(p[1], p[2]);
            else              g.lineTo(p[1], p[2]);
        }
    }
}
