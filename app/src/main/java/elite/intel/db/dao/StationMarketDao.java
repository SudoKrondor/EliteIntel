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

@RegisterRowMapper(StationMarketDao.StationMarketMapper.class)
public interface StationMarketDao {

    @SqlUpdate("""
            INSERT OR REPLACE INTO station_markets (marketId, stationName, json)
            VALUES(:marketId, :stationName, :json)
            ON CONFLICT(marketId) DO UPDATE SET
            stationName = excluded.stationName,
            json = excluded.json
            """)
    void upsert(@BindBean StationMarketDao.StationMarket stationMarket);

    @SqlQuery("SELECT * FROM station_markets WHERE marketId = :marketId")
    StationMarketDao.StationMarket get(@BindBean StationMarketDao.StationMarket marketId);


    /**
     * The market with this id. The only unambiguous handle a port has: station names repeat across the
     * galaxy, and a carrier's name is its callsign rather than anything the commander chose.
     */
    @SqlQuery("SELECT * FROM station_markets WHERE marketId = :marketId")
    StationMarket findByMarketId(@Bind("marketId") long marketId);

    @SqlQuery("SELECT * FROM station_markets WHERE stationName = :stationName LIMIT 1")
    StationMarketDao.StationMarket findForStation(@Bind("stationName") String stationName);

    /**
     * Every stored market with this station name. Station names are not unique across the galaxy, so
     * the caller has to check the system too - which is inside the JSON rather than in a column, so
     * the rows come back and the choosing happens in Java.
     */
    @SqlQuery("SELECT * FROM station_markets WHERE LOWER(stationName) = LOWER(:stationName)")
    StationMarket[] findAllForStation(@Bind("stationName") String stationName);

    @SqlQuery("SELECT * FROM station_markets")
    StationMarket[] listAll();

    @SqlUpdate("DELETE FROM station_markets")
    void clear();


    class StationMarketMapper implements RowMapper<StationMarket> {

        @Override public StationMarket map(ResultSet rs, StatementContext ctx) throws SQLException {
            StationMarket stationMarket = new StationMarket();
            stationMarket.setJson(rs.getString("json"));
            stationMarket.setStationName(rs.getString("stationName"));
            stationMarket.setMarketId(rs.getLong("marketId"));
            return stationMarket;
        }
    }


    class StationMarket {
        private String json;
        private String stationName;
        private Long marketId;
        public StationMarket() {
        }

        public String getJson() {
            return json;
        }

        public void setJson(String json) {
            this.json = json;
        }

        public Long getMarketId() {
            return marketId;
        }

        public void setMarketId(Long marketId) {
            this.marketId = marketId;
        }

        public String getStationName() {
            return stationName;
        }

        public void setStationName(String stationName) {
            this.stationName = stationName;
        }
    }
}
