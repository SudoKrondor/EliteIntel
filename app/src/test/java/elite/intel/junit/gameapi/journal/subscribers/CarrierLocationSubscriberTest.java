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

    private static final String SPANSH_JOB_ID = "carrier-job-live-replot";

    // gzip off: the Spansh client asks for it and does not decompress. EDSM is happy either way.
    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort().gzipDisabled(true))
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
        carrier.setMeasuredFuelLevel(500);
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

    /**
     * An arrival somewhere the plotted route does not run from leaves every remaining leg unreachable,
     * so the route is re-plotted from where the carrier now is, towards the destination it was already
     * heading for.
     *
     * <p>The re-plot is driven with an explicit destination rather than through the commander-facing
     * {@code calculate()}, which reads the clipboard: an automatic repair must not replace whatever the
     * commander had copied. The clipboard itself is deliberately not asserted here, because reading the
     * system clipboard from a test depends on the desktop environment. What is asserted is that Spansh
     * is asked to plot from the arrival system, which is the whole reason the destination has to travel
     * as an argument.
     */
    @Test
    void anOffRouteArrivalReplotsFromWhereTheCarrierNowIs() throws InterruptedException {
        System.setProperty("spansh.base.url", wm.baseUrl());
        stubSpanshRoute();

        FleetCarrierRouteManager route = FleetCarrierRouteManager.getInstance();
        route.clear();
        session.setLastKnownCarrierLocation("Sol");
        route.setFleetCarrierRoute(Map.of(1, leg("Deciat", 120), 2, leg("Colonia", 95)));

        subscriber.onCarrierLocationEvent(
                carrierLocationEvent("Eephaik CX-V b31-9", "FleetCarrier", 3712500736L, 20299220533521L));

        awaitTrue(() -> !wm.findAll(postRequestedFor(urlEqualTo("/api/fleetcarrier/route"))).isEmpty());

        wm.verify(postRequestedFor(urlEqualTo("/api/fleetcarrier/route"))
                .withRequestBody(containing("source=Eephaik+CX-V+b31-9"))
                .withRequestBody(containing("destinations=Colonia")));

        awaitTrue(() -> "Blua Eaec WW-E d11-32".equals(routeLegName(route, 1)));
        assertEquals("Blua Eaec WW-E d11-32", routeLegName(route, 1),
                "leg 1 must be reachable from where the carrier actually is");
        route.clear();
        System.clearProperty("spansh.base.url");
    }

    private static String routeLegName(FleetCarrierRouteManager route, int legNumber) {
        CarrierJump leg = route.getFleetCarrierRoute().get(legNumber);
        return leg == null ? null : leg.getSystemName();
    }

    private void stubSpanshRoute() {
        wm.stubFor(post(urlEqualTo("/api/fleetcarrier/route"))
                .willReturn(aResponse().withStatus(202).withBody("{\"job\":\"" + SPANSH_JOB_ID + "\"}")));
        wm.stubFor(get(urlEqualTo("/api/results/" + SPANSH_JOB_ID))
                .willReturn(okJson("""
                        {"result": {"jumps": [
                          {"name": "Eephaik CX-V b31-9", "fuel_used": 0, "distance": 0.0,
                           "has_icy_ring": false, "is_system_pristine": false, "x": -12135.0, "y": -895.0, "z": 17545.0},
                          {"name": "Blua Eaec WW-E d11-32", "fuel_used": 102, "distance": 498.2,
                           "has_icy_ring": true, "is_system_pristine": false, "x": -11700.0, "y": -890.0, "z": 17800.0},
                          {"name": "Colonia", "fuel_used": 97, "distance": 474.0,
                           "has_icy_ring": true, "is_system_pristine": true, "x": -9530.5, "y": -910.28125, "z": 19808.125}
                        ]}}
                        """)));
    }

    /**
     * A carrier arrival the commander was not aboard for produces CarrierLocation and nothing else:
     * no CarrierJump, so no StarPos. Out in uncharted space EDSM has never heard of the system and
     * the location table has no row for it either, because nobody has flown there. The coordinates
     * already on file belong to the system the carrier left, and keeping them made the distance query
     * answer confidently for the wrong place - 421 ly for a carrier that was 83 ly away. The
     * SystemAddress is the one source left, and it is always there.
     */
    @Test
    void anArrivalInUnchartedSpaceTakesItsPositionFromTheSystemAddress() throws InterruptedException {
        FleetCarrierRouteManager.getInstance().clear();
        wm.stubFor(get(urlPathEqualTo("/api-v1/systems"))
                .willReturn(ok().withHeader("Content-Type", "application/json").withBody("[]")));

        session.setLastKnownCarrierLocation("Eephaik LY-R b47-6");
        CarrierDataDto carrier = new CarrierDataDto();
        carrier.setStarName("Eephaik LY-R b47-6");
        carrier.setX(-11758.34375);
        carrier.setY(-894.53125);
        carrier.setZ(17880.5);
        session.setFleetCarrierData(carrier);

        subscriber.onCarrierLocationEvent(
                carrierLocationEvent("Eephaik CX-V b31-9", "FleetCarrier", 3712500736L, 20299220533521L));

        awaitTrue(() -> "Eephaik CX-V b31-9".equals(session.getFleetCarrierData().getStarName())
                && session.getFleetCarrierData().getX() != -11758.34375);

        CarrierDataDto stored = session.getFleetCarrierData();
        assertEquals(-12135.0, stored.getX(), 0.001, "x from the arrival system's boxel");
        assertEquals(-895.0, stored.getY(), 0.001, "y from the arrival system's boxel");
        assertEquals(17545.0, stored.getZ(), 0.001, "z from the arrival system's boxel");
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
        return carrierLocationEvent(system, carrierType, carrierId, 83852530386689L);
    }

    private static CarrierLocationEvent carrierLocationEvent(String system, String carrierType, long carrierId,
                                                             long systemAddress) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().plusSeconds(1).toString());
        j.addProperty("event", "CarrierLocation");
        j.addProperty("CarrierType", carrierType);
        j.addProperty("CarrierID", carrierId);
        j.addProperty("StarSystem", system);
        j.addProperty("SystemAddress", systemAddress);
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
