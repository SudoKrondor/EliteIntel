package elite.intel.util.json;

import com.google.gson.JsonParser;
import elite.intel.gameapi.journal.events.TouchdownEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Frontier leaks a literal {@code &NBSP;} into localised journal text. It used to reach the
 * commander: the prompt builder escaped the ampersand on the way into the XML payload and the model
 * copied "Crash Site [Threat&amp;NBSP;1]" straight back into what it said.
 */
class JournalTextTypeAdapterTest {

    @Test
    @DisplayName("a localised name arrives without the entity in it")
    void touchdownNameIsReadable() {
        // Verbatim from a commander's journal.
        String line = """
                { "timestamp":"2026-08-27T16:00:22Z", "event":"Touchdown", "PlayerControlled":true,
                  "StarSystem":"HR 1507", "SystemAddress":237533914284, "Body":"HR 1507 7 b", "BodyID":75,
                  "OnStation":false, "OnPlanet":true, "Latitude":79.177780, "Longitude":80.436455,
                  "NearestDestination":"$POIScenario_Watson_Wrecks_Sidewinder_01_Salvage_Easy; $USS_ThreatLevel:#threatLevel=1;",
                  "NearestDestination_Localised":"Crash Site [Threat&NBSP;1]" }
                """;

        TouchdownEvent event = new TouchdownEvent(JsonParser.parseString(line).getAsJsonObject());

        assertEquals("Crash Site [Threat 1]", event.getNearestDestinationLocalised());
    }

    @Test
    @DisplayName("the entity is decoded whatever case the game wrote it in")
    void caseInsensitive() {
        assertEquals("Threat 1", JournalTextTypeAdapter.decode("Threat&NBSP;1"));
        assertEquals("Threat 1", JournalTextTypeAdapter.decode("Threat&nbsp;1"));
    }

    @Test
    @DisplayName("ordinary text is handed back untouched, ampersands included")
    void leavesEverythingElseAlone() {
        String plain = "Hutton Orbital";
        assertSame(plain, JournalTextTypeAdapter.decode(plain), "no ampersand, so no work done");
        assertEquals("Smith & Sons Holdings", JournalTextTypeAdapter.decode("Smith & Sons Holdings"));
        assertNull(JournalTextTypeAdapter.decode(null));
    }
}
