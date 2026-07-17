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
    private static final LlmToolDefinition REQUIRED_GAME_TOOL = new LlmToolDefinition(
            "set_speed", "Set speed", "set speed",
            List.of(new ActionParameterSpec(
                    "key", "number", true, "Required speed value", List.of("10"), null)));

    private static Set<String> names(List<LlmToolDefinition> tools) {
        return tools.stream().map(LlmToolDefinition::name).collect(Collectors.toSet());
    }

    private List<LlmToolDefinition> commanderFunctions() {
        return provider.systemFunctions(ThoughtSource.COMMANDER, List.of(REQUIRED_GAME_TOOL));
    }

    @Test
    void commanderToolsCoverEveryFunctionWithDescriptionsAndNoPhrases() {
        List<LlmToolDefinition> tools = commanderFunctions();

        assertEquals(2, tools.size());
        assertEquals(
                Set.of("speak", "request_input"),
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
        List<String> commander = commanderFunctions().stream()
                .map(LlmToolDefinition::name).toList();
        assertEquals(
                List.of("speak", "request_input"),
                commander);

        List<String> event = provider.systemFunctions(ThoughtSource.EVENT, List.of()).stream()
                .map(LlmToolDefinition::name).toList();
        assertEquals(List.of("speak"), event);
    }

    @Test
    void eventToolsAreReadOnlySubset() {
        Set<String> eventNames = names(provider.systemFunctions(ThoughtSource.EVENT, List.of()));
        assertEquals(Set.of("speak"), eventNames);
    }

    @Test
    void requestInputIsOmittedForParameterlessGameTool() {
        LlmToolDefinition openMap = new LlmToolDefinition(
                "open_map", "Open map", "open map", List.of());

        Set<String> commanderNames = names(provider.systemFunctions(
                ThoughtSource.COMMANDER, List.of(openMap)));

        assertEquals(Set.of(SpeakFunction.ID), commanderNames);
        assertFalse(commanderNames.contains(RequestInputFunction.ID));
    }

    @Test
    void speakDeclaresOnlyText() {
        LlmToolDefinition speak = commanderFunctions().stream()
                .filter(t -> t.name().equals("speak")).findFirst().orElseThrow();

        Set<String> params = speak.parameters().stream().map(ActionParameterSpec::getName).collect(Collectors.toSet());
        assertEquals(Set.of("text"), params);
    }

    @Test
    void systemFunctionDescriptionsStayFocusedOnPurpose() {
        LlmToolDefinition speak = commanderFunctions().stream()
                .filter(tool -> tool.name().equals(SpeakFunction.ID))
                .findFirst()
                .orElseThrow();
        LlmToolDefinition requestInput = commanderFunctions().stream()
                .filter(tool -> tool.name().equals(RequestInputFunction.ID))
                .findFirst()
                .orElseThrow();

        assertTrue(speak.description().length() < 100);
        assertTrue(requestInput.description().length() < 150);
        assertFalse(speak.description().contains("call request_input"));
        assertFalse(requestInput.description().contains("unsupported requests"));
        assertFalse(requestInput.description().contains("ambiguity"));
    }

    @Test
    void requestInputDeclaresTypedContinuationFields() {
        LlmToolDefinition requestInput = commanderFunctions().stream()
                .filter(tool -> tool.name().equals(RequestInputFunction.ID))
                .findFirst()
                .orElseThrow();

        Set<String> params = requestInput.parameters().stream()
                .map(ActionParameterSpec::getName)
                .collect(Collectors.toSet());
        assertEquals(Set.of("action_id", "parameter_name", "question"), params);
        assertTrue(requestInput.parameters().stream().allMatch(ActionParameterSpec::isRequired));
    }
}
