package elite.intel.companion.prompt;

import elite.intel.ai.brain.ShipPersonality;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.session.SystemSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves that switching the commander-chosen AI personality ({@link SystemSession#setAIPersonality}) actually
 * reaches the rendered COMMANDER prompt: {@code {personalityClause}} is filled from the CURRENT personality on
 * every render (no stale in-memory field, no cached static prefix), so a switch shows up on the next turn. If
 * the companion "feels" unchanged after a switch, this test passing pins the cause to the model or the persona,
 * not to the switch failing to reach the prompt.
 */
class PersonalitySwitchPromptTest {

    private final CompanionSystemPromptPart prompt = new CompanionSystemPromptPart();
    private ShipPersonality original;

    private String render() {
        return prompt.staticRules(ThoughtSource.COMMANDER);
    }

    @BeforeEach
    void remember() {
        original = SystemSession.getInstance().getAIPersonality();
    }

    @AfterEach
    void restore() {
        SystemSession.getInstance().setAIPersonality(original);
    }

    @Test
    void switchingPersonalityChangesTheRenderedClause() {
        SystemSession.getInstance().setAIPersonality(ShipPersonality.PROFESSIONAL);
        String professional = render();
        assertTrue(professional.contains(ShipPersonality.PROFESSIONAL.getPersonalityClause()),
                "PROFESSIONAL clause must be in the prompt after switching to it");

        SystemSession.getInstance().setAIPersonality(ShipPersonality.ROGUE);
        String rogue = render();
        assertTrue(rogue.contains(ShipPersonality.ROGUE.getPersonalityClause()),
                "ROGUE clause must be in the prompt after switching to it");

        // The switch actually changed the prompt (not just the stored DB value)...
        assertNotEquals(professional, rogue, "switching personality must change the rendered prompt");
        // ...and the previous personality's clause is gone (no stale caching of the static prefix).
        assertTrue(!rogue.contains("military professional"),
                "the ROGUE prompt must not still carry the PROFESSIONAL clause");
    }
}
