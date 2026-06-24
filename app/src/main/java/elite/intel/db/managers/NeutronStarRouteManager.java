package elite.intel.db.managers;

import elite.intel.db.dao.NeutronStarRouteDao;
import elite.intel.db.util.Database;
import elite.intel.search.spansh.neutronroute.NeutronStarRoute;
import elite.intel.search.spansh.neutronroute.NeutronStarSystemJump;

import java.util.List;

public class NeutronStarRouteManager {

    private static volatile NeutronStarRouteManager instance;

    private NeutronStarRouteManager() {
    }

    public static NeutronStarRouteManager getInstance() {
        if (instance == null) {
            synchronized (NeutronStarRouteManager.class) {
                if (instance == null) {
                    instance = new NeutronStarRouteManager();
                }
            }
        }
        return instance;
    }

    public void saveNeutronStarRoute(NeutronStarRoute route) {
        if (route == null || route.getResult() == null) return;
        List<NeutronStarSystemJump> jumps = route.getResult().getSystemJumps();
        if (jumps == null || jumps.isEmpty()) return;

        clear();
        Database.withDao(NeutronStarRouteDao.class, dao -> {
            int legIndex = 1;
            for (int i = 1; i < jumps.size(); i++) { // skip index 0 — we are already there
                NeutronStarSystemJump jump = jumps.get(i);
                NeutronStarRouteDao.Route.Leg leg = new NeutronStarRouteDao.Route.Leg();
                leg.setLeg(legIndex++);
                leg.setSystemAddress(jump.getSystemAddress());
                leg.setSystemName(jump.getSystem());
                leg.setDistanceJumped(jump.getDistanceJumped());
                leg.setDistanceLeft(jump.getDistanceLeft());
                leg.setJumps(jump.getJumps());
                leg.setNeutronStar(jump.isNeutronStar());
                leg.setX(jump.getX());
                leg.setY(jump.getY());
                leg.setZ(jump.getZ());
                dao.save(leg);
            }
            return null;
        });
    }

    public NeutronStarRouteDao.Route getNeutronStarRoute() {
        return Database.withDao(NeutronStarRouteDao.class, dao -> {
            List<NeutronStarRouteDao.Route.Leg> legs = dao.getAll();
            return new NeutronStarRouteDao.Route(legs);
        });
    }

    public void removeLeg(long systemAddress) {
        Database.withDao(NeutronStarRouteDao.class, dao -> {
            dao.deleteBySystemAddress(systemAddress);
            return null;
        });
    }

    public void clear() {
        Database.withDao(NeutronStarRouteDao.class, dao -> {
            dao.clear();
            return null;
        });
    }
}
