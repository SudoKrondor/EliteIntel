package elite.intel.bindforge.devicefiles;

import elite.intel.db.dao.BindForgeDeviceInstallsDao.InstallDeviceRow;
import elite.intel.db.managers.BindForgeDeviceInstallsManager;
import elite.intel.db.managers.BindForgeDeviceInstallsManager.FoundDevice;
import elite.intel.db.managers.BindForgeInstallationsManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reading an installation's device files and recording what is there - the only copy of what is actually on
 * disk, against which the master is compared.
 */
class InstallationDeviceScannerTest {

    @TempDir
    Path install;

    private final BindForgeInstallationsManager installations = BindForgeInstallationsManager.getInstance();
    private final BindForgeDeviceInstallsManager devices = BindForgeDeviceInstallsManager.getInstance();
    private final InstallationDeviceScanner scanner =
            new InstallationDeviceScanner(FrontierStockDevices.getInstance(), devices);

    private long installId;

    @BeforeEach
    void oneInstallation() {
        installations.findAll().forEach(row -> installations.remove(row.id()));
        installId = installations.record("STEAM", install, false).id();
    }

    @Test
    void everyEntryInTheFileIsRecorded() throws IOException {
        deviceMappings("""
                <?xml version="1.0" encoding="UTF-8" ?>
                <Root>
                    <LVWAP><PID>83F4</PID><VID>3344</VID></LVWAP>
                    <T-Rudder><PID>B679</PID><VID>044F</VID></T-Rudder>
                </Root>
                """);

        scanner.scan(installId, install);

        List<InstallDeviceRow> rows = devices.findByInstall(installId);
        assertEquals(2, rows.size());
    }

    /**
     * The classification the stock reference exists for. Frontier's own entry is claimed as theirs; the
     * developer's stick is left unknown rather than claimed for him, because absence from the capture is not
     * proof.
     */
    @Test
    void frontiersEntriesAreMarkedTheirsAndTheRestAreLeftUnknown() throws IOException {
        deviceMappings("""
                <?xml version="1.0" encoding="UTF-8" ?>
                <Root>
                    <LVWAP><PID>83F4</PID><VID>3344</VID></LVWAP>
                    <T-Rudder><PID>B679</PID><VID>044F</VID></T-Rudder>
                </Root>
                """);

        scanner.scan(installId, install);

        assertEquals(Provenance.UNKNOWN, devices.findOne(installId, "LVWAP").provenance());
        assertEquals(Provenance.FRONTIER, devices.findOne(installId, "T-Rudder").provenance());
    }

    /** A rename has to rename the button map too, in every installation holding one, so the row records it. */
    @Test
    void anEntryWithAButtonMapIsRecordedAsHavingOne() throws IOException {
        deviceMappings("""
                <?xml version="1.0" encoding="UTF-8" ?>
                <Root>
                    <LVWAP><PID>83F4</PID><VID>3344</VID></LVWAP>
                    <RVWAP><PID>03F5</PID><VID>3344</VID></RVWAP>
                </Root>
                """);
        buttonMap("LVWAP");

        scanner.scan(installId, install);

        assertTrue(devices.findOne(installId, "LVWAP").hasButtonMap());
        assertFalse(devices.findOne(installId, "RVWAP").hasButtonMap(),
                "having none is the normal case, not a fault");
    }

    /**
     * An entry owns a set of VID/PID pairs rather than one - GamePad alone carries eighty - and they describe
     * one entry, not eighty devices. The row is keyed by element tag, which is the identity the file uses.
     */
    @Test
    void anEntryWithSeveralHardwarePairsIsStillOneRow() throws IOException {
        deviceMappings("""
                <?xml version="1.0" encoding="UTF-8" ?>
                <Root>
                    <DualShock4><PID>0BA0</PID><VID>054C</VID>
                        <Alternative><PID>05c4</PID><VID>054C</VID></Alternative>
                        <Alternative><PID>09cc</PID><VID>054C</VID></Alternative>
                    </DualShock4>
                </Root>
                """);

        scanner.scan(installId, install);

        assertEquals(1, devices.findByInstall(installId).size());
    }

    /**
     * Replacing, not merging: an entry deleted outside BindForge has to disappear from the rows too, or
     * BindForge reports a device the installation no longer has.
     */
    @Test
    void aSecondScanRecordsWhatIsThereNowRatherThanWhatWas() throws IOException {
        deviceMappings("""
                <?xml version="1.0" encoding="UTF-8" ?>
                <Root>
                    <LVWAP><PID>83F4</PID><VID>3344</VID></LVWAP>
                    <RVWAP><PID>03F5</PID><VID>3344</VID></RVWAP>
                </Root>
                """);
        scanner.scan(installId, install);

        deviceMappings("""
                <?xml version="1.0" encoding="UTF-8" ?>
                <Root>
                    <LVWAP><PID>83F4</PID><VID>3344</VID></LVWAP>
                </Root>
                """);
        scanner.scan(installId, install);

        List<InstallDeviceRow> rows = devices.findByInstall(installId);
        assertEquals(1, rows.size());
        assertEquals("LVWAP", rows.getFirst().deviceName());
    }

    /**
     * The case this whole table exists to notice, and the one it must never fake. An installation whose file
     * cannot be read keeps the rows it had: recording it as holding nothing would store the exact signature
     * of a game patch having wiped it.
     */
    @Test
    void anUnreadableFileLeavesTheRecordedDevicesAlone() throws IOException {
        deviceMappings("""
                <?xml version="1.0" encoding="UTF-8" ?>
                <Root>
                    <LVWAP><PID>83F4</PID><VID>3344</VID></LVWAP>
                </Root>
                """);
        scanner.scan(installId, install);

        deviceMappings("this is not xml at all");
        List<FoundDevice> found = scanner.scan(installId, install);

        assertTrue(found.isEmpty(), "nothing was read");
        assertEquals(1, devices.findByInstall(installId).size(),
                "and the rows from when it could be read are still there");
    }

    /**
     * An installation with no DeviceMappings.xml genuinely holds no entries, which is a finding rather than a
     * failure - unlike a file that exists and cannot be parsed.
     */
    @Test
    void anInstallationWithNoDeviceFileRecordsNothingWithoutFailing() {
        List<FoundDevice> found = scanner.scan(installId, install);

        assertTrue(found.isEmpty());
        assertTrue(devices.findByInstall(installId).isEmpty());
    }

    private void deviceMappings(String xml) throws IOException {
        Files.createDirectories(install);
        Files.writeString(install.resolve("DeviceMappings.xml"), xml);
    }

    private void buttonMap(String deviceName) throws IOException {
        Path folder = Files.createDirectories(install.resolve("DeviceButtonMaps"));
        Files.writeString(folder.resolve(deviceName + ".buttonMap"),
                "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n<Root><Joy_1>B1</Joy_1></Root>\n");
    }
}
