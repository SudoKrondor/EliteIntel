package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceEntry.HardwareId;
import elite.intel.bindforge.devicefiles.DeviceFilesPush.Report;
import elite.intel.bindforge.devicefiles.DeviceFilesPush.Target;
import elite.intel.db.managers.BindForgeDeviceDraftManager.StoredDevice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * First setup writes nothing until applied, then backs up, fills the master and pushes - in that order, and not a
 * step further than the one that failed.
 */
class FirstSetupTest {

    private static final HardwareId LEFT = new HardwareId("3344", "83F4");

    @TempDir
    Path temp;

    private final List<String> order = new ArrayList<>();

    @Test
    void anUnreachableInstallationIsNotReadAndIsNamed() throws IOException {
        Target steam = reachable(1);
        Target gone = new Target(2, "EPIC", temp.resolve("unplugged"), false);

        FirstSetup.Reading reading = setup(List.of(steam, gone), true, true).read();

        assertEquals(List.of(gone), reading.unreachable());
        assertEquals(List.of(1L), reading.plan().installations().stream()
                .map(FirstSetupPlan.Installation::installId).toList());
        assertTrue(order.isEmpty(), "reading writes nothing");
    }

    @Test
    void applyingBacksUpThenFillsTheMasterThenPushes() throws IOException {
        FirstSetup setup = setup(List.of(reachable(1)), true, true);

        FirstSetup.Result result = setup.apply(setup.read(), Map.of());

        assertEquals(FirstSetup.Outcome.APPLIED, result.outcome());
        assertEquals(List.of("backup", "establish [LVWAP]", "push", "moves {}"), order);
    }

    /**
     * A16: a losing name .binds uses hands its bindings over straight after the push - the winner keeps it as its old
     * name, and a second loser is handed over by name.
     */
    @Test
    void theLosingNamesHandTheirBindingsOverAfterThePush() throws IOException {
        HardwareId right = new HardwareId("3344", "03F5");
        Map<Long, String> names = Map.of(1L, "RVWAP", 2L, "RightStick", 3L, "Stick");
        List<StoredDevice> established = new ArrayList<>();
        List<Target> targets = List.of(reachable(1), reachable(2), reachable(3));
        FirstSetup setup = new FirstSetup(() -> targets,
                target -> new FirstSetupPlan.Installation(target.installId(), target.storefront(),
                        Map.of(names.get(target.installId()), right), Map.of(), Map.of()),
                () -> Set.of("RightStick", "Stick"),
                taken -> { order.add("backup"); return temp; },
                (devices, removals) -> { established.addAll(devices); order.add("establish " + removals); return true; },
                () -> { order.add("push"); return new Report(List.of()); },
                this::moves);
        FirstSetup.Reading reading = setup.read();

        FirstSetup.Result result = setup.apply(reading, Map.of(reading.plan().rows().getFirst().id(), "RVWAP"));

        assertEquals(FirstSetup.Outcome.APPLIED, result.outcome());
        assertEquals(List.of("backup", "establish [Stick]", "push", "moves {Stick=RVWAP}"), order);
        assertEquals("RightStick", established.getFirst().previousName());
        assertNotNull(result.moves());
    }

    /** The installations are written, so first setup is over even when the bindings could not be moved. */
    @Test
    void aMoveThatThrowsAfterThePushEndsFirstSetup() throws IOException {
        FirstSetup setup = new FirstSetup(List::of, target -> null, Set::of,
                taken -> temp,
                (devices, removals) -> true,
                () -> new Report(List.of()),
                (report, handedOver) -> { throw new IllegalStateException("bindings locked"); });

        FirstSetup.Result result = setup.apply(setup.read(), Map.of());

        assertEquals(FirstSetup.Outcome.FAILED, result.outcome());
        assertTrue(result.masterFilled());
        assertNotNull(result.report(), "the push landed");
        assertTrue(result.reason().contains("bindings locked"));
    }

    @Test
    void aBackupThatFailsStopsEverything() throws IOException {
        FirstSetup setup = setup(List.of(reachable(1)), false, true);

        FirstSetup.Result result = setup.apply(setup.read(), Map.of());

        assertEquals(FirstSetup.Outcome.BACKUP_FAILED, result.outcome());
        assertEquals(List.of("backup"), order);
        assertNull(result.report());
    }

    @Test
    void aMasterFilledMeanwhileIsNotOverwrittenOrPushed() throws IOException {
        FirstSetup setup = setup(List.of(reachable(1)), true, false);

        FirstSetup.Result result = setup.apply(setup.read(), Map.of());

        assertEquals(FirstSetup.Outcome.ALREADY_SET_UP, result.outcome());
        assertTrue(result.masterFilled(), "first setup is over, so the dialog closes");
        assertEquals(List.of("backup", "establish [LVWAP]"), order);
    }

    @Test
    void anUnansweredQuestionIsRefusedBeforeAnythingIsWritten() throws IOException {
        Target steam = reachable(1);
        Target epic = reachable(2);
        FirstSetup setup = new FirstSetup(() -> List.of(steam, epic),
                target -> new FirstSetupPlan.Installation(target.installId(), target.storefront(),
                        Map.of("LVWAP", target.installId() == 1 ? LEFT : new HardwareId("3344", "83F3")),
                        Map.of(), Map.of()),
                Set::of, targets -> { order.add("backup"); return temp; },
                (devices, removals) -> { order.add("establish"); return true; },
                () -> { order.add("push"); return new Report(List.of()); }, this::moves);

        FirstSetup.Reading reading = setup.read();

        assertThrows(IllegalStateException.class, () -> setup.apply(reading, Map.of()));
        assertTrue(order.isEmpty());
    }

    /** The master is saved in one transaction, so a failure there leaves first setup to run again. */
    @Test
    void aMasterThatCannotBeSavedIsAFailureNamedAsSuchAndNothingIsPushed() throws IOException {
        FirstSetup setup = new FirstSetup(List::of, target -> null, Set::of,
                taken -> { order.add("backup"); return temp; },
                (devices, removals) -> { throw new IllegalStateException("database locked"); },
                () -> { order.add("push"); return new Report(List.of()); }, this::moves);

        FirstSetup.Result result = setup.apply(setup.read(), Map.of());

        assertEquals(FirstSetup.Outcome.FAILED, result.outcome());
        assertTrue(result.reason().contains("database locked"));
        assertFalse(result.masterFilled());
        assertEquals(List.of("backup"), order);
    }

    /** Not "the backup failed": the backup worked and the master is saved, so first setup is over. */
    @Test
    void aPushThatThrowsAfterTheMasterIsSavedEndsFirstSetup() throws IOException {
        FirstSetup setup = new FirstSetup(List::of, target -> null, Set::of,
                taken -> temp,
                (devices, removals) -> true,
                () -> { throw new IllegalStateException(); }, this::moves);

        FirstSetup.Result result = setup.apply(setup.read(), Map.of());

        assertEquals(FirstSetup.Outcome.FAILED, result.outcome());
        assertTrue(result.masterFilled());
        assertTrue(result.reason().contains("IllegalStateException"), "no message is shown by kind, not as null");
    }

    private FirstSetup setup(List<Target> targets, boolean backupWorks, boolean masterEmpty) {
        return new FirstSetup(() -> targets,
                target -> new FirstSetupPlan.Installation(target.installId(), target.storefront(),
                        Map.of("LVWAP", LEFT), Map.of(), Map.of()),
                Set::of,
                taken -> {
                    order.add("backup");
                    if (!backupWorks) throw new IOException("disk full");
                    return temp.resolve("backup");
                },
                (devices, removals) -> {
                    order.add("establish " + devices.stream().map(StoredDevice::deviceName).toList());
                    return masterEmpty;
                },
                () -> {
                    order.add("push");
                    return new Report(List.of());
                },
                this::moves);
    }

    private DeviceApply.SetupMoves moves(Report report, Map<String, String> handedOver) {
        order.add("moves " + handedOver);
        return new DeviceApply.SetupMoves(List.of(), false);
    }

    private Target reachable(long id) throws IOException {
        return new Target(id, id == 1 ? "STEAM" : id == 2 ? "EPIC" : "FRONTIER",

                Files.createDirectories(temp.resolve(String.valueOf(id)).resolve("ControlSchemes")), false);
    }
}
