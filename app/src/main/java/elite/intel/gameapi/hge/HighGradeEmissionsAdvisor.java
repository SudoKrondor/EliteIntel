package elite.intel.gameapi.hge;

import elite.intel.ai.brain.vega.CompanionRuntime;
import elite.intel.db.FuzzySearch;
import elite.intel.db.dao.ShipSettingsDao;
import elite.intel.db.managers.ShipSettingsManager;
import elite.intel.gameapi.journal.events.dto.shiploadout.ShipLoadOutDto;
import elite.intel.session.PlayerSession;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static elite.intel.util.StringUtls.localizedEvent;

/**
 * Tells the commander when the system they just arrived in can drop Very Rare manufactured materials
 * from a High Grade Emissions signal.
 *
 * <p>The two facts that decide this arrive in separate journal events and on separate threads: the
 * system's allegiance, population and faction states come with {@code FSDJump}, while the emissions
 * signal itself comes with {@code FSSSignalDiscovered}. Either can land first — every subscriber runs
 * on its own virtual thread, and the jump handler makes EDSM calls before it finishes — so rather
 * than depend on an order that is not guaranteed, both halves report in here and whichever completes
 * the pair triggers the advice. One announcement per system, however many emissions it turns up.
 */
public final class HighGradeEmissionsAdvisor {

    private static HighGradeEmissionsAdvisor instance;

    private final BooleanSupplier alertsEnabled;
    private final Consumer<List<String>> announcer;

    /**
     * What is known about each system lately seen, keyed by system address and bounded so a long
     * session cannot grow it without limit.
     *
     * <p>WHY a map and not just the current system: signals are handled on their own virtual threads,
     * so one belonging to the system behind us can still land after the jump into this one. Held in a
     * single slot it would overwrite the newly-entered system's state; held per system it lands
     * harmlessly on the entry it actually describes.
     */
    private final Map<Long, SystemState> recentSystems = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, SystemState> eldest) {
            return size() > REMEMBERED_SYSTEMS;
        }
    };

    private static final int REMEMBERED_SYSTEMS = 8;

    /**
     * The system the commander is actually in; only this one is ever spoken about.
     */
    private long currentSystemAddress;

    private static final class SystemState {
        private String allegiance;
        private long population;
        private Collection<String> factionStates = List.of();
        private boolean systemKnown;
        private boolean emissionsSeen;
        private boolean announced;
    }

    /**
     * The setting lookup and the announcement are injected so the arrival/signal rendezvous can be
     * tested without a database or a voice: they are the only two things this class does that reach
     * outside it.
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
     * Called on arrival in a system, with the state the jump event reported about it. Arrival is what
     * makes a system the current one — an emissions signal never does, because one can arrive from
     * the system we are leaving.
     */
    public synchronized void onSystemEntered(long systemAddress, String allegiance, long population,
                                             Collection<String> factionStates) {
        currentSystemAddress = systemAddress;
        SystemState state = stateOf(systemAddress);
        state.allegiance = allegiance;
        state.population = population;
        state.factionStates = factionStates == null ? List.of() : List.copyOf(factionStates);
        state.systemKnown = true;
        advise(systemAddress, state);
    }

    /**
     * Called when a High Grade Emissions signal is discovered in the given system. Recorded against
     * that system whether or not it is the one we are in, so that a signal seen before the jump event
     * has been processed is not lost.
     */
    public synchronized void onHighGradeEmissions(long systemAddress) {
        SystemState state = stateOf(systemAddress);
        state.emissionsSeen = true;
        advise(systemAddress, state);
    }

    private SystemState stateOf(long systemAddress) {
        return recentSystems.computeIfAbsent(systemAddress, key -> new SystemState());
    }

    private void advise(long systemAddress, SystemState state) {
        if (systemAddress != currentSystemAddress) return;
        if (state.announced || !state.systemKnown || !state.emissionsSeen) return;

        List<String> symbols = HighGradeEmissions.materialSymbols(
                state.allegiance, state.factionStates, state.population);
        if (symbols.isEmpty()) return;
        if (!alertsEnabled.getAsBoolean()) return;

        state.announced = true;
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
