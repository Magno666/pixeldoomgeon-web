/* PixelDoomgeon — GLSurfaceView sobre canvas + WebGL. GPL-3.0-or-later */
package android.opengl;

import android.content.Context;
import android.view.View;
import android.view.MotionEvent;

import org.teavm.jso.browser.AnimationFrameCallback;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.webgl.WebGLRenderingContext;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Donde el juego de Android se engancha al navegador.
 *
 * En Android esta clase abre una superficie, crea el contexto GL y llama
 * al Renderer en un hilo aparte. Aqui crea un <canvas>, pide WebGL y usa
 * requestAnimationFrame.
 *
 * La diferencia que importa: Android dibuja en SU PROPIO HILO y el
 * navegador no tiene hilos. Todo corre en el mismo, lo cual para este
 * juego esta bien -- no hay nada esperando en segundo plano -- pero
 * significa que cualquier cosa que bloquee congela la pagina entera.
 */
public class GLSurfaceView extends View {

    public interface Renderer {
        void onSurfaceCreated(GL10 gl, EGLConfig config);
        void onSurfaceChanged(GL10 gl, int width, int height);
        void onDrawFrame(GL10 gl);
    }

    private Renderer renderer;
    private HTMLCanvasElement lienzo;
    private WebGLRenderingContext gl;
    private int anchoPrevio = -1, altoPrevio = -1;
    private int cuadros = 0;

    @org.teavm.jso.JSBody(params = "msg", script = "document.title = msg; console.log(msg);")
    private static native void diag(String msg);

    public GLSurfaceView(Context contexto) { super(contexto); }

    /**
     * Pide el contexto probando los tres nombres, como en la demo web:
     * bastantes navegadores dentro de otras apps solo ofrecen
     * 'experimental-webgl', y alguno solo 'webgl2'.
     *
     * depth va en true porque el render 3D lo necesita. Noosa llamaba a
     * setEGLConfigChooser(false) porque en 2D no hacia falta.
     */
    @org.teavm.jso.JSBody(params = "canvas", script =
        "var o = { alpha: false, depth: true, antialias: false };"
      + "return canvas.getContext('webgl', o)"
      + "    || canvas.getContext('experimental-webgl', o)"
      + "    || canvas.getContext('webgl2', o);")
    private static native WebGLRenderingContext contexto3D(
        org.teavm.jso.dom.html.HTMLCanvasElement canvas);

    public void setEGLContextClientVersion(int version) {}
    public void setEGLConfigChooser(boolean conProfundidad) {}
    public void setPreserveEGLContextOnPause(boolean p) {}
    public void setRenderMode(int modo) {}
    public void requestRender() {}
    public void onPause()  {}
    public void onResume() {}

    public void setRenderer(Renderer r) {
        this.renderer = r;
        crearLienzo();
        arrancar();
    }

    private void crearLienzo() {
        HTMLDocument doc = Window.current().getDocument();
        lienzo = (HTMLCanvasElement) doc.getElementById("juego");
        if (lienzo == null) {
            lienzo = (HTMLCanvasElement) doc.createElement("canvas");
            lienzo.setId("juego");
            doc.getBody().appendChild(lienzo);
        }
        // depth: el render 3D lo necesita. Noosa pedia setEGLConfigChooser(false)
        // porque en 2D no hacia falta; aqui se pide siempre.
        gl = contexto3D(lienzo);
        if (gl == null) {
            throw new IllegalStateException("este navegador no tiene WebGL");
        }
        GLES20.setContext(gl);
        web.Entrada.escuchar(lienzo, this);
    }

    private void arrancar() {
        try {
            renderer.onSurfaceCreated(null, null);
        } catch (Throwable t) {
            diag("REVENTO en onSurfaceCreated: " + t);
            return;
        }
        ajustarTamano();
        Window.requestAnimationFrame(new AnimationFrameCallback() {
            @Override public void onAnimationFrame(double tiempo) {
                ajustarTamano();
                try {
                    // Los toques se recogen en JS y se vacian aqui, antes
                    // de dibujar, para que el cuadro ya los vea.
                    web.Entrada.bombear(GLSurfaceView.this);
                    renderer.onDrawFrame(null);
                } catch (Throwable t) {
                    diag("REVENTO en el frame " + cuadros + ": " + t);
                    return;          // no seguir pintando sobre el error
                }
                cuadros++;
                // El diagnostico solo si la URL lo pide: ?diag. Los contadores
                // encontraron las texturas vacias y el listener sin conectar,
                // asi que se quedan -- pero no en el titulo de la pestana.
                if (cuadros % 30 == 0 && conDiagnostico()) {
                    diag("cuadros=" + cuadros
                        + " dibujos=" + GLES20.dibujos
                        + " indices=" + GLES20.indices_total
                        + " texturas=" + GLES20.texturasSubidas
                        + " errGL=" + GLES20.erroresGL + "/" + GLES20.ultimoError
                        + " ent=" + web.Entrada.eventos
                        + " canvas=" + anchoPrevio + "x" + altoPrevio);
                }
                Window.requestAnimationFrame(this);
            }
        });
    }

    /** El canvas sigue a la ventana; si cambia, se avisa al juego. */
    private void ajustarTamano() {
        Window w = Window.current();
        double dpr = Math.min(w.getDevicePixelRatio() <= 0 ? 1 : w.getDevicePixelRatio(), 2);
        int ancho = (int) (w.getInnerWidth() * dpr);
        int alto  = (int) (w.getInnerHeight() * dpr);
        if (ancho == anchoPrevio && alto == altoPrevio) return;
        lienzo.setWidth(ancho);
        lienzo.setHeight(alto);
        anchoPrevio = ancho;
        altoPrevio = alto;
        renderer.onSurfaceChanged(null, ancho, alto);
    }

    /** Lo llama web.Entrada cuando el navegador manda un toque. */
    @org.teavm.jso.JSBody(script = "return location.search.indexOf('diag') >= 0;")
    private static native boolean conDiagnostico();

    public boolean entregarToque(MotionEvent e) { return onTouchEvent(e); }


    @Override public int getWidth()  { return anchoPrevio > 0 ? anchoPrevio : 0; }
    @Override public int getHeight() { return altoPrevio > 0 ? altoPrevio : 0; }
}
