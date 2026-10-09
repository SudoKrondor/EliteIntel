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
import elite.intel.bindforge.devicefiles.DivergenceWaysOut.Kind;
import elite.intel.bindforge.devicefiles.DivergenceWaysOut.NoWayOut;
import elite.intel.bindforge.devicefiles.DivergenceWaysOut.WayOut;
import elite.intel.bindforge.install.GameInstallation;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Which ways out a row of the divergence list offers (Alan, 2026-10-08 - alias-designer.md, <em>The ways out</em>). */
class DivergenceWaysOutTest {

    private static final Target STEAM = new Target(1, "STEAM", Path.of("Steam", "ControlSchemes"), false);
    private static final Target EPIC = new Target(2, "EPIC", Path.of("Epic", "ControlSchemes"), false);

    private static final HardwareId MASTER_PID = new HardwareId("3344", "83F4");
    private static final HardwareId STEAM_PID = new HardwareId("3344", "83F5");
    private static final HardwareId EPIC_PID = new HardwareId("3344", "83F6");

    private static final Report REPORT = new Report(true, Set.of("LVWAP"), Set.of("LVWAP"), List.of(
            new InstallationCheck(STEAM, State.EDITED, null, List.of(
                    new ElementChange("LVWAP", ElementKind.HARDWARE_DIFFERS, MASTER_PID, STEAM_PID),
                    new ElementChange("STICK2", ElementKind.ADDED, null, new HardwareId("1234", "0001"))),
                    List.of()),
            new InstallationCheck(EPIC, State.EDITED, null, List.of(
                    new ElementChange("LVWAP", ElementKind.HARDWARE_DIFFERS, MASTER_PID, EPIC_PID)), List.of())));

    @Test
    void otherHardwareIsRevertedOrAdoptedFromEachInstallationWithTheHardwareItHolds() {
        List<WayOut> ways = DivergenceWaysOut.of(finding(Issue.ENTRY_HARDWARE_DIFFERS, "LVWAP", "2", "1"), REPORT);

        assertEquals(List.of(
                new WayOut(Kind.REVERT, List.of(1L, 2L), null),
                new WayOut(Kind.ADOPT, List.of(1L), STEAM_PID),
                new WayOut(Kind.ADOPT, List.of(2L), EPIC_PID)), ways);
    }

    /** Revert would leave it where it is: the push never removes an entry. */
    @Test
    void anEntryTheMasterLacksCanOnlyBeAdopted() {
        assertEquals(List.of(new WayOut(Kind.ADOPT, List.of(1L), new HardwareId("1234", "0001"))),
                DivergenceWaysOut.of(finding(Issue.ENTRY_NOT_IN_MASTER, "STICK2", "1"), REPORT));
    }

    /** Adopting a missing entry would drop the device from the master - that is CLEAR's to do, and to confirm. */
    @Test
    void whatTheInstallationLacksOrMisplacesIsReverted() {
        for (Issue issue : List.of(Issue.ENTRY_MISSING, Issue.ENTRY_OUT_OF_PLACE, Issue.BUTTON_MAP_MISSING,
                Issue.BUTTON_MAP_UNREADABLE, Issue.FILE_RESET)) {
            assertEquals(List.of(new WayOut(Kind.REVERT, List.of(1L), null)),
                    DivergenceWaysOut.of(finding(issue, "LVWAP", "1"), REPORT), issue.name());
        }
    }

    @Test
    void labelsAreReMergedWithTheMasterOrReverted() {
        assertEquals(List.of(new WayOut(Kind.REMERGE, List.of(1L, 2L), null), new WayOut(Kind.REVERT, List.of(1L, 2L), null)),
                DivergenceWaysOut.of(finding(Issue.LABELS_DIFFER, "LVWAP", "1", "2"), REPORT));
    }

    @Test
    void anUnreadableFileAndAnOrphanHaveNoWayOutAndSayWhy() {
        Finding unreadable = finding(Issue.FILE_UNREADABLE, GameInstallation.DEVICE_MAPPINGS, "1");
        Finding orphan = finding(Issue.ORPHANED_BUTTON_MAP, "OLDSTICK", "1");

        assertEquals(List.of(), DivergenceWaysOut.of(unreadable, REPORT));
        assertEquals(List.of(), DivergenceWaysOut.of(orphan, REPORT));
        assertEquals(NoWayOut.FILE_UNREADABLE, DivergenceWaysOut.whyNone(unreadable));
        assertEquals(NoWayOut.ORPHAN, DivergenceWaysOut.whyNone(orphan));
        assertNull(DivergenceWaysOut.whyNone(finding(Issue.ENTRY_MISSING, "LVWAP", "1")));
    }

    private static Finding finding(Issue issue, String device, String... installs) {
        return new Finding(issue, Severity.RED, device, Set.of(installs), true);
    }
}
