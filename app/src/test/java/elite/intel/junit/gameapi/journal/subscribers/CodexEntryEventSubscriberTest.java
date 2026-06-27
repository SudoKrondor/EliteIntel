package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import elite.intel.gameapi.journal.events.CodexEntryEvent;
import elite.intel.gameapi.journal.subscribers.CodexEntryEventSubscriber;
import elite.intel.session.LocationData;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CodexEntryEventSubscriberTest {

    private final CodexEntryEventSubscriber subscriber = new CodexEntryEventSubscriber();
    private final PlayerSession session = PlayerSession.getInstance();

    @Test
    void nullBodyIdDoesNotUpdateLocationId() throws InterruptedException {
        long uniqueSystem = 60001000L;
        long knownBodyId = 33L;
        session.setCurrentLocationId(knownBodyId, uniqueSystem);

        subscriber.onCodexEntryEvent(codexEntryEventNullBodyId(uniqueSystem));

        Thread.sleep(300);

        LocationData<Long, Long> loc = session.getLocationData();
        assertEquals(knownBodyId, loc.getInGameId(), "null BodyID must not overwrite current_location_id");
    }

    private static CodexEntryEvent codexEntryEventNullBodyId(long systemAddress) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "CodexEntry");
        j.addProperty("EntryID", 1001L);
        j.addProperty("Name", "$Codex_Ent_Bacterium_01_Name;");
        j.addProperty("Name_Localised", "Bacterium Aurasus");
        j.addProperty("SubCategory", "$Codex_SubCategory_Organic_Structures;");
        j.addProperty("SubCategory_Localised", "Organic Structures");
        j.addProperty("Category", "$Codex_Category_Biology;");
        j.addProperty("Category_Localised", "Biology");
        j.addProperty("Region", "$Codex_RegionName_18;");
        j.addProperty("Region_Localised", "Inner Orion Spur");
        j.addProperty("System", "Sol");
        j.addProperty("SystemAddress", systemAddress);
        j.addProperty("IsNewEntry", true);
        // BodyID intentionally omitted — Gson will deserialise it as null
        return new CodexEntryEvent(j);
    }
}
