package elite.intel.db.dao;

import org.jdbi.v3.sqlobject.config.KeyColumn;
import org.jdbi.v3.sqlobject.config.ValueColumn;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.util.Map;

public interface ShipTypeDao {

    @SqlQuery("SELECT internal_name, display_name FROM ship_type")
    @KeyColumn("internal_name")
    @ValueColumn("display_name")
    Map<String, String> findAll();
}
