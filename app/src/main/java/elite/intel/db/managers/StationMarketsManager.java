package elite.intel.db.managers;

import elite.intel.db.dao.StationMarketDao;
import elite.intel.db.util.Database;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.util.json.GsonFactory;

import java.util.List;

public class StationMarketsManager {
    private static final StationMarketsManager INSTANCE = new StationMarketsManager();

    private StationMarketsManager() {
    }

    public static StationMarketsManager getInstance() {
        return INSTANCE;
    }

    public void save(GameEvents.MarketEvent market) {
        Database.withDao(StationMarketDao.class, dao -> {
            StationMarketDao.StationMarket stationMarket = new StationMarketDao.StationMarket();
            stationMarket.setJson(market.toJson());
            stationMarket.setStationName(market.getStationName());
            stationMarket.setMarketId(market.getMarketID());
            dao.upsert(stationMarket);
            return null;
        });
    }

    public void clear() {
        Database.withDao(StationMarketDao.class, dao -> {
            dao.clear();
            return null;
        });
    }

    public List<GameEvents.MarketEvent> listAll() {
        return Database.withDao(StationMarketDao.class, dao -> {
            StationMarketDao.StationMarket[] all = dao.listAll();
            List<GameEvents.MarketEvent> result = new java.util.ArrayList<>();
            for (StationMarketDao.StationMarket entity : all) {
                result.add(GsonFactory.getGson().fromJson(entity.getJson(), GameEvents.MarketEvent.class));
            }
            return result;
        });
    }
}
