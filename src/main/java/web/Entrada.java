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
        instalarTeclado();
        instalarPuntero();
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
        avisarSiSeJuega();
        bombearTeclas();
        bombearMirada();
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
        "  var p;" +
        // Capturado, clientX/clientY se congelan donde se hizo clic. El
        // clic va entonces al centro de la pantalla, que es lo que se
        // esta mirando.
        "  if (document.pointerLockElement === c) {" +
        "    p = [c.width / 2, c.height / 2];" +
        "  } else {" +
        "    p = aLienzo(e.clientX, e.clientY);" +
        "  }" +
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

    /**
     * Teclado. Va por su propia cola: el formato de los toques es de largo
     * variable (cuantos dedos hay) y meter ahi registros de otra forma es
     * pedir que un dia se lean corridos.
     *
     * Solo movimiento y giro. Todo lo demas -- inventario, esperar, buscar
     * -- ya tiene boton en pantalla, y el juego se diseño para tocarse.
     */
    private static void bombearTeclas() {
        int n = teclasLargo();
        if (n == 0) return;
        for (int i = 0; i + 1 < n; i += 2) {
            int codigo = (int) teclasLeer(i);
            boolean abajo = teclasLeer(i + 1) != 0;
            com.github.dachhack.sprout.FirstPersonControls.tecla(codigo, abajo);
        }
        teclasLimpiar();
    }

    @JSBody(script =
        "if (window.__pdTeclado) return; window.__pdTeclado = true;" +
        "var q = window.__pdTeclas = [];" +
        // Los mismos numeros que FirstPersonControls.TECLA_*.
        "var mapa = {" +
        "  ArrowUp: 1, KeyW: 1," +
        "  ArrowRight: 2, KeyD: 2," +
        "  ArrowDown: 3, KeyS: 3," +
        "  ArrowLeft: 4, KeyA: 4," +
        "  KeyQ: 5, KeyE: 6" +
        "};" +
        "function manda(e, abajo) {" +
        "  var c = mapa[e.code];" +
        "  if (!c) return;" +
        // Sin esto las flechas hacen scroll de la pagina bajo el juego.
        "  e.preventDefault();" +
        // Un keydown repetido por el teclado no es una tecla nueva: el
        // paso lo marca el reloj del juego, no la velocidad de repeticion
        // del sistema.
        "  if (abajo && e.repeat) return;" +
        "  q.push(c, abajo ? 1 : 0);" +
        "}" +
        "window.addEventListener('keydown', function (e) { manda(e, true); });" +
        "window.addEventListener('keyup',   function (e) { manda(e, false); });" +
        // Cambiar de pestana con una tecla abajo nunca manda su keyup, y el
        // heroe se queda caminando solo contra una pared.
        "window.addEventListener('blur', function () {" +
        "  q.push(1,0, 2,0, 3,0, 4,0, 5,0, 6,0);" +
        "});")
    private static native void instalarTeclado();

    /**
     * Raton capturado. Sin esto, en una computadora hay que mantener el
     * boton apretado para mirar, que es como se mueve la camara en un
     * juego de estrategia, no en uno en primera persona.
     */
    /**
     * Le dice a JS si ahora mismo se esta jugando en primera persona.
     *
     * Capturar el puntero en la pantalla de titulo rompe los menus: con el
     * puntero preso clientX/clientY se congelan y TODO clic se va al centro
     * de la pantalla, asi que los botones dejan de responder. Y si se
     * captura con el inventario abierto, no puedes tocar un objeto.
     */
    private static void avisarSiSeJuega() {
        boolean jugando = com.github.dachhack.sprout.FirstPerson.enabled
            && com.github.dachhack.sprout.Dungeon.level != null
            && !com.github.dachhack.sprout.scenes.GameScene.windowOpen();
        marcarJugando(jugando);
    }

    @JSBody(params = "si", script =
        "window.__pdJugando = si;" +
        // Si se abrio una ventana o se salio de la mazmorra, soltar el
        // puntero: con el preso no se puede tocar nada del inventario.
        "if (!si && document.pointerLockElement && document.exitPointerLock) {" +
        "  try { document.exitPointerLock(); } catch (e) {}" +
        "}")
    private static native void marcarJugando(boolean si);

    private static void bombearMirada() {
        int n = miradaLargo();
        if (n < 2) return;
        float dx = 0f, dy = 0f;
        for (int i = 0; i + 1 < n; i += 2) {
            dx += (float) miradaLeer(i);
            dy += (float) miradaLeer(i + 1);
        }
        miradaLimpiar();
        if (dx != 0f || dy != 0f) {
            com.github.dachhack.sprout.FirstPersonControls.mirar(dx, dy);
        }
    }

    @JSBody(script =
        "if (window.__pdPuntero) return; window.__pdPuntero = true;" +
        "var c = document.getElementById('juego');" +
        "if (!c) return;" +
        "var q = window.__pdMirada = [];" +
        "c.addEventListener('mousedown', function () {" +
        "  if (!window.__pdJugando) return;" +
        "  if (document.pointerLockElement !== c && c.requestPointerLock) {" +
        "    try { c.requestPointerLock(); } catch (e) {}" +
        "  }" +
        "});" +
        "document.addEventListener('mousemove', function (e) {" +
        "  if (document.pointerLockElement !== c) return;" +
        "  q.push(e.movementX || 0, e.movementY || 0);" +
        "});" +
        "document.addEventListener('pointerlockchange', function () {" +
        "  var dentro = document.pointerLockElement === c;" +
        "  if (window.__pdAvisoPuntero) window.__pdAvisoPuntero(dentro);" +
        "});")
    private static native void instalarPuntero();

    @JSBody(script = "return window.__pdMirada ? window.__pdMirada.length : 0;")
    private static native int miradaLargo();

    @JSBody(params = "i", script = "return window.__pdMirada[i];")
    private static native double miradaLeer(int i);

    @JSBody(script = "if (window.__pdMirada) window.__pdMirada.length = 0;")
    private static native void miradaLimpiar();

    @JSBody(script = "return window.__pdTeclas ? window.__pdTeclas.length : 0;")
    private static native int teclasLargo();

    @JSBody(params = "i", script = "return window.__pdTeclas[i];")
    private static native double teclasLeer(int i);

    @JSBody(script = "if (window.__pdTeclas) window.__pdTeclas.length = 0;")
    private static native void teclasLimpiar();

    @JSBody(script = "return window.__pdCola ? window.__pdCola.length : 0;")
    private static native int colaLargo();

    @JSBody(params = "i", script = "return window.__pdCola[i];")
    private static native double colaLeer(int i);

    @JSBody(script = "if (window.__pdCola) window.__pdCola.length = 0;")
    private static native void colaLimpiar();
}
