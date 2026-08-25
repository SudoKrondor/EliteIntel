package elite.intel.db.managers;

import elite.intel.db.dao.SquadronCarrierDao;
import elite.intel.db.dao.SquadronCarrierDao.SquadronCarrier;
import elite.intel.db.util.Database;
import elite.intel.gameapi.carrier.CarrierStatsReading;
import elite.intel.gameapi.journal.events.CarrierStatsEvent;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.util.json.GsonFactory;

public class SquadronCarrierManager {

    private static SquadronCarrierManager instance;

    private SquadronCarrierManager() {
    }

    public static synchronized SquadronCarrierManager getInstance() {
        if (instance == null) {
            instance = new SquadronCarrierManager();
        }
        return instance;
    }

    public void save(CarrierDataDto data) {
        Database.withDao(SquadronCarrierDao.class, dao -> {
            SquadronCarrier carrier = new SquadronCarrier();
            carrier.setJson(data.toJson());
            dao.save(carrier);
            return null;
        });
    }

    public void setCarrierStats(CarrierStatsEvent event) {
        CarrierDataDto carrierData = get();
        CarrierStatsReading.applyTo(carrierData, event);
        save(carrierData);
    }

    public CarrierDataDto get() {
        return Database.withDao(SquadronCarrierDao.class, dao -> {
            SquadronCarrier carrier = dao.get();
            if (carrier == null) return new CarrierDataDto();
            return GsonFactory.getGson().fromJson(carrier.getJson(), CarrierDataDto.class);
        });
    }
}
