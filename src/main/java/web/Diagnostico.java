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
            + " heroe=" + (Dungeon.hero != null ? Dungeon.hero.pos : -1)
            + " listo=" + (Dungeon.hero != null && Dungeon.hero.ready)
            + " mirando=" + com.github.dachhack.sprout.FirstPerson.facing8()
            + " vecinos=" + vecinos()
            + " " + com.github.dachhack.sprout.FirstPersonControls.estadoTeclas();
    }

    /**
     * Banco de pruebas, no cosa de jugador: solo corre detras del
     * diagnostico. La prueba de bajar de piso no se puede hacer jugando
     * a ciegas desde un servidor sin GPU -- hay que encontrar la escalera
     * y caminar hasta ella. Esto le pide al juego lo mismo que un toque
     * sobre la escalera: handleCell(exit), que hace el camino solo.
     */
    // --- muestreo del paso ---
    private static float[] alturas = null;
    private static float[] avances = null;
    private static float[] lados = null;
    private static int muestra = 0;

    /** Guarda la altura del ojo y el avance, un dato por cuadro. Es la
     *  unica forma de saber si el "salto" que se ve al caminar es el
     *  cabeceo o la interpolacion del desplazamiento. */
    private static void muestrear() {
        if (alturas == null || muestra >= alturas.length) return;
        com.watabou.noosa.Camera3D cam =
            com.github.dachhack.sprout.FirstPerson.camera();
        if (cam == null) return;
        alturas[muestra] = cam.eyeY;
        // eyeX a secas, no la distancia al origen: el balanceo lateral es
        // una desviacion perpendicular al avance, y una distancia al origen
        // la esconde.
        avances[muestra] = cam.eyeX;
        lados[muestra] = cam.eyeZ;
        muestra++;
    }

    public static void atajos() {
        muestrear();
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
                    moverHeroe(Dungeon.level.exit);
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
                    moverHeroe(Dungeon.level.entrance);
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
            } else if ("marcarBicho".equals(cmd)) {
                com.github.dachhack.sprout.actors.mobs.Mob cerca = null;
                int mejorD = Integer.MAX_VALUE;
                for (com.github.dachhack.sprout.actors.mobs.Mob m
                        : Dungeon.level.mobs.toArray(
                            new com.github.dachhack.sprout.actors.mobs.Mob[0])) {
                    if (!com.github.dachhack.sprout.levels.Level.fieldOfView[m.pos]) continue;
                    int d = Math.abs(m.pos - Dungeon.hero.pos);
                    if (d < mejorD) { mejorD = d; cerca = m; }
                }
                if (cerca == null) { reportar("marcarBicho: no veo ninguno"); return; }
                celdaPrueba = cerca.pos;
                reportar("marcarBicho: " + cerca.getClass().getSimpleName()
                    + " en " + cerca.pos + " heroe=" + Dungeon.hero.pos);
            } else if ("marcarJefe".equals(cmd)) {
                com.github.dachhack.sprout.actors.mobs.Mob j2 = jefe();
                if (j2 == null) { reportar("marcarJefe: no hay jefe"); return; }
                celdaPrueba = j2.pos;
                reportar("marcarJefe: " + j2.getClass().getSimpleName()
                    + " en " + j2.pos);
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
                moverHeroe(desde);
                Dungeon.observe();
                celdaPrueba = salida;
                reportar("verEscalera: salida=" + salida + " mirando desde "
                    + desde + " (apuntar en la siguiente orden)");
            } else if ("reubicar".equals(cmd)) {
                int c = Dungeon.level.randomRespawnCell();
                if (c < 0) { reportar("sin sitio"); return; }
                moverHeroe(c);Dungeon.observe();
                reportar("reubicar: heroe a " + c
                    + " (entrada=" + Dungeon.level.entrance
                    + " salida=" + Dungeon.level.exit + ")");
            } else if ("reubicarAbierto".equals(cmd)) {
                // Una casilla con las CUATRO direcciones locales libres y
                // sin bicho encima, para que "no se movio" solo pueda
                // significar que el teclado fallo.
                int w = com.github.dachhack.sprout.levels.Level.getWidth();
                int elegida = -1;
                for (int intento = 0; intento < 400 && elegida < 0; intento++) {
                    int c = Dungeon.level.randomRespawnCell();
                    if (c < 0) continue;
                    boolean todas = true;
                    for (int d = 0; d < 8; d += 2) {
                        int v = com.github.dachhack.sprout.FirstPerson
                            .neighbour(c, d, w);
                        if (v < 0
                            || !com.github.dachhack.sprout.levels.Level.passable[v]
                            || com.github.dachhack.sprout.actors.Actor.findChar(v) != null) {
                            todas = false; break;
                        }
                    }
                    if (todas && c != Dungeon.level.entrance
                        && c != Dungeon.level.exit) elegida = c;
                }
                if (elegida < 0) { reportar("sin casilla abierta"); return; }
                celdaPrueba = elegida;
                moverHeroe(elegida);Dungeon.observe();
                reportar("reubicarAbierto: heroe a " + elegida + " " + vecinos());
            } else if ("volverAbierto".equals(cmd)) {
                if (celdaPrueba < 0) { reportar("sin casilla"); return; }
                moverHeroe(celdaPrueba);
                Dungeon.observe();
            } else if ("pelear".equals(cmd)) {
                // Un bicho pegado al heroe y a darse. Lo que se mira no es
                // quien gana sino que los turnos sigan corriendo: que el
                // heroe pegue, que el bicho pegue, y que nadie se quede
                // esperando a nadie.
                int w = com.github.dachhack.sprout.levels.Level.getWidth();
                int donde = -1;
                for (int d = 0; d < 8 && donde < 0; d += 2) {
                    int v = com.github.dachhack.sprout.FirstPerson
                        .neighbour(Dungeon.hero.pos, d, w);
                    if (v >= 0 && com.github.dachhack.sprout.levels.Level.passable[v]
                        && com.github.dachhack.sprout.actors.Actor.findChar(v) == null) {
                        donde = v;
                    }
                }
                if (donde < 0) { reportar("pelear: sin sitio al lado"); return; }
                com.github.dachhack.sprout.actors.mobs.Mob m =
                    com.github.dachhack.sprout.actors.mobs.Bestiary.mob(Dungeon.depth);
                m.pos = donde;
                GameScene.add(m);
                Dungeon.observe();
                reportar("pelear: " + m.getClass().getSimpleName()
                    + " en " + donde + " hp=" + m.HP + "/" + m.HT
                    + " heroeHP=" + Dungeon.hero.HP + "/" + Dungeon.hero.HT);
                GameScene.handleCell(donde);
            } else if ("atacar".equals(cmd)) {
                // El bicho visible mas cercano, y golpear. Igual que tocarlo
                // en pantalla.
                com.github.dachhack.sprout.actors.mobs.Mob blanco = null;
                int mejor = Integer.MAX_VALUE;
                for (com.github.dachhack.sprout.actors.mobs.Mob m
                        : Dungeon.level.mobs.toArray(
                            new com.github.dachhack.sprout.actors.mobs.Mob[0])) {
                    if (!com.github.dachhack.sprout.levels.Level
                            .fieldOfView[m.pos]) continue;
                    int d = Math.abs(m.pos - Dungeon.hero.pos);
                    if (d < mejor) { mejor = d; blanco = m; }
                }
                if (blanco == null) { reportar("atacar: no veo a nadie"); return; }
                reportar("atacar: " + blanco.getClass().getSimpleName()
                    + " en " + blanco.pos + " hp=" + blanco.HP
                    + " heroeHP=" + Dungeon.hero.HP
                    + " listo=" + Dungeon.hero.ready);
                GameScene.handleCell(blanco.pos);
            } else if ("verPelea".equals(cmd)) {
                StringBuilder sb = new StringBuilder("verPelea: heroeHP="
                    + Dungeon.hero.HP + "/" + Dungeon.hero.HT
                    + " vivo=" + Dungeon.hero.isAlive()
                    + " listo=" + Dungeon.hero.ready + " bichos=");
                for (com.github.dachhack.sprout.actors.mobs.Mob m
                        : Dungeon.level.mobs.toArray(
                            new com.github.dachhack.sprout.actors.mobs.Mob[0])) {
                    sb.append(m.getClass().getSimpleName())
                      .append(':').append(m.HP).append(' ');
                }
                com.github.dachhack.sprout.actors.mobs.Mob j = jefe();
                if (j != null) {
                    int w5 = com.github.dachhack.sprout.levels.Level.getWidth();
                    int dx = Math.abs(j.pos % w5 - Dungeon.hero.pos % w5);
                    int dy = Math.abs(j.pos / w5 - Dungeon.hero.pos / w5);
                    sb.append(" || JEFE ").append(j.getClass().getSimpleName())
                      .append(" en ").append(j.pos)
                      .append(" heroe ").append(Dungeon.hero.pos)
                      .append(" dist=").append(Math.max(dx, dy))
                      .append(" visible=").append(Dungeon.visible[j.pos])
                      .append(" enVista=").append(
                          com.github.dachhack.sprout.levels.Level.fieldOfView[j.pos])
                      .append(" sprite=").append(j.sprite != null)
                      .append(" yaw=").append(
                          com.github.dachhack.sprout.FirstPerson.yaw);
                }
                reportar(sb.toString());
            } else if ("generarTodos".equals(cmd)) {
                // Genera un piso de cada profundidad con el generador real.
                // Es lo unico que prueba que TeaVM no se comio el pintor de
                // algun cuarto que solo sale en la Prision o en la Ciudad:
                // un nivel que no se puede pintar no truena, sale vacio.
                //
                // DESTRUCTIVO: newLevel() hace Actor.clear() y deja
                // Dungeon.level en null. La partida no sobrevive, por eso
                // esta orden va al final de una corrida.
                // Por tandas: generar treinta niveles de un tiron mata la
                // pestana en un servidor sin GPU, y se pierde todo lo que
                // ya se habia probado. Cada llamada avanza seis.
                if (pisoGen == 0) { Dungeon.depth = 0; }
                int malos = 0;
                com.github.dachhack.sprout.levels.Level ultimo = null;
                for (int i = 0; i < 5 && pisoGen < 30; i++, pisoGen++) {
                    long t0 = System.currentTimeMillis();
                    try {
                        com.github.dachhack.sprout.levels.Level l =
                            Dungeon.newLevel();
                        ultimo = l;
                        int libres = 0;
                        for (int c = 0; c < l.map.length; c++) {
                            int t = l.map[c];
                            if (t >= 0 && t < com.github.dachhack.sprout.levels
                                    .Terrain.flags.length
                                && (com.github.dachhack.sprout.levels.Terrain
                                    .flags[t] & com.github.dachhack.sprout.levels
                                    .Terrain.SOLID) == 0) libres++;
                        }
                        boolean pobre = libres < 60;
                        if (pobre) malos++;
                        reportar("piso " + Dungeon.depth + " "
                            + l.getClass().getSimpleName()
                            + " libres=" + libres
                            + " entrada=" + l.entrance + " salida=" + l.exit
                            + " " + (System.currentTimeMillis() - t0) + "ms"
                            + (pobre ? "   <<< SOSPECHOSO" : ""));
                    } catch (Throwable t) {
                        malos++;
                        reportar("piso " + Dungeon.depth + " REVENTO tras "
                            + (System.currentTimeMillis() - t0) + "ms: " + t);
                    }
                }
                // newLevel() deja Dungeon.level en null y Actor.clear()
                // se lleva hasta al heroe: sin esto el siguiente cuadro
                // revienta, el bucle se detiene y la orden que viene nunca
                // llega a ejecutarse -- que es justo por que la segunda
                // tanda "no terminaba".
                if (ultimo != null) {
                    Dungeon.switchLevel(ultimo, ultimo.entrance);
                }
                reportar("generarTodos: tanda hasta piso " + Dungeon.depth
                    + ", sospechosos/rotos en la tanda=" + malos
                    + (pisoGen >= 30 ? "  TERMINADO" : ""));
            } else if ("medirPaso".equals(cmd)) {
                alturas = new float[240];
                avances = new float[240];
                lados = new float[240];
                muestra = 0;
                reportar("medirPaso: grabando " + alturas.length + " cuadros");
            } else if ("verPaso".equals(cmd)) {
                if (alturas == null || muestra < 3) { reportar("verPaso: sin datos"); return; }
                float min = alturas[0], max = alturas[0];
                for (int i = 0; i < muestra; i++) {
                    if (alturas[i] < min) min = alturas[i];
                    if (alturas[i] > max) max = alturas[i];
                }
                StringBuilder sb = new StringBuilder("verPaso: cuadros=" + muestra
                    + " cabeceo=" + (max - min) + " (min " + min + " max " + max + ")\n   alturas:");
                for (int i = 0; i < muestra; i += 2) {
                    sb.append(' ').append(Math.round((alturas[i] - min) * 1000f));
                }
                sb.append("\n   eyeX:");
                float base = avances[0];
                for (int i = 0; i < muestra; i += 2) {
                    sb.append(' ').append(Math.round((avances[i] - base) * 100f));
                }
                sb.append("\n   eyeZ:");
                float baseZ = lados[0];
                for (int i = 0; i < muestra; i += 2) {
                    sb.append(' ').append(Math.round((lados[i] - baseZ) * 100f));
                }
                reportar(sb.toString());
                alturas = null;
            } else if ("marear".equals(cmd)) {
                com.github.dachhack.sprout.actors.buffs.Buff.affect(
                    Dungeon.hero,
                    com.github.dachhack.sprout.actors.buffs.Vertigo.class, 30f);
                reportar("marear: vertigo puesto, "
                    + (Dungeon.hero.buff(
                        com.github.dachhack.sprout.actors.buffs.Vertigo.class) != null));
            } else if ("verMareo".equals(cmd)) {
                com.watabou.noosa.Camera3D cam =
                    com.github.dachhack.sprout.FirstPerson.camera();
                reportar("verMareo: vertigo="
                    + (Dungeon.hero.buff(
                        com.github.dachhack.sprout.actors.buffs.Vertigo.class) != null)
                    + " yawLogico=" + com.github.dachhack.sprout.FirstPerson.yaw
                    + " yawCamara=" + (cam != null ? cam.yaw : -999f)
                    + " pitchCamara=" + (cam != null ? cam.pitch : -999f));
            } else if ("matarConBicho".equals(cmd)) {
                // Muerte por el camino real: Char.damage llama a
                // Dungeon.fail y ahi es donde se escribe la causa. Matar al
                // heroe a mano con die(null) se salta eso y la causa sale
                // vacia -- lo que me hizo creer que faltaba el dato.
                com.github.dachhack.sprout.actors.mobs.Mob m =
                    com.github.dachhack.sprout.actors.mobs.Bestiary.mob(Dungeon.depth);
                int w9 = com.github.dachhack.sprout.levels.Level.getWidth();
                for (int d = 0; d < 8; d++) {
                    int v = com.github.dachhack.sprout.FirstPerson
                        .neighbour(Dungeon.hero.pos, d, w9);
                    if (v >= 0 && com.github.dachhack.sprout.levels.Level.passable[v]
                        && com.github.dachhack.sprout.actors.Actor.findChar(v) == null) {
                        m.pos = v; GameScene.add(m); break;
                    }
                }
                Dungeon.observe();
                reportar("matarConBicho: " + m.getClass().getSimpleName());
                // Por attack(), no por damage(): Dungeon.fail -- que es
                // donde se escribe la causa -- vive dentro de attack, no
                // dentro de damage. Pegando daño directo la causa se queda
                // en null y parece que la telemetria no la recoge.
                Dungeon.hero.HP = 1;
                for (int i = 0; i < 12 && Dungeon.hero.isAlive(); i++) {
                    m.attack(Dungeon.hero);
                }
                reportar("   vivo=" + Dungeon.hero.isAlive()
                    + " causa quedo: " + Dungeon.resultDescription);
            } else if ("morirme".equals(cmd)) {
                reportar("morirme: piso " + Dungeon.depth
                    + " hp " + Dungeon.hero.HP);
                Dungeon.hero.die(null);
            } else if ("invertirX".equals(cmd)) {
                com.github.dachhack.sprout.ShatteredPixelDungeon.invertX(
                    !com.github.dachhack.sprout.ShatteredPixelDungeon.invertX());
                reportar("invertirX ahora = "
                    + com.github.dachhack.sprout.ShatteredPixelDungeon.invertX());
            } else if ("ajustes".equals(cmd)) {
                GameScene.show(
                    new com.github.dachhack.sprout.windows.WndSettings(true));
                reportar("ajustes abiertos");
            } else if ("curar".equals(cmd)) {
                // Caerse hace daño de verdad; sin esto el heroe se muere a
                // la tercera y la prueba de memoria se acaba antes de decir
                // nada.
                Dungeon.hero.HP = Dungeon.hero.HT;
                reportar("curar: " + Dungeon.hero.HP + "/" + Dungeon.hero.HT);
            } else if ("buscarJefe".equals(cmd)) {
                com.github.dachhack.sprout.actors.mobs.Mob jefe = jefe();
                if (jefe == null) { reportar("buscarJefe: no hay jefe en el piso " + Dungeon.depth); return; }
                // Pegarse al jefe y aguantar el golpe: la pelea se prueba
                // sola, no se juega bien.
                int w = com.github.dachhack.sprout.levels.Level.getWidth();
                int alLado = -1;
                for (int d = 0; d < 8 && alLado < 0; d++) {
                    int v = com.github.dachhack.sprout.FirstPerson.neighbour(jefe.pos, d, w);
                    if (v >= 0 && com.github.dachhack.sprout.levels.Level.passable[v]
                        && com.github.dachhack.sprout.actors.Actor.findChar(v) == null) alLado = v;
                }
                if (alLado < 0) { reportar("buscarJefe: sin sitio al lado"); return; }
                Dungeon.hero.HT = 2000; Dungeon.hero.HP = 2000;
                // Un guerrero de nivel 1 no le hace cosquillas a un jefe de
                // 200 de vida que ademas se cura en el agua: la prueba se
                // acabaria por aburrimiento, no por resultado.
                if (Dungeon.hero.belongings.weapon != null) {
                    for (int u = 0; u < 20; u++) {
                        Dungeon.hero.belongings.weapon.upgrade();
                    }
                }
                moverHeroe(alLado);Dungeon.observe();
                reportar("buscarJefe: " + jefe.getClass().getSimpleName()
                    + " en " + jefe.pos + " hp=" + jefe.HP + "/" + jefe.HT
                    + ", heroe al lado en " + alLado + " con " + Dungeon.hero.HP + " hp");
            } else if ("golpearJefe".equals(cmd)) {
                com.github.dachhack.sprout.actors.mobs.Mob jefe = jefe();
                if (jefe == null) {
                    reportar("golpearJefe: ya no hay jefe -- piso=" + Dungeon.depth
                        + " heroeHP=" + Dungeon.hero.HP
                        + " salida=" + Dungeon.level.map[Dungeon.level.exit]);
                    return;
                }
                Dungeon.hero.HP = Dungeon.hero.HT;   // que no se muera a medias
                // Pegarse al jefe cada turno. Sin esto el heroe intenta
                // CAMINAR hasta el, y en cuanto una telaraña lo atrapa el
                // registro se llena de "You can't move!" y la prueba mide
                // el pathfinding en vez de la pelea.
                int w2 = com.github.dachhack.sprout.levels.Level.getWidth();
                for (int d = 0; d < 8; d++) {
                    int v = com.github.dachhack.sprout.FirstPerson
                        .neighbour(jefe.pos, d, w2);
                    if (v >= 0 && com.github.dachhack.sprout.levels.Level.passable[v]
                        && com.github.dachhack.sprout.actors.Actor.findChar(v) == null) {
                        moverHeroe(v);break;
                    }
                }
                Dungeon.observe();
                reportar("golpeJefe " + jefe.getClass().getSimpleName()
                    + " hp=" + jefe.HP + "/" + jefe.HT
                    + " heroeHP=" + Dungeon.hero.HP
                    + " manchas=" + Dungeon.level.blobs.size()
                    + " bichos=" + Dungeon.level.mobs.size());
                GameScene.handleCell(jefe.pos);
            } else if ("tomarLlave".equals(cmd)) {
                // La salida del piso de jefe se abre con la SkeletonKey que
                // suelta el jefe al morir, no sola: LOCKED_EXIT + Unlock es
                // el diseño del original.
                // heaps es SparseArray, no Map: se recorre por indice.
                int donde = -1;
                for (int i = 0; i < Dungeon.level.heaps.size() && donde < 0; i++) {
                    com.github.dachhack.sprout.items.Heap h =
                        Dungeon.level.heaps.valueAt(i);
                    if (h == null) continue;
                    for (com.github.dachhack.sprout.items.Item it : h.items) {
                        if (it instanceof com.github.dachhack.sprout.items.keys.SkeletonKey) {
                            donde = Dungeon.level.heaps.keyAt(i); break;
                        }
                    }
                }
                if (donde < 0) { reportar("tomarLlave: no hay SkeletonKey en el suelo"); return; }
                moverHeroe(donde);Dungeon.observe();
                // Dos caminos, para saber en cual se pierde: handleCell pasa
                // por CellSelector (que tiene enabled/listener propios) y
                // hero.handle va derecho a la logica.
                reportar("tomarLlave: llave en " + donde
                    + " apuntando=" + GameScene.targeting()
                    + " -- probando handleCell");
                GameScene.handleCell(donde);
            } else if ("alLadoDeLaLlave".equals(cmd)) {
                // Dejar al heroe junto a la llave y MIRANDOLA, para que la
                // recoja caminando con el teclado: el camino de un jugador,
                // sin teletransportes ni llamadas directas a la logica.
                int donde = -1;
                for (int i = 0; i < Dungeon.level.heaps.size() && donde < 0; i++) {
                    com.github.dachhack.sprout.items.Heap h =
                        Dungeon.level.heaps.valueAt(i);
                    if (h == null) continue;
                    for (com.github.dachhack.sprout.items.Item it : h.items) {
                        if (it instanceof com.github.dachhack.sprout.items.keys.SkeletonKey) {
                            donde = Dungeon.level.heaps.keyAt(i); break;
                        }
                    }
                }
                if (donde < 0) { reportar("alLadoDeLaLlave: no hay llave"); return; }
                int w3 = com.github.dachhack.sprout.levels.Level.getWidth();
                int[] lados = { -w3, w3, -1, 1 };
                int puesto = -1;
                for (int d : lados) {
                    int c = donde + d;
                    if (c > 0 && c < Dungeon.level.map.length
                        && com.github.dachhack.sprout.levels.Level.passable[c]
                        && com.github.dachhack.sprout.actors.Actor.findChar(c) == null) {
                        puesto = c; break;
                    }
                }
                if (puesto < 0) { reportar("alLadoDeLaLlave: sin hueco al lado"); return; }
                moverHeroe(puesto);Dungeon.observe();
                celdaPrueba = donde;
                reportar("alLadoDeLaLlave: llave en " + donde
                    + ", heroe en " + puesto + " (apuntar en la siguiente orden)");
            } else if ("probarPaso".equals(cmd)) {
                // Que acción decide el juego para la casilla de la llave.
                // Move significa que el monton no se vio; PickUp significa
                // que se creo y algo la cancelo despues.
                int c = celdaPrueba;
                com.github.dachhack.sprout.items.Heap h = Dungeon.level.heaps.get(c);
                com.github.dachhack.sprout.actors.Char quien =
                    com.github.dachhack.sprout.actors.Actor.findChar(c);
                reportar("probarPaso: celda=" + c
                    + " monton=" + (h != null ? h.type.toString() : "no")
                    + " vacio=" + (h != null && h.isEmpty())
                    + " ocupada por=" + (quien == null ? "nadie" : quien.getClass().getSimpleName())
                    + " enVista=" + com.github.dachhack.sprout.levels.Level.fieldOfView[c]
                    + " heroe=" + Dungeon.hero.pos
                    + " listo=" + Dungeon.hero.ready);
                GameScene.handleCell(c);
                reportar("   tras handleCell: accion=" + Dungeon.hero.curAction
                    + " heroe=" + Dungeon.hero.pos);
            } else if ("alLadoDeLaSalida".equals(cmd)) {
                int sal = Dungeon.level.exit;
                int w4 = com.github.dachhack.sprout.levels.Level.getWidth();
                int[] lados = { -w4, w4, -1, 1 };
                int puesto = -1;
                for (int d : lados) {
                    int c = sal + d;
                    if (c > 0 && c < Dungeon.level.map.length
                        && com.github.dachhack.sprout.levels.Level.passable[c]
                        && com.github.dachhack.sprout.actors.Actor.findChar(c) == null) {
                        puesto = c; break;
                    }
                }
                if (puesto < 0) { reportar("alLadoDeLaSalida: sin hueco"); return; }
                moverHeroe(puesto);Dungeon.observe();
                celdaPrueba = sal;
                reportar("alLadoDeLaSalida: salida=" + sal + " heroe=" + puesto
                    + " terreno=" + Dungeon.level.map[sal]);
            } else if ("verTuberia".equals(cmd)) {
                // Se consulta APARTE de alLadoDeUnaTuberia: el estado que se
                // lea en el mismo cuadro del teletransporte es el de antes
                // de mover, porque los adornos se encienden en su propio
                // update y eso ocurre al cuadro siguiente.
                reportar("verTuberia: celda=" + celdaPrueba
                    + " visible=" + (celdaPrueba >= 0
                        && com.github.dachhack.sprout.Dungeon.visible[celdaPrueba])
                    + " adornosVisibles="
                    + com.github.dachhack.sprout.Adornos.visibles()
                    + " de " + com.github.dachhack.sprout.Adornos.cuantos()
                    + " | " + com.github.dachhack.sprout.Adornos.comoEsta(celdaPrueba));
            } else if ("tocarArbusto".equals(cmd)) {
                // Planta al heroe mirando a una mata de hierba alta y toca
                // el centro de la pantalla. Antes el rayo la atravesaba y
                // devolvia una casilla de mas atras, asi que el heroe se
                // iba por el pasillo a darle la vuelta.
                int wA = com.github.dachhack.sprout.levels.Level.getWidth();
                int mata = -1, puestoA = -1, dirA = 0, lejos = 0;
                int[] ladosA = { wA, -wA, 1, -1 };
                for (int c = 0; c < Dungeon.level.map.length && lejos < 4; c++) {
                    if (Dungeon.level.map[c]
                            != com.github.dachhack.sprout.levels.Terrain.HIGH_GRASS) {
                        continue;
                    }
                    for (int d : ladosA) {
                        // Hay que poder ver la mata Y que haya suelo detras,
                        // que es lo unico que el rayo podria confundir.
                        int detras = c - d;
                        if (detras < 0 || detras >= Dungeon.level.map.length
                            || !com.github.dachhack.sprout.levels.Level.passable[detras]) {
                            continue;
                        }
                        int v = c + d, pasos = 0;
                        while (pasos < 4) {
                            if (v < 0 || v >= Dungeon.level.map.length
                                || !com.github.dachhack.sprout.levels.Level.passable[v]
                                || com.github.dachhack.sprout.actors.Actor.findChar(v) != null) {
                                break;
                            }
                            pasos++;
                            int sig = v + d;
                            if (pasos >= 3) { break; }
                            v = sig;
                        }
                        if (pasos >= 2 && pasos > lejos) {
                            mata = c; puestoA = v; dirA = d; lejos = pasos;
                        }
                    }
                }
                if (mata < 0) { reportar("tocarArbusto: no hay hierba con hueco"); return; }
                moverHeroe(puestoA);
                Dungeon.observe();
                com.github.dachhack.sprout.FirstPerson.faceCell(mata);
                celdaPrueba = mata;
                reportar("tocarArbusto: mata=" + mata + " heroe=" + puestoA
                    + " a " + lejos + " pasos (mira el centro y pregunta con verArbusto)");
            } else if ("enderezarToque".equals(cmd)) {
                // Pide un camino TOCANDO una casilla al norte y reporta el
                // yaw antes y despues. Tiene que acabar mirando al norte (0)
                // aunque se empiece mirando a otro lado.
                int wE = com.github.dachhack.sprout.levels.Level.getWidth();
                int destino = -1;
                for (int k = 3; k >= 1 && destino < 0; k--) {
                    int c = Dungeon.hero.pos - wE * k;
                    if (c > 0 && com.github.dachhack.sprout.levels.Level.passable[c]
                        && com.github.dachhack.sprout.actors.Actor.findChar(c) == null) {
                        destino = c;
                    }
                }
                if (destino < 0) { reportar("enderezarToque: sin hueco al norte"); return; }
                com.github.dachhack.sprout.FirstPerson.yaw = 90f;   // mirando al oeste
                com.github.dachhack.sprout.FirstPerson.caminoPorToque();
                GameScene.handleCell(destino);
                reportar("enderezarToque: heroe=" + Dungeon.hero.pos
                    + " destino=" + destino + " yaw de salida=90 (oeste)");
            } else if ("enderezarMando".equals(cmd)) {
                int wM = com.github.dachhack.sprout.levels.Level.getWidth();
                int destinoM = -1;
                for (int k = 3; k >= 1 && destinoM < 0; k--) {
                    int c = Dungeon.hero.pos - wM * k;
                    if (c > 0 && com.github.dachhack.sprout.levels.Level.passable[c]
                        && com.github.dachhack.sprout.actors.Actor.findChar(c) == null) {
                        destinoM = c;
                    }
                }
                if (destinoM < 0) { reportar("enderezarMando: sin hueco al norte"); return; }
                com.github.dachhack.sprout.FirstPerson.yaw = 90f;
                com.github.dachhack.sprout.FirstPerson.caminoPorMando();
                GameScene.handleCell(destinoM);
                reportar("enderezarMando: heroe=" + Dungeon.hero.pos
                    + " destino=" + destinoM + " yaw de salida=90 (oeste)");
            } else if ("enderezarApagado".equals(cmd)) {
                // La casilla de Ajustes de verdad lo apaga: mismo caso del
                // toque pero con la preferencia en false.
                com.github.dachhack.sprout.ShatteredPixelDungeon.enderezar(false);
                int wZ = com.github.dachhack.sprout.levels.Level.getWidth();
                int destZ = -1;
                for (int k = 3; k >= 1 && destZ < 0; k--) {
                    int c = Dungeon.hero.pos - wZ * k;
                    if (c > 0 && com.github.dachhack.sprout.levels.Level.passable[c]
                        && com.github.dachhack.sprout.actors.Actor.findChar(c) == null) {
                        destZ = c;
                    }
                }
                if (destZ < 0) { reportar("enderezarApagado: sin hueco"); return; }
                com.github.dachhack.sprout.FirstPerson.yaw = 90f;
                com.github.dachhack.sprout.FirstPerson.caminoPorToque();
                GameScene.handleCell(destZ);
                reportar("enderezarApagado: ajuste="
                    + com.github.dachhack.sprout.FirstPerson.enderezarAlCaminar
                    + " destino=" + destZ + " yaw de salida=90");
            } else if ("enderezarEncendido".equals(cmd)) {
                com.github.dachhack.sprout.ShatteredPixelDungeon.enderezar(true);
                reportar("enderezarEncendido: ajuste="
                    + com.github.dachhack.sprout.FirstPerson.enderezarAlCaminar);
            } else if ("verYaw".equals(cmd)) {
                reportar("verYaw: yaw=" + (int) com.github.dachhack.sprout.FirstPerson.yaw
                    + " heroe=" + Dungeon.hero.pos
                    + " ajuste enderezar="
                    + com.github.dachhack.sprout.FirstPerson.enderezarAlCaminar);
            } else if ("bichosEnElMapa".equals(cmd)) {
                // Cuantos bichos ve el heroe y cuantos puntos pinta el mapa.
                // Tienen que coincidir: ni de mas (seria hacer trampa) ni
                // de menos (seria no contestar a lo que pidieron).
                // Traer bichos al lado: sin ninguno a la vista la prueba
                // pasa sola sin comprobar nada.
                int wB = com.github.dachhack.sprout.levels.Level.getWidth();
                int[] ladosB = { -wB, wB, -1, 1, -wB-1, -wB+1, wB-1, wB+1 };
                int traidos = 0;
                for (com.github.dachhack.sprout.actors.mobs.Mob m
                        : Dungeon.level.mobs.toArray(
                            new com.github.dachhack.sprout.actors.mobs.Mob[0])) {
                    if (traidos >= 3) { break; }
                    for (int d : ladosB) {
                        int c = Dungeon.hero.pos + d;
                        if (c > 0 && c < Dungeon.level.map.length
                            && com.github.dachhack.sprout.levels.Level.passable[c]
                            && com.github.dachhack.sprout.actors.Actor.findChar(c) == null) {
                            com.github.dachhack.sprout.actors.Actor.freeCell(m.pos);
                            m.pos = c;
                            com.github.dachhack.sprout.actors.Actor.occupyCell(m);
                            if (m.sprite != null) { m.sprite.place(c); }
                            traidos++;
                            break;
                        }
                    }
                }
                Dungeon.observe();
                com.github.dachhack.sprout.Minimap.update();

                int aLaVista = 0;
                for (com.github.dachhack.sprout.actors.mobs.Mob m
                        : Dungeon.level.mobs) {
                    if (m != null && m.pos >= 0
                            && m.pos < Dungeon.level.map.length
                            && Dungeon.visible[m.pos]) {
                        aLaVista++;
                    }
                }
                reportar("bichosEnElMapa: traidos=" + traidos
                    + " a la vista=" + aLaVista
                    + " puntos pintados=" + com.github.dachhack.sprout.Minimap.puntosBicho()
                    + " bichos en el nivel=" + Dungeon.level.mobs.size()
                    + (aLaVista == com.github.dachhack.sprout.Minimap.puntosBicho()
                        ? "  CUADRA" : "  NO CUADRA"));
            } else if ("dentroDeLaHierba".equals(cmd)) {
                // El caso que casi se rompe: parado DENTRO de una mata. Si
                // la casilla del ojo tapase, cualquier toque devolveria la
                // propia y no se podria caminar tocando -- que es lo que
                // pasaba de verdad parado en una puerta.
                int wH = com.github.dachhack.sprout.levels.Level.getWidth();
                int mataH = -1;
                for (int c = 0; c < Dungeon.level.map.length; c++) {
                    if (Dungeon.level.map[c]
                            == com.github.dachhack.sprout.levels.Terrain.HIGH_GRASS
                        && com.github.dachhack.sprout.actors.Actor.findChar(c) == null) {
                        mataH = c; break;
                    }
                }
                if (mataH < 0) { reportar("dentroDeLaHierba: no hay"); return; }
                moverHeroe(mataH);
                Dungeon.observe();
                com.watabou.noosa.Camera3D cH =
                    com.github.dachhack.sprout.FirstPerson.camera();
                StringBuilder sh = new StringBuilder();
                boolean soloYo = true;
                for (int k = 0; k <= 6; k++) {
                    int celda = com.github.dachhack.sprout.FirstPerson.screenToCell(
                        com.watabou.noosa.Game.width / 2f,
                        com.watabou.noosa.Game.height * (0.62f - k * 0.015f));
                    sh.append(celda).append(' ');
                    if (celda >= 0 && celda != mataH) { soloYo = false; }
                }
                reportar("dentroDeLaHierba: heroe en la mata " + mataH
                    + " terreno=" + Dungeon.level.map[mataH]
                    + " barrido=[" + sh.toString().trim() + "]"
                    + (soloYo ? "  ROTO: todo cae en su propia casilla"
                              : "  BIEN: se puede tocar fuera"));
            } else if ("verArbusto".equals(cmd)) {
                // Se prueba cellFromRay a pelo, con las dos mascaras, para
                // que el resultado no dependa de billboards ni de donde
                // quedo el heroe: es la funcion que cambio y nada mas.
                com.watabou.noosa.Camera3D c3 =
                    com.github.dachhack.sprout.FirstPerson.camera();
                int wV = com.github.dachhack.sprout.levels.Level.getWidth();
                int filas = Dungeon.level.map.length / wV;
                boolean[] vieja = new boolean[Dungeon.level.map.length];
                boolean[] nueva = new boolean[Dungeon.level.map.length];
                for (int i = 0; i < vieja.length; i++) {
                    vieja[i] = com.github.dachhack.sprout.levels.Level.solid[i];
                    nueva[i] = com.github.dachhack.sprout.levels.Level.solid[i]
                        || com.github.dachhack.sprout.levels.Level.losBlocking[i];
                }
                StringBuilder sa = new StringBuilder(), sb = new StringBuilder();
                for (int k = 0; k <= 8; k++) {
                    float fy = 0.60f - k * 0.012f;
                    float px = com.watabou.noosa.Game.width / 2f;
                    float py = com.watabou.noosa.Game.height * fy;
                    sa.append(com.github.dachhack.sprout.FirstPerson.cellFromRay(
                        px, py, com.watabou.noosa.Game.width,
                        com.watabou.noosa.Game.height, c3.fovY(), c3.aspect(),
                        c3.eyeX, c3.eyeY, c3.eyeZ, c3.yaw, c3.pitch,
                        wV, filas, vieja)).append(' ');
                    sb.append(com.github.dachhack.sprout.FirstPerson.cellFromRay(
                        px, py, com.watabou.noosa.Game.width,
                        com.watabou.noosa.Game.height, c3.fovY(), c3.aspect(),
                        c3.eyeX, c3.eyeY, c3.eyeZ, c3.yaw, c3.pitch,
                        wV, filas, nueva)).append(' ');
                }
                // El terreno de la linea entre el heroe y la mata, para no
                // tener que suponer que hay en medio.
                StringBuilder linea = new StringBuilder();
                int paso = celdaPrueba > Dungeon.hero.pos ? wV : -wV;
                if (Math.abs(celdaPrueba - Dungeon.hero.pos) % wV != 0) {
                    paso = celdaPrueba > Dungeon.hero.pos ? 1 : -1;
                }
                for (int c = Dungeon.hero.pos; ; c += paso) {
                    linea.append(c).append(':')
                         .append(Dungeon.level.map[c] ==
                             com.github.dachhack.sprout.levels.Terrain.HIGH_GRASS
                             ? "hierba" : String.valueOf(Dungeon.level.map[c]))
                         .append(' ');
                    if (c == celdaPrueba) { break; }
                }
                reportar("verArbusto: mata=" + celdaPrueba
                    + " heroe=" + Dungeon.hero.pos
                    + "\n   en la linea: " + linea.toString().trim()
                    + "\n   solo SOLID (antes): " + sa.toString().trim()
                    + "\n   + tapa vista (hoy): " + sb.toString().trim());
            } else if ("verEmisores".equals(cmd)) {
                reportar("verEmisores: visibles="
                    + GameScene.emisoresPlanosVisibles() + " de "
                    + GameScene.emisoresPlanos()
                    + " capas planas (en primera persona debe ser 0)");
            } else if ("alLadoDeUnaTuberia".equals(cmd)) {
                // WALL_DECO es donde cada nivel cuelga su adorno: la
                // tuberia que gotea en las alcantarillas, la antorcha en
                // la prision, la veta en las cuevas. Son los que emitian
                // particulas en coordenadas planas.
                int wT = com.github.dachhack.sprout.levels.Level.getWidth();
                int tuberia = -1, puestoT = -1;
                // Lejos, no pegado: una gota a media celda de la cara llena
                // la pantalla y no dice nada sobre si esta bien puesta.
                // Ballistica no sirve para apuntar a una pared -- se para
                // antes -- asi que se retrocede en linea recta desde la
                // casilla de delante de la tuberia mientras se pueda.
                int mejorD = 0;
                int[] ladosT = { wT, -wT, 1, -1 };
                for (int c = 0; c < Dungeon.level.map.length && mejorD < 5; c++) {
                    if (Dungeon.level.map[c]
                            != com.github.dachhack.sprout.levels.Terrain.WALL_DECO) {
                        continue;
                    }
                    for (int d : ladosT) {
                        int frente = c + d;
                        if (frente < 0 || frente >= Dungeon.level.map.length
                            || !com.github.dachhack.sprout.levels.Level.passable[frente]) {
                            continue;
                        }
                        int v = frente, pasos = 1;
                        while (pasos < 6) {
                            int sig = v + d;
                            if (sig < 0 || sig >= Dungeon.level.map.length
                                || !com.github.dachhack.sprout.levels.Level.passable[sig]
                                || com.github.dachhack.sprout.actors.Actor.findChar(sig) != null) {
                                break;
                            }
                            v = sig; pasos++;
                        }
                        if (pasos > mejorD) { tuberia = c; puestoT = v; mejorD = pasos; }
                    }
                }
                if (tuberia < 0) { reportar("alLadoDeUnaTuberia: no hay"); return; }
                moverHeroe(puestoT);
                Dungeon.observe();
                com.github.dachhack.sprout.FirstPerson.faceCell(tuberia);
                celdaPrueba = tuberia;
                int cuantas = 0;
                for (int c = 0; c < Dungeon.level.map.length; c++) {
                    if (Dungeon.level.map[c]
                            == com.github.dachhack.sprout.levels.Terrain.WALL_DECO) {
                        cuantas++;
                    }
                }
                reportar("alLadoDeUnaTuberia: piso=" + Dungeon.depth
                    + " tuberia=" + tuberia + " heroe=" + puestoT
                    + " distancia=" + mejorD
                    + " tuberias en el nivel=" + cuantas
                    + " visibles=" + com.github.dachhack.sprout.Adornos.visibles()
                    + " estado[" + tuberia + "]="
                    + com.github.dachhack.sprout.Adornos.comoEsta(tuberia)
                    + " adornos colocados="
                    + com.github.dachhack.sprout.Adornos.colocados()
                    + " (" + com.github.dachhack.sprout.Adornos.cuantos() + ")");
            } else if ("alLadoDeLaEntrada".equals(cmd)) {
                int ent = Dungeon.level.entrance;
                int w6 = com.github.dachhack.sprout.levels.Level.getWidth();
                int[] lados6 = { -w6, w6, -1, 1 };
                int puesto6 = -1;
                for (int d : lados6) {
                    int c = ent + d;
                    if (c > 0 && c < Dungeon.level.map.length
                        && com.github.dachhack.sprout.levels.Level.passable[c]
                        && com.github.dachhack.sprout.actors.Actor.findChar(c) == null) {
                        puesto6 = c; break;
                    }
                }
                if (puesto6 < 0) { reportar("alLadoDeLaEntrada: sin hueco"); return; }
                moverHeroe(puesto6);Dungeon.observe();
                celdaPrueba = ent;
                reportar("alLadoDeLaEntrada: entrada=" + ent + " heroe=" + puesto6
                    + " terreno=" + Dungeon.level.map[ent]);
            } else if ("comoLlegarA".equals(cmd)) {
                // Que tecla o par de teclas lleva de verdad a celdaPrueba.
                // Las ocho direcciones, no cuatro: con la camara mirando en
                // diagonal las cuatro flechas dan las cuatro diagonales del
                // mapa, y a la casilla de al lado se llega con dos teclas.
                if (celdaPrueba < 0) { reportar("comoLlegarA: sin objetivo"); return; }
                int w8 = com.github.dachhack.sprout.levels.Level.getWidth();
                String[] porDir = {
                    "ArrowUp", "ArrowUp+ArrowRight", "ArrowRight",
                    "ArrowRight+ArrowDown", "ArrowDown", "ArrowDown+ArrowLeft",
                    "ArrowLeft", "ArrowLeft+ArrowUp" };
                String cual = "NINGUNA";
                for (int k = 0; k < 8; k++) {
                    int v = com.github.dachhack.sprout.FirstPerson
                        .neighbour(Dungeon.hero.pos, k, w8);
                    if (v == celdaPrueba) { cual = porDir[k]; break; }
                }
                reportar("comoLlegarA: objetivo=" + celdaPrueba
                    + " heroe=" + Dungeon.hero.pos
                    + " mirando=" + com.github.dachhack.sprout.FirstPerson.facing8()
                    + " tecla=" + cual);
            } else if ("mirarObjetivo".equals(cmd)) {
                // Con la API del propio juego, no forzando el yaw: update()
                // lo vuelve a mover cada cuadro y poner el angulo a mano no
                // sobrevive al siguiente.
                if (celdaPrueba < 0) { reportar("mirarObjetivo: sin objetivo"); return; }
                com.github.dachhack.sprout.FirstPerson.faceCell(celdaPrueba);
                reportar("mirarObjetivo: encarando " + celdaPrueba);
            } else if ("carneEnEscalera".equals(cmd)) {
                // El caso que sospecho: algo tirado justo en la escalera.
                // Hero.handle mira el monton ANTES que la escalera, asi que
                // pisarla podria recoger en vez de bajar.
                int sal7 = Dungeon.level.exit;
                Dungeon.level.drop(
                    new com.github.dachhack.sprout.items.food.MysteryMeat(), sal7);
                reportar("carneEnEscalera: carne en la salida " + sal7
                    + " monton=" + (Dungeon.level.heaps.get(sal7) != null));
            } else if ("tomarLlaveDirecto".equals(cmd)) {
                int donde = Dungeon.hero.pos;
                com.github.dachhack.sprout.items.Heap h =
                    Dungeon.level.heaps.get(donde);
                reportar("tomarLlaveDirecto: monton aqui=" + (h != null)
                    + " -- llamando hero.handle(" + donde + ")");
                reportar("   handle devolvio " + Dungeon.hero.handle(donde)
                    + " accion=" + Dungeon.hero.curAction);
            } else if ("verLlave".equals(cmd)) {
                com.github.dachhack.sprout.items.keys.SkeletonKey k =
                    Dungeon.hero.belongings.getItem(
                        com.github.dachhack.sprout.items.keys.SkeletonKey.class);
                com.github.dachhack.sprout.items.Heap aqui =
                    Dungeon.level.heaps.get(Dungeon.hero.pos);
                StringBuilder q = new StringBuilder();
                if (aqui != null) {
                    for (com.github.dachhack.sprout.items.Item it : aqui.items) {
                        q.append(it.getClass().getSimpleName()).append(' ');
                    }
                }
                reportar("verLlave: en la mochila=" + (k != null)
                    + " mochila=" + mochila()
                    + " heroe=" + Dungeon.hero.pos
                    + " monton aqui=" + (aqui != null ? ("[" + q + "]") : "no")
                    + " listo=" + Dungeon.hero.ready
                    + " accion=" + Dungeon.hero.curAction
                    + " ventana=" + GameScene.windowOpen());
            } else if ("verSalida".equals(cmd)) {
                int t = Dungeon.level.map[Dungeon.level.exit];
                String nombre = t == com.github.dachhack.sprout.levels.Terrain.LOCKED_EXIT
                        ? "LOCKED_EXIT (cerrada)"
                    : t == com.github.dachhack.sprout.levels.Terrain.UNLOCKED_EXIT
                        ? "UNLOCKED_EXIT (abierta)"
                    : t == com.github.dachhack.sprout.levels.Terrain.EXIT
                        ? "EXIT (normal)" : ("terreno " + t);
                reportar("verSalida: piso=" + Dungeon.depth
                    + " salida=" + Dungeon.level.exit + " -> " + nombre
                    + " jefe=" + (jefe() == null ? "muerto/ausente" : "vivo")
                    + " bichos=" + Dungeon.level.mobs.size());
            } else if ("esperar".equals(cmd)) {
                // Gastar turnos sin moverse: hay cosas que ocurren un turno
                // despues de matar al jefe.
                Dungeon.hero.spend(1f);
                Dungeon.hero.next();
                reportar("esperar: listo=" + Dungeon.hero.ready);
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
    private static int pisoGen = 0;

    /**
     * Mueve al heroe manteniendo el registro de actores al dia.
     *
     * Cambiar Dungeon.hero.pos a pelo deja Actor.chars apuntando a la
     * casilla vieja, y findChar sigue diciendo que ahi hay alguien. Eso me
     * costo un "subir por escaleras esta roto" que no existia: el heroe
     * habia aparecido en la entrada, yo lo teletransportaba al lado, y
     * getCloser se negaba a volver a la entrada porque la creia ocupada
     * -- por el propio heroe.
     */
    private static void moverHeroe(int celda) {
        com.github.dachhack.sprout.actors.Actor.freeCell(Dungeon.hero.pos);
        Dungeon.hero.pos = celda;
        com.github.dachhack.sprout.actors.Actor.occupyCell(Dungeon.hero);
        if (Dungeon.hero.sprite != null) {
            Dungeon.hero.sprite.place(celda);
        }
    }

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

    /** El jefe del piso, si lo hay. */
    /**
     * El jefe del piso. Quien cuenta como jefe lo decide Arena.esJefe y
     * nadie mas: tener dos listas costo una tarde de "no hay jefe" con el
     * Rey Enano delante, porque la de aqui se habia quedado sin King ni
     * Yog.
     */
    private static com.github.dachhack.sprout.actors.mobs.Mob jefe() {
        if (Dungeon.level == null) return null;
        // El Goo de verdad manda: Sprouted suelta PoisonGoo como secuaces
        // ademas de usarlo de segunda fase, y repartir los golpes entre el
        // jefe y su tropa deja a Goo curandose en el agua mas rapido de lo
        // que se le pega.
        com.github.dachhack.sprout.actors.mobs.Mob respaldo = null;
        for (com.github.dachhack.sprout.actors.mobs.Mob m
                : Dungeon.level.mobs.toArray(
                    new com.github.dachhack.sprout.actors.mobs.Mob[0])) {
            if (!com.github.dachhack.sprout.Arena.esJefe(m)) continue;
            if (m instanceof com.github.dachhack.sprout.actors.mobs.PoisonGoo) {
                if (respaldo == null || m.HT > respaldo.HT) respaldo = m;
            } else {
                return m;
            }
        }
        return respaldo;
    }

    private static String estadoArma() {
        com.github.dachhack.sprout.items.KindOfWeapon a =
            Dungeon.hero.belongings.weapon;
        return a == null ? "arma=(ninguna)"
            : "arma=" + a.getClass().getSimpleName()
              + " nivel=" + a.level + " nombre=" + a.name();
    }

    /** Que hay en las 4 direcciones locales del heroe: P pisable, X no. */
    private static String vecinos() {
        if (Dungeon.hero == null || Dungeon.level == null) return "?";
        int w = com.github.dachhack.sprout.levels.Level.getWidth();
        StringBuilder sb = new StringBuilder();
        int[] dirs = { 0, 2, 4, 6 };   // adelante, derecha, atras, izquierda
        for (int d : dirs) {
            int c = com.github.dachhack.sprout.FirstPerson.neighbour(
                Dungeon.hero.pos, d, w);
            sb.append(c >= 0
                && com.github.dachhack.sprout.levels.Level.passable[c] ? 'P' : 'X');
        }
        return sb.toString();
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
