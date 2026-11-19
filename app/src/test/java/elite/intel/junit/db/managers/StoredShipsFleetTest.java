package elite.intel.junit.db.managers;

import com.google.gson.JsonParser;
import elite.intel.db.dao.ShipDao;
import elite.intel.db.managers.ShipManager;
import elite.intel.gameapi.journal.events.StoredShipsEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The fleet heals from the game's list of stored ships instead of waiting for each one to be flown.
 */
class StoredShipsFleetTest {

    private static final int BASE = 913_400;

    @Test
    @DisplayName("StoredShips reads the ships here and the ships remote, and drops a ship with no type")
    void parsesBothLists() {
        StoredShipsEvent event = new StoredShipsEvent(JsonParser.parseString("""
                { "timestamp":"2026-11-19T03:06:21Z", "event":"StoredShips", "StationName":"GHY-L8X",
                  "ShipsHere":[ { "ShipID":38, "ShipType":"type6", "ShipType_Localised":"Type-6 Transporter", "Value":0, "Hot":false },
                                { "ShipID":31, "ShipType":"federation_corvette", "Name":"Wraith", "Value":810764259, "Hot":false } ],
                  "ShipsRemote":[ { "ShipID":41, "ShipType":"anacondamkii", "Name":"RAVEN", "InTransit":true },
                                  { "ShipID":99 } ] }
                """).getAsJsonObject());

        List<StoredShipsEvent.StoredShip> ships = event.allShips();

        assertEquals(List.of(38, 31, 41), ships.stream().map(StoredShipsEvent.StoredShip::getShipId).toList());
        assertNull(ships.getFirst().getName());
        assertEquals("Wraith", ships.get(1).getName());
        assertEquals("anacondamkii", ships.get(2).getShipType());
    }

    @Test
    @DisplayName("a ship the fleet lacks is added, and a known ship keeps its voice, personality and name")
    void addsMissingAndLeavesKnownAlone() {
        ShipManager ships = ShipManager.getInstance();
        ships.save(BASE, "Old Name", 120, "cutter", "ADAM", "Cmdr");
        ShipDao.Ship known = ships.getShipById(BASE);
        known.setPersonality("CASUAL");
        ships.saveShip(known);

        int added = ships.addMissingShips(List.of(
                new ShipManager.StoredShip(BASE, "New Name", "cutter"),
                new ShipManager.StoredShip(BASE + 1, "Wraith", "federation_corvette")), "BELLA", "Cmdr");

        assertEquals(1, added);
        ShipDao.Ship kept = ships.getShipById(BASE);
        assertEquals("Old Name", kept.getShipName());
        assertEquals("ADAM", kept.getVoice());
        assertEquals("CASUAL", kept.getPersonality());
        assertEquals(120, kept.getCargoCapacity());

        ShipDao.Ship restored = ships.getShipById(BASE + 1);
        assertEquals("Wraith", restored.getShipName());
        assertEquals("federation_corvette", restored.getShipIdentifier());
        assertEquals("BELLA", restored.getVoice());
        assertEquals("Cmdr", restored.getCommanderName());
        assertTrue(ships.getShipsForCommander("Cmdr").stream().anyMatch(s -> s.getShipId() == BASE + 1));
    }

    @Test
    @DisplayName("a second StoredShips adds nothing")
    void repeatIsANoOp() {
        ShipManager ships = ShipManager.getInstance();
        List<ShipManager.StoredShip> list = List.of(new ShipManager.StoredShip(BASE + 10, "Hawk", "smallcombat01_nx"));
        assertEquals(1, ships.addMissingShips(list, "BELLA", "Cmdr"));
        assertEquals(0, ships.addMissingShips(list, "BELLA", "Cmdr"));
    }
}
