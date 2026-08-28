package elite.intel.db.dao;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * The market a commodity search last sent the commander to.
 * <p>
 * Single row, like {@code destination_reminder}: only the most recent search is of interest, and each
 * new one replaces it.
 */
@RegisterRowMapper(CommoditySearchResultDao.FoundMarketMapper.class)
@RegisterRowMapper(CommoditySearchResultDao.FoundLineMapper.class)
public interface CommoditySearchResultDao {

    @SqlUpdate("""
            INSERT OR REPLACE INTO commodity_search_result
                (id, commodity, starSystem, stationName, stationType, price, supply, fleetCarrier, foundAt, side)
            VALUES (1, :commodity, :starSystem, :stationName, :stationType, :price, :supply, :fleetCarrier, :foundAt, :side)
            """)
    void save(@BindBean FoundMarket market);

    @SqlQuery("SELECT * FROM commodity_search_result WHERE id = 1")
    FoundMarket get();

    @SqlUpdate("""
            INSERT OR REPLACE INTO commodity_search_line (position, commodity, symbol, price, supply, unitsToBuy)
            VALUES (:position, :commodity, :symbol, :price, :supply, :unitsToBuy)
            """)
    void saveLine(@BindBean FoundLine line);

    @SqlQuery("SELECT * FROM commodity_search_line ORDER BY position")
    List<FoundLine> lines();

    @SqlUpdate("DELETE FROM commodity_search_result")
    void clearMarket();

    @SqlUpdate("DELETE FROM commodity_search_line")
    void clearLines();

    /**
     * Replaces the whole answer - the market and everything to buy there - as one unit.
     * <p>
     * Transactional because a reader that caught the gap would draw the new market with the previous
     * search's shopping list on it.
     */
    @Transaction
    default void replaceResult(FoundMarket market, List<FoundLine> lines) {
        clearLines();
        save(market);
        int position = 0;
        for (FoundLine line : lines == null ? List.<FoundLine>of() : lines) {
            if (line == null || line.getCommodity() == null) continue;
            line.setPosition(position++);
            saveLine(line);
        }
    }

    @Transaction
    default void clear() {
        clearMarket();
        clearLines();
    }

    class FoundLineMapper implements RowMapper<FoundLine> {
        @Override
        public FoundLine map(ResultSet rs, StatementContext ctx) throws SQLException {
            FoundLine line = new FoundLine();
            line.setPosition(rs.getInt("position"));
            line.setCommodity(rs.getString("commodity"));
            line.setSymbol(rs.getString("symbol"));
            line.setPrice(rs.getLong("price"));
            line.setSupply(rs.getLong("supply"));
            line.setUnitsToBuy(rs.getInt("unitsToBuy"));
            return line;
        }
    }

    /**
     * One good to load at the market that was found. Position 0 is the good the search was anchored on.
     */
    class FoundLine {
        private int position;
        private String commodity;
        private String symbol;
        private long price;
        private long supply;
        private int unitsToBuy;

        public FoundLine() {
        }

        public int getPosition() {
            return position;
        }

        public void setPosition(int position) {
            this.position = position;
        }

        /**
         * English name as the commodities table spells it; localized for display at read time.
         */
        public String getCommodity() {
            return commodity;
        }

        public void setCommodity(String commodity) {
            this.commodity = commodity;
        }

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        /**
         * Credits per tonne.
         */
        public long getPrice() {
            return price;
        }

        public void setPrice(long price) {
            this.price = price;
        }

        public long getSupply() {
            return supply;
        }

        public void setSupply(long supply) {
            this.supply = supply;
        }

        /**
         * Tonnes this trip would load, which is what the commander actually needs told.
         */
        public int getUnitsToBuy() {
            return unitsToBuy;
        }

        /**
         * Tonnes to move at this market, whichever way they are moving: bought on a buy card, sold on a
         * sell one. The column keeps its original name because renaming a column in SQLite means rebuilding
         * the table, and the market's own {@code side} already says which it is.
         */

        public void setUnitsToBuy(int unitsToBuy) {
            this.unitsToBuy = unitsToBuy;
        }
    }

    class FoundMarketMapper implements RowMapper<FoundMarket> {
        @Override
        public FoundMarket map(ResultSet rs, StatementContext ctx) throws SQLException {
            FoundMarket market = new FoundMarket();
            market.setCommodity(rs.getString("commodity"));
            market.setStarSystem(rs.getString("starSystem"));
            market.setStationName(rs.getString("stationName"));
            market.setStationType(rs.getString("stationType"));
            market.setPrice(rs.getLong("price"));
            market.setSupply(rs.getLong("supply"));
            market.setFleetCarrier(rs.getBoolean("fleetCarrier"));
            market.setFoundAt(rs.getString("foundAt"));
            market.setSide(rs.getString("side"));
            return market;
        }
    }

    class FoundMarket {
        private String commodity;
        private String starSystem;
        private String stationName;
        private String stationType;
        private long price;
        private long supply;
        private boolean fleetCarrier;
        private String foundAt;
        private String side;

        public FoundMarket() {
        }

        /**
         * English name as the commodities table spells it. Localized for display at read time, so a card
         * written before the commander changed language still reads correctly.
         */
        public String getCommodity() {
            return commodity;
        }

        public void setCommodity(String commodity) {
            this.commodity = commodity;
        }

        public String getStarSystem() {
            return starSystem;
        }

        public void setStarSystem(String starSystem) {
            this.starSystem = starSystem;
        }

        public String getStationName() {
            return stationName;
        }

        public void setStationName(String stationName) {
            this.stationName = stationName;
        }

        public String getStationType() {
            return stationType;
        }

        public void setStationType(String stationType) {
            this.stationType = stationType;
        }

        /**
         * Credits per tonne.
         */
        public long getPrice() {
            return price;
        }

        public void setPrice(long price) {
            this.price = price;
        }

        /**
         * Units the market can trade on the commander's side of the counter when Spansh last heard: stock
         * to buy, or tonnage wanted to sell. Zero when it did not say. See {@link #getSide()}.
         */
        public long getSupply() {
            return supply;
        }

        public void setSupply(long supply) {
            this.supply = supply;
        }

        /**
         * A carrier jumps, so a card pointing at one has to say so.
         */
        public boolean isFleetCarrier() {
            return fleetCarrier;
        }

        public void setFleetCarrier(boolean fleetCarrier) {
            this.fleetCarrier = fleetCarrier;
        }

        /**
         * {@code BUY} or {@code SELL} - which way round the figures on this card read.
         * <p>
         * The whole row means something different either way: the price is what the commander pays or is
         * paid, and {@code supply} is stock or demand. Null on a row written before searching had a
         * direction at all, which was always a buy.
         */
        public String getSide() {
            return side;
        }

        public void setSide(String side) {
            this.side = side;
        }

        public String getFoundAt() {
            return foundAt;
        }

        public void setFoundAt(String foundAt) {
            this.foundAt = foundAt;
        }
    }
}
