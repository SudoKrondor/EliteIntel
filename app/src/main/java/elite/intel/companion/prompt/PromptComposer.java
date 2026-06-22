package elite.intel.companion.prompt;

import elite.intel.companion.memory.MemoryAvailabilitySnapshot;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.llm.LlmToolDefinition;
import elite.intel.companion.model.Urgency;

import java.util.List;

/**
 * Dumb stacker of {@code messages + tools}. It does not decide which tools are allowed, which
 * commands are relevant, or how to describe a tool; it receives already-prepared data and assembles
 * the OpenAI/Mistral-compatible prompt (see COMPANION_ARCHITECTURE.md §2.10).
 */
public final class PromptComposer {

    /**
     * Assembles the initial messages and tool list for one thought turn.
     *
     * @param source           thought source (rendered into the current-input block)
     * @param urgency          thought urgency (rendered into the current-input block)
     * @param globalTopic      current global TopicModel
     * @param thoughtTopic     this thought's topic (may be PENDING)
     * @param currentInput     the current commander reply or event summary
     * @param selectedTools    Reducer-selected game/query tools
     * @param systemTools      system function tools for this source
     * @param shortTerm        short-term memory timeline for the context block
     * @param indexes          cheap memory index metadata (llm_memory / topic memory availability)
     * @param longTermSummary  the session-wide long-term summary
     */
    public ComposedPrompt compose(
            ThoughtSource source,
            Urgency urgency,
            ConversationTopic globalTopic,
            ConversationTopic thoughtTopic,
            String currentInput,
            List<LlmToolDefinition> selectedTools,
            List<LlmToolDefinition> systemTools,
            List<MemoryEntry> shortTerm,
            MemoryAvailabilitySnapshot indexes,
            String longTermSummary
    ) {
        // TODO: Phase 2 - build the stable system prefix, memory context block, and current-input turn.
        throw new UnsupportedOperationException("TODO: Phase 2");
    }
}
