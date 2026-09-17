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
        avances[muestra] = (float) Math.sqrt(cam.eyeX * cam.eyeX + cam.eyeZ * cam.eyeZ);
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
                sb.append("\n   avance:");
                float base = avances[0];
                for (int i = 0; i < muestra; i += 2) {
                    sb.append(' ').append(Math.round((avances[i] - base) * 100f));
                }
                reportar(sb.toString());
                alturas = null;
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
