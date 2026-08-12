package elite.intel.db.dao;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@RegisterRowMapper(TradeRouteDao.TradeRouteMapper.class)
public interface TradeRouteDao {

    @SqlUpdate("""
            INSERT OR REPLACE INTO trade_route (legNumber, json, totalLegs)
                VALUES (:legNumber, :json, :totalLegs)
                        on conflict do update set
                        json = excluded.json,
                        totalLegs = excluded.totalLegs
            """)
    void save(@BindBean TradeRouteDao.TradeRoute data);

    /**
     * How many legs the route had when it was plotted, or 0 when it was plotted before this was recorded.
     * Every surviving row carries the same value, so it outlives the legs that have been flown.
     */
    @SqlQuery("SELECT COALESCE(MAX(totalLegs), 0) FROM trade_route")
    int totalLegs();

    @SqlQuery("SELECT * FROM trade_route ORDER BY legNumber ASC LIMIT 1")
    TradeRoute getNextStop();

    @SqlUpdate("DELETE FROM trade_route")
    void clear();

    /**
     * Retires the leg being flown, and only if this market is where it ends.
     * <p>
     * This used to delete <em>every</em> leg ending at the market. A Spansh route bounces between the same few
     * stations, so one sale silently deleted every future leg back to that station: a ten-leg route lost six of
     * them at the first sale, and the overlay then counted its total from what survived, which is how
     * "LEG 3 OF 6" appeared on a route that was ten legs long. The commander lost the legs as well as the count.
     * <p>
     * Matching the <em>next</em> leg rather than any leg is what keeps it honest: a route is a chain, so the
     * only leg a sale can complete is the one currently being flown. Selling elsewhere - dumping unrelated
     * cargo at a station that a later leg happens to end at - now leaves the route alone.
     */
    @SqlUpdate("""
            DELETE FROM trade_route
             WHERE legNumber = (
                   SELECT legNumber FROM trade_route
                    WHERE CAST(json_extract(json, '$.destinationMarketId') AS INTEGER) = :marketId
                    ORDER BY legNumber ASC
                    LIMIT 1)
               AND legNumber = (SELECT MIN(legNumber) FROM trade_route)
            """)
    void deleteForMarketId(@Bind("marketId") long marketId);

    @SqlQuery("SELECT * FROM trade_route where json LIKE :pattern")
    TradeRoute findForStarSystem(@Bind("pattern") String pattern);

    @SqlQuery("SELECT * FROM trade_route")
    List<TradeRoute>listAll();



    class TradeRouteMapper implements RowMapper<TradeRoute> {

        @Override public TradeRoute map(ResultSet rs, StatementContext ctx) throws SQLException {
            TradeRoute route = new TradeRoute();
            route.setLegNumber(rs.getInt("legNumber"));
            route.setJson(rs.getString("json"));
            int total = rs.getInt("totalLegs");
            route.setTotalLegs(rs.wasNull() ? null : total);
            return route;
        }
    }


    class TradeRoute {
        private Integer legNumber;
        private String json;
        /**
         * The route's length when plotted; null for routes stored before it was recorded.
         */
        private Integer totalLegs;

        public Integer getTotalLegs() {
            return totalLegs;
        }

        public void setTotalLegs(Integer totalLegs) {
            this.totalLegs = totalLegs;
        }

        public TradeRoute() {
        }

        public Integer getLegNumber() {
            return legNumber;
        }

        public void setLegNumber(Integer legNumber) {
            this.legNumber = legNumber;
        }

        public String getJson() {
            return json;
        }

        public void setJson(String json) {
            this.json = json;
        }
    }
}
