package elite.intel.db.managers;

import elite.intel.db.dao.BindForgeDeviceInstallsDao.InstallDeviceRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * These rows record what each installation actually holds, which is what makes a game patch visible: with
 * nothing to compare a patched installation against, a wiped file looks exactly like a device that was never
 * configured.
 */
class BindForgeDeviceInstallsManagerTest {

    private final BindForgeInstallationsManager installations = BindForgeInstallationsManager.getInstance();
    private final BindForgeDeviceInstallsManager devices = BindForgeDeviceInstallsManager.getInstance();

    private long steam;
    private long epic;

    @BeforeEach
    void twoInstallations() {
        installations.findAll().forEach(row -> installations.remove(row.id()));
        steam = installations.record("STEAM", Path.of("E:/SteamLibrary/Elite Dangerous"), false).id();
        epic = installations.record("EPIC", Path.of("E:/EpicLibrary/Elite Dangerous"), false).id();
    }

    @Test
    void eachInstallationKeepsItsOwnRowForTheSameDevice() {
        devices.record(steam, "LVWAP", "3344", "83F4", "user_preexisting", true);
        devices.record(epic, "LVWAP", "3344", "83F4", "user_preexisting", false);

        assertEquals(1, devices.findByInstall(steam).size());
        assertEquals(1, devices.findByInstall(epic).size());
        assertTrue(devices.findOne(steam, "LVWAP").hasButtonMap());
        assertFalse(devices.findOne(epic, "LVWAP").hasButtonMap(),
                "the Epic install's orphaned button map is the September incident, and the rows carry it");
    }

    /** The incident these rows exist for: a patch replaces the file, and the row is what notices. */
    @Test
    void aDeviceMissingFromOneInstallationIsVisibleAsMissing() {
        devices.record(steam, "RVWAP", "3344", "03F5", "user_preexisting", true);

        assertNotNull(devices.findOne(steam, "RVWAP"));
        assertNull(devices.findOne(epic, "RVWAP"), "Epic has never heard of it, which is the finding");
    }

    @Test
    void recordingTheSameEntryAgainUpdatesItRatherThanFailing() {
        devices.record(steam, "LVWAP", "3344", "83F3", "unknown", false);
        devices.record(steam, "LVWAP", "3344", "83F4", "bindforge", true);

        List<InstallDeviceRow> rows = devices.findByInstall(steam);
        assertEquals(1, rows.size(), "a rescan re-reads the same entry, it does not add a second");
        assertEquals("83F4", rows.getFirst().pid(), "and what it just read is the truth");
        assertEquals("bindforge", rows.getFirst().provenance());
    }

    /**
     * An entry deleted outside BindForge has to disappear from these rows too - upserting what was found
     * would leave it behind and report a device the installation no longer has.
     */
    @Test
    void replacingAnInstallationsRowsDropsWhatTheScanNoLongerFinds() {
        devices.record(steam, "LVWAP", "3344", "83F4", "user_preexisting", true);
        devices.record(steam, "RVWAP", "3344", "03F5", "user_preexisting", true);

        devices.replaceForInstall(steam, List.of(new InstallDeviceRow(
                steam, "LVWAP", "3344", "83F4", "user_preexisting", true)));

        List<InstallDeviceRow> rows = devices.findByInstall(steam);
        assertEquals(1, rows.size());
        assertEquals("LVWAP", rows.getFirst().deviceName());
    }

    /**
     * Removing an installation discards what was held against it. That is deliberate - device rows are
     * rebuilt by a rescan - but it must actually happen, or the rows outlive the installation they describe.
     */
    @Test
    void removingAnInstallationTakesItsDeviceRowsWithIt() {
        devices.record(steam, "LVWAP", "3344", "83F4", "user_preexisting", true);
        devices.record(epic, "LVWAP", "3344", "83F4", "user_preexisting", false);

        installations.remove(steam);

        assertTrue(devices.findByInstall(steam).isEmpty(), "gone with the installation");
        assertEquals(1, devices.findByInstall(epic).size(), "and the other installation is untouched");
    }
}
