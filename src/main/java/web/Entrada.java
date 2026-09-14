/* PixelDoomgeon — eventos del navegador a MotionEvent. GPL-3.0-or-later */
package web;

import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

import org.teavm.jso.JSBody;

/**
 * El juego espera dedos: identificadores, indices, multiples a la vez.
 * El navegador da raton o tacto. Aqui se traduce.
 *
 * Los listeners se instalan en JavaScript y encolan; Java vacia la cola
 * una vez por cuadro. Enganchar los listeners desde Java con el JSO de
 * TeaVM se compilaba, corria y no oia nada: una sonda en JS puro sobre
 * el mismo canvas contaba 24 de 24 clics mientras el puente de Java
 * contaba cero. Esto ademas es justo lo que Noosa hace por dentro --
 * Game acumula MotionEvents y los procesa en update() -- asi que la
 * cola no es un parche, es el mismo patron un escalon mas abajo.
 *
 * El raton se presenta como un solo dedo con id 0, que es exactamente lo
 * que el juego hace con un telefono de un toque. El tacto llega con
 * varios a la vez y se pasa entero: el juego mueve con un pulgar y mira
 * con el otro al mismo tiempo, asi que perder el segundo dedo se nota.
 */
public final class Entrada {

    private Entrada() {}

    public static int eventos;

    public static void escuchar(org.teavm.jso.dom.html.HTMLCanvasElement lienzo,
                                GLSurfaceView vista) {
        instalar();
    }

    /**
     * Vacia la cola y entrega los eventos al juego. Se llama una vez por
     * cuadro, antes de dibujar.
     *
     * Formato de cada evento en la cola: accion, numero de dedos, y luego
     * id/x/y por dedo. Las coordenadas ya vienen en pixeles del lienzo:
     * la conversion se hace en JS, donde el rectangulo real esta a mano.
     */
    public static void bombear(GLSurfaceView vista) {
        int n = colaLargo();
        if (n == 0) return;

        int i = 0;
        while (i < n) {
            int accion = (int) colaLeer(i++);
            int dedos  = (int) colaLeer(i++);
            if (dedos <= 0 || i + dedos * 3 > n) break;

            int[] ids = new int[dedos];
            float[] xs = new float[dedos];
            float[] ys = new float[dedos];
            for (int d = 0; d < dedos; d++) {
                ids[d] = (int)   colaLeer(i++);
                xs[d]  = (float) colaLeer(i++);
                ys[d]  = (float) colaLeer(i++);
            }
            eventos++;
            vista.entregarToque(new MotionEvent(accion, 0, ids, xs, ys));
        }
        colaLimpiar();
    }

    @JSBody(script =
        "if (window.__pdInstalado) return; window.__pdInstalado = true;" +
        "var c = document.getElementById('juego');" +
        "if (!c) return;" +
        "var q = window.__pdCola = [];" +
        // ACTION_DOWN 0, ACTION_UP 1, ACTION_MOVE 2
        "function aLienzo(cx, cy) {" +
        "  var b = c.getBoundingClientRect();" +
        "  var ex = b.width  > 0 ? c.width  / b.width  : 1;" +
        "  var ey = b.height > 0 ? c.height / b.height : 1;" +
        "  return [(cx - b.left) * ex, (cy - b.top) * ey];" +
        "}" +
        "function raton(accion) { return function (e) {" +
        "  if (accion === 2 && e.buttons === 0) return;" +
        "  var p = aLienzo(e.clientX, e.clientY);" +
        "  q.push(accion, 1, 0, p[0], p[1]);" +
        "}; }" +
        "c.addEventListener('mousedown', raton(0));" +
        "c.addEventListener('mousemove', raton(2));" +
        "window.addEventListener('mouseup', raton(1));" +
        // touchend deja fuera de `touches` al dedo que se levanta, asi que
        // para soltar hay que mirar changedTouches o el juego nunca se
        // entera de cual se fue.
        "function tacto(accion) { return function (e) {" +
        "  e.preventDefault();" +
        "  var lista = accion === 1 ? e.changedTouches : e.touches;" +
        "  if (!lista.length) return;" +
        "  var campo = [accion, lista.length];" +
        "  for (var i = 0; i < lista.length; i++) {" +
        "    var p = aLienzo(lista[i].clientX, lista[i].clientY);" +
        "    campo.push(lista[i].identifier | 0, p[0], p[1]);" +
        "  }" +
        "  q.push.apply(q, campo);" +
        "}; }" +
        "c.addEventListener('touchstart', tacto(0), {passive: false});" +
        "c.addEventListener('touchmove',  tacto(2), {passive: false});" +
        "c.addEventListener('touchend',   tacto(1), {passive: false});" +
        "c.addEventListener('touchcancel', tacto(1), {passive: false});")
    private static native void instalar();

    @JSBody(script = "return window.__pdCola ? window.__pdCola.length : 0;")
    private static native int colaLargo();

    @JSBody(params = "i", script = "return window.__pdCola[i];")
    private static native double colaLeer(int i);

    @JSBody(script = "if (window.__pdCola) window.__pdCola.length = 0;")
    private static native void colaLimpiar();
}
