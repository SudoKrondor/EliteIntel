package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.ApproachSettlementEvent;
import elite.intel.gameapi.journal.events.DockedEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.journal.subscribers.DockedStationRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A settlement is a place, not a property of the moon it stands on. Its market, services and faction used to
 * be written onto the body the ship dropped at - the same record a landing and a scan write to - which is how
 * a moon came to carry a market.
 */
class DockedStationRecordTest {

    private static final AtomicInteger RUN = new AtomicInteger();

    private final LocationManager locations = LocationManager.getInstance();

    @Test
    void aSettlementIsFiledUnderItsOwnName() {
        int run = RUN.incrementAndGet();
        long sysAddr = 3_618_249_902_451L + run;
        long marketId = 128_680_583L + run;
        long bodyId = 7L;
        String star = "Settlement Test " + run;
        String moon = star + " 1 a";
        String settlement = "Trophy Camp " + run;

        LocationDto body = new LocationDto(bodyId, sysAddr);
        body.setStarName(star);
        body.setPlanetName(moon);
        body.setLocationType(LocationDto.LocationType.MOON);
        locations.save(body);

        DockedStationRecord.of(approachSettlement(settlement, marketId, sysAddr, bodyId, "Alliance"), star).store();

        LocationDto stored = locations.findByMarketId(marketId);
        assertEquals(settlement, stored.getStationName());
        assertEquals(star, stored.getStarName());
        assertEquals(LocationDto.LocationType.STATION, stored.getLocationType());
        assertEquals("Alliance", stored.getStationAllegiance());
        assertEquals(marketId, stored.getBodyId(), "a settlement has no BodyID of its own to be filed under");

        LocationDto stillTheMoon = locations.findBySystemAddress(sysAddr, moon);
        assertEquals(LocationDto.LocationType.MOON, stillTheMoon.getLocationType());
        assertNull(stillTheMoon.getStationName());
        assertEquals(0L, stillTheMoon.getMarketID(), "the settlement's market does not belong to the moon");
    }

    @Test
    void anEventThatNamesNoAllegianceDoesNotEraseTheOneWeHave() {
        int run = RUN.incrementAndGet();
        long sysAddr = 3_618_249_902_451L + run;
        long marketId = 128_680_583L + run;
        String star = "Settlement Test " + run;
        String settlement = "Trophy Camp " + run;

        // Two thirds of these events leave StationAllegiance out rather than reporting it empty.
        DockedStationRecord.of(approachSettlement(settlement, marketId, sysAddr, 7L, "Alliance"), star).store();
        DockedStationRecord.of(dockedAt(settlement, marketId, sysAddr, star)).store();

        LocationDto stored = locations.findByMarketId(marketId);
        assertEquals("Alliance", stored.getStationAllegiance());
        assertEquals("CraterOutpost", stored.getStationType(), "the docking is what learns the station's type");
    }

    private static ApproachSettlementEvent approachSettlement(String name, long marketId, long systemAddress,
                                                              long bodyId, String allegiance) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "ApproachSettlement");
        j.addProperty("Name", name);
        j.addProperty("MarketID", marketId);
        j.addProperty("SystemAddress", systemAddress);
        j.addProperty("BodyID", bodyId);
        j.addProperty("BodyName", "body");
        j.addProperty("Latitude", 1.0);
        j.addProperty("Longitude", 2.0);
        if (allegiance != null) j.addProperty("StationAllegiance", allegiance);
        j.addProperty("StationGovernment", "$government_Engineer;");
        j.addProperty("StationGovernment_Localised", "Workshop");
        j.addProperty("StationEconomy", "$economy_Colony;");
        j.addProperty("StationEconomy_Localised", "Colony");
        j.add("StationFaction", JsonParser.parseString("{\"Name\":\"Tod 'The Blaster' McQuinn\"}"));
        JsonArray services = new JsonArray();
        services.add("dock");
        services.add("refuel");
        j.add("StationServices", services);
        return new ApproachSettlementEvent(j);
    }

    /**
     * The docking that follows: it carries the station type, and never a StationAllegiance.
     */
    private static DockedEvent dockedAt(String name, long marketId, long systemAddress, String starSystem) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "Docked");
        j.addProperty("StationName", name);
        j.addProperty("StationType", "CraterOutpost");
        j.addProperty("StarSystem", starSystem);
        j.addProperty("SystemAddress", systemAddress);
        j.addProperty("MarketID", marketId);
        j.addProperty("StationGovernment", "$government_Engineer;");
        j.addProperty("StationGovernment_Localised", "Workshop");
        j.addProperty("StationEconomy", "$economy_Colony;");
        j.addProperty("StationEconomy_Localised", "Colony");
        j.add("StationFaction", JsonParser.parseString("{\"Name\":\"Tod 'The Blaster' McQuinn\"}"));
        JsonArray services = new JsonArray();
        services.add("dock");
        j.add("StationServices", services);
        j.addProperty("DistFromStarLS", 100.0);
        return new DockedEvent(j);
    }
}
