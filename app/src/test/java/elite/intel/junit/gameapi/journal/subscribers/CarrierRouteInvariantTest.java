package elite.intel.junit.gameapi.journal.subscribers;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.google.common.eventbus.EventBus;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.db.managers.FleetCarrierRouteManager;
import elite.intel.gameapi.journal.events.CarrierJumpEvent;
import elite.intel.gameapi.journal.events.CarrierLocationEvent;
import elite.intel.gameapi.journal.events.DockedEvent;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.gameapi.journal.subscribers.CarrierJumpCompleteSubscriber;
import elite.intel.gameapi.journal.subscribers.CarrierLocationSubscriber;
import elite.intel.gameapi.journal.subscribers.DockedSubscriber;
import elite.intel.gameapi.journal.subscribers.SilentPersistenceSubscriber;
import elite.intel.gameapi.search.spansh.carrierroute.CarrierJump;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The plotted carrier route never contains the system the carrier is sitting in, nor any system
 * behind it. Route A B C D E with the carrier at C is the route D E.
 *
 * <p>The invariant answers to the carrier's position and to nothing else. These tests therefore walk
 * the ways the game can tell us the carrier moved — CarrierLocation alone (the commander was
 * elsewhere), CarrierJump alone (an old journal), a start-up replay of either — and the ways the
 * commander can move without his carrier: docked, on foot, in a ship, on another commander's deck.
 * None of the latter may shift the route by a single leg.
 */
class CarrierRouteInvariantTest {

    /**
     * EDSM must never be reached for real, and its call count is evidence of the branch taken.
     */
    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private static final String OUR_CALLSIGN = "GHY-L8X";

    /**
     * A B C D E, plotted while the carrier sat at the origin.
     */
    private static final List<String> PLOT = List.of(
            "Phylurn AZ-C b14-0",   // A
            "Phyluwyg OA-Y c1-5",   // B
            "Sifeae RK-E b54-1",    // C
            "Pro Eurl FV-M c21-1",  // D
            "Pro Eurl EG-V c16-4"); // E

    private final PlayerSession session = PlayerSession.getInstance();
    private final FleetCarrierRouteManager route = FleetCarrierRouteManager.getInstance();

    private final CarrierLocationSubscriber carrierLocationSubscriber = new CarrierLocationSubscriber();
    private final CarrierJumpCompleteSubscriber carrierJumpSubscriber = new CarrierJumpCompleteSubscriber();
    private final DockedSubscriber dockedSubscriber = new DockedSubscriber();

    @BeforeAll
    static void pointEdsmAtTheStub() {
        System.setProperty("edsm.base.url", "http://localhost:" + wm.getPort());
        System.setProperty("edsm.min.interval.ms", "0");
    }

    @AfterAll
    static void releaseEdsm() {
        System.clearProperty("edsm.base.url");
        System.clearProperty("edsm.min.interval.ms");
    }

    @BeforeEach
    void plotRouteFromTheOrigin() {
        wm.stubFor(get(urlPathEqualTo("/api-v1/systems"))
                .willReturn(ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"name\":\"Sifeae RK-E b54-1\",\"coords\":{\"x\":1,\"y\":2,\"z\":3}}")));
        route.clear();
        session.setLastKnownCarrierLocation("Praea Euq BS-M c7-0"); // the origin: not on the plot
        session.setFleetCarrierData(carrierWithFuel(1000));
        Map<Integer, CarrierJump> plotted = new LinkedHashMap<>();
        for (int i = 0; i < PLOT.size(); i++) {
            plotted.put(i + 1, leg(PLOT.get(i)));
        }
        route.setFleetCarrierRoute(plotted);
        assertEquals(PLOT, remainingSystems(), "precondition: the whole plot is still ahead of us");
    }

    /**
     * Case 1: the app was not running when the carrier arrived. The start-up pre-scan replays the
     * journal, and the route must already be trimmed by the time anything reads it.
     */
    @Test
    void case1_appStartedAfterTheJumpCompleted() {
        EventBus preScanBus = new EventBus("pre-scan-test");
        preScanBus.register(new SilentPersistenceSubscriber());

        preScanBus.post(carrierLocation("Sifeae RK-E b54-1")); // carrier reached C while we were down

        assertEquals(List.of("Pro Eurl FV-M c21-1", "Pro Eurl EG-V c16-4"), remainingSystems());
    }

    /**
     * Cases 2, 5, 6, 7 and 8: the commander is anywhere but his carrier — in a ship, in an SRV, on
     * foot on a planet, docked at a station. The game writes CarrierLocation and no CarrierJump.
     */
    @Test
    void cases2to8_carrierJumpsWhileTheCommanderIsElsewhere() throws InterruptedException {
        carrierLocationSubscriber.onCarrierLocationEvent(carrierLocation("Sifeae RK-E b54-1"));

        awaitTrue(() -> remainingSystems().size() == 2);
        assertEquals(List.of("Pro Eurl FV-M c21-1", "Pro Eurl EG-V c16-4"), remainingSystems());
    }

    /**
     * Cases 3 and 4: the commander rode along, in his ship or on foot on the deck. Journals that
     * predate CarrierLocation carry only CarrierJump, so that event alone has to hold the invariant.
     */
    @Test
    void cases3and4_commanderAboardAndOnlyCarrierJumpIsWritten() throws InterruptedException {
        carrierJumpSubscriber.onCarrierJumpCompleteEvent(carrierJump("Sifeae RK-E b54-1", true, false));
        awaitTrue(() -> remainingSystems().size() == 2);
        assertEquals(List.of("Pro Eurl FV-M c21-1", "Pro Eurl EG-V c16-4"), remainingSystems(),
                "docked aboard: the carrier's system is behind us");
        assertEquals("Sifeae RK-E b54-1", session.getCurrentFleetCarrierSystem());

        carrierJumpSubscriber.onCarrierJumpCompleteEvent(carrierJump("Pro Eurl FV-M c21-1", false, true));
        awaitTrue(() -> remainingSystems().size() == 1);
        assertEquals(List.of("Pro Eurl EG-V c16-4"), remainingSystems(),
                "on foot aboard: the DOCKED status flag is clear, and it must not matter");
    }

    /**
     * Case 7, sharpened: docking a ship at a station has nothing to say about where the carrier is.
     */
    @Test
    void case7_dockingAtAStationLeavesTheRouteAlone() throws InterruptedException {
        dockedSubscriber.onDockedEvent(docked("Jameson Memorial", "Coriolis", "Shinrarta Dezhra"));

        Thread.sleep(300);
        assertEquals(PLOT, remainingSystems(), "a station berth is not a carrier position");
        assertEquals("Praea Euq BS-M c7-0", session.getCurrentFleetCarrierSystem());
    }

    /**
     * Cases 3 and 7 confused: another commander's fleet carrier is still a fleet carrier. Docking at
     * one used to teleport our own carrier onto his system and eat the route legs in between.
     */
    @Test
    void dockingAtAForeignFleetCarrierNeverMovesOurCarrier() throws InterruptedException {
        CarrierDataDto ours = carrierWithFuel(1000);
        ours.setCallSign(OUR_CALLSIGN);
        session.setFleetCarrierData(ours);

        dockedSubscriber.onDockedEvent(docked("XYZ-99Z", "FleetCarrier", "Sifeae RK-E b54-1"));

        Thread.sleep(300);
        assertEquals(PLOT, remainingSystems(), "his deck says nothing about our carrier");
        assertEquals("Praea Euq BS-M c7-0", session.getCurrentFleetCarrierSystem());
    }

    /**
     * Docking at our own carrier, by contrast, is first-hand evidence of where it is.
     */
    @Test
    void dockingAtOurOwnFleetCarrierRecordsItsSystem() throws InterruptedException {
        CarrierDataDto ours = carrierWithFuel(1000);
        ours.setCallSign(OUR_CALLSIGN);
        session.setFleetCarrierData(ours);

        dockedSubscriber.onDockedEvent(docked(OUR_CALLSIGN, "FleetCarrier", "Sifeae RK-E b54-1"));

        awaitTrue(() -> "Sifeae RK-E b54-1".equals(session.getCurrentFleetCarrierSystem()));
        assertEquals(List.of("Pro Eurl FV-M c21-1", "Pro Eurl EG-V c16-4"), remainingSystems());
    }

    /**
     * The game writes CarrierLocation at every LoadGame, where the carrier has not moved. Treating it
     * as an arrival burned a leg's tritium, cancelled a scheduled jump and re-plotted over the
     * commander's clipboard, every time he restarted the game.
     *
     * <p>WHY the EDSM count is asserted rather than only the session state: an arrival for a system
     * that is no longer a route leg resolves its coordinates over the network before it touches the
     * departure timer. If that call throws, the bookkeeping below it is skipped and the timer
     * survives by accident. The request count sees the wrong branch taken either way.
     */
    @Test
    void aLoadGamePositionReportIsNotAnArrival() throws InterruptedException {
        carrierLocationSubscriber.onCarrierLocationEvent(carrierLocation("Sifeae RK-E b54-1"));
        awaitTrue(() -> remainingSystems().size() == 2);
        int fuelAfterArrival = session.getFleetCarrierData().getFuelLevel();
        session.setCarrierDepartureTime("2026-07-10T09:00:00Z");

        // Game restarts. Same system, no jump.
        carrierLocationSubscriber.onCarrierLocationEvent(carrierLocation("Sifeae RK-E b54-1"));

        Thread.sleep(500);
        wm.verify(0, getRequestedFor(urlPathEqualTo("/api-v1/systems")));
        assertEquals(fuelAfterArrival, session.getFleetCarrierData().getFuelLevel(),
                "a position report burns no tritium");
        assertEquals("2026-07-10T09:00:00Z", session.getCarrierDepartureTime(),
                "a position report cancels no scheduled jump");
        assertEquals(List.of("Pro Eurl FV-M c21-1", "Pro Eurl EG-V c16-4"), remainingSystems());
    }

    /**
     * Arrival burns exactly the tritium of the leg reached, once.
     */
    @Test
    void arrivalBurnsTheLegsTritiumExactlyOnce() throws InterruptedException {
        carrierLocationSubscriber.onCarrierLocationEvent(carrierLocation("Phylurn AZ-C b14-0"));
        awaitTrue(() -> session.getFleetCarrierData().getFuelLevel() == 900);

        carrierLocationSubscriber.onCarrierLocationEvent(carrierLocation("Phylurn AZ-C b14-0"));
        Thread.sleep(300);

        assertEquals(900, session.getFleetCarrierData().getFuelLevel());
    }

    /**
     * Whatever the route holds, the carrier's own system is never in it.
     */
    @Test
    void theCarriersSystemIsNeverAmongTheRemainingLegs() throws InterruptedException {
        for (String system : PLOT) {
            carrierLocationSubscriber.onCarrierLocationEvent(carrierLocation(system));
            awaitTrue(() -> !remainingSystems().contains(system));

            assertFalse(remainingSystems().contains(system),
                    "the carrier is in " + system + "; it cannot also be a leg still to fly");
            assertTrue(remainingSystems().stream().allMatch(s -> PLOT.indexOf(s) > PLOT.indexOf(system)),
                    "every leg behind the carrier goes with it");
        }
        assertTrue(remainingSystems().isEmpty(), "at the destination there is nothing left to fly");
    }

    // --- helpers -------------------------------------------------------------------------------

    private List<String> remainingSystems() {
        return route.getFleetCarrierRoute().values().stream().map(CarrierJump::getSystemName).toList();
    }

    private static CarrierDataDto carrierWithFuel(int fuel) {
        CarrierDataDto carrier = new CarrierDataDto();
        carrier.setMeasuredFuelLevel(fuel);
        return carrier;
    }

    private static CarrierJump leg(String systemName) {
        CarrierJump jump = new CarrierJump();
        jump.setSystemName(systemName);
        jump.setFuelUsed(100);
        jump.setX(1.0);
        jump.setY(2.0);
        jump.setZ(3.0);
        return jump;
    }

    private static CarrierLocationEvent carrierLocation(String starSystem) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "CarrierLocation");
        j.addProperty("CarrierType", "FleetCarrier");
        j.addProperty("CarrierID", 3712500736L);
        j.addProperty("StarSystem", starSystem);
        j.addProperty("SystemAddress", 728401193201L);
        j.addProperty("BodyID", 1);
        return new CarrierLocationEvent(j);
    }

    private static CarrierJumpEvent carrierJump(String starSystem, boolean docked, boolean onFoot) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "CarrierJump");
        j.addProperty("Docked", docked);
        j.addProperty("OnFoot", onFoot);
        j.addProperty("StarSystem", starSystem);
        j.addProperty("SystemAddress", 728401193201L);
        j.addProperty("Body", starSystem + " A");
        j.addProperty("BodyID", 1);
        j.addProperty("BodyType", "Star");
        JsonArray starPos = new JsonArray();
        starPos.add(1.0);
        starPos.add(2.0);
        starPos.add(3.0);
        j.add("StarPos", starPos);
        return new CarrierJumpEvent(j);
    }

    private static DockedEvent docked(String stationName, String stationType, String starSystem) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "Docked");
        j.addProperty("StationName", stationName);
        j.addProperty("StationType", stationType);
        j.addProperty("StarSystem", starSystem);
        j.addProperty("SystemAddress", 3107509474002L);
        j.addProperty("MarketID", 128666762L);
        j.add("StationServices", new JsonArray());
        return new DockedEvent(j);
    }

    private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 3000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) fail("Condition not met within 3 seconds");
            Thread.sleep(10);
        }
    }
}
