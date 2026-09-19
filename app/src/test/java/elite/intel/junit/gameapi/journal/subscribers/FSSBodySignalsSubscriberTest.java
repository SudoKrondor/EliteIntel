package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.vega.VegaNarrator;
import elite.intel.ai.brain.vega.VegaRuntimeGraph;
import elite.intel.ai.brain.vega.VegaRuntimeTestSupport;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.FSSBodySignalsEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.journal.subscribers.FSSBodySignalsSubscriber;
import elite.intel.session.PlayerSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Shaped after the 76 Leonis journal of 2026-09-18: the commander arrived by carrier, so the system had no
 * PRIMARY_STAR record - only a jump writes one - and the FSS wrote FSSBodySignals for six moons resolved on an
 * earlier visit with no Scan for any of them. The subscriber took the system name from the primary star's record,
 * came away with null, and the save dropped all six without a word: "how many moons have bio signals" was
 * answered with the one moon a DSS pass had recorded.
 */
class FSSBodySignalsSubscriberTest {

    // Not the real 76 Leonis address: the table is shared across the run and another test files that one under a different name.
    private static final long SYSTEM_ADDRESS = 358999069301L;
    private static final String SYSTEM = "76 Leonis";

    private final FSSBodySignalsSubscriber subscriber = new FSSBodySignalsSubscriber();
    private final LocationManager locationManager = LocationManager.getInstance();
    private VegaRuntimeGraph runtimeGraph;

    @BeforeEach
    void installNarratorAndTheCarrierWeArrivedOn() {
        runtimeGraph = VegaRuntimeTestSupport.installNarrator(new SilentNarrator());
        // The only record a carrier arrival leaves for the system: the carrier itself. No primary star.
        LocationDto carrier = new LocationDto(3712500736L, SYSTEM_ADDRESS);
        carrier.setStarName(SYSTEM);
        carrier.setStationName("GHY-L8X");
        carrier.setLocationType(LocationDto.LocationType.FLEET_CARRIER);
        locationManager.save(carrier);
    }

    @AfterEach
    void clearNarrator() {
        VegaRuntimeTestSupport.uninstall(runtimeGraph);
    }

    @Test
    void aBodyWithNoScanIsRecordedAndClassifiedFromItsName() throws InterruptedException {
        subscriber.onFssBodySignal(bioSignals("76 Leonis 6 c", 27L, 7));

        awaitTrue(() -> locationManager.findBySystemAddress(SYSTEM_ADDRESS, 27L).getBioSignals() == 7);

        LocationDto moon = locationManager.findBySystemAddress(SYSTEM_ADDRESS, 27L);
        assertEquals(LocationDto.LocationType.MOON, moon.getLocationType());
        assertEquals(SYSTEM, moon.getStarName());
    }

    /**
     * The first body filed in a system nothing else has written to: the session's current system names it.
     */
    @Test
    void theFirstBodyInAnUnrecordedSystemIsNamedFromTheSession() throws InterruptedException {
        long emptySystem = 3932277478201L;
        PlayerSession session = PlayerSession.getInstance();
        session.setCurrentLocationId(0L, emptySystem);
        session.setCurrentPrimaryStarName("Synuefe XR-H d11-102");
        JsonObject j = bioSignalsJson("Synuefe XR-H d11-102 4 b", 12L, 3);
        j.addProperty("SystemAddress", emptySystem);

        subscriber.onFssBodySignal(new FSSBodySignalsEvent(j));

        awaitTrue(() -> locationManager.findBySystemAddress(emptySystem, 12L).getBioSignals() == 3);

        LocationDto moon = locationManager.findBySystemAddress(emptySystem, 12L);
        assertEquals("Synuefe XR-H d11-102", moon.getStarName());
        assertEquals(LocationDto.LocationType.MOON, moon.getLocationType());
    }

    @Test
    void aKindAScanEstablishedIsLeftAlone() throws InterruptedException {
        LocationDto scanned = new LocationDto(28L, SYSTEM_ADDRESS);
        scanned.setStarName(SYSTEM);
        scanned.setPlanetName("76 Leonis 6 d");
        scanned.setLocationType(LocationDto.LocationType.PLANET);
        locationManager.save(scanned);

        subscriber.onFssBodySignal(bioSignals("76 Leonis 6 d", 28L, 7));

        awaitTrue(() -> locationManager.findBySystemAddress(SYSTEM_ADDRESS, 28L).getBioSignals() == 7);

        assertEquals(LocationDto.LocationType.PLANET, locationManager.findBySystemAddress(SYSTEM_ADDRESS, 28L).getLocationType());
    }

    private static FSSBodySignalsEvent bioSignals(String bodyName, long bodyId, int count) {
        return new FSSBodySignalsEvent(bioSignalsJson(bodyName, bodyId, count));
    }

    private static JsonObject bioSignalsJson(String bodyName, long bodyId, int count) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().plusSeconds(1).toString());
        j.addProperty("event", "FSSBodySignals");
        j.addProperty("BodyName", bodyName);
        j.addProperty("BodyID", bodyId);
        j.addProperty("SystemAddress", SYSTEM_ADDRESS);
        JsonArray signals = new JsonArray();
        JsonObject bio = new JsonObject();
        bio.addProperty("Type", "$SAA_SignalType_Biological;");
        bio.addProperty("Type_Localised", "Biological");
        bio.addProperty("Count", count);
        signals.add(bio);
        j.add("Signals", signals);
        return j;
    }

    private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) fail("Condition not met within 2 seconds");
            Thread.sleep(10);
        }
    }

    /**
     * The subscriber announces the count when the discovery toggle is on; the row it writes is what is under test.
     */
    private static final class SilentNarrator implements VegaNarrator {
        @Override
        public void filler(String text, boolean urgent) {
        }

        @Override
        public void narrate(String data, String instructions) {
        }

        @Override
        public void announce(String phrase, boolean urgent) {
        }
    }
}
