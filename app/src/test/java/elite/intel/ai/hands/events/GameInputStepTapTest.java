package elite.intel.ai.hands.events;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards the split between the two ways to press a binding.
 * <p>
 * {@link GameInputStep#bindingTap} presses a binding as the commander's .binds file configures it, so a
 * long-press action holds. {@link GameInputStep#bindingForcedTap} always taps, for callers whose own
 * contract is a tap - the custom-command editor's Binding Tap step, chosen over the neighbouring Binding
 * Hold step. Collapsing the two is what previously made every long-press binding fire as a tap.
 */
class GameInputStepTapTest {

    @Test
    void aPlainTapStepDefersToTheBindingsFile() {
        assertEquals(GameInputStep.Type.BINDING_TAP, GameInputStep.bindingTap("NightVisionToggle").getType());
    }

    @Test
    void aForcedTapStepIsADistinctTypeSoTheExecutorCanIgnoreTheHoldFlag() {
        GameInputStep forced = GameInputStep.bindingForcedTap("NightVisionToggle");

        assertEquals(GameInputStep.Type.BINDING_FORCED_TAP, forced.getType());
        assertNotEquals(GameInputStep.bindingTap("NightVisionToggle").getType(), forced.getType());
    }

    @Test
    void bothCarryTheBindingAndCountAsInput() {
        for (GameInputStep step : List.of(
                GameInputStep.bindingTap("UI_Back"),
                GameInputStep.bindingForcedTap("UI_Back"))) {
            assertEquals("UI_Back", step.getBindingId());
            assertTrue(step.isInputProducing(), step.getType() + " drives the game and must be paced like input");
        }
    }

    @Test
    void aForcedTapStillRequiresABinding() {
        assertThrows(IllegalArgumentException.class, () -> GameInputStep.bindingForcedTap(" "));
    }
}
