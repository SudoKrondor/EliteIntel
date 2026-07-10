package elite.intel.db.managers;

import elite.intel.db.dao.FleetCarrierRouteDao;
import elite.intel.db.util.Database;
import elite.intel.search.spansh.carrierroute.CarrierJump;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FleetCarrierRouteManager {

    private static volatile FleetCarrierRouteManager instance;

    private FleetCarrierRouteManager() {
        // enforce singleton pattern.
    }

    public static FleetCarrierRouteManager getInstance() {
        if (instance == null) {
            synchronized (FleetCarrierRouteManager.class) {
                if (instance == null) {
                    instance = new FleetCarrierRouteManager();
                }
            }
        }
        return instance;
    }


    public void setFleetCarrierRoute(Map<Integer, CarrierJump> fleetCarrierRoute) {
        if (fleetCarrierRoute == null || fleetCarrierRoute.isEmpty()) return;
        clear();
        Database.withDao(FleetCarrierRouteDao.class, dao -> {

            Set<Integer> keys = fleetCarrierRoute.keySet();
            for (Integer key : keys) {
                FleetCarrierRouteDao.FleetCarrierRouteLeg leg = new FleetCarrierRouteDao.FleetCarrierRouteLeg();
                leg.setLeg(key);
                leg.setDistance(fleetCarrierRoute.get(key).getDistance());
                leg.setFuelUsed(fleetCarrierRoute.get(key).getFuelUsed());
                leg.setHasIcyRing(fleetCarrierRoute.get(key).getHasIcyRing());
                leg.setPristine(fleetCarrierRoute.get(key).isPristine());
                leg.setRemainingFuel(fleetCarrierRoute.get(key).getRemainingFuel());
                leg.setSystemName(fleetCarrierRoute.get(key).getSystemName());
                leg.setX(fleetCarrierRoute.get(key).getX());
                leg.setY(fleetCarrierRoute.get(key).getY());
                leg.setZ(fleetCarrierRoute.get(key).getZ());
                dao.save(leg);
            }
            return null;
        });

    }

    public Map<Integer, CarrierJump> getFleetCarrierRoute() {
        return Database.withDao(FleetCarrierRouteDao.class, dao -> {
            Map<Integer, CarrierJump> result = new HashMap<>();
            List<FleetCarrierRouteDao.FleetCarrierRouteLeg> all = dao.getAll();
            for (FleetCarrierRouteDao.FleetCarrierRouteLeg leg : all) {
                CarrierJump jump = entityToDto(leg);
                result.put(leg.getLeg(), jump);
            }
            return result;
        });
    }

    public Integer  getTotalFuelRequired() {
        return Database.withDao(FleetCarrierRouteDao.class, dao -> dao.getTotalFuelRequired());
    }

    private static CarrierJump entityToDto(FleetCarrierRouteDao.FleetCarrierRouteLeg leg) {
        CarrierJump jump = new CarrierJump();
        jump.setLeg(leg.getLeg());
        jump.setDistance(leg.getDistance());
        jump.setFuelUsed(leg.getFuelUsed());
        jump.setHasIcyRing(leg.getHasIcyRing());
        jump.setPristine(leg.getPristine());
        jump.setRemainingFuel(leg.getRemainingFuel());
        jump.setSystemName(leg.getSystemName());
        jump.setX(leg.getX());
        jump.setY(leg.getY());
        jump.setZ(leg.getZ());
        return jump;
    }

    public void removeLeg(String starName) {
        Database.withDao(FleetCarrierRouteDao.class, dao -> {
                    dao.delete(starName);
                    return null;
                }
        );
    }

    public void clear() {
        Database.withDao(FleetCarrierRouteDao.class, dao -> {
            dao.clear();
            return null;
        });
    }

    /**
     * The plotted leg that ends at the given system, or null when the system is not on the route.
     *
     * <p>WHY: this used to answer with an empty CarrierJump instead of null, which silently made
     * every {@code == null} check at the call sites unreachable. Callers could not tell "arrived at
     * a plotted leg" from "arrived somewhere off-route", and read fuelUsed 0 and coordinates 0,0,0
     * from the placeholder.
     */
    public CarrierJump findByPrimaryStar(String starSystem) {
        return Database.withDao(FleetCarrierRouteDao.class, dao ->{
            FleetCarrierRouteDao.FleetCarrierRouteLeg leg = dao.findByPrimaryStarName(starSystem);
            return leg == null ? null : entityToDto(leg);
        });
    }

    public String getFinalDestination() {
        return Database.withDao(FleetCarrierRouteDao.class, dao-> dao.getDestinationSystemName());
    }
}
