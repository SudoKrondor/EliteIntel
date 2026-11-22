package elite.intel.junit.db.managers;

import elite.intel.db.dao.ShipDao;
import elite.intel.db.dao.ShipSettingsDao;
import elite.intel.db.managers.ShipManager;
import elite.intel.db.managers.ShipSettingsManager;
import elite.intel.db.util.Database;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A sold ship leaves the fleet but keeps its row and settings, so a sale the game later contradicts costs the
 * commander nothing. Runs through the real schema.
 */
class ShipSoldTest {

    private static final int SHIP_ID = 827_364;
    private static final String SOLD_AT = "2026-11-18T20:00:00Z";

    @Test
    @DisplayName("selling a ship takes it off the fleet, keeps its row and settings, and leaves the rest alone")
    void soldShipLeavesTheFleet() {
        ShipManager ships = ShipManager.getInstance();
        ships.save(SHIP_ID, "Sold", 0, "typex", "EMMA", "Test");
        ships.save(SHIP_ID + 1, "Kept", 0, "cobra3", "EMMA", "Test");
        ShipSettingsManager.getInstance().getSettings(SHIP_ID);

        assertTrue(ships.markSold(SHIP_ID, SOLD_AT));

        assertFalse(fleetHas(ships.getAllShips(), SHIP_ID));
        assertFalse(fleetHas(ships.getShipsForCommander("Test"), SHIP_ID));
        assertTrue(fleetHas(ships.getShipsForCommander("Test"), SHIP_ID + 1));
        assertNotNull(ships.getShipById(SHIP_ID));
        assertNotNull(Database.withDao(ShipSettingsDao.class, dao -> dao.getShipSettings(SHIP_ID)));
    }

    @Test
    @DisplayName("a ship the game names again comes back with the voice and personality it had")
    void soldShipNamedAgainComesBack() {
        ShipManager ships = ShipManager.getInstance();
        ships.save(SHIP_ID + 2, "Wraith", 272, "federation_corvette", "NOVA", "Test");
        ships.markSold(SHIP_ID + 2, SOLD_AT);

        assertTrue(ships.inFleet(SHIP_ID + 2));

        ShipDao.Ship back = ships.getShipById(SHIP_ID + 2);
        assertTrue(fleetHas(ships.getAllShips(), SHIP_ID + 2));
        assertEquals("NOVA", back.getVoice());
        assertEquals(272, back.getCargoCapacity());
        assertFalse(ships.inFleet(SHIP_ID + 2), "a ship already in the fleet reports no change");
    }

    @Test
    @DisplayName("StoredShips brings a ship marked sold back without touching its row")
    void storedShipsBringsSoldShipBack() {
        ShipManager ships = ShipManager.getInstance();
        ships.save(SHIP_ID + 3, "Hawk", 0, "smallcombat01_nx", "SARAH", "Test");
        ships.markSold(SHIP_ID + 3, SOLD_AT);

        int changed = ships.addMissingShips(
                List.of(new ShipManager.StoredShip(SHIP_ID + 3, "Renamed", "smallcombat01_nx")), "BELLA", "Test");

        assertEquals(1, changed);
        ShipDao.Ship back = ships.getShipById(SHIP_ID + 3);
        assertTrue(fleetHas(ships.getAllShips(), SHIP_ID + 3));
        assertEquals("Hawk", back.getShipName());
        assertEquals("SARAH", back.getVoice());
    }

    @Test
    @DisplayName("selling a ship the fleet never knew, or selling it twice, changes nothing")
    void unknownOrRepeatedSaleIsANoOp() {
        ShipManager ships = ShipManager.getInstance();
        assertFalse(ships.markSold(SHIP_ID + 100, SOLD_AT));

        ships.save(SHIP_ID + 4, "Twice", 0, "sidewinder", "EMMA", "Test");
        assertTrue(ships.markSold(SHIP_ID + 4, SOLD_AT));
        assertFalse(ships.markSold(SHIP_ID + 4, "2026-11-19T00:00:00Z"));
    }

    @Test
    @DisplayName("an engine switch resets the voice of sold ships too")
    void voiceResetReachesSoldShips() {
        ShipManager ships = ShipManager.getInstance();
        ships.save(SHIP_ID + 5, "Parked", 0, "asp", "OLD_VOICE", "Test");
        ships.markSold(SHIP_ID + 5, SOLD_AT);

        ships.resetAllVoicesToDefault("NEW_VOICE");

        assertEquals("NEW_VOICE", ships.getShipById(SHIP_ID + 5).getVoice());
    }

    private static boolean fleetHas(List<ShipDao.Ship> fleet, int shipId) {
        return fleet.stream().anyMatch(s -> s.getShipId() == shipId);
    }
}
