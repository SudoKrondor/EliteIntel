package elite.intel.db.managers;

import elite.intel.db.dao.ShipScansDao;
import elite.intel.db.util.Database;

public class ShipScansManager {
    private static ShipScansManager instance;

    private ShipScansManager() {
    }

    public static synchronized ShipScansManager getInstance() {
        if (instance == null) {
            instance = new ShipScansManager();
        }
        return instance;
    }


    public void saveScan(ShipScansDao.ShipScan data) {
        Database.withDao(ShipScansDao.class, dao -> {
            dao.upsert(data);
            return null;
        });
    }


    public String get(String key) {
        return Database.withDao(ShipScansDao.class, dao -> dao.get(key));
    }

    public void remove(String key) {
        Database.withDao(ShipScansDao.class, dao -> {
            dao.remove(key);
            return null;
        });
    }

    public void clear() {
        Database.withDao(ShipScansDao.class, dao ->{
            dao.clear();
            return null;
        });
    }
}
