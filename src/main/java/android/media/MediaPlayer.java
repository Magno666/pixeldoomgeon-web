/* PixelDoomgeon — musica sobre <audio>. GPL-3.0-or-later */
package android.media;

import android.content.res.AssetFileDescriptor;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLAudioElement;

/**
 * La musica del juego, en un elemento <audio>.
 *
 * Los navegadores no dejan sonar nada hasta que el usuario toca la
 * pagina. Asi que play() puede ser rechazado en silencio; se ignora a
 * proposito, porque un juego que no arranca por no poder poner musica
 * seria mucho peor que uno mudo hasta el primer clic.
 */
public class MediaPlayer {

    private HTMLAudioElement audio;
    private String fuente;
    private boolean enBucle;
    private float volumen = 1f;

    public void setDataSource(AssetFileDescriptor fd) { fuente = fd.nombre; }
    public void setDataSource(String ruta)            { fuente = ruta; }
    public void setDataSource(Object fd, long inicio, long largo) {}

    public void prepare() { crear(); }
    public void prepareAsync() { crear(); }

    private void crear() {
        if (fuente == null) return;
        if (audio == null) {
            audio = (HTMLAudioElement) Window.current().getDocument().createElement("audio");
        }
        audio.setSrc(web.Assets.urlDe(fuente));
        audio.setLoop(enBucle);
        audio.setVolume(volumen);
    }

    public void start() {
        if (audio == null) crear();
        if (audio != null) {
            try { audio.play(); } catch (Exception e) { /* el navegador aun no deja */ }
        }
    }

    public void pause()   { if (audio != null) audio.pause(); }
    public void stop()    { pause(); }
    public void release() { audio = null; }
    public void reset()   { audio = null; fuente = null; }
    public void setLooping(boolean b) { enBucle = b; if (audio != null) audio.setLoop(b); }
    public void setVolume(float l, float r) {
        volumen = Math.max(0f, Math.min(1f, l));
        if (audio != null) audio.setVolume(volumen);
    }
    public boolean isPlaying() { return audio != null && !audio.isPaused(); }
    public void setOnCompletionListener(Object l) {}

    public interface OnPreparedListener { void onPrepared(MediaPlayer mp); }
    public interface OnErrorListener { boolean onError(MediaPlayer mp, int what, int extra); }

    private OnPreparedListener alPreparar;

    public void setOnPreparedListener(OnPreparedListener l) {
        alPreparar = l;
        // El <audio> se prepara solo; se avisa en cuanto se pide.
        if (l != null && fuente != null) l.onPrepared(this);
    }
    public void setOnErrorListener(OnErrorListener l) {}
    public void setAudioStreamType(int tipo) {}
}
