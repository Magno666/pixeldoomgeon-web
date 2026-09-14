/* PixelDoomgeon — encendido del juego en el navegador. GPL-3.0-or-later */
package com.github.dachhack.sprout;

import android.os.Bundle;

/**
 * En Android, el sistema crea la Activity y llama a onCreate, que es
 * protected. Aqui no hay sistema, asi que hace falta alguien DENTRO del
 * paquete que pueda llamarlo.
 *
 * Vive en com.github.dachhack.sprout por eso y solo por eso: es la unica
 * clase de este port que se mete en el paquete del juego, y no toca nada
 * de el.
 */
public final class Arranque {

    private Arranque() {}

    public static void encender() {
        new ShatteredPixelDungeon().onCreate(new Bundle());
    }
}
