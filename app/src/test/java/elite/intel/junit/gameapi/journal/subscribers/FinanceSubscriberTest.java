package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.gameapi.journal.events.CarrierBankTransferEvent;
import elite.intel.gameapi.journal.events.CarrierBuyEvent;
import elite.intel.gameapi.journal.events.LoadGameEvent;
import elite.intel.gameapi.journal.events.MarketBuyEvent;
import elite.intel.gameapi.journal.events.MarketSellEvent;
import elite.intel.gameapi.journal.events.MissionCompletedEvent;
import elite.intel.gameapi.journal.events.ModuleBuyEvent;
import elite.intel.gameapi.journal.events.RedeemVoucherEvent;
import elite.intel.gameapi.journal.events.ResurrectEvent;
import elite.intel.gameapi.journal.events.SellOrganicDataEvent;
import elite.intel.gameapi.journal.events.ShipyardBuyEvent;
import elite.intel.gameapi.journal.subscribers.FinanceSubscriber;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FinanceSubscriberTest {

    private final FinanceSubscriber subscriber = new FinanceSubscriber();
    private final PlayerSession session = PlayerSession.getInstance();

    @BeforeEach
    void resetBalance() {
        session.setPersonalCreditsAvailable(0L);
    }

    // --- Pure signed-delta math (no DB) ---

    @Test
    void inflowDeltasArePositive() {
        assertEquals(5_000L, FinanceSubscriber.delta(marketSell(5_000L)));
        assertEquals(7_500L, FinanceSubscriber.delta(missionCompleted(7_500L)));
        assertEquals(9_000L, FinanceSubscriber.delta(redeemVoucher(9_000L)));
    }

    @Test
    void outflowDeltasAreNegative() {
        assertEquals(-3_000L, FinanceSubscriber.delta(marketBuy(3_000L)));
        assertEquals(-4_875_000_000L, FinanceSubscriber.delta(carrierBuy(4_875_000_000L)));
    }

    @Test
    void moduleBuyIsNetOfTradeIn() {
        // bought for 8000, sold replaced module back for 2000 -> net -6000
        assertEquals(-6_000L, FinanceSubscriber.delta(moduleBuy(8_000L, 2_000L)));
        // no trade-in -> full cost
        assertEquals(-8_000L, FinanceSubscriber.delta(moduleBuy(8_000L, 0L)));
    }

    @Test
    void shipyardBuyIsNetOfTradeIn() {
        assertEquals(-(50_000_000L - 12_000_000L), FinanceSubscriber.delta(shipyardBuy(50_000_000L, 12_000_000L)));
    }

    @Test
    void resurrectChargesCostUnlessBankrupt() {
        assertEquals(-120_000L, FinanceSubscriber.delta(resurrect(120_000L, false)));
        assertEquals(0L, FinanceSubscriber.delta(resurrect(120_000L, true)));
    }

    @Test
    void carrierBankTransferDepositIsOutflowWithdrawIsInflow() {
        assertEquals(-80_000L, FinanceSubscriber.delta(carrierBankTransfer(80_000L, 0L)));
        assertEquals(80_000L, FinanceSubscriber.delta(carrierBankTransfer(0L, 80_000L)));
    }

    @Test
    void sellOrganicDataSumsValueAndBonusAcrossEntries() {
        SellOrganicDataEvent e = sellOrganicData(new long[][]{{10_000L, 1_000L}, {20_000L, 0L}});
        assertEquals(31_000L, FinanceSubscriber.delta(e));
    }

    // --- End to end through the subscriber against the in-memory DB ---

    @Test
    void loadGameSetsAbsoluteBalance() {
        session.setPersonalCreditsAvailable(42L);
        subscriber.onLoadGame(loadGame(1_500_000L));
        assertEquals(1_500_000L, session.getPersonalCredits());
    }

    @Test
    void inflowIncreasesBalance() {
        session.setPersonalCreditsAvailable(1_000L);
        subscriber.onMarketSell(marketSell(500L));
        assertEquals(1_500L, session.getPersonalCredits());
    }

    @Test
    void outflowDecreasesBalance() {
        session.setPersonalCreditsAvailable(1_000L);
        subscriber.onMarketBuy(marketBuy(400L));
        assertEquals(600L, session.getPersonalCredits());
    }

    @Test
    void bankruptResurrectDoesNotChangeBalance() {
        session.setPersonalCreditsAvailable(1_000L);
        subscriber.onResurrect(resurrect(999_999L, true));
        assertEquals(1_000L, session.getPersonalCredits());
    }

    @Test
    void nonBankruptResurrectDeductsRebuy() {
        session.setPersonalCreditsAvailable(1_000_000L);
        subscriber.onResurrect(resurrect(250_000L, false));
        assertEquals(750_000L, session.getPersonalCredits());
    }

    // --- event builders ---

    private static JsonObject base(String event) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", event);
        return j;
    }

    private static LoadGameEvent loadGame(long credits) {
        JsonObject j = base("LoadGame");
        j.addProperty("Commander", "CMDR Test");
        j.addProperty("Ship", "cobra");
        j.addProperty("Credits", credits);
        return new LoadGameEvent(j);
    }

    private static MarketSellEvent marketSell(long totalSale) {
        JsonObject j = base("MarketSell");
        j.addProperty("Type", "gold");
        j.addProperty("Count", 1);
        j.addProperty("SellPrice", totalSale);
        j.addProperty("TotalSale", totalSale);
        return new MarketSellEvent(j);
    }

    private static MarketBuyEvent marketBuy(long totalCost) {
        JsonObject j = base("MarketBuy");
        j.addProperty("Type", "gold");
        j.addProperty("Count", 1);
        j.addProperty("BuyPrice", totalCost);
        j.addProperty("TotalCost", totalCost);
        return new MarketBuyEvent(j);
    }

    private static MissionCompletedEvent missionCompleted(long reward) {
        JsonObject j = base("MissionCompleted");
        j.addProperty("Name", "Mission_Delivery");
        j.addProperty("Reward", reward);
        return new MissionCompletedEvent(j);
    }

    private static RedeemVoucherEvent redeemVoucher(long amount) {
        JsonObject j = base("RedeemVoucher");
        j.addProperty("Type", "bounty");
        j.addProperty("Amount", amount);
        return new RedeemVoucherEvent(j);
    }

    private static ModuleBuyEvent moduleBuy(long buyPrice, long sellPrice) {
        JsonObject j = base("ModuleBuy");
        j.addProperty("Slot", "MediumHardpoint1");
        j.addProperty("BuyItem", "$hpt_pulselaser_name;");
        j.addProperty("BuyPrice", buyPrice);
        if (sellPrice > 0) {
            j.addProperty("SellItem", "$hpt_beamlaser_name;");
            j.addProperty("SellPrice", sellPrice);
        }
        return new ModuleBuyEvent(j);
    }

    private static ShipyardBuyEvent shipyardBuy(long shipPrice, long sellPrice) {
        JsonObject j = base("ShipyardBuy");
        j.addProperty("ShipType", "python");
        j.addProperty("ShipPrice", shipPrice);
        if (sellPrice > 0) {
            j.addProperty("SellOldShip", "cobramkiii");
            j.addProperty("SellShipID", 3);
            j.addProperty("SellPrice", sellPrice);
        }
        return new ShipyardBuyEvent(j);
    }

    private static ResurrectEvent resurrect(long cost, boolean bankrupt) {
        JsonObject j = base("Resurrect");
        j.addProperty("Option", "rebuy");
        j.addProperty("Cost", cost);
        j.addProperty("Bankrupt", bankrupt);
        return new ResurrectEvent(j);
    }

    private static CarrierBuyEvent carrierBuy(long price) {
        JsonObject j = base("CarrierBuy");
        j.addProperty("CarrierID", 3_700_029_440L);
        j.addProperty("Price", price);
        j.addProperty("Callsign", "P07-V3L");
        return new CarrierBuyEvent(j);
    }

    private static CarrierBankTransferEvent carrierBankTransfer(long deposit, long withdraw) {
        JsonObject j = base("CarrierBankTransfer");
        j.addProperty("CarrierID", 3_700_005_632L);
        if (deposit > 0) j.addProperty("Deposit", deposit);
        if (withdraw > 0) j.addProperty("Withdraw", withdraw);
        j.addProperty("PlayerBalance", 1_000_000L);
        j.addProperty("CarrierBalance", 500_000L);
        return new CarrierBankTransferEvent(j);
    }

    private static SellOrganicDataEvent sellOrganicData(long[][] valueBonusPairs) {
        JsonObject j = base("SellOrganicData");
        j.addProperty("MarketID", 1L);
        JsonArray bioData = new JsonArray();
        for (long[] pair : valueBonusPairs) {
            JsonObject entry = new JsonObject();
            entry.addProperty("Genus", "$Codex_Ent_Bacterial_Genus_Name;");
            entry.addProperty("Species", "$Codex_Ent_Bacterial_01_Name;");
            entry.addProperty("Value", pair[0]);
            entry.addProperty("Bonus", pair[1]);
            bioData.add(entry);
        }
        j.add("BioData", bioData);
        return new SellOrganicDataEvent(j);
    }
}
