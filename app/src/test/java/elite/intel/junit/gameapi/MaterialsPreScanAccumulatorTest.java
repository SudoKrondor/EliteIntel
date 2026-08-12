package elite.intel.junit.gameapi;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.db.dao.MaterialNameDao;
import elite.intel.db.managers.MaterialManager;
import elite.intel.db.util.Database;
import elite.intel.gameapi.MaterialsPreScanAccumulator;
import elite.intel.gameapi.journal.events.*;
import elite.intel.gameapi.search.edsm.dto.MaterialsType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Startup drift: the live JournalParser skips everything written before app start ({@code isReplay()}),
 * so material movements that happened while the app was down were invisible and the stored counts were
 * whatever the last run left behind. This rebuilds them from the journal.
 */
class MaterialsPreScanAccumulatorTest {

    /**
     * Comfortably before app start, so every fixture counts as a replay.
     */
    private static final String T0 = "2026-01-01T00:00:00Z";
    private static final String T1 = "2026-01-01T00:05:00Z";
    private static final String T2 = "2026-01-01T00:10:00Z";

    private final MaterialManager materialManager = MaterialManager.getInstance();

    @BeforeEach
    void clearAmounts() throws InterruptedException {
        Thread.sleep(100);
        materialManager.clear();
    }

    @Test
    void theSnapshotAnchorsAndLaterMovementsAreReplayed() {
        MaterialsPreScanAccumulator acc = new MaterialsPreScanAccumulator();

        acc.onMaterials(snapshot(T0, "iron", 10));
        acc.onMaterialCollected(collected(T1, "iron", "Raw", 5));
        acc.onSynthesis(synthesis(T2, "iron", 2));
        acc.persist();

        assertEquals(13, held("iron"));
    }

    @Test
    void everyMovementKindIsReplayed() {
        MaterialsPreScanAccumulator acc = new MaterialsPreScanAccumulator();

        acc.onMaterials(snapshot(T0, "imperialshielding", 100));
        acc.onMaterialTrade(trade(T1, "imperialshielding", 50, "mechanicalcomponents", 75));
        acc.onEngineerCraft(craft(T1, "mechanicalcomponents", 5));
        acc.onMissionCompleted(missionReward(T2, "HybridCapacitors", 12));
        acc.persist();

        assertEquals(50, held("imperialshielding"));
        assertEquals(70, held("mechanicalcomponents"));
        // Mixed-case mission symbol still lands on the real row.
        assertEquals(12, held("hybridcapacitors"));
    }

    @Test
    void replayingTwiceGivesTheSameAnswer() {
        // The pre-scan re-reads the same journals on every launch, so the reconstruction has to be
        // idempotent. It is, because the anchor is absolute: nothing accumulates on top of the DB.
        for (int run = 0; run < 3; run++) {
            MaterialsPreScanAccumulator acc = new MaterialsPreScanAccumulator();
            acc.onMaterials(snapshot(T0, "iron", 10));
            acc.onMaterialCollected(collected(T1, "iron", "Raw", 5));
            acc.persist();

            assertEquals(15, held("iron"), "run " + run);
        }
    }

    @Test
    void aLaterSnapshotDiscardsWhateverCameBefore() {
        MaterialsPreScanAccumulator acc = new MaterialsPreScanAccumulator();

        acc.onMaterials(snapshot(T0, "iron", 10));
        acc.onMaterialCollected(collected(T0, "iron", "Raw", 200));
        acc.onMaterials(snapshot(T1, "iron", 42));   // second game load: absolute truth again
        acc.persist();

        assertEquals(42, held("iron"));
    }

    @Test
    void materialsAbsentFromTheSnapshotGoToZero() {
        // A Materials snapshot lists everything held, omitting anything at zero — so absent means none,
        // and a count left over from a previous run must not survive.
        leftOverFromAPreviousRun("nickel", 250);

        MaterialsPreScanAccumulator acc = new MaterialsPreScanAccumulator();
        acc.onMaterials(snapshot(T0, "iron", 10));
        acc.persist();

        assertEquals(0, held("nickel"));
        assertEquals(10, held("iron"));
    }

    @Test
    void withoutAnAnchorNothingIsWritten() {
        leftOverFromAPreviousRun("nickel", 250);

        MaterialsPreScanAccumulator acc = new MaterialsPreScanAccumulator();
        acc.onMaterialCollected(collected(T1, "iron", "Raw", 5));   // no Materials snapshot seen
        acc.persist();

        assertEquals(250, held("nickel"), "stored counts must stand when there is nothing to anchor on");
        assertEquals(0, held("iron"));
    }

    @Test
    void aLiveReportDuringTheRebuildIsNotOverwritten() {
        // The rebuild runs concurrently with the live parser. A material the live journal has already
        // reported on is newer than anything a replay can know, so the replay must leave it alone —
        // otherwise the absolute write would silently undo a pickup made in the first seconds of play.
        leftOverFromAPreviousRun("iron", 40);

        MaterialsPreScanAccumulator acc = new MaterialsPreScanAccumulator();
        acc.onMaterials(snapshot(T0, "iron", 10));

        materialManager.collect("iron", MaterialsType.GAME_RAW, 3, null);   // live pickup mid-rebuild
        acc.persist();

        assertEquals(43, held("iron"));
    }

    @Test
    void anUntouchedMaterialIsStillRebuiltWhenAnotherWasReportedLive() {
        leftOverFromAPreviousRun("iron", 40);
        leftOverFromAPreviousRun("nickel", 40);

        MaterialsPreScanAccumulator acc = new MaterialsPreScanAccumulator();
        acc.onMaterials(snapshot(T0, "nickel", 10));

        materialManager.collect("iron", MaterialsType.GAME_RAW, 3, null);
        acc.persist();

        assertEquals(43, held("iron"), "reported live, so left alone");
        assertEquals(10, held("nickel"), "never reported live, so rebuilt from the journal");
    }

    /**
     * What the last run left in the database — written straight to the row, with no live report.
     */
    private static void leftOverFromAPreviousRun(String symbol, int amount) {
        Database.withDao(MaterialNameDao.class, dao -> {
            dao.setAmount(symbol, amount);
            return null;
        });
    }

    @Test
    void eventsFromAfterAppStartAreLeftToTheLivePath() {
        // App start is the handoff boundary: the live subscribers own anything at or after it, so
        // counting it here too would double it.
        String now = Instant.now().toString();

        MaterialsPreScanAccumulator acc = new MaterialsPreScanAccumulator();
        acc.onMaterials(snapshot(T0, "iron", 10));
        acc.onMaterialCollected(collected(now, "iron", "Raw", 5));
        acc.persist();

        assertEquals(10, held("iron"));
    }

    @Test
    void aLiveSnapshotDoesNotAnchor() {
        MaterialsPreScanAccumulator acc = new MaterialsPreScanAccumulator();
        acc.onMaterials(snapshot(Instant.now().toString(), "iron", 10));
        acc.persist();

        assertEquals(0, held("iron"), "a snapshot at/after app start belongs to the live subscriber");
    }

    @Test
    void aReplayedPickupCannotExceedTheStorageCap() {
        // Imperial Shielding is grade 5, capped at 100 — the ceiling the game itself enforces.
        MaterialsPreScanAccumulator acc = new MaterialsPreScanAccumulator();
        acc.onMaterials(snapshot(T0, "imperialshielding", 98));
        acc.onMaterialCollected(collected(T1, "imperialshielding", "Manufactured", 9));
        acc.persist();

        assertEquals(100, held("imperialshielding"));
    }

    @Test
    void spendingMoreThanHeldFloorsAtZero() {
        MaterialsPreScanAccumulator acc = new MaterialsPreScanAccumulator();
        acc.onMaterials(snapshot(T0, "iron", 3));
        acc.onSynthesis(synthesis(T1, "iron", 10));
        acc.persist();

        assertEquals(0, held("iron"));
    }

    // ── fixtures ─────────────────────────────────────────────────────────────

    private int held(String symbol) {
        var material = materialManager.find(symbol);
        return material == null ? 0 : material.getAmount();
    }

    private static MaterialsEvent snapshot(String timestamp, String symbol, int count) {
        String json = """
                { "timestamp":"%s", "event":"Materials",
                  "Raw":[ { "Name":"%s", "Count":%d } ], "Manufactured":[], "Encoded":[] }
                """.formatted(timestamp, symbol, count);
        return new MaterialsEvent(json(json));
    }

    private static MaterialCollectedEvent collected(String timestamp, String symbol, String category, int count) {
        String json = """
                { "timestamp":"%s", "event":"MaterialCollected", "Category":"%s", "Name":"%s", "Count":%d }
                """.formatted(timestamp, category, symbol, count);
        return new MaterialCollectedEvent(json(json));
    }

    private static SynthesisEvent synthesis(String timestamp, String symbol, int count) {
        String json = """
                { "timestamp":"%s", "event":"Synthesis", "Name":"Repair Basic",
                  "Materials":[ { "Name":"%s", "Count":%d } ] }
                """.formatted(timestamp, symbol, count);
        return new SynthesisEvent(json(json));
    }

    private static MaterialTradeEvent trade(String timestamp, String paid, int paidQty, String received, int receivedQty) {
        String json = """
                { "timestamp":"%s", "event":"MaterialTrade", "MarketID":3223896576, "TraderType":"manufactured",
                  "Paid":{ "Material":"%s", "Category":"Manufactured", "Quantity":%d },
                  "Received":{ "Material":"%s", "Category":"Manufactured", "Quantity":%d } }
                """.formatted(timestamp, paid, paidQty, received, receivedQty);
        return new MaterialTradeEvent(json(json));
    }

    private static EngineerCraftEvent craft(String timestamp, String symbol, int count) {
        String json = """
                { "timestamp":"%s", "event":"EngineerCraft", "Slot":"MainEngines", "Module":"int_engine_size3_class5",
                  "Engineer":"Elvira Martuuk", "EngineerID":300160, "BlueprintID":128673638,
                  "BlueprintName":"Engine_Dirty", "Level":1, "Quality":1.0,
                  "Ingredients":[ { "Name":"%s", "Count":%d } ], "Modifiers":[] }
                """.formatted(timestamp, symbol, count);
        return new EngineerCraftEvent(json(json));
    }

    private static MissionCompletedEvent missionReward(String timestamp, String symbol, int count) {
        String json = """
                { "timestamp":"%s", "event":"MissionCompleted", "Faction":"Sol Nationalists",
                  "Name":"Mission_Delivery_name", "MissionID":1062994204, "Reward":1000,
                  "MaterialsReward":[ { "Name":"%s", "Category":"$MICRORESOURCE_CATEGORY_Manufactured;",
                                        "Category_Localised":"Manufactured", "Count":%d } ] }
                """.formatted(timestamp, symbol, count);
        return new MissionCompletedEvent(json(json));
    }

    private static JsonObject json(String journalLine) {
        return JsonParser.parseString(journalLine).getAsJsonObject();
    }
}
