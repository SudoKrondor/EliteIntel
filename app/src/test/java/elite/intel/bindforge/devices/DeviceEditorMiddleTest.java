package elite.intel.bindforge.devices;

import elite.intel.bindforge.devices.DeviceEditorMiddle.Kind;
import elite.intel.bindforge.devices.DeviceEditorMiddle.Why;
import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Device Editor's middle button: CLEAR or RESET LABELS, its undo, and when it is refused (Alan, 2026-10-11). */
class DeviceEditorMiddleTest {

    @Test
    void theUsersDeviceInTheMasterCanBeCleared() {
        DeviceEditorMiddle middle = DeviceEditorMiddle.of("LVWAP", false, true, false, false, 0);

        assertEquals(Kind.CLEAR, middle.kind());
        assertTrue(middle.enabled());
        assertFalse(middle.undoing());
    }

    /**
     * An edit to an entry .binds depends on must update .binds with it. Clearing would mean moving those bindings to
     * the hardware ID, which is not built - so CLEAR is refused, and says how many bindings are at stake.
     */
    @Test
    void clearIsRefusedWhileBindingsNameTheDevice() {
        DeviceEditorMiddle middle = DeviceEditorMiddle.of("LVWAP", false, true, true, true, 142);

        assertEquals(Kind.CLEAR, middle.kind());
        assertFalse(middle.enabled());
        assertEquals(Why.NAMED_IN_BINDS, middle.why());
        assertEquals(142, middle.bindings());
    }

    /** .binds names inputs by token, never by label, so a reset never touches it. */
    @Test
    void aBuiltInsLabelsCanBeResetWhateverTheBindingsName() {
        DeviceEditorMiddle middle = DeviceEditorMiddle.of("T-Rudder", true, true, false, false, 30);

        assertEquals(Kind.RESET_LABELS, middle.kind());
        assertTrue(middle.enabled());
    }

    @Test
    void aBuiltInWithNothingSavedHasNothingToReset() {
        DeviceEditorMiddle middle = DeviceEditorMiddle.of("T-Rudder", true, false, false, false, 0);

        assertFalse(middle.enabled());
        assertEquals(Why.NOTHING_TO_RESET, middle.why());
    }

    @Test
    void aDeviceClearedInTheDraftOffersItsUndo() {
        DeviceEditorMiddle middle = DeviceEditorMiddle.of("LVWAP", false, true, true, false, 142);

        assertEquals(Kind.UNDO_CLEAR, middle.kind());
        assertTrue(middle.enabled(), "the undo is never refused: it puts back what the master holds");
        assertTrue(middle.undoing());
    }

    @Test
    void aBuiltInResetInTheDraftOffersItsUndo() {
        assertEquals(Kind.UNDO_RESET, DeviceEditorMiddle.of("T-Rudder", true, true, true, false, 0).kind());
    }

    /** With a draft, the draft is what CLEAR acts on: a device only the draft holds can be cleared from it. */
    @Test
    void aDeviceSavedOnlyInTheDraftCanStillBeCleared() {
        assertTrue(DeviceEditorMiddle.of("TRudder2", false, false, true, true, 0).enabled());
    }

    @Test
    void aDeviceNeitherHoldsHasNothingToClear() {
        DeviceEditorMiddle middle = DeviceEditorMiddle.of("Lvwap", false, false, false, false, 0);

        assertFalse(middle.enabled());
        assertEquals(Why.NOT_ADDED, middle.why());
    }

    @Test
    void aDeviceWithNoNameHasNothingToClear() {
        DeviceEditorMiddle middle = DeviceEditorMiddle.of(null, false, false, false, false, 0);

        assertFalse(middle.enabled());
        assertEquals(Why.NO_NAME, middle.why());
    }

    /** Bindings that cannot be read refuse CLEAR rather than guess; a reset needs nothing from them (review). */
    @Test
    void unreadableBindingsRefuseClearButNotReset() {
        DeviceEditorMiddle clear = DeviceEditorMiddle.of("LVWAP", false, true, false, false, OptionalInt.empty());
        DeviceEditorMiddle reset = DeviceEditorMiddle.of("T-Rudder", true, true, false, false, OptionalInt.empty());

        assertFalse(clear.enabled());
        assertEquals(Why.BINDS_UNREAD, clear.why());
        assertTrue(reset.enabled());
    }

    /** Review, 2026-10-15 (A11): clearing mid-rename would drop the old name the next Apply needs to finish it. */
    @Test
    void clearIsRefusedWhileARenameIsPendingButAnUndoIsNot() {
        DeviceEditorMiddle clear = DeviceEditorMiddle.of("LeftStick", false, true, false, false, 0).whileRenamePending();
        DeviceEditorMiddle undo = DeviceEditorMiddle.of("LeftStick", false, true, true, false, 0).whileRenamePending();

        assertFalse(clear.enabled());
        assertEquals(Why.RENAME_PENDING, clear.why());
        assertTrue(undo.enabled());
        assertEquals(Kind.UNDO_CLEAR, undo.kind());
    }
}
