package elite.intel.junit.gameapi;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.gameapi.MissionTitle;
import elite.intel.gameapi.MissionType;
import elite.intel.gameapi.journal.events.MissionAcceptedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A commander must never hear or read a journal key. The narration payload for an accepted
 * mission used to carry {@code name: Mission_Collect_RankEmp} next to the localised name, and
 * the LLM announced the key.
 */
class MissionTitleTest {

    @Test
    @DisplayName("the game's own localised name wins over the key")
    void prefersLocalisedName() {
        assertEquals("Source 45 Units of H.E. Suits for the Imperial Navy",
                MissionTitle.of("Mission_Collect_RankEmp", "Source 45 Units of H.E. Suits for the Imperial Navy"));
    }

    @Test
    @DisplayName("a missing localised name falls back to words, never the key")
    void spellsOutTheKey() {
        assertEquals("Collect Imperial Navy rank", MissionTitle.of("Mission_Collect_RankEmp", null));
        assertEquals("Collect Imperial Navy rank", MissionTitle.of("Mission_Collect_RankEmp", "  "));
        assertEquals("Assassinate Federal Navy rank", MissionTitle.fromKey("Mission_Assassinate_RankFed"));
        assertEquals("Massacre", MissionTitle.fromKey("Mission_Massacre"));
        assertEquals("Collect industrial", MissionTitle.fromKey("Mission_Collect_Industrial"));
    }

    @Test
    @DisplayName("completion and variant suffixes are dropped")
    void stripsJournalSuffixes() {
        assertEquals("Collect Imperial Navy rank", MissionTitle.fromKey("Mission_Collect_RankEmp_name"));
        assertEquals("On foot onslaught offline", MissionTitle.fromKey("Mission_OnFoot_Onslaught_Offline_002"));
    }

    @Test
    @DisplayName("a mission with no name at all still reads as words")
    void handlesMissingKey() {
        assertEquals("unspecified mission", MissionTitle.of(null, null));
        assertEquals("unspecified mission", MissionTitle.fromKey(""));
    }

    @Test
    @DisplayName("every mission type has a label free of underscores and raw casing")
    void everyTypeHasASpokenLabel() {
        for (MissionType type : MissionType.values()) {
            String label = type.label();
            assertFalse(label.isBlank(), type + " has a blank label");
            assertFalse(label.contains("_"), type + " label still carries a key: " + label);
            assertFalse(label.toLowerCase().startsWith("mission"), type + " label still says 'mission': " + label);
        }
    }

    @Test
    @DisplayName("the narration payload carries the title and not the key")
    void acceptedEventYamlHidesTheKey() {
        String json = """
                { "timestamp":"2026-08-21T07:41:13Z", "event":"MissionAccepted", "Faction":"Cadubii Partners",
                  "Name":"Mission_Collect_RankEmp", "LocalisedName":"Source 45 Units of H.E. Suits for the Imperial Navy",
                  "Commodity":"$HazardousEnvironmentSuits_Name;", "Commodity_Localised":"H.E. Suits", "Count":45,
                  "DestinationSystem":"Cadubii", "DestinationStation":"Abe Gateway", "Expiry":"2026-08-22T07:40:51Z",
                  "Wing":false, "Influence":"++", "Reputation":"++", "Reward":435233, "MissionID":1063919474 }
                """;
        JsonObject event = JsonParser.parseString(json).getAsJsonObject();
        MissionAcceptedEvent accepted = new MissionAcceptedEvent(event);

        assertEquals("Mission_Collect_RankEmp", accepted.getName(), "the key is still available for type lookup");

        String yaml = accepted.toYaml();
        assertFalse(yaml.contains("Mission_Collect_RankEmp"), "narration payload still carries the key:\n" + yaml);
        assertTrue(yaml.contains("Source 45 Units of H.E. Suits for the Imperial Navy"),
                "narration payload lost the mission title:\n" + yaml);
    }
}
