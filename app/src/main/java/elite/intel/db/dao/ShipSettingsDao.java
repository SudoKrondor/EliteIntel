package elite.intel.db.dao;

import elite.intel.gameapi.SurfaceVehicle;
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
            INSERT INTO ship_settings (shipId, honkTrigger, honkFireGroup, honkOnJump, hgeAlerts,
                                       vehicleBay1, vehicleBay2, vehicleBay3, vehicleBay4)
            values(:shipId, :honkTrigger, :honkFireGroup, :honkOnJump, :hgeAlerts,
                   :vehicleBay1, :vehicleBay2, :vehicleBay3, :vehicleBay4)
                on conflict (shipId) do update set
                    honkTrigger   = excluded.honkTrigger,
                    honkFireGroup = excluded.honkFireGroup,
                    honkOnJump    = excluded.honkOnJump,
                    hgeAlerts     = excluded.hgeAlerts,
                    vehicleBay1   = excluded.vehicleBay1,
                    vehicleBay2   = excluded.vehicleBay2,
                    vehicleBay3   = excluded.vehicleBay3,
                    vehicleBay4   = excluded.vehicleBay4
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
            entity.setVehicleBay1(rs.getString("vehicleBay1"));
            entity.setVehicleBay2(rs.getString("vehicleBay2"));
            entity.setVehicleBay3(rs.getString("vehicleBay3"));
            entity.setVehicleBay4(rs.getString("vehicleBay4"));
            return entity;
        }
    }

    class ShipSettings {
        int shipId;
        int honkTrigger;
        String honkFireGroup;
        boolean honkOnJump;
        boolean hgeAlerts;
        /**
         * What is in each hangar bay, by {@code SurfaceVehicle} name, or null for a bay the commander has
         * not configured. Null is load-bearing: the deploy command refuses on it rather than guessing.
         */
        String vehicleBay1;
        String vehicleBay2;
        String vehicleBay3;
        String vehicleBay4;

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

        public String getVehicleBay1() {
            return vehicleBay1;
        }

        public void setVehicleBay1(String vehicleBay1) {
            this.vehicleBay1 = vehicleBay1;
        }

        public String getVehicleBay2() {
            return vehicleBay2;
        }

        public void setVehicleBay2(String vehicleBay2) {
            this.vehicleBay2 = vehicleBay2;
        }

        public String getVehicleBay3() {
            return vehicleBay3;
        }

        public void setVehicleBay3(String vehicleBay3) {
            this.vehicleBay3 = vehicleBay3;
        }

        public String getVehicleBay4() {
            return vehicleBay4;
        }

        public void setVehicleBay4(String vehicleBay4) {
            this.vehicleBay4 = vehicleBay4;
        }

        /**
         * The four bays in bay order, as the deploy decision wants them: nulls for the ones the commander
         * has not configured. Reading them through one accessor keeps the settings dialog and the deploy
         * command from disagreeing about which column is which bay.
         */
        public java.util.List<SurfaceVehicle> vehicleBays() {
            return java.util.Arrays.asList(
                    SurfaceVehicle.fromStored(vehicleBay1),
                    SurfaceVehicle.fromStored(vehicleBay2),
                    SurfaceVehicle.fromStored(vehicleBay3),
                    SurfaceVehicle.fromStored(vehicleBay4));
        }

        /**
         * Writes one bay, 1-based. A null vehicle clears it back to "not configured".
         */
        public void setVehicleBay(int bay, SurfaceVehicle vehicle) {
            String stored = vehicle == null ? null : vehicle.name();
            switch (bay) {
                case 1 -> vehicleBay1 = stored;
                case 2 -> vehicleBay2 = stored;
                case 3 -> vehicleBay3 = stored;
                case 4 -> vehicleBay4 = stored;
                default -> throw new IllegalArgumentException("No such vehicle bay: " + bay);
            }
        }

        /**
         * Reads one bay, 1-based.
         */
        public SurfaceVehicle getVehicleBay(int bay) {
            return vehicleBays().get(bay - 1);
        }
    }
}
