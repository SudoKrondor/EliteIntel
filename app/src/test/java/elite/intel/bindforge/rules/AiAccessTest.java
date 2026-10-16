package elite.intel.bindforge.rules;

import elite.intel.bindforge.io.KeyBindingsParser.BindingSlotType;
import elite.intel.bindforge.io.KeyBindingsParser.ReadOnlyBindingSlot;
import elite.intel.bindforge.io.KeyBindingsParser.ReadOnlyBindingSlots;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AiAccessTest {

    private static final String LANDING_GEAR = "LandingGearToggle";
    private final AiAccess access = new AiAccess(Set.of(LANDING_GEAR));

    @Test
    void aControlVegaNeverPressesIsNotJudgedWhateverItIsBoundTo() {
        assertEquals(AiAccess.State.NOT_USED, access.stateOf("HMD_Reset", slots(stick("Joy_3"), empty())));
        assertEquals(AiAccess.State.NOT_USED, access.stateOf("HMD_Reset", slots(empty(), empty())));
    }

    @Test
    void aKeyboardKeyInEitherSlotIsEnough() {
        assertEquals(AiAccess.State.READY, access.stateOf(LANDING_GEAR, slots(keyboard("Key_L"), empty())));
        assertEquals(AiAccess.State.READY, access.stateOf(LANDING_GEAR, slots(stick("Joy_6"), keyboard("Key_L"))));
    }

    @Test
    void aControlOnlyOnAStickIsOneVegaCannotPress() {
        assertEquals(AiAccess.State.CANT_PRESS, access.stateOf(LANDING_GEAR, slots(stick("Joy_6"), empty())));
    }

    @Test
    void aControlWithNothingBoundNeedsAKey() {
        assertEquals(AiAccess.State.NOT_BOUND, access.stateOf(LANDING_GEAR, slots(empty(), empty())));
        assertEquals(AiAccess.State.NOT_BOUND, access.stateOf(LANDING_GEAR, null));
    }

    @Test
    void bothSlotsTakenOnlyWhenNeitherIsFree() {
        assertTrue(AiAccess.bothSlotsTaken(slots(stick("Joy_6"), stick("Joy_7"))));
        assertFalse(AiAccess.bothSlotsTaken(slots(stick("Joy_6"), empty())));
        assertFalse(AiAccess.bothSlotsTaken(null));
    }

    private static ReadOnlyBindingSlots slots(ReadOnlyBindingSlot primary, ReadOnlyBindingSlot secondary) {
        return new ReadOnlyBindingSlots(primary, secondary);
    }

    private static ReadOnlyBindingSlot keyboard(String key) {
        return new ReadOnlyBindingSlot("Keyboard", key, null, null, false, BindingSlotType.PRIMARY, true, true);
    }

    private static ReadOnlyBindingSlot stick(String key) {
        return new ReadOnlyBindingSlot("VKB-Stick", key, null, null, false, BindingSlotType.PRIMARY, false, false);
    }

    private static ReadOnlyBindingSlot empty() {
        return new ReadOnlyBindingSlot("{NoDevice}", "", null, null, false, BindingSlotType.SECONDARY, false, true);
    }
}
