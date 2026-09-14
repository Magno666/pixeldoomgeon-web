/* PixelDoomgeon — registra los pintores de Room.Type para reflexion en
 * TeaVM. Hook de compilacion, no corre en el navegador. GPL-3.0-or-later */
package webbuild;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.teavm.classlib.ReflectionContext;
import org.teavm.classlib.ReflectionSupplier;
import org.teavm.model.MethodDescriptor;

/**
 * Room.Type (Room.java) pinta cada tipo de cuarto por reflexion:
 * painter.getMethod("paint", Level.class, Room.class) en el
 * constructor del enum, y Method.invoke(...) al pintar. TeaVM decide
 * que metodos exponer a Class.getMethod/getDeclaredMethods con este
 * mismo gancho -- sin el, getMethod tira NoSuchMethodException aunque
 * el metodo SI este compilado (son dos cosas distintas: que el codigo
 * exista, y que la tabla de reflexion lo liste).
 *
 * Verificado con un depurador real, pausando en el frame colgado:
 * jl_Class_getDeclaredMethods(StandardPainter) daba 0 incluso despues
 * de forzar a TeaVM a conservar el codigo (referenciandolo directo).
 * Esa es la señal de que faltaba esto, no el codigo.
 */
public final class PintoresReflexion implements ReflectionSupplier {

    private static final String PAQUETE = "com.github.dachhack.sprout.levels.painters.";

    private static final String[] CLASES = {
        "StandardPainter", "EntrancePainter", "ExitPainter", "BossExitPainter",
        "TunnelPainter", "PassagePainter", "ShopPainter", "BlacksmithPainter",
        "TreasuryPainter", "ArmoryPainter", "LibraryPainter", "LaboratoryPainter",
        "VaultPainter", "TrapsPainter", "StoragePainter", "MagicWellPainter",
        "GardenPainter", "CryptPainter", "StatuePainter", "PoolPainter",
        "RatKingPainter", "WeakFloorPainter", "PitPainter", "RatKingPainter2",
    };

    private static final Set<String> CLASES_COMPLETAS = new HashSet<>();
    static {
        for (String c : CLASES) {
            CLASES_COMPLETAS.add(PAQUETE + c);
        }
    }

    private static final String FIRMA_PAINT =
        "paint(Lcom/github/dachhack/sprout/levels/Level;"
        + "Lcom/github/dachhack/sprout/levels/Room;)V";

    @Override
    public Collection<MethodDescriptor> getAccessibleMethods(
            ReflectionContext context, String className) {
        if (!CLASES_COMPLETAS.contains(className)) {
            return Collections.emptyList();
        }
        Collection<MethodDescriptor> lista = new ArrayList<>();
        lista.add(MethodDescriptor.parse(FIRMA_PAINT));
        return lista;
    }
}
