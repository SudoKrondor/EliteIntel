package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceFilesPush.InstallationResult;
import elite.intel.bindforge.devicefiles.DeviceFilesPush.MasterDevice;
import elite.intel.bindforge.devicefiles.DeviceFilesPush.Outcome;
import elite.intel.bindforge.devicefiles.DeviceFilesPush.Report;
import elite.intel.bindforge.devicefiles.DeviceFilesPush.Target;
import elite.intel.io.TimestampedBackups;
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
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    private Report push(List<Target> targets, List<MasterDevice> master) {
        DeviceFilesPush push = new DeviceFilesPush(() -> targets, () -> master, FrontierStockDevices.getInstance(),
                new TimestampedBackups(), history, () -> 10, rescanned::add);
        return push.push();
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
