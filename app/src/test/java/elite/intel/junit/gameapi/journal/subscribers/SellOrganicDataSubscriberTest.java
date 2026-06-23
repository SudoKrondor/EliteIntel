package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.db.managers.CodexEntryManager;
import elite.intel.gameapi.journal.events.CodexEntryEvent;
import elite.intel.gameapi.journal.events.SellOrganicDataEvent;
import elite.intel.gameapi.journal.events.dto.BioSampleDto;
import elite.intel.gameapi.journal.subscribers.SellOrganicDataSubscriber;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SellOrganicDataSubscriberTest {

    private final SellOrganicDataSubscriber subscriber = new SellOrganicDataSubscriber();
    private final PlayerSession session = PlayerSession.getInstance();
    private final CodexEntryManager codex = CodexEntryManager.getInstance();

    @BeforeEach
    void clearSamples() {
        session.clearBioSamples();
        codex.clear();
    }

    @Test
    void sellOrganicDataClearsBioSamples() {
        BioSampleDto sample = new BioSampleDto();
        sample.setGenus("Bacterium Nebulus");
        sample.setSpecies("Nebulus");
        session.addBioSample(sample);

        subscriber.onSellOrganicDataEvent(sellOrganicDataEvent(500_000L));

        assertTrue(session.getBioCompletedSamples().isEmpty());
    }

    @Test
    void sellOrganicDataWithNoSamplesDoesNotThrow() {
        subscriber.onSellOrganicDataEvent(sellOrganicDataEvent(0L));

        assertTrue(session.getBioCompletedSamples().isEmpty());
    }

    @Test
    void sellOrganicDataClearsCodexEntries() {
        codex.save(codexEntry());
        assertFalse(codex.findAll().isEmpty(), "precondition: a codex entry was seeded");

        subscriber.onSellOrganicDataEvent(sellOrganicDataEvent(500_000L));

        assertTrue(codex.findAll().isEmpty(), "codex entries should be cleared after selling organic data");
    }

    private static CodexEntryEvent codexEntry() {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "CodexEntry");
        j.addProperty("EntryID", 2001L);
        j.addProperty("Name", "$Codex_Ent_Bacterial_01_Name;");
        j.addProperty("Name_Localised", "Bacterium Nebulus");
        j.addProperty("SubCategory", "$Codex_SubCategory_Organic_Structures;");
        j.addProperty("SubCategory_Localised", "Organic Structures");
        j.addProperty("Category", "$Codex_Category_Biology;");
        j.addProperty("System", "Sol");
        j.addProperty("SystemAddress", 10477373803L);
        j.addProperty("BodyID", 5L);
        j.addProperty("Latitude", 12.5);
        j.addProperty("Longitude", -3.25);
        j.addProperty("VoucherAmount", 50_000L);
        j.addProperty("IsNewEntry", true);
        return new CodexEntryEvent(j);
    }

    private static SellOrganicDataEvent sellOrganicDataEvent(long totalValue) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "SellOrganicData");
        j.addProperty("MarketID", 3700000001L);
        JsonArray bioData = new JsonArray();
        if (totalValue > 0) {
            JsonObject entry = new JsonObject();
            entry.addProperty("Genus", "$Codex_Ent_Bacterial_01_Name;");
            entry.addProperty("Genus_Localised", "Bacterium Nebulus");
            entry.addProperty("Species", "$Codex_Ent_Bacterial_01_Name;");
            entry.addProperty("Species_Localised", "Nebulus");
            entry.addProperty("Variant", "$Codex_Ent_Bacterial_01_Name;");
            entry.addProperty("Variant_Localised", "Nebulus");
            entry.addProperty("Value", totalValue);
            entry.addProperty("Bonus", 0L);
            bioData.add(entry);
        }
        j.add("BioData", bioData);
        return new SellOrganicDataEvent(j);
    }
}
