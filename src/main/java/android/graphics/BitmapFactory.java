/* PixelDoomgeon — carga de PNG. GPL-3.0-or-later */
package android.graphics;

import java.io.InputStream;

/**
 * Aqui esta el punto mas incomodo del port.
 *
 * Android decodifica un PNG de forma sincrona desde un InputStream. El
 * navegador solo sabe decodificar imagenes de forma ASINCRONA, con un
 * evento de carga. No hay manera de bloquear esperando, asi que las
 * imagenes se precargan antes de arrancar el juego (ver web.Assets) y
 * aqui solo se recogen ya listas, por nombre.
 */
public class BitmapFactory {

    public static class Options {
        public boolean inScaled = true;
        public boolean inJustDecodeBounds = false;
        public int outWidth, outHeight;
        public boolean inDither = false;
        public Bitmap.Config inPreferredConfig = Bitmap.Config.ARGB_8888;
    }

    public static Bitmap decodeStream(InputStream is) {
        return web.Assets.imagenDe(is);
    }

    public static Bitmap decodeStream(InputStream is, Rect pad, Options opts) {
        return decodeStream(is);
    }

    /** En Android saca la imagen de res/. El juego solo usa assets, asi
     *  que esto nunca deberia hacer falta; si se llama, mejor saberlo. */
    public static Bitmap decodeResource(android.content.res.Resources res, int id) {
        android.util.Log.w("BitmapFactory", "decodeResource no existe en web: " + id);
        return null;
    }

    public static Bitmap decodeResource(android.content.res.Resources res,
                                        int id, Options opts) {
        return decodeResource(res, id);
    }
}
