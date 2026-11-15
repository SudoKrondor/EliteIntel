package elite.intel.db.dao;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/**
 * The notices the user has chosen not to see again, from {@code 12025__quieted_notices.sql} (Alan, 2026-11-14, Storage
 * S21). Callers go through {@link elite.intel.db.managers.QuietedNoticesManager}.
 */
public interface QuietedNoticesDao {

    @SqlQuery("SELECT COUNT(*) FROM quieted_notices WHERE notice = :notice")
    int count(@Bind("notice") String notice);

    @SqlQuery("SELECT COUNT(*) FROM quieted_notices")
    int countAll();

    @SqlUpdate("INSERT INTO quieted_notices (notice) VALUES (:notice) ON CONFLICT (notice) DO NOTHING")
    void quiet(@Bind("notice") String notice);

    @SqlUpdate("DELETE FROM quieted_notices WHERE notice = :notice")
    void unquiet(@Bind("notice") String notice);

    @SqlUpdate("DELETE FROM quieted_notices")
    void deleteAll();
}
