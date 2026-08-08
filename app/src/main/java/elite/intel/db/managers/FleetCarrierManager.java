package elite.intel.db.managers;

import elite.intel.db.dao.FleetCarrierDao;
import elite.intel.db.dao.FleetCarrierDao.FleetCarrier;
import elite.intel.db.util.Database;
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
        CarrierStatsEvent.Finance finance = event.getFinance();
        CarrierDataDto carrierData = get();
        carrierData.setCallSign(event.getCallsign());
        carrierData.setCarrierName(event.getName());
        carrierData.setCarrierType(event.getCarrierType());
        carrierData.setDockingAccess(event.getDockingAccess());
        carrierData.setAllowNotorious(event.isAllowNotorious());
        carrierData.setPendingDecommission(event.isPendingDecommission());
        carrierData.setMeasuredFuelLevel(event.getFuelLevel());

        if (event.getSpaceUsage() != null) {
            CarrierStatsEvent.SpaceUsage spaceUsage = event.getSpaceUsage();
            carrierData.setCargoSpaceUsed(spaceUsage.getCargo());
            carrierData.setCargoSpaceReserved(spaceUsage.getCargoSpaceReserved());
            carrierData.setShipRacks(spaceUsage.getShipPacks());
            carrierData.setModulePacks(spaceUsage.getModulePacks());
            carrierData.setFreeSpaceInCargo(spaceUsage.getFreeSpace());
            carrierData.setCargoCapacity(spaceUsage.getTotalCapacity());
        }

        if (finance != null) {
            carrierData.setTotalBalance(finance.getCarrierBalance());
            carrierData.setReserveBalance(finance.getReserveBalance());
            carrierData.setMarketBalance(finance.getAvailableBalance());
            carrierData.setPioneerSupplyTax(finance.getTaxRatePioneerSupplies());
            carrierData.setShipYardSupplyTax(finance.getTaxRateShipyard());
            carrierData.setRearmSupplyTax(finance.getTaxRateRearm());
            carrierData.setRepairSupplyTax(finance.getTaxRateRepair());
            carrierData.setRefuelSupplyTax(finance.getTaxRateRefuel());
        }
        save(carrierData);
    }
}
