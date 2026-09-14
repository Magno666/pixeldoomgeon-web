/* PixelDoomgeon — cuenta casillas no-pared del nivel, sin depender de
 * verlo dibujado. GPL-3.0-or-later */
package web;

import com.github.dachhack.sprout.Dungeon;
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
        return "nivel libres=" + libres + "/" + total;
    }
}
