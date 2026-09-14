/* PixelDoomgeon — evita que TeaVM tire codigo que solo se llama por
 * reflexion. GPL-3.0-or-later */
package web;

import com.github.dachhack.sprout.levels.Level;
import com.github.dachhack.sprout.levels.Room;
import com.github.dachhack.sprout.levels.painters.*;
import com.github.dachhack.sprout.actors.mobs.*;
import com.github.dachhack.sprout.actors.mobs.npcs.Ghost;
import com.github.dachhack.sprout.actors.mobs.Mob;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * El juego pinta cuartos y elige monstruos por reflexion: Room.Type
 * guarda un java.lang.reflect.Method por cada tipo de cuarto y lo llama
 * con Method.invoke (Room.java, ~linea 90); Bestiary.mobClass junta
 * arreglos de Class y crea el monstruo con Class.newInstance.
 *
 * TeaVM no sigue esos dos caminos al decidir que metodos conservar: no
 * hay una llamada normal (invokestatic/new) que los conecte al punto de
 * entrada, asi que los tira como codigo muerto. El sintoma no es un
 * error -- Method.invoke() lanza, Room.Type.paint() atrapa la excepcion
 * y la traga (reportException), asi que el cuarto simplemente no se
 * pinta. El mapa queda casi entero de pared, y el bucle que reparte
 * monstruos (RegularLevel.createMobs) nunca encuentra una casilla libre
 * y se cuelga para siempre.
 *
 * La correccion no es de logica: es hacerle ver a TeaVM un uso de
 * verdad de cada uno de estos metodos, para que los conserve.
 *
 * Una llamada directa dentro de una rama que nunca corre no basta: el
 * optimizador de TeaVM la detecto como el UNICO sitio que llamaba a
 * EntrancePainter.paint, ExitPainter.paint y varios mas, y los
 * "inlineo" ahi mismo -- desaparecio la funcion aparte, y con ella lo
 * que Method.invoke() necesita para llamarlos en tiempo real (se
 * verifico leyendo el JS generado: StandardPainter/TunnelPainter/
 * PassagePainter quedaron como funciones de verdad porque su cuerpo es
 * grande; los mas chicos se aplanaron dentro de tocar()). Una
 * REFERENCIA a metodo no se puede aplanar asi: crear un BiConsumer con
 * EntrancePainter::paint obliga a que exista un objeto que apunte al
 * metodo real, sin importar que ese objeto nunca se use. Guardarlas en
 * un campo estatico evita ademas que el propio campo se optimice fuera.
 */
final class MantenerVivo {

    private MantenerVivo() {}

    @SuppressWarnings("unused")
    private static final BiConsumer<Level, Room>[] PINTORES = new BiConsumer[] {
        (BiConsumer<Level, Room>) StandardPainter::paint,
        (BiConsumer<Level, Room>) EntrancePainter::paint,
        (BiConsumer<Level, Room>) ExitPainter::paint,
        (BiConsumer<Level, Room>) BossExitPainter::paint,
        (BiConsumer<Level, Room>) TunnelPainter::paint,
        (BiConsumer<Level, Room>) PassagePainter::paint,
        (BiConsumer<Level, Room>) ShopPainter::paint,
        (BiConsumer<Level, Room>) BlacksmithPainter::paint,
        (BiConsumer<Level, Room>) TreasuryPainter::paint,
        (BiConsumer<Level, Room>) ArmoryPainter::paint,
        (BiConsumer<Level, Room>) LibraryPainter::paint,
        (BiConsumer<Level, Room>) LaboratoryPainter::paint,
        (BiConsumer<Level, Room>) VaultPainter::paint,
        (BiConsumer<Level, Room>) TrapsPainter::paint,
        (BiConsumer<Level, Room>) StoragePainter::paint,
        (BiConsumer<Level, Room>) MagicWellPainter::paint,
        (BiConsumer<Level, Room>) GardenPainter::paint,
        (BiConsumer<Level, Room>) CryptPainter::paint,
        (BiConsumer<Level, Room>) StatuePainter::paint,
        (BiConsumer<Level, Room>) PoolPainter::paint,
        (BiConsumer<Level, Room>) RatKingPainter::paint,
        (BiConsumer<Level, Room>) WeakFloorPainter::paint,
        (BiConsumer<Level, Room>) PitPainter::paint,
        (BiConsumer<Level, Room>) RatKingPainter2::paint,
    };

    @SuppressWarnings("unused")
    private static final Supplier<? extends Mob>[] MONSTRUOS = new Supplier[] {
        // Bestiary.mobClass / mutable(): un monstruo por Class<?> en
        // los arreglos por profundidad, mas los reemplazos de mutable().
        (Supplier<Rat>) Rat::new, (Supplier<BrownBat>) BrownBat::new,
        (Supplier<GreyRat>) GreyRat::new, (Supplier<RatBoss>) RatBoss::new,
        (Supplier<Gnoll>) Gnoll::new, (Supplier<Crab>) Crab::new,
        (Supplier<Swarm>) Swarm::new, (Supplier<Skeleton>) Skeleton::new,
        (Supplier<Thief>) Thief::new, (Supplier<Goo>) Goo::new,
        (Supplier<Shaman>) Shaman::new, (Supplier<FossilSkeleton>) FossilSkeleton::new,
        (Supplier<Assassin>) Assassin::new, (Supplier<Bat>) Bat::new,
        (Supplier<BanditKing>) BanditKing::new, (Supplier<Tengu>) Tengu::new,
        (Supplier<Brute>) Brute::new, (Supplier<Spinner>) Spinner::new,
        (Supplier<BrokenRobot>) BrokenRobot::new, (Supplier<Elemental>) Elemental::new,
        (Supplier<Monk>) Monk::new, (Supplier<DM300>) DM300::new,
        (Supplier<Warlock>) Warlock::new, (Supplier<Golem>) Golem::new,
        (Supplier<DwarfLich>) DwarfLich::new, (Supplier<Succubus>) Succubus::new,
        (Supplier<King>) King::new, (Supplier<Eye>) Eye::new,
        (Supplier<Scorpio>) Scorpio::new, (Supplier<DemonGoo>) DemonGoo::new,
        (Supplier<Yog>) Yog::new,
        (Supplier<Ghost.GnollArcher>) Ghost.GnollArcher::new,
        (Supplier<ForestProtector>) ForestProtector::new,
        (Supplier<MossySkeleton>) MossySkeleton::new,
        (Supplier<GraveProtector>) GraveProtector::new,
        (Supplier<AlbinoPiranha>) AlbinoPiranha::new,
        (Supplier<FishProtector>) FishProtector::new,
        (Supplier<GoldThief>) GoldThief::new,
        (Supplier<VaultProtector>) VaultProtector::new,
        (Supplier<BlueWraith>) BlueWraith::new, (Supplier<Oni>) Oni::new,
        (Supplier<FlyingProtector>) FlyingProtector::new,
        (Supplier<GreyOni>) GreyOni::new, (Supplier<SpectralRat>) SpectralRat::new,
        (Supplier<TenguDen>) TenguDen::new, (Supplier<Kupua>) Kupua::new,
        (Supplier<Gullin>) Gullin::new, (Supplier<Albino>) Albino::new,
        (Supplier<Bandit>) Bandit::new, (Supplier<Shielded>) Shielded::new,
        (Supplier<Senior>) Senior::new, (Supplier<Acidic>) Acidic::new,
    };

    /** Solo con que la clase se cargue basta -- los campos estaticos de
     *  arriba ya conservan todo. Esto nomas obliga a que la clase se
     *  cargue en vez de quedar ella misma como codigo muerto. */
    static void tocar() {
        if (PINTORES.length < 0 || MONSTRUOS.length < 0) {
            throw new IllegalStateException();
        }
    }
}
