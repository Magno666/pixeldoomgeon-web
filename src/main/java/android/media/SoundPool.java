/* PixelDoomgeon — efectos de sonido. GPL-3.0-or-later */
package android.media;

import android.content.Context;
import org.teavm.jso.browser.TimerHandler;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLAudioElement;
import java.util.HashMap;
import java.util.Map;

/**
 * Cada efecto se guarda como su URL y se reproduce clonando un <audio>.
 *
 * Clonar en cada disparo parece derrochador, pero es lo que permite que
 * dos golpes suenen a la vez; un solo elemento reiniciaria el sonido
 * anterior. Para los pocos efectos que el juego lanza a la vez, sobra.
 */
public class SoundPool {

    private final Map<Integer, String> sonidos = new HashMap<>();
    private int siguiente = 1;
    private OnLoadCompleteListener oyente;

    public SoundPool(int maxStreams, int streamType, int srcQuality) {}

    public int load(Context ctx, String nombre, int prioridad) {
        return registrar(web.Assets.urlDe(nombre));
    }

    public int load(android.content.res.AssetFileDescriptor fd, int prioridad) {
        return registrar(web.Assets.urlDe(fd.nombre));
    }

    private int registrar(String url) {
        final int id = siguiente++;
        sonidos.put(id, url);
        avisarDespues(id);
        return id;
    }

    /**
     * El aviso de "ya cargo" sale en otro turno del bucle de eventos, nunca
     * dentro de load(). Asi es Android -- el callback llega despues, desde
     * el hilo de audio -- y es lo unico que evita volver a entrar a
     * Sample.loadNext() mientras todavia esta corriendo.
     */
    private void avisarDespues(final int id) {
        final SoundPool yo = this;
        Window.setTimeout(new TimerHandler() {
            @Override public void onTimer() {
                OnLoadCompleteListener l = oyente;
                if (l != null) {
                    l.onLoadComplete(yo, id, 0);
                }
            }
        }, 0);
    }

    public int play(int id, float izq, float der, int prioridad, int bucle, float velocidad) {
        String url = sonidos.get(id);
        if (url == null) return 0;
        try {
            HTMLAudioElement a = (HTMLAudioElement)
                Window.current().getDocument().createElement("audio");
            a.setSrc(url);
            a.setVolume(Math.max(0f, Math.min(1f, Math.max(izq, der))));
            a.play();
        } catch (Exception e) { /* mudo hasta el primer toque */ }
        return id;
    }

    public void unload(int id) { sonidos.remove(id); }
    public void release()      { sonidos.clear(); oyente = null; }

    public interface OnLoadCompleteListener {
        void onLoadComplete(SoundPool pool, int sampleId, int status);
    }

    /**
     * SOLO guarda. La version anterior recorria `sonidos` llamando al
     * listener por cada sonido ya registrado, y el listener del juego es
     * Sample.loadNext(), que carga otro sonido y escribe en ese mismo mapa
     * mientras se recorre: ConcurrentModificationException, mas recursion.
     * Reventaba dentro del hilo de la transicion de piso y dejaba la
     * pantalla clavada en "Falling..." / "Descending..." para siempre.
     */
    public void setOnLoadCompleteListener(OnLoadCompleteListener l) {
        oyente = l;
    }

    public void autoPause()  {}
    public void autoResume() {}
}
