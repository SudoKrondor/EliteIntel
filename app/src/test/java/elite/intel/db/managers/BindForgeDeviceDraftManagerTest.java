package elite.intel.db.managers;

import elite.intel.db.dao.BindForgeDeviceDraftDao.DraftRow;
import elite.intel.db.dao.BindForgeDeviceMasterDao.DeviceRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The device draft holds what the user saved and has not applied. SAVE never reaches the master; applying
 * makes the draft the master.
 */
class BindForgeDeviceDraftManagerTest {

    private final BindForgeDeviceMasterManager master = BindForgeDeviceMasterManager.getInstance();
    private final BindForgeDeviceDraftManager draft = BindForgeDeviceDraftManager.getInstance();

    private long lvwap;

    @BeforeEach
    void aMasterOfTwoDevicesAndNoDraft() {
        draft.discard();
        master.findAll().forEach(row -> master.remove(row.id()));
        master.pendingRemovals().forEach(master::clearPendingRemoval);
        lvwap = master.record("LVWAP", "3344", "83F4", true).id();
        master.replaceLabels(lvwap, labels("Joy_1", "LV MAIN TRIGGER", "Joy_2", "PINKY"));
        master.record("RVWAP", "3344", "03F5", true);
    }

    @Test
    void thereIsNoDraftUntilTheFirstEdit() {
        assertFalse(draft.isStarted());
        assertTrue(draft.findAll().isEmpty(), "with no draft, the device list reads the master");
    }

    @Test
    void theFirstEditCopiesTheWholeMasterIntoTheDraft() {
        draft.record("TRudder2", "044F", "B679", false);

        assertTrue(draft.isStarted());
        assertEquals(List.of("LVWAP", "RVWAP", "TRudder2"), names(draft.findAll()));
        DraftRow copied = draft.findByName("LVWAP");
        assertEquals(lvwap, copied.masterId(), "a copied device remembers the master row it came from");
        assertEquals(labels("Joy_1", "LV MAIN TRIGGER", "Joy_2", "PINKY"), draft.labelsOf(copied.id()));
        assertNull(draft.findByName("TRudder2").masterId(), "a device added in the draft has no master row");
    }

    /** SAVE is the button pressed by habit, so it must never be the one that changes what was applied. */
    @Test
    void savingAnEditLeavesTheMasterAlone() {
        draft.start();
        DraftRow row = draft.findByName("LVWAP");

        draft.putLabel(row.id(), "Joy_1", "TRIGGER");
        draft.retarget(row.id(), "3344", "83F5");

        assertEquals("TRIGGER", draft.labelsOf(row.id()).get("Joy_1"));
        assertEquals("LV MAIN TRIGGER", master.labelsOf(lvwap).get("Joy_1"));
        assertEquals("83F4", master.findByName("LVWAP").pid());
    }

    @Test
    void promotingMakesTheDraftTheMasterAndEndsTheDraft() {
        draft.start();
        long row = draft.findByName("LVWAP").id();
        draft.replaceLabels(row, labels("Joy_1", "TRIGGER"));
        draft.record("TRudder2", "044F", "B679", true);

        List<String> removed = draft.promote();

        assertTrue(removed.isEmpty());
        assertFalse(draft.isStarted());
        assertTrue(draft.findAll().isEmpty());
        assertEquals(List.of("LVWAP", "RVWAP", "TRudder2"), master.findAll().stream().map(DeviceRow::deviceName).toList());
        assertEquals(labels("Joy_1", "TRIGGER"), master.labelsOf(master.findByName("LVWAP").id()),
                "labels are a whole set: the one dropped in the draft is gone from the master too");
    }

    /**
     * Apply never removes anything from a game file, so a device removed in the draft is kept as a pending
     * removal: its entry is still in every installation, and the list has to outlive the call that made it.
     */
    @Test
    void aRemovedDeviceIsKeptAsAPendingRemoval() {
        draft.start();
        draft.remove(draft.findByName("RVWAP").id());

        assertEquals(List.of("RVWAP"), draft.promote());
        assertNull(master.findByName("RVWAP"));
        assertEquals(List.of("RVWAP"), master.pendingRemovals(), "stored, not only returned");
    }

    /** A cleanup must never remove an entry the user has put back in the meantime. */
    @Test
    void aDevicePutBackInTheMasterIsNoLongerPendingRemoval() {
        draft.start();
        draft.remove(draft.findByName("RVWAP").id());
        draft.promote();

        draft.record("RVWAP", "3344", "03F5", true);
        draft.promote();

        assertTrue(master.pendingRemovals().isEmpty());
    }

    @Test
    void aRenameIsNotReportedAsARemoval() {
        draft.start();
        draft.rename(draft.findByName("LVWAP").id(), "LeftStick");

        assertTrue(draft.promote().isEmpty());
        DeviceRow renamed = master.findByName("LeftStick");
        assertEquals("LVWAP", renamed.previousName(), "the installations still hold LVWAP.buttonMap");
        assertEquals("LV MAIN TRIGGER", master.labelsOf(renamed.id()).get("Joy_1"));
    }

    /** The name kept is the one on disk, however many times the device was renamed before an Apply. */
    @Test
    void renamingTwiceKeepsTheNameTheInstallationsHold() {
        draft.start();
        long row = draft.findByName("LVWAP").id();

        draft.rename(row, "LeftStick");
        draft.rename(row, "Left");

        assertEquals("LVWAP", draft.findByName("Left").previousName());
    }

    @Test
    void aDeviceAddedInTheDraftKeepsNoPreviousNameWhenRenamed() {
        long row = draft.record("TRudder2", "044F", "B679", false).id();

        draft.rename(row, "Pedals");

        assertNull(draft.findByName("Pedals").previousName(), "nothing was ever written under the first name");
    }

    /** Emptied and refilled rather than updated in place, so a name swap cannot trip the unique alias. */
    @Test
    void twoDevicesCanSwapNames() {
        draft.start();
        long left = draft.findByName("LVWAP").id();
        long right = draft.findByName("RVWAP").id();
        draft.rename(left, "Swapping");
        draft.rename(right, "LVWAP");
        draft.rename(left, "RVWAP");

        assertTrue(draft.promote().isEmpty());

        assertEquals("03F5", master.findByName("LVWAP").pid());
        assertEquals("83F4", master.findByName("RVWAP").pid());
    }

    /** A draft with no devices in it is still a draft - clearing everything must reach the master. */
    @Test
    void clearingEveryDevicePromotesToAnEmptyMaster() {
        draft.start();
        draft.findAll().forEach(row -> draft.remove(row.id()));

        assertTrue(draft.isStarted());
        assertEquals(List.of("LVWAP", "RVWAP"), draft.promote());
        assertTrue(master.findAll().isEmpty());
    }

    @Test
    void promotingWithNoDraftChangesNothing() {
        assertTrue(draft.promote().isEmpty());
        assertEquals(2, master.findAll().size());
    }

    @Test
    void discardingThrowsTheDraftAwayAndLeavesTheMaster() {
        draft.record("TRudder2", "044F", "B679", false);

        draft.discard();

        assertFalse(draft.isStarted());
        assertTrue(draft.findAll().isEmpty());
        assertEquals(2, master.findAll().size());
    }

    @Test
    void revertingOneDeviceUndoesItsEditsAndLeavesTheOthers() {
        draft.start();
        long row = draft.findByName("LVWAP").id();
        draft.replaceLabels(row, labels("Joy_1", "TRIGGER"));
        draft.retarget(draft.findByName("RVWAP").id(), "3344", "03F6");

        draft.revert(lvwap);

        DraftRow reverted = draft.findByName("LVWAP");
        assertEquals(labels("Joy_1", "LV MAIN TRIGGER", "Joy_2", "PINKY"), draft.labelsOf(reverted.id()));
        assertEquals("03F6", draft.findByName("RVWAP").pid(), "only the reverted device went back");
    }

    @Test
    void revertingARemovedDeviceBringsItBack() {
        draft.start();
        draft.remove(draft.findByName("LVWAP").id());

        draft.revert(lvwap);

        assertNotNull(draft.findByName("LVWAP"));
        assertTrue(draft.promote().isEmpty());
    }

    /**
     * Starting the draft and the edit are one transaction. The null name fails its NOT NULL constraint after
     * the master has been copied in - left in autocommit, that would leave a draft holding the copy and no edit.
     */
    @Test
    void aFailedFirstEditLeavesNoDraft() {
        assertThrows(RuntimeException.class, () -> draft.record(null, "044F", "B679", false));

        assertFalse(draft.isStarted());
        assertTrue(draft.findAll().isEmpty());
    }

    @Test
    void renamingBackToTheOriginalNameLeavesNoRenamePending() {
        draft.start();
        long row = draft.findByName("LVWAP").id();

        draft.rename(row, "Left");
        draft.rename(row, "LVWAP");

        assertNull(draft.findByName("LVWAP").previousName(), "the installations already hold this name");
    }

    /** Opening a device starts a draft before anything is changed, so a started draft is not "unsaved edits". */
    @Test
    void startingADraftWithoutEditingLeavesTheDraftEqualToTheMaster() {
        draft.start();

        assertTrue(draft.isStarted(), "started, though nothing has been changed");
        assertEquals(List.of("LVWAP", "RVWAP"), names(draft.findAll()));
        assertTrue(draft.promote().isEmpty());
        assertEquals(labels("Joy_1", "LV MAIN TRIGGER", "Joy_2", "PINKY"),
                master.labelsOf(master.findByName("LVWAP").id()));
    }

    @Test
    void revertingIsRefusedWhenAnotherDraftDeviceHasTakenTheName() {
        draft.start();
        draft.rename(draft.findByName("LVWAP").id(), "Left");
        long taker = draft.record("LVWAP", "044F", "B679", false).id();

        IllegalStateException refused = assertThrows(IllegalStateException.class, () -> draft.revert(lvwap));

        assertTrue(refused.getMessage().contains("LVWAP"), refused.getMessage());
        assertNotNull(draft.findByName("Left"), "the refused revert changed nothing");
        assertEquals(taker, draft.findByName("LVWAP").id());
    }

    /**
     * A master row deleted while a draft exists - only a write outside Apply can do that - turns its draft row into
     * a device added in the draft. Recorded so the behaviour is known rather than discovered.
     */
    @Test
    void aMasterDeviceDeletedDuringADraftBecomesADeviceAddedInTheDraft() {
        draft.start();

        master.remove(lvwap);

        assertNull(draft.findByName("LVWAP").masterId());
        assertTrue(draft.promote().isEmpty(), "nothing is reported removed - the draft still holds it");
        assertNotNull(master.findByName("LVWAP"));
    }

    private static List<String> names(List<DraftRow> rows) {
        return rows.stream().map(DraftRow::deviceName).toList();
    }

    private static Map<String, String> labels(String... tokenThenLabel) {
        Map<String, String> labels = new LinkedHashMap<>();
        for (int i = 0; i < tokenThenLabel.length; i += 2) {
            labels.put(tokenThenLabel[i], tokenThenLabel[i + 1]);
        }
        return labels;
    }
}
