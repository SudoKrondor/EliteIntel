package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.vega.VegaRuntime;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.ApproachSettlementEvent;
import elite.intel.gameapi.journal.events.FileheaderEvent;
import elite.intel.session.PlayerSession;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static elite.intel.util.StringUtls.localizedEvent;

public class ApproachSettlementSubscriber {

    private final PlayerSession playerSession = PlayerSession.getInstance();

    /**
     * The settlements already announced in this game session, keyed by {@link #settlementKey}.
     *
     * <p>WHY: a commander pulling materials out of a settlement lands there over and over, and the
     * game logs an {@code ApproachSettlement} every time. The record is still refreshed on each
     * approach, but VEGA describes a settlement once per session, which is one journal file.
     */
    private final Set<String> announcedThisSession = ConcurrentHashMap.newKeySet();

    /**
     * A new game session forgets which settlements were announced. A {@code part} above 1 is the
     * game continuing the same session in a fresh file, so it is not a new session.
     */
    @Subscribe
    public void onFileheaderEvent(FileheaderEvent event) {
        if (event.getPart() <= 1) announcedThisSession.clear();
    }

    @Subscribe
    public void onApproachSettlementEvent(ApproachSettlementEvent event) {
        Thread.ofVirtual().start(() -> {
            StringBuilder sb = new StringBuilder(settlementFacts(event));

            // The settlement is a place of its own, not the body it stands on: give it its own record before
            // anything is filed against it. The event names the body but not the system, so that comes from
            // whatever is on record for the address, and from the session when the system has no record yet.
            String starSystem = LocationManager.getInstance().findStarName(event.getSystemAddress());
            DockedStationRecord settlement = DockedStationRecord.of(event, starSystem);
            settlement.store();

            String availableData = LocalServicesData.forStation(
                    event.getSystemAddress(), starSystem, settlement.recordKey(), event.getMarketID());
            if (!availableData.isEmpty()) sb.append(" ").append(localizedEvent("event.approach.settlement.moreData"));

            if (playerSession.isRouteAnnouncementOn() && firstApproachThisSession(event)) {
                String instructions = """
                            Approaching settlement.
                            Provide very brief summary for the settlement data.
                            Do not list every service.
                        """;
                VegaRuntime.narrator().narrate(sb.toString(), instructions);
            }
        });
    }

    /**
     * True the first time this session the settlement is approached, false on every return to it.
     */
    boolean firstApproachThisSession(ApproachSettlementEvent event) {
        return announcedThisSession.add(settlementKey(event));
    }

    /**
     * Name within system rather than {@code MarketID}: a settlement with no market reports none, and
     * the name is unique within its system.
     */
    private static String settlementKey(ApproachSettlementEvent event) {
        return event.getSystemAddress() + "/" + event.getName();
    }

    /**
     * The settlement as the journal described it, as labelled facts for VEGA to summarise.
     * Only the facts the event actually carried, see {@link #appendFact}.
     */
    static String settlementFacts(ApproachSettlementEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append(localizedEvent("event.approach.settlement.approaching", event.getName())).append(" ");

        String faction = event.getStationFaction() == null ? null : event.getStationFaction().getName();
        if ("$government_Engineer;".equalsIgnoreCase(event.getStationGovernment())) {
            appendFact(sb, "event.approach.settlement.engineer", faction);
        }

        appendFact(sb, "event.approach.settlement.allegiance", event.getStationAllegiance());
        appendFact(sb, "event.approach.settlement.economy",
                localisedOrSymbol(event.getStationEconomyLocalised(), event.getStationEconomy()));
        appendFact(sb, "event.approach.settlement.government",
                localisedOrSymbol(event.getStationGovernmentLocalised(), event.getStationGovernment()));
        appendFact(sb, "event.approach.settlement.faction", faction);

        List<String> stationServices = event.getStationServices();
        if (stationServices != null && !stationServices.isEmpty()) {
            sb.append(localizedEvent("event.approach.settlement.services")).append(" ");
            sb.append(String.join(", ", stationServices)).append(".");
        }
        return sb.toString();
    }

    /**
     * Appends one labelled fact, or nothing at all when the journal did not report it.
     *
     * <p>WHY: {@code ApproachSettlement} leaves a field out rather than reporting it empty. A
     * settlement held by a faction with no superpower behind it carries no {@code StationAllegiance}
     * at all, which is the ordinary case rather than an odd one: three of the four settlements
     * approached in the session that exposed this had none. Appending the label regardless put
     * "Allegiance: null." into the payload, and VEGA faithfully announced "null allegiance".
     */
    private static void appendFact(StringBuilder sb, String key, String value) {
        if (value == null || value.isBlank()) return;
        sb.append(localizedEvent(key, value)).append(" ");
    }

    /**
     * The game's own wording for a symbol-keyed field, falling back to the raw symbol on the rare
     * event that does not carry the translation.
     *
     * <p>WHY: the payload used to state "$economy_Extraction;" and leave VEGA to turn that
     * back into a word. It reads well enough in English and is guesswork in every other language,
     * while the event carries the game's own translation right beside the symbol. The symbol stays
     * the machine key, so the engineer test above still matches on it.
     */
    private static String localisedOrSymbol(String localised, String symbol) {
        return localised == null || localised.isBlank() ? symbol : localised;
    }
}
