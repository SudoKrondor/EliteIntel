package elite.intel.bindforge.devices;

import elite.intel.bindforge.devices.DeviceRename.Allowed;
import elite.intel.bindforge.devices.DeviceRename.Named;
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
 * The Device Editor's rename rule (A11, 2026-10-14): named devices, an attached built-in by shadowing it (A15), one
 * rename at a time, and no name still waiting as some device's old name.
 */
class DeviceRenameTest {

    @Test
    void aNamedDeviceOfTheUsersCanBeRenamed() {
        assertEquals(Allowed.YES, DeviceRename.allowed("LVWAP", USER, ATTACHED, false, "LVWAP", null));
    }

    @Test
    void anUnnamedDeviceAnUnpluggedBuiltInAndAClearedOneCannot() {
        assertEquals(Allowed.NO_NAME, DeviceRename.allowed(null, USER, ATTACHED, false, null, null));
        assertEquals(Allowed.BUILT_IN,
                DeviceRename.allowed("T-Rudder", FRONTIER, UNPLUGGED, false, "T-Rudder", null));
        assertEquals(Allowed.CLEARED, DeviceRename.allowed("LVWAP", USER, ATTACHED, true, "LVWAP", null));
    }

    /** The master keeps one old name per device, so a rename still under way blocks a second one. */
    @Test
    void aRenameStillUnderWayBlocksAnother() {
        assertEquals(Allowed.PENDING, DeviceRename.allowed("LeftStick", USER, ATTACHED, false, "LeftStick", "LVWAP"));
    }

    /**
     * A rename only in the draft can be renamed again - the draft keeps the installations' name, whatever comes after
     * - while one the master still carries cannot.
     */
    @Test
    void aDraftOnlyRenameMayBeRenamedAgainButAMasterPendingOneMayNot() {
        assertEquals(Allowed.YES, DeviceRename.allowed("LeftStick", USER, ATTACHED, false, "LVWAP", null));
        assertEquals(Allowed.PENDING, DeviceRename.allowed("LeftStick", USER, ATTACHED, false, "LeftStick", "LVWAP"));
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

    /** A built-in is renamed by shadowing it, while it is attached - its VID/PID comes from the hardware (A15). */
    @Test
    void anAttachedBuiltInCanBeRenamed() {
        assertEquals(Allowed.YES, DeviceRename.allowed("T-Rudder", FRONTIER, ATTACHED, false, null, null));
    }

    /** Going back is Frontier's name exactly as Frontier spells it - never another spelling the game would confuse. */
    @Test
    void goingBackToFrontierIsItsNameSpelledExactly() {
        assertTrue(DeviceRename.backToFrontier("T-Rudder", "T-Rudder"));
        assertFalse(DeviceRename.backToFrontier("t-rudder", "T-Rudder"));
        assertFalse(DeviceRename.backToFrontier("TRudder", "T-Rudder"));
    }

    /** Review, 2026-10-17: pad A labelled under DualShock4 closes going back to it for pad B's shadow. */
    @Test
    void goingBackIsClosedWhileAnotherControllerHoldsFrontiersName() {
        assertTrue(DeviceRename.frontierNameHeld("DualShock4", Set.of("DualShock4", "PadB")));
        assertFalse(DeviceRename.frontierNameHeld("DualShock4", Set.of("PadB")));
        assertFalse(DeviceRename.frontierNameHeld(null, Set.of("PadB")));
    }

    /**
     * Review, 2026-10-17 (A15): a built-in opens under Frontier's name, which is one of its own - yet it stays taken, so
     * another spelling of it is refused. Only {@link DeviceRename#backToFrontier} accepts Frontier's name.
     */
    @Test
    void aBuiltInsOwnFrontierNameStaysTaken() {
        Set<String> taken = DeviceRename.taken(List.of("T-Rudder"), List.of("T-Rudder"), List.of(), List.of(),
                own("T-Rudder", null, null));

        assertTrue(taken.contains("T-Rudder"));
        assertFalse(DeviceNames.validate("t-rudder", taken).valid());
        assertFalse(DeviceNames.validate("TRudder", taken).valid());
        assertTrue(DeviceNames.validate("MyRudder", taken).valid());
    }

    /** A shadow saved and not applied carries Frontier's name as its old one, which stays taken for a second rename. */
    @Test
    void aShadowsFrontierOldNameStaysTaken() {
        Set<String> taken = DeviceRename.taken(List.of("T-Rudder"), List.of(), List.of(),
                List.of(new Named("MyRudder", "T-Rudder")), own("MyRudder", null, "T-Rudder"));

        assertFalse(DeviceNames.validate("t-rudder", taken).valid());
        assertTrue(DeviceNames.validate("Pedals", taken).valid());
    }

    private static List<String> own(String... names) {
        return Arrays.asList(names);
    }
}
