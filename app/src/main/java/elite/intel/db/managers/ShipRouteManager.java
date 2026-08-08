package elite.intel.db.managers;

import elite.intel.db.dao.ShipRouteDao;
import elite.intel.db.util.Database;
import elite.intel.gameapi.gamestate.dtos.NavRouteDto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ShipRouteManager {


    private static volatile ShipRouteManager instance;


    private ShipRouteManager() {
    }

    public static ShipRouteManager getInstance() {
        if (instance == null) {
            synchronized (ShipRouteManager.class) {
                if (instance == null) {
                    instance = new ShipRouteManager();
                }
            }
        }
        return instance;
    }

    private static NavRouteDto routeLegToDto(ShipRouteDao.Route leg) {
        NavRouteDto dto = new NavRouteDto();
        dto.setLeg(leg.getLeg());
        dto.setRemainingJumps(leg.getRemainingJumps());
        dto.setName(leg.getSystemName());
        dto.setScoopable(leg.getScoopable());
        dto.setStarClass(leg.getStarClass());
        dto.setX(leg.getX());
        dto.setY(leg.getY());
        dto.setZ(leg.getZ());
        return dto;
    }

    public void setNavRoute(Map<Integer, NavRouteDto> routeMap) {
        Database.withDao(ShipRouteDao.class, dao -> {
            dao.clear();
            for (Integer key : routeMap.keySet()) {
                ShipRouteDao.Route leg = new ShipRouteDao.Route();
                leg.setLeg(key);
                leg.setRemainingJumps(routeMap.get(key).getRemainingJumps());
                leg.setScoopable(routeMap.get(key).isScoopable());
                leg.setStarClass(routeMap.get(key).getStarClass());
                leg.setSystemName(routeMap.get(key).getName());
                leg.setX(routeMap.get(key).getX());
                leg.setY(routeMap.get(key).getY());
                leg.setZ(routeMap.get(key).getZ());
                dao.upsert(leg);
            }
            return null;
        });
    }

    public List<NavRouteDto> removeLeg(String starSystem){
        Database.withDao(ShipRouteDao.class, dao ->{
            dao.delete(starSystem);
            return null;
        });
        return getOrderedRoute();
    }

    public List<NavRouteDto> getOrderedRoute() {
        return Database.withDao(ShipRouteDao.class, dao -> {
            List<NavRouteDto> result = new ArrayList<>();
            List<ShipRouteDao.Route> list = dao.getAll();
            for (ShipRouteDao.Route leg : list) {
                result.add(routeLegToDto(leg));
            }
            result.sort(Comparator.comparingInt(NavRouteDto::getLeg));
            return result;
        });
    }

    public void clearRoute() {
        Database.withDao(ShipRouteDao.class, dao -> {
            dao.clear();
            return null;
        });
    }

    public void updateRouteNode(NavRouteDto dto) {
        Database.withDao(ShipRouteDao.class, dao -> {
            ShipRouteDao.Route leg = new ShipRouteDao.Route();
            leg.setLeg(dto.getLeg());
            leg.setRemainingJumps(dto.getRemainingJumps());
            leg.setScoopable(dto.isScoopable());
            leg.setStarClass(dto.getStarClass());
            leg.setSystemName(dto.getName());
            leg.setX(dto.getX());
            leg.setY(dto.getY());
            leg.setZ(dto.getZ());
            dao.upsert(leg);
            return null;
        });
    }

    /**
     * The system the plotted route ends at, or null when nothing is plotted.
     * <p>
     * Legs are consumed as they are flown, so this is the destination still
     * ahead of the commander rather than the one the route was first plotted to.
     */
    public String getDestination() {
        return Database.withDao(ShipRouteDao.class, ShipRouteDao::getFinalSystemName);
    }
}
