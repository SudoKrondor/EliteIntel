package elite.intel.bindforge.rules;

import elite.intel.bindforge.io.KeyBindingsParser.BindingSlotType;
import elite.intel.bindforge.io.KeyBindingsParser.ReadOnlyBindingSlot;
import elite.intel.bindforge.io.KeyBindingsParser.ReadOnlyBindingSlots;
import elite.intel.bindforge.model.BindingInput;
import elite.intel.bindforge.model.BindingModifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * An input another control holds blocks the capture dialog's SAVE - by the game's exact-chord model, and only
 * between controls that can be live together.
 */
class SlotInputConflictsTest {

    private static final BindingInput HAT_UP = new BindingInput("VKB-Stick", "Joy_6");
    private static final BindingInput TRIGGER = new BindingInput("VKB-Stick", "Joy_1");

    @Test
    void anInputAnotherShipControlHoldsAloneIsTaken() {
        Map<String, ReadOnlyBindingSlots> slots = Map.of("PrimaryFire", primary(slot("VKB-Stick", "Joy_6", false)));

        assertEquals(Optional.of("PrimaryFire"),
                SlotInputConflicts.holderOf("LandingGearToggle", HAT_UP, List.of(), false, slots));
    }

    @Test
    void theGameMatchesDeviceAndTokenIgnoringCase() {
        Map<String, ReadOnlyBindingSlots> slots = Map.of("PrimaryFire", primary(slot("vkb-stick", "JOY_6", false)));

        assertEquals(Optional.of("PrimaryFire"),
                SlotInputConflicts.holderOf("LandingGearToggle", HAT_UP, List.of(), false, slots));
    }

    @Test
    void theSameButtonWithAModifierHeldIsAnotherControl() {
        Map<String, ReadOnlyBindingSlots> slots = Map.of("PrimaryFire", primary(slot("VKB-Stick", "Joy_6", false)));

        assertEquals(Optional.empty(),
                SlotInputConflicts.holderOf("LandingGearToggle", HAT_UP, List.of(TRIGGER), false, slots));
    }

    @Test
    void modifiersMatchInAnyOrder() {
        ReadOnlyBindingSlot chord = new ReadOnlyBindingSlot("VKB-Stick", "Joy_6", null,
                List.of(new BindingModifier("VKB-Stick", "Joy_1"), new BindingModifier("Keyboard", "Key_LeftShift")),
                false, BindingSlotType.SECONDARY, false, false);
        Map<String, ReadOnlyBindingSlots> slots = Map.of("PrimaryFire", new ReadOnlyBindingSlots(empty(), chord));

        assertEquals(Optional.of("PrimaryFire"), SlotInputConflicts.holderOf("LandingGearToggle", HAT_UP,
                List.of(new BindingInput("Keyboard", "Key_LeftShift"), TRIGGER), false, slots));
    }

    @Test
    void aTapAndAHoldOnOneButtonAreTwoControls() {
        Map<String, ReadOnlyBindingSlots> slots = Map.of("PrimaryFire", primary(slot("VKB-Stick", "Joy_6", true)));

        assertEquals(Optional.empty(),
                SlotInputConflicts.holderOf("LandingGearToggle", HAT_UP, List.of(), false, slots));
    }

    @Test
    void aShipAndAnSrvControlNeverClash() {
        Map<String, ReadOnlyBindingSlots> slots =
                Map.of("BuggyPrimaryFireButton", primary(slot("VKB-Stick", "Joy_6", false)));

        assertEquals(Optional.empty(),
                SlotInputConflicts.holderOf("PrimaryFire", HAT_UP, List.of(), false, slots));
    }

    @Test
    void theControlBeingEditedNeverHoldsItsOwnInput() {
        Map<String, ReadOnlyBindingSlots> slots = Map.of("LandingGearToggle", primary(slot("VKB-Stick", "Joy_6", false)));

        assertEquals(Optional.empty(),
                SlotInputConflicts.holderOf("LandingGearToggle", HAT_UP, List.of(), false, slots));
    }

    private static ReadOnlyBindingSlots primary(ReadOnlyBindingSlot slot) {
        return new ReadOnlyBindingSlots(slot, empty());
    }

    private static ReadOnlyBindingSlot slot(String device, String key, boolean hold) {
        return new ReadOnlyBindingSlot(device, key, null, List.of(), hold, BindingSlotType.PRIMARY, false, false);
    }

    private static ReadOnlyBindingSlot empty() {
        return new ReadOnlyBindingSlot("{NoDevice}", "", null, List.of(), false, BindingSlotType.SECONDARY, false, true);
    }
}
