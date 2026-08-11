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

@RegisterRowMapper(ShipSettingsDao.ShipSettingsMapper.class)
public interface ShipSettingsDao {


    @SqlQuery("SELECT * FROM ship_settings where shipId= :shipId")
    ShipSettings getShipSettings(@Bind("shipId") int shipId);


    @SqlUpdate("""
            INSERT INTO ship_settings (shipId, honkTrigger, honkFireGroup, honkOnJump, hgeAlerts)
            values(:shipId, :honkTrigger, :honkFireGroup, :honkOnJump, :hgeAlerts)
                on conflict (shipId) do update set
                    honkTrigger   = excluded.honkTrigger,
                    honkFireGroup = excluded.honkFireGroup,
                    honkOnJump    = excluded.honkOnJump,
                    hgeAlerts     = excluded.hgeAlerts
            """)
    void save(@BindBean ShipSettingsDao.ShipSettings settings);


    class ShipSettingsMapper implements RowMapper<ShipSettings> {
        @Override
        public ShipSettings map(ResultSet rs, StatementContext ctx) throws SQLException {
            ShipSettings entity = new ShipSettings();
            entity.setShipId(rs.getInt("shipId"));
            entity.setHonkTrigger(rs.getInt("honkTrigger"));
            entity.setHonkFireGroup(rs.getString("honkFireGroup"));
            entity.setHonkOnJump(rs.getBoolean("honkOnJump"));
            entity.setHgeAlerts(rs.getBoolean("hgeAlerts"));
            return entity;
        }
    }

    class ShipSettings {
        int shipId;
        int honkTrigger;
        String honkFireGroup;
        boolean honkOnJump;
        boolean hgeAlerts;

        public int getShipId() {
            return shipId;
        }

        public void setShipId(int shipId) {
            this.shipId = shipId;
        }

        public int getHonkTrigger() {
            return honkTrigger;
        }

        public void setHonkTrigger(int honkTrigger) {
            this.honkTrigger = honkTrigger;
        }

        public String getHonkFireGroup() {
            return honkFireGroup;
        }

        public void setHonkFireGroup(String honkFireGroup) {
            this.honkFireGroup = honkFireGroup;
        }

        public boolean isHonkOnJump() {
            return honkOnJump;
        }

        public void setHonkOnJump(boolean honkOnJump) {
            this.honkOnJump = honkOnJump;
        }

        public boolean isHgeAlerts() {
            return hgeAlerts;
        }

        public void setHgeAlerts(boolean hgeAlerts) {
            this.hgeAlerts = hgeAlerts;
        }
    }
}
