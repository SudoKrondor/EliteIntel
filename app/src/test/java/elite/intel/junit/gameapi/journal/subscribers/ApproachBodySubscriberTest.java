package elite.intel.junit.gameapi.journal.subscribers;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.google.gson.JsonObject;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.ApproachBodyEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.journal.events.dto.TargetLocation;
import elite.intel.gameapi.journal.subscribers.ApproachBodySubscriber;
import elite.intel.session.LocationData;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Instant;
import java.util.function.BooleanSupplier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class ApproachBodySubscriberTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private final ApproachBodySubscriber subscriber = new ApproachBodySubscriber();
    private final LocationManager locationManager = LocationManager.getInstance();

    private static final long SYSTEM_ADDRESS = 83852530386689L;
    private static final long BODY_ID = 5L;
    private static final String STAR_SYSTEM = "Deciat";
    private static final String BODY_NAME = "Deciat 5";

    @BeforeAll
    static void configureEdsmBaseUrl() {
        System.setProperty("edsm.base.url", "http://localhost:" + wm.getPort());
        System.setProperty("edsm.min.interval.ms", "0");
        // ApproachBodySubscriber calls playerSession.getTracking().isEnabled(); ensure non-null
        PlayerSession.getInstance().setTracking(new TargetLocation());
    }

    @AfterAll
    static void clearEdsmBaseUrl() {
        System.clearProperty("edsm.base.url");
        System.clearProperty("edsm.min.interval.ms");
    }

    @BeforeEach
    void sleep() throws InterruptedException {
        Thread.sleep(100);
    }

    @Test
    void approachBodyUsesEdsmGravityWhenNoOwnData() throws InterruptedException {
        wm.stubFor(get(urlPathEqualTo("/api-system-v1/bodies"))
                .willReturn(ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "name": "Deciat",
                                  "bodyCount": 1,
                                  "bodies": [{
                                    "bodyId": 5,
                                    "name": "Deciat 5",
                                    "type": "Planet",
                                    "distanceToArrival": 3200.5,
                                    "isLandable": true,
                                    "gravity": 0.42,
                                    "earthMasses": 0.28,
                                    "radius": 4750.0,
                                    "surfaceTemperature": 310.0,
                                    "atmosphereType": "No atmosphere"
                                  }]
                                }
                                """)));

        subscriber.onApproachBodyEvent(approachBodyEvent(STAR_SYSTEM, SYSTEM_ADDRESS, BODY_NAME, BODY_ID));

        awaitTrue(() -> {
            LocationDto loc = locationManager.findBySystemAddress(SYSTEM_ADDRESS, BODY_ID);
            return loc.getGravity() > 0;
        });

        LocationDto saved = locationManager.findBySystemAddress(SYSTEM_ADDRESS, BODY_ID);
        assertEquals(0.42, saved.getGravity(), 0.001);
    }

    @Test
    void approachBodyWithNoEdsmDataSavesBodyName() throws InterruptedException {
        wm.stubFor(get(urlPathEqualTo("/api-system-v1/bodies"))
                .willReturn(ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));

        subscriber.onApproachBodyEvent(approachBodyEvent(STAR_SYSTEM, SYSTEM_ADDRESS + 1, "Deciat 6", 6L));

        awaitTrue(() -> {
            LocationDto loc = locationManager.findBySystemAddress(SYSTEM_ADDRESS + 1, 6L);
            return "Deciat 6".equals(loc.getPlanetName());
        });

        LocationDto saved = locationManager.findBySystemAddress(SYSTEM_ADDRESS + 1, 6L);
        assertEquals("Deciat 6", saved.getPlanetName());
        assertEquals(0.0, saved.getGravity());
    }

    @Test
    void nullBodyIdDoesNotUpdateLocationId() throws InterruptedException {
        long uniqueSystem = 77777777L;
        long knownBodyId = 99L;
        PlayerSession.getInstance().setCurrentLocationId(knownBodyId, uniqueSystem);

        subscriber.onApproachBodyEvent(approachBodyEventNullBodyId(STAR_SYSTEM, uniqueSystem, "Deciat 9"));

        Thread.sleep(300); // virtual thread has no DB change to poll; just wait

        LocationData<Long, Long> loc = PlayerSession.getInstance().getLocationData();
        assertEquals(knownBodyId, loc.getInGameId(), "null BodyID must not overwrite current_location_id");
    }

    @Test
    void bodyIsSavedWhenTrackingIsEnabled() throws InterruptedException {
        long trackingSystem = 88888888L;
        long trackingBodyId = 12L;

        TargetLocation tracking = new TargetLocation(true);
        PlayerSession.getInstance().setTracking(tracking);
        try {
            subscriber.onApproachBodyEvent(approachBodyEvent("Tracking System", trackingSystem, "Tracking Body 12", trackingBodyId));

            awaitTrue(() -> {
                LocationDto loc = locationManager.findBySystemAddress(trackingSystem, trackingBodyId);
                return "Tracking Body 12".equals(loc.getPlanetName());
            });

            LocationDto saved = locationManager.findBySystemAddress(trackingSystem, trackingBodyId);
            assertEquals("Tracking Body 12", saved.getPlanetName());
            assertEquals("Tracking System", saved.getStarName());
        } finally {
            PlayerSession.getInstance().setTracking(new TargetLocation());
        }
    }

    private static ApproachBodyEvent approachBodyEvent(String starSystem, long systemAddress,
                                                       String body, long bodyId) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().plusSeconds(1).toString());
        j.addProperty("event", "ApproachBody");
        j.addProperty("StarSystem", starSystem);
        j.addProperty("SystemAddress", systemAddress);
        j.addProperty("Body", body);
        j.addProperty("BodyID", bodyId);
        return new ApproachBodyEvent(j);
    }

    private static ApproachBodyEvent approachBodyEventNullBodyId(String starSystem, long systemAddress, String body) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().plusSeconds(1).toString());
        j.addProperty("event", "ApproachBody");
        j.addProperty("StarSystem", starSystem);
        j.addProperty("SystemAddress", systemAddress);
        j.addProperty("Body", body);
        // BodyID intentionally omitted — Gson will deserialise it as null
        return new ApproachBodyEvent(j);
    }

    private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) fail("Condition not met within 2 seconds");
            Thread.sleep(10);
        }
    }
}
