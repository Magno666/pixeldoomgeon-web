/* PixelDoomgeon — android.graphics.Paint. GPL-3.0-or-later */
package android.graphics;

/** El juego lo usa para color, grosor y poco mas. */
public class Paint {

    public enum Style { FILL, STROKE, FILL_AND_STROKE }

    int color = 0xFF000000;
    Style estilo = Style.FILL;
    float grosor = 1f;
    boolean antialias = false;
    Shader sombreador;

    public Paint() {}
    public Paint(int flags) {}

    public void setColor(int c)            { color = c; }
    public int  getColor()                 { return color; }
    public void setStyle(Style s)          { estilo = s; }
    public void setStrokeWidth(float w)    { grosor = w; }
    public void setAntiAlias(boolean a)    { antialias = a; }
    public void setDither(boolean d)       {}
    public void setFilterBitmap(boolean f) {}
    public Shader setShader(Shader s)      { sombreador = s; return s; }
    public void setTypeface(Typeface t)    {}
    float tamanoTexto = 12f;
    public void setTextSize(float s)       { tamanoTexto = s; }

    public void setARGB(int a, int r, int g, int b) {
        color = (a << 24) | (r << 16) | (g << 8) | b;
    }

    // Medidas de texto aproximadas. El juego dibuja casi todo con su
    // propia fuente de mapa de bits; esto solo cubre RenderedText.
    public float measureText(String t) { return t == null ? 0 : t.length() * tamanoTexto * 0.55f; }
    public float ascent()  { return -tamanoTexto * 0.8f; }
    public float descent() { return  tamanoTexto * 0.2f; }

    String cssRelleno() {
        if (sombreador != null) {
            String css = sombreador.css();
            if (css != null) return css;
        }
        int a = (color >>> 24) & 0xFF, r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF, b = color & 0xFF;
        return "rgba(" + r + "," + g + "," + b + "," + (a / 255.0) + ")";
    }
}
