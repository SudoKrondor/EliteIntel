package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.vega.CompanionRuntime;
import elite.intel.gameapi.journal.events.ApproachSettlementEvent;
import elite.intel.session.PlayerSession;

import java.util.List;

import static elite.intel.util.StringUtls.localizedEvent;

public class ApproachSettlementSubscriber {

    private final PlayerSession playerSession = PlayerSession.getInstance();

    @Subscribe
    public void onApproachSettlementEvent(ApproachSettlementEvent event) {
        Thread.ofVirtual().start(() -> {
            StringBuilder sb = new StringBuilder(settlementFacts(event));

            String availableData = LocalServicesData.setLocalServicesData(event.getMarketID());
            if (!availableData.isEmpty()) sb.append(" ").append(localizedEvent("event.approach.settlement.moreData"));

            if (playerSession.isRouteAnnouncementOn()) {
                String instructions = """
                            Approaching settlement.
                            Provide very brief summary for the settlement data.
                            Do not list every service.
                        """;
                CompanionRuntime.narrator().narrate(sb.toString(), instructions);
            }
        });
    }

    /**
     * The settlement as the journal described it, as labelled facts for the companion to summarise.
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
     * "Allegiance: null." into the payload, and the companion faithfully announced "null allegiance".
     */
    private static void appendFact(StringBuilder sb, String key, String value) {
        if (value == null || value.isBlank()) return;
        sb.append(localizedEvent(key, value)).append(" ");
    }

    /**
     * The game's own wording for a symbol-keyed field, falling back to the raw symbol on the rare
     * event that does not carry the translation.
     *
     * <p>WHY: the payload used to state "$economy_Extraction;" and leave the companion to turn that
     * back into a word. It reads well enough in English and is guesswork in every other language,
     * while the event carries the game's own translation right beside the symbol. The symbol stays
     * the machine key, so the engineer test above still matches on it.
     */
    private static String localisedOrSymbol(String localised, String symbol) {
        return localised == null || localised.isBlank() ? symbol : localised;
    }
}
