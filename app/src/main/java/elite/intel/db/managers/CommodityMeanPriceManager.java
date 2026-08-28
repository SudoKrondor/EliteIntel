package elite.intel.db.managers;

import elite.intel.db.dao.CommodityMeanPriceDao;
import elite.intel.db.util.Database;
import elite.intel.gameapi.JournalSymbol;
import elite.intel.gameapi.gamestate.dtos.GameEvents;

import java.util.OptionalInt;

/**
 * What a commodity is worth on average across the galaxy, learned from the markets the commander stands in.
 * <p>
 * <b>Why this matters.</b> A price on its own says nothing: 57,844 credits a tonne for Tritium is either an
 * excellent sale or a poor one, and only the galactic average of 51,294 settles it. The game puts that
 * figure on every line of every {@code Market.json} and the app had been storing those files for other
 * reasons without ever reading it.
 * <p>
 * <b>Why it is not the whole answer.</b> Whether a sale is a PROFIT depends on what the cargo cost, and the
 * journal does not say - {@code CargoTransfer}, which is how a carrier owner loads most of what they sell,
 * carries no price at all. The galactic average is the honest half of the question: it says whether the
 * market is good, not whether the trade is.
 */
public final class CommodityMeanPriceManager {

    private static final CommodityMeanPriceManager INSTANCE = new CommodityMeanPriceManager();

    private CommodityMeanPriceManager() {
    }

    public static CommodityMeanPriceManager getInstance() {
        return INSTANCE;
    }

    /**
     * Records every average on a market board the commander has just opened.
     * <p>
     * Called from the one place every {@code Market.json} passes through, so the table fills itself as they
     * fly. Goods the board reports with no average - fleet carriers report none - are skipped, because zero
     * is not an average and storing it would answer a later question wrongly.
     */
    public void harvest(GameEvents.MarketEvent market) {
        if (market == null || market.getItems() == null || market.getItems().isEmpty()) return;
        String seenAt = market.getTimestamp();
        Database.withDao(CommodityMeanPriceDao.class, dao -> {
            for (GameEvents.MarketEvent.MarketItem item : market.getItems()) {
                String symbol = JournalSymbol.normalize(item.getName());
                if (symbol == null || item.getMeanPrice() <= 0) continue;
                dao.upsert(symbol, item.getMeanPrice(), seenAt);
            }
            return null;
        });
    }

    /**
     * The galactic average for a good, or empty when no market the commander has opened has listed it.
     *
     * @param commoditySymbol the bare journal symbol, as {@link JournalSymbol} normalises it
     */
    public OptionalInt meanPrice(String commoditySymbol) {
        if (commoditySymbol == null || commoditySymbol.isBlank()) return OptionalInt.empty();
        Integer mean = Database.withDao(CommodityMeanPriceDao.class,
                dao -> dao.meanPrice(JournalSymbol.normalize(commoditySymbol)));
        return mean == null || mean <= 0 ? OptionalInt.empty() : OptionalInt.of(mean);
    }

    /**
     * How many goods we know an average for; for diagnostics and tests.
     */
    public int known() {
        return Database.withDao(CommodityMeanPriceDao.class, CommodityMeanPriceDao::count);
    }
}
