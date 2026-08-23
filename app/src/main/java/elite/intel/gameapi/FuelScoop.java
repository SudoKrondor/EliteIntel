package elite.intel.gameapi;

import elite.intel.db.managers.GlobalSettingsManager;
import elite.intel.gameapi.journal.events.dto.shiploadout.ModuleDto;
import elite.intel.gameapi.journal.events.dto.shiploadout.ShipLoadOutDto;
import elite.intel.session.PlayerSession;

import java.util.List;
import java.util.Locale;

/**
 * Whether the ship the commander is flying can scoop fuel from a star, and therefore whether it is worth
 * being told that the next one is scoopable.
 * <p>
 * WHY the loadout has to be consulted and not just the setting: "refuel possible" on arrival is good news
 * only to a ship carrying a scoop. In a ship without one the same sentence is worse than silence - it
 * announces a fuel supply the commander cannot reach, on the very trips where fuel is the thing they are
 * worried about. The global toggle says whether the commander wants to hear about fuel at all; this says
 * whether there is anything to hear.
 * <p>
 * The scoop is found by module item name rather than by slot: a fuel scoop is fitted to an ordinary
 * optional-internal slot, so the slot name says nothing about what is in it. Item names arrive through
 * {@code LoadoutConverter} already spaced and title-cased ("Int Fuelscoop Size6 Class5"), which is why the
 * match is made in lower case against the run-together spelling the game uses.
 */
public final class FuelScoop {

    /**
     * The game's own spelling, once {@code toReadableModuleName} has had it. Every fuel scoop in the game
     * is an {@code int_fuelscoop_sizeN_classN}, engineered or not.
     */
    private static final String FUEL_SCOOP_ITEM = "fuelscoop";

    private FuelScoop() {
    }

    /**
     * Whether scoopable-star announcements are wanted right now: the commander asked for them AND the ship
     * can act on them.
     */
    public static boolean announceFuelStars() {
        return GlobalSettingsManager.getInstance().getAnnounceFuelAvailable() && isFitted();
    }

    /**
     * Whether the ship currently being flown has a fuel scoop.
     */
    public static boolean isFitted() {
        return isFittedTo(PlayerSession.getInstance().getShipLoadout());
    }

    /**
     * Test seam, and the whole rule.
     * <p>
     * An unknown loadout counts as fitted. The game sends {@code Loadout} on every login and every ship
     * change, so "we do not know yet" is a window of seconds at startup - and in that window the honest
     * failure is the old behaviour (say it) rather than silently withholding fuel information from a ship
     * that does have a scoop.
     */
    static boolean isFittedTo(ShipLoadOutDto loadout) {
        if (loadout == null) return true;
        List<ModuleDto> modules = loadout.getModules();
        if (modules == null || modules.isEmpty()) return true;
        return modules.stream()
                .map(ModuleDto::getItem)
                .filter(item -> item != null)
                .anyMatch(item -> item.toLowerCase(Locale.ROOT).contains(FUEL_SCOOP_ITEM));
    }
}
