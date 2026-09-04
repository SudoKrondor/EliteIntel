package elite.intel.junit.gameapi.journal.subscribers;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.LocationEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.journal.subscribers.LocationSubscriber;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Instant;
import java.util.function.BooleanSupplier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

class LocationSubscriberTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private final LocationSubscriber subscriber = new LocationSubscriber();
    private final LocationManager locationManager = LocationManager.getInstance();

    private static final long SOL_ADDRESS = 10477373803L;
    private static final long DECIAT_ADDRESS = 83852530386689L;

    @BeforeAll
    static void configureEdsmBaseUrl() {
        System.setProperty("edsm.base.url", "http://localhost:" + wm.getPort());
        System.setProperty("edsm.min.interval.ms", "0");
    }

    @AfterAll
    static void clearEdsmBaseUrl() {
        System.clearProperty("edsm.base.url");
        System.clearProperty("edsm.min.interval.ms");
    }

    @BeforeEach
    void stubEdsmEndpoints() throws InterruptedException {
        Thread.sleep(100);
        wm.stubFor(get(urlPathEqualTo("/api-system-v1/traffic"))
                .willReturn(ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"traffic":{"total":5000,"week":200,"day":30}}
                                """)));
        wm.stubFor(get(urlPathEqualTo("/api-system-v1/deaths"))
                .willReturn(ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"deaths":{"total":10,"week":2,"day":0}}
                                """)));
    }

    @Test
    void locationEventSavesStarNameAndAllegiance() throws InterruptedException {
        subscriber.onLocationEvent(locationEvent("Sol", SOL_ADDRESS, "Sol", "StarSystem", "Federation"));

        // Wait specifically for "Federation" so that a pre-existing Sol entry (from JumpCompleted
        // tests, allegiance "Independent") doesn't cause the condition to exit prematurely.
        awaitTrue(() -> {
            LocationDto loc = locationManager.findBySystemAddress(SOL_ADDRESS, "Sol");
            return "Sol".equals(loc.getStarName()) && "Federation".equals(loc.getAllegiance());
        });

        LocationDto saved = locationManager.findBySystemAddress(SOL_ADDRESS, "Sol");
        assertEquals("Sol", saved.getStarName());
        assertEquals("Federation", saved.getAllegiance());
    }

    @Test
    void locationEventStoresTrafficDataFromEdsm() throws InterruptedException {
        subscriber.onLocationEvent(locationEvent("Deciat", DECIAT_ADDRESS, "Deciat", "StarSystem", "Independent"));

        awaitTrue(() -> {
            LocationDto loc = locationManager.findBySystemAddress(DECIAT_ADDRESS, "Deciat");
            return "Deciat".equals(loc.getStarName()) && loc.getTrafficDto() != null;
        });

        LocationDto saved = locationManager.findBySystemAddress(DECIAT_ADDRESS, "Deciat");
        assertNotNull(saved.getTrafficDto());
        assertEquals(5000, saved.getTrafficDto().getData().getTraffic().getTotal());
        assertEquals(10, saved.getDeathsDto().getData().getDeaths().getTotal());
    }

    /**
     * A commander who quits on a carrier's pad and comes back gets a {@code Location} event naming the BODY the
     * carrier is parked at, with the carrier in the station fields. Writing those onto the body typed a moon as
     * a FLEET_CARRIER - and it was narrated as one on the next approach.
     */
    @Test
    void aCarrierParkedAtTheBodyDoesNotRelabelTheBody() throws InterruptedException {
        long sysAddr = 2283077046962L;
        String star = "Hyades Sector MH-V c2-8";
        String moon = star + " 7 a";
        long marketId = 3712500736L;

        LocationDto body = new LocationDto(25L, sysAddr);
        body.setStarName(star);
        body.setPlanetName(moon);
        body.setLocationType(LocationDto.LocationType.MOON);
        locationManager.save(body);

        subscriber.onLocationEvent(dockedAtCarrier(star, sysAddr, moon, "GHY-L8X", marketId));

        awaitTrue(() -> locationManager.findByMarketId(marketId).getStationName() != null);

        LocationDto stillTheMoon = locationManager.findBySystemAddress(sysAddr, moon);
        assertEquals(LocationDto.LocationType.MOON, stillTheMoon.getLocationType());
        assertNull(stillTheMoon.getStationName());

        LocationDto carrier = locationManager.findByMarketId(marketId);
        assertEquals(LocationDto.LocationType.FLEET_CARRIER, carrier.getLocationType());
        assertEquals("GHY-L8X", carrier.getStationName());
    }

    /**
     * Shaped after a verbatim docked Location line: the Body is the moon, the station is the carrier on it.
     */
    private static LocationEvent dockedAtCarrier(String starSystem, long systemAddress, String body,
                                                 String stationName, long marketId) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().plusSeconds(1).toString());
        j.addProperty("event", "Location");
        j.addProperty("Docked", true);
        j.addProperty("StarSystem", starSystem);
        j.addProperty("SystemAddress", systemAddress);
        j.addProperty("Body", body);
        j.addProperty("BodyID", 25L);
        j.addProperty("BodyType", "Planet");
        j.addProperty("DistFromStarLS", 11241.5);
        j.addProperty("SystemAllegiance", "Independent");
        j.addProperty("SystemSecurity", "Low");
        j.addProperty("Population", 0L);
        j.addProperty("StationName", stationName);
        j.addProperty("StationType", "FleetCarrier");
        j.addProperty("MarketID", marketId);
        j.addProperty("StationEconomy", "$economy_Carrier;");
        j.addProperty("StationEconomy_Localised", "Private Enterprise");
        JsonObject faction = new JsonObject();
        faction.addProperty("Name", "FleetCarrier");
        j.add("StationFaction", faction);
        JsonArray services = new JsonArray();
        services.add("dock");
        services.add("refuel");
        j.add("StationServices", services);
        JsonArray starPos = new JsonArray();
        starPos.add(119.90625);
        starPos.add(-87.6875);
        starPos.add(-184.625);
        j.add("StarPos", starPos);
        return new LocationEvent(j);
    }

    private static LocationEvent locationEvent(String starSystem, long systemAddress, String body,
                                               String bodyType, String allegiance) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().plusSeconds(1).toString());
        j.addProperty("event", "Location");
        j.addProperty("StarSystem", starSystem);
        j.addProperty("SystemAddress", systemAddress);
        j.addProperty("Body", body);
        j.addProperty("BodyID", 0L);
        j.addProperty("BodyType", bodyType);
        j.addProperty("DistFromStarLS", 0.0);
        j.addProperty("SystemAllegiance", allegiance);
        j.addProperty("SystemSecurity", "High");
        j.addProperty("SystemSecurity_Localised", "High Security");
        j.addProperty("Population", 10_000_000L);
        JsonArray starPos = new JsonArray();
        starPos.add(0.0);
        starPos.add(0.0);
        starPos.add(0.0);
        j.add("StarPos", starPos);
        return new LocationEvent(j);
    }

    private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) fail("Condition not met within 2 seconds");
            Thread.sleep(10);
        }
    }
}
