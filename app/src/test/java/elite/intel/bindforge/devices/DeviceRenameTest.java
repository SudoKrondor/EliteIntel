package elite.intel.bindforge.devices;

import elite.intel.bindforge.devices.DeviceRename.Allowed;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static elite.intel.bindforge.devices.DeviceRename.Owner.FRONTIER;
import static elite.intel.bindforge.devices.DeviceRename.Owner.USER;
import static elite.intel.bindforge.devices.DeviceRename.Presence.ATTACHED;
import static elite.intel.bindforge.devices.DeviceRename.Presence.UNPLUGGED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Device Editor's rename rule (A11, 2026-10-14): named devices, an attached built-in by shadowing it (A15), and no
 * name the working copy holds until the next APPLY - no chains. A rename is part of the edits since Storage S4a, so a
 * device renamed and not applied can be renamed again.
 */
class DeviceRenameTest {

    @Test
    void aNamedDeviceOfTheUsersCanBeRenamed() {
        assertEquals(Allowed.YES, DeviceRename.allowed("LVWAP", USER, ATTACHED, false));
    }

    @Test
    void anUnnamedDeviceAnUnpluggedBuiltInAndAClearedOneCannot() {
        assertEquals(Allowed.NO_NAME, DeviceRename.allowed(null, USER, ATTACHED, false));
        assertEquals(Allowed.BUILT_IN, DeviceRename.allowed("T-Rudder", FRONTIER, UNPLUGGED, false));
        assertEquals(Allowed.CLEARED, DeviceRename.allowed("LVWAP", USER, ATTACHED, true));
    }

    /** No chains: a name the working copy holds is taken, though a device gave it up in the edits. */
    @Test
    void aNameTheWorkingCopyHoldsIsTaken() {
        Set<String> taken = DeviceRename.taken(List.of("T-Rudder"), List.of("GamePad", "LeftStick", "LVWAP", "RVWAP"),
                own("RVWAP"));

        assertEquals(Set.of("T-Rudder", "GamePad", "LeftStick", "LVWAP"), taken);
        assertFalse(DeviceNames.validate("lvwap", taken).valid());
    }

    /**
     * A device's own names stay usable - changing only case is a rename, and so is going back to the name the working
     * copy holds - while the same names belonging to another device do not.
     */
    @Test
    void theDevicesOwnNamesAreNotTaken() {
        Set<String> taken = DeviceRename.taken(List.of(), List.of("LVWAP", "LeftStick"), own("LeftStick", "LVWAP"));

        assertTrue(DeviceNames.validate("LVWAP", taken).valid());
        assertTrue(DeviceNames.validate("Leftstick", taken).valid());
    }

    /** A built-in is renamed by shadowing it, while it is attached - its VID/PID comes from the hardware (A15). */
    @Test
    void anAttachedBuiltInCanBeRenamed() {
        assertEquals(Allowed.YES, DeviceRename.allowed("T-Rudder", FRONTIER, ATTACHED, false));
    }

    /** Going back is Frontier's name exactly as Frontier spells it - never another spelling the game would confuse. */
    @Test
    void goingBackToFrontierIsItsNameSpelledExactly() {
        assertTrue(DeviceRename.backToFrontier("T-Rudder", "T-Rudder"));
        assertFalse(DeviceRename.backToFrontier("t-rudder", "T-Rudder"));
        assertFalse(DeviceRename.backToFrontier("TRudder", "T-Rudder"));
    }

    /**
     * Review, 2026-10-17 (A15): a built-in opens under Frontier's name, which is one of its own - yet it stays taken, so
     * another spelling of it is refused. Only {@link DeviceRename#backToFrontier} accepts Frontier's name.
     */
    @Test
    void aBuiltInsOwnFrontierNameStaysTaken() {
        Set<String> taken = DeviceRename.taken(List.of("T-Rudder"), List.of("T-Rudder"), own("T-Rudder", null));

        assertTrue(taken.contains("T-Rudder"));
        assertFalse(DeviceNames.validate("t-rudder", taken).valid());
        assertFalse(DeviceNames.validate("TRudder", taken).valid());
        assertTrue(DeviceNames.validate("MyRudder", taken).valid());
    }

    private static List<String> own(String... names) {
        return Arrays.asList(names);
    }
}
