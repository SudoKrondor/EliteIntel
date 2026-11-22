package elite.intel.ui.screen.bindings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * When the split preset file's second question - should the game use only this file - is offered (Storage S13f;
 * storage.md, <em>Settled with Storage S13f's sources</em>): only when APPLY could write the answer and nothing else.
 */
class SplitPresetFlowTest {

    @Test
    void offeredWithAWorkingCopyNoEditsAndNotReadOnly() {
        assertTrue(SplitPresetFlow.canSetAll(false, true, false));
    }

    @Test
    void notOfferedWhileReadOnly() {
        assertFalse(SplitPresetFlow.canSetAll(true, true, false));
    }

    @Test
    void notOfferedBeforeFirstTimeStartupHasMadeAWorkingCopy() {
        assertFalse(SplitPresetFlow.canSetAll(false, false, false));
    }

    /** WHY: APPLY would write the edits on the bar too, which the user did not ask for here. */
    @Test
    void notOfferedWhileEditsNotAppliedAreOnTheBar() {
        assertFalse(SplitPresetFlow.canSetAll(false, true, true));
    }
}
