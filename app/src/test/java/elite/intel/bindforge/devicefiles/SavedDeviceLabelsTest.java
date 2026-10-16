package elite.intel.bindforge.devicefiles;

import elite.intel.db.managers.BindForgeDeviceDraftManager;
import elite.intel.db.managers.BindForgeDeviceMasterManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The bindings grid reads a controller's inputs by the names saved in the Device Editor: the draft's when it holds
 * the device, else the master's - the same rule the Device Editor opens with.
 */
class SavedDeviceLabelsTest {

    private final BindForgeDeviceMasterManager master = BindForgeDeviceMasterManager.getInstance();
    private final BindForgeDeviceDraftManager draft = BindForgeDeviceDraftManager.getInstance();

    @BeforeEach
    void aMasterHoldingOneNamedStick() {
        draft.discard();
        master.findAll().forEach(row -> master.remove(row.id()));
        master.pendingRemovals().forEach(master::clearPendingRemoval);
        long stick = master.record("VKB-Stick", "231D", "0200", true).id();
        master.replaceLabels(stick, Map.of("Joy_6", "1st Hat Up", "Joy_7", " "));
    }

    @Test
    void withNoDraftTheMastersNamesShow() {
        assertEquals(Optional.of("1st Hat Up"), SavedDeviceLabels.load().label("VKB-Stick", "Joy_6"));
        assertEquals(Map.of("Joy_6", "1st Hat Up", "Joy_7", " "), SavedDeviceLabels.of("VKB-Stick"));
    }

    @Test
    void aSavedButUnappliedNameShowsAtOnce() {
        draft.saveLabels("VKB-Stick", "231D", "0200", Map.of("Joy_6", "Gear"));

        assertEquals(Optional.of("Gear"), SavedDeviceLabels.load().label("VKB-Stick", "Joy_6"));
        assertEquals(Map.of("Joy_6", "Gear"), SavedDeviceLabels.of("VKB-Stick"));
    }

    @Test
    void aRenamedDeviceIsStillFoundByTheNameTheBindsFileHoldsUntilApply() {
        draft.saveDevice("VKB-Stick", "Right Stick", Map.of("Joy_6", "Gear"));

        SavedDeviceLabels labels = SavedDeviceLabels.load();
        assertEquals(Optional.of("Gear"), labels.label("VKB-Stick", "Joy_6"));
        assertEquals(Optional.of("Gear"), labels.label("Right Stick", "Joy_6"));
    }

    @Test
    void aBlankNameOrAnUnknownInputOrDeviceHasNoLabel() {
        SavedDeviceLabels labels = SavedDeviceLabels.load();
        assertEquals(Optional.empty(), labels.label("VKB-Stick", "Joy_7"));
        assertEquals(Optional.empty(), labels.label("VKB-Stick", "Joy_9"));
        assertEquals(Optional.empty(), labels.label("VPCThrottle", "Joy_6"));
        assertEquals(Optional.empty(), labels.label(null, "Joy_6"));
    }
}
