package elite.intel.gameapi.journal.subscribers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.db.managers.CartographicDataManager;
import elite.intel.db.util.Database;
import elite.intel.gameapi.journal.events.*;
import elite.intel.util.Cypher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The commander's own journal, replayed through the subscriber: the three honked systems sold on 2026-08-12
 * for a BaseValue of 12,103, then what a nav beacon, a sale and a death do to the ledger.
 */
class CartographicDataSubscriberTest {

    private static final long SHIP = 8_812;
    private static final long PISCIUM = 58138287819536L;
    private static final long TASCHETER = 58140435303200L;
    private static final long COL_359 = 110882633698256L;

    private final CartographicDataSubscriber subscriber = new CartographicDataSubscriber();
    private final CartographicDataManager ledger = CartographicDataManager.getInstance();

    @BeforeAll
    static void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
    }

    @AfterEach
    void scrap() {
        ledger.boardShip(SHIP);
        ledger.lost();
    }

    @Test
    void threeHonkedSystemsAreWorthWhatTheyFetched() {
        flyTheTrip();

        assertEquals(12_103, ledger.worth(PISCIUM).unsold());
        assertEquals(1_200 + 11 * 500, ledger.worth(PISCIUM).system());
    }

    @Test
    void theSaleClearsWhatItSold() {
        flyTheTrip();

        subscriber.onSold(new MultiSellExplorationDataEvent(json("""
                {"timestamp":"2026-08-12T13:18:06Z","event":"MultiSellExplorationData","Discovered":[
                 {"SystemName":"Piscium Sector TT-Z a3","NumBodies":1},
                 {"SystemName":"Tascheter Sector OY-R a4-3","NumBodies":1}],
                 "BaseValue":6701,"Bonus":0,"TotalEarnings":6165}""")));

        assertEquals(0, ledger.worth(PISCIUM).system());
        assertEquals(1_202 + 6 * 500, ledger.worth(COL_359).unsold(), "Col 359 was not in this sale");
    }

    @Test
    void aDeathLosesEverythingAboard() {
        flyTheTrip();

        subscriber.onDied(new DiedEvent(json("""
                { "timestamp":"2026-09-03T21:53:43Z", "event":"Died" }""")));

        assertEquals(0, ledger.worth(PISCIUM).unsold());
    }

    @Test
    void aNavBeaconDumpIsNotTheCommandersData() {
        subscriber.onLoadout(new LoadoutEvent(json("""
                {"timestamp":"2026-08-16T06:00:00Z","event":"Loadout","ShipID":%d}""".formatted(SHIP))));

        subscriber.onScan(new ScanEvent(json("""
                {"timestamp":"2026-08-16T06:36:58Z","event":"Scan","ScanType":"NavBeaconDetail",
                 "BodyName":"Col 285 Sector KX-U a32-0 8","BodyID":9,"StarSystem":"Col 285 Sector KX-U a32-0",
                 "SystemAddress":5377835813712,"DistanceFromArrivalLS":1200.0,"PlanetClass":"Icy body",
                 "MassEM":0.862188,"WasDiscovered":true,"WasMapped":false}""")));

        assertEquals(0, ledger.worth(5377835813712L).unsold());
    }

    @Test
    void aBeltClusterIsWorthNothing() {
        subscriber.onLoadout(new LoadoutEvent(json("""
                {"timestamp":"2026-08-16T06:00:00Z","event":"Loadout","ShipID":%d}""".formatted(SHIP))));

        subscriber.onScan(new ScanEvent(json("""
                {"timestamp":"2026-08-07T02:01:59Z","event":"Scan","ScanType":"AutoScan",
                 "BodyName":"Trianguli Sector YF-N a7-5 A Belt Cluster 1","BodyID":2,
                 "StarSystem":"Trianguli Sector YF-N a7-5","SystemAddress":93335544810280,
                 "DistanceFromArrivalLS":0.0,"WasDiscovered":true,"WasMapped":false}""")));

        assertEquals(0, ledger.worth(93335544810280L).unsold());
    }

    private void flyTheTrip() {
        subscriber.onLoadout(new LoadoutEvent(json("""
                {"timestamp":"2026-08-11T12:00:00Z","event":"Loadout","ShipID":%d}""".formatted(SHIP))));

        subscriber.onHonk(new FSSDiscoveryScanEvent(json("""
                {"timestamp": "2026-08-11T12:08:04Z", "event": "FSSDiscoveryScan", "SystemName": "Col 359 Sector FW-Z a3-6", "SystemAddress": 110882633698256, "BodyCount": 7, "NonBodyCount": 0}""")));
        subscriber.onScan(new ScanEvent(json("""
                {"timestamp": "2026-08-11T12:08:04Z", "event": "Scan", "ScanType": "Detailed", "BodyName": "Col 359 Sector FW-Z a3-6", "BodyID": 0, "StarSystem": "Col 359 Sector FW-Z a3-6", "SystemAddress": 110882633698256, "DistanceFromArrivalLS": 0.0, "StarType": "L", "StellarMass": 0.132813, "WasDiscovered": true, "WasMapped": false}""")));

        subscriber.onHonk(new FSSDiscoveryScanEvent(json("""
                {"timestamp": "2026-08-12T11:30:45Z", "event": "FSSDiscoveryScan", "SystemName": "Piscium Sector TT-Z a3", "SystemAddress": 58138287819536, "BodyCount": 12, "NonBodyCount": 5}""")));
        subscriber.onScan(new ScanEvent(json("""
                {"timestamp": "2026-08-12T11:30:46Z", "event": "Scan", "ScanType": "Detailed", "BodyName": "Piscium Sector TT-Z a3", "BodyID": 0, "StarSystem": "Piscium Sector TT-Z a3", "SystemAddress": 58138287819536, "DistanceFromArrivalLS": 0.0, "StarType": "Y", "StellarMass": 0.023438, "WasDiscovered": true, "WasMapped": false}""")));

        subscriber.onHonk(new FSSDiscoveryScanEvent(json("""
                {"timestamp": "2026-08-12T11:33:49Z", "event": "FSSDiscoveryScan", "SystemName": "Tascheter Sector OY-R a4-3", "SystemAddress": 58140435303200, "BodyCount": 1, "NonBodyCount": 4}""")));
        subscriber.onAllBodiesFound(new FSSAllBodiesFoundEvent(json("""
                {"timestamp": "2026-08-12T11:33:50Z", "event": "FSSAllBodiesFound", "SystemName": "Tascheter Sector OY-R a4-3", "SystemAddress": 58140435303200, "Count": 1}""")));
        subscriber.onScan(new ScanEvent(json("""
                {"timestamp": "2026-08-12T11:33:50Z", "event": "Scan", "ScanType": "Detailed", "BodyName": "Tascheter Sector OY-R a4-3", "BodyID": 0, "StarSystem": "Tascheter Sector OY-R a4-3", "SystemAddress": 58140435303200, "DistanceFromArrivalLS": 0.0, "StarType": "Y", "StellarMass": 0.03125, "WasDiscovered": true, "WasMapped": false}""")));
    }

    private static JsonObject json(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }
}
