package elite.intel.bindforge.storage;

import elite.intel.db.dao.BindForgeIdentityAnswerDao.AnswerRow;
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import elite.intel.devices.model.Device;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The report is read by someone who cannot ask the commander a follow-up, and collected when something is already
 * wrong. So what matters is that it states what the logs do not, and that one failure costs one section.
 */
class BindForgeSupportTest {

    private static final String LVWAP_GUID = "03002cec44330000f483000000000000";   // 3344:83F4

    private static final String DEVICE_MAPPINGS =
            "<Root><LVWAP><PID>83F4</PID><VID>3344</VID></LVWAP></Root>";

    @TempDir
    Path tmp;

    private Fake readings;

    @BeforeEach
    void setUp() throws IOException {
        readings = new Fake(Files.createDirectories(tmp.resolve("bindings")),
                Files.createDirectories(tmp.resolve("backups")));
    }

    @Test
    void aSplitPresetCarriesEveryBindsFileItNames() throws IOException {
        // WHY: no working copy to compare with, or the file no line names would be carried as one that differs.
        readings.complete = false;
        live("StartPreset.4.start", "Main\nShipOnly\n\nGhost\n");
        live("Main.4.2.binds", binds("Main", "Keyboard", "Key_A"));
        live("ShipOnly.4.2.binds", binds("ShipOnly", "Keyboard", "Key_B"));
        live("Unused.4.2.binds", binds("Unused", "Keyboard", "Key_C"));

        BindForgeSupport.Contents contents = BindForgeSupport.collect(readings);

        assertTrue(contents.files().containsKey("bindforge/live/Bindings/StartPreset.4.start"));
        assertTrue(contents.files().containsKey("bindforge/live/Bindings/Main.4.2.binds"));
        assertTrue(contents.files().containsKey("bindforge/live/Bindings/ShipOnly.4.2.binds"),
                "the file the root of the bundle does not carry");
        assertFalse(contents.files().containsKey("bindforge/live/Bindings/Unused.4.2.binds"));
        assertTrue(contents.report().contains("SHIP: ShipOnly -> ShipOnly.4.2.binds"), contents.report());
        assertTrue(contents.report().contains("SRV: (empty)"), contents.report());
        assertTrue(contents.report().contains("ON_FOOT: Ghost -> no binds file by this name here"), contents.report());
        assertTrue(contents.report().contains("Unused.4.2.binds"), "named among what the folder holds");
    }

    @Test
    void aFolderWithNoPresetFileSaysTheGameIsOnAFactoryPreset() {
        String report = BindForgeSupport.collect(readings).report();

        assertTrue(report.contains("none in the bindings folder, so the game is on a factory preset"), report);
    }

    @Test
    void beforeFirstSetupItCarriesWhatFirstSetupWillRead() {
        readings.complete = false;
        InstallationRow steam = row(1, "STEAM");
        readings.listed = List.of(steam);
        readings.all = List.of(steam);
        readings.deviceFiles = new DeviceFiles(bytes(DEVICE_MAPPINGS), Map.of("LVWAP.buttonMap", bytes("<Root/>")));

        BindForgeSupport.Contents contents = BindForgeSupport.collect(readings);

        assertTrue(contents.report().contains("First-time startup: not complete"), contents.report());
        assertTrue(contents.report().contains("not judged, no working copy yet"), contents.report());
        assertTrue(contents.files().containsKey("bindforge/installations/STEAM-1/DeviceMappings.xml"));
        assertTrue(contents.files().containsKey("bindforge/installations/STEAM-1/DeviceButtonMaps/LVWAP.buttonMap"));
        assertFalse(contents.report().contains("could not be read"), "no working copy is not a failure: "
                + contents.report());
    }

    @Test
    void whatTheGameChangedIsListedAndTheChangedFileCarried() throws IOException {
        live("Main.4.2.binds", binds("Main", "Keyboard", "Key_B"));
        readings.applied = set(Map.of("Main.4.2.binds", bytes(binds("Main", "Keyboard", "Key_A"))));
        readings.current = readings.applied;

        BindForgeSupport.Contents contents = BindForgeSupport.collect(readings);

        assertTrue(contents.report().contains("Main.4.2.binds: BOTH"), contents.report());
        assertTrue(contents.report().contains("Key_A"), "the working copy's side: " + contents.report());
        assertTrue(contents.report().contains("Key_B"), "the game's side: " + contents.report());
        assertTrue(contents.files().containsKey("bindforge/live/Bindings/Main.4.2.binds"),
                "named by no preset line, but it differs");
    }

    @Test
    void aMatchingFolderSaysSo() throws IOException {
        live("Main.4.2.binds", binds("Main", "Keyboard", "Key_A"));
        readings.applied = set(Map.of("Main.4.2.binds", bytes(binds("Main", "Keyboard", "Key_A"))));
        readings.current = readings.applied;

        String report = BindForgeSupport.collect(readings).report();

        assertTrue(report.contains("no differences"), report);
        assertTrue(report.contains("Edits not applied: none"), report);
    }

    @Test
    void editsNotAppliedAreCountedWhetherOrNotTheyTouchDeviceFiles() {
        readings.applied = set(Map.of("Main.4.2.binds", bytes(binds("Main", "Keyboard", "Key_A"))));
        readings.current = set(Map.of("Main.4.2.binds", bytes(binds("Main", "Keyboard", "Key_Z"))));

        String report = BindForgeSupport.collect(readings).report();

        assertTrue(report.contains("Edits not applied: 1 rows"), report);
        assertTrue(report.contains("device files unchanged"), report);
    }

    @Test
    void installationsAreStatedAndOnlyTheOnesThatDifferCarried() {
        InstallationRow steam = row(1, "STEAM");
        InstallationRow epic = row(2, "EPIC");
        InstallationRow frontier = row(3, "FRONTIER");
        InstallationRow removed = row(4, "MANUAL");
        readings.listed = List.of(steam, epic, frontier);
        readings.all = List.of(steam, epic, frontier, removed);
        DeviceFiles files = new DeviceFiles(bytes(DEVICE_MAPPINGS), Map.of());
        readings.found = new DeviceCheck.Found(List.of(
                new DeviceCheck.Judged(steam, DeviceCheck.State.MATCHES, files, null),
                new DeviceCheck.Judged(epic, DeviceCheck.State.DIFFERS, files, null)));

        BindForgeSupport.Contents contents = BindForgeSupport.collect(readings);

        assertTrue(contents.report().contains("STEAM " + steam.rootPath() + ": MATCHES"), contents.report());
        assertTrue(contents.report().contains("EPIC " + epic.rootPath() + ": DIFFERS"), contents.report());
        assertTrue(contents.report().contains("FRONTIER " + frontier.rootPath() + ": not judged, not reachable"),
                contents.report());
        assertTrue(contents.report().contains(", removed by the user"), contents.report());
        assertFalse(contents.files().containsKey("bindforge/installations/STEAM-1/DeviceMappings.xml"));
        assertTrue(contents.files().containsKey("bindforge/installations/EPIC-2/DeviceMappings.xml"));
        assertTrue(contents.files().containsKey("bindforge/working-copy/DeviceMappings.xml"));
    }

    @Test
    void aControllerIsStatedWithHowItsHardwareWasRead() {
        readings.attached = new BindForgeSupport.Attached(
                List.of(new Device(1, "VPC Stick", 6, 32, "usb-1", LVWAP_GUID, false)), true, true);

        String report = BindForgeSupport.collect(readings).report();

        assertTrue(report.contains("VPC Stick [3344:83F4] GUID " + LVWAP_GUID + ", 6 axes, 32 buttons"), report);
        assertTrue(report.contains("working copy names it LVWAP"), "matched to its entry: " + report);
        assertTrue(report.contains("Missing, with bindings naming them: none"), report);
    }

    @Test
    void aNamedControllerThatIsNotAttachedIsMissingWhenBindingsNameIt() throws IOException {
        live("Main.4.2.binds", binds("Main", "LVWAP", "Joy_1"));
        readings.attached = new BindForgeSupport.Attached(List.of(), true, true);

        String report = BindForgeSupport.collect(readings).report();

        assertTrue(report.contains("Missing, with bindings naming them: LVWAP (1)"), report);
        assertTrue(report.contains("LVWAP: 1"), report);
        assertFalse(report.contains("no entry for it"), "it has one: " + report);
    }

    @Test
    void missingIsNotJudgedBeforeTheControllersAreListed() throws IOException {
        live("Main.4.2.binds", binds("Main", "LVWAP", "Joy_1"));
        readings.attached = new BindForgeSupport.Attached(List.of(), true, false);

        String report = BindForgeSupport.collect(readings).report();

        assertTrue(report.contains("Missing: not judged"), report);
    }

    @Test
    void aNameBindingsUseWithNoEntryIsFlagged() throws IOException {
        live("Main.4.2.binds", binds("Main", "GoneStick", "Joy_1"));

        String report = BindForgeSupport.collect(readings).report();

        assertTrue(report.contains("GoneStick: 1 *** no entry for it"), report);
    }

    @Test
    void backupsAreListedNewestFirstWithAPartialOneCalledOut() throws IOException {
        backup("BeforeChanges", "2026-11-01_10-00-00", "Kind: Before changes\nTaken before: APPLY\nStatus: Full\n");
        backup("Daily", "2026-11-02_10-00-00", "Kind: Daily\nStatus: Partial\nCould not be read:\n- D:\\Steam gone\n");

        String report = BindForgeSupport.collect(readings).report();

        int partial = report.indexOf("2026-11-02 10:00:00 DAILY, PARTIAL");
        int apply = report.indexOf("2026-11-01 10:00:00 BEFORE_CHANGES before APPLY, full");
        assertTrue(partial >= 0 && apply > partial, report);
        assertTrue(report.contains("not read: D:\\Steam gone"), report);
        assertTrue(report.contains("Kept: daily for 90 days, the newest 50 before-changes"), report);
        assertTrue(report.contains("(the default)"), report);
    }

    @Test
    void aChosenBackupFolderThatFellBackIsCalledOut() {
        readings.chosen = tmp.resolve("gone").toString();

        String report = BindForgeSupport.collect(readings).report();

        assertTrue(report.contains("*** the chosen " + readings.chosen + " is not usable ***"), report);
    }

    @Test
    void identityAnswersAreStated() {
        readings.answers = List.of(new AnswerRow("LVWAP", "3344:83F4|3344:03F5", "3344:83F4"));

        String report = BindForgeSupport.collect(readings).report();

        assertTrue(report.contains("LVWAP (3344:83F4|3344:03F5): 3344:83F4"), report);
    }

    @Test
    void oneSectionThatFailsCostsOnlyThatSection() throws IOException {
        live("StartPreset.4.start", "Main");
        live("Main.4.2.binds", binds("Main", "Keyboard", "Key_A"));
        readings.judgeFailure = new IOException("the drive went away");
        readings.attachedFailure = new UnsatisfiedLinkError("no SDL3");

        BindForgeSupport.Contents contents = BindForgeSupport.collect(readings);

        assertTrue(contents.report().contains("could not be read: java.io.IOException: the drive went away"),
                contents.report());
        assertTrue(contents.report().contains("no SDL3"), contents.report());
        assertTrue(contents.report().contains("GENERAL: Main -> Main.4.2.binds"), contents.report());
        assertTrue(contents.files().containsKey("bindforge/live/Bindings/Main.4.2.binds"));
    }

    @Test
    void noBindingsFolderConfiguredIsWrittenRatherThanThrown() {
        readings.bindings = null;

        String report = BindForgeSupport.collect(readings).report();

        assertTrue(report.contains("no bindings folder is configured"), report);
    }

    // ------------------------------------------------------------------ fixtures

    private void live(String name, String content) throws IOException {
        Files.writeString(readings.bindings.resolve(name), content, StandardCharsets.UTF_8);
    }

    private void backup(String kindFolder, String stamp, String info) throws IOException {
        Path folder = Files.createDirectories(readings.backups.resolve(kindFolder));
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(folder.resolve(stamp + ".zip")))) {
            zip.putNextEntry(new ZipEntry(Backups.INFO_FILE));
            zip.write(bytes(info));
            zip.closeEntry();
        }
    }

    private InstallationRow row(long id, String storefront) {
        return new InstallationRow(id, storefront, tmp.resolve(storefront).toString(), false, false);
    }

    private static String binds(String preset, String device, String key) {
        return "<Root PresetName=\"" + preset + "\" MajorVersion=\"4\" MinorVersion=\"2\">"
                + "<Fire><Primary Device=\"" + device + "\" Key=\"" + key + "\" />"
                + "<Secondary Device=\"{NoDevice}\" Key=\"\" /></Fire></Root>";
    }

    private static ConfigurationFiles set(Map<String, byte[]> bindings) {
        return new ConfigurationFiles(bindings, bytes(DEVICE_MAPPINGS), Map.of());
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static final class Fake implements BindForgeSupport.Readings {
        boolean complete = true;
        ConfigurationFiles applied = BindForgeSupportTest.set(Map.of());
        ConfigurationFiles current = applied;
        Path bindings;
        final Path backups;
        List<InstallationRow> listed = new ArrayList<>();
        List<InstallationRow> all = new ArrayList<>();
        DeviceCheck.Found found = new DeviceCheck.Found(List.of());
        IOException judgeFailure;
        DeviceFiles deviceFiles;
        BindForgeSupport.Attached attached = new BindForgeSupport.Attached(List.of(), false, false);
        LinkageError attachedFailure;
        String chosen;
        List<AnswerRow> answers = List.of();

        Fake(Path bindings, Path backups) {
            this.bindings = bindings;
            this.backups = backups;
        }

        @Override
        public boolean firstSetupComplete() {
            return complete;
        }

        @Override
        public Edits.Snapshot set() {
            assertTrue(complete, "the working copy is not read before first setup");
            return new Edits.Snapshot(current, applied);
        }

        @Override
        public Path bindingsFolder() {
            return bindings;
        }

        @Override
        public List<InstallationRow> installations() {
            return listed;
        }

        @Override
        public List<InstallationRow> installationsIncludingRemoved() {
            return all;
        }

        @Override
        public DeviceCheck.Found judge(ConfigurationFiles workingCopy) throws IOException {
            if (judgeFailure != null) throw judgeFailure;
            return found;
        }

        @Override
        public Optional<DeviceFiles> deviceFiles(InstallationRow row) {
            return Optional.ofNullable(deviceFiles);
        }

        @Override
        public BindForgeSupport.Attached attached() {
            if (attachedFailure != null) throw attachedFailure;
            return attached;
        }

        @Override
        public Path backupFolder() {
            return backups;
        }

        @Override
        public String chosenBackupFolder() {
            return chosen;
        }

        @Override
        public int dailyBackupDays() {
            return 90;
        }

        @Override
        public int beforeChangesBackupsKept() {
            return 50;
        }

        @Override
        public List<AnswerRow> identityAnswers() {
            return answers;
        }
    }
}
