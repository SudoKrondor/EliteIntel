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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Activation, the first opening of the BindForge tab (Alan, 2026-10-13): the "before BindForge" backup, then the
 * master filled with no prompt when the installations are identical, and first setup when they are not. Taking the
 * backup once is {@link BeforeBindForgeBackupTest}'s.
 */
class ActivationTest {

    private static final HardwareId LEFT = new HardwareId("3344", "83F4");

    @TempDir
    Path temp;

    private final List<String> order = new ArrayList<>();
    private boolean activated;
    private boolean backupWorks = true;
    private boolean pushWouldWrite;
    private FirstSetup.Establish establish = (devices, removals) -> {
        order.add("establish " + devices.stream().map(StoredDevice::deviceName).toList());
        activated = true;
        return true;
    };

    @Test
    void anActivatedBindForgeIsLeftAlone() throws IOException {
        activated = true;

        Activation.Result result = activation(List.of(reachable(1)), sameEverywhere()).run();

        assertEquals(Activation.Outcome.ALREADY_ACTIVATED, result.outcome());
        assertTrue(order.isEmpty(), "no backup, no read");
    }

    @Test
    void identicalInstallationsAreBackedUpAndActivatedWithNoPrompt() throws IOException {
        Activation.Result result = activation(List.of(reachable(1), reachable(2)), sameEverywhere()).run();

        assertEquals(Activation.Outcome.ACTIVATED, result.outcome());
        assertEquals(List.of("backup", "establish [LVWAP]"), order);
    }

    @Test
    void aBackupThatFailsStopsActivation() throws IOException {
        backupWorks = false;

        Activation.Result result = activation(List.of(reachable(1)), sameEverywhere()).run();

        assertEquals(Activation.Outcome.BACKUP_FAILED, result.outcome());
        assertTrue(result.reason().contains("disk full"));
        assertEquals(List.of("backup"), order, "nothing read, nothing activated");
    }

    /** Any difference opens first setup - even one with nothing to ask, such as a file only the push would add. */
    @Test
    void aDifferenceTheFillWouldWriteOpensFirstSetup() throws IOException {
        pushWouldWrite = true;

        Activation.Result result = activation(List.of(reachable(1), reachable(2)), sameEverywhere()).run();

        assertEquals(Activation.Outcome.FIRST_SETUP, result.outcome());
        assertNotNull(result.reading());
        assertNotNull(result.setup());
        assertNull(result.reason(), "nothing went wrong, so first setup has nothing to name");
        assertEquals(List.of("backup"), order, "first setup writes nothing until its last button");
    }

    @Test
    void aQuestionOpensFirstSetup() throws IOException {
        Activation.Result result = activation(List.of(reachable(1), reachable(2)),
                target -> new FirstSetupPlan.Installation(target.installId(), target.storefront(),
                        Map.of("LVWAP", target.installId() == 1 ? LEFT : new HardwareId("3344", "83F3")),
                        Map.of(), Map.of())).run();

        assertEquals(Activation.Outcome.FIRST_SETUP, result.outcome());
        assertFalse(order.contains("establish [LVWAP]"));
    }

    /** An unread file could hold the one entry the master must not lose, so it is never activated past. */
    @Test
    void anUnreadableInstallationOpensFirstSetupNamingIt() throws IOException {
        Activation.Result result = activation(List.of(reachable(1)), target -> {
            throw new IOException("DeviceMappings.xml is not well-formed");
        }).run();

        assertEquals(Activation.Outcome.FIRST_SETUP, result.outcome());
        assertNull(result.reading());
        assertTrue(result.reason().contains("STEAM"), "the reason names the installation");
        assertEquals(List.of("backup"), order);
    }

    /** A file the push's plan cannot read is named too, so first setup does not open on a reading that looks whole. */
    @Test
    void aFileThePushCannotReadOpensFirstSetupNamingIt() throws IOException {
        Target steam = reachable(1);
        Activation activation = new Activation(() -> activated, this::backup,
                () -> firstSetup(List.of(steam), sameEverywhere()),
                (target, master) -> {
                    throw new IOException(target.storefront() + ": LVWAP.buttonMap is locked");
                }, establish);

        Activation.Result result = activation.run();

        assertEquals(Activation.Outcome.FIRST_SETUP, result.outcome());
        assertNotNull(result.reading());
        assertTrue(result.reason().contains("LVWAP.buttonMap is locked"));
        assertFalse(order.contains("establish [LVWAP]"));
    }

    @Test
    void aMasterThatCannotBeSavedIsAFailureAndActivatesNothing() throws IOException {
        establish = (devices, removals) -> {
            throw new IllegalStateException("database locked");
        };

        Activation.Result result = activation(List.of(reachable(1)), sameEverywhere()).run();

        assertEquals(Activation.Outcome.FAILED, result.outcome());
        assertTrue(result.reason().contains("database locked"));
        assertFalse(activated);
    }

    /** Activated meanwhile - by first setup finishing first, say. Nothing is overwritten. */
    @Test
    void aMasterFilledMeanwhileIsAlreadyActivated() throws IOException {
        establish = (devices, removals) -> false;

        Activation.Result result = activation(List.of(reachable(1)), sameEverywhere()).run();

        assertEquals(Activation.Outcome.ALREADY_ACTIVATED, result.outcome());
    }

    @Test
    void noInstallationActivatesWithAnEmptyMaster() {
        Activation.Result result = activation(List.of(), sameEverywhere()).run();

        assertEquals(Activation.Outcome.ACTIVATED, result.outcome());
        assertEquals(List.of("backup", "establish []"), order);
    }

    @Test
    void anInstallationWhoseFolderIsGoneIsLeftOutOfIdentical() {
        pushWouldWrite = true;
        Target gone = new Target(2, "EPIC", temp.resolve("unplugged"), false);
        Activation activation = new Activation(() -> activated, this::backup,
                () -> firstSetup(List.of(gone), sameEverywhere()), (target, master) -> {
                    order.add("planned " + target.storefront());
                    return pushWouldWrite;
                }, establish);

        assertEquals(Activation.Outcome.ACTIVATED, activation.run().outcome());
        assertFalse(order.contains("planned EPIC"));
    }

    /** Against the real push: Frontier's file in both and nothing of the user's is identical; no file at all is not. */
    @Test
    void identicalIsWhatThePushWouldWrite() throws IOException {
        Target steam = withFile(1, "STEAM", true);
        Target epic = withFile(2, "EPIC", true);
        Target bare = withFile(3, "FRONTIER", false);
        Activation.PushPlanner push = (target, master) ->
                !DeviceFilesPush.plan(target, master, FrontierStockDevices.getInstance()).writes().isEmpty();

        assertTrue(Activation.fillWithoutWriting(reading(List.of(steam, epic)), push).isPresent());
        assertTrue(Activation.fillWithoutWriting(reading(List.of(steam, bare)), push).isEmpty(),
                "the push would seed the missing file from Frontier's");
    }

    private Activation activation(List<Target> targets, FirstSetup.Reader reader) {
        return new Activation(() -> activated, this::backup, () -> firstSetup(targets, reader),
                (target, master) -> pushWouldWrite, establish);
    }

    private void backup() throws IOException {
        order.add("backup");
        if (!backupWorks) throw new IOException("disk full");
    }

    private static FirstSetup firstSetup(List<Target> targets, FirstSetup.Reader reader) {
        return new FirstSetup(() -> targets, reader, Set::of, taken -> null, (devices, removals) -> false,
                () -> new Report(List.of()), (report, handedOver) -> new DeviceApply.SetupMoves(List.of(), false));
    }

    private static FirstSetup.Reader sameEverywhere() {
        return target -> new FirstSetupPlan.Installation(target.installId(), target.storefront(),
                Map.of("LVWAP", LEFT), Map.of(), Map.of());
    }

    private static FirstSetup.Reading reading(List<Target> targets) throws IOException {
        return firstSetup(targets, target -> new FirstSetupPlan.Installation(target.installId(), target.storefront(),
                Map.of(), Map.of(), Map.of())).read();
    }

    private Target reachable(long id) throws IOException {
        String storefront = id == 1 ? "STEAM" : "EPIC";
        return new Target(id, storefront, Files.createDirectories(temp.resolve(storefront)), false);
    }

    private Target withFile(long id, String storefront, boolean stockFile) throws IOException {
        Path controlSchemes = Files.createDirectories(temp.resolve(storefront).resolve("ControlSchemes"));
        if (stockFile) Files.write(controlSchemes.resolve("DeviceMappings.xml"), DeviceFilesPush.stockFile());
        return new Target(id, storefront, controlSchemes, false);
    }
}
