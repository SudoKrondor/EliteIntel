package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import elite.intel.companion.CompanionNarrator;
import elite.intel.companion.CompanionRuntimeGraph;
import elite.intel.companion.CompanionRuntimeTestSupport;
import elite.intel.gameapi.journal.events.ShipyardBuyEvent;
import elite.intel.gameapi.journal.subscribers.NewShipPurchasedHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NewShipPurchasedHandlerTest {

    private final NewShipPurchasedHandler handler = new NewShipPurchasedHandler();
    private final CapturingNarrator narrator = new CapturingNarrator();
    private CompanionRuntimeGraph runtimeGraph;

    @BeforeEach
    void installNarrator() {
        runtimeGraph = CompanionRuntimeTestSupport.installNarrator(narrator);
    }

    @AfterEach
    void clearNarrator() {
        CompanionRuntimeTestSupport.uninstall(runtimeGraph);
    }

    private ShipyardBuyEvent buyEvent(String shipType) {
        JsonObject json = new JsonObject();
        json.addProperty("timestamp", "2025-01-01T12:00:00Z");
        json.addProperty("ShipType", shipType);
        json.addProperty("ShipPrice", 100000);
        return new ShipyardBuyEvent(json);
    }

    private ShipyardBuyEvent buyEvent(String shipType, String shipTypeLocalised) {
        JsonObject json = new JsonObject();
        json.addProperty("timestamp", "2025-01-01T12:00:00Z");
        json.addProperty("ShipType", shipType);
        json.addProperty("ShipType_Localised", shipTypeLocalised);
        json.addProperty("ShipPrice", 100000);
        return new ShipyardBuyEvent(json);
    }

    @Test
    void usesLocalisedNameDirectlyWhenPresent() {
        // Codename absent from the seed table: only the journal's localised value can supply the name.
        handler.onNewShipPurchased(buyEvent("brandnewmake_nx", "Brand New Make"));
        assertNotNull(narrator.data);
        assertTrue(narrator.data.contains("Brand New Make"),
                "Expected localised name in message but got: " + narrator.data);
        assertFalse(narrator.data.contains("brandnewmake_nx"));
    }

    @Test
    void resolvesSeededShipTypeToDisplayName() {
        handler.onNewShipPurchased(buyEvent("type9_military"));
        assertNotNull(narrator.data);
        assertTrue(narrator.data.contains("Type-10 Defender"),
                "Expected display name in message but got: " + narrator.data);
        assertFalse(narrator.data.contains("type9_military"));
    }

    @Test
    void titleCasesUnknownShipType() {
        handler.onNewShipPurchased(buyEvent("unknownship"));
        assertNotNull(narrator.data);
        assertTrue(narrator.data.contains("Unknownship"),
                "Expected title-cased name but got: " + narrator.data);
        assertFalse(narrator.data.contains("unknownship"),
                "Raw lowercase codename should not appear in message");
    }

    /** Captures the data the handler hands the companion to narrate, so the test can assert on the message. */
    private static final class CapturingNarrator implements CompanionNarrator {
        String data;

        @Override public void filler(String text, boolean urgent) { }
        @Override public void narrate(String data, String instructions) { this.data = data; }
        @Override public void announce(String phrase, boolean urgent) { }
    }
}
