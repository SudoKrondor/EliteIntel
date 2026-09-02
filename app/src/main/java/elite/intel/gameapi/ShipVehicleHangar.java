package elite.intel.gameapi;

import elite.intel.db.managers.ShipLoadoutManager;
import elite.intel.gameapi.journal.events.dto.shiploadout.ModuleDto;
import elite.intel.gameapi.journal.events.dto.shiploadout.ShipLoadOutDto;

import java.util.List;
import java.util.Locale;

/**
 * Whether the ship the commander is flying can carry a surface vehicle at all.
 * <p>
 * This is the one part of the deploy decision that comes from the game rather than from the commander:
 * the loadout names the hangar module, even though it never says what is inside it.
 */
public final class ShipVehicleHangar {

    /**
     * Fragments of the module symbol that identify a planetary vehicle hangar.
     * <p>
     * Matched as a symbol substring rather than by exact name because the hangar comes in several sizes and
     * ratings and every one of them is the same answer to "can this hull carry a buggy". Both spellings are
     * accepted: the module has been {@code int_buggybay_*} since the SRV shipped, and a rename to the
     * vehicle wording it is described by in game would otherwise silently make every ship look like it had
     * no hangar - a failure that would read as the feature being broken rather than as one word changing.
     */
    private static final List<String> HANGAR_SYMBOLS = List.of("buggybay", "vehiclebay");

    private ShipVehicleHangar() {
    }

    /**
     * Whether the current ship's loadout carries a planetary vehicle hangar.
     * <p>
     * A loadout we have never seen reads as no hangar. That is the safe way round: the commander is told
     * their ship has no vehicle bay, which they can immediately see is wrong, rather than the app pressing
     * a sequence of keys at a ship that has no bay to open.
     */
    public static boolean isFitted() {
        ShipLoadOutDto loadout = ShipLoadoutManager.getInstance().get();
        return isFitted(loadout == null ? null : loadout.getModules());
    }

    /**
     * The same question asked of a loadout that is already in hand, so the rule can be tested without a
     * database.
     */
    public static boolean isFitted(List<ModuleDto> modules) {
        if (modules == null) return false;
        return modules.stream().anyMatch(module -> {
            String item = module == null ? null : module.getItem();
            if (item == null) return false;
            String symbol = item.toLowerCase(Locale.ROOT);
            return HANGAR_SYMBOLS.stream().anyMatch(symbol::contains);
        });
    }
}
