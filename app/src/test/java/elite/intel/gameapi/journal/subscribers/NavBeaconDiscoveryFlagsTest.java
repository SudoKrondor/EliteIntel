package elite.intel.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.gameapi.journal.events.ScanEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A nav beacon hands over a whole system at once, and the Scan events it produces report
 * {@code WasDiscovered:false} for bodies charted decades ago: the flags say how this scan learned about the
 * body, not whether anyone had been there before.
 * <p>
 * Believing them announced "New System discovered!" for Wolf 1323, a populated system with named planets,
 * while the commander was deep inside the bubble. The same flags were also being written to the database,
 * where {@code ourDiscovery} feeds the exobiology first-discovery bonus.
 */
class NavBeaconDiscoveryFlagsTest {

    /**
     * Verbatim from Journal.2026-08-11T200621.01.log, the scan that produced the false announcement.
     */
    private static final String WOLF_1323_BEACON = """
            { "timestamp":"2026-08-12T11:36:53Z", "event":"Scan", "ScanType":"NavBeaconDetail",
              "BodyName":"Wolf 1323", "BodyID":0, "StarSystem":"Wolf 1323", "SystemAddress":83852530378,
              "DistanceFromArrivalLS":0.0, "StarType":"K", "WasDiscovered":false, "WasMapped":false }
            """;

    /**
     * Also verbatim: the beacon claimed this body was mapped but never discovered, which cannot happen. The
     * contradiction is what proves the flags carry no discovery information rather than being merely stale.
     */
    private static final String IMPOSSIBLE_STATE = """
            { "timestamp":"2026-08-12T11:36:53Z", "event":"Scan", "ScanType":"NavBeaconDetail",
              "BodyName":"Karpo", "BodyID":1, "StarSystem":"Wolf 1323", "SystemAddress":83852530378,
              "DistanceFromArrivalLS":1000.0, "PlanetClass":"High metal content body",
              "WasDiscovered":false, "WasMapped":true }
            """;

    private static final String AUTO_SCAN = """
            { "timestamp":"2026-08-12T11:33:50Z", "event":"Scan", "ScanType":"AutoScan",
              "BodyName":"Tascheter Sector OY-R a4-3", "BodyID":0, "StarSystem":"Tascheter Sector OY-R a4-3",
              "SystemAddress":58140435303200, "DistanceFromArrivalLS":0.0, "StarType":"Y",
              "WasDiscovered":true, "WasMapped":false }
            """;

    @Test
    void aBeaconScanCarriesNoDiscoveryInformation() {
        assertTrue(ScanEventSubscriber.carriesNoDiscoveryInformation(scan(WOLF_1323_BEACON)));
        assertTrue(ScanEventSubscriber.carriesNoDiscoveryInformation(scan(IMPOSSIBLE_STATE)));
    }

    @Test
    void arrivingUnderOwnPowerStillCarriesIt() {
        // AutoScan and Detailed are the commander's own discovery scanner and must keep announcing.
        assertFalse(ScanEventSubscriber.carriesNoDiscoveryInformation(scan(AUTO_SCAN)));
        assertFalse(ScanEventSubscriber.carriesNoDiscoveryInformation(
                scan(AUTO_SCAN.replace("AutoScan", "Detailed"))));
    }

    @Test
    void anUnknownOrAbsentScanTypeIsTrusted() {
        // Suppressing on anything unrecognised would silence real discoveries, which is the worse failure:
        // a missed first discovery cannot be recovered, a spurious announcement is only noise.
        assertFalse(ScanEventSubscriber.carriesNoDiscoveryInformation(
                scan(AUTO_SCAN.replace("\"ScanType\":\"AutoScan\",", ""))));
        assertFalse(ScanEventSubscriber.carriesNoDiscoveryInformation(
                scan(AUTO_SCAN.replace("AutoScan", "SomeFutureScanType"))));
    }

    @Test
    void theScanTypeIsMatchedRegardlessOfCasing() {
        assertTrue(ScanEventSubscriber.carriesNoDiscoveryInformation(
                scan(WOLF_1323_BEACON.replace("NavBeaconDetail", "navbeacondetail"))));
    }

    private static ScanEvent scan(String journalLine) {
        JsonObject json = JsonParser.parseString(journalLine).getAsJsonObject();
        return new ScanEvent(json);
    }
}
