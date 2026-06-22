package elite.intel.companion.prompt;

import elite.intel.companion.memory.MemoryAvailabilitySnapshot;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.llm.LlmMessage;
import elite.intel.companion.model.llm.LlmMessageRole;
import elite.intel.companion.model.llm.LlmToolDefinition;
import elite.intel.companion.model.llm.PromptCacheProfile;
import elite.intel.companion.model.Urgency;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Dumb stacker of {@code messages + tools}. It does not decide which tools are allowed, which
 * commands are relevant, or how to describe a tool; it receives already-prepared data and assembles
 * the OpenAI/Mistral-compatible prompt (see COMPANION_ARCHITECTURE.md §2.10).
 * <p>
 * Cache-friendly ordering: the stable narrative + topic enum head the system message, the
 * slowly-changing memory indexes/summary follow, and the per-turn timeline and current input are
 * separate later messages so the cached prefix survives across turns.
 */
public final class PromptComposer {

    private final SystemPromptText systemPrompt;

    /** Production constructor: uses the real {@link CompanionSystemPromptPart} owner. */
    public PromptComposer() {
        this(new CompanionSystemPromptPart());
    }

    /** Injectable constructor for tests (avoids the session/localization singletons). */
    PromptComposer(SystemPromptText systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

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
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(LlmMessage.of(LlmMessageRole.SYSTEM, buildStablePrefix(source, indexes, longTermSummary)));
        messages.add(LlmMessage.of(LlmMessageRole.SYSTEM, buildContextBlock(shortTerm)));
        messages.add(LlmMessage.of(LlmMessageRole.USER, buildCurrentInput(source, urgency, globalTopic, thoughtTopic, currentInput)));

        // Game/query tools first, then system functions; both already chosen upstream.
        List<LlmToolDefinition> tools = new ArrayList<>(selectedTools);
        tools.addAll(systemTools);

        PromptCacheProfile profile = source == ThoughtSource.COMMANDER
                ? PromptCacheProfile.COMMANDER
                : PromptCacheProfile.EVENT;

        return new ComposedPrompt(List.copyOf(messages), List.copyOf(tools), profile);
    }

    /** Stable narrative + topic enum (truly stable) followed by the slowly-changing memory indexes. */
    private String buildStablePrefix(ThoughtSource source, MemoryAvailabilitySnapshot indexes, String longTermSummary) {
        StringBuilder sb = new StringBuilder();
        sb.append(systemPrompt.staticRules(source));
        appendTopicEnum(sb);
        appendLlmMemoryIndex(sb, indexes);
        appendTopicMemoryIndex(sb, indexes);
        appendLongTermSummary(sb, longTermSummary);
        return sb.toString();
    }

    /** Full selectable topic enum; the model needs the valid values for set_topic / recall(topic=...). */
    private void appendTopicEnum(StringBuilder sb) {
        sb.append("\nTOPICS (valid values for set_topic and recall(topic=...)):\n");
        for (ConversationTopic topic : ConversationTopic.values()) {
            if (topic.selectable()) {
                sb.append("- ").append(id(topic)).append(": ").append(topic.description()).append('\n');
            }
        }
    }

    private void appendLlmMemoryIndex(StringBuilder sb, MemoryAvailabilitySnapshot indexes) {
        sb.append("\nllm_memory: ")
                .append(indexes.llmMemoryUsed()).append(" / ").append(indexes.llmMemoryCapacity())
                .append(" remembered items available. Use recall(scope=llm_memory) to load all.\n");
    }

    /** Lists only topics that actually hold mid-term memory, so recall hints are honest. */
    private void appendTopicMemoryIndex(StringBuilder sb, MemoryAvailabilitySnapshot indexes) {
        List<ConversationTopic> topics = indexes.topicsWithMemory();
        sb.append("\ntopic memory available:\n");
        if (topics.isEmpty()) {
            sb.append("- none\n");
            return;
        }
        for (ConversationTopic topic : topics) {
            sb.append("- ").append(id(topic)).append(": ").append(topic.description()).append('\n');
        }
    }

    private void appendLongTermSummary(StringBuilder sb, String longTermSummary) {
        sb.append("\nlong-term summary:\n");
        sb.append(longTermSummary == null || longTermSummary.isBlank() ? "none yet." : longTermSummary.strip());
        sb.append('\n');
    }

    /** Per-turn short-term timeline as a context block; dynamic, kept out of the cached prefix. */
    private String buildContextBlock(List<MemoryEntry> shortTerm) {
        StringBuilder sb = new StringBuilder("Session memory timeline:\n");
        if (shortTerm == null || shortTerm.isEmpty()) {
            sb.append("(empty)\n");
            return sb.toString();
        }
        for (MemoryEntry entry : shortTerm) {
            sb.append('[').append(entry.source().name()).append(']')
                    .append('[').append(id(entry.topic())).append(']')
                    .append('[').append(entry.processingState().name().toLowerCase(Locale.ROOT)).append("] ")
                    .append(entry.content()).append('\n');
        }
        return sb.toString();
    }

    private String buildCurrentInput(ThoughtSource source, Urgency urgency, ConversationTopic globalTopic,
                                     ConversationTopic thoughtTopic, String currentInput) {
        return "Current input:\n"
                + "source: " + source.name() + '\n'
                + "urgency: " + urgency.name().toLowerCase(Locale.ROOT) + '\n'
                + "global topic: " + id(globalTopic) + '\n'
                + "current topic: " + id(thoughtTopic) + '\n'
                + "content: " + (currentInput == null ? "" : currentInput) + '\n';
    }

    private static String id(ConversationTopic topic) {
        return topic.name().toLowerCase(Locale.ROOT);
    }
}
