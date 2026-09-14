/* PixelDoomgeon — assets desde memoria. GPL-3.0-or-later */
package android.content.res;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Los assets se descargan ANTES de arrancar el juego (web.Assets) y aqui
 * solo se reparten. Es el mismo peaje que con las imagenes: el navegador
 * no sabe leer un archivo de forma sincrona, y el juego da por hecho que
 * si.
 *
 * El InputStream que se devuelve lleva el nombre pegado, porque
 * BitmapFactory necesita saber que PNG le pidieron.
 */
public class AssetManager {

    public InputStream open(String nombre) throws IOException {
        byte[] datos = web.Assets.bytesDe(nombre);
        if (datos == null) {
            throw new IOException("asset no precargado: " + nombre);
        }
        return new AssetStream(nombre, datos);
    }

    public String[] list(String ruta) { return web.Assets.listar(ruta); }

    public AssetFileDescriptor openFd(String nombre) throws IOException {
        return new AssetFileDescriptor(nombre);
    }

    /** Un stream que recuerda de que archivo salio. */
    public static class AssetStream extends ByteArrayInputStream {
        public final String nombre;
        AssetStream(String nombre, byte[] datos) { super(datos); this.nombre = nombre; }
    }
}
