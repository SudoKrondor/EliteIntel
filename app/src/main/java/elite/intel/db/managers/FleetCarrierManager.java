package elite.intel.db.managers;

import elite.intel.db.dao.FleetCarrierDao;
import elite.intel.db.dao.FleetCarrierDao.FleetCarrier;
import elite.intel.db.util.Database;
import elite.intel.gameapi.carrier.CarrierStatsReading;
import elite.intel.gameapi.journal.events.CarrierStatsEvent;
import elite.intel.gameapi.journal.events.dto.CarrierDataDto;
import elite.intel.util.json.GsonFactory;

public class FleetCarrierManager {

    private static FleetCarrierManager instance;

    private FleetCarrierManager() {
    }

    public static synchronized FleetCarrierManager getInstance() {
        if (instance == null) {
            instance = new FleetCarrierManager();
        }
        return instance;
    }

    public void save(CarrierDataDto data) {
        Database.withDao(FleetCarrierDao.class, dao -> {
            FleetCarrier carrier = new FleetCarrier();
            carrier.setJson(data.toJson());
            dao.save(carrier);
            return null;
        });

    }

    public CarrierDataDto get() {
        return Database.withDao(FleetCarrierDao.class, dao -> {
            FleetCarrier fleetCarrier = dao.get();
            if(fleetCarrier == null) return new CarrierDataDto();
            return GsonFactory.getGson().fromJson(fleetCarrier.getJson(), CarrierDataDto.class);
        });
    }


    public void setCarrierStats(CarrierStatsEvent event) {
        CarrierDataDto carrierData = get();
        CarrierStatsReading.applyTo(carrierData, event);
        save(carrierData);
    }
}
