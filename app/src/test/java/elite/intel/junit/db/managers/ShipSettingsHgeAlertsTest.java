package elite.intel.junit.db.managers;

import elite.intel.db.dao.ShipSettingsDao;
import elite.intel.db.managers.ShipSettingsManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trips the per-ship High Grade Emissions alert flag through the real schema, so the migration
 * that adds the column is exercised rather than assumed. Every material name the feature can speak is
 * already reference data, but this flag is the commander's and has to survive a save.
 */
class ShipSettingsHgeAlertsTest {

    private static final int SHIP_ID = 918_273;

    @Test
    @DisplayName("a ship that has never been configured stays quiet")
    void defaultsToOff() {
        assertFalse(ShipSettingsManager.getInstance().getSettings(SHIP_ID).isHgeAlerts());
    }

    @Test
    @DisplayName("turning the alert on survives a reload, and turning it back off does too")
    void persistsBothWays() {
        ShipSettingsManager manager = ShipSettingsManager.getInstance();

        ShipSettingsDao.ShipSettings settings = manager.getSettings(SHIP_ID + 1);
        settings.setHgeAlerts(true);
        manager.saveShipSettings(settings);
        assertTrue(manager.getSettings(SHIP_ID + 1).isHgeAlerts());

        settings.setHgeAlerts(false);
        manager.saveShipSettings(settings);
        assertFalse(manager.getSettings(SHIP_ID + 1).isHgeAlerts());
    }

    @Test
    @DisplayName("saving the alert flag does not disturb the honk settings beside it")
    void doesNotClobberHonkSettings() {
        ShipSettingsManager manager = ShipSettingsManager.getInstance();

        ShipSettingsDao.ShipSettings settings = manager.getSettings(SHIP_ID + 2);
        settings.setHonkOnJump(true);
        settings.setHonkFireGroup("C");
        settings.setHonkTrigger(2);
        settings.setHgeAlerts(true);
        manager.saveShipSettings(settings);

        ShipSettingsDao.ShipSettings reloaded = manager.getSettings(SHIP_ID + 2);
        assertTrue(reloaded.isHgeAlerts());
        assertTrue(reloaded.isHonkOnJump());
    }
}
