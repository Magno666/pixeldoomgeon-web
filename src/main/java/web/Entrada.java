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
     * Formato de cada evento en la cola: accion, indice del dedo al que
     * se refiere la accion, numero de dedos, y luego id/x/y por dedo. Las
     * coordenadas ya vienen en pixeles del lienzo: la conversion se hace
     * en JS, donde el rectangulo real esta a mano.
     */
    public static void bombear(GLSurfaceView vista) {
        int n = colaLargo();
        if (n == 0) return;

        int i = 0;
        while (i + 3 <= n) {
            int accion = (int) colaLeer(i++);
            int indice = (int) colaLeer(i++);
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
            vista.entregarToque(new MotionEvent(accion, indice, ids, xs, ys));
        }
        colaLimpiar();
    }

    @JSBody(script =
        "if (window.__pdInstalado) return; window.__pdInstalado = true;" +
        "var c = document.getElementById('juego');" +
        "if (!c) return;" +
        "var q = window.__pdCola = [];" +
        // Los mismos numeros que android.view.MotionEvent:
        // DOWN 0, UP 1, MOVE 2, POINTER_DOWN 5, POINTER_UP 6.
        "function aLienzo(cx, cy) {" +
        "  var b = c.getBoundingClientRect();" +
        "  var ex = b.width  > 0 ? c.width  / b.width  : 1;" +
        "  var ey = b.height > 0 ? c.height / b.height : 1;" +
        "  return [(cx - b.left) * ex, (cy - b.top) * ey];" +
        "}" +

        // ---- raton ----
        // El "soltar" se escucha en toda la ventana (no solo el canvas) para
        // no perderlo si el arrastre termina fuera de el. Eso significa que
        // puede llegar un soltar sin que el juego haya visto el bajar -- por
        // ejemplo si el bajar cayo en otro elemento encima del canvas.
        // Touchscreen.processTouchEvents no lo espera: un soltar huerfano
        // tira null.up() y revienta el motor de toques entero. `abajo` lleva
        // la cuenta de si el juego de verdad vio el bajar.
        "var abajo = false;" +
        "function raton(accion) { return function (e) {" +
        "  if (accion === 0) abajo = true;" +
        "  if (!abajo) return;" +
        "  if (accion === 2 && e.buttons === 0) return;" +
        "  if (accion === 1) abajo = false;" +
        "  var p = aLienzo(e.clientX, e.clientY);" +
        "  q.push(accion, 0, 1, 0, p[0], p[1]);" +
        "}; }" +
        "c.addEventListener('mousedown', raton(0));" +
        "c.addEventListener('mousemove', raton(2));" +
        "window.addEventListener('mouseup', raton(1));" +

        // ---- tacto ----
        // El juego usa la mecanica multitactil de Android tal cual
        // (Touchscreen.processTouchEvents): el PRIMER dedo llega como DOWN y
        // se registra solo el del indice 0; los siguientes llegan como
        // POINTER_DOWN y se registran por getActionIndex(). Un MOVE
        // actualiza TODOS los dedos y da por hecho que cada uno ya esta
        // registrado -- si alguno no lo esta, hace null.update() y revienta.
        //
        // Antes esto mandaba todos los dedos siempre como DOWN, asi que solo
        // el primero quedaba registrado y el segundo tronaba el juego en el
        // siguiente MOVE. Y el segundo dedo no es un caso raro: el control en
        // primera persona es justo eso -- un pulgar en la ruedita para
        // caminar y el otro para mirar.
        //
        // `activos` lleva los identificadores en orden; esa posicion ES el
        // indice que espera el juego.
        "var activos = [];" +
        "function buscaDedo(e, id) {" +
        "  var i;" +
        "  for (i = 0; i < e.touches.length; i++)" +
        "    if ((e.touches[i].identifier | 0) === id) return e.touches[i];" +
        "  for (i = 0; i < e.changedTouches.length; i++)" +
        "    if ((e.changedTouches[i].identifier | 0) === id) return e.changedTouches[i];" +
        "  return null;" +
        "}" +
        "function empuja(accion, indice, e) {" +
        "  var campo = [accion, indice, activos.length], i;" +
        "  for (i = 0; i < activos.length; i++) {" +
        "    var t = buscaDedo(e, activos[i]);" +
        "    if (!t) return;" +   // incompleto: mejor no mandar nada que mandar basura
        "    var p = aLienzo(t.clientX, t.clientY);" +
        "    campo.push(activos[i], p[0], p[1]);" +
        "  }" +
        "  q.push.apply(q, campo);" +
        "}" +
        "c.addEventListener('touchstart', function (e) {" +
        "  e.preventDefault();" +
        "  for (var i = 0; i < e.changedTouches.length; i++) {" +
        "    var id = e.changedTouches[i].identifier | 0;" +
        "    if (activos.indexOf(id) >= 0) continue;" +
        "    activos.push(id);" +
        "    var indice = activos.length - 1;" +
        "    empuja(indice === 0 ? 0 : 5, indice, e);" +
        "  }" +
        "}, {passive: false});" +
        "c.addEventListener('touchmove', function (e) {" +
        "  e.preventDefault();" +
        "  if (!activos.length) return;" +
        "  empuja(2, 0, e);" +
        "}, {passive: false});" +
        // touchend deja fuera de `touches` al dedo que se levanta, por eso
        // buscaDedo tambien mira changedTouches.
        "function suelta(e) {" +
        "  e.preventDefault();" +
        "  for (var i = 0; i < e.changedTouches.length; i++) {" +
        "    var id = e.changedTouches[i].identifier | 0;" +
        "    var indice = activos.indexOf(id);" +
        "    if (indice < 0) continue;" +
        "    empuja(activos.length === 1 ? 1 : 6, indice, e);" +
        "    activos.splice(indice, 1);" +
        "  }" +
        "}" +
        "c.addEventListener('touchend', suelta, {passive: false});" +
        "c.addEventListener('touchcancel', suelta, {passive: false});")
    private static native void instalar();

    @JSBody(script = "return window.__pdCola ? window.__pdCola.length : 0;")
    private static native int colaLargo();

    @JSBody(params = "i", script = "return window.__pdCola[i];")
    private static native double colaLeer(int i);

    @JSBody(script = "if (window.__pdCola) window.__pdCola.length = 0;")
    private static native void colaLimpiar();
}
