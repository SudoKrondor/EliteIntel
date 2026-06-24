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

@RegisterRowMapper(ShipDao.ShipRowMapper.class)
public interface ShipDao {


    @SqlQuery("SELECT * FROM ship where shipId= :shipId")
    Ship findShip(int shipId);

    @SqlQuery("SELECT * FROM ship")
    List<Ship> allShips();

    @SqlQuery("SELECT * FROM ship WHERE commanderName = :commanderName OR commanderName IS NULL")
    List<Ship> allShipsForCommander(@Bind("commanderName") String commanderName);


    @SqlUpdate("""
            INSERT INTO ship (shipName, shipId, shipIdentifier, cargoCapacity, voice, personality, commanderName)
                        VALUES (:shipName, :shipId, :shipIdentifier, :cargoCapacity, :voice, :personality, :commanderName)
                        ON CONFLICT DO UPDATE SET
                        shipName = excluded.shipName,
                        shipIdentifier = excluded.shipIdentifier,
                        cargoCapacity = excluded.cargoCapacity,
                        voice = excluded.voice,
                        personality = excluded.personality,
                        commanderName = COALESCE(excluded.commanderName, commanderName)
            """)
    void save(@BindBean ShipDao.Ship ship);


    class ShipRowMapper implements RowMapper<Ship> {
        @Override public Ship map(ResultSet rs, StatementContext ctx) throws SQLException {
            Ship ship = new Ship();
            ship.setShipName(rs.getString("shipName"));
            ship.setShipId(rs.getInt("shipId"));
            ship.setShipIdentifier(rs.getString("shipIdentifier"));
            ship.setCargoCapacity(rs.getInt("cargoCapacity"));
            ship.setVoice(rs.getString("voice"));
            ship.setPersonality(rs.getString("personality"));
            ship.setCommanderName(rs.getString("commanderName"));
            return ship;
        }
    }



    public enum PadSize {
        SMALL("S"), MEDIUM("M"), LARGE("L");

        private String size;
        PadSize(String size) {
            this.size = size;
        }

        public String getSize() {
            return size;
        }
    }

    class Ship {
        private String shipName;
        private Integer shipId;
        private String shipIdentifier;
        private Integer cargoCapacity;
        private String voice;
        private String personality;
        private String commanderName;

        public String getShipName() {
            return shipName;
        }

        public void setShipName(String shipName) {
            this.shipName = shipName;
        }

        public Integer getShipId() {
            return shipId;
        }

        public void setShipId(Integer shipId) {
            this.shipId = shipId;
        }

        public String getShipIdentifier() {
            return shipIdentifier;
        }

        public void setShipIdentifier(String shipIdentifier) {
            this.shipIdentifier = shipIdentifier;
        }

        public int getCargoCapacity() {
            return cargoCapacity;
        }

        public void setCargoCapacity(int cargoCapacity) {
            this.cargoCapacity = cargoCapacity;
        }

        public void setCargoCapacity(Integer cargoCapacity) {
            this.cargoCapacity = cargoCapacity;
        }

        public String getVoice() {
            return voice;
        }

        public void setVoice(String voice) {
            this.voice = voice;
        }

        public String getPersonality() {
            return personality;
        }

        public void setPersonality(String personality) {
            this.personality = personality;
        }

        public String getCommanderName() {
            return commanderName;
        }

        public void setCommanderName(String commanderName) {
            this.commanderName = commanderName;
        }
    }
}
