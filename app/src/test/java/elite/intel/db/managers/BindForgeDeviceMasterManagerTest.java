package elite.intel.db.managers;

import elite.intel.db.dao.BindForgeDeviceMasterDao.DeviceRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The master holds the user's device entries - what Apply writes outward - and its labels are the content of
 * a {@code .buttonMap}.
 */
class BindForgeDeviceMasterManagerTest {

    private final BindForgeDeviceMasterManager master = BindForgeDeviceMasterManager.getInstance();

    @BeforeEach
    void emptyTheMaster() {
        master.findAll().forEach(row -> master.remove(row.id()));
    }

    @Test
    void recordingTheSameAliasTwiceKeepsTheFirstRow() {
        DeviceRow first = master.record("LVWAP", "3344", "83F4", false);
        DeviceRow again = master.record("LVWAP", "3344", "83F4", false);

        assertEquals(first.id(), again.id(), "the same alias is the same device, however often it is recorded");
        assertEquals(1, master.findAll().size());
    }

    /**
     * Frontier's own file has no case convention - {@code DualShock4} carries VID {@code 054C} uppercase
     * beside PID {@code 05c4} lowercase on one line - so a case-sensitive lookup would miss roughly half of
     * it, and hardware need not report the case a file used either.
     */
    @Test
    void hardwareIsMatchedWhateverCaseTheHexIsIn() {
        master.record("LVWAP", "3344", "83F4", true);

        assertNotNull(master.findByHardware("3344", "83f4"), "lowercase pid must still find it");
        assertNotNull(master.findByHardware("3344", "83F4"));
        assertNull(master.findByHardware("3344", "0000"), "a genuinely different device is still not a match");
    }

    @Test
    void renamingRemembersTheOldNameUntilTheRenameIsClosed() {
        long id = master.record("RVWAP", "3344", "03F5", true).id();

        master.rename(id, "RightStick");
        DeviceRow renamed = master.findByName("RightStick");

        assertNotNull(renamed, "the new name is the one in the master now");
        assertEquals("RVWAP", renamed.previousName(),
                "kept so every installation's old .buttonMap can be found and renamed with it");
        assertNull(master.findByName("RVWAP"), "and the old name is gone");

        master.clearPreviousName(id);
        assertNull(master.findByName("RightStick").previousName(), "the rename is finished");
    }

    /**
     * The firmware-update repair: the entry keeps its name, so every binding naming it follows without being
     * touched.
     */
    @Test
    void retargetingChangesTheHardwareAndKeepsTheName() {
        long id = master.record("LVWAP", "3344", "83F3", true).id();

        master.retarget(id, "3344", "83F4");

        DeviceRow row = master.findByName("LVWAP");
        assertEquals("83F4", row.pid());
        assertEquals("LVWAP", row.deviceName(), "the name is the whole reason this repair is one field");
    }

    @Test
    void aliasConfirmationIsRecordedForTheDeviceRatherThanPerInstallation() {
        long id = master.record("VirpilControls20220720", "3344", "83F4", false).id();
        assertFalse(master.findByName("VirpilControls20220720").aliasConfirmed());

        master.setAliasConfirmed(id, true);

        assertTrue(master.findByName("VirpilControls20220720").aliasConfirmed());
    }

    @Test
    void replacingLabelsDiscardsTheOnesThatWereThere() {
        long id = master.record("LVWAP", "3344", "83F4", true).id();
        master.replaceLabels(id, labels("Joy_1", "B1", "Joy_2", "B2"));

        master.replaceLabels(id, labels("Joy_1", "MAIN TRIGGER"));

        Map<String, String> labels = master.labelsOf(id);
        assertEquals(1, labels.size(), "a whole-set replacement, not a merge");
        assertEquals("MAIN TRIGGER", labels.get("Joy_1"));
    }

    @Test
    void aSingleLabelCanBeChangedWithoutTouchingTheRest() {
        long id = master.record("LVWAP", "3344", "83F4", true).id();
        master.replaceLabels(id, labels("Joy_1", "B1", "Joy_2", "B2"));

        master.putLabel(id, "Joy_2", "PINKY PADDLE");

        assertEquals("B1", master.labelsOf(id).get("Joy_1"));
        assertEquals("PINKY PADDLE", master.labelsOf(id).get("Joy_2"));
    }

    /**
     * The delete and the inserts are one transaction, not merely one call.
     * <p>
     * {@code Database.withDao} borrows a handle in autocommit, under which the delete commits by itself and a
     * failure part way through the inserts leaves the device holding a mixture of both label sets - which it
     * would then write outward on the next Apply. The null token below fails its NOT NULL constraint after
     * the first label has already been written, which is exactly that moment.
     */
    @Test
    void aFailedLabelReplacementLeavesTheOriginalSetIntact() {
        long id = master.record("LVWAP", "3344", "83F4", true).id();
        master.replaceLabels(id, labels("Joy_1", "B1", "Joy_2", "B2"));

        Map<String, String> halfBad = new LinkedHashMap<>();
        halfBad.put("Joy_1", "NEW");
        halfBad.put(null, "no token, so this insert fails");
        assertThrows(RuntimeException.class, () -> master.replaceLabels(id, halfBad));

        Map<String, String> labels = master.labelsOf(id);
        assertEquals(2, labels.size(), "the original pair survived, rather than being half replaced");
        assertEquals("B1", labels.get("Joy_1"), "and was not overwritten by the attempt");
        assertEquals("B2", labels.get("Joy_2"));
    }

    /** CLEAR returns a device to "not added", which means its labels go with it. */
    @Test
    void removingADeviceTakesItsLabelsWithIt() {
        long id = master.record("LVWAP", "3344", "83F4", true).id();
        master.replaceLabels(id, labels("Joy_1", "B1"));

        master.remove(id);

        assertTrue(master.labelsOf(id).isEmpty());
    }

    private static Map<String, String> labels(String... tokenThenLabel) {
        Map<String, String> labels = new LinkedHashMap<>();
        for (int i = 0; i < tokenThenLabel.length; i += 2) {
            labels.put(tokenThenLabel[i], tokenThenLabel[i + 1]);
        }
        return labels;
    }
}
