package elite.intel.companion.model.llm;

import elite.intel.ai.brain.actions.ActionParameterSpec;

import java.util.List;
import java.util.Objects;

/**
 * Provider-neutral definition of one callable function offered to the LLM in the native tool-calling
 * {@code tools} array. Built within companion (never by the program's command classes): for game tools
 * by an adapter that reads an {@code IntelAction}'s existing {@code id()}/{@code parameters()}, and for
 * {@code SystemFunction}s directly. The {@code LlmGateway} bridge renders this into provider-native JSON.
 *
 * @param name                      unique tool name the LLM must call
 * @param description               short natural-language description for the model
 * @param localizedTrainingPhrases  example utterances in the current language that map to this tool,
 *                                  as a single string (as authored in the action alias localization);
 *                                  empty for system functions, which the commander never triggers by voice
 * @param parameters                parameter schema, reusing the project's {@link ActionParameterSpec}
 */
public record LlmToolDefinition(
        String name,
        String description,
        String localizedTrainingPhrases,
        List<ActionParameterSpec> parameters
) {
    /** Freezes the parameter list so rendering and response validation observe the same per-request schema. */
    public LlmToolDefinition {
        parameters = List.copyOf(Objects.requireNonNull(parameters, "parameters"));
    }
}
