/* PixelDoomgeon — android.graphics.Canvas. GPL-3.0-or-later */
package android.graphics;

import org.teavm.jso.canvas.CanvasRenderingContext2D;

/** Lo poco que el juego dibuja a mano: rectangulos y circulos. */
public class Canvas {

    private Bitmap destino;

    public Canvas(Bitmap bitmap) { this.destino = bitmap; }

    /** RenderedText crea uno vacio y luego le pone el bitmap con
     *  setBitmap. */
    public Canvas() { this.destino = null; }

    public void drawRect(float l, float t, float r, float b, Paint paint) {
        CanvasRenderingContext2D g = destino.ctx();
        g.setFillStyle(paint.cssRelleno());
        g.fillRect(l, t, r - l, b - t);
    }

    public void drawRect(RectF rect, Paint paint) {
        drawRect(rect.left, rect.top, rect.right, rect.bottom, paint);
    }

    public void drawCircle(float cx, float cy, float radio, Paint paint) {
        CanvasRenderingContext2D g = destino.ctx();
        g.beginPath();
        g.arc(cx, cy, radio, 0, Math.PI * 2);
        if (paint.estilo == Paint.Style.STROKE) {
            g.setStrokeStyle(paint.cssRelleno());
            g.setLineWidth(paint.grosor);
            g.stroke();
        } else {
            g.setFillStyle(paint.cssRelleno());
            g.fill();
        }
    }

    private Bitmap sustituto;
    public void setBitmap(Bitmap b) { sustituto = b; }

    public void drawPaint(Paint paint) {
        Bitmap d = destinoActual();
        drawRect(0, 0, d.getWidth(), d.getHeight(), paint);
    }

    public void drawText(String texto, float x, int y, Paint paint) {
        CanvasRenderingContext2D g = destinoActual().ctx();
        g.setFillStyle(paint.cssRelleno());
        g.setFont(paint.tamanoTexto + "px monospace");
        g.fillText(texto, x, y);
    }

    private Bitmap destinoActual() { return sustituto != null ? sustituto : destino; }

    public void drawPath(Path path, Paint paint) {
        CanvasRenderingContext2D g = destino.ctx();
        g.beginPath();
        path.reproducir(g);
        g.closePath();
        if (paint.estilo == Paint.Style.STROKE) {
            g.setStrokeStyle(paint.cssRelleno());
            g.setLineWidth(paint.grosor);
            g.stroke();
        } else {
            g.setFillStyle(paint.cssRelleno());
            g.fill();
        }
    }
}
