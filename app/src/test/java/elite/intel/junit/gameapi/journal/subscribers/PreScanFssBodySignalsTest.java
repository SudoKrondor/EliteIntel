package elite.intel.junit.gameapi.journal.subscribers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import elite.intel.db.managers.LocationManager;
import elite.intel.gameapi.journal.events.FSSBodySignalsEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.gameapi.journal.subscribers.SilentPersistenceSubscriber;
import elite.intel.util.ExoBio;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The pre-scan replay is the only reader of a journal line older than the app start, and the FSS repeats no
 * Scan for a body resolved on an earlier visit - so a signal report replayed here is all the app will ever
 * hear about such a body. Shaped after 76 Leonis: arrived by carrier (no primary star on record), six moons
 * FSS'd before a restart. Synchronous, like every pre-scan handler.
 */
class PreScanFssBodySignalsTest {

    // Not the real 76 Leonis address: the table is shared across the run and another test files that one under a different name.
    private static final long SYSTEM_ADDRESS = 358999069302L;
    private static final String SYSTEM = "76 Leonis";

    private final SilentPersistenceSubscriber subscriber = new SilentPersistenceSubscriber();
    private final LocationManager locationManager = LocationManager.getInstance();

    @Test
    void aReplayedReportInACarrierArrivalSystemIsFiledAsABioMoon() {
        LocationDto carrier = new LocationDto(3712500736L, SYSTEM_ADDRESS);
        carrier.setStarName(SYSTEM);
        carrier.setStationName("GHY-L8X");
        carrier.setLocationType(LocationDto.LocationType.FLEET_CARRIER);
        locationManager.save(carrier);

        subscriber.onFSSBodySignals(bioSignals("76 Leonis 6 g", 31L, 7));

        LocationDto moon = locationManager.findBySystemAddress(SYSTEM_ADDRESS, 31L);
        assertEquals("76 Leonis 6 g", moon.getPlanetName());
        assertEquals(SYSTEM, moon.getStarName());
        assertEquals(LocationDto.LocationType.MOON, moon.getLocationType());
        assertEquals(7, ExoBio.bioSignalsDetected(moon));
    }

    @Test
    void replayingTheSameReportTwiceFilesItOnce() {
        LocationDto carrier = new LocationDto(3712500736L, SYSTEM_ADDRESS);
        carrier.setStarName(SYSTEM);
        carrier.setStationName("GHY-L8X");
        carrier.setLocationType(LocationDto.LocationType.FLEET_CARRIER);
        locationManager.save(carrier);

        subscriber.onFSSBodySignals(bioSignals("76 Leonis 6 f", 30L, 7));
        subscriber.onFSSBodySignals(bioSignals("76 Leonis 6 f", 30L, 7));

        LocationDto moon = locationManager.findBySystemAddress(SYSTEM_ADDRESS, 30L);
        assertEquals(7, ExoBio.bioSignalsDetected(moon));
        assertEquals(1, moon.getDetectedSignals().size());
    }

    private static FSSBodySignalsEvent bioSignals(String bodyName, long bodyId, int count) {
        JsonObject j = new JsonObject();
        j.addProperty("timestamp", Instant.now().minusSeconds(3600).toString());
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
        return new FSSBodySignalsEvent(j);
    }
}
