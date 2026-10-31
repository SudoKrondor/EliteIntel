package elite.intel.ui.screen.bindings;

import elite.intel.ui.screen.bindings.LeavePrompt.Choice;
import elite.intel.ui.screen.bindings.LeavePrompt.Leaving;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Leaving BindForge with edits not applied (storage.md rule 9; Storage S7): Go back stays, Discard throws them away
 * and leaves, Apply leaves once they are gone; closing while a write runs is refused (Alan, 2026-10-30).
 */
class LeavePromptTest {

    private final List<String> happened = new ArrayList<>();
    private boolean hasEdits = true;
    private boolean writing;
    private boolean writeStartsOnDiscard;
    private Choice answer = Choice.GO_BACK;
    /** Whether the bar's APPLY writes the edits; false is a refusal, a failure or a cancelled confirmation. */
    private boolean applyWrites = true;

    private final LeavePrompt prompt = new LeavePrompt(new LeavePrompt.EditsState() {
        @Override
        public boolean noEdits() {
            return !hasEdits;
        }

        @Override
        public boolean writing() {
            return writing;
        }

        @Override
        public boolean discard() {
            if (writeStartsOnDiscard) return false;
            happened.add("discarded");
            hasEdits = false;
            return true;
        }
    }, () -> {
        happened.add("asked");
        return answer;
    }, finished -> {
        happened.add("applied");
        if (applyWrites) hasEdits = false;
        finished.run();
    }, () -> happened.add("told write running"));

    private void leave(Leaving leaving) {
        prompt.request(leaving, () -> happened.add("left"));
    }

    @Test
    void withNoEditsItLeavesWithoutAsking() {
        hasEdits = false;
        leave(Leaving.TAB);
        leave(Leaving.CLOSE);
        assertEquals(List.of("left", "left"), happened);
    }

    @Test
    void goBackStaysWithTheEdits() {
        leave(Leaving.CLOSE);
        assertEquals(List.of("asked"), happened);
        assertTrue(hasEdits);
    }

    @Test
    void discardThrowsTheEditsAwayAndLeavesWithoutAskingAgain() {
        answer = Choice.DISCARD;
        leave(Leaving.TAB);
        assertEquals(List.of("asked", "discarded", "left"), happened);
    }

    @Test
    void applyLeavesOnceTheEditsAreWritten() {
        answer = Choice.APPLY;
        leave(Leaving.CLOSE);
        assertEquals(List.of("asked", "applied", "left"), happened);
    }

    @Test
    void anApplyThatWritesNothingStays() {
        answer = Choice.APPLY;
        applyWrites = false;
        leave(Leaving.CLOSE);
        assertEquals(List.of("asked", "applied"), happened);
        assertTrue(hasEdits);
    }

    @Test
    void closingWhileAWriteRunsIsRefused() {
        writing = true;
        leave(Leaving.CLOSE);
        assertEquals(List.of("told write running"), happened);
    }

    @Test
    void switchingTabsWhileAWriteRunsIsLetThrough() {
        writing = true;
        leave(Leaving.TAB);
        assertEquals(List.of("left"), happened);
    }

    @Test
    void aDiscardAWriteGotAheadOfStays() {
        answer = Choice.DISCARD;
        writeStartsOnDiscard = true;
        leave(Leaving.TAB);
        assertEquals(List.of("asked", "told write running"), happened);
        assertTrue(hasEdits);
    }
}
