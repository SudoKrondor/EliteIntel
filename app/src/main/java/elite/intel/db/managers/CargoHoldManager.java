package elite.intel.db.managers;

import elite.intel.db.dao.CargoDao;
import elite.intel.db.util.Database;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.util.json.GsonFactory;

public class CargoHoldManager {
    private static CargoHoldManager instance;

    private CargoHoldManager() {
    }

    public static synchronized CargoHoldManager getInstance() {
        if (instance == null) {
            instance = new CargoHoldManager();
        }
        return instance;
    }


    public void save(GameEvents.CargoEvent event) {
        Database.withDao(CargoDao.class, dao ->{
            CargoDao.Cargo data = new CargoDao.Cargo();
            data.setJson(event.toJson());
            dao.save(data);
            return null;
        });
    }

    public GameEvents.CargoEvent get() {
        return Database.withDao(CargoDao.class, dao -> {
            CargoDao.Cargo cargo = dao.get();
            if(cargo == null) return new GameEvents.CargoEvent();
            return GsonFactory.getGson().fromJson(cargo.getJson(), GameEvents.CargoEvent.class);
        });
    }

    /**
     * Records one limpet leaving the hold, and reports whether one actually did.
     * <p>
     * Read-modify-write against the stored snapshot, kept in one place so the read and the
     * write cannot drift apart. A {@code Cargo} event landing in the middle of it loses this
     * one decrement - a self-correcting miss, because that same event carries the game's own
     * count and is the more truthful of the two.
     */
    public boolean recordDroneLaunched() {
        return Database.withDao(CargoDao.class, dao -> {
            CargoDao.Cargo stored = dao.get();
            if (stored == null) return false;
            GameEvents.CargoEvent cargo =
                    GsonFactory.getGson().fromJson(stored.getJson(), GameEvents.CargoEvent.class);
            if (cargo == null || !cargo.launchDrone()) return false;
            CargoDao.Cargo data = new CargoDao.Cargo();
            data.setJson(cargo.toJson());
            dao.save(data);
            return true;
        });
    }
}
