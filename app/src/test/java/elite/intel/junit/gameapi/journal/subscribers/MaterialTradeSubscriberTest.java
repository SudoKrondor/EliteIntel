package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.db.managers.MaterialManager;
import elite.intel.gameapi.journal.EventRegistry;
import elite.intel.gameapi.journal.events.BaseEvent;
import elite.intel.gameapi.journal.events.MaterialCollectedEvent;
import elite.intel.gameapi.journal.events.MaterialTradeEvent;
import elite.intel.gameapi.journal.subscribers.MaterialCollectedSubscriber;
import elite.intel.gameapi.journal.subscribers.MaterialTradeSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * MaterialTrade was unregistered and unhandled, so only the pickup half of the ledger was recorded:
 * material gathered was added and material spent at a trader was never removed. A commander who
 * gathered 100 Imperial Shielding and traded all of it away was still told they held 100.
 */
class MaterialTradeSubscriberTest {

    private final MaterialTradeSubscriber subscriber = new MaterialTradeSubscriber();
    private final MaterialCollectedSubscriber materialCollectedSubscriber = new MaterialCollectedSubscriber();
    private final MaterialManager materialManager = MaterialManager.getInstance();

    @BeforeEach
    void clearAmounts() throws InterruptedException {
        Thread.sleep(100);
        materialManager.clear();
    }

    @Test
    void materialTradeIsRegisteredSoTheJournalLineIsNotDropped() {
        // createEventForPreScan applies no recency gate, so this asserts registration alone.
        BaseEvent event = EventRegistry.createEventForPreScan("MaterialTrade", json(TRADE_IMPERIAL_SHIELDING));

        assertNotNull(event, "MaterialTrade must be registered or the journal line is dropped");
        MaterialTradeEvent trade = assertInstanceOf(MaterialTradeEvent.class, event);
        assertEquals("imperialshielding", trade.getPaid().getMaterial());
        assertEquals(50, trade.getPaid().getQuantity());
        assertEquals("mechanicalcomponents", trade.getReceived().getMaterial());
        assertEquals(75, trade.getReceived().getQuantity());
    }

    @Test
    void aLiveTradeIsBuiltByTheParserPath() {
        // The live path additionally drops anything older than its threshold, so this uses a fresh
        // timestamp: the trade must survive the gate the way it does when the trader is used with the
        // app running.
        JsonObject fresh = json(TRADE_IMPERIAL_SHIELDING);
        fresh.addProperty("timestamp", Instant.now().toString());

        assertInstanceOf(MaterialTradeEvent.class, EventRegistry.createEvent("MaterialTrade", fresh));
    }

    @Test
    void tradingAwayEverythingHeldLeavesZero() throws InterruptedException {
        materialCollectedSubscriber.onMaterialCollected(collected("imperialshielding", "Manufactured", 50, "Imperial Shielding"));

        subscriber.onMaterialTrade(trade(TRADE_IMPERIAL_SHIELDING));

        awaitAmount("imperialshielding", 0);
    }

    @Test
    void tradeCreditsTheMaterialReceived() throws InterruptedException {
        materialCollectedSubscriber.onMaterialCollected(collected("imperialshielding", "Manufactured", 50, "Imperial Shielding"));

        subscriber.onMaterialTrade(trade(TRADE_IMPERIAL_SHIELDING));

        awaitAmount("mechanicalcomponents", 75);
    }

    @Test
    void tradeDeductsOnlyWhatWasPaid() throws InterruptedException {
        materialCollectedSubscriber.onMaterialCollected(collected("imperialshielding", "Manufactured", 80, "Imperial Shielding"));

        subscriber.onMaterialTrade(trade(TRADE_IMPERIAL_SHIELDING));

        awaitAmount("imperialshielding", 30);
    }

    @Test
    void receivingAMaterialNeverHeldRegistersItRatherThanDroppingTheCount() throws InterruptedException {
        // Nothing has ever written a row for the received side in this run; collect() must register it.
        subscriber.onMaterialTrade(trade(TRADE_MILITARY_GRADE_ALLOYS));

        awaitAmount("uncutfocuscrystals", 108);
    }

    @Test
    void tradingMoreThanHeldFloorsAtZero() throws InterruptedException {
        // The game cannot produce this, but a trade replayed against a stale inventory can.
        materialCollectedSubscriber.onMaterialCollected(collected("imperialshielding", "Manufactured", 10, "Imperial Shielding"));

        subscriber.onMaterialTrade(trade(TRADE_IMPERIAL_SHIELDING));

        awaitAmount("imperialshielding", 0);
    }

    // Verbatim journal lines from Journal.2026-08-11T200621.01.log.
    private static final String TRADE_IMPERIAL_SHIELDING = """
            { "timestamp":"2026-08-12T03:23:38Z", "event":"MaterialTrade", "MarketID":3223896576, "TraderType":"manufactured",
              "Paid":{ "Material":"imperialshielding", "Material_Localised":"Imperial Shielding", "Category":"Manufactured", "Quantity":50 },
              "Received":{ "Material":"mechanicalcomponents", "Material_Localised":"Mechanical Components", "Category":"Manufactured", "Quantity":75 } }
            """;

    private static final String TRADE_MILITARY_GRADE_ALLOYS = """
            { "timestamp":"2026-08-12T03:23:06Z", "event":"MaterialTrade", "MarketID":3223896576, "TraderType":"manufactured",
              "Paid":{ "Material":"militarygradealloys", "Material_Localised":"Military Grade Alloys", "Category":"Manufactured", "Quantity":24 },
              "Received":{ "Material":"uncutfocuscrystals", "Material_Localised":"Flawed Focus Crystals", "Category":"Manufactured", "Quantity":108 } }
            """;

    private static JsonObject json(String journalLine) {
        return JsonParser.parseString(journalLine).getAsJsonObject();
    }

    private static MaterialTradeEvent trade(String journalLine) {
        return new MaterialTradeEvent(json(journalLine));
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
