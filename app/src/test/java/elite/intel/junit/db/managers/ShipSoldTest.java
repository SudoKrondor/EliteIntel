package elite.intel.junit.db.managers;

import elite.intel.db.dao.ShipSettingsDao;
import elite.intel.db.managers.ShipManager;
import elite.intel.db.managers.ShipSettingsManager;
import elite.intel.db.util.Database;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A sold ship leaves the fleet through the real schema, so the cascade onto its settings is exercised
 * rather than assumed.
 */
class ShipSoldTest {

    private static final int SHIP_ID = 827_364;

    @Test
    @DisplayName("selling a ship removes it and its settings, and leaves the rest of the fleet alone")
    void soldShipIsForgotten() {
        ShipManager ships = ShipManager.getInstance();
        ships.save(SHIP_ID, "Sold", 0, "typex", "EMMA", "Test");
        ships.save(SHIP_ID + 1, "Kept", 0, "cobra3", "EMMA", "Test");
        ShipSettingsManager.getInstance().getSettings(SHIP_ID);

        assertTrue(ships.forgetSoldShip(SHIP_ID));

        assertNull(ships.getShipById(SHIP_ID));
        assertNull(Database.withDao(ShipSettingsDao.class, dao -> dao.getShipSettings(SHIP_ID)));
        assertNotNull(ships.getShipById(SHIP_ID + 1));
    }

    @Test
    @DisplayName("selling a ship the fleet never knew changes nothing")
    void unknownShipIsANoOp() {
        assertFalse(ShipManager.getInstance().forgetSoldShip(SHIP_ID + 100));
    }
}
