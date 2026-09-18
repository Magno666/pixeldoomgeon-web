/* PixelDoomgeon — sustituto de android.os.Build. GPL-3.0-or-later */
package android.os;

/**
 * Lo unico que el juego pregunta de aqui es que aparato es, para la
 * telemetria. En el navegador no hay aparato que valga: quien contesta
 * esa pregunta es web.Progreso mirando el user agent, y este sustituto
 * existe nada mas para que TelemetriaAndroid compile.
 *
 * Esa clase nunca corre aqui -- la llama la actividad de Android, a la
 * que el navegador no llega -- pero TeaVM la compila igual porque esta
 * al alcance del codigo, y sin estos campos no pasa del javac.
 */
public final class Build {

    private Build() {}

    public static final String MODEL = "navegador";

    public static final class VERSION {
        private VERSION() {}
        public static final String RELEASE = "";
        public static final int SDK_INT = 0;
    }
}
