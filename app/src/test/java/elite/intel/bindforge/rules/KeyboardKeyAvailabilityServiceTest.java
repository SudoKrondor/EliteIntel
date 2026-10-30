package elite.intel.bindforge.rules;

import elite.intel.bindforge.model.BindingModifier;
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

    // WHY: the four below moved here from BindingsWriterTest (Storage S6R2, 2026-10-30) - they never call the writer.

    @Test
    void occupiedKeysAreCollectedFromEntireFileIncludingUnusedActions() throws Exception {
        byte[] file = binds("""
                <Root>
                    <GalaxyMapOpen>
                        <Primary Device="{NoDevice}" Key="" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </GalaxyMapOpen>
                    <UnusedByEliteIntel>
                        <Primary Device="Keyboard" Key="Key_L" />
                    </UnusedByEliteIntel>
                </Root>
                """);

        assertTrue(service.isKeyOccupiedByOtherSlot(file, "GalaxyMapOpen", "Key_L", List.of()));
    }

    @Test
    void joystickHotasMouseAssignmentsDoNotOccupyKeyboardKeys() throws Exception {
        byte[] file = binds("""
                <Root>
                    <JoystickAction>
                        <Primary Device="044F0404" Key="Key_L" />
                        <Secondary Device="Mouse" Key="Key_M" />
                    </JoystickAction>
                </Root>
                """);

        assertFalse(service.isKeyOccupiedByOtherSlot(file, "GalaxyMapOpen", "Key_L", List.of()));
        assertFalse(service.isKeyOccupiedByOtherSlot(file, "GalaxyMapOpen", "Key_M", List.of()));
    }

    @Test
    void noDeviceBlankAndKeyPlaceholderDoNotOccupyKeys() throws Exception {
        byte[] file = binds("""
                <Root>
                    <EmptyAction>
                        <Primary Device="{NoDevice}" Key="Key_L" />
                        <Secondary Device="Keyboard" Key="" />
                    </EmptyAction>
                    <PlaceholderAction>
                        <Primary Device="Keyboard" Key="Key_" />
                        <Secondary Device="Keyboard" Key="{NoDevice}" />
                    </PlaceholderAction>
                </Root>
                """);

        assertFalse(service.isKeyOccupiedByOtherSlot(file, "GalaxyMapOpen", "Key_L", List.of()));
        assertFalse(service.isKeyOccupiedByOtherSlot(file, "GalaxyMapOpen", "Key_", List.of()));
        assertFalse(service.isKeyOccupiedByOtherSlot(file, "GalaxyMapOpen", "{NoDevice}", List.of()));
    }

    @Test
    void sameMainKeyConflictsOnlyWhenTheFullModifierSetMatches() throws Exception {
        byte[] file = binds("""
                <Root>
                    <GalaxyMapOpen>
                        <Primary Device="{NoDevice}" Key="" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </GalaxyMapOpen>
                    <CtrlShiftAction>
                        <Primary Device="Keyboard" Key="Key_J">
                            <Modifier Device="Keyboard" Key="Key_LeftControl" />
                            <Modifier Device="Keyboard" Key="Key_LeftShift" />
                        </Primary>
                    </CtrlShiftAction>
                </Root>
                """);

        List<BindingModifier> ctrlShift = List.of(
                new BindingModifier("Keyboard", "Key_LeftControl"),
                new BindingModifier("Keyboard", "Key_LeftShift")
        );
        List<BindingModifier> ctrlOnly = List.of(new BindingModifier("Keyboard", "Key_LeftControl"));

        // Order within the set must not matter.
        List<BindingModifier> shiftCtrl = List.of(
                new BindingModifier("Keyboard", "Key_LeftShift"),
                new BindingModifier("Keyboard", "Key_LeftControl")
        );

        assertTrue(service.isKeyOccupiedByOtherSlot(file, "GalaxyMapOpen", "Key_J", ctrlShift));
        assertTrue(service.isKeyOccupiedByOtherSlot(file, "GalaxyMapOpen", "Key_J", shiftCtrl));
        assertFalse(service.isKeyOccupiedByOtherSlot(file, "GalaxyMapOpen", "Key_J", ctrlOnly));
    }
}
