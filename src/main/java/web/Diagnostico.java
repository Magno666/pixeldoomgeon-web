/* PixelDoomgeon — cuenta casillas no-pared del nivel, sin depender de
 * verlo dibujado. GPL-3.0-or-later */
package web;

import com.github.dachhack.sprout.Dungeon;
import com.github.dachhack.sprout.scenes.GameScene;

import org.teavm.jso.JSBody;
import com.github.dachhack.sprout.levels.Level;
import com.github.dachhack.sprout.levels.Terrain;

/**
 * Detras de ?diag, en el titulo de la pestana. Sirve para confirmar que
 * los cuartos se pintaron sin necesitar ver el render 3D -- util en un
 * entorno sin GPU real, donde el render va a ~1 cuadro/s pero la logica
 * del nivel corre en un solo cuadro igual.
 */
public final class Diagnostico {

    private Diagnostico() {}

    public static String nivel() {
        Level lvl = Dungeon.level;
        if (lvl == null || lvl.map == null) {
            return "nivel=(nada)";
        }
        int total = lvl.map.length;
        int libres = 0;
        for (int i = 0; i < total; i++) {
            int t = lvl.map[i];
            if (t >= 0 && t < Terrain.flags.length
                    && (Terrain.flags[t] & Terrain.SOLID) == 0) {
                libres++;
            }
        }
        return "nivel libres=" + libres + "/" + total
            + " piso=" + Dungeon.depth
            + " salida=" + lvl.exit + " entrada=" + lvl.entrance
            + " heroe=" + (Dungeon.hero != null ? Dungeon.hero.pos : -1);
    }

    /**
     * Banco de pruebas, no cosa de jugador: solo corre detras del
     * diagnostico. La prueba de bajar de piso no se puede hacer jugando
     * a ciegas desde un servidor sin GPU -- hay que encontrar la escalera
     * y caminar hasta ella. Esto le pide al juego lo mismo que un toque
     * sobre la escalera: handleCell(exit), que hace el camino solo.
     */
    public static void atajos() {
        String cmd = ordenPendiente();
        if (cmd == null || cmd.length() == 0) {
            return;
        }
        limpiarOrden();
        try {
            if ("bajar".equals(cmd)) {
                if (Dungeon.level != null) {
                    GameScene.handleCell(Dungeon.level.exit);
                    reportar("orden bajar: rumbo a " + Dungeon.level.exit);
                }
            } else if ("bajarYa".equals(cmd)) {
                // Teleporta a la escalera y la pisa. El camino no se puede
                // pedir a una casilla sin explorar, y lo que se prueba aqui
                // es la transicion de piso, no el pathfinding.
                if (Dungeon.level != null && Dungeon.hero != null) {
                    Dungeon.hero.pos = Dungeon.level.exit;
                    if (Dungeon.hero.sprite != null) {
                        Dungeon.hero.sprite.place(Dungeon.hero.pos);
                    }
                    reportar("orden bajarYa: heroe puesto en " + Dungeon.hero.pos);
                    GameScene.handleCell(Dungeon.level.exit);
                }
            } else if ("poblar".equals(cmd)) {
                // Aproxima un nivel ya jugado: bichos vivos y cosas en el
                // suelo. Un nivel recien generado guarda casi nada; lo que
                // hay que estresar es lo que se serializa de una partida
                // de verdad.
                int puestos = 0, tirados = 0;
                for (int i = 0; i < 12; i++) {
                    int c = Dungeon.level.randomRespawnCell();
                    if (c == -1) continue;
                    com.github.dachhack.sprout.actors.mobs.Mob m =
                        com.github.dachhack.sprout.actors.mobs.Bestiary.mob(
                            Dungeon.depth);
                    m.pos = c;
                    GameScene.add(m);
                    puestos++;
                }
                for (int i = 0; i < 12; i++) {
                    int c = Dungeon.level.randomRespawnCell();
                    if (c == -1) continue;
                    Dungeon.level.drop(
                        com.github.dachhack.sprout.items.Generator.random(), c);
                    tirados++;
                }
                reportar("orden poblar: " + puestos + " bichos, "
                    + tirados + " cosas, mobs=" + Dungeon.level.mobs.size()
                    + " heaps=" + Dungeon.level.heaps.size());
            } else if ("subirYa".equals(cmd)) {
                // Subir relee un nivel ya guardado: Dungeon.loadLevel, que
                // es el camino de Class.forName -- justo donde TeaVM se
                // rompe si a una clase le falta metadata.
                if (Dungeon.level != null && Dungeon.hero != null) {
                    Dungeon.hero.pos = Dungeon.level.entrance;
                    if (Dungeon.hero.sprite != null) {
                        Dungeon.hero.sprite.place(Dungeon.hero.pos);
                    }
                    reportar("orden subirYa: desde piso " + Dungeon.depth);
                    GameScene.handleCell(Dungeon.level.entrance);
                }
            } else if ("caerPaso".equals(cmd)) {
                // fall() de InterlevelScene, partido en pasos. El hilo de
                // la transicion se traga el error y solo deja el nombre;
                // TeaVM no guarda pilas. Asi se ve cual paso es.
                try {
                    com.github.dachhack.sprout.actors.Actor.fixTime();
                    reportar("paso 1 fixTime ok");
                    Dungeon.saveLevel();
                    reportar("paso 2 saveLevel ok");
                    com.github.dachhack.sprout.levels.Level lvl = Dungeon.newLevel();
                    reportar("paso 3 newLevel ok, piso=" + Dungeon.depth);
                    int celda = lvl.randomRespawnCell();
                    reportar("paso 4 randomRespawnCell=" + celda);
                    Dungeon.switchLevel(lvl, celda);
                    reportar("paso 5 switchLevel ok");
                } catch (Throwable t) {
                    reportar("PASO QUE REVENTO -> " + t);
                }
            } else if ("caer".equals(cmd)) {
                // El reporte de Leonel, tal cual: tirarse a un chasm.
                reportar("orden caer: desde " + Dungeon.hero.pos
                    + " piso " + Dungeon.depth);
                com.github.dachhack.sprout.levels.features.Chasm.heroFall(
                    Dungeon.hero.pos);
            }
        } catch (Throwable t) {
            reportar("orden " + cmd + " revento: " + t);
        }
    }

    @JSBody(script = "return window.__pdOrden || null;")
    private static native String ordenPendiente();

    @JSBody(script = "window.__pdOrden = null;")
    private static native void limpiarOrden();

    @JSBody(params = "m", script =
        "(window.__pdLog = window.__pdLog || []).push(m); console.log(m);")
    private static native void reportar(String m);
}
