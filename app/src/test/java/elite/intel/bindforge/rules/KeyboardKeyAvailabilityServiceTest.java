package elite.intel.bindforge.rules;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The save-time occupancy check must be context-aware: a chord used only in a mutually-exclusive
 * context (camera / SRV / on-foot) does not block it for a ship binding, matching the keyboard
 * widget and {@link BindingConflictScanner}.
 */
class KeyboardKeyAvailabilityServiceTest {

    private final KeyboardKeyAvailabilityService service = new KeyboardKeyAvailabilityService();

    private static byte[] binds(String xml) {
        return xml.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void keyUsedOnlyInAnotherContextIsNotOccupied() throws Exception {
        // bare Q owned by a camera sub-mode binding must NOT block bare Q for a ship binding.
        byte[] file = binds("""
                <Root>
                  <FreeCamSpeedInc><Primary Device="Keyboard" Key="Key_Q" /><Secondary Device="{NoDevice}" Key="" /></FreeCamSpeedInc>
                  <GalaxyMapOpen><Primary Device="{NoDevice}" Key="" /><Secondary Device="{NoDevice}" Key="" /></GalaxyMapOpen>
                </Root>
                """);
        assertFalse(service.isKeyOccupiedByOtherSlot(file, "GalaxyMapOpen", "Key_Q", List.of()));
    }

    @Test
    void keyUsedInTheSameContextIsOccupied() throws Exception {
        // bare Q owned by another ship binding DOES block bare Q for a ship binding.
        byte[] file = binds("""
                <Root>
                  <LandingGearToggle><Primary Device="Keyboard" Key="Key_Q" /><Secondary Device="{NoDevice}" Key="" /></LandingGearToggle>
                  <GalaxyMapOpen><Primary Device="{NoDevice}" Key="" /><Secondary Device="{NoDevice}" Key="" /></GalaxyMapOpen>
                </Root>
                """);
        assertTrue(service.isKeyOccupiedByOtherSlot(file, "GalaxyMapOpen", "Key_Q", List.of()));
    }

    @Test
    void aBindingNeverBlocksItsOwnOtherSlot() throws Exception {
        // Q on the binding's primary must not block assigning Q to its own secondary.
        byte[] file = binds("""
                <Root>
                  <GalaxyMapOpen><Primary Device="Keyboard" Key="Key_Q" /><Secondary Device="{NoDevice}" Key="" /></GalaxyMapOpen>
                </Root>
                """);
        assertFalse(service.isKeyOccupiedByOtherSlot(file, "GalaxyMapOpen", "Key_Q", List.of()));
    }
}
