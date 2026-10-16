package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceDivergence.Finding;
import elite.intel.bindforge.devicefiles.DeviceDivergence.Issue;
import elite.intel.bindforge.devicefiles.DeviceDivergence.Severity;
import elite.intel.bindforge.devicefiles.DeviceFilesCheck.Report;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * APPLY is offered only when pressing it would do something - including the retry after a partial Apply.
 */
class ApplyWorkTest {

    private static final Report REPORT = new Report(true, Set.of("LVWAP"), Set.of("LVWAP"), List.of());

    @Test
    void nothingSavedNothingPendingAndEveryInstallationMatchingIsNoWork() {
        assertFalse(ApplyWork.pending(List.of(), List.of(), false, List.of(), REPORT));
    }

    @Test
    void aDraftChangeIsWork() {
        assertTrue(ApplyWork.pending(List.of("LVWAP"), List.of(), false, List.of(), REPORT));
    }

    @Test
    void aRemovalOrRenameStillOnItsWayIsWork() {
        assertTrue(ApplyWork.pending(List.of(), List.of("OldStick"), false, List.of(), REPORT));
        assertTrue(ApplyWork.pending(List.of(), List.of(), true, List.of(), REPORT));
    }

    @Test
    void anInstallationThePushWouldRepairIsWorkSoAPartialApplyCanBeRetried() {
        assertTrue(ApplyWork.pending(List.of(), List.of(), false, List.of(finding(Issue.LABELS_DIFFER)), REPORT));
        assertTrue(ApplyWork.pending(List.of(), List.of(), false, List.of(finding(Issue.ENTRY_MISSING)), REPORT));
    }

    @Test
    void whatThePushCannotFixLeavesApplyDimmed() {
        assertFalse(ApplyWork.pending(List.of(), List.of(), false,
                List.of(finding(Issue.ORPHANED_BUTTON_MAP), finding(Issue.FILE_UNREADABLE)), REPORT));
    }

    @Test
    void beforeActivationThereIsNoMasterToPush() {
        assertFalse(ApplyWork.pending(List.of(), List.of(), false, List.of(finding(Issue.LABELS_DIFFER)), null));
    }

    private static Finding finding(Issue issue) {
        return new Finding(issue, Severity.YELLOW, "LVWAP", Set.of(DeviceDivergence.installKey(1)), true);
    }
}
