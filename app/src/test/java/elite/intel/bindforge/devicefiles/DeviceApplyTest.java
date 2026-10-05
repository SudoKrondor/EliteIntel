package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceFilesPush.MasterDevice;
import elite.intel.bindforge.devicefiles.DeviceFilesPush.Outcome;
import elite.intel.bindforge.devicefiles.DeviceFilesPush.Report;
import elite.intel.bindforge.devicefiles.DeviceFilesPush.Target;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Apply for the device files: the draft becomes the master first, so a write that fails loses none of the
 * user's edits.
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
    }

    @Test
    void theDraftIsPromotedBeforeAnythingIsPushed() {
        List<String> order = new ArrayList<>();
        Report empty = new Report(List.of());

        DeviceApply.Result result = new DeviceApply(
                () -> { order.add("promote"); return List.of("RVWAP"); },
                () -> { order.add("push"); return empty; }).apply();

        assertEquals(List.of("promote", "push"), order);
        assertEquals(List.of("RVWAP"), result.removed(), "what the draft removed is handed to the caller");
    }

    @Test
    void savedEditsReachEveryInstallation() throws IOException {
        Target steam = steamInstallation();
        long row = draft.record("RVWAP", "3344", "03F5", true).id();
        draft.replaceLabels(row, Map.of("Joy_1", "RV Main Trigger"));

        DeviceApply.Result result = apply(List.of(steam));

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
        Target gone = new Target(1, "EPIC", temp.resolve("unplugged").resolve("ControlSchemes"), false);
        draft.record("RVWAP", "3344", "03F5", true);

        DeviceApply.Result result = apply(List.of(gone));

        assertEquals(Outcome.SKIPPED_MISSING, result.report().installations().getFirst().outcome());
        assertNotNull(master.findByName("RVWAP"), "the edit is in the master, waiting for the next Apply");
    }

    /** With nothing saved, Apply pushes the master as it stands - the retry after a partial Apply. */
    @Test
    void withNoDraftTheMasterIsStillPushed() throws IOException {
        Target steam = steamInstallation();
        master.record("LVWAP", "3344", "83F4", true);

        DeviceApply.Result result = apply(List.of(steam));

        assertTrue(result.removed().isEmpty());
        assertEquals(Outcome.WRITTEN, result.report().installations().getFirst().outcome());
        assertTrue(Files.readString(steam.controlSchemes().resolve("DeviceMappings.xml")).contains("<LVWAP>"));
    }

    private DeviceApply.Result apply(List<Target> targets) {
        DeviceFilesPush push = new DeviceFilesPush(() -> targets, this::storedMaster,
                FrontierStockDevices.getInstance(), new TimestampedBackups(), temp.resolve("history"), () -> 10,
                target -> { });
        return new DeviceApply(draft::promote, push::push).apply();
    }

    private List<MasterDevice> storedMaster() {
        List<MasterDevice> devices = new ArrayList<>();
        for (DeviceRow row : master.findAll()) {
            devices.add(new MasterDevice(row.deviceName(), row.vid(), row.pid(), master.labelsOf(row.id())));
        }
        return devices;
    }

    private Target steamInstallation() throws IOException {
        Path controlSchemes = temp.resolve("STEAM").resolve("ControlSchemes");
        Files.createDirectories(controlSchemes);
        Files.writeString(controlSchemes.resolve("DeviceMappings.xml"), DEVICE_MAPPINGS);
        return new Target(1, "STEAM", controlSchemes, false);
    }
}
