package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceFilesPush.*;
import elite.intel.io.TimestampedBackups;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Apply for the device files: every installation brought to the master, each one reported, none left half
 * written, and nothing replaced without a copy kept.
 */
class DeviceFilesPushTest {

    private static final String DEVICE_MAPPINGS = """
            <?xml version="1.0" encoding="UTF-8" ?>
            <Root>
            \t<GamePad>
            \t\t<PID>028E</PID><VID>045E</VID>
            \t\t<!-- XB1 Controller -->
            \t</GamePad>
            \t<T-Rudder><PID>B679</PID><VID>044F</VID></T-Rudder>
            </Root>
            """;

    private static final MasterDevice RVWAP =
            new MasterDevice("RVWAP", "3344", "43F4", Map.of("Joy_1", "RV Main Trigger"));

    @TempDir
    Path temp;

    private Path history;
    private final List<Target> rescanned = new ArrayList<>();

    @BeforeEach
    void historyFolder() {
        history = temp.resolve("history");
    }

    @Test
    void everyInstallationIsBroughtToTheMasterAndRescanned() throws IOException {
        Target steam = installation(1, "STEAM");
        Target epic = installation(2, "EPIC");

        Report report = push(List.of(steam, epic), List.of(RVWAP));

        assertEquals(2, report.matchingCount());
        for (Target target : List.of(steam, epic)) {
            assertEquals(Outcome.WRITTEN, resultFor(report, target).outcome());
            assertTrue(read(target.controlSchemes().resolve("DeviceMappings.xml")).contains("<RVWAP>"));
            assertTrue(read(buttonMap(target, "RVWAP")).contains("<Joy_1>RV Main Trigger</Joy_1>"));
        }
        assertEquals(List.of(steam, epic), rescanned);
    }

    /** "Updated 2 of 3" rather than nothing: the installation that cannot be reached is named, not fatal. */
    @Test
    void aMissingInstallationIsSkippedAndNamedWhileTheOthersAreWritten() throws IOException {
        Target steam = installation(1, "STEAM");
        Target gone = new Target(2, "EPIC", temp.resolve("unplugged").resolve("ControlSchemes"), false);

        Report report = push(List.of(steam, gone), List.of(RVWAP));

        assertEquals(1, report.matchingCount());
        InstallationResult skipped = resultFor(report, gone);
        assertEquals(Outcome.SKIPPED_MISSING, skipped.outcome());
        assertTrue(skipped.reason().contains("folder not found"), skipped.reason());
        assertEquals(List.of(steam), rescanned);
    }

    /** Without the file the game loads no bindings at all, so it is rebuilt from Frontier's own (2026-10-04). */
    @Test
    void anInstallationWithNoDeviceMappingsIsSeededFromFrontiersStockFile() throws IOException {
        Target steam = installation(1, "STEAM");
        Files.delete(steam.controlSchemes().resolve("DeviceMappings.xml"));

        InstallationResult result = resultFor(push(List.of(steam), List.of(RVWAP)), steam);

        assertEquals(Outcome.WRITTEN, result.outcome());
        assertTrue(result.seededFromStock());
        List<DeviceEntry> entries = DeviceMappingsParser.parse(steam.controlSchemes().resolve("DeviceMappings.xml"));
        assertEquals("RVWAP", entries.getFirst().name());
        assertEquals(FrontierStockDevices.getInstance().names().size() + 1, entries.size());
    }

    @Test
    void aSecondPushWithNothingNewWritesNothing() throws IOException {
        Target steam = installation(1, "STEAM");
        push(List.of(steam), List.of(RVWAP));
        rescanned.clear();
        long historyCopies = historyCopies();

        InstallationResult again = resultFor(push(List.of(steam), List.of(RVWAP)), steam);

        assertEquals(Outcome.UNCHANGED, again.outcome());
        assertEquals(historyCopies, historyCopies());
        assertTrue(rescanned.isEmpty());
    }

    /**
     * What is replaced is kept, once: two installations holding the same file before the push give one copy,
     * not two - after standardisation an entry per installation would be identical copies of one file.
     */
    @Test
    void identicalContentIsKeptInEditHistoryOncePerPush() throws IOException {
        Target steam = installation(1, "STEAM");
        Target epic = installation(2, "EPIC");

        push(List.of(steam, epic), List.of(RVWAP));

        List<Path> copies = historyFiles();
        assertEquals(1, copies.size(), copies.toString());
        assertEquals(DEVICE_MAPPINGS, read(copies.getFirst()));
        assertTrue(copies.getFirst().startsWith(history.resolve("1")), copies.toString());
    }

    @Test
    void installationsThatDisagreeEachKeepTheirOwnCopy() throws IOException {
        Target steam = installation(1, "STEAM");
        Target epic = installation(2, "EPIC");
        Files.writeString(epic.controlSchemes().resolve("DeviceMappings.xml"),
                DEVICE_MAPPINGS.replace("<!-- XB1 Controller -->", "<!-- edited in Epic -->"));

        push(List.of(steam, epic), List.of(RVWAP));

        assertEquals(2, historyFiles().size());
    }

    /**
     * A built-in under Frontier's own name carries labels only. Its element is Frontier's definition and is
     * neither moved nor rewritten; its {@code .buttonMap} is still written.
     */
    @Test
    void anUnrenamedBuiltInGetsItsButtonMapAndItsElementIsLeftAlone() throws IOException {
        Target steam = installation(1, "STEAM");
        MasterDevice rudder = new MasterDevice("T-Rudder", "044F", "B679", Map.of("Joy_RZAxis", "Rudder"));

        InstallationResult result = resultFor(push(List.of(steam), List.of(rudder)), steam);

        assertEquals(Outcome.WRITTEN, result.outcome());
        assertEquals(List.of(buttonMap(steam, "T-Rudder")), result.written());
        assertEquals(DEVICE_MAPPINGS, read(steam.controlSchemes().resolve("DeviceMappings.xml")));
    }

    @Test
    void aDeviceWithNoLabelsGetsNoButtonMap() throws IOException {
        Target steam = installation(1, "STEAM");

        push(List.of(steam), List.of(new MasterDevice("RVWAP", "3344", "43F4", Map.of())));

        assertFalse(Files.exists(buttonMap(steam, "RVWAP")));
    }

    /**
     * One installation is all or nothing: its {@code DeviceMappings.xml} is written first, the {@code .buttonMap}
     * then fails, and the first is put back. The other installation is unaffected.
     */
    @Test
    void aFailedWriteRestoresThatInstallationAndLeavesTheOthersWritten() throws IOException {
        Target steam = installation(1, "STEAM");
        Target epic = installation(2, "EPIC");
        Path blocked = buttonMap(epic, "RVWAP");
        Files.createDirectories(blocked);
        Files.writeString(blocked.resolve("in-the-way.txt"), "a folder where the file should go");

        Report report = push(List.of(steam, epic), List.of(RVWAP));

        InstallationResult failed = resultFor(report, epic);
        assertEquals(Outcome.FAILED, failed.outcome());
        assertTrue(failed.reason().contains("RVWAP.buttonMap"), failed.reason());
        assertEquals(DEVICE_MAPPINGS, read(epic.controlSchemes().resolve("DeviceMappings.xml")));
        assertEquals(Outcome.WRITTEN, resultFor(report, steam).outcome());
        assertEquals(List.of(steam), rescanned);
    }

    @Test
    void aFileThatCannotBeReadFailsThatInstallationWithoutWritingIt() throws IOException {
        Target steam = installation(1, "STEAM");
        Path deviceMappings = steam.controlSchemes().resolve("DeviceMappings.xml");
        Files.writeString(deviceMappings, "<Root><unclosed></Root>");
        byte[] before = Files.readAllBytes(deviceMappings);

        InstallationResult result = resultFor(push(List.of(steam), List.of(RVWAP)), steam);

        assertEquals(Outcome.FAILED, result.outcome());
        assertArrayEquals(before, Files.readAllBytes(deviceMappings));
        assertFalse(Files.exists(buttonMap(steam, "RVWAP")));
        assertFalse(result.seededFromStock());
    }

    /** Revert: the master pushed to one installation, and no other installation touched (Alan, 2026-10-05). */
    @Test
    void revertPushesTheMasterToThatInstallationAlone() throws IOException {
        Target steam = installation(1, "STEAM");
        Target epic = installation(2, "EPIC");

        Report report = pusher(List.of(steam, epic), List.of(RVWAP)).push(2);

        assertEquals(1, report.installations().size());
        assertEquals(Outcome.WRITTEN, resultFor(report, epic).outcome());
        assertTrue(read(epic.controlSchemes().resolve("DeviceMappings.xml")).contains("<RVWAP>"));
        assertEquals(DEVICE_MAPPINGS, read(steam.controlSchemes().resolve("DeviceMappings.xml")));
        assertEquals(List.of(epic), rescanned);
    }

    /**
     * Catch-up: Apply's push - removals included - to the installations named, and no other installation touched
     * (Alan, 2026-10-15, A14).
     */
    @Test
    void catchUpPushesWithRemovalsToTheNamedInstallationAlone() throws IOException {
        Target steam = installation(1, "STEAM");
        Target epic = installation(2, "EPIC");
        push(List.of(steam, epic), List.of(RVWAP));
        String steamBefore = read(steam.controlSchemes().resolve("DeviceMappings.xml"));
        rescanned.clear();

        Report report = pusher(List.of(steam, epic), List.of()).push(List.of(2L), Set.of("RVWAP"));

        assertEquals(1, report.installations().size());
        assertEquals(Outcome.WRITTEN, resultFor(report, epic).outcome());
        assertFalse(read(epic.controlSchemes().resolve("DeviceMappings.xml")).contains("RVWAP"));
        assertEquals(steamBefore, read(steam.controlSchemes().resolve("DeviceMappings.xml")));
        assertEquals(List.of(epic), rescanned);
    }

    /** A missed installation is marked by any push; only Apply's push and catch-up clear it, never revert. */
    @Test
    void missedWritesAreRecordedByTheKindOfPush() throws IOException {
        Target steam = new Target(1, "STEAM", installation(1, "STEAM").controlSchemes(), false, true);
        Target away = new Target(2, "EPIC", temp.resolve("gone"), false);
        Map<Long, Boolean> stored = new HashMap<>();
        DeviceFilesPush push = new DeviceFilesPush(() -> List.of(steam, away), () -> List.of(RVWAP),
                FrontierStockDevices.getInstance(), new TimestampedBackups(), history, () -> 10, rescanned::add,
                new MissedWrites(stored::put));

        push.push(1);
        assertEquals(Map.of(), stored, "revert landing clears nothing");

        push.push(Set.of());
        assertEquals(Map.of(1L, false, 2L, true), stored, "Apply landing clears, a skip marks");

        stored.clear();
        push.push(List.of(1L), Set.of());
        assertEquals(Map.of(1L, false), stored, "catch-up landing clears");
    }

    @Test
    void revertingAnInstallationThatIsNotStoredReportsNothing() throws IOException {
        Target steam = installation(1, "STEAM");

        assertTrue(pusher(List.of(steam), List.of(RVWAP)).push(9).installations().isEmpty());
        assertEquals(DEVICE_MAPPINGS, read(steam.controlSchemes().resolve("DeviceMappings.xml")));
    }

    /** CLEAR, applied: the entry and its {@code .buttonMap} leave every installation, and both are kept first. */
    @Test
    void aClearedDeviceLosesItsEntryAndButtonMapInEveryInstallation() throws IOException {
        Target steam = installation(1, "STEAM");
        Target epic = installation(2, "EPIC");
        push(List.of(steam, epic), List.of(RVWAP));

        Report report = pusher(List.of(steam, epic), List.of()).push(Set.of("RVWAP"));

        assertEquals(2, report.matchingCount());
        for (Target target : List.of(steam, epic)) {
            String deviceMappings = read(target.controlSchemes().resolve("DeviceMappings.xml"));
            assertFalse(deviceMappings.contains("RVWAP"), deviceMappings);
            assertTrue(deviceMappings.contains("<!-- XB1 Controller -->"), "everything else in the file is kept");
            assertFalse(Files.exists(buttonMap(target, "RVWAP")));
        }
        assertTrue(historyFiles().stream().anyMatch(file -> file.toString().contains("RVWAP")),
                "the removed .buttonMap is in Edit History");
    }

    /** RESET LABELS on a built-in: its {@code .buttonMap} goes, and Frontier's element stays exactly where it was. */
    @Test
    void aResetBuiltInLosesOnlyItsButtonMap() throws IOException {
        Target steam = installation(1, "STEAM");
        writeButtonMap(steam, "T-Rudder", "<Root><Joy_XAxis>RUDDER</Joy_XAxis></Root>");

        InstallationResult result = resultFor(pusher(List.of(steam), List.of()).push(Set.of("T-Rudder")), steam);

        assertEquals(Outcome.WRITTEN, result.outcome());
        assertEquals(DEVICE_MAPPINGS, read(steam.controlSchemes().resolve("DeviceMappings.xml")));
        assertFalse(Files.exists(buttonMap(steam, "T-Rudder")));
    }

    /** A map Frontier ships is never removed: RESET LABELS puts Frontier's file back (Alan, 2026-10-11). */
    @Test
    void resettingAMapFrontierShipsPutsFrontiersFileBack() throws IOException {
        Target steam = installation(1, "STEAM");
        writeButtonMap(steam, "VPCPanel", "<Root><Joy_1>MY LABEL</Joy_1></Root>");

        pusher(List.of(steam), List.of()).push(Set.of("VPCPanel"));

        assertArrayEquals(FrontierStockButtonMaps.getInstance().shippedContent("VPCPanel").orElseThrow(),
                Files.readAllBytes(buttonMap(steam, "VPCPanel")));
    }

    @Test
    void aRemovalAlreadyDoneWritesNothing() throws IOException {
        Target steam = installation(1, "STEAM");

        InstallationResult result = resultFor(pusher(List.of(steam), List.of()).push(Set.of("RVWAP")), steam);

        assertEquals(Outcome.UNCHANGED, result.outcome());
        assertEquals(0, historyCopies());
    }

    /** Revert removes nothing: an entry the master no longer holds stays until Apply removes it. */
    @Test
    void revertRemovesNothing() throws IOException {
        Target steam = installation(1, "STEAM");
        push(List.of(steam), List.of(RVWAP));

        pusher(List.of(steam), List.of()).push(1);

        assertTrue(read(steam.controlSchemes().resolve("DeviceMappings.xml")).contains("<RVWAP>"));
        assertTrue(Files.exists(buttonMap(steam, "RVWAP")));
    }

    /**
     * An XML tag may hold a colon, which no Windows filename can. A labelled master device under such a name fails
     * each installation with a reason, writes nothing there, and does not stop the push reaching the others.
     */
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void aNameThatCannotBeAFilenameFailsEachInstallationWithoutStoppingThePush() throws IOException {
        Target steam = installation(1, "STEAM");
        Target epic = installation(2, "EPIC");
        MasterDevice colon = new MasterDevice("a:b", "3344", "43F4", Map.of("Joy_1", "Trigger"));

        Report report = push(List.of(steam, epic), List.of(colon));

        assertEquals(2, report.installations().size());
        for (Target target : List.of(steam, epic)) {
            InstallationResult result = resultFor(report, target);
            assertEquals(Outcome.FAILED, result.outcome());
            assertTrue(result.reason().contains("cannot be a filename"), result.reason());
            assertEquals(DEVICE_MAPPINGS, read(target.controlSchemes().resolve("DeviceMappings.xml")));
        }
    }

    // ------------------------------------------------------------------ rename (A11)

    /** The element is renamed in place and the .buttonMap is renamed - never a second file beside the first. */
    @Test
    void aRenameRenamesTheElementAndTheButtonMapInEveryInstallation() throws IOException {
        Target steam = installation(1, "STEAM");
        Target epic = installation(2, "EPIC");
        push(List.of(steam, epic), List.of(RVWAP));

        Report report = push(List.of(steam, epic), List.of(renamed("RightStick", RVWAP.labels())));

        assertEquals(2, report.matchingCount());
        for (Target target : List.of(steam, epic)) {
            String mappings = read(target.controlSchemes().resolve("DeviceMappings.xml"));
            assertTrue(mappings.contains("<RightStick>"), mappings);
            assertFalse(mappings.contains("RVWAP"), mappings);
            assertTrue(read(buttonMap(target, "RightStick")).contains("<Joy_1>RV Main Trigger</Joy_1>"));
            assertFalse(Files.exists(buttonMap(target, "RVWAP")), "no orphan left under the old name");
        }
    }

    /** With no labels in the master the old file's own content goes to the new name - renamed, not dropped. */
    @Test
    void aRenameOfADeviceWithNoLabelsMovesItsButtonMap() throws IOException {
        Target steam = installation(1, "STEAM");
        push(List.of(steam), List.of(RVWAP));
        String before = read(buttonMap(steam, "RVWAP"));

        push(List.of(steam), List.of(renamed("RightStick", Map.of())));

        assertEquals(before, read(buttonMap(steam, "RightStick")));
        assertFalse(Files.exists(buttonMap(steam, "RVWAP")));
    }

    /** The push renames wherever the old name is still found, so an installation that already took it is unchanged. */
    @Test
    void aRenameAnInstallationAlreadyTookWritesNothing() throws IOException {
        Target steam = installation(1, "STEAM");
        push(List.of(steam), List.of(RVWAP));
        push(List.of(steam), List.of(renamed("RightStick", RVWAP.labels())));

        Report again = push(List.of(steam), List.of(renamed("RightStick", RVWAP.labels())));

        assertEquals(Outcome.UNCHANGED, resultFor(again, steam).outcome());
    }

    /** On Windows, LVWAP.buttonMap and Lvwap.buttonMap are one file: a case-only rename must not delete it. */
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void aCaseOnlyRenameKeepsTheButtonMapOnWindows() throws IOException {
        Target steam = installation(1, "STEAM");
        push(List.of(steam), List.of(RVWAP));

        Report report = push(List.of(steam), List.of(renamed("Rvwap", RVWAP.labels())));

        assertEquals(1, report.matchingCount());
        assertTrue(read(steam.controlSchemes().resolve("DeviceMappings.xml")).contains("<Rvwap>"));
        assertTrue(read(buttonMap(steam, "Rvwap")).contains("<Joy_1>RV Main Trigger</Joy_1>"));
    }

    /** On a file system that keeps case apart, a case-only rename is a real rename: old file gone, new one written. */
    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void aCaseOnlyRenameRenamesTheButtonMapWhereCaseMatters() throws IOException {
        Target steam = installation(1, "STEAM");
        push(List.of(steam), List.of(RVWAP));

        push(List.of(steam), List.of(renamed("Rvwap", RVWAP.labels())));

        assertTrue(read(buttonMap(steam, "Rvwap")).contains("<Joy_1>RV Main Trigger</Joy_1>"));
        assertFalse(Files.exists(buttonMap(steam, "RVWAP")));
    }

    /** An installation with no DeviceMappings.xml is seeded from Frontier's: nothing to rename, the new name added. */
    @Test
    void aRenameInAnInstallationWithNoDeviceMappingsAddsTheNewName() throws IOException {
        Target steam = installation(1, "STEAM");
        Files.delete(steam.controlSchemes().resolve("DeviceMappings.xml"));

        Report report = push(List.of(steam), List.of(renamed("RightStick", RVWAP.labels())));

        assertEquals(Outcome.WRITTEN, resultFor(report, steam).outcome());
        String mappings = read(steam.controlSchemes().resolve("DeviceMappings.xml"));
        assertTrue(mappings.contains("<RightStick>"), mappings);
        assertFalse(mappings.contains("<RVWAP>"), mappings);
    }

    private static MasterDevice renamed(String name, Map<String, String> labels) {
        return new MasterDevice(name, RVWAP.vid(), RVWAP.pid(), labels, RVWAP.name());
    }

    private Report push(List<Target> targets, List<MasterDevice> master) {
        return pusher(targets, master).push();
    }

    private static void writeButtonMap(Target target, String device, String content) throws IOException {
        Path file = buttonMap(target, device);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    private DeviceFilesPush pusher(List<Target> targets, List<MasterDevice> master) {
        return new DeviceFilesPush(() -> targets, () -> master, FrontierStockDevices.getInstance(),
                new TimestampedBackups(), history, () -> 10, rescanned::add);
    }

    private Target installation(long id, String storefront) throws IOException {
        Path controlSchemes = temp.resolve(storefront).resolve("ControlSchemes");
        Files.createDirectories(controlSchemes);
        Files.writeString(controlSchemes.resolve("DeviceMappings.xml"), DEVICE_MAPPINGS);
        return new Target(id, storefront, controlSchemes, false);
    }

    private static InstallationResult resultFor(Report report, Target target) {
        return report.installations().stream()
                .filter(result -> result.target().equals(target))
                .findFirst()
                .orElseThrow();
    }

    private static Path buttonMap(Target target, String device) {
        return target.controlSchemes().resolve("DeviceButtonMaps").resolve(device + ".buttonMap");
    }

    private List<Path> historyFiles() throws IOException {
        if (!Files.isDirectory(history)) return List.of();
        try (Stream<Path> files = Files.walk(history)) {
            return files.filter(Files::isRegularFile).toList();
        }
    }

    private long historyCopies() throws IOException {
        return historyFiles().size();
    }

    private static String read(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }
}
