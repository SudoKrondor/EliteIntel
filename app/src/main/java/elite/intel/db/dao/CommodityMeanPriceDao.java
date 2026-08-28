package elite.intel.db.dao;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/**
 * The galactic average price of a commodity, keyed by its bare journal symbol.
 * <p>
 * One row per good, not per station: MeanPrice is the GALACTIC average, the same number at every market
 * that lists it, so a sighting anywhere teaches it everywhere.
 */
public interface CommodityMeanPriceDao {

    /**
     * The newest sighting wins. The figure is a galaxy-wide constant, so a later look is only ever a
     * confirmation - except on the rare occasion Frontier moves one, and then the newer number is right.
     */
    @SqlUpdate("""
            INSERT INTO commodity_mean_price (symbol, meanPrice, seenAt)
            VALUES (:symbol, :meanPrice, :seenAt)
            ON CONFLICT(symbol) DO UPDATE SET meanPrice = excluded.meanPrice, seenAt = excluded.seenAt
            """)
    void upsert(@Bind("symbol") String symbol, @Bind("meanPrice") int meanPrice, @Bind("seenAt") String seenAt);

    /**
     * Null when no market the commander has opened has ever listed the good.
     */
    @SqlQuery("SELECT meanPrice FROM commodity_mean_price WHERE symbol = :symbol")
    Integer meanPrice(@Bind("symbol") String symbol);

    @SqlQuery("SELECT COUNT(*) FROM commodity_mean_price")
    int count();
}
