package elite.intel.ai.hands.events;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards the step that waits for the game instead of guessing how long it needs.
 * <p>
 * A {@link GameInputStep#waitUntil} step blocks on a condition the game itself reports, which is what
 * replaced the blind three-second delay that assumed the galaxy map was ready. It spends its own time,
 * so unlike a keystroke it must not also collect the executor's post-input pacing delay.
 */
class GameInputStepWaitUntilTest {

    @Test
    void aWaitStepCarriesItsConditionTimeoutAndMeaning() {
        GameInputStep step = GameInputStep.waitUntil("galaxy map open", () -> true, 15000);

        assertEquals(GameInputStep.Type.WAIT_UNTIL, step.getType());
        assertEquals(15000, step.getDurationMs());
        assertEquals("galaxy map open", step.getConditionDescription());
        assertTrue(step.getCondition().getAsBoolean());
    }

    @Test
    void aWaitStepIsNotPacedLikeInputBecauseItHasAlreadySpentItsTime() {
        assertFalse(GameInputStep.waitUntil("galaxy map open", () -> true, 1).isInputProducing());
        assertFalse(GameInputStep.delay(1).isInputProducing(), "a plain delay is the sibling case");
        assertTrue(GameInputStep.bindingTap("UI_Back").isInputProducing());
    }

    @Test
    void aWaitStepWithoutAConditionOrMeaningIsRejected() {
        assertThrows(NullPointerException.class, () -> GameInputStep.waitUntil("galaxy map open", null, 1));
        assertThrows(IllegalArgumentException.class, () -> GameInputStep.waitUntil(" ", () -> true, 1));
        assertThrows(IllegalArgumentException.class, () -> GameInputStep.waitUntil("galaxy map open", () -> true, -1));
    }

    @Test
    void onlyWaitStepsCarryACondition() {
        assertNull(GameInputStep.delay(1).getCondition());
        assertNull(GameInputStep.bindingTap("UI_Back").getConditionDescription());
    }
}
