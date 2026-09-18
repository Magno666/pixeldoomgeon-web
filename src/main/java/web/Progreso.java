/* PixelDoomgeon — progreso anonimo. GPL-3.0-or-later */
package web;

import com.github.dachhack.sprout.Dungeon;

import org.teavm.jso.JSBody;

/**
 * Cuenta hasta donde llega la gente y donde muere.
 *
 * Existe por una pregunta concreta que no se podia contestar: tres
 * personas dijeron en r/PixelDungeon que no pasaron del piso 2, y sin
 * datos no hay forma de saber si es dificultad, aburrimiento o algo
 * roto. Son cosas opuestas -- una se arregla con equilibrio y la otra
 * con contenido.
 *
 * Se manda lo minimo para contestar eso: el numero del piso y, al morir,
 * la causa que el propio juego escribe. Nada de partidas, nada de
 * inventarios, ninguna IP -- el servidor agrupa por un hash con sal,
 * igual que los votos de la encuesta.
 *
 * No toca el juego: mira Dungeon.depth y si el heroe sigue vivo una vez
 * por cuadro y dispara cuando eso cambia. Un gancho dentro de la logica
 * seria mas limpio de leer y mucho mas facil de romper.
 */
public final class Progreso {

    private Progreso() {}

    private static int pisoVisto = 0;
    private static boolean estabaVivo = false;

    public static void revisar() {

        if (Dungeon.hero == null || Dungeon.level == null) {
            return;
        }

        // La arena no cuenta: es un sandbox con el equipo regalado, y
        // mezclarla con partidas de verdad arruinaria justo el dato que se
        // quiere medir.
        if (com.github.dachhack.sprout.Arena.activa) {
            return;
        }

        int piso = Dungeon.depth;
        if (piso > 0 && piso != pisoVisto) {
            pisoVisto = piso;
            mandar("piso", piso, "");
        }

        boolean vivo = Dungeon.hero.isAlive();
        if (estabaVivo && !vivo) {
            String causa = Dungeon.resultDescription;
            mandar("muerte", piso, causa == null ? "" : causa);
        }
        estabaVivo = vivo;
    }

    /** Al empezar otra partida, volver a contar desde cero. */
    public static void reiniciar() {
        pisoVisto = 0;
        estabaVivo = false;
    }

    @JSBody(params = { "evento", "piso", "causa" }, script =
        // keepalive para que el aviso de muerte salga aunque la pestaña se
        // cierre justo despues, que es lo que suele pasar al morir.
        "try {" +
        "  fetch('/api/progreso', {" +
        "    method: 'POST'," +
        "    headers: { 'Content-Type': 'application/json' }," +
        "    body: JSON.stringify({ evento: evento, piso: piso, causa: causa })," +
        "    keepalive: true" +
        "  }).catch(function () {});" +
        "} catch (e) {}")
    private static native void mandar(String evento, int piso, String causa);
}
