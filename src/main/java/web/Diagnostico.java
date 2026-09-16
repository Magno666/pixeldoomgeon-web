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
            } else if ("soltarCarne".equals(cmd)) {
                celdaPrueba = vecinoPisable();
                if (celdaPrueba < 0) { reportar("sin vecino pisable"); return; }
                Dungeon.level.drop(
                    new com.github.dachhack.sprout.items.food.MysteryMeat(),
                    celdaPrueba);
                reportar("carne en " + celdaPrueba
                    + " heroe=" + Dungeon.hero.pos
                    + " mochila=" + mochila()
                    + " monton=" + (Dungeon.level.heaps.get(celdaPrueba) != null));
            } else if ("pisarCarne".equals(cmd)) {
                // Exactamente lo que hace el stick: un paso a la casilla.
                reportar("pisarCarne: handleCell(" + celdaPrueba + ")"
                    + " listo=" + (Dungeon.hero != null && Dungeon.hero.ready)
                    + " ventana=" + GameScene.windowOpen());
                GameScene.handleCell(celdaPrueba);
            } else if ("verMochila".equals(cmd)) {
                reportar("mochila=" + mochila()
                    + " heroe=" + Dungeon.hero.pos
                    + " celdaPrueba=" + celdaPrueba
                    + " monton sigue=" + (Dungeon.level.heaps.get(celdaPrueba) != null)
                    + " accion=" + Dungeon.hero.curAction
                    + " listo=" + Dungeon.hero.ready);
            } else if ("apuntarCarne".equals(cmd)) {
                // Poner el ojo mirando a la carne, como haria el jugador
                // antes de tocarla.
                if (celdaPrueba < 0) { reportar("no hay celda de prueba"); return; }
                apuntarA(celdaPrueba);
            } else if ("rayoCarne".equals(cmd)) {
                // Y ahora: de todos los pixeles de la pantalla, cuantos
                // devuelven la casilla de la carne. Si son cero, tocarla es
                // imposible por mas que se vea.
                if (celdaPrueba < 0) { reportar("no hay celda de prueba"); return; }
                int aciertos = 0, total = 0, centro = -1;
                float an = com.watabou.noosa.Game.width;
                float al = com.watabou.noosa.Game.height;
                for (int ix = 1; ix < 20; ix++) {
                    for (int iy = 1; iy < 20; iy++) {
                        float sx = an * ix / 20f, sy = al * iy / 20f;
                        int c = com.github.dachhack.sprout.FirstPerson
                            .screenToCell(sx, sy);
                        total++;
                        if (c == celdaPrueba) aciertos++;
                    }
                }
                centro = com.github.dachhack.sprout.FirstPerson
                    .screenToCell(an / 2f, al / 2f);
                reportar("rayoCarne: objetivo=" + celdaPrueba
                    + " centro devuelve=" + centro
                    + " aciertos=" + aciertos + "/" + total
                    + (aciertos == 0 ? "  <<< IMPOSIBLE DE TOCAR" : ""));
            } else if ("darPergamino".equals(cmd)) {
                com.github.dachhack.sprout.items.scrolls.ScrollOfUpgrade sou =
                    new com.github.dachhack.sprout.items.scrolls.ScrollOfUpgrade();
                sou.identify();
                sou.collect();
                reportar("pergamino dado. " + estadoArma());
            } else if ("leerPergamino".equals(cmd)) {
                com.github.dachhack.sprout.items.scrolls.ScrollOfUpgrade sou =
                    Dungeon.hero.belongings.getItem(
                        com.github.dachhack.sprout.items.scrolls.ScrollOfUpgrade.class);
                if (sou == null) { reportar("no hay pergamino"); return; }
                sou.execute(Dungeon.hero, "READ");
                reportar("leerPergamino: ventana abierta="
                    + GameScene.windowOpen() + ". " + estadoArma());
            } else if ("verArma".equals(cmd)) {
                reportar("verArma: " + estadoArma());
            } else if ("verEscalera".equals(cmd)) {
                // Dos casillas antes de la escalera, en la direccion donde
                // de verdad se pueda estar: si el pasillo dobla, mirar
                // desde cinco casillas atras solo enseña pared.
                int w = com.github.dachhack.sprout.levels.Level.getWidth();
                int salida = Dungeon.level.exit;
                int[] dirs = { w, -w, 1, -1 };
                int desde = -1;
                for (int d : dirs) {
                    int uno = salida + d, dos = salida + 2 * d;
                    if (dos > 0 && dos < Dungeon.level.map.length
                        && com.github.dachhack.sprout.levels.Level.passable[uno]
                        && com.github.dachhack.sprout.levels.Level.passable[dos]) {
                        desde = dos; break;
                    }
                }
                if (desde < 0) { reportar("sin sitio desde donde mirar"); return; }
                Dungeon.hero.pos = desde;
                if (Dungeon.hero.sprite != null) {
                    Dungeon.hero.sprite.place(desde);
                }
                Dungeon.observe();
                celdaPrueba = salida;
                reportar("verEscalera: salida=" + salida + " mirando desde "
                    + desde + " (apuntar en la siguiente orden)");
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

    private static int celdaPrueba = -1;

    private static int mochila() {
        return Dungeon.hero == null ? -1
            : Dungeon.hero.belongings.backpack.items.size();
    }

    /**
     * Gira el ojo para mirar a una casilla. La direccion de la vista con
     * el pixel central sale de cellFromRay: el horizontal es
     * (-sin yaw, -cos yaw) y el vertical sin(pitch), asi que el yaw que
     * apunta a (tx,tz) es atan2(-tx,-tz).
     */
    private static void apuntarA(int celda) {
        com.watabou.noosa.Camera3D cam =
            com.github.dachhack.sprout.FirstPerson.camera();
        if (cam == null) { reportar("sin camara"); return; }
        int w = com.github.dachhack.sprout.levels.Level.getWidth();
        float tx = com.github.dachhack.sprout.DungeonTilemap3D.worldX(celda, w)
            - cam.eyeX;
        float tz = com.github.dachhack.sprout.DungeonTilemap3D.worldZ(celda, w)
            - cam.eyeZ;
        float largo = (float) Math.sqrt(tx * tx + tz * tz);
        float yaw = (float) Math.toDegrees(Math.atan2(-tx, -tz));
        float alto = com.github.dachhack.sprout.Billboards.itemHeight * 0.5f;
        float pitch = (float) Math.toDegrees(
            Math.atan2(alto - cam.eyeY, largo));
        com.github.dachhack.sprout.FirstPerson.yaw = yaw;
        com.github.dachhack.sprout.FirstPerson.pitch = pitch;
        reportar("apuntarA " + celda + ": yaw=" + yaw + " pitch=" + pitch
            + " dist=" + largo + " ojo=(" + cam.eyeX + "," + cam.eyeY
            + "," + cam.eyeZ + ")");
    }

    private static String estadoArma() {
        com.github.dachhack.sprout.items.KindOfWeapon a =
            Dungeon.hero.belongings.weapon;
        return a == null ? "arma=(ninguna)"
            : "arma=" + a.getClass().getSimpleName()
              + " nivel=" + a.level + " nombre=" + a.name();
    }

    /** Una casilla vecina por la que se pueda caminar. */
    private static int vecinoPisable() {
        int w = com.github.dachhack.sprout.levels.Level.getWidth();
        int[] alrededor = { -w, w, -1, 1 };
        for (int d : alrededor) {
            int c = Dungeon.hero.pos + d;
            if (c > 0 && c < Dungeon.level.map.length
                && com.github.dachhack.sprout.levels.Level.passable[c]) {
                return c;
            }
        }
        return -1;
    }

    @JSBody(script = "return window.__pdOrden || null;")
    private static native String ordenPendiente();

    @JSBody(script = "window.__pdOrden = null;")
    private static native void limpiarOrden();

    @JSBody(params = "m", script =
        "(window.__pdLog = window.__pdLog || []).push(m); console.log(m);")
    private static native void reportar(String m);
}
