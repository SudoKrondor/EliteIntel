package elite.intel.junit.gameapi.journal.subscribers;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.google.gson.JsonObject;
import elite.intel.gameapi.journal.events.CarrierLocationEvent;
import elite.intel.gameapi.journal.subscribers.CarrierLocationSubscriber;
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

class CarrierLocationSubscriberTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private final CarrierLocationSubscriber subscriber = new CarrierLocationSubscriber();
    private final PlayerSession session = PlayerSession.getInstance();

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
        wm.stubFor(get(urlPathEqualTo("/api-v1/systems"))
                .willReturn(ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"name":"Deciat","information":{"security":"High"}}
                                """)));
    }

    @Test
    void fleetCarrierLocationSetsLastKnownCarrierSystem() throws InterruptedException {
        subscriber.onCarrierLocationEvent(carrierLocationEvent("Deciat", "FleetCarrier", 3803463824L));

        awaitTrue(() -> "Deciat".equals(session.getLastKnownCarrierLocation()));

        assertEquals("Deciat", session.getLastKnownCarrierLocation());
    }

    @Test
    void squadronCarrierDoesNotUpdateLastKnownCarrierLocation() throws InterruptedException {
        String priorLocation = session.getLastKnownCarrierLocation();

        subscriber.onCarrierLocationEvent(carrierLocationEvent("Sol", "SquadronCarrier", 1111111111));

        Thread.sleep(300);

        // Last known carrier location should be unchanged (squadrons don't update it)
        assertEquals(priorLocation, session.getLastKnownCarrierLocation());
    }

    private static CarrierLocationEvent carrierLocationEvent(String system, String carrierType, long carrierId) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().plusSeconds(1).toString());
        j.addProperty("event", "CarrierLocation");
        j.addProperty("CarrierType", carrierType);
        j.addProperty("CarrierID", carrierId);
        j.addProperty("StarSystem", system);
        j.addProperty("SystemAddress", 83852530386689L);
        j.addProperty("BodyID", 0);
        return new CarrierLocationEvent(j);
    }

    private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) fail("Condition not met within 2 seconds");
            Thread.sleep(10);
        }
    }
}
