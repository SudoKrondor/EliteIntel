package elite.intel.junit.db.managers;

import elite.intel.db.dao.ShipSettingsDao;
import elite.intel.db.managers.ShipSettingsManager;
import elite.intel.gameapi.SurfaceVehicle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Round-trips the per-ship vehicle bay configuration through the real schema, so the migration that adds
 * the four columns is exercised rather than assumed.
 *
 * <p>What the commander puts here cannot be recovered from anywhere else - the journal never says what is
 * in a hangar bay - so losing it means the deploy command silently stops working until they notice and
 * set it up again.
 */
class ShipSettingsVehicleBaysTest {

    private static final int SHIP_ID = 771_001;

    @Test
    @DisplayName("a ship that has never been configured has four empty bays")
    void defaultsToUnconfigured() {
        List<SurfaceVehicle> bays = ShipSettingsManager.getInstance().getSettings(SHIP_ID).vehicleBays();

        assertEquals(4, bays.size());
        assertTrue(bays.stream().allMatch(bay -> bay == null),
                "unset has to be distinct from Scarab, or the deploy command would guess rather than refuse");
    }

    @Test
    @DisplayName("each bay survives a reload independently")
    void persistsEveryBay() {
        ShipSettingsManager manager = ShipSettingsManager.getInstance();
        int shipId = SHIP_ID + 1;

        ShipSettingsDao.ShipSettings settings = manager.getSettings(shipId);
        settings.setVehicleBay(1, SurfaceVehicle.SCARAB);
        settings.setVehicleBay(2, SurfaceVehicle.SCORPION);
        settings.setVehicleBay(3, SurfaceVehicle.RHINO);
        manager.saveShipSettings(settings);

        List<SurfaceVehicle> reloaded = manager.getSettings(shipId).vehicleBays();
        assertEquals(SurfaceVehicle.SCARAB, reloaded.get(0));
        assertEquals(SurfaceVehicle.SCORPION, reloaded.get(1));
        assertEquals(SurfaceVehicle.RHINO, reloaded.get(2));
        assertNull(reloaded.get(3), "a bay left alone stays unset");
    }

    @Test
    @DisplayName("a bay can be cleared back to unset")
    void clearingABayPersists() {
        ShipSettingsManager manager = ShipSettingsManager.getInstance();
        int shipId = SHIP_ID + 2;

        ShipSettingsDao.ShipSettings settings = manager.getSettings(shipId);
        settings.setVehicleBay(1, SurfaceVehicle.RHINO);
        manager.saveShipSettings(settings);
        assertEquals(SurfaceVehicle.RHINO, manager.getSettings(shipId).getVehicleBay(1));

        settings.setVehicleBay(1, null);
        manager.saveShipSettings(settings);
        assertNull(manager.getSettings(shipId).getVehicleBay(1),
                "the commander has to be able to take a vehicle back out of a bay");
    }

    @Test
    @DisplayName("saving bays does not disturb the honk settings beside them")
    void doesNotClobberHonkSettings() {
        ShipSettingsManager manager = ShipSettingsManager.getInstance();
        int shipId = SHIP_ID + 3;

        ShipSettingsDao.ShipSettings settings = manager.getSettings(shipId);
        settings.setHonkFireGroup("D");
        settings.setHonkTrigger(2);
        settings.setHonkOnJump(true);
        manager.saveShipSettings(settings);

        settings.setVehicleBay(2, SurfaceVehicle.SCORPION);
        manager.saveShipSettings(settings);

        ShipSettingsDao.ShipSettings reloaded = manager.getSettings(shipId);
        assertEquals(SurfaceVehicle.SCORPION, reloaded.getVehicleBay(2));
        assertEquals("D", reloaded.getHonkFireGroup());
        assertEquals(2, reloaded.getHonkTrigger());
        assertTrue(reloaded.isHonkOnJump());
    }

    @Test
    @DisplayName("a bay name written by a later version reads as unset rather than throwing")
    void anUnknownVehicleNameReadsAsUnset() {
        ShipSettingsDao.ShipSettings settings = new ShipSettingsDao.ShipSettings();
        settings.setVehicleBay1("MOON_BUGGY_MK7");

        assertNull(settings.vehicleBays().get(0),
                "a downgrade must refuse the bay, not crash the deploy command");
    }
}
