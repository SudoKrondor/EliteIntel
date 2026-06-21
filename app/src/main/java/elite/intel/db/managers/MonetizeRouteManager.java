package elite.intel.db.managers;

import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.db.dao.RouteMonetisationDao;
import elite.intel.db.dao.RouteMonetisationDao.MonetisationTransaction;
import elite.intel.db.util.Database;
import elite.intel.eventbus.GameEventBus;
import elite.intel.search.edsm.monetize.MonetizeRoute;

public class MonetizeRouteManager {

    private static volatile MonetizeRouteManager instance;

    private MonetizeRouteManager() {
    }

    public static MonetizeRouteManager getInstance() {
        if (instance == null) {
            synchronized (MonetizeRouteManager.class) {
                if (instance == null) {
                    instance = new MonetizeRouteManager();
                }
            }
        }
        return instance;
    }

    public MonetizeRoute.TradeTransaction monetizeRoute() {

        ShipRouteManager shipRouteManager = ShipRouteManager.getInstance();
        MonetizeRoute.TradeTransaction tradeTuple = MonetizeRoute.findTrade(shipRouteManager.getOrderedRoute());
        if (tradeTuple == null) {
            GameEventBus.publish(new MissionCriticalAnnouncementEvent("No trade found"));
            return null;
        }

        Database.withDao(RouteMonetisationDao.class, dao->{
            MonetisationTransaction entity = new MonetisationTransaction();

            entity.setSourceCommodity(tradeTuple.getSource().getCommodity());
            entity.setSourceStationName(tradeTuple.getSource().getStationName());
            entity.setSourceStarSystem(tradeTuple.getSource().getStarSystem());
            entity.setSourceSupply(tradeTuple.getSource().getSupply());
            entity.setSourceStationType(tradeTuple.getSource().getStationType());
            entity.setSourceBuyPrice(tradeTuple.getSource().getBuyPrice());


            entity.setDestinationCommodity(tradeTuple.getDestination().getCommodity());
            entity.setDestinationDemand(tradeTuple.getDestination().getDemand());
            entity.setDestinationStarSystem(tradeTuple.getDestination().getStarSystem());
            entity.setDestinationStationName(tradeTuple.getDestination().getStationName());
            entity.setDestinationStationType(tradeTuple.getDestination().getStationType());
            entity.setDestinationSellPrice(tradeTuple.getDestination().getSellPrice());

            dao.save(entity);
            return Void.class;
        });

        return tradeTuple;
    }

    public boolean isSeller(String starSystem) {
        return Database.withDao(RouteMonetisationDao.class, dao->{
            MonetisationTransaction entity = dao.get();
            if(entity == null) return false;
            return entity.getSourceStarSystem().equals(starSystem);
        });
    }

    public boolean isBuyer(String starSystem) {
        return Database.withDao(RouteMonetisationDao.class, dao->{
            MonetisationTransaction entity = dao.get();
            if(entity == null) return false;
            return entity.getDestinationStarSystem().equals(starSystem);
        });
    }

    public MonetisationTransaction getTransaction() {
        return Database.withDao(RouteMonetisationDao.class, RouteMonetisationDao::get);
    }

    public void clear() {
        Database.withDao(RouteMonetisationDao.class, dao->{
            dao.clear();
            return Void.class;
        });
    }
}
