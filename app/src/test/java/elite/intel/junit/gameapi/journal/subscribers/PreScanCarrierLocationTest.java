package elite.intel.junit.gameapi.journal.subscribers;

import com.google.common.eventbus.EventBus;
import com.google.gson.JsonObject;
import elite.intel.db.managers.FleetCarrierRouteManager;
import elite.intel.gameapi.journal.events.CarrierLocationEvent;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.gameapi.journal.subscribers.SilentPersistenceSubscriber;
import elite.intel.gameapi.search.spansh.carrierroute.CarrierJump;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The pre-scan replays every carrier arrival on startup, including the ones the commander was not
 * aboard for, and including jumps the carrier made with the game closed. The live parser drops journal
 * lines older than app start, so whatever such an arrival implies has to be worked out here or not at
 * all: where the carrier is, that the departure it was counting down to is over, and that a plotted
 * route no longer runs from where it now sits.
 *
 * <p>The arrivals carry no StarPos, and out in uncharted space nothing else knows the system either,
 * so the coordinates on file would otherwise stay the ones the carrier left and be read as this
 * system's. The pre-scan makes no network calls, so the SystemAddress is its only source.
 *
 * <p>Posted through a real EventBus rather than called directly, for the reason
 * {@code SilentPersistenceCarrierJumpTest} documents.
 */
class PreScanCarrierLocationTest {

    private final PlayerSession session = PlayerSession.getInstance();

    @Test
    void anUnchartedArrivalIsPositionedFromItsSystemAddress() {
        EventBus privateBus = new EventBus("pre-scan-test");
        privateBus.register(new SilentPersistenceSubscriber());

        session.setFleetCarrierData(carrierAt("Eephaik LY-R b47-6", -11758.34375, -894.53125, 17880.5));

        privateBus.post(carrierLocation("Eephaik CX-V b31-9", 20299220533521L));

        CarrierDataDto stored = session.getFleetCarrierData();
        assertEquals("Eephaik CX-V b31-9", stored.getStarName());
        assertEquals(-12135.0, stored.getX(), 0.001, "x from the arrival system's boxel");
        assertEquals(-895.0, stored.getY(), 0.001, "y from the arrival system's boxel");
        assertEquals(17545.0, stored.getZ(), 0.001, "z from the arrival system's boxel");
    }

    /**
     * The journal is replayed on every start, so a position report for the system the carrier is
     * already in must not trade exact coordinates (a plotted leg's, say) for a boxel estimate of the
     * same system.
     */
    @Test
    void aRepeatedArrivalKeepsCoordinatesAlreadyResolvedForThatSystem() {
        EventBus privateBus = new EventBus("pre-scan-test");
        privateBus.register(new SilentPersistenceSubscriber());

        // Exact coordinates, and deliberately not the boxel centre of this system.
        session.setFleetCarrierData(carrierAt("Eephaik CX-V b31-9", -12140.125, -897.375, 17549.625));

        privateBus.post(carrierLocation("Eephaik CX-V b31-9", 20299220533521L));

        CarrierDataDto stored = session.getFleetCarrierData();
        assertEquals(-12140.125, stored.getX(), 0.001, "exact coordinates must not be downgraded");
        assertEquals(-897.375, stored.getY(), 0.001, "exact coordinates must not be downgraded");
        assertEquals(17549.625, stored.getZ(), 0.001, "exact coordinates must not be downgraded");
    }

    /**
     * A carrier jumps on its own schedule, so the app can come up to find the jump it was counting
     * down to already made. The live parser drops journal lines older than app start, so the pre-scan
     * is the only place that can retire the departure.
     */
    @Test
    void anArrivalLearnedAtStartupRetiresTheDepartureItCompleted() {
        EventBus privateBus = new EventBus("pre-scan-test");
        session.setLastKnownCarrierLocation("Eephaik LY-R b47-6");
        session.setCarrierDepartureTime("2026-07-30T04:24:10Z");
        privateBus.register(new SilentPersistenceSubscriber());

        privateBus.post(carrierLocation("Eephaik CX-V b31-9", 20299220533521L));

        // Blank, not null: the column is NOT NULL DEFAULT '', and "no departure scheduled" is what
        // every reader of this value tests for (see AnalyzeCarrierDepartureEtaQuery).
        String departureTime = session.getCarrierDepartureTime();
        assertTrue(departureTime == null || departureTime.isBlank(),
                "the departure the carrier has made is over, but the app still reports " + departureTime);
    }

    /**
     * The game writes CarrierLocation at every LoadGame, where the carrier has not moved and a pending
     * jump is still pending. The journal is replayed on every start, so clearing on those would forget
     * a scheduled departure the commander is waiting on.
     */
    @Test
    void aPositionReportLeavesAPendingDepartureAlone() {
        EventBus privateBus = new EventBus("pre-scan-test");
        session.setLastKnownCarrierLocation("Eephaik CX-V b31-9");
        session.setCarrierDepartureTime("2026-07-30T06:00:00Z");
        privateBus.register(new SilentPersistenceSubscriber());

        privateBus.post(carrierLocation("Eephaik CX-V b31-9", 20299220533521L));

        assertEquals("2026-07-30T06:00:00Z", session.getCarrierDepartureTime(),
                "the carrier has not moved, so its scheduled jump is still ahead of it");
    }

    @Test
    void anOffRouteArrivalLearnedAtStartupMarksTheRouteForReplotting() {
        FleetCarrierRouteManager route = FleetCarrierRouteManager.getInstance();
        route.clear();
        session.setLastKnownCarrierLocation("Eephaik LY-R b47-6");
        route.setFleetCarrierRoute(Map.of(1, leg("Dryooe Flyou GB-I c24-147"), 2, leg("Colonia")));

        EventBus privateBus = new EventBus("pre-scan-test");
        SilentPersistenceSubscriber persistence = new SilentPersistenceSubscriber();
        privateBus.register(persistence);

        privateBus.post(carrierLocation("Eephaik CX-V b31-9", 20299220533521L));

        assertTrue(persistence.carrierRouteNeedsReplot(),
                "the stored route no longer runs from where the carrier is");
        route.clear();
    }

    @Test
    void anOnRouteArrivalLeavesTheRouteAlone() {
        FleetCarrierRouteManager route = FleetCarrierRouteManager.getInstance();
        route.clear();
        session.setLastKnownCarrierLocation("Eephaik LY-R b47-6");
        route.setFleetCarrierRoute(Map.of(1, leg("Dryooe Flyou GB-I c24-147"), 2, leg("Colonia")));

        EventBus privateBus = new EventBus("pre-scan-test");
        SilentPersistenceSubscriber persistence = new SilentPersistenceSubscriber();
        privateBus.register(persistence);

        // The carrier flew the leg it was plotted to fly.
        privateBus.post(carrierLocation("Dryooe Flyou GB-I c24-147", 20299220533521L));

        assertFalse(persistence.carrierRouteNeedsReplot(),
                "reads already truncate a route at the carrier's own system; nothing to re-plot");
        route.clear();
    }

    @Test
    void aPositionReportDoesNotMarkTheRouteForReplotting() {
        FleetCarrierRouteManager route = FleetCarrierRouteManager.getInstance();
        route.clear();
        session.setLastKnownCarrierLocation("Eephaik CX-V b31-9");
        route.setFleetCarrierRoute(Map.of(1, leg("Dryooe Flyou GB-I c24-147"), 2, leg("Colonia")));

        EventBus privateBus = new EventBus("pre-scan-test");
        SilentPersistenceSubscriber persistence = new SilentPersistenceSubscriber();
        privateBus.register(persistence);

        privateBus.post(carrierLocation("Eephaik CX-V b31-9", 20299220533521L));

        assertFalse(persistence.carrierRouteNeedsReplot(), "the carrier has not moved");
        route.clear();
    }

    private static CarrierJump leg(String systemName) {
        CarrierJump jump = new CarrierJump();
        jump.setSystemName(systemName);
        jump.setFuelUsed(100);
        return jump;
    }

    private static CarrierDataDto carrierAt(String starName, double x, double y, double z) {
        CarrierDataDto carrier = new CarrierDataDto();
        carrier.setStarName(starName);
        carrier.setX(x);
        carrier.setY(y);
        carrier.setZ(z);
        return carrier;
    }

    private static CarrierLocationEvent carrierLocation(String system, long systemAddress) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "CarrierLocation");
        j.addProperty("CarrierType", "FleetCarrier");
        j.addProperty("CarrierID", 3712500736L);
        j.addProperty("StarSystem", system);
        j.addProperty("SystemAddress", systemAddress);
        j.addProperty("BodyID", 0);
        return new CarrierLocationEvent(j);
    }
}
