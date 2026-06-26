package elite.intel.companion.mind;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.CommandOutcome;
import elite.intel.ai.brain.commons.AiResponseLanguagePolicy;
import elite.intel.ai.brain.i18n.LlmTextProvider;
import elite.intel.companion.confirm.ConfirmationCoordinator;
import elite.intel.companion.model.*;
import elite.intel.companion.model.execution.ExecutionRequest;
import elite.intel.companion.model.llm.*;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemoryProcessingState;
import elite.intel.companion.model.memory.MemorySource;
import elite.intel.companion.model.speech.SpeechRequest;
import elite.intel.companion.prompt.CompanionNarrationPolicy.Narration;
import elite.intel.companion.prompt.ComposedPrompt;
import elite.intel.companion.prompt.EventSpeechPolicy;
import elite.intel.companion.tools.ChangeGlobalTopicFunction;
import elite.intel.companion.tools.NothingToDoFunction;
import elite.intel.companion.tools.SpeakFunction;
import elite.intel.gameapi.journal.events.BaseEvent;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.util.json.GsonFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * A unit of work of the consciousness. Owns its local message flow, request handles, the tool-calling
 * loop, dangerous-action confirmation, and safe-flush on interrupt.
 * <p>
 * The {@code ThoughtDispatcher} does not know a thought's internal state (tool-calls, handles, message
 * flow); those belong to the thought. There is no per-thought topic field: the memory tag is the global
 * conversation topic for COMMANDER, and the event's static topic for EVENT (see
 * COMPANION_ARCHITECTURE.md §2.4/§2.5).
 * <p>
 * Threading: {@link #run} executes on a dispatcher lane thread; {@link #interrupt} is called from another
 * thread and cooperates via a volatile flag and by cancelling the awaited future, so the lane thread wakes,
 * safe-flushes and dies. An already-started action/macro is never cancelled (§1.9.41).
 */
public final class Thought {

    private static final Logger log = LogManager.getLogger(Thought.class);

    /** Defensive per-turn round cap, complementing the dispatcher watchdog's wall-clock timeout. */
    private static final int MAX_TOOL_ROUNDS = 8;

    /** How long a frozen dangerous set waits for the commander's confirmation before discard (§7.2 setting). */
    private static final long CONFIRMATION_TIMEOUT_SECONDS = 30;

    /** Existing llm.properties key for the COMMANDER service phrase spoken on an unrecoverable LLM response. */
    private static final String CANNOT_EXECUTE_KEY = "handler.common.cantDoNow";

    private final ThoughtSource source;
    private final Urgency urgency;
    private final String currentInput;
    private final EventInputKind eventInputKind;
    /** Memory tag for an EVENT thought (from the static event-type map); null for COMMANDER. */
    private final ConversationTopic eventTopic;
    /** Importance of the originating event for an EVENT thought; null for COMMANDER. Drives the NORMAL short-circuit. */
    private final BaseEvent.Importance importance;
    private final ThoughtContext ctx;

    /**
     * Turn-scoped narration accounting (COMMANDER only). Set once any game command/query runs this turn.
     * A command/query owns its spoken outcome deterministically - its {@code text_to_speech_response} is
     * vocalized verbatim (see {@link #vocalizeDeterministicOutcome}), a side-effect stays silent - so the
     * LLM's own {@code speak} is withheld for the rest of the turn to avoid re-voicing or rephrasing it.
     * The LLM only speaks on a turn that ran no command/query (pure conversation, memory recall).
     */
    private boolean turnRanGameAction;

    /** Set by {@link #interrupt} from another thread; the run loop honors it at step boundaries (§2.7). */
    private volatile boolean interrupted;
    /** The future the lane thread is currently awaiting (LLM round / confirmation wait), or null. */
    private volatile CompletableFuture<?> inFlight;

    private Thought(ThoughtSource source, Urgency urgency, String currentInput, EventInputKind eventInputKind,
                    ConversationTopic eventTopic, BaseEvent.Importance importance, ThoughtContext ctx) {
        this.source = source;
        this.urgency = urgency;
        this.currentInput = currentInput;
        this.eventInputKind = eventInputKind;
        this.eventTopic = eventTopic;
        this.importance = importance;
        this.ctx = ctx;
    }

    /**
     * Creates a thought from a commander reply. Its memory tag is the live global conversation topic
     * (which a {@code change_global_topic} call may move during the thought).
     */
    public static Thought commander(Urgency urgency, String input, ThoughtContext ctx) {
        return new Thought(ThoughtSource.COMMANDER, urgency, input, null, null, null, ctx);
    }

    /**
     * Creates a thought from a filtered game event. Its memory tag is fixed at birth from the static
     * event-type map; an EVENT thought never moves the global conversation topic. The event's importance
     * drives behaviour in {@link #run}: a {@code NORMAL} event is recorded to memory without engaging the
     * LLM, a {@code HIGH} one runs the full thinking loop ({@code LOW} never reaches here - the filter drops it).
     */
    public static Thought event(Urgency urgency, String summary, ConversationTopic eventTopic,
                                BaseEvent.Importance importance, ThoughtContext ctx) {
        return new Thought(ThoughtSource.EVENT, urgency, summary, EventInputKind.GAME_EVENT, eventTopic, importance, ctx);
    }

    /**
     * Creates an EVENT thought from subscriber-prepared sensor narration. The subscriber layer already
     * decided this is worth saying, so this kind skips game query tools and bypasses verbosity suppression
     * for {@code speak}; the LLM's job is to phrase the provided data/instructions.
     */
    public static Thought sensorNarration(Urgency urgency, String summary, ConversationTopic eventTopic, ThoughtContext ctx) {
        return new Thought(ThoughtSource.EVENT, urgency, summary, EventInputKind.SENSOR_NARRATION,
                eventTopic, BaseEvent.Importance.HIGH, ctx);
    }

    /**
     * Runs the thinking loop: compose -> LLM -> (first round) resolve topic + record input -> execute
     * tool-calls -> append results -> next round, until {@code nothing_to_do} ends the turn or the LLM
     * returns an unrecoverable response (§2.5/§2.6/§2.8/§5.1). Blocking: it joins on each gateway future.
     */
    public void run() {
        // NORMAL events are recorded to memory but never engage the LLM or speak (importance taxonomy,
        // COMPANION_ARCHITECTURE.md §2.2): write the memory entry under the event's static topic and end.
        if (source == ThoughtSource.EVENT && importance == BaseEvent.Importance.NORMAL) {
            recordCurrentInput();
            return;
        }
        boolean inputRecorded = false;
        try {
            ComposedPrompt prompt = composeInitialPrompt();
            List<LlmMessage> flow = new ArrayList<>(prompt.messages());
            List<LlmToolDefinition> tools = prompt.tools(); // immutable snapshot, reused every round
            PromptCacheProfile profile = prompt.profile();

            for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
                if (interrupted) {
                    safeFlush(inputRecorded);
                    return;
                }
                LlmResult result = submitRound(flow, tools, profile);
                if (interrupted) {
                    safeFlush(inputRecorded); // interrupt takes precedence over an invalid/cancelled result
                    return;
                }
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

                // §2.13: a dangerous action freezes the whole validated set for the commander's confirmation.
                if (hasDangerousAction(invocations)) {
                    handleDangerousConfirmation(invocations, preExecuted);
                    return; // a dangerous turn is terminal
                }

                if (runToolCalls(flow, invocations, preExecuted)) {
                    return; // nothing_to_do terminated the turn
                }
            }
            // Round cap reached without nothing_to_do: end defensively (the watchdog is the wall-clock backstop).
        } catch (RuntimeException unexpected) {
            // An unexpected failure (e.g. during prompt assembly) must leave no memory hole; the lane logs and survives.
            onInvalidResponse(inputRecorded);
            throw unexpected;
        }
    }

    /**
     * One LLM round, registered as the interruptible in-flight handle. A provider/transport failure or an
     * interrupt-driven cancellation (exceptional future) is treated as no usable result (§2.9/§2.7).
     */
    private LlmResult submitRound(List<LlmMessage> flow, List<LlmToolDefinition> tools, PromptCacheProfile profile) {
        CompletableFuture<LlmResult> future = ctx.llmGateway()
                .submit(new LlmRequest(newId(), List.copyOf(flow), tools, profile));
        inFlight = future;
        if (interrupted) {
            future.cancel(true); // interrupt raced ahead of registration: cancel now so join unblocks
        }
        try {
            return future.join();
        } catch (RuntimeException llmFailure) {
            if (!interrupted) {
                // A provider/transport failure (not an interrupt-driven cancel) - surface the cause.
                log.warn("Companion LLM round failed; treating as no usable result", llmFailure);
            }
            return null;
        } finally {
            inFlight = null;
        }
    }

    /** Assembles the seed prompt: access policy -> reduced game tools + system tools + memory snapshot. */
    private ComposedPrompt composeInitialPrompt() {
        List<LlmToolDefinition> selectedTools = selectedGameTools();
        return ctx.promptComposer().compose(
                source, urgency, ctx.state().globalTopic(), currentInput,
                selectedTools, systemTools(),
                ctx.memoryGateway().readShortTermTimeline(),
                ctx.memoryGateway().indexes(),
                ctx.memoryGateway().longTermSummary());
    }

    /**
     * Game/query tool selection for the thought. Subscriber-prepared sensor narration does not receive
     * query tools: the event subscriber already calculated and filtered the data to narrate.
     */
    private List<LlmToolDefinition> selectedGameTools() {
        if (eventInputKind == EventInputKind.SENSOR_NARRATION) {
            return List.of();
        }
        Set<IntelActionCategory> categories = ctx.intelActionAccessPolicy().allowedCategories(source);
        return ctx.reducer().selectTools(categories, currentInput);
    }

    /**
     * System tools for this thought; an EVENT thought is denied {@code speak} when commentary is not
     * allowed by its urgency and the current verbosity (§2.11/§4.2), leaving it only {@code nothing_to_do}.
     */
    private List<LlmToolDefinition> systemTools() {
        List<LlmToolDefinition> tools = ctx.systemFunctionProvider().systemFunctions(source);
        if (source == ThoughtSource.EVENT
                && eventInputKind != EventInputKind.SENSOR_NARRATION
                && !EventSpeechPolicy.mayComment(urgency, ctx.state().verbosity())) {
            return tools.stream().filter(tool -> !SpeakFunction.ID.equals(tool.name())).toList();
        }
        return tools;
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
        boolean suppressSpeak = shouldSuppressSpeak(invocations);
        boolean terminate = false;
        List<LlmMessage> toolResults = new ArrayList<>();
        for (LlmToolInvocation inv : invocations) {
            if (NothingToDoFunction.ID.equals(inv.name())) {
                terminate = true;
                continue;
            }
            // A speak voiced only alongside a command/query (whose result is voiced deterministically) is
            // dropped: the action still ran, but the LLM's speak fires no TTS. A synthetic result keeps the
            // assistant/tool-result pairing intact so a following round stays valid, and tells the LLM the
            // narration was intentionally withheld.
            JsonObject result = suppressSpeak && SpeakFunction.ID.equals(inv.name())
                    ? narrationSuppressedResult(inv.name())
                    : (preExecuted.containsKey(inv) ? preExecuted.get(inv) : execute(inv));
            vocalizeDeterministicOutcome(inv, result);
            recordToolResult(result);
            toolResults.add(LlmMessage.toolResult(inv.id(), stringify(result)));
        }
        if (!terminate) {
            flow.add(LlmMessage.assistantToolCalls(invocations));
            flow.addAll(toolResults);
        }
        return terminate;
    }

    /**
     * Folds this round's game actions into the turn's narration accounting and decides whether the round's
     * {@code speak} should be withheld. Only COMMANDER turns are gated (EVENT speech has its own policy via
     * {@link #systemTools()}). A command/query owns its spoken outcome deterministically (vocalized verbatim
     * or silent), so once any command/query has run this turn the LLM's own {@code speak} is dropped - the
     * LLM neither re-voices nor rephrases a handler result. A turn that ran no command/query (only system
     * functions such as memory recall, or pure conversation) still speaks.
     */
    private boolean shouldSuppressSpeak(List<LlmToolInvocation> invocations) {
        if (source != ThoughtSource.COMMANDER) {
            return false;
        }
        for (LlmToolInvocation inv : invocations) {
            if (SpeakFunction.ID.equals(inv.name()) || NothingToDoFunction.ID.equals(inv.name())) {
                continue;
            }
            Narration narration = ctx.narrationPolicy().classify(inv.name());
            if (narration == Narration.NARRATABLE || narration == Narration.SILENT_COMMAND) {
                turnRanGameAction = true; // a command/query ran; its outcome is voiced (or silent) deterministically
            }
        }
        return turnRanGameAction;
    }

    /**
     * Deterministic vocalization of a command/query outcome (COMMANDER only): the handler - not the LLM -
     * owns whether and what a result says. A {@code handle()} that returned a {@code text_to_speech_response}
     * (a {@code CommandOutcome} string or a query's analysis sentence) is voiced verbatim through the speech
     * gateway, on the urgent channel when the outcome is mission-critical; a side-effect outcome (no spoken
     * text) stays silent. System functions ({@link Narration#NEUTRAL}) own their own speech and are skipped
     * here, as are EVENT turns (the consciousness narrates events in character from the query data).
     */
    private void vocalizeDeterministicOutcome(LlmToolInvocation inv, JsonObject result) {
        if (source != ThoughtSource.COMMANDER) {
            return;
        }
        if (SpeakFunction.ID.equals(inv.name()) || NothingToDoFunction.ID.equals(inv.name())) {
            return;
        }
        Narration narration = ctx.narrationPolicy().classify(inv.name());
        if (narration != Narration.NARRATABLE && narration != Narration.SILENT_COMMAND) {
            return; // only command/query outcomes are vocalized here
        }
        String spoken = CommandOutcome.spokenText(result);
        if (spoken.isBlank()) {
            return; // side-effect outcome: silent by design
        }
        Urgency speechUrgency = CommandOutcome.isCritical(result) ? Urgency.URGENT : Urgency.NORMAL;
        ctx.speechGateway().submit(new SpeechRequest(newId(), spoken, speechUrgency));
    }

    /**
     * Synthetic tool result standing in for a withheld {@code speak} on a turn whose command/query already
     * owns the spoken outcome.
     */
    private static JsonObject narrationSuppressedResult(String toolName) {
        JsonObject suppressed = new JsonObject();
        suppressed.addProperty("status", "narration_suppressed");
        suppressed.addProperty("tool", toolName);
        return suppressed;
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

    /** Whether any tool-call in the validated set is a dangerous action requiring confirmation (§2.13). */
    private boolean hasDangerousAction(List<LlmToolInvocation> invocations) {
        for (LlmToolInvocation inv : invocations) {
            if (ctx.dangerousActionPolicy().isDangerous(inv)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Freezes the validated tool-call set and waits for the commander's confirmation (§2.13/§5.3). The
     * confirmation_request {@code speak} (if present) is voiced immediately; the rest stays frozen. On
     * confirm the whole set runs in LLM order; on cancel/timeout it is discarded. The outcome is recorded
     * and the turn ends (terminal).
     */
    private void handleDangerousConfirmation(List<LlmToolInvocation> invocations,
                                             Map<LlmToolInvocation, JsonObject> preExecuted) {
        ctx.memoryGateway().write(new MemoryEntry(Instant.now(), memoryTopic(), MemorySource.SYSTEM,
                "dangerous action requires confirmation", MemoryProcessingState.AWAITING_CONFIRMATION));

        // The question is voiced now; it is not a tool result, so it is not recorded as one.
        LlmToolInvocation confirmationSpeak = findConfirmationSpeak(invocations);
        if (confirmationSpeak != null) {
            execute(confirmationSpeak);
        }

        MemoryProcessingState outcome = awaitConfirmationOutcome();
        if (outcome == MemoryProcessingState.CONFIRMED) {
            // Execute the frozen set in LLM order, skipping the already-voiced confirmation_request.
            for (LlmToolInvocation inv : invocations) {
                if (inv == confirmationSpeak || NothingToDoFunction.ID.equals(inv.name())) {
                    continue;
                }
                JsonObject result = preExecuted.containsKey(inv) ? preExecuted.get(inv) : execute(inv);
                recordToolResult(result);
            }
        }
        ctx.memoryGateway().write(new MemoryEntry(Instant.now(), memoryTopic(), MemorySource.SYSTEM,
                "dangerous action " + outcome.name().toLowerCase(Locale.ROOT), outcome));
    }

    /** Blocks on the confirmation coordinator; maps confirm/cancel/timeout/overlap to a memory outcome. */
    private MemoryProcessingState awaitConfirmationOutcome() {
        ConfirmationCoordinator coordinator = ctx.confirmationCoordinator();
        CompletableFuture<Boolean> wait = coordinator.open();
        if (wait == null) {
            return MemoryProcessingState.CANCELLED; // an overlapping confirmation is already pending (§1.6.25)
        }
        inFlight = wait;
        if (interrupted) {
            wait.cancel(true);
        }
        try {
            return wait.get(CONFIRMATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    ? MemoryProcessingState.CONFIRMED
                    : MemoryProcessingState.CANCELLED;
        } catch (TimeoutException timedOut) {
            return MemoryProcessingState.TIMED_OUT;
        } catch (CancellationException interruptedWait) {
            return MemoryProcessingState.INTERRUPTED;
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            return MemoryProcessingState.INTERRUPTED;
        } catch (ExecutionException failed) {
            return MemoryProcessingState.CANCELLED;
        } finally {
            inFlight = null;
            coordinator.close(wait);
        }
    }

    /** The {@code speak} carrying the confirmation_request marker, or null if the LLM included none. */
    private static LlmToolInvocation findConfirmationSpeak(List<LlmToolInvocation> invocations) {
        for (LlmToolInvocation inv : invocations) {
            if (SpeakFunction.ID.equals(inv.name())) {
                JsonObject args = inv.arguments();
                if (args.has(SpeakFunction.PARAM_CONFIRMATION_REQUEST)
                        && args.get(SpeakFunction.PARAM_CONFIRMATION_REQUEST).isJsonPrimitive()
                        && args.get(SpeakFunction.PARAM_CONFIRMATION_REQUEST).getAsBoolean()) {
                    return inv;
                }
            }
        }
        return null;
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

    /**
     * Safe-flush on interrupt (§2.7): never leave a memory hole. If the input was not yet recorded, write
     * it under the source's unresolved fallback as INTERRUPTED; tool results are written as they execute,
     * so nothing is batched to flush. No new LLM/query/action/speech is started after this.
     */
    private void safeFlush(boolean inputRecorded) {
        if (!inputRecorded) {
            ConversationTopic topic = source == ThoughtSource.COMMANDER
                    ? ConversationTopic.UNRESOLVED_COMMANDER_INPUT
                    : ConversationTopic.UNRESOLVED_GAME_EVENT;
            MemorySource memorySource = source == ThoughtSource.COMMANDER ? MemorySource.COMMANDER : MemorySource.EVENT;
            ctx.memoryGateway().write(new MemoryEntry(
                    Instant.now(), topic, memorySource, currentInput, MemoryProcessingState.INTERRUPTED));
        }
    }

    /**
     * Interrupts the thought from another thread (§2.7): raises the interrupt flag and cancels the awaited
     * future so the lane thread unblocks, safe-flushes and dies. It never cancels a started action/macro
     * (§1.9.41) and writes no memory itself - the owning thread owns the safe-flush.
     */
    public void interrupt() {
        interrupted = true;
        CompletableFuture<?> current = inFlight;
        if (current != null) {
            current.cancel(true);
        }
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
