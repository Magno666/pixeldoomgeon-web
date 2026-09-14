/* PixelDoomgeon — preferencias sobre localStorage. GPL-3.0-or-later */
package android.content;

import org.teavm.jso.browser.Storage;

/**
 * Ajustes y progreso global (medallas, ranking) en localStorage.
 *
 * Sobrevive a recargas y es por navegador, que es lo mas parecido a lo
 * que el jugador espera. Si el navegador lo bloquea (ventana privada,
 * cookies desactivadas) todo se lee con el valor por defecto y el juego
 * arranca igual, sin ajustes guardados.
 */
public class SharedPreferences {

    private final String prefijo;
    private Storage store;

    SharedPreferences(String nombre) { this.prefijo = "pdg." + nombre + "."; }

    private Storage store() {
        if (store == null) {
            try { store = Storage.getLocalStorage(); } catch (Exception e) { store = null; }
        }
        return store;
    }

    private String leer(String k) {
        Storage s = store();
        if (s == null) return null;
        try { return s.getItem(prefijo + k); } catch (Exception e) { return null; }
    }

    public boolean getBoolean(String k, boolean d) { String v = leer(k); return v == null ? d : "1".equals(v); }
    public int getInt(String k, int d) {
        String v = leer(k);
        try { return v == null ? d : Integer.parseInt(v); } catch (NumberFormatException e) { return d; }
    }
    public String getString(String k, String d) { String v = leer(k); return v == null ? d : v; }
    public boolean contains(String k) { return leer(k) != null; }

    public Editor edit() { return new Editor(); }

    public class Editor {
        private void escribir(String k, String v) {
            Storage s = store();
            if (s == null) return;
            try { s.setItem(prefijo + k, v); } catch (Exception e) { /* lleno o bloqueado */ }
        }
        public Editor putBoolean(String k, boolean v) { escribir(k, v ? "1" : "0"); return this; }
        public Editor putInt(String k, int v)         { escribir(k, String.valueOf(v)); return this; }
        public Editor putString(String k, String v)   { escribir(k, v); return this; }
        public Editor remove(String k) {
            Storage s = store();
            if (s != null) { try { s.removeItem(prefijo + k); } catch (Exception e) {} }
            return this;
        }
        public Editor clear() { return this; }
        public boolean commit() { return true; }
        public void apply() {}
    }
}
