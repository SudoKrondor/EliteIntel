package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.db.dao.LocationDao;
import elite.intel.db.managers.LocationManager;
import elite.intel.db.util.Database;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.gameapi.journal.events.CarrierJumpEvent;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.journal.subscribers.CarrierJumpCompleteSubscriber;
import elite.intel.session.LocationData;
import elite.intel.session.PlayerSession;
import elite.intel.session.Status;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class CarrierJumpCompleteSubscriberTest {

    private final CarrierJumpCompleteSubscriber subscriber = new CarrierJumpCompleteSubscriber();
    private final PlayerSession session = PlayerSession.getInstance();
    private final LocationManager locationManager = LocationManager.getInstance();

    @BeforeEach
    @AfterEach
    void clearDockedStatus() {
        GameEvents.StatusEvent undocked = new GameEvents.StatusEvent();
        undocked.setFlags(0L);
        Status.getInstance().setStatus(undocked);
    }

    @Test
    void dockedWithBodyIdUpdatesLocationId() throws InterruptedException {
        long sysAddr = 70001000L;
        long bodyId = 7L;
        session.setCurrentLocationId(999L, sysAddr);
        setDockedStatus();

        subscriber.onCarrierJumpCompleteEvent(carrierJumpEvent("CarrierTestSystem", sysAddr, bodyId));

        awaitTrue(() -> Long.valueOf(bodyId).equals(session.getLocationData().getInGameId()));

        assertEquals(bodyId, session.getLocationData().getInGameId());
    }

    /**
     * The commander may be walking around the concourse while the carrier jumps. The DOCKED status
     * flag is clear in that case, but he has still moved with the ship.
     */
    @Test
    void onFootAboardCarrierUpdatesLocationId() throws InterruptedException {
        long sysAddr = 70003000L;
        long bodyId = 3L;
        session.setCurrentLocationId(999L, 12345L);
        // Status left undocked on purpose: flags = 0

        subscriber.onCarrierJumpCompleteEvent(onFootCarrierJumpEvent("CarrierOnFootSystem", sysAddr, bodyId));

        awaitTrue(() -> Long.valueOf(bodyId).equals(session.getLocationData().getInGameId()));

        assertEquals(bodyId, session.getLocationData().getInGameId());
        assertEquals(sysAddr, session.getLocationData().getSystemAddress());
    }

    @Test
    void notAboardDoesNotUpdateLocationId() throws InterruptedException {
        long knownSysAddr = 70004000L;
        long knownBodyId = 55L;
        session.setCurrentLocationId(knownBodyId, knownSysAddr);
        session.setFleetCarrierData(new CarrierDataDto());

        // Docked and OnFoot both false — the carrier moved without the commander.
        subscriber.onCarrierJumpCompleteEvent(notAboardCarrierJumpEvent("CarrierAwaySystem", 70004999L, 8L));

        awaitHandlerPastLocationBlock();

        assertEquals(knownBodyId, session.getLocationData().getInGameId());
        assertEquals(knownSysAddr, session.getLocationData().getSystemAddress());
    }

    /**
     * Half the galaxy has a negative x. The old guard only persisted coordinates when x > 0.
     */
    @Test
    void negativeGalacticCoordinatesArePersistedForCarrier() throws InterruptedException {
        session.setFleetCarrierData(new CarrierDataDto());

        subscriber.onCarrierJumpCompleteEvent(
                carrierJumpEventAt("Colonia", 70005000L, 4L, -9530.5, -910.28, 19808.12));

        awaitTrue(() -> session.getFleetCarrierData().getX() == -9530.5);

        CarrierDataDto carrier = session.getFleetCarrierData();
        assertEquals(-9530.5, carrier.getX());
        assertEquals(-910.28, carrier.getY());
        assertEquals(19808.12, carrier.getZ());
    }

    /**
     * The row must be written with the real systemAddress, otherwise "where am I" cannot find it:
     * the current-location pointer holds the real address and the lookup joins on it.
     */
    @Test
    void dockedJumpSavesLocationRowFindableByTheCurrentLocationPointer() throws InterruptedException {
        long sysAddr = 70006000L;
        long bodyId = 1L;
        String starSystem = "CarrierFindableSystem";
        setDockedStatus();

        subscriber.onCarrierJumpCompleteEvent(carrierJumpEvent(starSystem, sysAddr, bodyId));

        // PlayerSession is a singleton shared across tests, so wait on the values this test owns
        // rather than on "some row is reachable", which a previous test's pointer would satisfy.
        awaitTrue(() -> sysAddr == session.getLocationData().getSystemAddress()
                && locationManager.findBySystemAddress(sysAddr, bodyId).getStarName() != null);

        LocationDto saved = locationManager.findByLocationData(session.getLocationData());
        assertEquals(starSystem, saved.getStarName());
        assertEquals(sysAddr, saved.getSystemAddress());
        assertEquals(LocationDto.LocationType.PRIMARY_STAR, saved.getLocationType());
    }

    /**
     * LocationDao.upsert conflicts on the unique locationName. Fetching the row by bodyId instead
     * would miss when the journal reports a different BodyID for the same body, and the upsert would
     * then overwrite the real row with a sparse one, destroying scan data.
     */
    @Test
    void carrierJumpDoesNotWipeExistingBodyDataWhenBodyIdDiffers() throws InterruptedException {
        long sysAddr = 70007000L;
        String starSystem = "CarrierClobberSystem";
        String bodyName = starSystem + " A";

        LocationDto rich = new LocationDto(1L, sysAddr);
        rich.setStarName(starSystem);
        rich.setPlanetName(bodyName);
        rich.setLocationType(LocationDto.LocationType.PRIMARY_STAR);
        rich.setPopulation(123456789L);
        Database.withDao(LocationDao.class, dao -> {
            dao.upsert(1L, bodyName, starSystem, sysAddr, GsonFactory.getGson().toJson(rich));
            return null;
        });

        setDockedStatus();
        // Same body name, but the journal reports BodyID 0 rather than the stored 1.
        subscriber.onCarrierJumpCompleteEvent(carrierJumpEvent(starSystem, sysAddr, 0L));

        // StarPos x is 1.0 in the fixture, so a non-zero x means the handler's save has landed.
        awaitTrue(() -> locationManager.findBySystemAddress(sysAddr, bodyName).getX() != 0.0);

        LocationDto after = locationManager.findBySystemAddress(sysAddr, bodyName);
        assertEquals(123456789L, after.getPopulation(), "existing body data must survive a carrier jump");
        assertEquals(LocationDto.LocationType.PRIMARY_STAR, after.getLocationType());

        // LocationDto.setBodyId refuses to lower an id, so the row keeps bodyId 1. The location
        // pointer has to agree with the row, or "where am I" resolves to nothing.
        assertEquals(1L, after.getBodyId(), "the stored id must not be lowered to the journal's 0");
        assertEquals(Long.valueOf(1L), session.getLocationData().getInGameId(),
                "the location pointer must match the id the row actually holds");
        assertEquals(starSystem, locationManager.findByLocationData(session.getLocationData()).getStarName(),
                "the saved row must be reachable through the current-location pointer");
    }

    /**
     * A missing BodyID must not move the location pointer, and must not abort the handler either:
     * the jump still has to be announced and the carrier's own position recorded.
     */
    @Test
    void dockedWithNullBodyIdDoesNotUpdateLocationIdButHandlerCompletes() throws InterruptedException {
        long sysAddr = 70002000L;
        long knownBodyId = 42L;
        session.setCurrentLocationId(knownBodyId, sysAddr);
        session.setFleetCarrierData(new CarrierDataDto());
        setDockedStatus();

        subscriber.onCarrierJumpCompleteEvent(carrierJumpEventNullBodyId("CarrierNullSystem", sysAddr));

        awaitHandlerPastLocationBlock();

        LocationData<Long, Long> loc = session.getLocationData();
        assertEquals(knownBodyId, loc.getInGameId(), "null BodyID must not overwrite current_location_id");
    }

    /**
     * The carrier coordinate write sits immediately after the location block, so a carrier x of 1.0
     * (the fixture's StarPos) proves the handler ran through that block. Avoids sleeping to prove a
     * negative, which passes vacuously if the virtual thread has not started yet.
     */
    private void awaitHandlerPastLocationBlock() throws InterruptedException {
        awaitTrue(() -> session.getFleetCarrierData().getX() == 1.0);
    }

    private static void setDockedStatus() {
        GameEvents.StatusEvent docked = new GameEvents.StatusEvent();
        docked.setFlags(1L); // DOCKED bit
        Status.getInstance().setStatus(docked);
    }

    private static CarrierJumpEvent carrierJumpEvent(String starSystem, long systemAddress, long bodyId) {
        return build(starSystem, systemAddress, bodyId, true, false, 1.0, 2.0, 3.0);
    }

    private static CarrierJumpEvent onFootCarrierJumpEvent(String starSystem, long systemAddress, long bodyId) {
        return build(starSystem, systemAddress, bodyId, false, true, 1.0, 2.0, 3.0);
    }

    private static CarrierJumpEvent notAboardCarrierJumpEvent(String starSystem, long systemAddress, long bodyId) {
        return build(starSystem, systemAddress, bodyId, false, false, 1.0, 2.0, 3.0);
    }

    private static CarrierJumpEvent carrierJumpEventAt(String starSystem, long systemAddress, long bodyId,
                                                       double x, double y, double z) {
        return build(starSystem, systemAddress, bodyId, true, false, x, y, z);
    }

    private static CarrierJumpEvent build(String starSystem, long systemAddress, long bodyId,
                                          boolean docked, boolean onFoot, double x, double y, double z) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "CarrierJump");
        j.addProperty("Docked", docked);
        j.addProperty("OnFoot", onFoot);
        j.addProperty("StarSystem", starSystem);
        j.addProperty("SystemAddress", systemAddress);
        j.addProperty("Body", starSystem + " A");
        j.addProperty("BodyID", bodyId);
        j.addProperty("BodyType", "Star");
        JsonArray starPos = new JsonArray();
        starPos.add(x);
        starPos.add(y);
        starPos.add(z);
        j.add("StarPos", starPos);
        return new CarrierJumpEvent(j);
    }

    private static CarrierJumpEvent carrierJumpEventNullBodyId(String starSystem, long systemAddress) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().toString());
        j.addProperty("event", "CarrierJump");
        j.addProperty("Docked", true);
        j.addProperty("StarSystem", starSystem);
        j.addProperty("SystemAddress", systemAddress);
        j.addProperty("Body", starSystem + " A");
        // BodyID intentionally omitted — Gson will deserialise it as null
        JsonArray starPos = new JsonArray();
        starPos.add(1.0);
        starPos.add(2.0);
        starPos.add(3.0);
        j.add("StarPos", starPos);
        return new CarrierJumpEvent(j);
    }

    private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) fail("Condition not met within 2 seconds");
            Thread.sleep(10);
        }
    }
}
