/* PixelDoomgeon — arena de jefes. GPL-3.0-or-later */
package web;

import com.github.dachhack.sprout.Dungeon;
import com.github.dachhack.sprout.actors.Actor;
import com.github.dachhack.sprout.actors.mobs.Mob;
import com.github.dachhack.sprout.actors.hero.Hero;
import com.github.dachhack.sprout.actors.hero.HeroClass;
import com.github.dachhack.sprout.items.Item;
import com.github.dachhack.sprout.levels.Level;
import com.github.dachhack.sprout.levels.Terrain;
import com.github.dachhack.sprout.scenes.GameScene;
import com.github.dachhack.sprout.scenes.StartScene;
import com.watabou.noosa.Game;

/**
 * Pelea contra un jefe, sin bajar veinticinco pisos para llegar.
 *
 * Es un sandbox de verdad, no un atajo dentro de la partida: antes de
 * tocar nada se cambia el cajon de guardado (Context.usarCajon), asi que
 * insignias, ranking y partida guardada de la arena viven aparte y no
 * pisan las del juego real. Sin eso, cualquiera que se pusiera roto para
 * matar a Yog desbloquearia logros y ensuciaria las estadisticas.
 */
public final class Arena {

    private Arena() {}

    /** True mientras se juega en la arena. */
    public static boolean activa = false;

    /** El jefe que se esta peleando, para la pantalla de resultado. */
    public static String jefeActual = "";

    /** Celda del jefe a la que hay que encarar, o -1 si ya se hizo. */
    private static int celdaAMirar = -1;

    /**
     * Monta la pelea y salta a ella.
     *
     * @param jefe  clave corta: goo, tengu, dm300, rey, yog...
     * @param clase warrior, mage, rogue, huntress
     * @param roto  true para equipo maximo; false para lo que da la clase
     */
    public static void iniciar(String jefe, String clase, boolean roto) {

        activa = true;
        jefeActual = jefe;
        android.content.Context.usarCajon("arena");

        StartScene.curClass = claseDe(clase);
        Dungeon.init();

        Dungeon.depth = profundidadDe(jefe);
        Level nivel = nivelDe(jefe);
        // Asignarlo ANTES de crearlo: varios createMobs() hablan de
        // Dungeon.level.mobs mientras se estan construyendo, y con
        // Dungeon.level todavia en null eso es un null.mobs.
        Dungeon.level = nivel;
        nivel.create();

        if (roto) {
            equiparRoto(Dungeon.hero);
        }

        // El jefe se mete en la lista del nivel, no con GameScene.add: la
        // escena no existe todavia y GameScene.add le pide un sprite. Asi
        // entra igual que los bichos que pone el propio piso, y switchLevel
        // -- que llama a Actor.init() -- lo despierta con los demas.
        ponerJefe(jefe, nivel.entrance);

        Dungeon.switchLevel(nivel, nivel.entrance);
        // Frente al jefe, no en la puerta del piso. Varios jefes nacen en
        // su propia sala al otro extremo del nivel -- Yog entre ellos -- y
        // aparecer en la entrada de las Salas a buscarlo por los pasillos
        // no es una arena, es una caminata.
        plantarseFrenteAlJefe();

        Game.switchScene(GameScene.class);
    }

    /**
     * Entrar viendo al jefe. En una arena, aparecer de cara a una pared
     * mientras la cosa que viniste a pelear resopla a tu espalda no es
     * tension, es desorientacion.
     */
    private static void plantarseFrenteAlJefe() {
        Mob jefe = null;
        for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])) {
            if (esJefe(m)) { jefe = m; break; }
        }
        if (jefe == null) return;

        int w = Level.getWidth();

        // De lejos hacia cerca, no al reves: a dos casillas el billboard
        // del jefe llena la pantalla y se lee como una textura verde, no
        // como un bicho. A cinco se le ve entero y da la talla.
        //
        // Un sitio desde el que DE VERDAD se le vea. Contar casillas no
        // basta: a cuatro de distancia puede haber un muro en medio, y
        // entonces apareces mirando con toda precision hacia una pared con
        // el jefe detras. Se prueban candidatos y se le pregunta al propio
        // campo de vision del juego cual funciona.
        int elegida = -1;
        int col0 = jefe.pos % w, fil0 = jefe.pos / w;
        int filas = Dungeon.level.map.length / w;

        // Primero, justo al SUR del jefe y en su misma columna. El yaw de
        // arranque es 0, que mira al norte, asi que colocado ahi lo tienes
        // de frente sin girar la camara -- y girarla desde aqui no sirve:
        // FirstPerson.reset() la pone a cero al construirse la escena y
        // update() la sigue moviendo cada cuadro.
        for (int r = 5; r >= 2 && elegida < 0; r--) {
            int fil = fil0 + r;
            if (fil >= filas) break;
            int c = fil * w + col0;
            if (!Level.passable[c] || Actor.findChar(c) != null) continue;
            Dungeon.hero.pos = c;
            Dungeon.observe();
            if (Level.fieldOfView[jefe.pos]) elegida = c;
        }

        // Si esa columna no sirve -- muro, agua, la sala no da -- cualquier
        // sitio con linea de vision, y ahi si se encara con faceCell.
        for (int r = 5; r >= 2 && elegida < 0; r--) {
            for (int dc = -r; dc <= r && elegida < 0; dc++) {
                for (int df = -r; df <= r; df++) {
                    if (Math.max(Math.abs(dc), Math.abs(df)) != r) continue;
                    int col = col0 + dc, fil = fil0 + df;
                    if (col < 0 || col >= w || fil < 0 || fil >= filas) continue;
                    int c = fil * w + col;
                    if (!Level.passable[c] || Actor.findChar(c) != null) continue;

                    Dungeon.hero.pos = c;
                    Dungeon.observe();
                    if (Level.fieldOfView[jefe.pos]) {
                        elegida = c;
                        celdaAMirar = jefe.pos;
                        break;
                    }
                }
            }
        }

        if (elegida >= 0) {
            Dungeon.hero.pos = elegida;
        }
        if (Dungeon.hero.sprite != null) {
            Dungeon.hero.sprite.place(Dungeon.hero.pos);
        }
        Dungeon.observe();
    }

    /**
     * Se llama una vez por cuadro. En cuanto la camara existe, encara al
     * jefe y se apaga. Usa faceCell, que es la manera que el propio juego
     * tiene de girar la vista hacia algo -- poner el yaw a mano no sirve,
     * porque update() lo vuelve a mover cada cuadro.
     */
    public static void aplicarMirada() {
        if (celdaAMirar < 0
                || com.github.dachhack.sprout.FirstPerson.camera() == null) {
            return;
        }
        com.github.dachhack.sprout.FirstPerson.faceCell(celdaAMirar);
        celdaAMirar = -1;
    }

    private static HeroClass claseDe(String c) {
        if ("mage".equals(c))     return HeroClass.MAGE;
        if ("rogue".equals(c))    return HeroClass.ROGUE;
        if ("huntress".equals(c)) return HeroClass.HUNTRESS;
        return HeroClass.WARRIOR;
    }

    /** La profundidad importa: los jefes y el botin escalan con ella. */
    private static int profundidadDe(String jefe) {
        if ("tengu".equals(jefe))  return 10;
        if ("dm300".equals(jefe))  return 15;
        if ("rey".equals(jefe))    return 20;
        if ("yog".equals(jefe))    return 25;
        return 5;
    }

    /**
     * Cada nivel se construye con new, no por reflexion: TeaVM tira lo que
     * nadie nombra, y un nivel invocado por su nombre en texto no lo
     * nombra nadie.
     */
    private static Level nivelDe(String jefe) {
        if ("tengu".equals(jefe)) {
            return new com.github.dachhack.sprout.levels.PrisonBossLevel();
        }
        if ("dm300".equals(jefe)) {
            return new com.github.dachhack.sprout.levels.CavesBossLevel();
        }
        if ("rey".equals(jefe)) {
            return new com.github.dachhack.sprout.levels.CityBossLevel();
        }
        if ("yog".equals(jefe)) {
            return new com.github.dachhack.sprout.levels.HallsBossLevel();
        }
        return new com.github.dachhack.sprout.levels.SewerBossLevel();
    }

    /**
     * Pone al jefe al lado del heroe si el nivel no lo trajo puesto.
     *
     * Hace falta porque varios no nacen con el piso: PrisonBossLevel tiene
     * createMobs() vacio a proposito -- Tengu no existe hasta que abres la
     * arena con la llave -- y en un sandbox no vamos a pedirle al jugador
     * que busque una llave para poder pelear.
     */
    private static void ponerJefe(String jefe, int cerca) {

        for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])) {
            if (esJefe(m)) {
                return;
            }
        }

        Mob m = crearJefe(jefe);
        if (m == null) return;

        int w = Level.getWidth();
        // En anillos, para no plantarlo pegado a la cara: se quiere ver al
        // jefe entero al entrar, no su textura a un palmo del ojo.
        int[] anillo = { -w * 3, w * 3, -3, 3, -w * 2, w * 2, -2, 2, -w, w, -1, 1 };
        for (int d : anillo) {
            int c = cerca + d;
            if (c > 0 && c < Dungeon.level.map.length
                && Level.passable[c] && Actor.findChar(c) == null) {
                m.pos = c;
                Dungeon.level.mobs.add(m);
                return;
            }
        }
    }

    private static Mob crearJefe(String jefe) {
        if ("tengu".equals(jefe)) {
            return new com.github.dachhack.sprout.actors.mobs.Tengu();
        }
        if ("dm300".equals(jefe)) {
            return new com.github.dachhack.sprout.actors.mobs.DM300();
        }
        if ("rey".equals(jefe)) {
            return new com.github.dachhack.sprout.actors.mobs.King();
        }
        if ("yog".equals(jefe)) {
            return new com.github.dachhack.sprout.actors.mobs.Yog();
        }
        return new com.github.dachhack.sprout.actors.mobs.Goo();
    }

    public static boolean esJefe(Mob m) {
        return m instanceof com.github.dachhack.sprout.actors.mobs.Goo
            || m instanceof com.github.dachhack.sprout.actors.mobs.PoisonGoo
            || m instanceof com.github.dachhack.sprout.actors.mobs.Tengu
            || m instanceof com.github.dachhack.sprout.actors.mobs.DM300
            || m instanceof com.github.dachhack.sprout.actors.mobs.King
            || m instanceof com.github.dachhack.sprout.actors.mobs.Yog;
    }

    /** Equipo de "ponerme roto": lo que trae la clase, subido al tope. */
    private static void equiparRoto(Hero heroe) {
        if (heroe.belongings.weapon != null) {
            for (int i = 0; i < 15; i++) heroe.belongings.weapon.upgrade();
        }
        if (heroe.belongings.armor != null) {
            for (int i = 0; i < 15; i++) heroe.belongings.armor.upgrade();
        }
        heroe.HT = 500;
        heroe.HP = heroe.HT;
        heroe.lvl = 30;
        heroe.STR = 25;
    }

    /** Terreno pisable alrededor, por si hace falta sitio. */
    static boolean libre(int celda) {
        return celda > 0 && celda < Dungeon.level.map.length
            && Level.passable[celda]
            && Dungeon.level.map[celda] != Terrain.LOCKED_EXIT;
    }
}
