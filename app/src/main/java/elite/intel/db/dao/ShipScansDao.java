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

@RegisterRowMapper(ShipScansDao.ShipScanMapper.class)
public interface ShipScansDao {

    @SqlUpdate("""
            INSERT OR REPLACE INTO ship_scans (key, scan)
            VALUES(:key, :scan)
            ON CONFLICT(key) DO UPDATE SET
                scan = excluded.scan
            """)
    void upsert(@BindBean ShipScansDao.ShipScan scan);


    @SqlQuery("SELECT scan FROM ship_scans WHERE key = :key")
    String get(@Bind("key") String key);

    @SqlUpdate("DELETE FROM ship_scans WHERE key = :key")
    void remove(@Bind("key") String key);

    @SqlUpdate("DELETE FROM ship_scans")
    void clear();


    class ShipScanMapper  implements RowMapper<ShipScansDao.ShipScan> {
        @Override public ShipScan map(ResultSet rs, StatementContext ctx) throws SQLException {
            ShipScan shipScan = new ShipScan();
            shipScan.setKey(rs.getString("key"));
            shipScan.setScan(rs.getString("scan"));
            return shipScan;
        }
    }


    class ShipScan {
        private String key;
        private String scan;

        public ShipScan() {
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getScan() {
            return scan;
        }

        public void setScan(String scan) {
            this.scan = scan;
        }
    }
}
