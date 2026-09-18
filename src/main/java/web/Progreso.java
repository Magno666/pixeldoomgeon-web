/* PixelDoomgeon — el transporte de la telemetria en el navegador.
   GPL-3.0-or-later */
package web;

import com.github.dachhack.sprout.Telemetria;

import org.teavm.jso.JSBody;

/**
 * La telemetria vive en el juego ({@link Telemetria}) porque el APK
 * necesita exactamente lo mismo. Aqui solo queda lo que si es del
 * navegador: mandar el paquete con fetch, y decir que navegador es.
 *
 * Antes toda la logica estaba en este archivo. Se movio al juego cuando
 * hubo APK, no por limpieza: duplicarla habria significado dos sitios
 * donde arreglar el mismo dato mal contado.
 */
public final class Progreso {

    private Progreso() {}

    private static boolean puesto;

    public static void revisar() {
        // En modo diagnostico no se manda NADA.
        //
        // Cada corrida de una prueba automatica abre un navegador limpio,
        // que se inventa un id de instalacion nuevo y lo registra como si
        // fuera una persona. Doce pruebas son doce jugadores que no
        // existen, y el dato deja de servir justo para lo que se hizo.
        // Ya paso dos veces y las dos hubo que limpiar a mano.
        if (enDiagnostico()) {
            return;
        }
        if (!puesto) {
            puesto = true;
            Telemetria.plataforma = "web";
            Telemetria.dispositivo = navegador();
            Telemetria.enviador = new Telemetria.Enviador() {
                @Override
                public void mandar(String json) {
                    mandarJson(json);
                }
            };
        }
        Telemetria.revisar();
    }

    public static void reiniciar() {
        Telemetria.reiniciar();
    }

    @JSBody(script = "return /[?&]diag\\b/.test(location.search);")
    private static native boolean enDiagnostico();

    @JSBody(params = "json", script =
        // keepalive para que el aviso de muerte salga aunque la pestaña se
        // cierre justo despues, que es lo que suele pasar al morir.
        "try {" +
        "  fetch('/api/progreso', {" +
        "    method: 'POST'," +
        "    headers: { 'Content-Type': 'application/json' }," +
        "    body: json," +
        "    keepalive: true" +
        "  }).catch(function () {});" +
        "} catch (e) {}")
    private static native void mandarJson(String json);

    /** Que navegador y que sistema, sin nada mas. El user agent entero
     *  trae de todo; aqui se recorta a lo que sirve para explicar por que
     *  a alguien le va lento. */
    @JSBody(script =
        "var u = navigator.userAgent || '';" +
        "var nav = /Firefox\\/(\\d+)/.exec(u) ? 'Firefox ' + RegExp.$1" +
        "        : /Edg\\/(\\d+)/.exec(u)     ? 'Edge ' + RegExp.$1" +
        "        : /OPR\\/(\\d+)/.exec(u)     ? 'Opera ' + RegExp.$1" +
        "        : /Chrome\\/(\\d+)/.exec(u)  ? 'Chrome ' + RegExp.$1" +
        "        : /Version\\/(\\d+).*Safari/.exec(u) ? 'Safari ' + RegExp.$1" +
        "        : 'otro';" +
        "var so = /Android (\\d+)/.exec(u)  ? 'Android ' + RegExp.$1" +
        "       : /iPhone OS (\\d+)/.exec(u) ? 'iOS ' + RegExp.$1" +
        "       : /Windows NT ([\\d.]+)/.exec(u) ? 'Windows'" +
        "       : /Mac OS X/.test(u) ? 'macOS'" +
        "       : /Linux/.test(u) ? 'Linux' : '';" +
        "return (nav + ' / ' + so).slice(0, 40);")
    private static native String navegador();
}
