package elite.intel.db.dao;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@RegisterRowMapper(ShipMakeDao.ShipMakeRowMapper.class)
public interface ShipMakeDao {

    @SqlQuery("SELECT * FROM ship_make")
    List<ShipMake> findAll();

    @SqlUpdate("""
            INSERT INTO ship_make (shipIdentifier, displayName)
                        VALUES (:shipIdentifier, :displayName)
                        ON CONFLICT(shipIdentifier) DO UPDATE SET
                        displayName = excluded.displayName
            """)
    void upsert(@Bind("shipIdentifier") String shipIdentifier, @Bind("displayName") String displayName);


    class ShipMakeRowMapper implements RowMapper<ShipMake> {
        @Override
        public ShipMake map(ResultSet rs, StatementContext ctx) throws SQLException {
            ShipMake make = new ShipMake();
            make.setId(rs.getInt("id"));
            make.setShipIdentifier(rs.getString("shipIdentifier"));
            make.setDisplayName(rs.getString("displayName"));
            return make;
        }
    }

    class ShipMake {
        private Integer id;
        private String shipIdentifier;
        private String displayName;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getShipIdentifier() {
            return shipIdentifier;
        }

        public void setShipIdentifier(String shipIdentifier) {
            this.shipIdentifier = shipIdentifier;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
    }
}
