package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.db.managers.MaterialManager;
import elite.intel.gameapi.journal.EventRegistry;
import elite.intel.gameapi.journal.events.*;
import elite.intel.gameapi.journal.subscribers.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The remaining events that move materials without being a pickup, an engineering roll, or a trade.
 * Each one was unregistered and unhandled, so material they consumed stayed on the books forever and
 * material they paid out was never credited.
 */
class MaterialConsumptionSubscribersTest {

    private final MaterialCollectedSubscriber collectedSubscriber = new MaterialCollectedSubscriber();
    private final MaterialDiscardedSubscriber discardedSubscriber = new MaterialDiscardedSubscriber();
    private final SynthesisSubscriber synthesisSubscriber = new SynthesisSubscriber();
    private final TechnologyBrokerSubscriber brokerSubscriber = new TechnologyBrokerSubscriber();
    private final ScientificResearchSubscriber researchSubscriber = new ScientificResearchSubscriber();
    private final MissionMaterialsRewardSubscriber missionRewardSubscriber = new MissionMaterialsRewardSubscriber();
    private final MaterialManager materialManager = MaterialManager.getInstance();

    @BeforeEach
    void clearAmounts() throws InterruptedException {
        Thread.sleep(100);
        materialManager.clear();
    }

    // ── registration ─────────────────────────────────────────────────────────

    @Test
    void everyConsumingEventIsRegistered() {
        // createEventForPreScan applies no recency gate, so this asserts registration alone.
        assertInstanceOf(MaterialDiscardedEvent.class,
                EventRegistry.createEventForPreScan("MaterialDiscarded", json(DISCARD_IRON)));
        assertInstanceOf(SynthesisEvent.class,
                EventRegistry.createEventForPreScan("Synthesis", json(SYNTHESIS_AMMO)));
        assertInstanceOf(TechnologyBrokerEvent.class,
                EventRegistry.createEventForPreScan("TechnologyBroker", json(BROKER_UNLOCK)));
        assertInstanceOf(ScientificResearchEvent.class,
                EventRegistry.createEventForPreScan("ScientificResearch", json(RESEARCH_DONATION)));
    }

    // ── MaterialDiscarded ────────────────────────────────────────────────────

    @Test
    void discardingRemovesTheMaterial() throws InterruptedException {
        collectedSubscriber.onMaterialCollected(collected("iron", "Raw", 30, null));

        discardedSubscriber.onMaterialDiscarded(new MaterialDiscardedEvent(json(DISCARD_IRON)));

        awaitAmount("iron", 25);
    }

    // ── Synthesis ────────────────────────────────────────────────────────────

    @Test
    void synthesisDeductsEveryIngredient() throws InterruptedException {
        // Verbatim recipe from the commander's own journal: nickel 3, carbon 3, sulphur 3, tungsten 2.
        collectedSubscriber.onMaterialCollected(collected("nickel", "Raw", 10, null));
        collectedSubscriber.onMaterialCollected(collected("carbon", "Raw", 10, null));
        collectedSubscriber.onMaterialCollected(collected("sulphur", "Raw", 10, null));
        collectedSubscriber.onMaterialCollected(collected("tungsten", "Raw", 10, null));

        synthesisSubscriber.onSynthesis(new SynthesisEvent(json(SYNTHESIS_AMMO)));

        awaitAmount("nickel", 7);
        awaitAmount("carbon", 7);
        awaitAmount("sulphur", 7);
        awaitAmount("tungsten", 8);
    }

    // ── TechnologyBroker ─────────────────────────────────────────────────────

    @Test
    void brokerUnlockDeductsMaterialsButNotCommodities() throws InterruptedException {
        collectedSubscriber.onMaterialCollected(collected("guardian_powercell", "Manufactured", 30, "Guardian Power Cell"));
        collectedSubscriber.onMaterialCollected(collected("guardian_techcomponent", "Manufactured", 20, "Guardian Technology Component"));

        brokerSubscriber.onTechnologyBroker(new TechnologyBrokerEvent(json(BROKER_UNLOCK)));

        awaitAmount("guardian_powercell", 6);
        awaitAmount("guardian_techcomponent", 14);
    }

    // ── ScientificResearch ───────────────────────────────────────────────────

    @Test
    void researchDonationRemovesTheMaterial() throws InterruptedException {
        collectedSubscriber.onMaterialCollected(collected("tellurium", "Raw", 20, null));

        researchSubscriber.onScientificResearch(new ScientificResearchEvent(json(RESEARCH_DONATION)));

        awaitAmount("tellurium", 10);
    }

    // ── MissionCompleted MaterialsReward ─────────────────────────────────────

    @Test
    void missionRewardIsCreditedDespiteMixedCaseSymbolAndTokenCategory() throws InterruptedException {
        // Verbatim from the commander's journal: Name is "HybridCapacitors", not "hybridcapacitors",
        // and Category is "$MICRORESOURCE_CATEGORY_Manufactured;". Matched raw, this credit would land
        // on a second row and the real one would never move.
        missionRewardSubscriber.onMissionCompleted(new MissionCompletedEvent(json(MISSION_WITH_MATERIAL_REWARD)));

        awaitAmount("hybridcapacitors", 12);
    }

    @Test
    void missionRewardIsCreditedEvenWhenTheMissionIsNotInSessionStorage() throws InterruptedException {
        // MissionCompletedSubscriber gives up on an unknown mission; the materials still arrive, which
        // is why this credit lives in its own subscriber.
        collectedSubscriber.onMaterialCollected(collected("hybridcapacitors", "Manufactured", 5, "Hybrid Capacitors"));

        missionRewardSubscriber.onMissionCompleted(new MissionCompletedEvent(json(MISSION_WITH_MATERIAL_REWARD)));

        awaitAmount("hybridcapacitors", 17);
    }

    @Test
    void aMissionWithoutMaterialsTouchesNothing() throws InterruptedException {
        collectedSubscriber.onMaterialCollected(collected("iron", "Raw", 30, null));
        awaitAmount("iron", 30);

        missionRewardSubscriber.onMissionCompleted(new MissionCompletedEvent(json(MISSION_WITHOUT_MATERIAL_REWARD)));

        Thread.sleep(200);
        assertEquals(30, materialManager.find("iron").getAmount());
    }

    // ── journal fixtures ─────────────────────────────────────────────────────

    /**
     * Verbatim from Journal.2026-07-29T*.log.
     */
    private static final String SYNTHESIS_AMMO = """
            { "timestamp":"2026-07-29T05:12:21Z", "event":"Synthesis", "Name":"Sub-surface Displacement Ammo",
              "Materials":[ { "Name":"nickel", "Count":3 }, { "Name":"carbon", "Count":3 },
                            { "Name":"sulphur", "Count":3 }, { "Name":"tungsten", "Count":2 } ] }
            """;

    /**
     * Verbatim from Journal.2026-08-10T*.log, trimmed to the fields this ledger reads.
     */
    private static final String MISSION_WITH_MATERIAL_REWARD = """
            { "timestamp":"2026-08-10T06:57:38Z", "event":"MissionCompleted", "Faction":"Sol Nationalists",
              "Name":"Mission_Delivery_RankFed_name", "LocalisedName":"Federal Navy Supply Mission",
              "MissionID":1062994204, "Reward":1039752,
              "MaterialsReward":[ { "Name":"HybridCapacitors", "Name_Localised":"Hybrid Capacitors",
                                    "Category":"$MICRORESOURCE_CATEGORY_Manufactured;",
                                    "Category_Localised":"Manufactured", "Count":12 } ] }
            """;

    private static final String MISSION_WITHOUT_MATERIAL_REWARD = """
            { "timestamp":"2026-08-10T07:11:02Z", "event":"MissionCompleted", "Faction":"Sol Nationalists",
              "Name":"Mission_Delivery_name", "LocalisedName":"Delivery", "MissionID":1062994205, "Reward":50000 }
            """;

    private static final String DISCARD_IRON = """
            { "timestamp":"2026-08-12T04:00:00Z", "event":"MaterialDiscarded", "Category":"Raw",
              "Name":"iron", "Count":5 }
            """;

    private static final String BROKER_UNLOCK = """
            { "timestamp":"2026-08-12T04:05:00Z", "event":"TechnologyBroker", "BrokerType":"guardian",
              "MarketID":128678535,
              "ItemsUnlocked":[ { "Name":"Hpt_Guardian_PlasmaLauncher_Fixed_Medium",
                                  "Name_Localised":"Guardian Plasma Charger" } ],
              "Commodities":[ { "Name":"powertransferbus", "Name_Localised":"Power Transfer Bus", "Count":6 } ],
              "Materials":[ { "Name":"guardian_powercell", "Name_Localised":"Guardian Power Cell",
                              "Category":"Manufactured", "Count":24 },
                            { "Name":"guardian_techcomponent", "Name_Localised":"Guardian Technology Component",
                              "Category":"Manufactured", "Count":6 } ] }
            """;

    private static final String RESEARCH_DONATION = """
            { "timestamp":"2026-08-12T04:10:00Z", "event":"ScientificResearch", "MarketID":128678535,
              "Name":"tellurium", "Category":"Raw", "Count":10 }
            """;

    private static JsonObject json(String journalLine) {
        return JsonParser.parseString(journalLine).getAsJsonObject();
    }

    private static MaterialCollectedEvent collected(String symbol, String category, int count, String localised) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "MaterialCollected");
        j.addProperty("Name", symbol);
        j.addProperty("Category", category);
        j.addProperty("Count", count);
        if (localised != null) j.addProperty("Name_Localised", localised);
        return new MaterialCollectedEvent(j);
    }

    private void awaitAmount(String symbol, int expected) throws InterruptedException {
        awaitTrue(() -> {
            var mat = materialManager.find(symbol);
            return mat != null && mat.getAmount() == expected;
        });
        assertNotNull(materialManager.find(symbol), symbol);
        assertEquals(expected, materialManager.find(symbol).getAmount(), symbol);
    }

    private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) fail("Condition not met within 2 seconds");
            Thread.sleep(10);
        }
    }
}
