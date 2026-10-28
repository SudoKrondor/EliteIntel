package elite.intel.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import elite.intel.gameapi.journal.events.ApproachSettlementEvent;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reads a settlement off real-shaped {@code ApproachSettlement} payloads. The journal leaves a field
 * out rather than reporting it empty, so what the announcement may claim is decided entirely by which
 * keys the event actually carried.
 *
 * <p>The payloads below are the ones recorded on 2026-08-13: Bogacki Prospecting Facility has no
 * {@code StationAllegiance}, which is what produced the spoken "null allegiance", and The Beach has
 * one.
 */
class ApproachSettlementFactsTest {

    private static ApproachSettlementEvent event(String bodyJson) {
        String json = """
                {"timestamp":"2026-08-13T22:37:39Z","event":"ApproachSettlement",%s}
                """.formatted(bodyJson);
        return new ApproachSettlementEvent(GsonFactory.getGson().fromJson(json, JsonObject.class));
    }

    @Test
    @DisplayName("a settlement with no reported allegiance is described without one")
    void missingAllegianceIsOmitted() {
        String facts = ApproachSettlementSubscriber.settlementFacts(event("""
                "Name":"Bogacki Prospecting Facility","MarketID":3908321536,
                "StationFaction":{"Name":"Helveti Power Industry"},
                "StationGovernment":"$government_Corporate;","StationGovernment_Localised":"Corporate",
                "StationEconomy":"$economy_Extraction;","StationEconomy_Localised":"Extraction",
                "StationServices":["dock","refuel"],"SystemAddress":1247427922275,"BodyID":31
                """));

        assertFalse(facts.contains("null"), "the payload must never offer a fact the journal did not report");
        assertFalse(facts.contains("Allegiance"), facts);
        assertTrue(facts.contains("Bogacki Prospecting Facility"), facts);
        assertTrue(facts.contains("Helveti Power Industry"), facts);
        assertTrue(facts.contains("Extraction"), facts);
    }

    @Test
    @DisplayName("the game's own wording is stated, not the raw symbol")
    void localisedWordingIsPreferred() {
        String facts = ApproachSettlementSubscriber.settlementFacts(event("""
                "Name":"Bogacki Prospecting Facility","MarketID":3908321536,
                "StationGovernment":"$government_Corporate;","StationGovernment_Localised":"Corporate",
                "StationEconomy":"$economy_Extraction;","StationEconomy_Localised":"Extraction",
                "SystemAddress":1247427922275,"BodyID":31
                """));

        assertTrue(facts.contains("Extraction"), facts);
        assertTrue(facts.contains("Corporate"), facts);
        assertFalse(facts.contains("$economy_"), "the commander is not read journal symbols: " + facts);
        assertFalse(facts.contains("$government_"), facts);
    }

    @Test
    @DisplayName("an event without the translation still states the symbol rather than nothing")
    void symbolIsTheFallback() {
        String facts = ApproachSettlementSubscriber.settlementFacts(event("""
                "Name":"Bogacki Prospecting Facility","MarketID":3908321536,
                "StationEconomy":"$economy_Extraction;","SystemAddress":1247427922275,"BodyID":31
                """));

        assertTrue(facts.contains("$economy_Extraction;"), facts);
    }

    @Test
    @DisplayName("a settlement that does report an allegiance still states it")
    void reportedAllegianceIsKept() {
        String facts = ApproachSettlementSubscriber.settlementFacts(event("""
                "Name":"The Beach","MarketID":3221845248,
                "StationFaction":{"Name":"The Sarge"},"StationAllegiance":"Federation",
                "StationGovernment":"$government_Workshop;","StationGovernment_Localised":"Workshop",
                "StationEconomy":"$economy_Colony;","StationEconomy_Localised":"Colony",
                "SystemAddress":2870246379426,"BodyID":13
                """));

        assertTrue(facts.contains("Federation"), facts);
        assertFalse(facts.contains("null"), facts);
    }

    /**
     * An engineer base names its faction in the greeting, and that read the faction without checking
     * it was there while the line below it did check. A settlement stripped to its bare keys pins
     * both against the same absence.
     */
    @Test
    @DisplayName("a settlement with no faction at all is described, not thrown at")
    void missingFactionIsSurvived() {
        String facts = ApproachSettlementSubscriber.settlementFacts(event("""
                "Name":"Nameless Outpost","MarketID":0,
                "StationGovernment":"$government_Engineer;","SystemAddress":1,"BodyID":2
                """));

        assertTrue(facts.contains("Nameless Outpost"), facts);
        assertFalse(facts.contains("null"), facts);
    }

    /**
     * Recorded 2026-10-28 at Synuefe TP-F b44-0 CD 1: Guardian ruins carry a symbol for a name, the game's
     * wording beside it, and nothing else. VEGA read out "$Ancient:#index=3;" and invented "Trade and Repair".
     */
    private static ApproachSettlementEvent guardianRuins() {
        return event("""
                "Name":"$Ancient:#index=3;","Name_Localised":"Ancient Ruins (3)",
                "SystemAddress":682228131193,"BodyID":16,"BodyName":"Synuefe TP-F b44-0 CD 1",
                "Latitude":59.331982,"Longitude":15.105293
                """);
    }

    @Test
    @DisplayName("Guardian ruins are named in the game's wording, never by their symbol")
    void guardianRuinsAreNamedNotSymbolised() {
        String facts = ApproachSettlementSubscriber.settlementFacts(guardianRuins());

        assertTrue(facts.contains("Ancient Ruins (3)"), facts);
        assertFalse(facts.contains("$"), "the commander is not read journal symbols: " + facts);
        assertFalse(facts.contains("Services"), "the ruins reported no services: " + facts);
        assertTrue(guardianRuins().memorySummary().contains("Ancient Ruins (3)"));
    }

    @Test
    @DisplayName("Guardian ruins are filed under their readable name")
    void guardianRuinsAreRecordedByReadableName() {
        DockedStationRecord record = DockedStationRecord.of(guardianRuins(), "Synuefe TP-F b44-0");
        assertEquals("Ancient Ruins (3)", record.recordKey());
    }

    @Test
    @DisplayName("a name-only site is announced without any talk of services")
    void nameOnlyInstructionsNeverAskAboutServices() {
        assertFalse(ApproachSettlementSubscriber.narrationInstructions(true).toLowerCase().contains("service"));
        assertTrue(ApproachSettlementSubscriber.narrationInstructions(false).contains("Mention only what the data states"));
    }

    @Test
    @DisplayName("an ordinary settlement keeps its plain name")
    void plainNameIsUnchanged() {
        ApproachSettlementEvent event = event("""
                "Name":"Bogacki Prospecting Facility","MarketID":3908321536,"SystemAddress":1247427922275,"BodyID":31
                """);
        assertEquals("Bogacki Prospecting Facility", event.getDisplayName());
    }
}
