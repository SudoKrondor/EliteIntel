package elite.intel.db.managers;

import elite.intel.bindforge.devicefiles.Provenance;
import elite.intel.db.dao.BindForgeDeviceInstallsDao;
import elite.intel.db.dao.BindForgeDeviceInstallsDao.InstallDeviceRow;
import elite.intel.db.util.Database;
import elite.intel.db.managers.BindForgeDeviceInstallsManager.FoundDevice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        devices.record(steam, "LVWAP", "3344", "83F4", Provenance.USER_PREEXISTING, true);
        devices.record(epic, "LVWAP", "3344", "83F4", Provenance.USER_PREEXISTING, false);

        assertEquals(1, devices.findByInstall(steam).size());
        assertEquals(1, devices.findByInstall(epic).size());
        assertTrue(devices.findOne(steam, "LVWAP").hasButtonMap());
        assertFalse(devices.findOne(epic, "LVWAP").hasButtonMap(),
                "the Epic install's orphaned button map is the September incident, and the rows carry it");
    }

    /** The incident these rows exist for: a patch replaces the file, and the row is what notices. */
    @Test
    void aDeviceMissingFromOneInstallationIsVisibleAsMissing() {
        devices.record(steam, "RVWAP", "3344", "03F5", Provenance.USER_PREEXISTING, true);

        assertNotNull(devices.findOne(steam, "RVWAP"));
        assertNull(devices.findOne(epic, "RVWAP"), "Epic has never heard of it, which is the finding");
    }

    @Test
    void recordingTheSameEntryAgainUpdatesItRatherThanFailing() {
        devices.record(steam, "LVWAP", "3344", "83F3", Provenance.UNKNOWN, false);
        devices.record(steam, "LVWAP", "3344", "83F4", Provenance.BINDFORGE, true);

        List<InstallDeviceRow> rows = devices.findByInstall(steam);
        assertEquals(1, rows.size(), "a rescan re-reads the same entry, it does not add a second");
        assertEquals("83F4", rows.getFirst().pid(), "and what it just read is the truth");
        assertEquals(Provenance.BINDFORGE, rows.getFirst().provenance());
    }

    /**
     * An entry deleted outside BindForge has to disappear from these rows too - upserting what was found
     * would leave it behind and report a device the installation no longer has.
     */
    @Test
    void replacingAnInstallationsRowsDropsWhatTheScanNoLongerFinds() {
        devices.record(steam, "LVWAP", "3344", "83F4", Provenance.USER_PREEXISTING, true);
        devices.record(steam, "RVWAP", "3344", "03F5", Provenance.USER_PREEXISTING, true);

        devices.replaceForInstall(steam, List.of(new FoundDevice(
                "LVWAP", "3344", "83F4", Provenance.USER_PREEXISTING, true)));

        List<InstallDeviceRow> rows = devices.findByInstall(steam);
        assertEquals(1, rows.size());
        assertEquals("LVWAP", rows.getFirst().deviceName());
    }

    /**
     * The most consequential transaction in the slice.
     * <p>
     * The delete commits by itself under autocommit, so a failure before the rows are refilled would leave
     * this installation with no device rows at all - and per the migration's own header, an installation with
     * no rows cannot be told from a device that was never configured. That is the signature of a game patch
     * having wiped the file, which is the incident this table exists to detect. Replacing a set must never be
     * able to manufacture it.
     * <p>
     * The second entry below has no device name, which its NOT NULL constraint rejects, so it fails after
     * the first has already been written.
     */
    @Test
    void aFailedReplacementLeavesTheInstallationsRowsIntact() {
        devices.record(steam, "LVWAP", "3344", "83F4", Provenance.USER_PREEXISTING, true);
        devices.record(steam, "RVWAP", "3344", "03F5", Provenance.USER_PREEXISTING, true);

        List<FoundDevice> halfBad = new ArrayList<>();
        halfBad.add(new FoundDevice("LVWAP", "3344", "83F4", Provenance.USER_PREEXISTING, true));
        halfBad.add(new FoundDevice(null, "3344", "0259", Provenance.FRONTIER, false));
        assertThrows(RuntimeException.class, () -> devices.replaceForInstall(steam, halfBad));

        List<InstallDeviceRow> rows = devices.findByInstall(steam);
        assertEquals(2, rows.size(), "the installation still has its devices, rather than looking wiped");
        assertEquals("LVWAP", rows.getFirst().deviceName());
        assertEquals("RVWAP", rows.get(1).deviceName());
    }

    /**
     * The enum stops a bad provenance being written through the manager, but the column is the last line and
     * nothing proved it held. Reached through the DAO, which still takes the stored String because that is
     * the SQL seam.
     */
    @Test
    void theColumnItselfRefusesAProvenanceOutsideTheFour() {
        assertThrows(RuntimeException.class, () -> Database.withDao(BindForgeDeviceInstallsDao.class, dao -> {
            dao.record(steam, "LVWAP", "3344", "83F4", "not a provenance", true);
            return Void.TYPE;
        }));

        assertTrue(devices.findByInstall(steam).isEmpty(), "and nothing was written");
    }

    /** Every value the enum offers is one the column accepts - the two lists cannot drift apart silently. */
    @Test
    void everyProvenanceTheEnumOffersIsAcceptedByTheColumn() {
        for (Provenance provenance : Provenance.values()) {
            devices.record(steam, "Device" + provenance.name(), "3344", "83F4", provenance, false);
        }

        assertEquals(Provenance.values().length, devices.findByInstall(steam).size());
    }

    /**
     * Removing an installation discards what was held against it. That is deliberate - device rows are
     * rebuilt by a rescan - but it must actually happen, or the rows outlive the installation they describe.
     */
    @Test
    void removingAnInstallationTakesItsDeviceRowsWithIt() {
        devices.record(steam, "LVWAP", "3344", "83F4", Provenance.USER_PREEXISTING, true);
        devices.record(epic, "LVWAP", "3344", "83F4", Provenance.USER_PREEXISTING, false);

        installations.remove(steam);

        assertTrue(devices.findByInstall(steam).isEmpty(), "gone with the installation");
        assertEquals(1, devices.findByInstall(epic).size(), "and the other installation is untouched");
    }
}
