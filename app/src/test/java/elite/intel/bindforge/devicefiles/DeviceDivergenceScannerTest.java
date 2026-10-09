package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceDivergence.Finding;
import elite.intel.bindforge.devicefiles.DeviceDivergence.Issue;
import elite.intel.bindforge.devicefiles.DeviceDivergence.Severity;
import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.bindforge.devicefiles.DeviceFilesCheck.ElementChange;
import elite.intel.bindforge.devicefiles.DeviceFilesCheck.ElementKind;
import elite.intel.bindforge.devicefiles.DeviceFilesCheck.InstallationCheck;
import elite.intel.bindforge.devicefiles.DeviceFilesCheck.Report;
import elite.intel.bindforge.devicefiles.DeviceFilesCheck.State;
import elite.intel.bindforge.devicefiles.DeviceFilesPush.Target;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The scanner reads real folders, so its tests build real ones. It is also where the readings are joined,
 * which makes it the place a bad file can do the most damage.
 */
class DeviceDivergenceScannerTest {

    @TempDir
    Path machine;

    private Path bindings;

    @BeforeEach
    void layOutTheMachine() throws IOException {
        bindings = Files.createDirectories(machine.resolve("Bindings"));
    }

    @Test
    void installationsHoldingTheSameEntriesReportNothing() throws IOException {
        Path steam = install("steam", "<Root><RVWAP><PID>83F4</PID><VID>3344</VID></RVWAP></Root>");
        Path epic = install("epic", "<Root><RVWAP><PID>83F4</PID><VID>3344</VID></RVWAP></Root>");
        binds("<Root><A><Primary Device=\"RVWAP\" Key=\"Joy_1\" /></A></Root>");

        assertTrue(DeviceDivergenceScanner.scan(Map.of("steam", steam, "epic", epic), bindings).isEmpty());
    }

    @Test
    void anEntryMissingFromOneInstallationIsRedWhenTheBindingsNameIt() throws IOException {
        Path steam = install("steam", "<Root><RVWAP><PID>83F4</PID><VID>3344</VID></RVWAP></Root>");
        Path epic = install("epic", "<Root></Root>");
        binds("<Root><A><Primary Device=\"RVWAP\" Key=\"Joy_1\" /></A></Root>");

        List<Finding> findings = DeviceDivergenceScanner.scan(Map.of("steam", steam, "epic", epic), bindings);

        assertEquals(1, findings.size());
        assertEquals(Severity.RED, findings.get(0).severity());
        assertEquals("RVWAP", findings.get(0).deviceName());
    }

    /**
     * The defect this pins: an unreadable file used to enter the comparison as an installation with no
     * entries, so every entry the others held was reported missing from it. One bad file produced a screen
     * full of red that described nothing real.
     */
    @Test
    void anInstallationWhoseFileCannotBeReadIsLeftOutRatherThanTreatedAsEmpty() throws IOException {
        Path steam = install("steam", """
                <Root>
                  <RVWAP><PID>83F4</PID><VID>3344</VID></RVWAP>
                  <LVWAP><PID>83F3</PID><VID>3344</VID></LVWAP>
                </Root>
                """);
        Path broken = install("epic", "this file is not xml at all");
        binds("<Root><A><Primary Device=\"RVWAP\" Key=\"Joy_1\" /></A></Root>");

        List<Finding> findings = DeviceDivergenceScanner.scan(Map.of("steam", steam, "epic", broken), bindings);

        assertTrue(findings.isEmpty(),
                "one unreadable file must not report every other installation's entries as missing from it");
    }

    /**
     * An installation that is read and genuinely holds no entries is a different thing, and does differ from
     * the others.
     */
    @Test
    void anInstallationThatReallyHasNoEntriesStillCounts() throws IOException {
        Path steam = install("steam", "<Root><RVWAP><PID>83F4</PID><VID>3344</VID></RVWAP></Root>");
        Path epic = install("epic", "<Root></Root>");
        binds("<Root><A><Primary Device=\"RVWAP\" Key=\"Joy_1\" /></A></Root>");

        List<Finding> findings = DeviceDivergenceScanner.scan(Map.of("steam", steam, "epic", epic), bindings);

        assertEquals(1, findings.size(), "empty is not the same as unreadable");
    }

    @Test
    void anOrphanedButtonMapIsFoundAndIsYellow() throws IOException {
        Path steam = install("steam", "<Root><RVWAP><PID>83F4</PID><VID>3344</VID></RVWAP></Root>");
        Path epic = install("epic", "<Root><RVWAP><PID>83F4</PID><VID>3344</VID></RVWAP></Root>");
        Files.writeString(Files.createDirectories(epic.resolve("DeviceButtonMaps"))
                .resolve("LVWAP.buttonMap"), "<Root></Root>");
        binds("<Root><A><Primary Device=\"RVWAP\" Key=\"Joy_1\" /></A></Root>");

        List<Finding> findings = DeviceDivergenceScanner.scan(Map.of("steam", steam, "epic", epic), bindings);

        assertEquals(1, findings.size());
        assertEquals(Issue.ORPHANED_BUTTON_MAP, findings.get(0).issue());
        assertEquals(Severity.YELLOW, findings.get(0).severity());
    }

    /**
     * With nothing readable to judge against, everything falls to yellow rather than guessing that it breaks.
     */
    @Test
    void withNoBindingsToJudgeAgainstNothingIsRed() throws IOException {
        Path steam = install("steam", "<Root><RVWAP><PID>83F4</PID><VID>3344</VID></RVWAP></Root>");
        Path epic = install("epic", "<Root></Root>");

        List<Finding> findings = DeviceDivergenceScanner.scan(Map.of("steam", steam, "epic", epic), bindings);

        assertEquals(Severity.YELLOW, findings.get(0).severity());
    }

    /** Against the master, the check's findings are joined by the orphans read from disk and judged by the bindings. */
    @Test
    void againstTheMasterAnOrphanBesideAnEditedInstallationIsFoundAndAMissingNamedEntryIsRed() throws IOException {
        Path steam = install("steam", "<Root><LVWAP><PID>83F4</PID><VID>3344</VID></LVWAP></Root>");
        Files.writeString(Files.createDirectories(steam.resolve("DeviceButtonMaps"))
                .resolve("Old.buttonMap"), "<Root></Root>");
        binds("<Root><A><Primary Device=\"RVWAP\" Key=\"Joy_1\" /></A></Root>");
        Report report = new Report(true, Set.of("RVWAP", "LVWAP"), Set.of("RVWAP", "LVWAP"),
                List.of(new InstallationCheck(target(1, steam), State.EDITED, null,
                        List.of(new ElementChange("RVWAP", ElementKind.MISSING, new HardwareId("3344", "03F5"), null)),
                        List.of())));

        List<Finding> findings = DeviceDivergenceScanner.scanAgainstMaster(report, bindings);

        assertEquals(List.of(Issue.ENTRY_MISSING, Issue.ORPHANED_BUTTON_MAP),
                findings.stream().map(Finding::issue).toList());
        assertEquals(Severity.RED, findings.get(0).severity());
        assertEquals(Set.of("1"), findings.get(1).installs());
    }

    /**
     * A reset installation's maps are orphans of the entries it lost; its one row says so, and listing each map
     * again would bury it.
     */
    @Test
    void againstTheMasterAResetInstallationsMapsAreNotListedAsOrphans() throws IOException {
        Path epic = install("epic", "<Root><GamePad><PID>028E</PID><VID>045E</VID></GamePad></Root>");
        Files.writeString(Files.createDirectories(epic.resolve("DeviceButtonMaps"))
                .resolve("RVWAP.buttonMap"), "<Root></Root>");
        Report report = new Report(true, Set.of("RVWAP"), Set.of("RVWAP"), List.of(
                new InstallationCheck(target(2, epic), State.WIPED, null, List.of(), List.of())));

        List<Finding> findings = DeviceDivergenceScanner.scanAgainstMaster(report, bindings);

        assertEquals(List.of(Issue.FILE_RESET), findings.stream().map(Finding::issue).toList());
    }

    private static Target target(long id, Path controlSchemes) {
        return new Target(id, "STEAM", controlSchemes, false);
    }

    /** Creates an installation's ControlSchemes folder holding the given DeviceMappings.xml. */
    private Path install(String name, String deviceMappings) throws IOException {
        Path controlSchemes = Files.createDirectories(machine.resolve(name).resolve("ControlSchemes"));
        Files.writeString(controlSchemes.resolve("DeviceMappings.xml"), deviceMappings);
        return controlSchemes;
    }

    private void binds(String content) throws IOException {
        Files.writeString(bindings.resolve("Custom.4.2.binds"), content);
    }
}
