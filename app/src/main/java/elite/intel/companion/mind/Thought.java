package elite.intel.companion.mind;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.commons.AiResponseLanguagePolicy;
import elite.intel.ai.brain.i18n.LlmTextProvider;
import elite.intel.companion.confirm.DangerousActionConfirmedEvent;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.IntelActionCategory;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.Urgency;
import elite.intel.companion.model.execution.ExecutionRequest;
import elite.intel.companion.model.llm.LlmMessage;
import elite.intel.companion.model.llm.LlmRequest;
import elite.intel.companion.model.llm.LlmResult;
import elite.intel.companion.model.llm.LlmToolDefinition;
import elite.intel.companion.model.llm.LlmToolInvocation;
import elite.intel.companion.model.llm.PromptCacheProfile;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemoryProcessingState;
import elite.intel.companion.model.memory.MemorySource;
import elite.intel.companion.model.speech.SpeechRequest;
import elite.intel.companion.prompt.ComposedPrompt;
import elite.intel.companion.tools.ChangeGlobalTopicFunction;
import elite.intel.companion.tools.NothingToDoFunction;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.util.json.GsonFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A unit of work of the consciousness. Owns its local message flow, request handles, the tool-calling
 * loop, and (in later phases) dangerous-confirmation waiting and safe-flush on interrupt.
 * <p>
 * The {@code ThoughtDispatcher} does not know a thought's internal state (tool-calls, handles, message
 * flow); those belong to the thought. There is no per-thought topic field: the memory tag is the global
 * conversation topic for COMMANDER, and the event's static topic for EVENT (see
 * COMPANION_ARCHITECTURE.md §2.4/§2.5).
 */
public final class Thought {

    /** Defensive cap on tool rounds until the dispatcher watchdog (Phase 3) owns runaway-loop termination. */
    private static final int MAX_TOOL_ROUNDS = 8;

    /** Existing llm.properties key for the COMMANDER service phrase spoken on an unrecoverable LLM response. */
    private static final String CANNOT_EXECUTE_KEY = "handler.common.cantDoNow";

    private final ThoughtSource source;
    private final Urgency urgency;
    private final String currentInput;
    /** Memory tag for an EVENT thought (from the static event-type map); null for COMMANDER. */
    private final ConversationTopic eventTopic;
    private final ThoughtContext ctx;

    private Thought(ThoughtSource source, Urgency urgency, String currentInput,
                    ConversationTopic eventTopic, ThoughtContext ctx) {
        this.source = source;
        this.urgency = urgency;
        this.currentInput = currentInput;
        this.eventTopic = eventTopic;
        this.ctx = ctx;
    }

    /**
     * Creates a thought from a commander reply. Its memory tag is the live global conversation topic
     * (which a {@code change_global_topic} call may move during the thought).
     */
    public static Thought commander(Urgency urgency, String input, ThoughtContext ctx) {
        return new Thought(ThoughtSource.COMMANDER, urgency, input, null, ctx);
    }

    /**
     * Creates a thought from a filtered game event. Its memory tag is fixed at birth from the static
     * event-type map; an EVENT thought never moves the global conversation topic.
     */
    public static Thought event(Urgency urgency, String summary, ConversationTopic eventTopic, ThoughtContext ctx) {
        return new Thought(ThoughtSource.EVENT, urgency, summary, eventTopic, ctx);
    }

    /**
     * Runs the thinking loop: compose -> LLM -> (first round) resolve topic + record input -> execute
     * tool-calls -> append results -> next round, until {@code nothing_to_do} ends the turn or the LLM
     * returns an unrecoverable response (§2.5/§2.6/§2.8/§5.1). Blocking: it joins on each gateway future.
     */
    public void run() {
        ComposedPrompt prompt = composeInitialPrompt();
        List<LlmMessage> flow = new ArrayList<>(prompt.messages());
        List<LlmToolDefinition> tools = prompt.tools(); // immutable snapshot, reused every round
        PromptCacheProfile profile = prompt.profile();

        boolean inputRecorded = false;
        for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
            LlmResult result = submitRound(flow, tools, profile);
            if (result == null || !result.isValid()) {
                onInvalidResponse(inputRecorded);
                return;
            }

            List<LlmToolInvocation> invocations = result.toolInvocations();

            // First valid response: resolve the topic and record the input before any tool runs (§2.6).
            Map<LlmToolInvocation, JsonObject> preExecuted = Map.of();
            if (!inputRecorded) {
                preExecuted = applyTopicChange(invocations);
                recordCurrentInput();
                inputRecorded = true;
            }

            if (runToolCalls(flow, invocations, preExecuted)) {
                return; // nothing_to_do terminated the turn
            }
        }
        // Round cap reached without nothing_to_do: end defensively (real watchdog is the dispatcher's).
    }

    /** One LLM round; a provider/transport failure (exceptional future) is treated as no usable result (§2.9). */
    private LlmResult submitRound(List<LlmMessage> flow, List<LlmToolDefinition> tools, PromptCacheProfile profile) {
        try {
            return ctx.llmGateway()
                    .submit(new LlmRequest(newId(), List.copyOf(flow), tools, profile))
                    .join();
        } catch (RuntimeException llmFailure) {
            return null;
        }
    }

    /** Assembles the seed prompt: access policy -> reduced game tools + system tools + memory snapshot. */
    private ComposedPrompt composeInitialPrompt() {
        Set<IntelActionCategory> categories = ctx.intelActionAccessPolicy().allowedCategories(source);
        List<LlmToolDefinition> selectedTools = ctx.reducer().selectTools(categories, currentInput);
        List<LlmToolDefinition> systemTools = ctx.systemFunctionProvider().systemFunctions(source);
        return ctx.promptComposer().compose(
                source, urgency, ctx.state().globalTopic(), currentInput,
                selectedTools, systemTools,
                ctx.memoryGateway().readShortTermTimeline(),
                ctx.memoryGateway().indexes(),
                ctx.memoryGateway().longTermSummary());
    }

    /**
     * COMMANDER pre-execution step (§2.5/§1.5.17): if the response calls {@code change_global_topic}, run
     * it now (its handle moves the global topic) so the recorded input is tagged with the new topic.
     * Returns the pre-executed result keyed by its invocation so the main loop does not run it twice.
     */
    private Map<LlmToolInvocation, JsonObject> applyTopicChange(List<LlmToolInvocation> invocations) {
        Map<LlmToolInvocation, JsonObject> preExecuted = new IdentityHashMap<>();
        if (source == ThoughtSource.COMMANDER) {
            for (LlmToolInvocation inv : invocations) {
                if (ChangeGlobalTopicFunction.ID.equals(inv.name())) {
                    preExecuted.put(inv, execute(inv));
                    break;
                }
            }
        }
        return preExecuted;
    }

    /**
     * Executes the round's tool-calls in LLM order, recording each result to memory; {@code nothing_to_do}
     * is the lifecycle terminator (not executed, not recorded). The assistant turn and its tool results are
     * appended to the flow only when another round will run, so a terminating turn sends nothing back.
     *
     * @return {@code true} if {@code nothing_to_do} ended the turn
     */
    private boolean runToolCalls(List<LlmMessage> flow, List<LlmToolInvocation> invocations,
                                 Map<LlmToolInvocation, JsonObject> preExecuted) {
        boolean terminate = false;
        List<LlmMessage> toolResults = new ArrayList<>();
        for (LlmToolInvocation inv : invocations) {
            if (NothingToDoFunction.ID.equals(inv.name())) {
                terminate = true;
                continue;
            }
            JsonObject result = preExecuted.containsKey(inv) ? preExecuted.get(inv) : execute(inv);
            recordToolResult(result);
            toolResults.add(LlmMessage.toolResult(inv.id(), stringify(result)));
        }
        if (!terminate) {
            flow.add(LlmMessage.assistantToolCalls(invocations));
            flow.addAll(toolResults);
        }
        return terminate;
    }

    /** Runs one tool-call via the execution gateway; a failed call becomes an error result the LLM can read. */
    private JsonObject execute(LlmToolInvocation inv) {
        try {
            return ctx.executionGateway()
                    .submit(new ExecutionRequest(newId(), inv.name(), inv.arguments()))
                    .join();
        } catch (RuntimeException failed) {
            Throwable cause = failed.getCause() != null ? failed.getCause() : failed;
            JsonObject error = new JsonObject();
            error.addProperty("error", String.valueOf(cause.getMessage()));
            error.addProperty("tool", inv.name());
            return error;
        }
    }

    /** Records the current input under the resolved topic before tool-calls run (§2.6). */
    private void recordCurrentInput() {
        MemorySource memorySource = source == ThoughtSource.COMMANDER ? MemorySource.COMMANDER : MemorySource.EVENT;
        ctx.memoryGateway().write(new MemoryEntry(
                Instant.now(), memoryTopic(), memorySource, currentInput, MemoryProcessingState.PROCESSED));
    }

    /** Records one tool result on the timeline as TOOL_RESULT under the current topic. */
    private void recordToolResult(JsonObject result) {
        ctx.memoryGateway().write(new MemoryEntry(
                Instant.now(), memoryTopic(), MemorySource.TOOL_RESULT, stringify(result), MemoryProcessingState.PROCESSED));
    }

    /**
     * Handles an unrecoverable LLM response (§2.9): records the still-unwritten input as unresolved; a
     * COMMANDER thought speaks a fixed service phrase, an EVENT thought ends silently.
     */
    private void onInvalidResponse(boolean inputRecorded) {
        if (!inputRecorded) {
            ConversationTopic topic = source == ThoughtSource.COMMANDER
                    ? ConversationTopic.UNRESOLVED_COMMANDER_INPUT
                    : ConversationTopic.UNRESOLVED_GAME_EVENT;
            MemorySource memorySource = source == ThoughtSource.COMMANDER ? MemorySource.COMMANDER : MemorySource.EVENT;
            ctx.memoryGateway().write(new MemoryEntry(
                    Instant.now(), topic, memorySource, currentInput, MemoryProcessingState.UNRESOLVED));
        }
        if (source == ThoughtSource.COMMANDER) {
            ctx.speechGateway().submit(new SpeechRequest(newId(), cannotExecutePhrase(), urgency));
        }
    }

    /** COMMANDER: the live global conversation topic; EVENT: the event's fixed topic. */
    private ConversationTopic memoryTopic() {
        return source == ThoughtSource.COMMANDER ? ctx.state().globalTopic() : eventTopic;
    }

    /** Interrupts the thought: safe-flush, cancel handles, no new work, then die (§2.7). */
    public void interrupt() {
        // TODO: Phase 3 - interrupt/safe-flush.
        throw new UnsupportedOperationException("TODO: Phase 3");
    }

    /** Delivers a confirmation; effective only while awaiting_confirmation (§2.13). */
    public void onConfirm(DangerousActionConfirmedEvent event) {
        // TODO: Phase 3 - dangerous confirmation.
        throw new UnsupportedOperationException("TODO: Phase 3");
    }

    public ThoughtSource source() {
        return source;
    }

    public Urgency urgency() {
        return urgency;
    }

    /** The fixed, code-generated "cannot execute" phrase in the commander's language (no LLM). */
    private static String cannotExecutePhrase() {
        Language language = AiResponseLanguagePolicy.resolveEffectiveAiResponseLanguage(SystemSession.getInstance());
        return LlmTextProvider.getText(language, CANNOT_EXECUTE_KEY);
    }

    /** Compact JSON of a tool result, for both the tool-result message and the memory entry. */
    private static String stringify(JsonObject result) {
        return GsonFactory.getGson().toJson(result);
    }

    private static String newId() {
        return UUID.randomUUID().toString();
    }
}
