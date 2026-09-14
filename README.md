# PixelDoomgeon — versión para navegador

Sprouted Pixel Dungeon, con la capa de render en primera persona, corriendo
en el navegador. El juego no se reescribió: se compila el bytecode de Java a
JavaScript con [TeaVM](https://teavm.org/), y lo que hay aquí es la capa que
le hace creer que está en Android.

Corre en <https://pixeldoomgeon.jaliscomundial.com/juego/>

## Qué hay en `src`

- `android/**` — los sustitutos de la plataforma. `GLES20` sobre WebGL,
  `Bitmap`/`Canvas`/`Paint` sobre el canvas 2D, `Context` guardando en
  `localStorage`, `MediaPlayer` y `SoundPool` sobre `<audio>`.
- `web/Assets.java` — precarga los 138 PNG antes de arrancar. El juego los
  pide de forma síncrona, así que no hay manera de cargarlos sobre la marcha.
- `web/Entrada.java` — ratón y tacto. Los listeners se instalan en JS y
  encolan; Java vacía la cola una vez por cuadro.
- `com/github/dachhack/sprout/Arranque.java` — la única clase en el paquete
  del juego. Existe nada más para poder llamar a `onCreate`, que es
  `protected`.

## Compilar

Hacen falta dos jar en `libs/`, que no están en el historial:

1. **`juego.jar`** — el código del juego compilado. Sale del fork, en
   `/root/proyectos/sprouted-build`. Se empaqueta **sólo** `com/github` y
   `com/watabou` de sus clases; meter más de una vez costó una tarde:
   TeaVM resolvió `web.Main` desde una copia vieja que se había colado en
   el jar y compiló 34 KB de nada.

2. **`json.jar`** — `org.json:json:20240303`, de Maven Central. El sistema
   de guardado del juego lo usa. Se me pasó en el primer inventario de
   dependencias porque no empieza con `android.`: 42 errores de golpe.

Después:

    JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew generateJavaScript

TeaVM necesita Java 17 o más para sus propias clases, pero el juego se
compila aparte con `source 8` — usa `_` como nombre de variable, que dejó
de ser legal en Java 9.

El resultado queda en `build/generated/teavm/js/juego.js`.

## Diagnóstico

Añadir `?diag` a la URL pone contadores en el título de la pestaña: cuadros,
dibujos, índices, texturas subidas, errores de GL y eventos de entrada.
Esos contadores encontraron los dos fallos que tenían la pantalla en negro
—las texturas subiéndose vacías y el listener de toques sin conectar— así
que se quedan.

## Licencia

GPL-3.0-or-later, igual que el juego del que sale. Ver `LICENSE.txt`.

Créditos: **Watabou** (Pixel Dungeon y el motor Noosa), **Evan Debenham**
(Shattered) y **dachhack** (Sprouted).
