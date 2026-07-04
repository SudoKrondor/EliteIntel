package elite.intel.ai.brain;

import elite.intel.ai.brain.actions.command.builtin.IgnoreNonsensicalInputCommand;
import elite.intel.ai.brain.actions.handlers.query.GeneralConversationQueryCommand;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Locks the default (word-overlap) reduction behaviour across the refactor that introduced the semantic
 * strategy. No model needed — runs in the default suite. Targets the package-private method directly so it
 * is independent of the {@code elite.intel.reducer} system property.
 */
class ReducerWordOverlapTest {

    private static Map<String, String> map() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("carrier status", "carrier_status");
        m.put("deploy chaff", "deploy_chaff");
        m.put("cargo hatch", "cargo_hatch");
        return m;
    }

    @Test
    void blankInputReturnsFullMapUnchanged() {
        Map<String, String> full = map();
        assertEquals(full, Reducer.reduce("  ", full, false));
    }

    @Test
    void keepsActionSharingAMeaningfulWord() {
        Map<String, String> result = Reducer.wordOverlapReduce("show carrier status now", map(), false);
        assertTrue(result.containsValue("carrier_status"));
        assertFalse(result.containsValue("deploy_chaff"));
    }

    @Test
    void noOverlapFallsBackToNonsensicalWhenNotConversational() {
        Map<String, String> result = Reducer.wordOverlapReduce("xyzzy plugh", map(), false);
        assertTrue(result.containsKey(IgnoreNonsensicalInputCommand.ID));
        assertFalse(result.containsValue("carrier_status"));
    }

    @Test
    void noOverlapFallsBackToConversationWhenConversational() {
        Map<String, String> result = Reducer.wordOverlapReduce("xyzzy plugh", map(), true);
        assertTrue(result.containsKey(GeneralConversationQueryCommand.ID));
    }

    @Test
    void exactAliasIsPreservedAsCandidate() {
        Map<String, String> result = Reducer.wordOverlapReduce("deploy chaff", map(), false);
        assertTrue(result.containsValue("deploy_chaff"));
    }
}
