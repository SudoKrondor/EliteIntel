package elite.intel.db.dao;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

/**
 * Star systems struck from every search that ends in a plotted route. Names compare case-insensitively.
 */
public interface SearchExcludedSystemDao {

    /**
     * @return 1 when the system was added, 0 when it was already on the list
     */
    @SqlUpdate("""
            INSERT OR IGNORE INTO search_excluded_system (starSystem, excludedAt)
            VALUES (:starSystem, datetime('now'))
            """)
    int exclude(@Bind("starSystem") String starSystem);

    /**
     * @return 1 when the system was taken off the list, 0 when it was not on it
     */
    @SqlUpdate("DELETE FROM search_excluded_system WHERE starSystem = :starSystem")
    int allow(@Bind("starSystem") String starSystem);

    @SqlQuery("SELECT starSystem FROM search_excluded_system")
    List<String> all();
}
