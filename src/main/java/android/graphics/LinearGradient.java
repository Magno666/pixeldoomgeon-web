/* PixelDoomgeon — degradado lineal. GPL-3.0-or-later */
package android.graphics;

/**
 * Lo usa TextureCache.createGradient para el cielo y las barras.
 *
 * De momento devuelve el color medio en vez de un degradado de verdad:
 * para pintar una textura de unos pocos pixeles de alto la diferencia no
 * se ve, y montar un CanvasGradient exige el contexto del canvas, que
 * aqui no se tiene. Si alguna textura sale plana y deberia degradar, es
 * por esto.
 */
public class LinearGradient extends Shader {

    private final int[] colores;

    public LinearGradient(float x0, float y0, float x1, float y1,
                          int[] colores, float[] posiciones, TileMode modo) {
        this.colores = colores;
    }

    public LinearGradient(float x0, float y0, float x1, float y1,
                          int color0, int color1, TileMode modo) {
        this.colores = new int[] { color0, color1 };
    }

    @Override
    String css() {
        if (colores == null || colores.length == 0) return null;
        long a = 0, r = 0, g = 0, b = 0;
        for (int c : colores) {
            a += (c >>> 24) & 0xFF; r += (c >> 16) & 0xFF;
            g += (c >> 8) & 0xFF;   b += c & 0xFF;
        }
        int n = colores.length;
        return "rgba(" + (r / n) + "," + (g / n) + "," + (b / n) + "," + ((a / n) / 255.0) + ")";
    }
}
