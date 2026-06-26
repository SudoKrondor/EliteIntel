package elite.intel.companion.mind;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.CommandOutcome;
import elite.intel.ai.brain.commons.AiResponseLanguagePolicy;
import elite.intel.ai.brain.i18n.LlmTextProvider;
import elite.intel.companion.confirm.ConfirmationCoordinator;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.Urgency;
import elite.intel.companion.model.execution.ExecutionRequest;
import elite.intel.companion.model.llm.LlmMessage;
import elite.intel.companion.model.llm.LlmResult;
import elite.intel.companion.model.llm.LlmToolDefinition;
import elite.intel.companion.model.llm.LlmToolInvocation;
import elite.intel.companion.model.llm.PromptCacheProfile;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemoryProcessingState;
import elite.intel.companion.model.memory.MemorySource;
import elite.intel.companion.model.speech.SpeechRequest;
import elite.intel.companion.prompt.ComposedPrompt;
import elite.intel.companion.prompt.CompanionNarrationPolicy.Narration;
import elite.intel.companion.tools.ChangeGlobalTopicFunction;
import elite.intel.companion.tools.NothingToDoFunction;
import elite.intel.companion.tools.SpeakFunction;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;

import java.time.Instant;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A thought born from a commander reply. It owns the full tool-calling loop: compose -> LLM round -> (first
 * round) apply {@code change_global_topic} and record the input -> dangerous-action confirmation -> execute
 * tool-calls -> next round, until {@code nothing_to_do} ends the turn or an unrecoverable response stops it
 * (§2.5/§2.6/§2.8/§5.1).
 * <p>
 * It has the full commander tool set and the COMMANDER-only paths an EVENT/narration thought cannot reach:
 * applying {@code change_global_topic} before the input is filed, dispatching commands/queries
 * fire-and-forget, and vocalizing their outcome deterministically. Narration ownership (§2.14): a
 * command/query owns its spoken outcome - the handler's {@code text_to_speech_response} is voiced verbatim
 * and a side-effect stays silent - so once any command/query runs this turn the LLM's own {@code speak} is
 * withheld (no re-voicing or rephrasing). A turn that ran no command/query (pure conversation, memory recall)
 * still speaks.
 */
public final class CommanderThought extends Thought {

    /** Defensive per-turn round cap, complementing the dispatcher watchdog's wall-clock timeout. */
    private static final int MAX_TOOL_ROUNDS = 8;
    /** How long a frozen dangerous set waits for the commander's confirmation before discard (§7.2 setting). */
    private static final long CONFIRMATION_TIMEOUT_SECONDS = 30;
    /** Existing llm.properties key for the COMMANDER service phrase spoken on an unrecoverable LLM response. */
    private static final String CANNOT_EXECUTE_KEY = "handler.common.cantDoNow";

    /**
     * Turn-scoped narration accounting. Set once any game command/query runs this turn; from then on the
     * LLM's own {@code speak} is withheld for the rest of the turn (the command/query already owns the
     * spoken outcome).
     */
    private boolean turnRanGameAction;

    CommanderThought(Urgency urgency, String input, ThoughtContext ctx) {
        super(ThoughtSource.COMMANDER, urgency, input, ctx);
    }

    /**
     * The full thinking loop. Blocking: it joins on each gateway future. Interrupt is honored at step
     * boundaries via safe-flush; an unrecoverable response speaks a service phrase and ends the turn.
     */
    @Override
    public void run() {
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

                if (executeRound(flow, invocations, preExecuted)) {
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

    /** The live global conversation topic (a {@code change_global_topic} call may move it during the thought). */
    @Override
    protected ConversationTopic memoryTopic() {
        return ctx.state().globalTopic();
    }

    // Game-tool categories are the access policy's default for COMMANDER (QUERY/ACTION/MACRO); inherited.

    @Override
    protected List<LlmToolDefinition> systemTools() {
        return ctx.systemFunctionProvider().systemFunctions(source());
    }

    /**
     * COMMANDER pre-execution step (§2.5/§1.5.17): if the response calls {@code change_global_topic}, run it
     * now (its handle moves the global topic) so the recorded input is tagged with the new topic. Returns the
     * pre-executed result keyed by its invocation so the main loop does not run it twice.
     */
    private Map<LlmToolInvocation, JsonObject> applyTopicChange(List<LlmToolInvocation> invocations) {
        Map<LlmToolInvocation, JsonObject> preExecuted = new IdentityHashMap<>();
        for (LlmToolInvocation inv : invocations) {
            if (ChangeGlobalTopicFunction.ID.equals(inv.name())) {
                preExecuted.put(inv, execute(inv));
                break;
            }
        }
        return preExecuted;
    }

    /**
     * Executes the round's tool-calls in LLM order. A command/query is dispatched fire-and-forget (see
     * {@link #dispatchFireAndForget}): the lane never blocks on a handler, so a multi-minute search cannot
     * freeze command input. Its spoken outcome is vocalized, and its real result recorded, by the completion
     * callback whenever the handler finishes; the LLM flow gets a synthetic {@code dispatched} marker (never
     * persisted) so the turn can end without waiting. {@code speak} is voiced (or withheld) here, system
     * functions run synchronously, and {@code nothing_to_do} is the lifecycle terminator.
     *
     * @return {@code true} if {@code nothing_to_do} ended the turn
     */
    private boolean executeRound(List<LlmMessage> flow, List<LlmToolInvocation> invocations,
                                 Map<LlmToolInvocation, JsonObject> preExecuted) {
        boolean suppressSpeak = shouldSuppressSpeak(invocations);
        boolean terminate = false;
        List<LlmMessage> toolResults = new ArrayList<>();
        for (LlmToolInvocation inv : invocations) {
            if (NothingToDoFunction.ID.equals(inv.name())) {
                terminate = true;
                continue;
            }
            if (!preExecuted.containsKey(inv) && isFireAndForget(inv)) {
                // Command/query: dispatch without blocking the lane. The completion callback is the sole memory
                // writer and vocalizer for this tool; the LLM flow gets a marker so the turn can end without
                // waiting, but the marker is not persisted - only the real outcome is, when ready.
                dispatchFireAndForget(inv);
                toolResults.add(LlmMessage.toolResult(inv.id(), stringify(dispatchedResult(inv.name()))));
                continue;
            }
            if (SpeakFunction.ID.equals(inv.name())) {
                if (suppressSpeak) {
                    // A command/query already owns the spoken outcome this turn, so the LLM's speak fires no
                    // TTS - nothing is said, nothing is recorded. The synthetic result keeps the
                    // assistant/tool-result pairing intact and tells the LLM the narration was withheld.
                    toolResults.add(LlmMessage.toolResult(inv.id(), stringify(narrationSuppressedResult(inv.name()))));
                } else {
                    // The companion's own voice: vocalize and record the words said as a COMPANION entry (not a
                    // {"status":"spoken"} ack), so a later turn knows it already answered.
                    JsonObject result = execute(inv);
                    recordCompanionSpeech(spokenTextOf(inv));
                    toolResults.add(LlmMessage.toolResult(inv.id(), stringify(result)));
                }
                continue;
            }
            // System functions (change_global_topic, remember, search): execute and record the tool result.
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

    /**
     * Whether a tool-call is dispatched fire-and-forget rather than awaited. A command/query qualifies: its
     * outcome is vocalized deterministically (the LLM does not consume the result), so the lane need not wait
     * for it. {@code speak}/{@code nothing_to_do} are handled before this is reached; system functions
     * ({@link Narration#NEUTRAL}) are awaited.
     */
    private boolean isFireAndForget(LlmToolInvocation inv) {
        Narration narration = ctx.narrationPolicy().classify(inv.name());
        return narration == Narration.NARRATABLE || narration == Narration.SILENT_COMMAND;
    }

    /**
     * Dispatches a command/query without blocking the lane (§1.9 responsiveness). The handler runs on the
     * execution gateway's pool; whenever it finishes - 1ms or minutes later - the completion callback voices
     * its deterministic outcome and records the real result. The topic is captured now so a late result is
     * filed under the topic that was current when the commander asked, even if the global topic has since
     * moved. The thought may have ended by then; the background job and its callback outlive it.
     */
    private void dispatchFireAndForget(LlmToolInvocation inv) {
        ConversationTopic topic = memoryTopic();
        CompletableFuture<JsonObject> future;
        try {
            future = ctx.executionGateway().submit(new ExecutionRequest(newId(), inv.name(), inv.arguments()));
        } catch (RuntimeException dispatchFailed) {
            recordToolResultUnderTopic(topic, executionError(inv.name(), dispatchFailed));
            return;
        }
        future.whenComplete((result, error) -> {
            JsonObject outcome = error != null ? executionError(inv.name(), error) : result;
            vocalizeDeterministicOutcome(inv, outcome);
            recordToolResultUnderTopic(topic, outcome);
        });
    }

    /**
     * Synthetic tool result standing in for a fire-and-forget dispatch whose real result arrives later.
     */
    private static JsonObject dispatchedResult(String toolName) {
        JsonObject dispatched = new JsonObject();
        dispatched.addProperty("status", "dispatched");
        dispatched.addProperty("tool", toolName);
        return dispatched;
    }

    /**
     * Folds this round's game actions into the turn's narration accounting and decides whether the round's
     * {@code speak} should be withheld. A command/query owns its spoken outcome deterministically (vocalized
     * verbatim or silent), so once any command/query has run this turn the LLM's own {@code speak} is dropped
     * - the LLM neither re-voices nor rephrases a handler result. A turn that ran no command/query (only
     * system functions such as memory recall, or pure conversation) still speaks.
     */
    private boolean shouldSuppressSpeak(List<LlmToolInvocation> invocations) {
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
     * Deterministic vocalization of a command/query outcome: the handler - not the LLM - owns whether and
     * what a result says. A {@code handle()} that returned a {@code text_to_speech_response} (a
     * {@code CommandOutcome} string or a query's analysis sentence) is voiced verbatim through the speech
     * gateway, on the urgent channel when the outcome is mission-critical; a side-effect outcome (no spoken
     * text) stays silent. System functions ({@link Narration#NEUTRAL}) own their own speech and are skipped.
     */
    private void vocalizeDeterministicOutcome(LlmToolInvocation inv, JsonObject result) {
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
     * confirmation_request {@code speak} (if present) is voiced immediately and recorded as the companion's
     * words; the rest stays frozen. On confirm the whole set runs in LLM order; on cancel/timeout it is
     * discarded. The outcome is recorded and the turn ends (terminal).
     */
    private void handleDangerousConfirmation(List<LlmToolInvocation> invocations,
                                             Map<LlmToolInvocation, JsonObject> preExecuted) {
        ctx.memoryGateway().write(new MemoryEntry(Instant.now(), memoryTopic(), MemorySource.SYSTEM,
                "dangerous action requires confirmation", MemoryProcessingState.AWAITING_CONFIRMATION));

        // The question is voiced now; it is the companion's own words, recorded as a COMPANION entry (not a
        // tool result).
        LlmToolInvocation confirmationSpeak = findConfirmationSpeak(invocations);
        if (confirmationSpeak != null) {
            execute(confirmationSpeak);
            recordCompanionSpeech(spokenTextOf(confirmationSpeak));
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
     * Handles an unrecoverable LLM response (§2.9): records the still-unwritten input as unresolved and speaks
     * a fixed service phrase (no LLM). The turn ends.
     */
    private void onInvalidResponse(boolean inputRecorded) {
        if (!inputRecorded) {
            ctx.memoryGateway().write(new MemoryEntry(Instant.now(), ConversationTopic.UNRESOLVED_COMMANDER_INPUT,
                    MemorySource.COMMANDER, currentInput, MemoryProcessingState.UNRESOLVED));
        }
        ctx.speechGateway().submit(new SpeechRequest(newId(), cannotExecutePhrase(), urgency()));
    }

    /**
     * Safe-flush on interrupt (§2.7): never leave a memory hole. If the input was not yet recorded, write it
     * under the unresolved-commander-input fallback as INTERRUPTED; tool results are written as they execute,
     * so nothing is batched to flush. No new LLM/query/action/speech is started after this.
     */
    private void safeFlush(boolean inputRecorded) {
        if (!inputRecorded) {
            ctx.memoryGateway().write(new MemoryEntry(Instant.now(), ConversationTopic.UNRESOLVED_COMMANDER_INPUT,
                    MemorySource.COMMANDER, currentInput, MemoryProcessingState.INTERRUPTED));
        }
    }

    /** Records one tool result on the timeline as TOOL_RESULT under the current topic. */
    private void recordToolResult(JsonObject result) {
        recordToolResultUnderTopic(memoryTopic(), result);
    }

    /**
     * Records one tool result as TOOL_RESULT under a given topic (captured for a late fire-and-forget result).
     */
    private void recordToolResultUnderTopic(ConversationTopic topic, JsonObject result) {
        ctx.memoryGateway().write(new MemoryEntry(
                Instant.now(), topic, MemorySource.TOOL_RESULT, stringify(result), MemoryProcessingState.PROCESSED));
    }

    /** The fixed, code-generated "cannot execute" phrase in the commander's language (no LLM). */
    private static String cannotExecutePhrase() {
        Language language = AiResponseLanguagePolicy.resolveEffectiveAiResponseLanguage(SystemSession.getInstance());
        return LlmTextProvider.getText(language, CANNOT_EXECUTE_KEY);
    }
}
