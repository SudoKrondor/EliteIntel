package elite.intel.companion.tools;

import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.llm.LlmToolDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the provider turns registered system functions into provider-neutral tool definitions:
 * correct id set per source, a resolved (non-blank) English description for every function, no training
 * phrases, and valid parameter specs. Building the full COMMANDER set also proves every function
 * declares a non-blank English {@code llmDescription()}.
 */
class SystemFunctionProviderTest {

    private final SystemFunctionProvider provider = new SystemFunctionProvider();

    private static Set<String> names(List<LlmToolDefinition> tools) {
        return tools.stream().map(LlmToolDefinition::name).collect(Collectors.toSet());
    }

    @Test
    void commanderToolsCoverEveryFunctionWithDescriptionsAndNoPhrases() {
        List<LlmToolDefinition> tools = provider.systemFunctions(ThoughtSource.COMMANDER);

        assertEquals(3, tools.size());
        assertEquals(
                Set.of("speak", "classify_turn", "search_in_memory"),
                names(tools));
        for (LlmToolDefinition tool : tools) {
            assertFalse(tool.description() == null || tool.description().isBlank(), tool.name() + " description");
            assertTrue(tool.localizedTrainingPhrases().isEmpty(), tool.name() + " must have no training phrases");
            for (ActionParameterSpec spec : tool.parameters()) {
                spec.validate(); // throws if a parameter schema is malformed
            }
        }
    }

    @Test
    void toolsAreInDeterministicLeadThenAlphabeticalOrder() {
        List<String> commander = provider.systemFunctions(ThoughtSource.COMMANDER).stream()
                .map(LlmToolDefinition::name).toList();
        assertEquals(
                List.of("speak", "classify_turn", "search_in_memory"),
                commander);

        List<String> event = provider.systemFunctions(ThoughtSource.EVENT).stream()
                .map(LlmToolDefinition::name).toList();
        assertEquals(List.of("speak"), event);
    }

    @Test
    void eventToolsAreReadOnlySubset() {
        Set<String> eventNames = names(provider.systemFunctions(ThoughtSource.EVENT));
        assertEquals(Set.of("speak"), eventNames);
    }

    @Test
    void speakDeclaresOnlyText() {
        LlmToolDefinition speak = provider.systemFunctions(ThoughtSource.COMMANDER).stream()
                .filter(t -> t.name().equals("speak")).findFirst().orElseThrow();

        Set<String> params = speak.parameters().stream().map(ActionParameterSpec::getName).collect(Collectors.toSet());
        assertEquals(Set.of("text"), params);
    }
}
