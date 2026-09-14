/* PixelDoomgeon — assets precargados. GPL-3.0-or-later */
package web;

import android.graphics.Bitmap;
import android.content.res.AssetManager;

import org.teavm.jso.JSBody;
import org.teavm.jso.browser.Window;
import org.teavm.jso.canvas.CanvasRenderingContext2D;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLImageElement;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Aqui esta el peaje del port entero, en una clase.
 *
 * El juego decodifica un PNG en mitad del codigo y sigue en la linea
 * siguiente. El navegador solo carga imagenes de forma asincrona y no hay
 * forma de esperar. Asi que se descargan TODAS antes de arrancar y a
 * partir de ahi el juego encuentra siempre lo que pide.
 *
 * El precio: la primera carga tarda, y la lista de archivos tiene que
 * estar escrita aqui porque el navegador no puede mirar un directorio.
 * Los mp3 NO se precargan: el elemento <audio> los busca por su cuenta
 * cuando toca sonar, y esperar a 49 pistas antes de ver el titulo seria
 * absurdo.
 */
public final class Assets {

    private Assets() {}

    private static final Map<String, Bitmap> imagenes = new HashMap<>();
    private static final Map<String, byte[]> archivos = new HashMap<>();
    private static String base = "assets/";

    /** Los 138 PNG del juego. Generada desde app/src/main/assets. */
    private static final String[] PNG = {
        "adultdragon.png",
        "albinopiranha.png",
        "amulet.png",
        "arcs1.png",
        "arcs2.png",
        "assassin.png",
        "avatars.png",
        "badges.png",
        "banditking.png",
        "banners.png",
        "bat.png",
        "bee.png",
        "blacksmith.png",
        "bluewraith.png",
        "brute.png",
        "buffs.png",
        "burning_fist.png",
        "chrome.png",
        "crabking.png",
        "crab.png",
        "dashboard.png",
        "demongoo.png",
        "demon.png",
        "dewprotector.png",
        "dm300.png",
        "dwarfkingtomb.png",
        "dwarflich.png",
        "effects.png",
        "elemental.png",
        "exp_bar.png",
        "eye.png",
        "fairy.png",
        "fireball.png",
        "flyingprotector.png",
        "font15x.png",
        "font1x.png",
        "font25x.png",
        "font2x.png",
        "font3x.png",
        "fossilskeleton.png",
        "ghost.png",
        "gnollarcher.png",
        "gnoll.png",
        "goldthief.png",
        "golem.png",
        "goo.png",
        "greyoni.png",
        "gullin.png",
        "hermitcrab.png",
        "hp_bar.png",
        "icons.png",
        "infecting_fist.png",
        "items.png",
        "king.png",
        "kupua.png",
        "large_buffs.png",
        "larva.png",
        "littower.png",
        "mage.png",
        "magiceye.png",
        "mimic.png",
        "monk.png",
        "monsterbox.png",
        "mossyskeleton.png",
        "mrdestructo2.png",
        "mrdestructo.png",
        "oni.png",
        "orbofzot.png",
        "otiluke.png",
        "otilukestone.png",
        "pet.png",
        "petreddragon.png",
        "pinning_fist.png",
        "piranha.png",
        "plants.png",
        "poisongoo.png",
        "rabbit.png",
        "ranger.png",
        "ratboss.png",
        "ratking.png",
        "rat.png",
        "redwraith.png",
        "robot.png",
        "rogue.png",
        "rotting_fist.png",
        "scorpio.png",
        "seekingbomb.png",
        "sentinel.png",
        "shadow.png",
        "shadowyog.png",
        "shaman.png",
        "sheep.png",
        "shell.png",
        "shopkeeper.png",
        "skeletonhand1.png",
        "skeletonkingskull.png",
        "skeleton.png",
        "sokobansheep.png",
        "specks.png",
        "spectralrat.png",
        "spell_icons.png",
        "spinner.png",
        "statue.png",
        "status_pane.png",
        "succubus.png",
        "surface.png",
        "swarm.png",
        "tengu.png",
        "thiefking.png",
        "thief.png",
        "tiles0.png",
        "tiles1.png",
        "tiles2.png",
        "tiles3.png",
        "tiles4.png",
        "tiles_beach.png",
        "tiles_forest.png",
        "tiles_magic_cave.png",
        "tiles_skeleton.png",
        "tiles_town.png",
        "tiles_vault.png",
        "tinkerer.png",
        "toolbar.png",
        "tower.png",
        "undead.png",
        "velocirooster.png",
        "wandmaker.png",
        "warlock.png",
        "warrior.png",
        "water0.png",
        "water1.png",
        "water2.png",
        "water3.png",
        "water4.png",
        "wraith.png",
        "yog.png",
        "zotphase.png",
        "zot.png"
    };

    public static void base(String ruta) { base = ruta; }

    // ---- precarga ----

    private static int pendientes;
    private static Runnable alTerminar;

    public static void precargar(Runnable cuandoEste) {
        alTerminar = cuandoEste;
        pendientes = PNG.length;
        progreso(0, PNG.length);
        for (final String nombre : PNG) {
            final HTMLImageElement img = (HTMLImageElement)
                Window.current().getDocument().createElement("img");
            img.addEventListener("load", new EventListener<Event>() {
                @Override public void handleEvent(Event e) {
                    guardarImagen(nombre, img);
                    uno();
                }
            });
            img.addEventListener("error", new EventListener<Event>() {
                @Override public void handleEvent(Event e) {
                    android.util.Log.w("Assets", "no cargo: " + nombre);
                    uno();          // seguir: mejor un hueco que no arrancar
                }
            });
            img.setSrc(base + nombre);
        }
    }

    private static void uno() {
        pendientes--;
        progreso(PNG.length - pendientes, PNG.length);
        if (pendientes == 0 && alTerminar != null) {
            Runnable r = alTerminar;
            alTerminar = null;
            r.run();
        }
    }

    @JSBody(params = {"hechos", "total"}, script =
        "var b = document.getElementById('barra');"
      + "if (b) b.style.width = Math.round(hechos * 100 / total) + '%';"
      + "var t = document.getElementById('cargando-texto');"
      + "if (t) t.textContent = hechos + ' / ' + total;")
    private static native void progreso(int hechos, int total);

    @JSBody(params = "msg", script =
        "var c = document.getElementById('cargando');"
      + "if (c) c.innerHTML = '<p style=\\'color:#E07360\\'>' + msg + '</p>';")
    public static native void mostrarError(String msg);

    @JSBody(script = "var c = document.getElementById('cargando'); if (c) c.remove();")
    public static native void quitarPantallaDeCarga();

    // ---- lo que el juego pide ----

    public static void guardarImagen(String nombre, HTMLImageElement img) {
        HTMLCanvasElement c = (HTMLCanvasElement)
            Window.current().getDocument().createElement("canvas");
        c.setWidth(img.getWidth());
        c.setHeight(img.getHeight());
        CanvasRenderingContext2D g = (CanvasRenderingContext2D) c.getContext("2d");
        g.drawImage(img, 0, 0);
        imagenes.put(nombre, android.graphics.BitmapPuente.envolver(c));
    }

    public static void guardarArchivo(String nombre, byte[] datos) {
        archivos.put(nombre, datos);
    }

    public static Bitmap imagenDe(InputStream is) {
        if (is instanceof AssetManager.AssetStream) {
            return imagenes.get(((AssetManager.AssetStream) is).nombre);
        }
        return null;
    }

    public static byte[] bytesDe(String nombre) {
        byte[] b = archivos.get(nombre);
        if (b != null) return b;
        return imagenes.containsKey(nombre) ? new byte[0] : null;
    }

    public static String urlDe(String nombre) { return base + nombre; }

    public static String[] listar(String ruta) {
        return imagenes.keySet().toArray(new String[0]);
    }

    public static HTMLCanvasElement lienzoDe(Bitmap b) {
        return android.graphics.BitmapPuente.lienzo(b);
    }
}
