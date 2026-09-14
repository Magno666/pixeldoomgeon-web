/* PixelDoomgeon — GLUtils.texImage2D. GPL-3.0-or-later */
package android.opengl;

import android.graphics.Bitmap;

/**
 * En Android sube un Bitmap a la textura ligada. WebGL puede tomar un
 * canvas directo, que es justo lo que Bitmap lleva dentro: se le pasa tal
 * cual y el navegador hace la conversion, sin copiar pixeles a mano.
 */
public final class GLUtils {

    private GLUtils() {}

    public static void texImage2D(int target, int level, Bitmap bitmap, int border) {
        GLES20.texturasCanvas++;
        GLES20.gl().texImage2D(target, level, GLES20.GL_RGBA,
                               GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
                               web.Assets.lienzoDe(bitmap));
    }

    public static void texImage2D(int target, int level, int internalFormat,
                                  Bitmap bitmap, int border) {
        texImage2D(target, level, bitmap, border);
    }
}
