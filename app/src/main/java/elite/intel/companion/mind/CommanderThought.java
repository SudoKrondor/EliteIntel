package elite.intel.companion.mind;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.commons.AiResponseLanguagePolicy;
import elite.intel.ai.brain.i18n.LlmTextProvider;
import elite.intel.companion.confirm.ConfirmationCoordinator;
import elite.intel.companion.diag.CompanionDiagnostics;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.llm.LlmMessage;
import elite.intel.companion.model.llm.LlmResult;
import elite.intel.companion.model.llm.LlmToolDefinition;
import elite.intel.companion.model.llm.LlmToolInvocation;
import elite.intel.companion.model.llm.PromptCacheProfile;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemoryImportance;
import elite.intel.companion.model.memory.MemoryProcessingState;
import elite.intel.companion.model.memory.MemorySource;
import elite.intel.companion.model.memory.TurnBoundaryMarkers;
import elite.intel.companion.model.speech.SpeechRequest;
import elite.intel.companion.memory.facts.MergedFactCandidates;
import elite.intel.companion.memory.facts.MemoryFactContext;
import elite.intel.companion.prompt.ComposedPrompt;
import elite.intel.companion.prompt.Fact;
import elite.intel.companion.tools.ClassifyTurnFunction;
import elite.intel.companion.tools.IntelActionTypeResolver.IntelActionType;
import elite.intel.companion.tools.SpeakFunction;
import elite.intel.companion.tools.SystemFunctionResultFields;
import elite.intel.util.json.JsonUtils;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.util.StringUtls;

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
 * A thought born from a commander reply. It owns the tool-calling turn: compose -> LLM round -> apply
 * {@code classify_turn} -> freeze the turn topic and file the input -> dispatch the settling call. It is
 * single-round by design: memory is retrieved before the turn as inlined answer facts, so no in-turn lookup
 * round is needed. A game handler may finish after later cognitive turns, but its result remains owned by this
 * thought and its frozen topic (§2.5/§2.6/§2.8/§5.1).
 * <p>
 * It has the full commander tool set and the COMMANDER-only paths an EVENT/narration thought cannot reach:
 * applying {@code classify_turn} (topic + importance + is_question) before the input is filed, detaching
 * commands/queries from the ordered cognitive worker, and vocalizing their outcome deterministically. Narration ownership (§2.14): a
 * command/query owns its spoken outcome - the handler's {@code text_to_speech_response} is voiced verbatim
 * and a side-effect stays silent - so once any command/query runs this turn the LLM's own {@code speak} is
 * withheld (no re-voicing or rephrasing). A turn that ran no command/query (pure conversation, memory recall)
 * still speaks.
 */
public final class CommanderThought extends Thought {

    /** How long a frozen dangerous set waits for the commander's confirmation before discard (§7.2 setting). */
    private static final long CONFIRMATION_TIMEOUT_SECONDS = 30;
    /** Existing llm.properties key for the COMMANDER service phrase spoken on an unrecoverable LLM response. */
    private static final String CANNOT_EXECUTE_KEY = "handler.common.cantDoNow";
    /** llm.properties key for the fixed, code-voiced dangerous-action confirmation prompt (§2.13). */
    private static final String CONFIRM_DANGEROUS_KEY = "handler.common.confirmDangerousAction";

    /**
     * Turn-scoped narration accounting. Set once any game command/query runs this turn; from then on the
     * LLM's own {@code speak} is withheld for the rest of the turn (the command/query already owns the
     * spoken outcome).
     */
    private boolean turnRanGameAction;

    /** Importance the consciousness set for this turn via classify_turn (default NORMAL); stamps the turn's entries. */
    private MemoryImportance turnImportance = MemoryImportance.NORMAL;

    /** Whether classify_turn flagged this turn as a question; a question is still recorded, but stamped LOW so it is not a fact candidate. */
    private boolean turnIsQuestion;

    /** The clean canonical fact the consciousness stated for this turn via classify_turn (empty when none). */
    private String turnCanonicalFact = "";

    /** Topic frozen for this turn before slow execution detaches from the ordered commander cognitive lane. */
    private volatile ConversationTopic turnTopic;

    CommanderThought(ThoughtContext context, ThoughtDependencies dependencies) {
        super(context, dependencies);
    }

    /** Runs the complete turn and waits for any detached game handler; retained for direct callers and tests. */
    @Override
    public void run() {
        startLifecycle().join();
    }

    /**
     * Runs the ordered cognitive stage on the commander lane and returns the detached handler completion. The
     * lane worker may accept the next commander turn once this method returns, while ThoughtLane keeps this
     * thought live for cancellation, watchdog, shutdown, and {@code isIdle()} until the future settles.
     */
    @Override
    CompletableFuture<Void> startLifecycle() {
        if (isStopped()) {
            return CompletableFuture.completedFuture(null);
        }
        try {
            return beginTurn();
        } catch (Throwable failure) {
            return CompletableFuture.failedFuture(failure);
        }
    }

    private CompletableFuture<Void> beginTurn() {
        boolean inputRecorded = false;
        turnTopic = dependencies.state().globalTopic();
        try {
            ComposedPrompt prompt = composeInitialPrompt();
            List<LlmMessage> flow = new ArrayList<>(prompt.messages());
            List<LlmToolDefinition> tools = prompt.tools(); // immutable snapshot, reused every round
            PromptCacheProfile profile = prompt.profile();

            // Single-round by design: one LLM round settles the turn (memory is retrieved before the turn as
            // inlined answer facts, so there is no in-turn lookup round).
            if (isStopped()) {
                safeFlush(inputRecorded);
                return CompletableFuture.completedFuture(null);
            }
            LlmResult result = submitRound(flow, tools, profile);
            if (isStopped()) {
                safeFlush(inputRecorded); // interrupt takes precedence over an invalid/cancelled result
                return CompletableFuture.completedFuture(null);
            }
            if (result == null || !result.isValid()) {
                onInvalidResponse(inputRecorded);
                return CompletableFuture.completedFuture(null);
            }

            List<LlmToolInvocation> invocations = result.toolInvocations();

            // Classify the turn (topic + importance + is_question) before any tool runs (§2.6).
            Map<LlmToolInvocation, JsonObject> preExecuted = applyClassification(invocations);
            // Every turn's input is filed so the dialogue history keeps its user/assistant alternation - a command
            // turn included: its imperative ("optimal speed", "request docking") is remembered as the user turn,
            // paired with the companion's spoken reply (the handler outcome, or the immediate acknowledgement when
            // the command returns none). A question is stamped LOW in applyClassification and filtered out of
            // fact-recall candidates; a statement keeps its rated importance. (A command's call echo is still
            // dropped - see gameToolCallId - so the pair replays as plain dialogue, not a tool-call.)
            recordCurrentInput();
            // Mark the input handled either way - filed, or deliberately skipped for a command turn - so an
            // interrupt/error does not fall back to re-filing it as unresolved input.
            inputRecorded = true;

            // An interrupt landing after the input was handled (filed, or skipped for a command turn) but before
            // the turn replies is a cut-off turn: route it through safeFlush so it drops a <cut_off/> boundary
            // marker instead of ending silently.
            if (isStopped()) {
                safeFlush(inputRecorded);
                return CompletableFuture.completedFuture(null);
            }

            // §2.13: a dangerous action freezes the whole validated set for the commander's confirmation.
            List<LlmToolInvocation> dangerous = dangerousActions(invocations);
            if (!dangerous.isEmpty()) {
                handleDangerousConfirmation(tools, invocations, preExecuted, dangerous);
                return CompletableFuture.completedFuture(null); // a dangerous turn is terminal
            }

            // Execute the settling call(s) - a command/query/macro, a speak, or a bare classify_turn - and end.
            return executeRound(tools, invocations, preExecuted);
        } catch (RuntimeException unexpected) {
            // An unexpected failure (e.g. during prompt assembly) must leave no memory hole; the lane logs and survives.
            onInvalidResponse(inputRecorded);
            throw unexpected;
        }
    }

    /** The topic frozen for this turn after classify_turn, inherited from global state when classification is absent. */
    @Override
    protected ConversationTopic memoryTopic() {
        ConversationTopic frozen = turnTopic;
        return frozen != null ? frozen : dependencies.state().globalTopic();
    }

    /** The importance the consciousness set for this turn via {@code classify_turn} (default NORMAL). */
    @Override
    protected MemoryImportance memoryImportance() {
        return turnImportance;
    }

    // Game-tool categories are the access policy's default for COMMANDER (QUERY/ACTION/MACRO); inherited.

    @Override
    protected List<LlmToolDefinition> systemTools() {
        return dependencies.systemFunctionProvider().systemFunctions(source());
    }

    /** Pre-turn clean answer facts for this commander input (memory core plus pluggable sources), inlined as {@code <facts>}. */
    @Override
    protected List<Fact> memoryCandidates() {
        return MergedFactCandidates.forInput(dependencies.memoryGateway(),
                new MemoryFactContext(context.matchInput(), source(), urgency()), context.semanticQuery());
    }

    /** The canonical fact classify_turn stated this turn (empty when none), for the recorded entry. */
    @Override
    protected String memoryCanonicalFact() {
        return turnCanonicalFact;
    }

    /**
     * COMMANDER pre-execution step (§2.5/§1.5.17): if the response calls {@code classify_turn}, apply it now,
     * before the input is filed - read its importance into the turn's importance (so the recorded input and
     * outcome are stamped with it), read its {@code is_question} flag (a question is still recorded, but forced
     * to LOW so it never becomes a fact candidate), and run its handle, which moves the global topic (so the
     * input is tagged with the new topic). Returns the pre-executed result keyed by its invocation so the main
     * loop does not run it twice. An absent or unknown importance leaves the turn at {@code NORMAL}; an absent
     * flag leaves it a non-question; an absent {@code classify_turn} leaves the topic unchanged.
     */
    private Map<LlmToolInvocation, JsonObject> applyClassification(List<LlmToolInvocation> invocations) {
        Map<LlmToolInvocation, JsonObject> preExecuted = new IdentityHashMap<>();
        for (LlmToolInvocation inv : invocations) {
            if (ClassifyTurnFunction.ID.equals(inv.name())) {
                MemoryImportance level = MemoryImportance.fromId(
                        JsonUtils.getAsStringOrEmpty(inv.arguments(), ClassifyTurnFunction.PARAM_IMPORTANCE));
                if (level != null) {
                    turnImportance = level;
                }
                turnIsQuestion = Boolean.parseBoolean(
                        JsonUtils.getAsStringOrEmpty(inv.arguments(), ClassifyTurnFunction.PARAM_IS_QUESTION));
                if (turnIsQuestion) {
                    // A question carries no durable fact; stamp it LOW so its recorded input never surfaces as a
                    // fact-recall candidate (the MemoryMergedFactCandidates filter drops LOW commander lines).
                    turnImportance = MemoryImportance.LOW;
                }
                turnCanonicalFact = cleanCanonicalFact(JsonUtils.getAsStringOrEmpty(
                        inv.arguments(), ClassifyTurnFunction.PARAM_CANONICAL_FACT));
                JsonObject classified = execute(inv); // runs classify_turn's handle before this turn is filed
                preExecuted.put(inv, classified);
                ConversationTopic selectedTopic = ConversationTopic.fromSelectableId(
                        JsonUtils.getAsStringOrEmpty(inv.arguments(), ClassifyTurnFunction.PARAM_TOPIC));
                if (selectedTopic != null && !classified.has(SystemFunctionResultFields.ERROR)) {
                    // Freeze the classified topic on this thought before its handler detaches. The explicit state
                    // write keeps the injected dependency authoritative even though the legacy function handle
                    // also updates the runtime facade in production.
                    dependencies.state().setGlobalTopic(selectedTopic);
                    turnTopic = selectedTopic;
                }
                CompanionDiagnostics.debug(trace(), "classify",
                        "topic=" + memoryTopic() + " importance=" + turnImportance + " question=" + turnIsQuestion
                                + (turnCanonicalFact.isBlank() ? "" : " fact=\"" + CompanionDiagnostics.truncate(turnCanonicalFact) + "\""));
                break;
            }
        }
        return preExecuted;
    }

    /**
     * Normalizes classify_turn's canonical_fact to a real fact or empty. Small local models echo the literal
     * placeholder {@code ""} (or wrap the fact in quotes) when they mean "no fact"; any quote-only or
     * whitespace-only value carries no fact, so it must never reach memory as a stored fact / recall line.
     */
    private static String cleanCanonicalFact(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.strip();
        while (s.length() >= 2 && isQuote(s.charAt(0)) && isQuote(s.charAt(s.length() - 1))) {
            s = s.substring(1, s.length() - 1).strip();
        }
        return s.chars().anyMatch(Character::isLetterOrDigit) ? s : "";
    }

    private static boolean isQuote(char c) {
        return c == '"' || c == '\'' || c == '«' || c == '»';
    }

    /**
     * Settles the round in LLM order. Companion system functions execute immediately on the ordered cognitive
     * lane; game handlers are submitted and returned as detached lifecycle futures, so they cannot block the next
     * commander turn. Their callbacks retain this thought's frozen topic and are suppressed after interruption.
     */
    private CompletableFuture<Void> executeRound(List<LlmToolDefinition> tools,
                                                 List<LlmToolInvocation> invocations,
                                                 Map<LlmToolInvocation, JsonObject> preExecuted) {
        boolean suppressSpeak = shouldSuppressSpeak(invocations);
        List<CompletableFuture<Void>> detached = new ArrayList<>();
        for (LlmToolInvocation inv : invocations) {
            if (SpeakFunction.ID.equals(inv.name())) {
                if (suppressSpeak) {
                    // A game action already owns the spoken outcome this turn, so the LLM's speak fires no TTS -
                    // nothing is said, nothing is recorded.
                    CompanionDiagnostics.debug(trace(), "settle", "speak withheld (game action owns outcome)");
                    continue;
                }
                // The companion's own voice (pure conversation / memory recall): vocalize and record the words
                // said as a COMPANION entry, so a later turn knows it already answered.
                CompanionDiagnostics.info(trace(), "settle", "speak \"" + CompanionDiagnostics.truncate(spokenTextOf(inv)) + "\"");
                execute(inv);
                recordCompanionSpeech(spokenTextOf(inv));
                continue;
            }
            IntelActionType settledType = dependencies.actionTypeResolver().resolve(inv.name());
            if (settledType.isGameAction()) {
                CompanionDiagnostics.info(trace(), "settle", settledType + " " + inv.name());
                detached.add(dispatchGameCall(inv, tools, settledType));
                continue;
            }
            settleGameCall(inv, tools, preExecuted);
        }
        // No game action owned the outcome and no non-blank speak was voiced (bare classify_turn, or an empty
        // speak): the turn drew no reply - record the omitted (assistant-side) reply so the turn stays a clean
        // user->assistant pair and keeps a distinct boundary here.
        if (!suppressSpeak && !spokeToCommander(invocations)) {
            CompanionDiagnostics.debug(trace(), "settle", "no reply");
            recordTurnBoundary(TurnBoundaryMarkers.NO_ANSWER);
        }
        return detached.isEmpty()
                ? CompletableFuture.completedFuture(null)
                : CompletableFuture.allOf(detached.toArray(CompletableFuture[]::new));
    }

    /** Dispatches one game handler and owns its late result without retaining the commander cognitive worker. */
    private CompletableFuture<Void> dispatchGameCall(LlmToolInvocation inv, List<LlmToolDefinition> tools,
                                                     IntelActionType settledType) {
        if (settledType == IntelActionType.COMMAND) {
            String acknowledgement = StringUtls.affirmative();
            voice(acknowledgement, false);
            // The acknowledgement means the order was accepted, not that the handler already succeeded. Recording
            // it now closes this user turn before the next commander input is classified.
            recordCompanionSpeech(acknowledgement);
        }

        String toolCallId = gameToolCallId(inv);
        CompletableFuture<JsonObject> execution = submitExecution(inv);
        boolean pendingQueryBoundary = settledType == IntelActionType.QUERY && !execution.isDone();
        if (pendingQueryBoundary || settledType == IntelActionType.MACRO) {
            // A detached query/macro has no immediate companion line. Close its user turn before the next
            // cognitive turn; a query's linked CALL/RESULT pair is appended together when the result arrives.
            recordTurnBoundary(TurnBoundaryMarkers.PROCESSING);
        }
        inFlight = execution;
        if (isStopped()) {
            execution.cancel(true);
        }
        return execution.handle((result, failure) -> {
            try {
                if (isStopped() || execution.isCancelled()) {
                    CompanionDiagnostics.debug(trace(), "settle", inv.name() + " late result discarded");
                    return null;
                }
                JsonObject settled = result;
                if (failure != null) {
                    CompanionDiagnostics.debug(trace(), "exec", inv.name() + " failed: "
                            + CompanionDiagnostics.truncate(String.valueOf(failure.getMessage())));
                    settled = executionError(inv.name(), failure);
                } else if (settled == null) {
                    settled = new JsonObject();
                }
                if (settledType == IntelActionType.QUERY) {
                    boolean answered = publishCompletedQuery(inv, settled, toolCallId);
                    if (!answered && !pendingQueryBoundary) {
                        recordTurnBoundary(TurnBoundaryMarkers.NO_ANSWER);
                    }
                } else {
                    recordOutcome(inv, settled, tools, toolCallId);
                }
                return null;
            } finally {
                if (inFlight == execution) {
                    inFlight = null;
                }
            }
        });
    }

    /** Whether the round emitted a non-blank {@code speak} - i.e. the companion actually said something this turn. */
    private static boolean spokeToCommander(List<LlmToolInvocation> invocations) {
        return invocations.stream()
                .anyMatch(inv -> SpeakFunction.ID.equals(inv.name()) && !spokenTextOf(inv).isBlank());
    }

    /**
     * Settles one game tool-call: record the model's call (for pair replay), run it (reusing a pre-executed
     * result when present), then record its outcome - all under one tool-call id linking the call to its result.
     * A system function (classify_turn) gets no id and no CALL entry. Shared by the normal round and the
     * dangerous-confirmation round so the recording sequence lives in one place.
     * <p>
     * Returns the raw handle result so the caller can tell whether the command produced a spoken outcome (a blank
     * one means the turn's reply falls back to the immediate acknowledgement).
     */
    private JsonObject settleGameCall(LlmToolInvocation inv, List<LlmToolDefinition> tools,
                                Map<LlmToolInvocation, JsonObject> preExecuted) {
        String toolCallId = gameToolCallId(inv);
        if (toolCallId != null) {
            recordCall(toolCallId, inv);
        }
        JsonObject result = preExecuted.containsKey(inv) ? preExecuted.get(inv) : execute(inv);
        recordOutcome(inv, result, tools, toolCallId);
        return result;
    }

    /**
     * A fresh tool-call id for a QUERY, so its recorded call pairs with its answer on replay; {@code null} for a
     * COMMAND / custom command and for a system function (classify_turn). A command turn is a side effect, not
     * dialogue - its call echo is not recorded - so it gets no id; with none, any handler-voiced outcome is
     * remembered as a plain companion line rather than a linked tool result, leaving nothing orphaned in the
     * replayed timeline.
     */
    private String gameToolCallId(LlmToolInvocation inv) {
        return dependencies.actionTypeResolver().resolve(inv.name()) == IntelActionType.QUERY ? newId() : null;
    }

    /**
     * Decides whether the round's {@code speak} should be withheld. A command, query or macro owns its spoken
     * outcome deterministically (the handler's text, an ack, or its own steps), so once any of them has run
     * this turn the LLM's own {@code speak} is dropped. A turn that ran no game action (only system functions
     * or pure conversation) still speaks.
     */
    private boolean shouldSuppressSpeak(List<LlmToolInvocation> invocations) {
        for (LlmToolInvocation inv : invocations) {
            if (!SpeakFunction.ID.equals(inv.name())
                    && dependencies.actionTypeResolver().resolve(inv.name()).isGameAction()) {
                turnRanGameAction = true;
            }
        }
        return turnRanGameAction;
    }

    /** COMMANDER-only immediate acknowledgement before an LLM-selected command starts executing. */
    private boolean isCommand(LlmToolInvocation inv) {
        return dependencies.actionTypeResolver().resolve(inv.name()) == IntelActionType.COMMAND;
    }

    // recordOutcome / recordCall / recordToolResult / voice now live on the base Thought - shared with the
    // deterministic ReflexThought, which runs the same per-type outcome handling without an LLM round.

    /** The tool-calls in the validated set that require dangerous-action confirmation, in LLM order (empty when none) (§2.13). */
    private List<LlmToolInvocation> dangerousActions(List<LlmToolInvocation> invocations) {
        return invocations.stream()
                .filter(inv -> dependencies.dangerousActionPolicy().isDangerous(inv))
                .toList();
    }

    /**
     * Freezes the validated tool-call set and waits for the commander's confirmation (§2.13/§5.3). The model
     * is never told an action is dangerous: the thought detects it from the danger policy after the response
     * and voices a fixed, localized confirmation prompt itself (no LLM), recorded as the companion's own
     * words. On confirm the whole set runs in LLM order; on cancel/timeout it is discarded. The outcome is
     * recorded and the turn ends (terminal).
     */
    private void handleDangerousConfirmation(List<LlmToolDefinition> tools, List<LlmToolInvocation> invocations,
                                             Map<LlmToolInvocation, JsonObject> preExecuted, List<LlmToolInvocation> dangerous) {
        if (!isRuntimeActive()) {
            return;
        }
        CompanionDiagnostics.info(trace(), "confirm", "dangerous action detected: " + CompanionDiagnostics.calls(dangerous));
        writeMemory(new MemoryEntry(Instant.now(), memoryTopic(), MemorySource.SYSTEM,
                "dangerous action requires confirmation"));

        // Code-voiced confirmation prompt (no LLM), recorded as the companion's own COMPANION line; urgent so
        // it preempts, mirroring how the confirmation question reaches the commander before anything runs.
        String prompt = confirmDangerousActionPhrase();
        voice(prompt, true);
        recordCompanionSpeech(prompt);

        MemoryProcessingState outcome = awaitConfirmationOutcome();
        if (!isRuntimeActive()) {
            return;
        }
        CompanionDiagnostics.info(trace(), "confirm", "outcome=" + outcome.name().toLowerCase(Locale.ROOT));
        if (outcome == MemoryProcessingState.CONFIRMED) {
            // The commander confirmed: record that as a distinct user turn (a <confirmed/> marker) so the executed
            // outcome pairs with it as its own exchange, rather than trailing the confirmation prompt as a second
            // assistant line for the same turn. Stamped LOW (bookkeeping, never a durable fact or recall candidate).
            writeMemory(new MemoryEntry(Instant.now(), memoryTopic(), MemorySource.COMMANDER,
                    TurnBoundaryMarkers.CONFIRMED, MemoryImportance.LOW));
            // Execute the frozen set in LLM order. Each call is recorded then voiced and remembered by its
            // action type, exactly like a normal turn (§settleGameCall / §recordOutcome).
            for (LlmToolInvocation inv : invocations) {
                settleGameCall(inv, tools, preExecuted);
            }
        }
        writeMemory(new MemoryEntry(Instant.now(), memoryTopic(), MemorySource.SYSTEM,
                "dangerous action " + outcome.name().toLowerCase(Locale.ROOT)));
    }

    /** Blocks on the confirmation coordinator; maps confirm/cancel/timeout/overlap to a memory outcome. */
    private MemoryProcessingState awaitConfirmationOutcome() {
        ConfirmationCoordinator coordinator = dependencies.confirmationCoordinator();
        CompletableFuture<Boolean> wait = coordinator.open();
        if (wait == null) {
            return MemoryProcessingState.CANCELLED; // an overlapping confirmation is already pending (§1.6.25)
        }
        inFlight = wait;
        if (isStopped()) {
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

    /**
     * Handles an unrecoverable LLM response (§2.9): records the still-unwritten input as unresolved and speaks
     * a fixed service phrase (no LLM). The turn ends.
     */
    private void onInvalidResponse(boolean inputRecorded) {
        if (!isRuntimeActive()) {
            return;
        }
        CompanionDiagnostics.info(trace(), "settle", "cannot execute (unrecoverable LLM response)");
        if (!inputRecorded) {
            writeMemory(new MemoryEntry(Instant.now(), ConversationTopic.UNRESOLVED_COMMANDER_INPUT,
                    MemorySource.COMMANDER, context.currentInput()));
        }
        dependencies.speechGateway().submit(new SpeechRequest(newId(), cannotExecutePhrase(), urgency()));
    }

    /**
     * Safe-flush on interrupt (§2.7): never leave a memory hole. If the input was not yet recorded, write it
     * under the unresolved-commander-input fallback as INTERRUPTED; tool results are written as they execute,
     * so nothing is batched to flush. If the input WAS already recorded (the turn was cut off after filing it
     * but before replying), drop a {@link TurnBoundaryMarkers#INTERRUPTED} boundary marker so the interrupted turn
     * is not left adjacent to the next commander turn and coalesced with it. No new LLM/query/action/speech is
     * started here.
     */
    private void safeFlush(boolean inputRecorded) {
        if (!isRuntimeActive()) {
            return;
        }
        CompanionDiagnostics.debug(trace(), "flush",
                inputRecorded ? "cut off after filing input" : "interrupted before filing (input saved as unresolved)");
        if (!inputRecorded) {
            writeMemory(new MemoryEntry(Instant.now(), ConversationTopic.UNRESOLVED_COMMANDER_INPUT,
                    MemorySource.COMMANDER, context.currentInput()));
        } else {
            recordTurnBoundary(TurnBoundaryMarkers.INTERRUPTED);
        }
    }

    /** The fixed, code-generated "cannot execute" phrase in the commander's language (no LLM). */
    private static String cannotExecutePhrase() {
        Language language = AiResponseLanguagePolicy.resolveEffectiveAiResponseLanguage(SystemSession.getInstance());
        return LlmTextProvider.getText(language, CANNOT_EXECUTE_KEY);
    }

    /** The fixed, code-generated dangerous-action confirmation prompt in the commander's language (no LLM). */
    private static String confirmDangerousActionPhrase() {
        Language language = AiResponseLanguagePolicy.resolveEffectiveAiResponseLanguage(SystemSession.getInstance());
        return LlmTextProvider.getText(language, CONFIRM_DANGEROUS_KEY);
    }
}
