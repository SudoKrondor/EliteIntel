package elite.intel.db.dao;

import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/**
 * The "first-time startup complete" flag (storage.md, <em>First-time startup</em>). One row, at {@code id = 1},
 * created by {@code 12005__bindforge_activation.sql}; the flag is its {@code activated_at}.
 * <p>
 * Callers go through {@link elite.intel.db.managers.BindForgeActivationManager}.
 */
public interface BindForgeActivationDao {

    @SqlQuery("SELECT activated_at IS NOT NULL FROM bindforge_activation WHERE id = 1")
    boolean isComplete();

    /** Marks first-time startup complete, keeping the first time it happened. */
    @SqlUpdate("""
            UPDATE bindforge_activation
            SET activated_at = COALESCE(activated_at, CURRENT_TIMESTAMP)
            WHERE id = 1
            """)
    void markComplete();

    /** Clears the flag, so first-time startup runs again - Run first-time startup again. */
    @SqlUpdate("UPDATE bindforge_activation SET activated_at = NULL WHERE id = 1")
    void clear();
}
