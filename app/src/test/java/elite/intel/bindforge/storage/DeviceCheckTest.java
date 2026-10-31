package elite.intel.bindforge.storage;

import elite.intel.bindforge.devicefiles.FirstSetupPlan;
import elite.intel.bindforge.install.GameInstallation;
import elite.intel.bindforge.storage.DeviceCheck.State;
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static elite.intel.bindforge.storage.BindingsComparisonTest.binds;
import static elite.intel.bindforge.storage.WorkingCopyTest.bytes;
import static elite.intel.bindforge.storage.WorkingCopyTest.text;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The check for the device files (storage.md rule 14, <em>Settled with Storage S9's sources</em>; Storage S9a): each
 * reachable installation judged against the working copy, the reconcile with the working copy as one side, and which
 * installations the check's write leaves alone.
 */
class DeviceCheckTest {

    private static final String LVWAP = "<LVWAP><PID>83F4</PID><VID>3344</VID></LVWAP>";
    private static final String RUDDER = "<T-Rudder><PID>B679</PID><VID>044F</VID></T-Rudder>";
    private static final String CUSTOM = "Custom.4.2.binds";
    /** Frontier ships T-Rudder in these tests; LVWAP is the user's. */
    private static final Predicate<String> SHIPS = "T-Rudder"::equals;

    @TempDir
    Path temp;

    private final ConfigurationFiles workingCopy = new ConfigurationFiles(
            Map.of(CUSTOM, bytes(binds("Custom", "Key_A", "Joy_XAxis", ""))),
            bytes("<Root>" + LVWAP + RUDDER + "</Root>"), Map.of());

    @Test
    void eachReachableInstallationIsJudged() throws IOException {
        InstallationRow steam = installation(1, "STEAM", "<Root>" + LVWAP + RUDDER + "</Root>");
        InstallationRow epic = installation(2, "EPIC", "<Root>" + RUDDER + "</Root>");
        InstallationRow frontier = installation(3, "FRONTIER", null);
        InstallationRow unplugged = new InstallationRow(4, "OCULUS", temp.resolve("Oculus").toString(), false, false);

        DeviceCheck.Found found = judge(List.of(steam, epic, frontier, unplugged));

        assertEquals(List.of(State.MATCHES, State.RESET_FACTORY, State.RESET_MISSING),
                found.installations().stream().map(DeviceCheck.Judged::state).toList(),
                "an installation that cannot be reached is left out, and judged when it comes back");
        assertTrue(found.anyReset());
        assertEquals(List.of(epic), found.sides().stream().map(DeviceCheck.Judged::row).toList(),
                "a missing file cannot be a side - only putting the working copy back is offered for it");
    }

    /** One that cannot be parsed is offered putting the working copy back; one that cannot be read is left alone. */
    @Test
    void aFileThatCannotBeParsedIsAskedAboutAndOneThatCannotBeReadIsNot() throws IOException {
        InstallationRow steam = installation(1, "STEAM", "not xml at all");
        InstallationRow epic = installation(2, "EPIC", "<Root>" + LVWAP + RUDDER + "</Root>");

        DeviceCheck.Found found = DeviceCheck.judge(workingCopy, List.of(steam, epic), root -> {
            if (root.endsWith("EPIC")) throw new IOException("locked");
            return DeviceFiles.read(root);
        }, SHIPS);

        assertEquals(State.UNPARSEABLE, found.installations().get(0).state());
        assertEquals(State.UNREADABLE, found.installations().get(1).state());
        assertEquals("locked", found.installations().get(1).problem());
        assertEquals(List.of(found.installations().getFirst()), found.asked());
        assertTrue(found.sides().isEmpty());
    }

    /** Compared again before the write by value: the same values in other bytes are no change; another value is. */
    @Test
    void theSameValuesAreTheSameAndAnotherIsAChange() throws IOException {
        InstallationRow epic = installation(2, "EPIC", "<Root>" + RUDDER + "</Root>");
        DeviceCheck.Found before = judge(List.of(epic));

        write(epic, "<Root>\r\n  " + RUDDER + "\r\n</Root>\r\n");
        assertTrue(judge(List.of(epic)).sameAs(before));

        write(epic, "<Root>" + LVWAP + "</Root>");
        assertFalse(judge(List.of(epic)).sameAs(before));
    }

    /**
     * Compared again both ways (Storage S9R): a Frontier-shipped map the working copy lacks is no difference to the
     * working copy, but between two reads its coming or going is the game's doing.
     */
    @Test
    void aShippedMapThatComesOrGoesMeanwhileIsAChange() throws IOException {
        InstallationRow epic = installation(2, "EPIC", "<Root>" + LVWAP + RUDDER + "</Root>");
        Path shipped = buttonMap(epic, "VPCPanel.buttonMap");
        DeviceCheck.Found withIt = judge(List.of(epic));
        assertEquals(State.MATCHES, withIt.installations().getFirst().state());

        Files.delete(shipped);
        DeviceCheck.Found withoutIt = judge(List.of(epic));
        assertEquals(State.MATCHES, withoutIt.installations().getFirst().state());

        assertFalse(withoutIt.sameAs(withIt), "removed meanwhile");
        assertFalse(withIt.sameAs(withoutIt), "added meanwhile");
    }

    /**
     * A DeviceMappings.xml the judgement never parsed - the working copy holds none, so the installation matches
     * unread - and cannot be parsed now counts as changed rather than throwing.
     */
    @Test
    void aFileThatCannotBeParsedWhenComparedAgainIsAChange() throws IOException {
        ConfigurationFiles noDeviceMappings = new ConfigurationFiles(workingCopy.bindings(), null, Map.of());
        InstallationRow epic = installation(2, "EPIC", "not xml at all");
        DeviceCheck.Found before = DeviceCheck.judge(noDeviceMappings, List.of(epic), DeviceFiles::read, SHIPS);
        assertEquals(State.MATCHES, before.installations().getFirst().state());

        assertFalse(DeviceCheck.judge(noDeviceMappings, List.of(epic), DeviceFiles::read, SHIPS).sameAs(before));
    }

    /** The reconcile: the working copy first, under its own side id, then each installation that can be a side. */
    @Test
    void theReconcileHasTheWorkingCopyAsOneSide() throws IOException {
        InstallationRow epic = installation(2, "EPIC",
                "<Root><LVWAP><PID>83F5</PID><VID>3344</VID></LVWAP>" + RUDDER + "</Root>");

        FirstSetupReading reading = judge(List.of(epic)).reading(workingCopy, workingCopy.bindings(), temp,
                temp.resolve("working-copy"), "BindForge's working copy");

        assertEquals(List.of(DeviceCheck.WORKING_COPY_SIDE, 2L),
                reading.installations().stream().map(InstallationFiles::installId).toList());
        assertEquals("BindForge's working copy", reading.installations().getFirst().storefront());
        assertTrue(reading.plan().rows().stream()
                        .anyMatch(row -> row.kind() == FirstSetupPlan.Kind.HARDWARE_DIFFERS),
                "LVWAP on another PID is a question, as in first setup");
        assertEquals(workingCopy.bindings().keySet(), reading.bindings().keySet());
    }

    /**
     * The check's write: one it could not read at all is left alone, every other one it read is written, and one it
     * never read is left alone only if it differs from the working copy (storage.md rule 1).
     */
    @Test
    void theWriteLeavesAloneWhatItCouldNotReadAndAnUnseenInstallationThatDiffers() throws IOException {
        InstallationRow steam = installation(1, "STEAM", "<Root>" + RUDDER + "</Root>");
        InstallationRow epic = installation(2, "EPIC", "<Root>" + LVWAP + RUDDER + "</Root>");
        InstallationRow differsUnseen = installation(3, "FRONTIER", "<Root>" + RUDDER + "</Root>");
        InstallationRow matchesUnseen = installation(4, "OCULUS", "<Root>" + LVWAP + RUDDER + "</Root>");
        DeviceCheck.Found found = DeviceCheck.judge(workingCopy, List.of(steam, epic), root -> {
            if (root.endsWith("EPIC")) throw new IOException("locked");
            return DeviceFiles.read(root);
        }, SHIPS);

        LiveCopies.LeaveAlone leaveAlone = found.leavingAlone(DeviceFilesComparison.of(workingCopy, SHIPS),
                installation -> DeviceFiles.read(installation.root()));

        assertFalse(leaveAlone.leaveAlone(place(steam)), "the reset one it read gets what the user settled on");
        assertTrue(leaveAlone.leaveAlone(place(epic)), "the one it could not read is left as it is");
        assertTrue(leaveAlone.leaveAlone(place(differsUnseen)));
        assertFalse(leaveAlone.leaveAlone(place(matchesUnseen)));
    }

    @Test
    void puttingTheWorkingCopyBackChangesNothingInTheSet() throws IOException {
        assertSame(workingCopy, DeviceCheck.withDevices(workingCopy, DeviceCheck.Answer.PUT_MINE_BACK));
    }

    /** A reconcile's device files replace the working copy's, and a losing name's bindings move to the winner. */
    @Test
    void aReconcileReplacesTheDeviceFilesAndMovesTheBindings() throws IOException {
        ConfigurationFiles reconciled = new ConfigurationFiles(Map.of(), bytes("<Root>" + RUDDER + "</Root>"),
                Map.of("RightStick.buttonMap", bytes("<Root/>")));

        ConfigurationFiles files = DeviceCheck.withDevices(workingCopy,
                DeviceCheck.Answer.reconciled(reconciled, Map.of("RVWAP", "RightStick")));

        assertEquals("<Root>" + RUDDER + "</Root>", text(files.deviceMappings()));
        assertEquals(List.of("RightStick.buttonMap"), List.copyOf(files.buttonMaps().keySet()));
        assertTrue(text(files.bindings().get(CUSTOM)).contains("Device=\"RightStick\""));
        assertFalse(text(files.bindings().get(CUSTOM)).contains("Device=\"RVWAP\""));
    }

    // ---- helpers

    private DeviceCheck.Found judge(List<InstallationRow> rows) throws IOException {
        return DeviceCheck.judge(workingCopy, rows, DeviceFiles::read, SHIPS);
    }

    /** An installation folder named after its storefront, with this DeviceMappings.xml - none when {@code null}. */
    private InstallationRow installation(long id, String storefront, String deviceMappings) throws IOException {
        Path root = temp.resolve(storefront);
        Files.createDirectories(GameInstallation.controlSchemesUnder(root));
        InstallationRow row = new InstallationRow(id, storefront, root.toString(), false, false);
        if (deviceMappings != null) write(row, deviceMappings);
        return row;
    }

    private static void write(InstallationRow row, String deviceMappings) throws IOException {
        Files.writeString(GameInstallation.deviceMappingsIn(GameInstallation.controlSchemesUnder(
                Path.of(row.rootPath()))), deviceMappings);
    }

    private static Path buttonMap(InstallationRow row, String name) throws IOException {
        Path folder = Files.createDirectories(GameInstallation.deviceButtonMapsIn(GameInstallation.controlSchemesUnder(
                Path.of(row.rootPath()))));
        return Files.writeString(folder.resolve(name), "<Root><Joy_1>FIRE</Joy_1></Root>");
    }

    private static BackupSources.Installation place(InstallationRow row) {
        return new BackupSources.Installation(row.storefront(), Path.of(row.rootPath()), false, null);
    }
}
