package elite.intel.junit.gameapi;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.gameapi.journal.events.MissionAcceptedEvent;
import elite.intel.gameapi.journal.events.dto.MissionDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The event copies the parsed fields across one by one, and two of them were being dropped:
 * {@code Commodity} and {@code DestinationSettlement}. The settlement is the whole destination
 * of an on-foot mission, so every such mission was stored without one.
 */
class MissionAcceptedEventTest {

    private static final String ON_FOOT_MISSION = """
            { "timestamp":"2026-08-21T07:41:13Z", "event":"MissionAccepted", "Faction":"Cadubii Partners",
              "Name":"Mission_OnFoot_Collect_MB", "LocalisedName":"Recover 8 units of Biological Sample",
              "Commodity":"$BiologicalSample_Name;", "Commodity_Localised":"Biological Sample", "Count":8,
              "DestinationSystem":"Cadubii", "DestinationStation":"Abe Gateway",
              "DestinationSettlement":"Kelly Chemical Plant", "Expiry":"2026-08-22T07:40:51Z",
              "Wing":false, "Influence":"++", "Reputation":"++", "Reward":435233, "MissionID":1063919475 }
            """;

    private static MissionAcceptedEvent parse() {
        JsonObject json = JsonParser.parseString(ON_FOOT_MISSION).getAsJsonObject();
        return new MissionAcceptedEvent(json);
    }

    @Test
    @DisplayName("the settlement destination survives parsing and reaches the stored mission")
    void carriesDestinationSettlement() {
        MissionAcceptedEvent event = parse();
        assertEquals("Kelly Chemical Plant", event.getDestinationSettlement());
        assertEquals("Kelly Chemical Plant", new MissionDto(event).getDestinationSettlement());
    }

    @Test
    @DisplayName("the commodity symbol survives parsing but stays out of the narration payload")
    void carriesCommoditySymbolWithoutSpeakingIt() {
        MissionAcceptedEvent event = parse();
        assertEquals("$BiologicalSample_Name;", event.getCommodity());
        assertEquals("Biological Sample", event.getCommodityLocalised());

        String yaml = event.toYaml();
        assertFalse(yaml.contains("$BiologicalSample_Name;"), "narration payload carries a raw symbol:\n" + yaml);
    }
}
