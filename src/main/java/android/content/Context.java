/* PixelDoomgeon — Context, con archivos sobre localStorage. GPL-3.0-or-later */
package android.content;

import android.content.res.AssetManager;
import android.content.pm.PackageManager;

import org.teavm.jso.browser.Storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * La puerta al sistema. Lo que de verdad importa aqui son los ARCHIVOS.
 *
 * El juego guarda la partida en archivos: abre uno, escribe JSON, lo
 * cierra. En el navegador no hay sistema de archivos, asi que cada
 * "archivo" es una entrada de localStorage, y el contenido se guarda en
 * base64 para que sobreviva cualquier byte.
 *
 * Limitacion real: localStorage ronda los 5 MB por sitio. Una partida de
 * Pixel Dungeon son decenas de KB, asi que caben muchas -- pero no es
 * infinito, y si se llena, escribir falla en silencio. Por eso guardar
 * avisa por consola cuando no puede.
 */
public class Context {

    public static final int MODE_PRIVATE = 0;

    private final AssetManager assets = new AssetManager();
    private static final String PREFIJO = "pdg.file.";

    public AssetManager getAssets() { return assets; }

    public SharedPreferences getSharedPreferences(String nombre, int modo) {
        return new SharedPreferences(nombre);
    }

    public SharedPreferences getPreferences(int modo) {
        return new SharedPreferences("default");
    }

    public Object getSystemService(String nombre) {
        if (VIBRATOR_SERVICE.equals(nombre)) return new android.os.Vibrator();
        return null;
    }
    public static final String VIBRATOR_SERVICE = "vibrator";

    private final android.content.res.Resources recursos = new android.content.res.Resources();
    public android.content.res.Resources getResources() { return recursos; }
    public PackageManager getPackageManager() { return new PackageManager(); }
    public String getPackageName() { return "com.jaliscomundial.pixeldoomgeon"; }
    public void startActivity(Intent intent) {}

    // ---- archivos ----

    private static Storage store() {
        try { return Storage.getLocalStorage(); } catch (Exception e) { return null; }
    }

    public InputStream openFileInput(String nombre) throws FileNotFoundException {
        Storage s = store();
        String b64 = s == null ? null : s.getItem(PREFIJO + nombre);
        if (b64 == null) {
            throw new FileNotFoundException(nombre);
        }
        return new ByteArrayInputStream(Base64.decodificar(b64));
    }

    public OutputStream openFileOutput(String nombre, int modo) {
        return new GuardaEnStorage(nombre);
    }

    public boolean deleteFile(String nombre) {
        Storage s = store();
        if (s == null) return false;
        boolean habia = s.getItem(PREFIJO + nombre) != null;
        s.removeItem(PREFIJO + nombre);
        return habia;
    }

    public String[] fileList() {
        Storage s = store();
        List<String> out = new ArrayList<>();
        if (s != null) {
            for (int i = 0; i < s.getLength(); i++) {
                String k = s.key(i);
                if (k != null && k.startsWith(PREFIJO)) {
                    out.add(k.substring(PREFIJO.length()));
                }
            }
        }
        return out.toArray(new String[0]);
    }

    /** Acumula y escribe al cerrar: localStorage no sabe de escritura por
     *  partes, y una partida a medio guardar seria peor que ninguna. */
    private static class GuardaEnStorage extends ByteArrayOutputStream {
        private final String nombre;
        GuardaEnStorage(String nombre) { this.nombre = nombre; }
        @Override public void close() throws IOException {
            super.close();
            Storage s = store();
            if (s == null) return;
            try {
                s.setItem(PREFIJO + nombre, Base64.codificar(toByteArray()));
            } catch (Exception e) {
                android.util.Log.e("Context", "no se pudo guardar " + nombre
                    + " (localStorage lleno o bloqueado)");
            }
        }
    }
}
