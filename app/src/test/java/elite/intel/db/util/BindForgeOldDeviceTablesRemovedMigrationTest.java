package elite.intel.db.util;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static elite.intel.db.util.MigrationScripts.run;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Migration {@code 12013} (Storage S4b, Alan, 2026-10-28): the device master, the draft, the pending removals, the
 * per-installation rows and the waiting hardware-id moves are dropped, with {@code missed_write} - on a database that
 * still holds rows in them. The installations, the remembered identity answers and the settings stay.
 */
class BindForgeOldDeviceTablesRemovedMigrationTest {

    private static final List<String> DROPPED = List.of("bindforge_device_master", "bindforge_device_labels",
            "bindforge_device_installs", "bindforge_device_draft_state", "bindforge_device_draft",
            "bindforge_device_draft_labels", "bindforge_device_pending_removal", "bindforge_hardware_id_move");

    @Test
    void theOldTablesGoAndWhatIsStillUsedStays() throws IOException {
        try (Handle handle = Jdbi.create("jdbc:sqlite::memory:").open()) {
            handle.execute("PRAGMA foreign_keys = ON");
            for (String file : List.of("12000__bindforge_settings.sql", "12001__bindforge_installations.sql",
                    "12002__bindforge_devices.sql", "12003__bindforge_device_draft.sql",
                    "12004__bindforge_device_pending_removal.sql", "12005__bindforge_activation.sql",
                    "12006__bindforge_missed_write.sql", "12007__bindforge_identity_answer.sql",
                    "12008__bindforge_hardware_id_move.sql", "12012__bindforge_first_time_startup.sql")) {
                run(handle, file);
            }
            handle.execute("INSERT INTO bindforge_installations (storefront, root_path, missed_write) "
                    + "VALUES ('STEAM', 'E:\\Steam', 1)");
            handle.execute("INSERT INTO bindforge_device_master (device_name, vid, pid) VALUES ('LVWAP', '3344', '83F4')");
            handle.execute("INSERT INTO bindforge_device_labels (device_id, input_token, label) "
                    + "VALUES (1, 'Joy_1', 'TRIGGER')");
            handle.execute("INSERT INTO bindforge_identity_answer (device_name, candidates, answer) "
                    + "VALUES ('LVWAP', '3344:83F5', 'NOT_NOW')");

            run(handle, "12013__bindforge_old_device_tables_removed.sql");

            List<String> tables = handle.createQuery("SELECT name FROM sqlite_master WHERE type = 'table'")
                    .mapTo(String.class).list();
            DROPPED.forEach(table -> assertFalse(tables.contains(table), table));
            assertTrue(tables.containsAll(List.of("bindforge_installations", "bindforge_identity_answer",
                    "bindforge_settings", "bindforge_activation")), tables.toString());

            List<String> columns = handle.createQuery("SELECT name FROM pragma_table_info('bindforge_installations')")
                    .mapTo(String.class).list();
            assertFalse(columns.contains("missed_write"), columns.toString());
            assertEquals(1, handle.createQuery("SELECT COUNT(*) FROM bindforge_installations")
                    .mapTo(Integer.class).one(), "the installation list is kept");
            assertEquals(1, handle.createQuery("SELECT COUNT(*) FROM bindforge_identity_answer")
                    .mapTo(Integer.class).one(), "the remembered answers are kept");
            List<String> settings = handle.createQuery("SELECT name FROM pragma_table_info('bindforge_settings')")
                    .mapTo(String.class).list();
            assertTrue(settings.contains("edit_history_retention"), "it goes in Storage S6, with what reads it");
        }
    }
}
