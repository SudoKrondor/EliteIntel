package elite.intel.db.dao;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.ResultSet;
import java.sql.SQLException;

@RegisterRowMapper(TradeProfileDao.TradeProfileMapper.class)
public interface TradeProfileDao {

    @SqlQuery("SELECT * FROM trade_profile where shipId= :shipId")
    TradeProfile getTradeProfile(int shipId);

    @SqlUpdate("""
            INSERT INTO trade_profile (shipId, padSize, allowPlanetary, allowProhibited, allowPermit, allowFleetCarrier, startingBudget, maxDistanceLs, maxJumps, allowStrongHold)
            values(:shipId, :padSize, :allowPlanetary, :allowProhibited, :allowPermit, :allowFleetCarrier, :startingBudget, :maxDistanceLs, :maxJumps, :allowStrongHold)
                on conflict do update set
                    padSize = excluded.padSize,
                    allowFleetCarrier = excluded.allowFleetCarrier,
                    allowPlanetary = excluded.allowPlanetary,
                    allowPermit = excluded.allowPermit,
                    allowProhibited = excluded.allowProhibited,
                    startingBudget = excluded.startingBudget,
                    maxDistanceLs = excluded.maxDistanceLs,
                    maxJumps = excluded.maxJumps,
                    allowStrongHold = excluded.allowStrongHold
            """)
    void save(@BindBean TradeProfileDao.TradeProfile profile);


    class TradeProfileMapper implements RowMapper<TradeProfile> {

        @Override public TradeProfile map(ResultSet rs, StatementContext ctx) throws SQLException {
            TradeProfile profile = new TradeProfile();
            profile.setShipId(rs.getInt("shipId"));
            profile.setPadSize(rs.getString("padSize"));
            profile.setAllowPlanetary(rs.getBoolean("allowPlanetary"));
            profile.setAllowProhibited(rs.getBoolean("allowProhibited"));
            profile.setAllowPermit(rs.getBoolean("allowPermit"));
            profile.setAllowFleetCarrier(rs.getBoolean("allowFleetCarrier"));
            profile.setStartingBudget(rs.getInt("startingBudget"));
            profile.setMaxDistanceLs(rs.getInt("maxDistanceLs"));
            profile.setMaxJumps(rs.getInt("maxJumps"));
            profile.setAllowStrongHold(rs.getBoolean("allowStrongHold"));
            return profile;
        }
    }


    class TradeProfile {
        private Integer shipId;
        private String padSize = "S";
        private boolean allowPlanetary = false;
        private boolean allowProhibited = false;
        private boolean allowPermit = false;
        private boolean allowFleetCarrier = false;
        private boolean allowStrongHold = false;
        private Integer startingBudget = 250_000_000;
        private Integer maxDistanceLs = 6000;
        private Integer maxJumps = 4;

        @SuppressWarnings("unused") // used for mapping
        public Integer getShipId() {
            return shipId;
        }

        public void setShipId(Integer shipId) {
            this.shipId = shipId;
        }

        @SuppressWarnings("unused") // used for mapping
        public String getPadSize() {
            return padSize;
        }

        public void setPadSize(String padSize) {
            this.padSize = padSize;
        }

        public boolean isAllowPlanetary() {
            return allowPlanetary;
        }

        public void setAllowPlanetary(boolean allowPlanetary) {
            this.allowPlanetary = allowPlanetary;
        }

        public boolean isAllowProhibited() {
            return allowProhibited;
        }

        public void setAllowProhibited(boolean allowProhibited) {
            this.allowProhibited = allowProhibited;
        }

        public boolean isAllowPermit() {
            return allowPermit;
        }

        public void setAllowPermit(boolean allowPermit) {
            this.allowPermit = allowPermit;
        }

        public boolean isAllowFleetCarrier() {
            return allowFleetCarrier;
        }

        public void setAllowFleetCarrier(boolean allowFleetCarrier) {
            this.allowFleetCarrier = allowFleetCarrier;
        }

        public Integer getStartingBudget() {
            return startingBudget;
        }

        public void setStartingBudget(Integer startingBudget) {
            this.startingBudget = startingBudget;
        }

        public Integer getMaxDistanceLs() {
            return maxDistanceLs;
        }

        public void setMaxDistanceLs(Integer maxDistanceLs) {
            this.maxDistanceLs = maxDistanceLs;
        }

        public Integer getMaxJumps() {
            return maxJumps;
        }

        public void setMaxJumps(Integer maxJumps) {
            this.maxJumps = maxJumps;
        }

        public boolean isAllowStrongHold() {
            return allowStrongHold;
        }

        public void setAllowStrongHold(boolean allowStrongHold) {
            this.allowStrongHold = allowStrongHold;
        }
    }
}
