package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceFilesPush.MasterDevice;
import elite.intel.bindforge.devicefiles.DeviceFilesPush.Outcome;
import elite.intel.bindforge.devicefiles.DeviceFilesPush.Report;
import elite.intel.bindforge.devicefiles.DeviceFilesPush.Target;
import elite.intel.bindforge.io.DeviceReferenceRewrite;
import elite.intel.bindforge.io.DeviceRewriteResult;
import elite.intel.db.dao.BindForgeDeviceMasterDao.DeviceRow;
import elite.intel.db.managers.BindForgeDeviceDraftManager;
import elite.intel.db.managers.BindForgeDeviceMasterManager;
import elite.intel.io.TimestampedBackups;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Apply for the device files: the draft becomes the master first, so a write that fails loses none of the
 * user's edits - and the pending removals leave every installation, unless a binding still names them.
 */
class DeviceApplyTest {

    private static final String DEVICE_MAPPINGS = """
            <?xml version="1.0" encoding="UTF-8" ?>
            <Root>
            \t<T-Rudder><PID>B679</PID><VID>044F</VID></T-Rudder>
            </Root>
            """;

    private final BindForgeDeviceMasterManager master = BindForgeDeviceMasterManager.getInstance();
    private final BindForgeDeviceDraftManager draft = BindForgeDeviceDraftManager.getInstance();

    @TempDir
    Path temp;

    @BeforeEach
    void anEmptyMasterAndNoDraft() {
        draft.discard();
        master.findAll().forEach(row -> master.remove(row.id()));
        master.pendingRemovals().forEach(master::clearPendingRemoval);
    }

    @Test
    void theDraftIsPromotedBeforeAnythingIsPushed() {
        List<String> order = new ArrayList<>();
        Report empty = new Report(List.of());

        DeviceApply.Result result = new DeviceApply(
                () -> { order.add("promote"); return List.of("RVWAP"); },
                removals -> { order.add("push"); return empty; },
                Set::of, FrontierStockDevices.getInstance(), master).apply();

        assertEquals(List.of("promote", "push"), order);
        assertEquals(List.of("RVWAP"), result.removed(), "what the draft removed is handed to the caller");
    }

    @Test
    void savedEditsReachEveryInstallation() throws IOException {
        Target steam = steamInstallation();
        long row = draft.record("RVWAP", "3344", "03F5", true).id();
        draft.replaceLabels(row, Map.of("Joy_1", "RV Main Trigger"));

        DeviceApply.Result result = apply(List.of(steam), Set.of());

        assertEquals(Outcome.WRITTEN, result.report().installations().getFirst().outcome());
        assertTrue(Files.readString(steam.controlSchemes().resolve("DeviceMappings.xml")).contains("<RVWAP>"));
        assertFalse(draft.isStarted(), "applied edits are no longer a draft");
    }

    /**
     * The point of promoting first (Alan, 2026-10-04): an installation that cannot be written is reported, and
     * the edits stay in the master for the next Apply rather than being lost with the failed write.
     */
    @Test
    void anInstallationThatCannotBeReachedLosesNoEdits() {
        draft.record("RVWAP", "3344", "03F5", true);

        DeviceApply.Result result = apply(List.of(unplugged()), Set.of());

        assertEquals(Outcome.SKIPPED_MISSING, result.report().installations().getFirst().outcome());
        assertNotNull(master.findByName("RVWAP"), "the edit is in the master, waiting for the next Apply");
    }

    /** With nothing saved, Apply pushes the master as it stands - the retry after a partial Apply. */
    @Test
    void withNoDraftTheMasterIsStillPushed() throws IOException {
        Target steam = steamInstallation();
        master.record("LVWAP", "3344", "83F4", true);

        DeviceApply.Result result = apply(List.of(steam), Set.of());

        assertTrue(result.removed().isEmpty());
        assertEquals(Outcome.WRITTEN, result.report().installations().getFirst().outcome());
        assertTrue(Files.readString(steam.controlSchemes().resolve("DeviceMappings.xml")).contains("<LVWAP>"));
    }

    /** CLEAR, then APPLY: the device leaves every installation, and is no longer pending (A10). */
    @Test
    void aClearedDeviceLeavesEveryInstallationAndStopsBeingPending() throws IOException {
        Target steam = steamInstallation();
        lvwapAppliedTo(steam);
        draft.removeByName("LVWAP");

        DeviceApply.Result result = apply(List.of(steam), Set.of());

        assertEquals(List.of("LVWAP"), result.removed());
        assertEquals(List.of("LVWAP"), result.cleanedUp());
        assertFalse(Files.readString(steam.controlSchemes().resolve("DeviceMappings.xml")).contains("LVWAP"));
        assertFalse(Files.exists(buttonMap(steam, "LVWAP")));
        assertTrue(master.pendingRemovals().isEmpty());
    }

    /**
     * Without its entry the game rejects the whole preset, so an entry the bindings name stays - compared ignoring
     * case - and stays pending until a .binds rewrite can move them (Alan, 2026-10-11).
     */
    @Test
    void anEntryTheBindingsNameIsHeldBackAndStaysPending() throws IOException {
        Target steam = steamInstallation();
        lvwapAppliedTo(steam);
        draft.removeByName("LVWAP");

        DeviceApply.Result result = apply(List.of(steam), Set.of("lvwap"));

        assertEquals(List.of("LVWAP"), result.heldBack());
        assertTrue(result.cleanedUp().isEmpty());
        assertTrue(Files.readString(steam.controlSchemes().resolve("DeviceMappings.xml")).contains("<LVWAP>"));
        assertTrue(Files.exists(buttonMap(steam, "LVWAP")));
        assertEquals(List.of("LVWAP"), master.pendingRemovals());
    }

    @Test
    void bindingsThatCannotBeReadHoldEveryRemovalBack() throws IOException {
        Target steam = steamInstallation();
        lvwapAppliedTo(steam);
        draft.removeByName("LVWAP");

        DeviceApply.Result result = new DeviceApply(draft::promote, pusher(List.of(steam))::push,
                () -> { throw new IOException("locked"); }, FrontierStockDevices.getInstance(), master).apply();

        assertTrue(result.bindsUnread());
        assertEquals(List.of("LVWAP"), result.heldBack());
        assertTrue(Files.readString(steam.controlSchemes().resolve("DeviceMappings.xml")).contains("<LVWAP>"));
        assertEquals(List.of("LVWAP"), master.pendingRemovals());
    }

    /** An installation the removal could not reach keeps it pending, so the next Apply finishes the job. */
    @Test
    void aRemovalAnInstallationMissedStaysPending() throws IOException {
        Target steam = steamInstallation();
        lvwapAppliedTo(steam);
        draft.removeByName("LVWAP");

        DeviceApply.Result result = apply(List.of(steam, unplugged()), Set.of());

        assertEquals(List.of("LVWAP"), result.notEverywhere());
        assertTrue(result.cleanedUp().isEmpty());
        assertFalse(Files.readString(steam.controlSchemes().resolve("DeviceMappings.xml")).contains("LVWAP"),
                "the installation that was reachable lost it");
        assertEquals(List.of("LVWAP"), master.pendingRemovals());
    }

    /**
     * Review, 2026-10-11: RESET LABELS on a built-in the bindings name still takes effect. Frontier's element stays,
     * and .binds names inputs by token, never by label, so nothing a binding needs is lost.
     */
    @Test
    void aResetBuiltInIsAppliedEvenWhenTheBindingsNameIt() throws IOException {
        Target steam = steamInstallation();
        long id = master.record("T-Rudder", "044F", "B679", true).id();
        master.replaceLabels(id, Map.of("Joy_XAxis", "RUDDER"));
        apply(List.of(steam), Set.of());
        draft.removeByName("T-Rudder");

        DeviceApply.Result result = apply(List.of(steam), Set.of("T-Rudder"));

        assertEquals(List.of("T-Rudder"), result.cleanedUp());
        assertTrue(result.heldBack().isEmpty());
        assertFalse(Files.exists(buttonMap(steam, "T-Rudder")));
        assertEquals(DEVICE_MAPPINGS, Files.readString(steam.controlSchemes().resolve("DeviceMappings.xml")),
                "Frontier's element is left exactly as it was");
        assertTrue(master.pendingRemovals().isEmpty());
    }

    /** Unreadable bindings hold back the user's entries, never a reset - it needs nothing from them. */
    @Test
    void unreadableBindingsDoNotHoldBackAReset() throws IOException {
        Target steam = steamInstallation();
        long id = master.record("T-Rudder", "044F", "B679", true).id();
        master.replaceLabels(id, Map.of("Joy_XAxis", "RUDDER"));
        apply(List.of(steam), Set.of());
        draft.removeByName("T-Rudder");

        DeviceApply.Result result = new DeviceApply(draft::promote, pusher(List.of(steam))::push,
                () -> { throw new IOException("locked"); }, FrontierStockDevices.getInstance(), master).apply();

        assertFalse(result.bindsUnread(), "nothing was held back for it");
        assertEquals(List.of("T-Rudder"), result.cleanedUp());
    }

    /**
     * Review, 2026-10-11: .binds is read only when a pending removal is the user's - otherwise it decides nothing, and
     * a failed read would log a hold-back that never happened.
     */
    @Test
    void bindingsAreNotReadWhenNoNameOfTheUsersIsPending() throws IOException {
        Target steam = steamInstallation();
        master.record("LVWAP", "3344", "83F4", true);
        List<String> reads = new ArrayList<>();

        new DeviceApply(draft::promote, pusher(List.of(steam))::push,
                () -> { reads.add("read"); return Set.of(); }, FrontierStockDevices.getInstance(), master).apply();

        assertTrue(reads.isEmpty());
    }

    /**
     * Onboarding's hex case lands here: once an installation holds the entry, the bindings naming the device by its
     * hardware id move to its name - built from the value found (A6b, 2026-10-14).
     */
    @Test
    void bindingsNamingAMasterDeviceByHardwareIdMoveToItsName() throws IOException {
        master.record("LVWAP", "3344", "83F4", true);
        FakeRewrite rewrite = new FakeRewrite(Map.of("83f43344", 5, "334403F5", 2));

        DeviceApply.Result result = applyWith(List.of(steamInstallation()), rewrite);

        assertEquals(List.of("LVWAP <- [83f43344]"), rewrite.calls, "the value found, in the order found");
        assertEquals(List.of(new DeviceApply.Move("LVWAP", 5, DeviceApply.MoveOutcome.MOVED,
                DeviceRewriteResult.Status.REWRITTEN)), result.moves());
    }

    /** The user's Apply comes first: one installation missing does not hold the move back (Alan, 2026-10-14). */
    @Test
    void aMissingInstallationDoesNotHoldTheMoveBack() throws IOException {
        master.record("LVWAP", "3344", "83F4", true);
        FakeRewrite rewrite = new FakeRewrite(Map.of("334483F4", 5));

        DeviceApply.Result result = applyWith(List.of(steamInstallation(), unplugged()), rewrite);

        assertEquals(DeviceApply.MoveOutcome.MOVED, result.moves().getFirst().outcome());
    }

    /** With no installation holding the entry nothing was applied for the bindings to follow - they wait. */
    @Test
    void noInstallationTookTheEntrySoTheBindingsWait() {
        master.record("LVWAP", "3344", "83F4", true);
        FakeRewrite rewrite = new FakeRewrite(Map.of("334483F4", 5));

        DeviceApply.Result result = applyWith(List.of(unplugged()), rewrite);

        assertTrue(rewrite.calls.isEmpty());
        assertEquals(DeviceApply.MoveOutcome.NOT_APPLIED, result.moves().getFirst().outcome());
    }

    /** One of Frontier's names is never a target: its element is Frontier's definition, not the user's. */
    @Test
    void aFrontierNameIsNeverATarget() throws IOException {
        master.record("T-Rudder", "044F", "B679", true);
        FakeRewrite rewrite = new FakeRewrite(Map.of("044FB679", 3));

        DeviceApply.Result result = applyWith(List.of(steamInstallation()), rewrite);

        assertTrue(rewrite.calls.isEmpty());
        assertTrue(result.moves().isEmpty());
    }

    @Test
    void bindingsThatCannotBeReadMoveNothingAndSaySo() throws IOException {
        master.record("LVWAP", "3344", "83F4", true);
        FakeRewrite rewrite = new FakeRewrite(null);

        DeviceApply.Result result = applyWith(List.of(steamInstallation()), rewrite);

        assertTrue(result.movesUnread());
        assertTrue(rewrite.calls.isEmpty());
    }

    /**
     * One rewrite per device, back to back (Alan, 2026-10-14): a second one that meets the bindings monitor
     * mid-reload stops on its own, is reported, and leaves the first moved - the next Apply moves it.
     */
    @Test
    void twoDevicesMoveOneRewriteEachAndAFailureStopsOnlyItsOwn() throws IOException {
        master.record("LVWAP", "3344", "83F4", true);
        master.record("RVWAP", "3344", "83F5", true);
        FakeRewrite rewrite = new FakeRewrite(Map.of("334483F4", 5, "334483F5", 7));
        rewrite.answers.put("RVWAP", DeviceRewriteResult.Status.STALE_FILE);

        DeviceApply.Result result = applyWith(List.of(steamInstallation()), rewrite);

        assertEquals(List.of("LVWAP <- [334483F4]", "RVWAP <- [334483F5]"), rewrite.calls);
        assertEquals(List.of(
                new DeviceApply.Move("LVWAP", 5, DeviceApply.MoveOutcome.MOVED, DeviceRewriteResult.Status.REWRITTEN),
                new DeviceApply.Move("RVWAP", 7, DeviceApply.MoveOutcome.FAILED, DeviceRewriteResult.Status.STALE_FILE)),
                result.moves());
    }

    /** Two master devices on one VID/PID: the first in the master's order takes the bindings, the other nothing. */
    @Test
    void aSharedHardwareIdGoesToTheFirstInMasterOrder() throws IOException {
        master.record("LVWAP", "3344", "83F4", true);
        master.record("AlphaStick", "3344", "83F4", true);
        FakeRewrite rewrite = new FakeRewrite(Map.of("334483F4", 5));

        DeviceApply.Result result = applyWith(List.of(steamInstallation()), rewrite);

        assertEquals(List.of("AlphaStick <- [334483F4]"), rewrite.calls);
        assertEquals(1, result.moves().size());
    }

    /** The game saved .binds between the count and the rewrite: nothing was there to move, and it says so. */
    @Test
    void aMoveThatFindsNothingIsNotReportedAsMoved() throws IOException {
        master.record("LVWAP", "3344", "83F4", true);
        FakeRewrite rewrite = new FakeRewrite(Map.of("334483F4", 5));
        rewrite.answers.put("LVWAP", DeviceRewriteResult.Status.NO_REFERENCES);

        DeviceApply.Result result = applyWith(List.of(steamInstallation()), rewrite);

        assertEquals(DeviceApply.MoveOutcome.NOT_FOUND, result.moves().getFirst().outcome());
    }

    // ------------------------------------------------------------------ renames (A11)

    /**
     * A rename applied: the element and the .buttonMap take the new name, the bindings follow, and with every
     * installation and .binds done the old name is forgotten (Alan, 2026-10-14, A11).
     */
    @Test
    void aRenamedDeviceTakesItsBindingsAlong() throws IOException {
        Target steam = steamInstallation();
        lvwapAppliedTo(steam);
        draft.saveDevice("LVWAP", "LeftStick", Map.of("Joy_1", "LV MAIN TRIGGER"));
        FakeRewrite rewrite = new FakeRewrite(Map.of());
        rewrite.names.put("LVWAP", 4);

        DeviceApply.Result result = applyWith(List.of(steam), rewrite);

        assertEquals(List.of("LeftStick <- [LVWAP]"), rewrite.calls);
        assertEquals(List.of(new DeviceApply.RenameMove("LVWAP", "LeftStick", 4, DeviceApply.MoveOutcome.MOVED,
                DeviceRewriteResult.Status.REWRITTEN)), result.renameMoves());
        String mappings = Files.readString(steam.controlSchemes().resolve("DeviceMappings.xml"));
        assertTrue(mappings.contains("<LeftStick>"), mappings);
        assertFalse(mappings.contains("LVWAP"), mappings);
        assertTrue(Files.exists(buttonMap(steam, "LeftStick")));
        assertFalse(Files.exists(buttonMap(steam, "LVWAP")), "renamed, never copied");
        assertEquals(List.of("LeftStick"), result.renamesFinished());
        assertNull(master.findByName("LeftStick").previousName(), "the rename is finished");
        assertTrue(master.pendingRemovals().isEmpty(), "a renamed device is not a removed one");
    }

    /** The partial rule: one installation took it, so .binds moves - and the old name stays for the one that missed. */
    @Test
    void aRenameAnInstallationMissedMovesTheBindingsAndKeepsTheOldName() throws IOException {
        Target steam = steamInstallation();
        lvwapAppliedTo(steam);
        draft.saveDevice("LVWAP", "LeftStick", Map.of("Joy_1", "LV MAIN TRIGGER"));
        FakeRewrite rewrite = new FakeRewrite(Map.of());
        rewrite.names.put("LVWAP", 4);

        DeviceApply.Result result = applyWith(List.of(steam, unplugged()), rewrite);

        assertEquals(DeviceApply.MoveOutcome.MOVED, result.renameMoves().getFirst().outcome());
        assertTrue(result.renamesFinished().isEmpty());
        assertEquals("LVWAP", master.findByName("LeftStick").previousName(),
                "the push still renames wherever the old name is found");
    }

    @Test
    void noInstallationTookTheRenameSoTheBindingsWait() {
        master.record("LVWAP", "3344", "83F4", true);
        draft.saveDevice("LVWAP", "LeftStick", Map.of());
        FakeRewrite rewrite = new FakeRewrite(Map.of());
        rewrite.names.put("lvwap", 4);

        DeviceApply.Result result = applyWith(List.of(unplugged()), rewrite);

        assertTrue(rewrite.calls.isEmpty());
        assertEquals(List.of(new DeviceApply.RenameMove("LVWAP", "LeftStick", 4,
                DeviceApply.MoveOutcome.NOT_APPLIED, null)), result.renameMoves(),
                "counted ignoring case, as the game matches it");
        assertEquals("LVWAP", master.findByName("LeftStick").previousName());
    }

    /** Measured 2026-10-14: .binds finds an element ignoring case, so a case-only rename moves no binding. */
    @Test
    void aCaseOnlyRenameMovesNoBinding() throws IOException {
        Target steam = steamInstallation();
        lvwapAppliedTo(steam);
        draft.saveDevice("LVWAP", "Lvwap", Map.of("Joy_1", "LV MAIN TRIGGER"));
        FakeRewrite rewrite = new FakeRewrite(Map.of());
        rewrite.names.put("LVWAP", 4);

        DeviceApply.Result result = applyWith(List.of(steam), rewrite);

        assertTrue(rewrite.calls.isEmpty());
        assertTrue(result.renameMoves().isEmpty());
        assertTrue(Files.readString(steam.controlSchemes().resolve("DeviceMappings.xml")).contains("<Lvwap>"));
        assertTrue(Files.exists(buttonMap(steam, "Lvwap")));
        assertNull(master.findByName("Lvwap").previousName());
    }

    /** A move that stops keeps the old name, so the next Apply moves what is left. */
    @Test
    void aRenameMoveThatStopsKeepsTheOldNameForTheNextApply() throws IOException {
        Target steam = steamInstallation();
        lvwapAppliedTo(steam);
        draft.saveDevice("LVWAP", "LeftStick", Map.of("Joy_1", "LV MAIN TRIGGER"));
        FakeRewrite rewrite = new FakeRewrite(Map.of());
        rewrite.names.put("LVWAP", 4);
        rewrite.answers.put("LeftStick", DeviceRewriteResult.Status.STALE_FILE);

        DeviceApply.Result result = applyWith(List.of(steam), rewrite);

        assertEquals(DeviceApply.MoveOutcome.FAILED, result.renameMoves().getFirst().outcome());
        assertTrue(result.renamesFinished().isEmpty());
        assertEquals("LVWAP", master.findByName("LeftStick").previousName());
    }

    @Test
    void bindingsThatCannotBeReadMoveNoRenameAndSaySo() throws IOException {
        Target steam = steamInstallation();
        lvwapAppliedTo(steam);
        draft.saveDevice("LVWAP", "LeftStick", Map.of("Joy_1", "LV MAIN TRIGGER"));
        FakeRewrite rewrite = new FakeRewrite(Map.of());
        rewrite.names = null;

        DeviceApply.Result result = applyWith(List.of(steam), rewrite);

        assertTrue(result.movesUnread());
        assertTrue(rewrite.calls.isEmpty());
        assertEquals("LVWAP", master.findByName("LeftStick").previousName());
    }

    /** Several renames per Apply, one rewrite each (Alan, 2026-10-14): both are moved and both reported. */
    @Test
    void twoRenamesInOneApplyAreEachMovedAndReported() throws IOException {
        Target steam = steamInstallation();
        lvwapAppliedTo(steam);
        master.record("RVWAP", "3344", "03F5", true);
        draft.saveDevice("LVWAP", "LeftStick", Map.of("Joy_1", "LV MAIN TRIGGER"));
        draft.saveDevice("RVWAP", "RightStick", Map.of());
        FakeRewrite rewrite = new FakeRewrite(Map.of());
        rewrite.names.put("LVWAP", 4);
        rewrite.names.put("RVWAP", 6);

        DeviceApply.Result result = applyWith(List.of(steam), rewrite);

        assertEquals(Set.of("LeftStick <- [LVWAP]", "RightStick <- [RVWAP]"), Set.copyOf(rewrite.calls));
        assertEquals(Set.of("LeftStick", "RightStick"), Set.copyOf(result.renamesFinished()));
        assertEquals(2, result.renameMoves().size());
    }

    /**
     * Review, 2026-10-15 (A11): a device that leaves the master while its rename is still under way keeps its old name
     * pending - and held back while .binds names it - rather than losing it with the row, which would leave those
     * bindings on a device with no entry.
     */
    @Test
    void aDeviceClearedMidRenameHoldsItsOldNameBack() throws IOException {
        Target steam = steamInstallation();
        lvwapAppliedTo(steam);
        draft.saveDevice("LVWAP", "LeftStick", Map.of("Joy_1", "LV MAIN TRIGGER"));
        FakeRewrite stalled = new FakeRewrite(Map.of());
        stalled.names.put("LVWAP", 4);
        stalled.answers.put("LeftStick", DeviceRewriteResult.Status.STALE_FILE);
        applyWith(List.of(steam), stalled);
        assertEquals("LVWAP", master.findByName("LeftStick").previousName(), "the move stopped");
        draft.removeByName("LeftStick");

        DeviceApply.Result result = apply(List.of(steam), Set.of("LVWAP"));

        assertEquals(List.of("LeftStick"), result.cleanedUp());
        assertEquals(List.of("LVWAP"), result.heldBack(), "the bindings still name it");
        assertEquals(List.of("LVWAP"), master.pendingRemovals(), "kept for the Apply that can move them");
    }

    // ------------------------------------------------------------------ catch-up (A14)

    /**
     * Catch-up writes the installation that missed an Apply, as Apply would - and never the draft: what the user saved
     * and has not applied stays theirs to apply (Alan, 2026-10-15, A14).
     */
    @Test
    void catchUpWritesTheMasterToTheInstallationAndLeavesTheDraftAlone() throws IOException {
        Target steam = steamInstallation();
        lvwapAppliedTo(steam);
        Target epic = epicInstallation();
        draft.record("RVWAP", "3344", "03F5", true);

        DeviceApply.Result result = catchUp(List.of(steam, epic), List.of(epic), new FakeRewrite(Map.of()));

        assertEquals(Outcome.WRITTEN, result.report().installations().getFirst().outcome());
        assertEquals(1, result.report().installations().size(), "only the installation found again");
        String mappings = Files.readString(epic.controlSchemes().resolve("DeviceMappings.xml"));
        assertTrue(mappings.contains("<LVWAP>"), mappings);
        assertFalse(mappings.contains("RVWAP"), "the draft is not applied by catch-up");
        assertTrue(Files.exists(buttonMap(epic, "LVWAP")));
        assertTrue(draft.isStarted(), "the draft is still the user's to apply");
        assertTrue(result.removed().isEmpty());
    }

    /** An installation edited in the game meanwhile is not the question's to revert - catch-up leaves it alone. */
    @Test
    void catchUpTouchesNoOtherInstallation() throws IOException {
        Target steam = steamInstallation();
        lvwapAppliedTo(steam);
        Path steamMappings = steam.controlSchemes().resolve("DeviceMappings.xml");
        Files.writeString(steamMappings, DEVICE_MAPPINGS);
        Target epic = epicInstallation();

        catchUp(List.of(steam, epic), List.of(epic), new FakeRewrite(Map.of()));

        assertEquals(DEVICE_MAPPINGS, Files.readString(steamMappings));
    }

    /**
     * CLEAR, then an Apply that missed one installation: catch-up carries the removal to it, and with no installation
     * holding the device any more, it stops being pending.
     */
    @Test
    void catchUpCarriesAClearAndSettlesIt() throws IOException {
        Target steam = steamInstallation();
        Target epic = epicInstallation();
        long id = master.record("LVWAP", "3344", "83F4", true).id();
        master.replaceLabels(id, Map.of("Joy_1", "LV MAIN TRIGGER"));
        apply(List.of(steam, epic), Set.of());
        draft.removeByName("LVWAP");
        apply(List.of(steam, epicAway()), Set.of());
        assertEquals(List.of("LVWAP"), master.pendingRemovals(), "the missed installation still holds it");

        DeviceApply.Result result = catchUp(List.of(steam, epic), List.of(epic), new FakeRewrite(Map.of()));

        assertEquals(List.of("LVWAP"), result.cleanedUp());
        assertFalse(Files.readString(epic.controlSchemes().resolve("DeviceMappings.xml")).contains("LVWAP"));
        assertFalse(Files.exists(buttonMap(epic, "LVWAP")));
        assertTrue(master.pendingRemovals().isEmpty());
    }

    /**
     * An installation still out of reach that missed a write may hold the device - so the removal stays pending, for
     * whatever reaches that installation next.
     */
    @Test
    void catchUpKeepsARemovalPendingWhileAMissedInstallationIsStillAway() throws IOException {
        Target steam = steamInstallation();
        Target epic = epicInstallation();
        long id = master.record("LVWAP", "3344", "83F4", true).id();
        master.replaceLabels(id, Map.of("Joy_1", "LV MAIN TRIGGER"));
        apply(List.of(steam, epic), Set.of());
        draft.removeByName("LVWAP");
        apply(List.of(steam, epicAway()), Set.of());
        Target frontierAway = new Target(3, "FRONTIER", temp.resolve("gone").resolve("ControlSchemes"), true, true);

        DeviceApply.Result result = catchUp(List.of(steam, epic, frontierAway), List.of(epic),
                new FakeRewrite(Map.of()));

        assertEquals(List.of("LVWAP"), result.notEverywhere());
        assertFalse(Files.readString(epic.controlSchemes().resolve("DeviceMappings.xml")).contains("LVWAP"),
                "the installation found again lost it all the same");
        assertEquals(List.of("LVWAP"), master.pendingRemovals());
    }

    /** A removal the bindings still name is held back by catch-up, as Apply holds it back. */
    @Test
    void catchUpHoldsBackARemovalTheBindingsName() throws IOException {
        Target steam = steamInstallation();
        Target epic = epicInstallation();
        long id = master.record("LVWAP", "3344", "83F4", true).id();
        master.replaceLabels(id, Map.of("Joy_1", "LV MAIN TRIGGER"));
        apply(List.of(steam, epic), Set.of());
        draft.removeByName("LVWAP");
        apply(List.of(steam, epicAway()), Set.of());

        DeviceApply.Result result = new DeviceApply(draft::promote, pusher(List.of(steam, epic)),
                () -> Set.of("LVWAP"), FrontierStockDevices.getInstance(), master, new FakeRewrite(Map.of()),
                holdings(List.of(steam, epic))).catchUp(List.of(epic.installId()));

        assertEquals(List.of("LVWAP"), result.heldBack());
        assertTrue(Files.readString(epic.controlSchemes().resolve("DeviceMappings.xml")).contains("<LVWAP>"));
        assertEquals(List.of("LVWAP"), master.pendingRemovals());
    }

    /** A rename one installation missed: catch-up renames it there, and the rename is finished. */
    @Test
    void catchUpFinishesARename() throws IOException {
        Target steam = steamInstallation();
        Target epic = epicInstallation();
        long id = master.record("LVWAP", "3344", "83F4", true).id();
        master.replaceLabels(id, Map.of("Joy_1", "LV MAIN TRIGGER"));
        apply(List.of(steam, epic), Set.of());
        draft.saveDevice("LVWAP", "LeftStick", Map.of("Joy_1", "LV MAIN TRIGGER"));
        FakeRewrite moved = new FakeRewrite(Map.of());
        moved.names.put("LVWAP", 4);
        new DeviceApply(draft::promote, pusher(List.of(steam, epicAway()))::push, Set::of,
                FrontierStockDevices.getInstance(), master, moved).apply();
        assertEquals("LVWAP", master.findByName("LeftStick").previousName(), "the missed installation holds it");

        DeviceApply.Result result = catchUp(List.of(steam, epic), List.of(epic), new FakeRewrite(Map.of()));

        String mappings = Files.readString(epic.controlSchemes().resolve("DeviceMappings.xml"));
        assertTrue(mappings.contains("<LeftStick>"), mappings);
        assertFalse(mappings.contains("LVWAP"), mappings);
        assertFalse(Files.exists(buttonMap(epic, "LVWAP")), "renamed, never copied");
        assertEquals(List.of("LeftStick"), result.renamesFinished());
        assertNull(master.findByName("LeftStick").previousName());
    }

    /**
     * No installation had taken a rename - the bindings stayed on the old name. Catch-up is then the first to take it,
     * and the bindings follow, as Apply would have moved them.
     */
    @Test
    void catchUpThatIsTheFirstToTakeARenameMovesTheBindings() throws IOException {
        Target epic = epicInstallation();
        long id = master.record("LVWAP", "3344", "83F4", true).id();
        master.replaceLabels(id, Map.of("Joy_1", "LV MAIN TRIGGER"));
        apply(List.of(epic), Set.of());
        draft.saveDevice("LVWAP", "LeftStick", Map.of("Joy_1", "LV MAIN TRIGGER"));
        FakeRewrite waiting = new FakeRewrite(Map.of());
        waiting.names.put("LVWAP", 4);
        new DeviceApply(draft::promote, pusher(List.of(epicAway()))::push, Set::of,
                FrontierStockDevices.getInstance(), master, waiting).apply();
        assertTrue(waiting.calls.isEmpty(), "nothing was applied for the bindings to follow");

        DeviceApply.Result result = catchUp(List.of(epic), List.of(epic), waiting);

        assertEquals(List.of("LeftStick <- [LVWAP]"), waiting.calls);
        assertEquals(DeviceApply.MoveOutcome.MOVED, result.renameMoves().getFirst().outcome());
    }

    private DeviceApply.Result catchUp(List<Target> targets, List<Target> foundAgain, FakeRewrite rewrite) {
        return new DeviceApply(draft::promote, pusher(targets), Set::of, FrontierStockDevices.getInstance(),
                master, rewrite, holdings(targets))
                .catchUp(foundAgain.stream().map(Target::installId).toList());
    }

    private static InstallationHoldings holdings(List<Target> targets) {
        return new InstallationHoldings(() -> targets, FrontierStockDevices.getInstance(),
                FrontierStockButtonMaps.getInstance());
    }

    /** Epic, at its folder, with Frontier's file. */
    private Target epicInstallation() throws IOException {
        Path controlSchemes = temp.resolve("EPIC").resolve("ControlSchemes");
        Files.createDirectories(controlSchemes);
        Files.writeString(controlSchemes.resolve("DeviceMappings.xml"), DEVICE_MAPPINGS);
        return new Target(2, "EPIC", controlSchemes, false);
    }

    /** The same Epic installation, out of reach - an unplugged drive. */
    private Target epicAway() {
        return new Target(2, "EPIC", temp.resolve("EPIC").resolve("ControlSchemes"), true);
    }

    private DeviceApply.Result applyWith(List<Target> targets, DeviceApply.BindsRewrite rewrite) {
        return new DeviceApply(draft::promote, pusher(targets)::push, Set::of, FrontierStockDevices.getInstance(),
                master, rewrite).apply();
    }

    /**
     * Hardware-id and name counts as given, and the rewrites asked for - each succeeding unless told otherwise.
     * Answers are keyed by the name a rewrite moves to.
     */
    private static final class FakeRewrite implements DeviceApply.BindsRewrite {
        private final Map<String, Integer> counts;
        /** {@code null} to fail the read. */
        Map<String, Integer> names = new HashMap<>();
        final Map<String, DeviceRewriteResult.Status> answers = new HashMap<>();
        final List<String> calls = new ArrayList<>();

        /** @param counts {@code null} to fail the read */
        FakeRewrite(Map<String, Integer> counts) {
            this.counts = counts;
        }

        @Override
        public Map<String, Integer> hardwareIdCounts() throws IOException {
            if (counts == null) throw new IOException("bindings folder not there");
            return counts;
        }

        @Override
        public Map<String, Integer> nameCounts() throws IOException {
            if (names == null) throw new IOException("bindings folder not there");
            return names;
        }

        @Override
        public DeviceApply.Rewritten rewrite(DeviceReferenceRewrite rewrite) {
            calls.add(rewrite.to() + " <- " + rewrite.from().stream().sorted().toList());
            DeviceRewriteResult.Status status =
                    answers.getOrDefault(rewrite.to(), DeviceRewriteResult.Status.REWRITTEN);
            int moved = status == DeviceRewriteResult.Status.REWRITTEN
                    ? rewrite.from().stream()
                    .mapToInt(from -> counts.getOrDefault(from, names.getOrDefault(from, 0))).sum()
                    : 0;
            return new DeviceApply.Rewritten(status, moved);
        }
    }

    private void lvwapAppliedTo(Target steam) {
        long id = master.record("LVWAP", "3344", "83F4", true).id();
        master.replaceLabels(id, Map.of("Joy_1", "LV MAIN TRIGGER"));
        apply(List.of(steam), Set.of());
        assertTrue(Files.exists(buttonMap(steam, "LVWAP")));
    }

    private DeviceApply.Result apply(List<Target> targets, Set<String> namedInBinds) {
        return new DeviceApply(draft::promote, pusher(targets)::push, () -> namedInBinds,
                FrontierStockDevices.getInstance(), master).apply();
    }

    private DeviceFilesPush pusher(List<Target> targets) {
        return new DeviceFilesPush(() -> targets, this::storedMaster,
                FrontierStockDevices.getInstance(), new TimestampedBackups(), temp.resolve("history"), () -> 10,
                target -> { });
    }

    private List<MasterDevice> storedMaster() {
        List<MasterDevice> devices = new ArrayList<>();
        for (DeviceRow row : master.findAll()) {
            devices.add(new MasterDevice(row.deviceName(), row.vid(), row.pid(), master.labelsOf(row.id()),
                    row.previousName()));
        }
        return devices;
    }

    private Target unplugged() {
        return new Target(2, "EPIC", temp.resolve("unplugged").resolve("ControlSchemes"), false);
    }

    private Target steamInstallation() throws IOException {
        Path controlSchemes = temp.resolve("STEAM").resolve("ControlSchemes");
        Files.createDirectories(controlSchemes);
        Files.writeString(controlSchemes.resolve("DeviceMappings.xml"), DEVICE_MAPPINGS);
        return new Target(1, "STEAM", controlSchemes, false);
    }

    private static Path buttonMap(Target target, String device) {
        return target.controlSchemes().resolve("DeviceButtonMaps").resolve(device + ".buttonMap");
    }
}
