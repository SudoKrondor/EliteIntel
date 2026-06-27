package elite.intel.ai.hands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The save-time occupancy check must be context-aware: a chord used only in a mutually-exclusive
 * context (camera / SRV / on-foot) does not block it for a ship binding, matching the keyboard
 * widget and {@link BindingConflictScanner}.
 */
class KeyboardKeyAvailabilityServiceTest {

    @TempDir
    Path tempDir;

    private final KeyboardKeyAvailabilityService service = new KeyboardKeyAvailabilityService();

    private Path write(String xml) throws Exception {
        Path file = tempDir.resolve("binds-" + System.nanoTime() + ".binds");
        Files.write(file, xml.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    @Test
    void keyUsedOnlyInAnotherContextIsNotOccupied() throws Exception {
        // bare Q owned by a camera sub-mode binding must NOT block bare Q for a ship binding.
        Path file = write("""
                <Root>
                  <FreeCamSpeedInc><Primary Device="Keyboard" Key="Key_Q" /><Secondary Device="{NoDevice}" Key="" /></FreeCamSpeedInc>
                  <GalaxyMapOpen><Primary Device="{NoDevice}" Key="" /><Secondary Device="{NoDevice}" Key="" /></GalaxyMapOpen>
                </Root>
                """);
        assertFalse(service.isKeyOccupiedByOtherSlot(file, "GalaxyMapOpen", BindingSlotType.PRIMARY, "Key_Q"));
    }

    @Test
    void keyUsedInTheSameContextIsOccupied() throws Exception {
        // bare Q owned by another ship binding DOES block bare Q for a ship binding.
        Path file = write("""
                <Root>
                  <LandingGearToggle><Primary Device="Keyboard" Key="Key_Q" /><Secondary Device="{NoDevice}" Key="" /></LandingGearToggle>
                  <GalaxyMapOpen><Primary Device="{NoDevice}" Key="" /><Secondary Device="{NoDevice}" Key="" /></GalaxyMapOpen>
                </Root>
                """);
        assertTrue(service.isKeyOccupiedByOtherSlot(file, "GalaxyMapOpen", BindingSlotType.PRIMARY, "Key_Q"));
    }

    @Test
    void aBindingNeverBlocksItsOwnOtherSlot() throws Exception {
        // Q on the binding's primary must not block assigning Q to its own secondary.
        Path file = write("""
                <Root>
                  <GalaxyMapOpen><Primary Device="Keyboard" Key="Key_Q" /><Secondary Device="{NoDevice}" Key="" /></GalaxyMapOpen>
                </Root>
                """);
        assertFalse(service.isKeyOccupiedByOtherSlot(file, "GalaxyMapOpen", BindingSlotType.SECONDARY, "Key_Q"));
    }
}
