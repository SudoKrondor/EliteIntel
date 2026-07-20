package elite.intel.junit.gameapi.journal.subscribers;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.google.gson.JsonObject;
import elite.intel.db.managers.FleetCarrierRouteManager;
import elite.intel.gameapi.journal.events.CarrierLocationEvent;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.gameapi.journal.subscribers.CarrierLocationSubscriber;
import elite.intel.gameapi.search.spansh.carrierroute.CarrierJump;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Instant;
import java.util.Map;
import java.util.function.BooleanSupplier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

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

    /**
     * CarrierLocation fires for every arrival and always ahead of CarrierJump, so it owns the fuel
     * decrement. It has to read the completed leg before removing it from the route.
     */
    @Test
    void fleetCarrierArrivalBurnsTheLegsFuelAndClearsTheLeg() throws InterruptedException {
        FleetCarrierRouteManager route = FleetCarrierRouteManager.getInstance();
        route.clear();
        // The route is plotted from where the carrier is now; Deciat is the leg it is about to fly.
        session.setLastKnownCarrierLocation("Sol");
        route.setFleetCarrierRoute(Map.of(1, leg("Deciat", 120), 2, leg("Shinrarta Dezhra", 95)));

        CarrierDataDto carrier = new CarrierDataDto();
        carrier.setFuelLevel(500);
        session.setFleetCarrierData(carrier);

        subscriber.onCarrierLocationEvent(carrierLocationEvent("Deciat", "FleetCarrier", 3803463824L));

        awaitTrue(() -> session.getFleetCarrierData().getFuelLevel() == 380);

        assertEquals(380, session.getFleetCarrierData().getFuelLevel(), "the arrival leg's tritium must be burned");
        assertNull(route.findByPrimaryStar("Deciat"), "the system we are sitting in is never part of the route");
        route.clear();
    }

    @Test
    void squadronCarrierDoesNotUpdateLastKnownCarrierLocation() throws InterruptedException {
        String priorLocation = session.getLastKnownCarrierLocation();

        subscriber.onCarrierLocationEvent(carrierLocationEvent("Sol", "SquadronCarrier", 1111111111));

        Thread.sleep(300);

        // Last known carrier location should be unchanged (squadrons don't update it)
        assertEquals(priorLocation, session.getLastKnownCarrierLocation());
    }

    private static CarrierJump leg(String systemName, int fuelUsed) {
        CarrierJump jump = new CarrierJump();
        jump.setSystemName(systemName);
        jump.setFuelUsed(fuelUsed);
        jump.setX(1.0);
        jump.setY(2.0);
        jump.setZ(3.0);
        return jump;
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
