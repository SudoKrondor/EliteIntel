package elite.intel.db.managers;

import elite.intel.db.util.Database;
import org.jdbi.v3.core.Handle;

/**
 * Empties {@code bindforge_installations} between tests. The suite shares one database, and Remove keeps the row
 * (a removed installation stays removed), so a test that starts from an empty list deletes the rows by SQL.
 */
public final class InstallationsTable {

    private InstallationsTable() {
    }

    public static void empty() {
        try (Handle handle = Database.init()) {
            handle.execute("DELETE FROM bindforge_installations");
        }
        BindForgeInstallationsManager.getInstance().forgetCached();
    }
}
