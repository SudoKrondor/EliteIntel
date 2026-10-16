package elite.intel.bindforge.devices;

import elite.intel.bindforge.devices.DeviceRename.Allowed;
import elite.intel.bindforge.devices.DeviceRename.Named;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Device Editor's rename rule (A11, 2026-10-14): the user's own named devices only, one rename at a time, and no
 * name still waiting as some device's old name.
 */
class DeviceRenameTest {

    @Test
    void aNamedDeviceOfTheUsersCanBeRenamed() {
        assertEquals(Allowed.YES, DeviceRename.allowed("LVWAP", false, false, "LVWAP", null));
    }

    @Test
    void anUnnamedDeviceABuiltInAndAClearedOneCannot() {
        assertEquals(Allowed.NO_NAME, DeviceRename.allowed(null, false, false, null, null));
        assertEquals(Allowed.BUILT_IN, DeviceRename.allowed("T-Rudder", true, false, "T-Rudder", null));
        assertEquals(Allowed.CLEARED, DeviceRename.allowed("LVWAP", false, true, "LVWAP", null));
    }

    /** The master keeps one old name per device, so a rename still under way blocks a second one. */
    @Test
    void aRenameStillUnderWayBlocksAnother() {
        assertEquals(Allowed.PENDING, DeviceRename.allowed("LeftStick", false, false, "LeftStick", "LVWAP"));
    }

    /**
     * A rename only in the draft can be renamed again - the draft keeps the installations' name, whatever comes after
     * - while one the master still carries cannot.
     */
    @Test
    void aDraftOnlyRenameMayBeRenamedAgainButAMasterPendingOneMayNot() {
        assertEquals(Allowed.YES, DeviceRename.allowed("LeftStick", false, false, "LVWAP", null));
        assertEquals(Allowed.PENDING, DeviceRename.allowed("LeftStick", false, false, "LeftStick", "LVWAP"));
    }

    /** No chains: an old name still waiting is taken, so no other device can move into it. */
    @Test
    void anOldNameStillWaitingIsTaken() {
        Set<String> taken = DeviceRename.taken(List.of("T-Rudder"), List.of("GamePad"),
                List.of(new Named("LeftStick", "LVWAP"), new Named("RVWAP", null)),
                List.of(new Named("Throttle", null)), own("RVWAP", "RVWAP", null));

        assertEquals(Set.of("T-Rudder", "GamePad", "LeftStick", "LVWAP", "Throttle"), taken);
        assertFalse(DeviceNames.validate("lvwap", taken).valid());
    }

    /**
     * A device's own names stay usable - changing only case is a rename, and so is going back to the name the
     * installations still hold - while the same names belonging to another device do not.
     */
    @Test
    void theDevicesOwnNamesAreNotTaken() {
        Set<String> taken = DeviceRename.taken(List.of(), List.of("LVWAP"),
                List.of(new Named("LVWAP", null)), List.of(new Named("LeftStick", "LVWAP")),
                own("LeftStick", "LVWAP", "LVWAP"));

        assertTrue(DeviceNames.validate("LVWAP", taken).valid());
        assertTrue(DeviceNames.validate("Leftstick", taken).valid());
    }

    private static List<String> own(String... names) {
        return Arrays.asList(names);
    }
}
