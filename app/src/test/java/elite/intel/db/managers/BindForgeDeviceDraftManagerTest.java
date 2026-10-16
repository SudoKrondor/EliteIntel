package elite.intel.db.managers;

import elite.intel.db.dao.BindForgeDeviceDraftDao.DraftRow;
import elite.intel.db.dao.BindForgeDeviceMasterDao.DeviceRow;
import elite.intel.db.managers.BindForgeDeviceDraftManager.AdoptionWrite;
import elite.intel.db.managers.BindForgeDeviceDraftManager.DraftWrite;
import elite.intel.db.managers.BindForgeDeviceDraftManager.Stored;
import elite.intel.db.managers.BindForgeDeviceDraftManager.StoredDevice;
import elite.intel.db.util.Database;
import org.jdbi.v3.core.Handle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
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
        notActivated();
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

    /** The identity question's yes (A12): the name and labels stay, the hardware moves - in the draft only. */
    @Test
    void retargetingByNameKeepsTheNameAndLabelsAndStartsTheDraft() {
        assertTrue(draft.retargetByName("LVWAP", "3344", "83f4", "3344", "83F5"));

        assertTrue(draft.isStarted());
        DraftRow row = draft.findByName("LVWAP");
        assertEquals("83F5", row.pid());
        assertEquals(labels("Joy_1", "LV MAIN TRIGGER", "Joy_2", "PINKY"), draft.labelsOf(row.id()));
        assertEquals("83F4", master.findByName("LVWAP").pid(), "Apply moves the master, not the answer");
    }

    @Test
    void aRetargetFromHardwareTheDeviceNoLongerHasWritesNothing() {
        assertFalse(draft.retargetByName("LVWAP", "3344", "0001", "3344", "83F5"));
        assertFalse(draft.retargetByName("Gone", "3344", "83F4", "3344", "83F5"));

        assertEquals("83F4", draft.findByName("LVWAP").pid());
    }

    @Test
    void aRetargetOntoHardwareAnotherDeviceHasWritesNothing() {
        assertFalse(draft.retargetByName("LVWAP", "3344", "83F4", "3344", "03f5"));

        assertEquals("83F4", draft.findByName("LVWAP").pid());
    }

    /** The Device Editor's SAVE: the whole set replaces the device's labels in the draft, never the master's. */
    @Test
    void savingLabelsReplacesTheDraftDevicesSetAndLeavesTheMasterAlone() {
        DraftRow saved = draft.saveLabels("LVWAP", "3344", "83F4", labels("Joy_1", "TRIGGER", "Joy_3", "Button 3"));

        assertTrue(draft.isStarted());
        assertEquals(lvwap, saved.masterId(), "the master device's own draft row, not a new one");
        assertEquals(labels("Joy_1", "TRIGGER", "Joy_3", "Button 3"), draft.labelsOf(saved.id()));
        assertEquals(labels("Joy_1", "LV MAIN TRIGGER", "Joy_2", "PINKY"), master.labelsOf(lvwap));
        assertEquals(List.of("LVWAP", "RVWAP"), names(draft.findAll()), "the rest of the master is in the draft too");
    }

    /** A built-in the master holds no labels for is added under Frontier's name, as a name the user has seen. */
    @Test
    void savingLabelsForADeviceTheDraftLacksAddsIt() {
        DraftRow saved = draft.saveLabels("T-Rudder", "044F", "B679", labels("Joy_RZAxis", "RUDDER"));

        assertNull(saved.masterId());
        assertEquals("B679", saved.pid());
        assertTrue(saved.aliasConfirmed());
        assertEquals(labels("Joy_RZAxis", "RUDDER"), draft.labelsOf(saved.id()));
        assertNull(master.findByName("T-Rudder"));
    }

    /** SAVE with the name changed (A11): one write, the old name kept for Apply, and no removal once applied. */
    @Test
    void savingWithANewNameRenamesAndKeepsTheOldNameForApply() {
        DraftRow saved = draft.saveDevice("LVWAP", "LeftStick", labels("Joy_1", "TRIGGER"));

        assertEquals("LeftStick", saved.deviceName());
        assertEquals("LVWAP", saved.previousName());
        assertEquals(lvwap, saved.masterId());
        assertEquals(labels("Joy_1", "TRIGGER"), draft.labelsOf(saved.id()));
        assertNotNull(master.findByName("LVWAP"), "the master waits for Apply");

        assertTrue(draft.promote().isEmpty(), "a renamed device is not a removed one");
        assertEquals("LVWAP", master.findByName("LeftStick").previousName());
        assertTrue(master.pendingRemovals().isEmpty());
    }

    /** Renamed back before Apply: nothing is left to rename, so no old name is kept. */
    @Test
    void renamingBackToTheAppliedNameKeepsNoOldName() {
        draft.saveDevice("LVWAP", "LeftStick", labels("Joy_1", "TRIGGER"));

        DraftRow back = draft.saveDevice("LeftStick", "LVWAP", labels("Joy_1", "TRIGGER"));

        assertNull(back.previousName());
    }

    /** A name another draft device holds, or a device that is gone, writes nothing - the editor read a stale list. */
    @Test
    void savingUnderATakenNameOrAGoneDeviceWritesNothing() {
        assertNull(draft.saveDevice("LVWAP", "RVWAP", labels("Joy_1", "TRIGGER")));
        assertNull(draft.saveDevice("Nobody", "LeftStick", labels("Joy_1", "TRIGGER")));
        assertEquals(labels("Joy_1", "LV MAIN TRIGGER", "Joy_2", "PINKY"),
                draft.labelsOf(draft.findByName("LVWAP").id()), "the transaction wrote nothing");
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
        rename("LVWAP", "LeftStick");

        assertTrue(draft.promote().isEmpty());
        DeviceRow renamed = master.findByName("LeftStick");
        assertEquals("LVWAP", renamed.previousName(), "the installations still hold LVWAP.buttonMap");
        assertEquals("LV MAIN TRIGGER", master.labelsOf(renamed.id()).get("Joy_1"));
    }

    /** The name kept is the one on disk, however many times the device was renamed before an Apply. */
    @Test
    void renamingTwiceKeepsTheNameTheInstallationsHold() {
        rename("LVWAP", "LeftStick");
        rename("LeftStick", "Left");

        assertEquals("LVWAP", draft.findByName("Left").previousName());
    }

    @Test
    void aDeviceAddedInTheDraftKeepsNoPreviousNameWhenRenamed() {
        draft.record("TRudder2", "044F", "B679", false);

        rename("TRudder2", "Pedals");

        assertNull(draft.findByName("Pedals").previousName(), "nothing was ever written under the first name");
    }

    /** Emptied and refilled rather than updated in place, so a name swap cannot trip the unique alias. */
    @Test
    void twoDevicesCanSwapNames() {
        rename("LVWAP", "Swapping");
        rename("RVWAP", "LVWAP");
        rename("Swapping", "RVWAP");

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
        rename("LVWAP", "Left");
        rename("Left", "LVWAP");

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
        rename("LVWAP", "Left");
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

    /**
     * Adopt writes the master and the draft together. A surviving device keeps its row, so the draft still points
     * at it; a device leaving the master becomes a pending removal (2026-10-05).
     */
    @Test
    void adoptingWritesTheMasterAndTheDraftInOneGo() {
        draft.start();
        long rvwap = master.findByName("RVWAP").id();
        List<Stored> seen = new ArrayList<>();

        draft.adopt(stored -> {
            seen.add(stored);
            StoredDevice lvwapNow = new StoredDevice(lvwap, "LVWAP", "3344", "83F5", true, null,
                    labels("Joy_1", "GAME TRIGGER"));
            StoredDevice stick = new StoredDevice(null, "STICK2", "1234", "0001", true, null, labels("Joy_1", "FIRE"));
            return new AdoptionWrite(List.of(lvwapNow, stick), List.of("RVWAP"), List.of(
                    new DraftWrite("LVWAP", lvwapNow),
                    new DraftWrite(null, new StoredDevice(rvwap, "RVWAP", "3344", "03F5", true, null, Map.of())),
                    new DraftWrite("STICK2", stick)));
        });

        assertEquals(List.of("LVWAP", "RVWAP"), seen.getFirst().master().stream().map(StoredDevice::deviceName).toList());
        assertEquals(2, seen.getFirst().draft().size(), "the started draft is handed to the plan");

        DeviceRow adopted = master.findByName("LVWAP");
        assertEquals(lvwap, adopted.id(), "a surviving device keeps its row");
        assertEquals("83F5", adopted.pid());
        assertEquals(labels("Joy_1", "GAME TRIGGER"), master.labelsOf(lvwap));
        assertNull(master.findByName("RVWAP"));
        assertEquals(List.of("RVWAP"), master.pendingRemovals());
        assertEquals(labels("Joy_1", "FIRE"), master.labelsOf(master.findByName("STICK2").id()));

        assertTrue(draft.isStarted());
        assertEquals(lvwap, draft.findByName("LVWAP").masterId());
        assertNull(draft.findByName("RVWAP").masterId(), "kept by the draft, no longer from a master device");
        assertEquals(master.findByName("STICK2").id(), draft.findByName("STICK2").masterId());
    }

    @Test
    void anAdoptWithNoDraftLeavesTheDraftUnstarted() {
        draft.adopt(stored -> {
            assertNull(stored.draft());
            return new AdoptionWrite(List.of(new StoredDevice(lvwap, "LVWAP", "3344", "83F4", true, null,
                    labels("Joy_1", "LV MAIN TRIGGER", "Joy_2", "PINKY"))), List.of(), null);
        });

        assertFalse(draft.isStarted());
        assertNotNull(master.findByName("RVWAP"), "the write names the whole master, but only removes what it lists");
    }

    @Test
    void aPlanThatReturnsNothingWritesNothing() {
        draft.adopt(stored -> null);

        assertEquals(List.of("LVWAP", "RVWAP"), master.findAll().stream().map(DeviceRow::deviceName).toList());
        assertFalse(draft.isStarted());
    }

    @Test
    void aFailedAdoptLeavesTheMasterAndTheDraftAsTheyWere() {
        draft.start();

        assertThrows(RuntimeException.class, () -> draft.adopt(stored -> new AdoptionWrite(
                List.of(new StoredDevice(null, "STICK2", "1234", "0001", true, null, Map.of())),
                List.of("RVWAP"),
                List.of(new DraftWrite("NO-SUCH-DEVICE", stored.draft().getFirst())))));

        assertNotNull(master.findByName("RVWAP"));
        assertNull(master.findByName("STICK2"));
        assertTrue(master.pendingRemovals().isEmpty());
        assertEquals(List.of("LVWAP", "RVWAP"), names(draft.findAll()));
    }

    /** First setup runs once: a master that is no longer empty is never overwritten by it. */
    @Test
    void establishingRefusesAMasterThatIsNotEmpty() {
        assertFalse(draft.establish(List.of(new StoredDevice(null, "STICK2", "1234", "0001", true, null, Map.of())),
                List.of()));

        assertNull(master.findByName("STICK2"));
        assertEquals(2, master.findAll().size());
    }

    @Test
    void establishingFillsAnEmptyMasterRecordsTheLosersAndEndsAnyDraft() {
        master.findAll().forEach(row -> master.remove(row.id()));
        draft.start();

        assertTrue(draft.establish(List.of(
                        new StoredDevice(null, "RVWAP", "3344", "03F5", true, null, labels("Joy_1", "TRIGGER")),
                        new StoredDevice(null, "T-Rudder", "044F", "B679", true, null, labels("Joy_XAxis", "X Axis"))),
                List.of("RightStick")));

        assertEquals(List.of("RVWAP", "T-Rudder"), master.findAll().stream().map(DeviceRow::deviceName).toList());
        assertEquals(labels("Joy_1", "TRIGGER"), master.labelsOf(master.findByName("RVWAP").id()));
        assertTrue(master.findByName("RVWAP").aliasConfirmed());
        assertEquals(List.of("RightStick"), master.pendingRemovals());
        assertFalse(draft.isStarted(), "a draft left started would replace the new master with its empty copy");
    }

    @Test
    void establishingActivatesInTheSameTransaction() {
        master.findAll().forEach(row -> master.remove(row.id()));

        assertTrue(draft.establish(List.of(), List.of()), "a user who owns nothing is still activated");

        assertTrue(BindForgeActivationManager.getInstance().isActivated());
        assertTrue(master.findAll().isEmpty());
    }

    /** Activation happens once: an activated BindForge with an empty master is not filled again (Alan, 2026-10-13). */
    @Test
    void establishingRefusesOnceActivatedEvenWithAnEmptyMaster() {
        master.findAll().forEach(row -> master.remove(row.id()));
        activated();

        assertFalse(draft.establish(List.of(new StoredDevice(null, "STICK2", "1234", "0001", true, null, Map.of())),
                List.of()));

        assertTrue(master.findAll().isEmpty());
    }

    /** CLEAR: the device leaves the draft, and applying makes it a pending removal for the cleanup (A10). */
    @Test
    void clearingADeviceMakesItAPendingRemovalOnApply() {
        assertTrue(draft.removeByName("RVWAP"));

        assertEquals(List.of("RVWAP"), draft.promote());
        assertNull(master.findByName("RVWAP"));
        assertEquals(List.of("RVWAP"), master.pendingRemovals());
    }

    @Test
    void clearingANameTheDraftDoesNotHoldChangesNothing() {
        assertFalse(draft.removeByName("TRudder2"));
        assertEquals(List.of("LVWAP", "RVWAP"), names(draft.findAll()), "the draft is still a whole copy");
    }

    /**
     * Found 2026-10-11: a device cleared and then saved again comes back as a new draft row with no master row
     * behind it. Its name must not become a pending removal while it is in the master, or the cleanup would take a
     * live device out of every installation.
     */
    @Test
    void aDeviceClearedThenSavedAgainIsNeverAPendingRemoval() {
        draft.removeByName("LVWAP");
        draft.saveLabels("LVWAP", "3344", "83F4", labels("Joy_1", "TRIGGER"));

        assertTrue(draft.promote().isEmpty());
        assertNotNull(master.findByName("LVWAP"));
        assertTrue(master.pendingRemovals().isEmpty());
    }

    /** UNDO CLEAR: the device comes back as the master holds it, and the draft changes nothing again. */
    @Test
    void undoingAClearPutsTheMastersDeviceBack() {
        draft.removeByName("LVWAP");

        assertTrue(draft.restoreFromMaster("LVWAP"));

        DraftRow back = draft.findByName("LVWAP");
        assertEquals(lvwap, back.masterId());
        assertEquals(labels("Joy_1", "LV MAIN TRIGGER", "Joy_2", "PINKY"), draft.labelsOf(back.id()));
        assertTrue(draft.promote().isEmpty(), "nothing is removed");
    }

    /** Onboarding names a controller no binding names by hex straight away: into the master (A6b, 2026-10-14). */
    @Test
    void onboardingNamedNowGoesIntoTheMaster() {
        activated();

        assertTrue(draft.onboard(List.of(stick("VirpilControls20220720", "3344", "0194")), List.of()));

        DeviceRow row = master.findByName("VirpilControls20220720");
        assertNotNull(row);
        assertTrue(row.aliasConfirmed(), "the user was shown the name");
        assertEquals(labels("Joy_1", "Button 1"), master.labelsOf(row.id()));
        assertFalse(draft.isStarted(), "no draft is started for a name applied straight away");
    }

    /** A started draft gets the name too, or Apply's promote would drop it and store it as a pending removal. */
    @Test
    void onboardingNamedNowGoesIntoAStartedDraftAsWell() {
        activated();
        draft.start();

        assertTrue(draft.onboard(List.of(stick("VirpilControls20220720", "3344", "0194")), List.of()));

        DraftRow copy = draft.findByName("VirpilControls20220720");
        assertNotNull(copy);
        assertEquals(master.findByName("VirpilControls20220720").id(), copy.masterId());
        assertTrue(draft.promote().isEmpty(), "nothing leaves the master on Apply");
        assertNotNull(master.findByName("VirpilControls20220720"));
        assertTrue(master.pendingRemovals().isEmpty());
    }

    /** A controller .binds names by hex waits for Apply: the draft only, starting it from the master. */
    @Test
    void onboardingNamedAtApplyGoesIntoTheDraftOnly() {
        activated();

        assertTrue(draft.onboard(List.of(), List.of(stick("LeftStick", "3344", "83F3"))));

        assertNull(master.findByName("LeftStick"), "not applied yet");
        assertEquals(List.of("LVWAP", "LeftStick", "RVWAP"), names(draft.findAll()), "a whole copy of the master");
        assertEquals(labels("Joy_1", "Button 1"), draft.labelsOf(draft.findByName("LeftStick").id()));
        draft.promote();
        assertNotNull(master.findByName("LeftStick"), "Apply makes it the master's");
    }

    @Test
    void onboardingWritesNothingBeforeActivation() {
        assertFalse(draft.onboard(List.of(stick("STICK2", "1234", "0001")), List.of(stick("STICK3", "1234", "0002"))));

        assertNull(master.findByName("STICK2"));
        assertFalse(draft.isStarted());
    }

    /** A name taken since the dialog read the list writes nothing at all - not even the names that were free. */
    @Test
    void onboardingWritesNothingWhenANameWasTakenMeanwhile() {
        activated();

        assertFalse(draft.onboard(List.of(stick("STICK2", "1234", "0001"), stick("RVWAP", "1234", "0002")),
                List.of()));

        assertNull(master.findByName("STICK2"));
    }

    private static StoredDevice stick(String name, String vid, String pid) {
        return new StoredDevice(null, name, vid, pid, true, null, labels("Joy_1", "Button 1"));
    }

    @Test
    void storedReadsTheMasterAndTheDraftTogether() {
        assertNull(draft.stored().draft(), "no draft is started");

        draft.start();

        Stored stored = draft.stored();
        assertEquals(2, stored.master().size());
        assertEquals(2, stored.draft().size());
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

    /** The suite shares one database, and activation is a stored fact - each test starts before it. */
    static void notActivated() {
        try (Handle handle = Database.init()) {
            handle.execute("UPDATE bindforge_activation SET activated_at = NULL, backup_folder = NULL WHERE id = 1");
        }
    }

    /** Activated by SQL: the manager has no way to activate outside {@code establish}, by design. */
    static void activated() {
        try (Handle handle = Database.init()) {
            handle.execute("UPDATE bindforge_activation SET activated_at = CURRENT_TIMESTAMP WHERE id = 1");
        }
    }

    /** A rename as the Device Editor makes it: SAVE with the name changed and the labels as they are. */
    private void rename(String from, String to) {
        Map<String, String> labels = draft.isStarted() && draft.findByName(from) != null
                ? draft.labelsOf(draft.findByName(from).id())
                : master.labelsOf(master.findByName(from).id());
        assertNotNull(draft.saveDevice(from, to, labels), from + " -> " + to);
    }
}
