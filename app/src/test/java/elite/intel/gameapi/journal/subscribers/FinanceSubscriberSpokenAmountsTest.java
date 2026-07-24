package elite.intel.gameapi.journal.subscribers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.vega.SpokenAmounts;
import elite.intel.gameapi.journal.events.*;
import elite.intel.util.yaml.YamlFactory;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The finance announcements are spoken by the companion, so every credit figure they voice must reach the LLM
 * already rounded and spelled out. Each announcement pairs an event's serialized YAML with a {@code ...Spoken}
 * sibling per amount; if the field name passed for a sibling does not match a field the payload actually
 * carries, the model gets a spoken value it cannot tie back to its number. These tests build each event and
 * assert, on the exact payload the subscriber sends, that the numeric field and its spoken sibling are present
 * and consistent.
 */
class FinanceSubscriberSpokenAmountsTest {

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parse(String yaml) throws Exception {
        return YamlFactory.getMapper().readValue(yaml, Map.class);
    }

    /**
     * Asserts the payload speaks {@code field}: the numeric value round-trips and its sibling is the spoken form.
     */
    private static void assertSpeaks(Map<String, Object> payload, String field, long expected) {
        assertEquals(expected, ((Number) payload.get(field)).longValue(),
                field + " numeric value must survive so the exact figure stays answerable");
        assertEquals(SpokenAmounts.forLlm(expected), payload.get(field + "Spoken"),
                field + "Spoken must be the rounded, spelled-out form of " + field);
    }

    @Test
    void everyAnnouncementInstructionCarriesTheSpokenAmountRule() {
        assertTrue(FinanceSubscriber.withSpokenAmountRule("Say something.").endsWith(SpokenAmounts.RULE),
                "the spoken-amount rule must ride along with every finance announcement");
    }

    @Test
    void voucherSpeaksTheAmount() throws Exception {
        assertSpeaks(parse(FinanceSubscriber.voucherPayload(voucher(1_023_309_245L))),
                "amount", 1_023_309_245L);
    }

    @Test
    void organicSaleSpeaksTotalAndBonus() throws Exception {
        // One sample worth 4,980,000 with no first-discovery bonus.
        Map<String, Object> payload = parse(FinanceSubscriber.organicSalePayload(organicSale(4_980_000L, 0L)));
        assertSpeaks(payload, "totalCredits", 4_980_000L);
        assertSpeaks(payload, "totalBonus", 0L);
    }

    @Test
    void explorationSaleSpeaksTotalAndBonus() throws Exception {
        Map<String, Object> payload = parse(
                FinanceSubscriber.explorationSalePayload(explorationSale(12_449_000L, 1_000_000L)));
        assertSpeaks(payload, "totalEarnings", 12_449_000L);
        assertSpeaks(payload, "bonus", 1_000_000L);
    }

    @Test
    void rebuySpeaksTheCost() throws Exception {
        assertSpeaks(parse(FinanceSubscriber.rebuyPayload(rebuy(8_450L))), "cost", 8_450L);
    }

    @Test
    void shipyardBuySpeaksTheNetCostEvenThoughItIsComputed() throws Exception {
        // netCost is not a field of the event; it is shipPrice minus the trade-in and must still be spoken.
        Map<String, Object> payload = parse(FinanceSubscriber.shipyardBuyPayload(shipyardBuy(2_000_000_000L, 0L)));
        assertSpeaks(payload, "netCost", 2_000_000_000L);
    }

    @Test
    void carrierBuySpeaksThePrice() throws Exception {
        assertSpeaks(parse(FinanceSubscriber.carrierBuyPayload(carrierBuy(1_000_000_000L))), "price", 1_000_000_000L);
    }

    // --- Event builders from minimal journal JSON ---

    private static JsonObject base(String event) {
        JsonObject json = new JsonObject();
        json.addProperty("timestamp", Instant.now().toString());
        json.addProperty("event", event);
        return json;
    }

    private static RedeemVoucherEvent voucher(long amount) {
        JsonObject json = base("RedeemVoucher");
        json.addProperty("Type", "bounty");
        json.addProperty("Amount", amount);
        return new RedeemVoucherEvent(json);
    }

    private static SellOrganicDataEvent organicSale(long value, long bonus) {
        JsonObject row = new JsonObject();
        row.addProperty("Genus", "Osseus");
        row.addProperty("Species", "Osseus Spiralis");
        row.addProperty("Value", value);
        row.addProperty("Bonus", bonus);
        JsonArray bioData = new JsonArray();
        bioData.add(row);
        JsonObject json = base("SellOrganicData");
        json.add("BioData", bioData);
        return new SellOrganicDataEvent(json);
    }

    private static MultiSellExplorationDataEvent explorationSale(long totalEarnings, long bonus) {
        JsonObject json = base("MultiSellExplorationData");
        json.addProperty("BaseValue", totalEarnings - bonus);
        json.addProperty("Bonus", bonus);
        json.addProperty("TotalEarnings", totalEarnings);
        return new MultiSellExplorationDataEvent(json);
    }

    private static ResurrectEvent rebuy(long cost) {
        JsonObject json = base("Resurrect");
        json.addProperty("Option", "rebuy");
        json.addProperty("Cost", cost);
        json.addProperty("Bankrupt", false);
        return new ResurrectEvent(json);
    }

    private static ShipyardBuyEvent shipyardBuy(long shipPrice, long sellPrice) {
        JsonObject json = base("ShipyardBuy");
        json.addProperty("ShipType", "federation_corvette");
        json.addProperty("ShipPrice", shipPrice);
        json.addProperty("SellPrice", sellPrice);
        return new ShipyardBuyEvent(json);
    }

    private static CarrierBuyEvent carrierBuy(long price) {
        JsonObject json = base("CarrierBuy");
        json.addProperty("Price", price);
        json.addProperty("Callsign", "X7K-99B");
        return new CarrierBuyEvent(json);
    }
}
