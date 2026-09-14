/* PixelDoomgeon — Typeface. El juego dibuja texto con su propia fuente de
   mapa de bits, asi que esto solo necesita existir. GPL-3.0-or-later */
package android.graphics;

public class Typeface {
    public static final Typeface DEFAULT = new Typeface();
    public static Typeface createFromAsset(Object assets, String ruta) { return DEFAULT; }
    public static Typeface create(String familia, int estilo) { return DEFAULT; }
}
