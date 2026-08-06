package elite.intel.ai.brain.commons;

import elite.intel.ai.brain.CompanionIdentity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the identity of the analysis prompt - the one every {@code Analyze*Query} handler speaks through.
 */
class PromptFactoryTest {

    private final PromptFactory factory = PromptFactory.getInstance();

    /**
     * The analysis path is spoken, so it carries the same identity as the companion prompts.
     *
     * <p>WHY: it used to open "You are {shipName}, a ship in Elite Dangerous - space sim game", which made the
     * model answer as the hull while the companion prompt had it answering as an AI named Vega. Same session,
     * two speakers, depending only on whether the answer came from a query handler or from dialogue.
     */
    @Test
    void analysisPromptSpeaksAsTheCompanionAiNotAsTheShip() {
        String prompt = factory.generateAnalysisPrompt();

        assertTrue(prompt.contains(CompanionIdentity.identityClause()),
                "the spoken analysis prompt must open with the shared identity clause");
        assertFalse(prompt.contains("a ship in Elite Dangerous"),
                "the model must never be told it is the ship");
    }

    /**
     * Identity travels with personality: a style clause on its own ("respond as a close friend", "full chaos
     * mode") leaves the model to infer who is speaking, and inferring a person is exactly the failure mode.
     */
    @Test
    void personalityBlockRestatesWhoIsSpeaking() {
        String prompt = factory.generateAnalysisPrompt();
        int personality = prompt.indexOf("Personality: ");

        assertTrue(personality >= 0, "the analysis prompt must carry a personality block");
        assertTrue(prompt.indexOf(CompanionIdentity.identityClause(), personality) > personality,
                "the personality block must be preceded by the identity clause, not stand alone");
    }
}
