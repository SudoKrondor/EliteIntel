package elite.intel.companion.tools;

import elite.intel.companion.memory.CompanionMemoryLimits;

import java.util.Map;

/**
 * Single owner of the model-facing descriptions for companion system functions, resolved by a
 * {@link SystemFunction#descriptionKey()}. Descriptions are authored in English by the companion
 * language policy (one cache-stable prompt prefix across all commander languages); a per-language
 * layer can be added here later without changing the functions or the provider.
 */
public final class CompanionFunctionTextProvider {

    private static final Map<String, String> DESCRIPTIONS = Map.ofEntries(
            Map.entry(SpeakFunction.ID,
                    "Speak a phrase aloud to the commander through the ship's voice. Use this whenever you want the commander to hear something."),
            Map.entry(NothingToDoFunction.ID,
                    "End this turn because there is nothing more to say and nothing to do. This is the explicit way to finish a turn cleanly; returning no function call at all is an error, not this. To merely stay silent while still acting, simply do not call speak."),
            Map.entry(ChangeGlobalTopicFunction.ID,
                    "Switch the global conversation topic to a different one, chosen from the listed valid topics. Call this only when this turn actually moves the conversation to a topic different from the current topic shown in the current input; otherwise leave the topic unchanged."),
            Map.entry(ClarifyFunction.ID,
                    "Ask the commander a short clarifying question and wait for their reply before acting. Use only when their intent is genuinely ambiguous; unlike speak, this expects an answer to continue the exchange."),
            Map.entry(RememberFunction.ID,
                    "Save a short fact (maximum " + CompanionMemoryLimits.LLM_MEMORY_MAX_CONTENT_LENGTH + " characters) to long-lived memory for later recall."),
            Map.entry(RecallFunction.ID,
                    "Load stored memory. scope=llm_memory returns all remembered facts; scope=topic_memory returns entries for one topic (topic required, query optional)."),
            Map.entry(FindActionFunction.ID,
                    "Search the full action catalog for a ship or game action matching a description. Use this when the action the commander wants is not among the functions offered this turn (only a relevant subset is offered each turn)."),
            Map.entry(ChangeVerbosityFunction.ID,
                    "Change how talkative you are by switching the verbosity mode.")
    );

    /**
     * Resolves the English description for a system function description key.
     *
     * @param key a {@link SystemFunction#descriptionKey()}
     * @return the description text
     * @throws IllegalArgumentException if the key has no registered description (a wiring bug)
     */
    public String describe(String key) {
        String text = DESCRIPTIONS.get(key);
        if (text == null) {
            throw new IllegalArgumentException("No description registered for system function key: " + key);
        }
        return text;
    }
}
