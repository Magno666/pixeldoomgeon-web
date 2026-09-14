/* PixelDoomgeon — efectos de sonido. GPL-3.0-or-later */
package android.media;

import android.content.Context;
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

    public SoundPool(int maxStreams, int streamType, int srcQuality) {}

    public int load(Context ctx, String nombre, int prioridad) {
        int id = siguiente++;
        sonidos.put(id, web.Assets.urlDe(nombre));
        return id;
    }

    public int load(android.content.res.AssetFileDescriptor fd, int prioridad) {
        int id = siguiente++;
        sonidos.put(id, web.Assets.urlDe(fd.nombre));
        return id;
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
    public void release()      { sonidos.clear(); }
    public interface OnLoadCompleteListener {
        void onLoadComplete(SoundPool pool, int sampleId, int status);
    }

    public void setOnLoadCompleteListener(OnLoadCompleteListener l) {
        // Los <audio> se cargan solos; se avisa de todo lo ya registrado.
        if (l == null) return;
        for (Integer id : sonidos.keySet()) l.onLoadComplete(this, id, 0);
    }

    public void autoPause()  {}
    public void autoResume() {}
}
