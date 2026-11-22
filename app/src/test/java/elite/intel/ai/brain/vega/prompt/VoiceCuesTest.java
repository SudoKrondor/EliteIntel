package elite.intel.ai.brain.vega.prompt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VoiceCuesTest {

    @Test
    void theLaughingSideOfTheFlipInvitesTheLaugh() {
        assertEquals("Gloat. You may end it with <laugh>.", VoiceCues.coinFlipped("Gloat.", true));
    }

    @Test
    void theDrySideOfTheFlipForbidsTheCueTheSystemPromptWouldOtherwiseAllow() {
        assertEquals("Gloat. Deliver it dry, with no voice cue.", VoiceCues.coinFlipped("Gloat.", false));
    }
}
