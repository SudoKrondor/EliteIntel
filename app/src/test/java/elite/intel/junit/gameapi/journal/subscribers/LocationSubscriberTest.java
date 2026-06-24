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
