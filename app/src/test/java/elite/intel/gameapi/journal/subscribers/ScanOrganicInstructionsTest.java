package elite.intel.gameapi.journal.subscribers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression cover for the invented genus list: a second sample was announced as "remaining genus:
 * Fungivora, Fungivora, Fungivora..." because the one instruction shared by all three scan stages told
 * the model to list the remaining genus, while only the final stage ever sends that list.
 */
class ScanOrganicInstructionsTest {

    @Test
    void firstSampleInstructionsNeverAskForTheRemainingList() {
        assertFalse(ScanOrganicSubscriber.FIRST_SAMPLE_INSTRUCTIONS.contains("'Remaining genus' listed"));
        assertTrue(ScanOrganicSubscriber.FIRST_SAMPLE_INSTRUCTIONS.contains("Say NOTHING about other genus"));
    }

    @Test
    void finalSampleInstructionsReadTheRemainingListBack() {
        assertTrue(ScanOrganicSubscriber.FINAL_SAMPLE_INSTRUCTIONS.contains("'Remaining genus' listed"));
        assertFalse(ScanOrganicSubscriber.FINAL_SAMPLE_INSTRUCTIONS.contains("%s"));
    }
}
