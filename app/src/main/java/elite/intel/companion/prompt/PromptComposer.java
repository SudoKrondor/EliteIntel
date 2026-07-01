package elite.intel.companion.prompt;

import elite.intel.companion.CompanionConfig;
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
 * slowly-changing memory index follows, and the per-turn Visible context and current input are
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
     * @param indexes          cheap memory index metadata (topics with mid-term memory)
     */
    public ComposedPrompt compose(
            ThoughtSource source,
            Urgency urgency,
            ConversationTopic currentTopic,
            String currentInput,
            List<LlmToolDefinition> selectedTools,
            List<LlmToolDefinition> systemTools,
            List<MemoryEntry> shortTerm,
            MemoryAvailabilitySnapshot indexes
    ) {
        return switch (source) {
            case COMMANDER -> composeCommander(source, urgency, currentTopic, currentInput,
                    selectedTools, systemTools, shortTerm, indexes);
            case NARRATION -> composeNarration(source, urgency, currentTopic, currentInput, systemTools, shortTerm);
            // EVENT thoughts are memory-only (see EventThought); they never reach here.
            case EVENT -> throw new IllegalStateException("EVENT thoughts do not compose a prompt");
        };
    }

    /**
     * Full consciousness prompt: stable prefix (rules + topic enum + memory indexes), the Visible context
     * block, the current-input block, the reduced game tools plus system functions, and the COMMANDER cache
     * profile.
     */
    private ComposedPrompt composeCommander(
            ThoughtSource source, Urgency urgency, ConversationTopic currentTopic, String currentInput,
            List<LlmToolDefinition> selectedTools, List<LlmToolDefinition> systemTools,
            List<MemoryEntry> shortTerm,
            MemoryAvailabilitySnapshot indexes) {
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(LlmMessage.of(LlmMessageRole.SYSTEM, buildStablePrefix(source, indexes)));
        messages.add(LlmMessage.of(LlmMessageRole.SYSTEM, buildContextBlock(shortTerm)));
        messages.add(LlmMessage.of(LlmMessageRole.USER, buildCurrentInput(source, urgency, currentTopic, currentInput)));

        // Game/query tools first, then system functions; both already chosen upstream.
        List<LlmToolDefinition> tools = new ArrayList<>(selectedTools);
        tools.addAll(systemTools);

        return new ComposedPrompt(List.copyOf(messages), List.copyOf(tools), PromptCacheProfile.COMMANDER);
    }

    /**
     * Lean narration prompt: the narration static block only (no topic enum, no memory indexes, no safety -
     * a narration thought has only speak), the Visible context for continuity, the sensor data as the
     * current input, the system tools, and its own NARRATION cache profile so it never shares the commander
     * prefix. {@code selectedTools}/{@code indexes} do not apply here.
     */
    private ComposedPrompt composeNarration(
            ThoughtSource source, Urgency urgency, ConversationTopic currentTopic, String currentInput,
            List<LlmToolDefinition> systemTools, List<MemoryEntry> shortTerm) {
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(LlmMessage.of(LlmMessageRole.SYSTEM, systemPrompt.staticRules(source)));
        messages.add(LlmMessage.of(LlmMessageRole.SYSTEM, buildContextBlock(shortTerm)));
        messages.add(LlmMessage.of(LlmMessageRole.USER, buildCurrentInput(source, urgency, currentTopic, currentInput)));

        return new ComposedPrompt(List.copyOf(messages), List.copyOf(systemTools), PromptCacheProfile.NARRATION);
    }

    /** Stable narrative + topic enum (truly stable) followed by the slowly-changing memory index. */
    private String buildStablePrefix(ThoughtSource source, MemoryAvailabilitySnapshot indexes) {
        StringBuilder sb = new StringBuilder();
        sb.append(systemPrompt.staticRules(source));
        appendTopics(sb);
        appendMemory(sb, indexes);
        return sb.toString();
    }

    /** Full selectable topic enum; the model needs the valid values for the classify_turn topic parameter. */
    private void appendTopics(StringBuilder sb) {
        PromptSections.heading(sb, "Topics");
        sb.append("Valid values for the classify_turn topic parameter:\n");
        for (ConversationTopic topic : ConversationTopic.values()) {
            if (topic.selectable()) {
                sb.append("- ").append(topic.id()).append(": ").append(topic.description()).append('\n');
            }
        }
        // The topic is sticky and never moves on its own; tell the model to carry the current topic forward so
        // an earlier subject does not keep tagging unrelated turns (the cause of topic "stickiness").
        sb.append("The current topic is shown with each input and stays until you move it. Keep it when the new "
                + "input still fits; move it to the matching topic above only on a real subject change.\n");
    }

    /** Cheap memory index (topics with mid-term memory), so the model knows memory is worth searching. */
    private void appendMemory(StringBuilder sb, MemoryAvailabilitySnapshot indexes) {
        PromptSections.heading(sb, "Memory data");
        sb.append("You carry memory from earlier this session.\n");

        // Lists only topics that actually hold mid-term memory, so the model knows memory is worth searching.
        // Non-selectable sentinels (unresolved_commander_input/unresolved_game_event) can hold memory too, but
        // they are not valid classify_turn topics, so they are filtered out here to avoid tempting the model
        // into emitting one as a topic (a schema error); their content is still reachable via search_in_memory.
        PromptSections.subheading(sb, "Topics with stored memory");
        List<ConversationTopic> topics = indexes.topicsWithMemory().stream()
                .filter(ConversationTopic::selectable)
                .toList();
        if (topics.isEmpty()) {
            sb.append("- none\n");
        } else {
            for (ConversationTopic topic : topics) {
                sb.append("- ").append(topic.id()).append('\n');
            }
        }
    }

    /**
     * Per-turn dynamic context block (kept out of the cached prefix): the short-term timeline, inlined whole as
     * the "Visible context". Durable facts that aged out of short-term are not inlined - they are reached through
     * {@code search_in_memory}.
     */
    private String buildContextBlock(List<MemoryEntry> shortTerm) {
        StringBuilder sb = new StringBuilder();
        PromptSections.heading(sb, "Visible context");
        if (shortTerm == null || shortTerm.isEmpty()) {
            sb.append("(empty)\n");
            return sb.toString();
        }
        for (MemoryEntry entry : shortTerm) {
            appendEntry(sb, entry);
        }
        return sb.toString();
    }

    /** Renders one entry as a Visible context line: {@code [speaker][topic] content}. */
    private void appendEntry(StringBuilder sb, MemoryEntry entry) {
        sb.append('[').append(entry.source().displayLabel(CompanionConfig.companionName())).append(']')
                .append('[').append(entry.topic().id()).append("] ")
                .append(entry.content()).append('\n');
    }

    private String buildCurrentInput(ThoughtSource source, Urgency urgency, ConversationTopic currentTopic,
                                     String currentInput) {
        StringBuilder sb = new StringBuilder();
        PromptSections.heading(sb, "Current input");
        sb.append("source: ").append(source.name()).append('\n')
                .append("urgency: ").append(urgency.name().toLowerCase(Locale.ROOT)).append('\n')
                .append("current topic: ").append(currentTopic.id()).append('\n')
                .append("content: ").append(currentInput == null ? "" : currentInput).append('\n');
        return sb.toString();
    }
}
