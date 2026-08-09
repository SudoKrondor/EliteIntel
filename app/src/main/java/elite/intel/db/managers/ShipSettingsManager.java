package elite.intel.db.managers;

import elite.intel.db.dao.ShipSettingsDao;
import elite.intel.db.util.Database;

public class ShipSettingsManager {
    private static ShipSettingsManager instance;

    private ShipSettingsManager() {
    }

    public static synchronized ShipSettingsManager getInstance() {
        if (instance == null) {
            instance = new ShipSettingsManager();
        }
        return instance;
    }

    public ShipSettingsDao.ShipSettings getSettings(int shipId) {
        return Database.withDao(ShipSettingsDao.class, dao -> {
            ShipSettingsDao.ShipSettings shipSettings = dao.getShipSettings(shipId);
            if (shipSettings == null) {
                shipSettings = new ShipSettingsDao.ShipSettings();
                shipSettings.setShipId(shipId);
                shipSettings.setHonkTrigger(1);
                shipSettings.setHonkFireGroup("A");
                shipSettings.setHonkOnJump(false);
                shipSettings.setHgeAlerts(false);
                dao.save(shipSettings);
            }
            return shipSettings;
        });
    }

    public void saveShipSettings(ShipSettingsDao.ShipSettings settings) {
        Database.withDao(ShipSettingsDao.class, dao -> {
            dao.save(settings);
            return Void.TYPE;
        });
    }
}
