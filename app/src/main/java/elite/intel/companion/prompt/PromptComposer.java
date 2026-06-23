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
     * @param currentTopic     the conversation's current topic (the global TopicModel)
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
            ConversationTopic currentTopic,
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
        messages.add(LlmMessage.of(LlmMessageRole.USER, buildCurrentInput(source, urgency, currentTopic, currentInput)));

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
        appendTopics(sb);
        appendMemory(sb, indexes, longTermSummary);
        return sb.toString();
    }

    /** Full selectable topic enum; the model needs the valid values for change_global_topic / recall(topic=...). */
    private void appendTopics(StringBuilder sb) {
        PromptSections.heading(sb, "Topics");
        sb.append("Valid values for change_global_topic and recall(topic=...):\n");
        for (ConversationTopic topic : ConversationTopic.values()) {
            if (topic.selectable()) {
                sb.append("- ").append(id(topic)).append(": ").append(topic.description()).append('\n');
            }
        }
    }

    /** Cheap memory indexes (llm_memory, topic memory) plus the long-term summary, grouped under one header. */
    private void appendMemory(StringBuilder sb, MemoryAvailabilitySnapshot indexes, String longTermSummary) {
        PromptSections.heading(sb, "Memory");

        PromptSections.subheading(sb, "llm_memory");
        sb.append(indexes.llmMemoryUsed()).append(" / ").append(indexes.llmMemoryCapacity())
                .append(" remembered items available. Use recall(scope=llm_memory) to load all.\n");

        // Lists only topics that actually hold mid-term memory, so recall hints are honest.
        PromptSections.subheading(sb, "Topic memory");
        List<ConversationTopic> topics = indexes.topicsWithMemory();
        if (topics.isEmpty()) {
            sb.append("- none\n");
        } else {
            for (ConversationTopic topic : topics) {
                sb.append("- ").append(id(topic)).append(": ").append(topic.description()).append('\n');
            }
        }

        PromptSections.subheading(sb, "Long-term summary");
        sb.append(longTermSummary == null || longTermSummary.isBlank() ? "none yet." : longTermSummary.strip())
                .append('\n');
    }

    /** Per-turn short-term timeline as a context block; dynamic, kept out of the cached prefix. */
    private String buildContextBlock(List<MemoryEntry> shortTerm) {
        StringBuilder sb = new StringBuilder();
        PromptSections.heading(sb, "Session memory timeline");
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

    private String buildCurrentInput(ThoughtSource source, Urgency urgency, ConversationTopic currentTopic,
                                     String currentInput) {
        StringBuilder sb = new StringBuilder();
        PromptSections.heading(sb, "Current input");
        sb.append("source: ").append(source.name()).append('\n')
                .append("urgency: ").append(urgency.name().toLowerCase(Locale.ROOT)).append('\n')
                .append("current topic: ").append(id(currentTopic)).append('\n')
                .append("content: ").append(currentInput == null ? "" : currentInput).append('\n');
        return sb.toString();
    }

    private static String id(ConversationTopic topic) {
        return topic.name().toLowerCase(Locale.ROOT);
    }
}
