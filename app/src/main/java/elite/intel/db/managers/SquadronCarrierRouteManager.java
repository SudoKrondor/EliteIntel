package elite.intel.db.managers;

import elite.intel.db.dao.SquadronCarrierRouteDao;
import elite.intel.db.util.Database;
import elite.intel.search.spansh.carrierroute.CarrierJump;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SquadronCarrierRouteManager {

    private static volatile SquadronCarrierRouteManager instance;

    private SquadronCarrierRouteManager() {
        // enforce singleton pattern.
    }

    public static SquadronCarrierRouteManager getInstance() {
        if (instance == null) {
            synchronized (SquadronCarrierRouteManager.class) {
                if (instance == null) {
                    instance = new SquadronCarrierRouteManager();
                }
            }
        }
        return instance;
    }

    public void setSquadronCarrierRoute(Map<Integer, CarrierJump> squadronCarrierRoute) {
        if (squadronCarrierRoute == null || squadronCarrierRoute.isEmpty()) return;
        clear();
        Database.withDao(SquadronCarrierRouteDao.class, dao -> {
            Set<Integer> keys = squadronCarrierRoute.keySet();
            for (Integer key : keys) {
                SquadronCarrierRouteDao.SquadronCarrierRouteLeg leg = new SquadronCarrierRouteDao.SquadronCarrierRouteLeg();
                leg.setLeg(key);
                leg.setDistance(squadronCarrierRoute.get(key).getDistance());
                leg.setFuelUsed(squadronCarrierRoute.get(key).getFuelUsed());
                leg.setHasIcyRing(squadronCarrierRoute.get(key).getHasIcyRing());
                leg.setPristine(squadronCarrierRoute.get(key).isPristine());
                leg.setRemainingFuel(squadronCarrierRoute.get(key).getRemainingFuel());
                leg.setSystemName(squadronCarrierRoute.get(key).getSystemName());
                leg.setX(squadronCarrierRoute.get(key).getX());
                leg.setY(squadronCarrierRoute.get(key).getY());
                leg.setZ(squadronCarrierRoute.get(key).getZ());
                dao.save(leg);
            }
            return null;
        });
    }

    public Map<Integer, CarrierJump> getSquadronCarrierRoute() {
        return Database.withDao(SquadronCarrierRouteDao.class, dao -> {
            Map<Integer, CarrierJump> result = new HashMap<>();
            List<SquadronCarrierRouteDao.SquadronCarrierRouteLeg> all = dao.getAll();
            for (SquadronCarrierRouteDao.SquadronCarrierRouteLeg leg : all) {
                CarrierJump jump = entityToDto(leg);
                result.put(leg.getLeg(), jump);
            }
            return result;
        });
    }

    public Integer getTotalFuelRequired() {
        return Database.withDao(SquadronCarrierRouteDao.class, dao -> dao.getTotalFuelRequired());
    }

    public void removeLeg(String starName) {
        Database.withDao(SquadronCarrierRouteDao.class, dao -> {
            dao.delete(starName);
            return null;
        });
    }

    public void clear() {
        Database.withDao(SquadronCarrierRouteDao.class, dao -> {
            dao.clear();
            return null;
        });
    }

    /**
     * The plotted leg that ends at the given system, or null when the system is not on the route.
     */
    public CarrierJump findByPrimaryStar(String starSystem) {
        return Database.withDao(SquadronCarrierRouteDao.class, dao -> {
            SquadronCarrierRouteDao.SquadronCarrierRouteLeg leg = dao.findByPrimaryStarName(starSystem);
            return leg == null ? null : entityToDto(leg);
        });
    }

    public String getFinalDestination() {
        return Database.withDao(SquadronCarrierRouteDao.class, dao -> dao.getDestinationSystemName());
    }

    private static CarrierJump entityToDto(SquadronCarrierRouteDao.SquadronCarrierRouteLeg leg) {
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
}
