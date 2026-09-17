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
    private double[] ultimoBorde = {0,0,0,0};
    private double ultimoDispW, ultimoDispH;
    private int cuadros = 0;

    // Chrome de Android no enseña document.title en ningun lado visible,
    // asi que una captura del telefono nunca iba a traer los numeros --
    // por eso, ademas del titulo, esto pinta un recuadro fijo en pantalla
    // cuando la URL pide ?diag. La otra sesion lo señalo tras medir en el
    // emulador y no poder ver el render (bug del propio emulador, no del
    // puerto): sin esto, cualquier captura del telefono de Leonel iba a
    // seguir sin decir nada.
    @org.teavm.jso.JSBody(params = "msg", script =
        "document.title = msg; console.log(msg);" +
        "if (/[?&]diag\\b/.test(location.search) || /REVENTO/.test(msg)) {" +
        "  var d = document.getElementById('__pdDiagVis');" +
        "  if (!d) {" +
        "    d = document.createElement('div');" +
        "    d.id = '__pdDiagVis';" +
        "    d.style.cssText = 'position:fixed;left:0;top:0;z-index:999;" +
        "      background:rgba(0,0,0,.78);color:#4fdc4f;" +
        "      font:11px/1.4 monospace;padding:4px 6px;" +
        "      pointer-events:none;white-space:pre-wrap;max-width:100vw';" +
        "    document.body.appendChild(d);" +
        "  }" +
        "  d.textContent = msg;" +
        "}")
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
            diag("REVENTO en onSurfaceCreated: " + conCausas(t));
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
                    web.Arena.aplicarMirada();
                    if (conDiagnostico()) {
                        web.Diagnostico.atajos();
                    }
                    renderer.onDrawFrame(null);
                } catch (Throwable t) {
                    diag("REVENTO en el frame " + cuadros + ": " + conCausas(t)
                        + " || " + pila(t));
                    return;          // no seguir pintando sobre el error
                }
                cuadros++;
                // El diagnostico solo si la URL lo pide: ?diag. Los contadores
                // encontraron las texturas vacias y el listener sin conectar,
                // asi que se quedan -- pero no en el titulo de la pestana.
                if (cuadros % 30 == 0 && conDiagnostico()) {
                    diag("cuadros=" + cuadros
                        + " errGL=" + GLES20.erroresGL + "/" + GLES20.ultimoError
                        + " ent=" + web.Entrada.eventos
                        + " win=" + Window.current().getInnerWidth() + "x" + Window.current().getInnerHeight()
                        + " dpr=" + Window.current().getDevicePixelRatio()
                        + " borde=" + java.util.Arrays.toString(ultimoBorde)
                        + " disp=" + ultimoDispW + "x" + ultimoDispH
                        + " rect=" + java.util.Arrays.toString(rectLienzo(lienzo))
                        + " canvas=" + anchoPrevio + "x" + altoPrevio
                        + " " + web.Diagnostico.nivel());
                }
                Window.requestAnimationFrame(this);
            }
        });
    }

    /**
     * El juego cree que tiene toda la ventana, y pone botones a un par de
     * pixeles del borde -- el "Okay!" de la pantalla de bienvenida vive en
     * una franja de 18px pegada al fondo (WelcomeScene.java). Eso es justo
     * donde el telefono pone su barra de gestos: un toque ahi no llega al
     * boton, lo intercepta el sistema. Se le resta el area seguro antes de
     * decirle al juego cuanto espacio tiene, para que layouts como ese
     * queden arriba de la barra en vez de debajo.
     */
    private void ajustarTamano() {
        Window w = Window.current();
        double dpr = Math.min(w.getDevicePixelRatio() <= 0 ? 1 : w.getDevicePixelRatio(), 2);
        double[] borde = bordesSeguros();  // arriba, derecha, abajo, izquierda -- en px CSS
        double despX = borde[3], despY = borde[0];
        double dispW = Math.max(1, w.getInnerWidth()  - borde[1] - borde[3]);
        double dispH = Math.max(1, w.getInnerHeight() - borde[0] - borde[2]);
        ultimoBorde = borde; ultimoDispW = dispW; ultimoDispH = dispH;
        int ancho = (int) (dispW * dpr);
        int alto  = (int) (dispH * dpr);
        posicionar(lienzo, despX, despY, dispW, dispH);
        if (ancho == anchoPrevio && alto == altoPrevio) return;
        lienzo.setWidth(ancho);
        lienzo.setHeight(alto);
        anchoPrevio = ancho;
        altoPrevio = alto;

        // Game.onSurfaceChanged (el metodo del juego, sin tocar) solo pide
        // reiniciar la escena cuando ShatteredPixelDungeon.immersiveModeChanged
        // esta prendido -- pensado para cuando el modo inmersivo aparece o
        // desaparece, que en Android tambien cambia el tamano de la
        // superficie. En el navegador el tamano cambia por otras razones
        // (girar el telefono, que Chrome acomode la barra) sin que
        // immersiveModeChanged se entere, asi que la escena se queda
        // dibujando con las dimensiones viejas.
        //
        // No hace falta tocar Game.java: ShatteredPixelDungeon.immerse(...)
        // ya es publico y ya hace justo esto como efecto secundario de
        // aplicar la MISMA preferencia que ya estaba (no cambia nada del
        // modo inmersivo, solo dispara el aviso que el juego ya sabe
        // atender).
        //
        // Se dispara con CUALQUIER cambio, ancho o alto -- se probo
        // limitarlo solo al ancho (para no tocar una ventana abierta si
        // solo se movia la barra de direcciones de Chrome) y eso dejo un
        // bug peor: si solo cambia el alto, la escena vieja se queda
        // pegada arriba con una franja negra abajo, la diferencia exacta
        // de alto. Confirmado con el emulador de la otra sesion, columna
        // por columna del framebuffer.
        //
        // Queda el riesgo de que reiniciar a media partida cierre una
        // ventana abierta (Window cuelga de la escena como hijo; al
        // recrearla no sobrevive) si el cambio de tamano no fue un giro
        // -- pero en Android nativo girar el telefono igual reinicia la
        // Activity entera y cierra cualquier ventana, asi que no es un
        // riesgo nuevo, es el mismo que ya existe.
        com.github.dachhack.sprout.ShatteredPixelDungeon.immerse(
            com.github.dachhack.sprout.ShatteredPixelDungeon.immersed());

        renderer.onSurfaceChanged(null, ancho, alto);
    }

    @org.teavm.jso.JSBody(script =
        "var d = document.getElementById('__pdSafeArea');" +
        "if (!d) {" +
        "  d = document.createElement('div');" +
        "  d.id = '__pdSafeArea';" +
        "  d.style.cssText = 'position:fixed;inset:0;pointer-events:none;visibility:hidden;' +" +
        "    'padding-top:env(safe-area-inset-top,0px);padding-right:env(safe-area-inset-right,0px);' +" +
        "    'padding-bottom:env(safe-area-inset-bottom,0px);padding-left:env(safe-area-inset-left,0px);';" +
        "  document.body.appendChild(d);" +
        "}" +
        "var s = getComputedStyle(d);" +
        "return [parseFloat(s.paddingTop)||0, parseFloat(s.paddingRight)||0," +
        "        parseFloat(s.paddingBottom)||0, parseFloat(s.paddingLeft)||0];")
    private static native double[] bordesSeguros();

    @org.teavm.jso.JSBody(params = { "c", "x", "y", "w", "h" }, script =
        "c.style.position='fixed'; c.style.left=x+'px'; c.style.top=y+'px';" +
        "c.style.width=w+'px'; c.style.height=h+'px';")
    private static native void posicionar(HTMLCanvasElement c, double x, double y, double w, double h);

    @org.teavm.jso.JSBody(params = "c", script =
        "var r = c.getBoundingClientRect(); return [r.left,r.top,r.width,r.height];")
    private static native double[] rectLienzo(HTMLCanvasElement c);

    /**
     * El mensaje de arriba solo dice la excepcion de mas afuera, y el juego
     * envuelve las suyas: InterlevelScene atrapa lo que sea que falle al
     * generar un piso y lo relanza como "fatal error occured while moving
     * between floors", con la de verdad adentro como causa. Sin recorrer la
     * cadena, el aviso no dice nada util.
     */
    private static String conCausas(Throwable t) {
        StringBuilder sb = new StringBuilder();
        Throwable actual = t;
        for (int i = 0; actual != null && i < 5; i++) {
            if (i > 0) sb.append("  <- ");
            sb.append(actual);
            Throwable causa = actual.getCause();
            actual = (causa == actual) ? null : causa;
        }
        return sb.toString();
    }

    // Otra vez detras de ?diag. Estuvo siempre encendido mientras se cazaba
    // el cuelgue de las transiciones de piso; ya esta cazado (era el shim
    // de SoundPool llamando al listener de carga en pleno recorrido de su
    // propio mapa), asi que el recuadro vuelve a ser cosa de quien lo pida.
    // Los reventones siguen saliendo en pantalla sin ?diag: una pantalla
    // congelada y muda fue justo lo que costo tres sesiones encontrar.
    @org.teavm.jso.JSBody(script =
        "return /[?&]diag\\b/.test(location.search);")
    private static native boolean conDiagnostico();

    /** Las primeras lineas de la pila, de la causa mas profunda. Sin esto
     *  un ConcurrentModificationException solo dice su nombre, y el nombre
     *  no dice quien estaba iterando. */
    private static String pila(Throwable t) {
        Throwable raiz = t;
        for (int i = 0; i < 5 && raiz.getCause() != null; i++) {
            raiz = raiz.getCause();
        }
        StackTraceElement[] p = raiz.getStackTrace();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < p.length && i < 12; i++) {
            sb.append(p[i]).append(" / ");
        }
        return sb.length() == 0 ? "(sin pila)" : sb.toString();
    }

    public boolean entregarToque(MotionEvent e) { return onTouchEvent(e); }


    @Override public int getWidth()  { return anchoPrevio > 0 ? anchoPrevio : 0; }
    @Override public int getHeight() { return altoPrevio > 0 ? altoPrevio : 0; }
}
