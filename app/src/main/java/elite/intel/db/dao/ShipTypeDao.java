package elite.intel.db.dao;

import org.jdbi.v3.sqlobject.config.KeyColumn;
import org.jdbi.v3.sqlobject.config.ValueColumn;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.Map;

public interface ShipTypeDao {

    @SqlQuery("SELECT internal_name, display_name FROM ship_type")
    @KeyColumn("internal_name")
    @ValueColumn("display_name")
    Map<String, String> findAll();

    @SqlUpdate("INSERT INTO ship_type (internal_name, display_name) VALUES (:internalName, :displayName) " +
               "ON CONFLICT(internal_name) DO UPDATE SET display_name = excluded.display_name")
    void upsert(@Bind("internalName") String internalName, @Bind("displayName") String displayName);
}
