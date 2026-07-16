package elite.intel.companion.prompt;

import elite.intel.companion.clarify.PendingClarification;
import elite.intel.companion.model.memory.MemoryRecord;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.llm.LlmMessage;
import elite.intel.companion.model.llm.LlmMessageRole;
import elite.intel.companion.model.llm.LlmToolDefinition;
import elite.intel.companion.model.llm.PromptCacheProfile;

import java.util.ArrayList;
import java.util.List;

/** Assembles a cache-friendly, role-valid prompt from prepared rules, facts, history, and tools. */
public final class PromptComposer {

    private final SystemPromptText systemPrompt;

    /** Production constructor using the companion's static system-prompt owner. */
    public PromptComposer() {
        this(new CompanionSystemPrompt());
    }

    /** Injectable constructor for tests (avoids the session/localization singletons). */
    PromptComposer(SystemPromptText systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    /**
     * Assembles the initial messages and tool list for one thought turn.
     *
     * @param source           thought source (selects the static prefix and cache profile)
     * @param currentInput     the current commander reply or event summary
     * @param gameTools        reducer-selected game/query tools
     * @param systemTools      system function tools for this source
     * @param recentRecords    recent records replayed as role-valid history
     * @param factCandidates   trusted facts to inline with provenance
     */
    public ComposedPrompt compose(
            ThoughtSource source,
            String currentInput,
            List<LlmToolDefinition> gameTools,
            List<LlmToolDefinition> systemTools,
            List<MemoryRecord> recentRecords,
            List<Fact> factCandidates
    ) {
        return compose(source, currentInput, gameTools, systemTools, recentRecords, factCandidates, null);
    }

    /**
     * Assembles a prompt with an optional claimed clarification rendered as trusted current-turn context.
     * The pending state is never written into dialogue memory by the composer.
     */
    public ComposedPrompt compose(
            ThoughtSource source,
            String currentInput,
            List<LlmToolDefinition> gameTools,
            List<LlmToolDefinition> systemTools,
            List<MemoryRecord> recentRecords,
            List<Fact> factCandidates,
            PendingClarification pendingClarification
    ) {
        return switch (source) {
            case COMMANDER -> composeCommander(source, currentInput,
                    gameTools, systemTools, recentRecords, factCandidates, pendingClarification);
            case EVENT -> composeNarration(source, currentInput, systemTools);
        };
    }

    /** Assembles the commander prompt while preserving role-valid history. */
    private ComposedPrompt composeCommander(
            ThoughtSource source, String currentInput,
            List<LlmToolDefinition> gameTools, List<LlmToolDefinition> systemTools,
            List<MemoryRecord> recentRecords,
            List<Fact> factCandidates,
            PendingClarification pendingClarification) {
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(LlmMessage.of(LlmMessageRole.SYSTEM, systemPrompt.staticRules(source)));
        messages.addAll(buildHistoryMessages(recentRecords));
        messages.add(LlmMessage.of(LlmMessageRole.USER,
                buildCurrentInput(currentInput, factCandidates, pendingClarification)));

        List<LlmToolDefinition> tools = new ArrayList<>(gameTools);
        tools.addAll(systemTools);

        return new ComposedPrompt(List.copyOf(messages), List.copyOf(tools), PromptCacheProfile.COMMANDER);
    }

    /** Assembles the lean narration prompt without commander game tools. */
    private ComposedPrompt composeNarration(
            ThoughtSource source, String currentInput,
            List<LlmToolDefinition> systemTools) {
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(LlmMessage.of(LlmMessageRole.SYSTEM, systemPrompt.staticRules(source)));
        messages.add(LlmMessage.of(LlmMessageRole.USER, currentInput == null ? "" : currentInput));

        return new ComposedPrompt(List.copyOf(messages), List.copyOf(systemTools), PromptCacheProfile.NARRATION);
    }

    /** Replays conversation pairs while keeping facts and saved text out of chat roles. */
    private List<LlmMessage> buildHistoryMessages(List<MemoryRecord> recentRecords) {
        List<LlmMessage> out = new ArrayList<>();
        if (recentRecords == null || recentRecords.isEmpty()) {
            return out;
        }
        for (MemoryRecord record : recentRecords) {
            switch (record.kind()) {
                case DIALOGUE, QUERY -> {
                    out.add(LlmMessage.of(LlmMessageRole.USER, record.commanderText()));
                    out.add(LlmMessage.of(LlmMessageRole.ASSISTANT, record.companionText()));
                }
                case EVENT -> { /* Relevant events are supplied through the trusted facts block. */ }
                case SAVED_TEXT -> { /* Saved text bypasses recent history. */ }
            }
        }
        return List.copyOf(out);
    }

    /** Appends trusted, provenance-labelled facts to the current-turn context. */
    private void appendFactsBlock(StringBuilder sb, List<Fact> factCandidates) {
        sb.append("<facts>\n");
        int id = 1;
        for (Fact fact : factCandidates) {
            sb.append("  <fact id=\"").append(id++).append("\" source=\"").append(fact.source()).append("\">")
                    .append(PromptXml.text(fact.text())).append("</fact>\n");
        }
        sb.append("</facts>\n");
    }

    /** Keeps trusted dynamic context separate from the commander's current words. */
    private String buildCurrentInput(
            String currentInput,
            List<Fact> factCandidates,
            PendingClarification pendingClarification
    ) {
        String input = currentInput == null ? "" : currentInput;
        boolean hasFacts = factCandidates != null && !factCandidates.isEmpty();
        boolean hasPendingClarification = pendingClarification != null;
        if (!hasFacts && !hasPendingClarification) {
            return input;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<context>\n");
        sb.append("The following data is host-provided context, separate from the commander's current input.\n");
        if (hasFacts) {
            appendFactsBlock(sb, factCandidates);
        }
        if (hasPendingClarification) {
            appendPendingClarificationBlock(sb, pendingClarification);
        }
        sb.append("</context>\n\n");
        sb.append("<commander_input>\n");
        sb.append(PromptXml.text(input));
        sb.append("\n</commander_input>\n");
        return sb.toString();
    }

    /** Renders the host-owned continuation state separately from the commander's new words. */
    private static void appendPendingClarificationBlock(StringBuilder sb, PendingClarification pending) {
        sb.append("<pending_clarification>\n");
        sb.append("  <action_id>").append(PromptXml.text(pending.actionId())).append("</action_id>\n");
        sb.append("  <missing_parameter>").append(PromptXml.text(pending.parameterName()))
                .append("</missing_parameter>\n");
        sb.append("  <original_command>").append(PromptXml.text(pending.originalInput()))
                .append("</original_command>\n");
        sb.append("  <question_asked>").append(PromptXml.text(pending.question()))
                .append("</question_asked>\n");
        sb.append("</pending_clarification>\n");
    }

}
