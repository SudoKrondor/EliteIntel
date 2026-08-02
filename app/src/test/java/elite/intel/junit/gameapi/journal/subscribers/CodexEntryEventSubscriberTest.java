package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import elite.intel.gameapi.journal.events.CodexEntryEvent;
import elite.intel.gameapi.journal.subscribers.CodexEntryEventSubscriber;
import elite.intel.session.LocationData;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

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

    /**
     * Payload/instruction parity for money: the organic payload can carry a report voucher, a Vista Genomics
     * payment and a first-discovery bonus, so the instructions must name all three. When the voucher had no
     * slot, the model gave it one - a 2500 voucher was voiced as "2.5K per sample", a rate that exists nowhere.
     */
    @Test
    void organicInstructionsAccountForEveryCreditFigureAndBanDerivedRates() {
        String instructions = CodexEntryEventSubscriber.narrationInstructions(true).toLowerCase();

        assertTrue(instructions.contains("voucher"), "voucher figure must have a sanctioned slot: " + instructions);
        assertTrue(instructions.contains("vista genomics"), "set-of-three payment must be named: " + instructions);
        assertTrue(instructions.contains("first-discovery bonus"), "bonus figure must be named: " + instructions);
        assertTrue(instructions.contains("per-sample rate"), "the absent per-sample rate must be ruled out: " + instructions);
        assertTrue(instructions.contains("never relabel"), "relabelling must be forbidden: " + instructions);
    }

    /**
     * A non-organic entry can still carry a voucher (a real geology entry carried 2500), and it is the only
     * credit figure there - a non-organic name resolves to a null genus, so no Vista payment is appended.
     * The branch must therefore name the voucher while still refusing every bio concept.
     */
    @Test
    void nonOrganicInstructionsNameTheVoucherButExcludeBioConcepts() {
        String instructions = CodexEntryEventSubscriber.narrationInstructions(false).toLowerCase();

        assertTrue(instructions.contains("not a biological/organic entry"), instructions);
        assertTrue(instructions.contains("voucher"), "the voucher is the one figure here and needs a slot: " + instructions);
        assertFalse(instructions.contains("vista genomics"), "no bio payment slot on a non-organic entry: " + instructions);
        assertTrue(instructions.contains("never relabel"), "relabelling must be forbidden here too: " + instructions);
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
