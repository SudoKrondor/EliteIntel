package elite.intel.gameapi.eddn;

import elite.intel.db.dao.LocationDao.Coordinates;
import elite.intel.db.managers.ConflictZoneManager;
import elite.intel.db.managers.HuntingGroundManager;
import elite.intel.db.managers.PowerConflictZoneManager;
import elite.intel.db.util.Database;
import elite.intel.gameapi.missions.HuntingGround;
import elite.intel.gameapi.missions.ResourceSiteProfile;
import elite.intel.gameapi.signals.ConflictZoneProfile;
import elite.intel.gameapi.signals.PowerWarZone;
import elite.intel.gameapi.signals.WarZone;
import elite.intel.util.Cypher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * What one message off the EDDN relay teaches the ledgers.
 * <p>
 * The envelopes are shaped like the real ones: the gateway re-serialises with spaces after colons,
 * puts the header before the schema reference about a third of the time, and batches a whole FSS
 * sweep into one message. Each case uses its own corner of the galaxy so the shared database keeps
 * the tests apart.
 */
class EddnSightingsTest {

    private final EddnSightings sightings = new EddnSightings(
            HuntingGroundManager.getInstance(), ConflictZoneManager.getInstance(), PowerConflictZoneManager.getInstance());

    @BeforeAll
    static void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
    }

    @Test
    void oneSweepFillsBothLedgers() {
        String envelope = """
                {"$schemaRef": "https://eddn.edcd.io/schemas/fsssignaldiscovered/1",
                 "header": {"uploaderID": "abc", "softwareName": "E:D Market Connector [Windows]", "gatewayTimestamp": "%1$s"},
                 "message": {"event": "FSSSignalDiscovered", "horizons": true, "odyssey": true, "timestamp": "%1$s",
                   "SystemAddress": 90001, "StarSystem": "Eddn Sweep A", "StarPos": [10000.0, 0.0, 0.0],
                   "signals": [
                     {"timestamp": "%1$s", "SignalName": "Keats Landing", "SignalType": "Outpost", "IsStation": true},
                     {"timestamp": "%1$s", "SignalName": "$MULTIPLAYER_SCENARIO79_TITLE;", "SignalType": "ResourceExtraction"},
                     {"timestamp": "%1$s", "SignalName": "$MULTIPLAYER_SCENARIO79_TITLE;", "SignalType": "ResourceExtraction"},
                     {"timestamp": "%1$s", "SignalName": "$MULTIPLAYER_SCENARIO77_TITLE;", "SignalType": "ResourceExtraction"},
                     {"timestamp": "%1$s", "SignalName": "$Warzone_PointRace_High:#index=1;", "SignalType": "Combat"},
                     {"timestamp": "%1$s", "SignalName": "$Warzone_PointRace_Low:#index=1;", "SignalType": "Combat"},
                     {"timestamp": "%1$s", "SignalName": "$Warzone_PointRace_Low:#index=1;", "SignalType": "Combat"},
                     {"timestamp": "%1$s", "SignalName": "$Warzone_PointRace_Low:#index=2;", "SignalType": "Combat"},
                     {"timestamp": "%1$s", "SignalName": "$Warzone_Powerplay_Med:#index=1;", "SignalType": "Combat"}
                   ]}}
                """.formatted(minutesAgo(50));

        assertTrue(sightings.accept(envelope));

        Coordinates near = new Coordinates("Eddn Near A", 10000.5, 0, 0);
        HuntingGround ground = HuntingGroundManager.getInstance().bestHuntingGrounds(near, 1).getFirst();
        assertEquals("Eddn Sweep A", ground.starSystem());
        assertEquals(new ResourceSiteProfile(0, 1, 0, 2), ground.sites());

        WarZone war = ConflictZoneManager.getInstance().bestWarZones(near, 1).getFirst();
        assertEquals(new ConflictZoneProfile(2, 0, 1, 1), war.zones(),
                "the low zone listed twice is one zone, and the powerplay zone is counted apart");
        assertFalse(war.sidesKnown(), "a sweep names the zones, not the factions");
    }

    @Test
    void anArrivalNamesTheSidesAndTheEnvelopeMayLeadWithTheHeader() {
        String envelope = """
                {"header": {"uploaderID": "abc", "softwareName": "EDDiscovery"},
                 "$schemaRef": "https://eddn.edcd.io/schemas/journal/1",
                 "message": {"timestamp": "%s", "event": "FSDJump",
                   "StarSystem": "Eddn War B", "SystemAddress": 90002, "StarPos": [11000.0, 0.0, 0.0],
                   "Factions": [{"Name": "Starlance Alpha", "FactionState": "War"}],
                   "Conflicts": [
                     {"WarType": "election", "Status": "active", "Faction1": {"Name": "Voters", "Stake": "", "WonDays": 1}, "Faction2": {"Name": "Others", "Stake": "", "WonDays": 0}},
                     {"WarType": "war", "Status": "active", "Faction1": {"Name": "Starlance Alpha", "Stake": "Cole Vista", "WonDays": 3}, "Faction2": {"Name": "Explorer on Tour", "Stake": "Marques Prospect", "WonDays": 3}}
                   ]}}
                """.formatted(minutesAgo(40));
        assertTrue(sightings.accept(envelope));
        sightings.accept(sweep("Eddn War B", 90002, 11000.0, minutesAgo(39),
                "$Warzone_PointRace_Med:#index=1;"));

        WarZone war = ConflictZoneManager.getInstance()
                .bestWarZones(new Coordinates("Eddn Near B", 11000.5, 0, 0), 1).getFirst();
        assertEquals("war", war.warType(), "an election is a conflict the game settles without guns");
        assertEquals("Starlance Alpha", war.faction1());
        assertEquals("Explorer on Tour", war.faction2());
    }

    @Test
    void anArrivalWithNoWarEndsTheOneOnFile() {
        sightings.accept(sweep("Eddn Peace C", 90003, 12000.0, minutesAgo(30),
                "$Warzone_PointRace_High:#index=1;"));
        Coordinates near = new Coordinates("Eddn Near C", 12000.5, 0, 0);
        assertFalse(ConflictZoneManager.getInstance().bestWarZones(near, 1).isEmpty());

        String envelope = """
                {"$schemaRef": "https://eddn.edcd.io/schemas/journal/1", "header": {},
                 "message": {"timestamp": "%s", "event": "Location", "Docked": true,
                   "StarSystem": "Eddn Peace C", "SystemAddress": 90003, "StarPos": [12000.0, 0.0, 0.0],
                   "Factions": [{"Name": "Somebody", "FactionState": "Boom"}]}}
                """.formatted(minutesAgo(10));
        assertTrue(sightings.accept(envelope));

        assertTrue(ConflictZoneManager.getInstance().bestWarZones(near, 1).isEmpty(),
                "another commander landing there and finding no war is the war being over");
    }

    @Test
    void aPowerConflictZoneIsOfferedToAPowerTheRelayFoundThere() {
        sightings.accept(sweep("Eddn Power E", 90005, 13000.0, minutesAgo(30),
                "$Warzone_Powerplay_Med:#index=1;", "$Warzone_Powerplay_Med:#index=2;", "$Warzone_Powerplay_Med:#index=1;"));
        assertTrue(sightings.accept(arrival("Eddn Power E", 90005, 13000.0, minutesAgo(20),
                "\"ControllingPower\": \"Aisling Duval\", \"Powers\": [\"Aisling Duval\", \"Zemina Torval\"], ")));

        Coordinates near = new Coordinates("Eddn Near E", 13000.5, 0, 0);
        PowerWarZone zone = PowerConflictZoneManager.getInstance().bestZonesFor("Zemina Torval", near, 1).getFirst();
        assertEquals("Eddn Power E", zone.starSystem());
        assertEquals(2, zone.zones(), "index 1 listed twice is one zone");
        assertEquals("Aisling Duval", zone.controllingPower());
        assertEquals(List.of("Zemina Torval"), zone.contenders());
        assertTrue(PowerConflictZoneManager.getInstance().bestZonesFor("Felicia Winters", near, 1).isEmpty(),
                "a fight between two other powers is no fight of a Winters pledge");
    }

    @Test
    void anArrivalWithNoPowersEndsThePowerConflictZonesOnFile() {
        sightings.accept(sweep("Eddn Power F", 90006, 14000.0, minutesAgo(30), "$Warzone_Powerplay_Med:#index=1;"));
        sightings.accept(arrival("Eddn Power F", 90006, 14000.0, minutesAgo(25), "\"Powers\": [\"Nakato Kaine\"], "));
        Coordinates near = new Coordinates("Eddn Near F", 14000.5, 0, 0);
        assertFalse(PowerConflictZoneManager.getInstance().bestZonesFor("Nakato Kaine", near, 1).isEmpty());

        sightings.accept(arrival("Eddn Power F", 90006, 14000.0, minutesAgo(10), ""));

        assertTrue(PowerConflictZoneManager.getInstance().bestZonesFor("Nakato Kaine", near, 1).isEmpty(),
                "no power left in the system is no power war in it");
    }

    @Test
    void anOlderArrivalNeverOverwritesTheNewerPowers() {
        sightings.accept(sweep("Eddn Power G", 90007, 15000.0, minutesAgo(30), "$Warzone_Powerplay_Med:#index=1;"));
        sightings.accept(arrival("Eddn Power G", 90007, 15000.0, minutesAgo(10), "\"Powers\": [\"Jerome Archer\"], "));
        sightings.accept(arrival("Eddn Power G", 90007, 15000.0, minutesAgo(20), "\"Powers\": [\"Li Yong-Rui\"], "));

        Coordinates near = new Coordinates("Eddn Near G", 15000.5, 0, 0);
        assertFalse(PowerConflictZoneManager.getInstance().bestZonesFor("Jerome Archer", near, 1).isEmpty(),
                "the relay delivers out of order, and the newest arrival is the one that says who is there");
        assertTrue(PowerConflictZoneManager.getInstance().bestZonesFor("Li Yong-Rui", near, 1).isEmpty());
    }

    @Test
    void everythingElseOnTheRelayIsIgnoredUnparsed() {
        assertFalse(sightings.accept("""
                {"$schemaRef": "https://eddn.edcd.io/schemas/commodity/3", "header": {}, "message": {"this is": "not even valid for us", "oops": [}}
                """), "a commodity message is never parsed, so its shape cannot hurt");
        assertFalse(sightings.accept("""
                {"$schemaRef": "https://eddn.edcd.io/schemas/journal/1", "header": {},
                 "message": {"timestamp": "%s", "event": "Scan", "StarSystem": "Eddn Scan D", "SystemAddress": 90004, "StarPos": [1,2,3]}}
                """.formatted(minutesAgo(5))), "a Scan says nothing about wars");
        assertNull(ConflictZoneManager.getInstance().zonesIn("Eddn Scan D"));
    }

    /**
     * A timestamp shortly before the test runs. Sightings are dated against the real clock and a war unseen
     * for a week expires, so a fixed date turns every war in these cases stale once it is a week old.
     */
    private static String minutesAgo(int minutes) {
        return Instant.now().minus(Duration.ofMinutes(minutes)).truncatedTo(ChronoUnit.SECONDS).toString();
    }

    private static String arrival(String starSystem, long address, double x, String timestamp, String powerFields) {
        return """
                {"$schemaRef": "https://eddn.edcd.io/schemas/journal/1", "header": {},
                 "message": {"timestamp": "%s", "event": "FSDJump", %s
                   "StarSystem": "%s", "SystemAddress": %d, "StarPos": [%s, 0.0, 0.0]}}
                """.formatted(timestamp, powerFields, starSystem, address, x);
    }

    private static String sweep(String starSystem, long address, double x, String timestamp, String... symbols) {
        StringBuilder signals = new StringBuilder();
        for (String symbol : symbols) {
            if (!signals.isEmpty()) signals.append(",");
            signals.append("{\"timestamp\": \"").append(timestamp).append("\", \"SignalName\": \"")
                    .append(symbol).append("\", \"SignalType\": \"Combat\"}");
        }
        return "{\"$schemaRef\": \"https://eddn.edcd.io/schemas/fsssignaldiscovered/1\", \"header\": {},"
                + " \"message\": {\"event\": \"FSSSignalDiscovered\", \"timestamp\": \"" + timestamp + "\","
                + " \"SystemAddress\": " + address + ", \"StarSystem\": \"" + starSystem + "\","
                + " \"StarPos\": [" + x + ", 0.0, 0.0], \"signals\": [" + signals + "]}}";
    }
}
