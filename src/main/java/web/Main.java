/* PixelDoomgeon — arranque en el navegador. GPL-3.0-or-later */
package web;

import com.github.dachhack.sprout.Arranque;

import org.teavm.jso.JSBody;

/**
 * Lo que en Android hace el sistema: crear la Activity y llamar onCreate.
 *
 * Antes de eso hay que tener los assets en memoria, porque el juego los
 * pide de forma sincrona en cuanto arranca. Assets.precargar avisa cuando
 * estan y entonces se enciende el juego.
 */
public final class Main {

    private Main() {}

    public static void main(String[] args) {
        Assets.precargar(new Runnable() {
            @Override public void run() {
                arrancar();
            }
        });
    }

    @JSBody(params = "nombre", script =
        "var v = new URLSearchParams(location.search).get(nombre);"
        + "return v === null ? '' : v;")
    private static native String parametro(String nombre);

    private static void arrancar() {
        try {
            MantenerVivo.tocar();
            MantenerGuardables.tocar();
            Arranque.encender();

            // Arena de jefes: la pagina de /arena/ manda su eleccion en la
            // URL. Va DESPUES de encender porque el juego tiene que estar
            // vivo -- assets, texturas, escena -- antes de montarle una
            // pelea encima. switchScene solo deja pedida la escena, asi que
            // esta gana sobre la pantalla de titulo que acaba de pedir el
            // arranque normal.
            String jefe = parametro("jefe");
            if (jefe != null && jefe.length() > 0) {
                Arena.iniciar(jefe, parametro("clase"), "1".equals(parametro("roto")));
            }
            // Sin esto el juego dibuja perfectamente debajo de una pantalla
            // de carga opaca que nunca se va. Es el mismo fallo que ya
            // costo tres dias en la demo web, con otro panel.
            Assets.quitarPantallaDeCarga();
        } catch (Throwable t) {
            android.util.Log.e("PixelDoomgeon", "no arranco: " + t);
            Assets.mostrarError(String.valueOf(t));
        }
    }
}
