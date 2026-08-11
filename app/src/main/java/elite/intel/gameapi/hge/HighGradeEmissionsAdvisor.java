package elite.intel.gameapi.hge;

import elite.intel.ai.brain.vega.CompanionRuntime;
import elite.intel.db.FuzzySearch;
import elite.intel.db.dao.ShipSettingsDao;
import elite.intel.db.managers.ShipSettingsManager;
import elite.intel.gameapi.journal.events.dto.shiploadout.ShipLoadOutDto;
import elite.intel.session.PlayerSession;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static elite.intel.util.StringUtls.localizedEvent;

/**
 * Tells the commander, on arrival, which Very Rare manufactured materials a High Grade Emissions
 * signal can drop in the system they just jumped into.
 *
 * <p>Everything that decides the answer arrives with {@code FSDJump}: the system's allegiance, its
 * population and its factions' states. What an emissions site drops is fixed by those three, so the
 * advice can be given the moment the commander arrives.
 *
 * <p>WHY it does not wait to see an emissions signal first: it used to, and that made it silent in
 * exactly the systems worth farming. Emissions sources spawn and despawn over time rather than being
 * present on arrival, and the game does not reliably write an {@code FSSSignalDiscovered} for them -
 * in one recorded session the commander jumped into an Empire system in Expansion, dropped into a
 * High Grade Emissions site four minutes later and collected seven Imperial Shielding, while the only
 * signal the journal ever reported for that system was its Nav Beacon. Waiting for a signal that
 * never comes is not caution, it is a missed announcement. Whether a site has spawned yet does not
 * change what this system's sites would drop, which is the whole of the advice.
 */
public final class HighGradeEmissionsAdvisor {

    private static HighGradeEmissionsAdvisor instance;

    private final BooleanSupplier alertsEnabled;
    private final Consumer<List<String>> announcer;

    /**
     * The setting lookup and the announcement are injected so arrival handling can be tested without
     * a database or a voice: they are the only two things this class does that reach outside it.
     */
    HighGradeEmissionsAdvisor(BooleanSupplier alertsEnabled, Consumer<List<String>> announcer) {
        this.alertsEnabled = alertsEnabled;
        this.announcer = announcer;
    }

    public static synchronized HighGradeEmissionsAdvisor getInstance() {
        if (instance == null) {
            instance = new HighGradeEmissionsAdvisor(
                    HighGradeEmissionsAdvisor::shipWantsAlerts,
                    HighGradeEmissionsAdvisor::speak);
        }
        return instance;
    }

    /**
     * Called on arrival in a system, with the state the jump event reported about it. Says nothing
     * when the system yields no Very Rare materials, which is most of them.
     */
    public void onSystemEntered(String allegiance, long population, Collection<String> factionStates) {
        List<String> symbols = HighGradeEmissions.materialSymbols(allegiance, factionStates, population);
        if (symbols.isEmpty()) return;
        if (!alertsEnabled.getAsBoolean()) return;
        announcer.accept(symbols);
    }

    /**
     * Whether the ship the commander is flying is set up to be told about this. Off by default, and
     * per ship because a build with no material capacity to spare has no use for the interruption.
     *
     * <p>WHY a missing loadout is a "no" rather than a failure: the setting lives on a ship, so with
     * no ship there is no setting to consult, and speaking anyway would be answering a question
     * nobody configured.
     */
    private static boolean shipWantsAlerts() {
        ShipLoadOutDto loadout = PlayerSession.getInstance().getShipLoadout();
        if (loadout == null) return false;
        ShipSettingsDao.ShipSettings settings = ShipSettingsManager.getInstance().getSettings(loadout.getShipId());
        return settings != null && settings.isHgeAlerts();
    }

    private static void speak(List<String> symbols) {
        CompanionRuntime.narrator().announce(localizedEvent("event.hge.materials", spokenNames(symbols)), false);
    }

    /**
     * Material names as the commander's language says them, comma separated for speech.
     */
    private static String spokenNames(List<String> symbols) {
        List<String> names = new ArrayList<>(symbols.size());
        for (String symbol : symbols) {
            names.add(FuzzySearch.localizedMaterialName(symbol));
        }
        return String.join(", ", names);
    }
}
