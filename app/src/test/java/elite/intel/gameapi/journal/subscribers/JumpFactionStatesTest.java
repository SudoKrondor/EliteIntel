package elite.intel.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import elite.intel.gameapi.journal.events.FSDJumpEvent;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reads faction states off a real-shaped {@code FSDJump} payload. The High Grade Emissions advice
 * hangs off these strings, so the shape of the journal is what has to be right here.
 */
class JumpFactionStatesTest {

    private static FSDJumpEvent event(String factionsJson) {
        String json = """
                {"timestamp":"2026-08-08T18:00:00Z","event":"FSDJump",
                 "StarSystem":"Wolf 359","SystemAddress":1729531826338,
                 "SystemAllegiance":"Federation","Population":8000000,
                 "Factions":%s}
                """.formatted(factionsJson);
        return new FSDJumpEvent(GsonFactory.getGson().fromJson(json, JsonObject.class));
    }

    @Test
    @DisplayName("the dominant state and every active state are both collected")
    void collectsDominantAndActiveStates() {
        FSDJumpEvent event = event("""
                [{"Name":"Wolf 359 Alliance","FactionState":"Boom",
                  "ActiveStates":[{"State":"Boom"},{"State":"War"}]},
                 {"Name":"Independents of Wolf 359","FactionState":"CivilUnrest",
                  "ActiveStates":[{"State":"CivilUnrest"}]}]
                """);

        List<String> states = JumpCompletedSubscriber.factionStates(event);

        assertTrue(states.contains("Boom"));
        assertTrue(states.contains("War"), "a secondary state only ever appears in ActiveStates");
        assertTrue(states.contains("CivilUnrest"));
    }

    @Test
    @DisplayName("a faction with no active states still contributes its dominant one")
    void factionWithoutActiveStates() {
        FSDJumpEvent event = event("""
                [{"Name":"Wolf 359 Alliance","FactionState":"Expansion"}]
                """);

        assertEquals(List.of("Expansion"), JumpCompletedSubscriber.factionStates(event));
    }

    @Test
    @DisplayName("an uninhabited system has no factions at all")
    void noFactions() {
        String json = """
                {"timestamp":"2026-08-08T18:00:00Z","event":"FSDJump",
                 "StarSystem":"Praea Euq WV-Z b1-2","SystemAddress":123,"Population":0}
                """;
        FSDJumpEvent event = new FSDJumpEvent(GsonFactory.getGson().fromJson(json, JsonObject.class));

        assertTrue(JumpCompletedSubscriber.factionStates(event).isEmpty());
    }
}
