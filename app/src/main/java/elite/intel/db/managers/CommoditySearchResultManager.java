package elite.intel.db.managers;

import elite.intel.db.dao.CommoditySearchResultDao;
import elite.intel.db.dao.CommoditySearchResultDao.FoundLine;
import elite.intel.db.dao.CommoditySearchResultDao.FoundMarket;
import elite.intel.db.util.Database;

import java.util.List;

/**
 * Where the last commodity search told the commander to go, kept so the HUD overlay can show it.
 * <p>
 * The overlay's rule is derive-never-remember: a source recomputes its card from persisted state on every
 * poll, because the app is restarted mid-trip all the time. A result that lived only in the search's local
 * variable could not be drawn at all.
 */
public final class CommoditySearchResultManager {

    private static final CommoditySearchResultManager INSTANCE = new CommoditySearchResultManager();

    private CommoditySearchResultManager() {
    }

    public static CommoditySearchResultManager getInstance() {
        return INSTANCE;
    }

    public FoundMarket get() {
        return Database.withDao(CommoditySearchResultDao.class, CommoditySearchResultDao::get);
    }

    /**
     * Everything to load at the market that was found, anchor first. Empty when nothing has been searched
     * for yet.
     */
    public List<FoundLine> lines() {
        return Database.withDao(CommoditySearchResultDao.class, CommoditySearchResultDao::lines);
    }

    public void save(FoundMarket market, List<FoundLine> lines) {
        if (market == null || market.getCommodity() == null || market.getCommodity().isBlank()) return;
        Database.withDao(CommoditySearchResultDao.class, dao -> {
            dao.replaceResult(market, lines);
            return null;
        });
    }

    public void clear() {
        Database.withDao(CommoditySearchResultDao.class, dao -> {
            dao.clear();
            return null;
        });
    }
}
