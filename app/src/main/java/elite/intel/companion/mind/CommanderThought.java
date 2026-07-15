package elite.intel.companion.mind;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.commons.AiResponseLanguagePolicy;
import elite.intel.ai.brain.i18n.InputNormalizerLocalizations;
import elite.intel.ai.brain.i18n.LlmTextProvider;
import elite.intel.companion.clarify.PendingClarification;
import elite.intel.companion.confirm.ConfirmationCoordinator;
import elite.intel.companion.diag.CompanionDiagnostics;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.llm.LlmMessage;
import elite.intel.companion.model.llm.LlmResult;
import elite.intel.companion.model.llm.LlmToolDefinition;
import elite.intel.companion.model.llm.LlmToolInvocation;
import elite.intel.companion.model.llm.PromptCacheProfile;
import elite.intel.companion.model.memory.MemoryImportance;
import elite.intel.companion.model.memory.MemoryProcessingState;
import elite.intel.companion.model.speech.SpeechRequest;
import elite.intel.companion.memory.facts.MergedFactCandidates;
import elite.intel.companion.memory.facts.MemoryFactContext;
import elite.intel.companion.prompt.ComposedPrompt;
import elite.intel.companion.prompt.Fact;
import elite.intel.companion.tools.ClassifyTurnFunction;
import elite.intel.companion.tools.IntelActionTypeResolver.IntelActionType;
import elite.intel.companion.tools.RequestInputFunction;
import elite.intel.companion.tools.SpeakFunction;
import elite.intel.companion.tools.SystemFunctionResultFields;
import elite.intel.util.json.JsonUtils;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.util.StringUtls;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A thought born from a commander reply. It owns the tool-calling turn: compose -> LLM round -> apply
 * {@code classify_turn} -> freeze the turn topic -> dispatch the settling call -> commit a complete dialogue pair
 * when the LLM actually speaks. It is
 * single-round by design: memory is retrieved before the turn as inlined answer facts, so no in-turn lookup
 * round is needed. A game handler may finish after later cognitive turns, but its result remains owned by this
 * thought and its frozen topic (§2.5/§2.6/§2.8/§5.1).
 * <p>
 * It has the full commander tool set and the COMMANDER-only paths an EVENT/narration thought cannot reach:
 * applying {@code classify_turn} (topic + importance + is_question) before a dialogue pair is filed, detaching
 * commands/queries from the ordered cognitive worker, and vocalizing their outcome deterministically. Narration ownership (§2.14): a
 * command/query owns its spoken outcome - the handler's {@code text_to_speech_response} is voiced verbatim
 * and a side-effect stays silent - so once any command/query runs this turn the LLM's own {@code speak} is
 * withheld (no re-voicing or rephrasing). A turn that ran no command/query (pure conversation, memory recall)
 * still speaks.
 */
public final class CommanderThought extends Thought {

    /** How long a frozen dangerous set waits for the commander's confirmation before discard (§7.2 setting). */
    private static final long CONFIRMATION_TIMEOUT_SECONDS = 30;
    /** llm.properties key for the fixed, code-voiced dangerous-action confirmation prompt (§2.13). */
    private static final String CONFIRM_DANGEROUS_KEY = "handler.common.confirmDangerousAction";

    /**
     * Turn-scoped narration accounting. Set once any game command/query runs this turn; from then on the
     * LLM's own {@code speak} is withheld for the rest of the turn (the command/query already owns the
     * spoken outcome).
     */
    private boolean turnRanGameAction;

    /** Importance set via classify_turn (default NORMAL); used if this turn eventually publishes memory. */
    private MemoryImportance turnImportance = MemoryImportance.NORMAL;

    /** Whether classify_turn flagged this turn as a question; if recorded, it is LOW and never a fact candidate. */
    private boolean turnIsQuestion;

    /** Host-grounded HIGH fact text for this turn; empty for every other importance, questions, and game actions. */
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
        turnTopic = dependencies.state().globalTopic();
        try {
            ComposedPrompt prompt = composeInitialPrompt();
            List<LlmMessage> flow = new ArrayList<>(prompt.messages());
            List<LlmToolDefinition> tools = prompt.tools(); // immutable snapshot, reused every round
            PromptCacheProfile profile = prompt.profile();

            // Single-round by design: one LLM round settles the turn (memory is retrieved before the turn as
            // inlined answer facts, so there is no in-turn lookup round).
            if (isStopped()) {
                discardIncompleteTurn();
                return CompletableFuture.completedFuture(null);
            }
            LlmResult result = submitRound(flow, tools, profile);
            if (isStopped()) {
                discardIncompleteTurn(); // interrupt takes precedence over an invalid/cancelled result
                return CompletableFuture.completedFuture(null);
            }
            if (result == null || !result.isValid()) {
                onInvalidResponse();
                return CompletableFuture.completedFuture(null);
            }

            List<LlmToolInvocation> invocations = result.toolInvocations();

            // Classify the turn (topic + importance + is_question) before any tool runs (§2.6).
            Map<LlmToolInvocation, JsonObject> preExecuted = applyClassification(invocations);
            // The input remains only in ThoughtContext until settlement. Pure LLM speech commits a complete
            // commander->companion pair; QUERY explicitly files its structured exchange; every action/service-only
            // outcome leaves conversational memory untouched.
            if (isStopped()) {
                discardIncompleteTurn();
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
            // An unexpected failure (e.g. during prompt assembly) is diagnosed and voiced, but is not dialogue.
            onInvalidResponse();
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
    protected List<LlmToolDefinition> systemTools(List<LlmToolDefinition> gameTools) {
        return dependencies.systemFunctionProvider().systemFunctions(source(), gameTools);
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
     * before memory settlement - read its importance into the turn's importance (so any recorded dialogue/query
     * input is stamped with it), force questions to LOW, and retain {@code canonical_fact} only for a non-question HIGH
     * fact with no game action. The host also verifies that the canonical text shares a concrete token with the
     * current input; an ungrounded model copy is replaced by the verbatim current input. This prevents a canonical
     * string on a routine command, question, MAX order, or unrelated prior turn from becoming trusted memory. The
     * handle then moves the global topic. Returns the pre-executed
     * result keyed by invocation so the main loop does not run it twice. Unknown importance stays NORMAL; absent
     * {@code classify_turn} leaves the topic unchanged.
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
                String classifiedFact = cleanCanonicalFact(JsonUtils.getAsStringOrEmpty(
                        inv.arguments(), ClassifyTurnFunction.PARAM_CANONICAL_FACT));
                turnCanonicalFact = validatedCanonicalFact(classifiedFact, invocations);
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

    /** Applies the host's durable-fact contract to the model's proposed canonical text. */
    private String validatedCanonicalFact(String classifiedFact, List<LlmToolInvocation> invocations) {
        if (turnIsQuestion || turnImportance != MemoryImportance.HIGH || classifiedFact.isBlank()) {
            return "";
        }
        boolean handlesGameIntent = invocations.stream().anyMatch(inv -> RequestInputFunction.ID.equals(inv.name())
                || dependencies.actionTypeResolver().resolve(inv.name()).isGameAction());
        if (handlesGameIntent) {
            return "";
        }
        String currentInput = context.memoryInput() == null ? "" : context.memoryInput().strip();
        if (currentInput.isBlank()) {
            return "";
        }
        // A small model may copy an earlier fact while correctly marking this turn HIGH. The raw commander input is
        // the safe ground truth when that proposed restatement contains none of this turn's concrete tokens.
        return canonicalFactGroundedInInput(classifiedFact, currentInput) ? classifiedFact : currentInput;
    }

    /** Exact meaningful-token overlap: fuzzy "код"/"кодовое" must not validate a copied docking code. */
    private static boolean canonicalFactGroundedInInput(String canonicalFact, String currentInput) {
        Set<String> inputTokens = factTokens(currentInput);
        return factTokens(canonicalFact).stream().anyMatch(inputTokens::contains);
    }

    private static Set<String> factTokens(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        Set<String> stopWords = InputNormalizerLocalizations.stopWords();
        Set<String> tokens = new LinkedHashSet<>();
        for (String token : text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}_]+")) {
            if (token.length() >= 2 && !stopWords.contains(token)) {
                tokens.add(token);
            }
        }
        return Set.copyOf(tokens);
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
            if (RequestInputFunction.ID.equals(inv.name())) {
                handleInputRequest(inv, tools);
                continue;
            }
            if (SpeakFunction.ID.equals(inv.name())) {
                if (suppressSpeak) {
                    // A game action already owns the spoken outcome this turn, so the LLM's speak fires no TTS -
                    // nothing is said, nothing is recorded.
                    CompanionDiagnostics.debug(trace(), "settle", "speak withheld (game action owns outcome)");
                    continue;
                }
                // Pure conversation / memory recall: vocalize, then commit the current input and the LLM's words as
                // one complete dialogue pair. An interrupted/blank reply commits nothing.
                CompanionDiagnostics.info(trace(), "settle", "speak \"" + CompanionDiagnostics.truncate(spokenTextOf(inv)) + "\"");
                execute(inv);
                recordDialoguePair(spokenTextOf(inv));
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
        // A bare classify_turn or blank speak has no LLM reply and therefore contributes no conversational memory.
        if (!suppressSpeak && !spokeToCommander(invocations)) {
            CompanionDiagnostics.debug(trace(), "settle", "no reply");
        }
        return detached.isEmpty()
                ? CompletableFuture.completedFuture(null)
                : CompletableFuture.allOf(detached.toArray(CompletableFuture[]::new));
    }

    /**
     * Validates and opens one cross-turn clarification without keeping this thought alive. Only a game tool from
     * this exact prompt snapshot and one of its required parameters can become pending execution context.
     */
    private void handleInputRequest(LlmToolInvocation inv, List<LlmToolDefinition> tools) {
        String actionId = JsonUtils.getAsStringOrEmpty(inv.arguments(), RequestInputFunction.PARAM_ACTION_ID);
        String parameterName = JsonUtils.getAsStringOrEmpty(
                inv.arguments(), RequestInputFunction.PARAM_PARAMETER_NAME);
        String question = JsonUtils.getAsStringOrEmpty(inv.arguments(), RequestInputFunction.PARAM_QUESTION);

        Optional<LlmToolDefinition> target = tools.stream()
                .filter(tool -> actionId.equals(tool.name()))
                .filter(tool -> dependencies.actionTypeResolver().resolve(tool.name()).isGameAction())
                .findFirst();
        Optional<ActionParameterSpec> requestedParameter = target.stream()
                .flatMap(tool -> tool.parameters().stream())
                .filter(parameter -> parameterName.equals(parameter.getName()))
                .filter(ActionParameterSpec::isRequired)
                .findFirst();

        if (target.isEmpty() || requestedParameter.isEmpty() || question.isBlank()) {
            CompanionDiagnostics.debug(trace(), "clarify",
                    "rejected request_input target=" + actionId + " parameter=" + parameterName);
            String failure = executionFailurePhrase();
            voice(failure, false);
            return;
        }

        PendingClarification parent = context.pendingClarification();
        String originalInput = parent != null && actionId.equals(parent.actionId())
                ? parent.originalInput()
                : context.memoryInput();
        if (!isRuntimeActive()) {
            return;
        }
        CompanionDiagnostics.info(trace(), "settle",
                "request_input " + actionId + "." + parameterName
                        + " \"" + CompanionDiagnostics.truncate(question) + "\"");
        voice(question, false);
        if (!isRuntimeActive()) {
            return;
        }
        dependencies.clarificationCoordinator().open(actionId, parameterName, originalInput, question);
    }

    /** Dispatches one game handler and owns its late result without retaining the commander cognitive worker. */
    private CompletableFuture<Void> dispatchGameCall(LlmToolInvocation inv, List<LlmToolDefinition> tools,
                                                     IntelActionType settledType) {
        if (settledType == IntelActionType.COMMAND) {
            String acknowledgement = StringUtls.affirmative();
            voice(acknowledgement, false);
            // The acknowledgement means only that execution was accepted. It is code-generated action feedback,
            // not an LLM dialogue reply, so it never enters conversational memory.
        }

        String toolCallId = gameToolCallId(inv);
        CompletableFuture<JsonObject> execution = submitExecution(inv);
        boolean queryWasPending = settledType == IntelActionType.QUERY && !execution.isDone();
        inFlight = execution;
        if (isStopped()) {
            execution.cancel(true);
        }
        return execution.handle((result, failure) -> {
            try {
                if (isStopped()) {
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
                    publishCompletedQuery(inv, settled, toolCallId, queryWasPending);
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

    /** Whether the round emitted a non-blank direct reply ({@code speak} or an input-request question). */
    private static boolean spokeToCommander(List<LlmToolInvocation> invocations) {
        return invocations.stream()
                .anyMatch(inv -> (SpeakFunction.ID.equals(inv.name()) && !spokenTextOf(inv).isBlank())
                        || (RequestInputFunction.ID.equals(inv.name())
                        && !JsonUtils.getAsStringOrEmpty(
                        inv.arguments(), RequestInputFunction.PARAM_QUESTION).isBlank()));
    }

    /**
     * Settles one game tool-call: run it (reusing a pre-executed result when present), then apply its type-specific
     * outcome policy. QUERY publishes input/CALL/RESULT only after the result exists; commands and macros run
     * without conversational-memory writes.
     * A system function (classify_turn) gets no id and no CALL entry. Shared by the normal round and the
     * dangerous-confirmation round so execution sequencing lives in one place.
     * Returns the raw handle result after applying the type-specific speech/memory policy.
     */
    private JsonObject settleGameCall(LlmToolInvocation inv, List<LlmToolDefinition> tools,
                                Map<LlmToolInvocation, JsonObject> preExecuted) {
        String toolCallId = gameToolCallId(inv);
        JsonObject result = preExecuted.containsKey(inv) ? preExecuted.get(inv) : execute(inv);
        recordOutcome(inv, result, tools, toolCallId);
        return result;
    }

    /**
     * A fresh tool-call id for a QUERY, so its recorded call pairs with its answer on replay; {@code null} for a
     * COMMAND / custom command and for a system function (classify_turn). Actions are execution, not dialogue, so
     * neither their call echo nor their handler outcome is recorded.
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

    // recordOutcome / transactional QUERY publication / voice live on the base Thought - shared with the
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
     * and voices a fixed, localized confirmation prompt itself (no LLM). On confirm the whole set runs in LLM
     * order; on cancel/timeout it is discarded. Confirmation and execution remain runtime/diagnostic state and
     * contribute no conversational memory.
     */
    private void handleDangerousConfirmation(List<LlmToolDefinition> tools, List<LlmToolInvocation> invocations,
                                             Map<LlmToolInvocation, JsonObject> preExecuted, List<LlmToolInvocation> dangerous) {
        if (!isRuntimeActive()) {
            return;
        }
        CompanionDiagnostics.info(trace(), "confirm", "dangerous action detected: " + CompanionDiagnostics.calls(dangerous));

        // Code-voiced confirmation prompt (no LLM); urgent so it preempts before anything runs.
        String prompt = confirmDangerousActionPhrase();
        voice(prompt, true);

        MemoryProcessingState outcome = awaitConfirmationOutcome();
        if (!isRuntimeActive()) {
            return;
        }
        CompanionDiagnostics.info(trace(), "confirm", "outcome=" + outcome.name().toLowerCase(Locale.ROOT));
        if (outcome == MemoryProcessingState.CONFIRMED) {
            // Execute the frozen set in LLM order. COMMAND/MACRO outcomes are voiced without memory; SYSTEM
            // functions remain memory-silent. Dangerous QUERY is outside the supported policy surface.
            for (LlmToolInvocation inv : invocations) {
                settleGameCall(inv, tools, preExecuted);
            }
        }
    }

    /** Blocks on the confirmation coordinator; maps confirm/cancel/timeout/overlap to its typed runtime outcome. */
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
     * Handles an unrecoverable LLM response (§2.9): speaks a fixed service phrase (no LLM) and leaves
     * conversational memory untouched. The turn ends.
     */
    private void onInvalidResponse() {
        if (!isRuntimeActive()) {
            return;
        }
        CompanionDiagnostics.info(trace(), "settle", "cannot execute (unrecoverable LLM response)");
        String phrase = executionFailurePhrase();
        dependencies.speechGateway().submit(new SpeechRequest(newId(), phrase, urgency()));
    }

    /**
     * Interrupt discard (§2.7): an incomplete commander turn has no LLM reply, so there is deliberately no memory
     * publication and no boundary marker. No new LLM/query/action/speech is started here.
     */
    private void discardIncompleteTurn() {
        if (!isRuntimeActive()) {
            return;
        }
        CompanionDiagnostics.debug(trace(), "discard", "incomplete turn discarded (no dialogue pair)");
    }

    /** The fixed, code-generated dangerous-action confirmation prompt in the commander's language (no LLM). */
    private static String confirmDangerousActionPhrase() {
        Language language = AiResponseLanguagePolicy.resolveEffectiveAiResponseLanguage(SystemSession.getInstance());
        return LlmTextProvider.getText(language, CONFIRM_DANGEROUS_KEY);
    }
}
