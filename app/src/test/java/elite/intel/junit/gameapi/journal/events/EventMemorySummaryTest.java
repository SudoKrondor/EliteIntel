package elite.intel.junit.gameapi.journal.events;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elite.intel.gameapi.journal.events.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit coverage for each event's {@code memorySummary()} (the readable line the EVENT "knowing" channel writes
 * to companion memory): the populated case and, where the event guards on missing data, the empty case.
 * Events are built from a journal {@link JsonObject} the same way the parser builds them.
 */
class EventMemorySummaryTest {

    private static final String TS = "2025-01-01T12:00:00Z";

    /** Builds a journal object from key/value pairs (no timestamp), inferring the JSON type from the value. */
    private static JsonObject obj(Object... kv) {
        JsonObject json = new JsonObject();
        for (int i = 0; i < kv.length; i += 2) {
            String key = (String) kv[i];
            Object value = kv[i + 1];
            if (value instanceof Number number) {
                json.addProperty(key, number);
            } else if (value instanceof Boolean bool) {
                json.addProperty(key, bool);
            } else if (value instanceof JsonElement element) {
                json.add(key, element);
            } else {
                json.addProperty(key, String.valueOf(value));
            }
        }
        return json;
    }

    /** A journal event object: like {@link #obj} but carrying the {@code timestamp} every constructor reads. */
    private static JsonObject ev(Object... kv) {
        JsonObject json = obj(kv);
        json.addProperty("timestamp", TS);
        return json;
    }

    private static JsonArray arr(JsonObject... items) {
        JsonArray array = new JsonArray();
        for (JsonObject item : items) {
            array.add(item);
        }
        return array;
    }

    @Test
    void docked() {
        assertEquals("docked at Jameson Memorial in Shinrarta Dezhra",
                new DockedEvent(ev("StationName", "Jameson Memorial", "StarSystem", "Shinrarta Dezhra")).memorySummary());
        assertEquals("docked at Jameson Memorial",
                new DockedEvent(ev("StationName", "Jameson Memorial")).memorySummary());
        assertEquals("", new DockedEvent(ev()).memorySummary());
    }

    @Test
    void location() {
        assertEquals("in Sol, docked at Galileo",
                new LocationEvent(ev("StarSystem", "Sol", "Docked", true, "StationName", "Galileo")).memorySummary());
        assertEquals("in Sol, near Earth",
                new LocationEvent(ev("StarSystem", "Sol", "Body", "Earth")).memorySummary());
        assertEquals("in Sol", new LocationEvent(ev("StarSystem", "Sol")).memorySummary());
        assertEquals("", new LocationEvent(ev()).memorySummary());
    }

    /// FIX ME. The CommanderEvent prefers String preferredName = PlayerSession.getInstance().getConfiguredPlayerName();
    /// Use of the journal commander name is not advised. This was a very specific fix for most commander names are not TTS friendly
//    @Test
//    void commander() {
//        assertEquals("our commander is Jameson", new CommanderEvent(ev("Name", "Jameson")).memorySummary());
//        assertEquals("", new CommanderEvent(ev()).memorySummary());
//    }

    @Test
    void loadGame() {
        assertEquals("started the session flying the Anaconda",
                new LoadGameEvent(ev("Ship", "anaconda", "Ship_Localised", "Anaconda")).memorySummary());
        assertEquals("started the session flying the anaconda",
                new LoadGameEvent(ev("Ship", "anaconda")).memorySummary());
        assertEquals("", new LoadGameEvent(ev()).memorySummary());
    }

    @Test
    void promotion() {
        assertEquals("promoted: Combat rank 4", new PromotionEvent(ev("Combat", 4)).memorySummary());
        assertEquals("promoted: Empire rank 7", new PromotionEvent(ev("Empire", 7)).memorySummary());
        assertEquals("", new PromotionEvent(ev()).memorySummary());
    }

    @Test
    void rank() {
        assertEquals("career ranks - combat 5, trade 3, exploration 7",
                new RankEvent(ev("Combat", 5, "Trade", 3, "Explore", 7)).memorySummary());
    }

    @Test
    void progress() {
        assertEquals("rank progress - combat 50%, trade 20%, exploration 80%",
                new ProgressEvent(ev("Combat", 50, "Trade", 20, "Explore", 80)).memorySummary());
    }

    @Test
    void reputation() {
        String summary = new ReputationEvent(ev("Federation", 75.0, "Empire", 50.0, "Alliance", 25.0)).memorySummary();
        assertTrue(summary.startsWith("reputation - federation "), summary);
        assertTrue(summary.contains(", empire "), summary);
        assertTrue(summary.contains(", alliance "), summary);
    }

    @Test
    void powerplay() {
        assertEquals("pledged to Aisling Duval, rank 3, 1500 merits",
                new PowerplayEvent(ev("Power", "Aisling Duval", "Rank", 3, "Merits", 1500)).memorySummary());
        assertEquals("", new PowerplayEvent(ev()).memorySummary());
    }

    @Test
    void engineerProgress() {
        assertEquals("engineer progress: 1 engineers on record",
                new EngineerProgressEvent(ev("Engineers", arr(obj("Engineer", "Felicity Farseer")))).memorySummary());
        assertEquals("", new EngineerProgressEvent(ev()).memorySummary());
    }

    @Test
    void engineerCraft() {
        assertEquals("engineered FSD_LongRange grade 5 at Felicity Farseer",
                new EngineerCraftEvent(ev("Engineer", "Felicity Farseer", "BlueprintName", "FSD_LongRange", "Level", 5)).memorySummary());
        assertEquals("", new EngineerCraftEvent(ev("Level", 5)).memorySummary());
    }

    @Test
    void carrierStats() {
        assertEquals("fleet carrier My Carrier (ABC-123), fuel 500",
                new CarrierStatsEvent(ev("Name", "My Carrier", "Callsign", "ABC-123", "FuelLevel", 500)).memorySummary());
    }

    @Test
    void sellOrganicData() {
        assertEquals("sold exobiology data from 2 species for 350 credits",
                new SellOrganicDataEvent(ev("BioData",
                        arr(obj("Value", 100, "Bonus", 50), obj("Value", 200, "Bonus", 0)))).memorySummary());
        assertEquals("", new SellOrganicDataEvent(ev()).memorySummary());
    }

    @Test
    void statistics() {
        assertEquals("net worth: 123456 credits",
                new StatisticsEvent(ev("Bank_Account", obj("Current_Wealth", 123456))).memorySummary());
        assertEquals("", new StatisticsEvent(ev()).memorySummary());
    }

    @Test
    void shipyardSell() {
        assertEquals("sold the stored federation_corvette for 50000000 credits",
                new ShipyardSellEvent(ev("ShipType", "federation_corvette", "ShipPrice", 50000000)).memorySummary());
    }

    @Test
    void shipyardSwap() {
        assertEquals("switched to the Python",
                new ShipyardSwapEvent(ev("ShipType", "python", "ShipType_Localised", "Python")).memorySummary());
        assertEquals("switched to the python", new ShipyardSwapEvent(ev("ShipType", "python")).memorySummary());
    }

    @Test
    void shipyardTransfer() {
        assertEquals("ordered transfer of the cobramkiii from Sol",
                new ShipyardTransferEvent(ev("ShipType", "cobramkiii", "System", "Sol")).memorySummary());
        assertEquals("ordered transfer of the cobramkiii",
                new ShipyardTransferEvent(ev("ShipType", "cobramkiii")).memorySummary());
    }

    @Test
    void moduleBuy() {
        assertEquals("bought the Power Plant for 100000 credits",
                new ModuleBuyEvent(ev("BuyItem", "int_powerplant", "BuyItem_Localised", "Power Plant", "BuyPrice", 100000)).memorySummary());
    }

    @Test
    void moduleSell() {
        assertEquals("sold the Shield Generator for 5000 credits",
                new ModuleSellEvent(ev("SellItem", "int_shield", "SellItem_Localised", "Shield Generator", "SellPrice", 5000)).memorySummary());
    }

    @Test
    void moduleSellRemote() {
        assertEquals("sold the stored Shield Generator for 5000 credits",
                new ModuleSellRemoteEvent(ev("SellItem", "int_shield", "SellItem_Localised", "Shield Generator", "SellPrice", 5000)).memorySummary());
    }

    @Test
    void missions() {
        assertEquals("mission log: 1 active, 0 completed, 0 failed",
                new MissionsEvent(ev("Active", arr(obj("MissionID", 1, "Name", "x")))).memorySummary());
        assertEquals("", new MissionsEvent(ev()).memorySummary());
    }

    @Test
    void materials() {
        assertEquals("materials on hand: 1 raw, 0 manufactured, 0 encoded",
                new MaterialsEvent(ev("Raw", arr(obj("Name", "iron", "Count", 5)))).memorySummary());
        assertEquals("", new MaterialsEvent(ev()).memorySummary());
    }

    @Test
    void missionFailed() {
        assertEquals("mission failed: Assassinate the pirate lord",
                new MissionFailedEvent(ev("Name", "Mission_Assassinate", "LocalisedName", "Assassinate the pirate lord")).memorySummary());
        assertEquals("", new MissionFailedEvent(ev()).memorySummary());
    }
}
