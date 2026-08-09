package elite.intel.ai.brain.vega.mind;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.commons.AiResponseLanguagePolicy;
import elite.intel.ai.brain.i18n.ResponseTextProvider;
import elite.intel.ai.brain.i18n.TrailingStringAliasMatcher;
import elite.intel.ai.brain.vega.CompanionConfig;
import elite.intel.ai.brain.vega.clarify.PendingClarification;
import elite.intel.ai.brain.vega.confirm.ConfirmationCoordinator;
import elite.intel.ai.brain.vega.diag.CompanionDiagnostics;
import elite.intel.ai.brain.vega.memory.facts.MemoryFactContext;
import elite.intel.ai.brain.vega.memory.facts.MergedFactCandidates;
import elite.intel.ai.brain.vega.model.llm.*;
import elite.intel.ai.brain.vega.model.speech.SpeechRequest;
import elite.intel.ai.brain.vega.prompt.ComposedPrompt;
import elite.intel.ai.brain.vega.prompt.Fact;
import elite.intel.ai.brain.vega.tools.IntelActionTypeResolver.IntelActionType;
import elite.intel.ai.brain.vega.tools.RequestInputFunction;
import elite.intel.ai.brain.vega.tools.SpeakFunction;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.util.StringUtls;
import elite.intel.util.json.JsonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.*;

/**
 * A commander tool-calling turn: compose one prompt, receive the function calls it selected, and settle them in
 * model order. Conversation and query outcomes publish only their complete record; commands and clarification
 * remain operational state rather than dialogue history. A command may own a non-dialogue side effect such as
 * writing a SAVED_TEXT record. A game handler may finish after later cognitive turns, but its result remains
 * owned by this thought.
 * <p>
 * It has the full commander tool set and detaches commands/queries from the ordered cognitive worker. One
 * utterance may carry more than one request ("check the loadout, what is our cargo capacity"), so the turn
 * settles up to {@link CompanionConfig#maxCommanderToolCalls()} calls, each under its own type's outcome policy,
 * strictly one after another: a batch is several answers to one utterance, never several things happening at
 * once. Two kinds of call are never batched - {@code request_input} suspends the turn until the commander
 * answers, and a dangerous action gates on confirmation - so either of those reduces the response to itself.
 * {@link #settleableCalls} is the single owner of that reduction.
 */
public final class CommanderThought extends Thought {

    private static final Logger log = LogManager.getLogger(CommanderThought.class);

    /** How long a dangerous action waits for the commander's confirmation before discard. */
    private static final long CONFIRMATION_TIMEOUT_SECONDS = 30;
    /** llm.properties key for the fixed, code-voiced dangerous-action confirmation prompt. */
    private static final String CONFIRM_DANGEROUS_KEY = "handler.common.confirmDangerousAction";
    /**
     * Joins the answers of a batch into the turn's one companion reply.
     */
    private static final String ANSWER_SEPARATOR = "\n";

    private enum ConfirmationOutcome { CONFIRMED, CANCELLED, TIMED_OUT, INTERRUPTED}

    /**
     * This turn's query answers in the order they were voiced, joined into its single completed record.
     */
    private final List<String> queryAnswers = Collections.synchronizedList(new ArrayList<>());
    /**
     * Whether this turn has already voiced its one command acknowledgement.
     */
    private volatile boolean acknowledged;

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
     * <p>
     * The turn's completed query record is published here rather than inside any one settlement path, because
     * every path ends in this future: a batch, a single call, and a confirmed dangerous action alike. Which of
     * them can produce a query answer is the danger policy's business, not this method's.
     */
    @Override
    CompletableFuture<Void> startLifecycle() {
        if (isStopped()) {
            return CompletableFuture.completedFuture(null);
        }
        try {
            return beginTurn().whenComplete((ignored, failure) -> publishTurnAnswers());
        } catch (Throwable failure) {
            return CompletableFuture.failedFuture(failure);
        }
    }

    private CompletableFuture<Void> beginTurn() {
        try {
            ComposedPrompt prompt = composeInitialPrompt();
            List<LlmMessage> messages = prompt.messages();
            List<LlmToolDefinition> tools = prompt.tools();
            PromptCacheProfile profile = prompt.profile();

            // Single-round by design: one LLM call selects the settling function(s). Explicit memory_search is an
            // existing terminal game query; durable memory is never injected before this call.
            if (isStopped()) {
                discardIncompleteTurn();
                return CompletableFuture.completedFuture(null);
            }
            LlmResult result = submitRound(messages, tools, profile);
            if (isStopped()) {
                discardIncompleteTurn(); // interrupt takes precedence over an invalid/cancelled result
                return CompletableFuture.completedFuture(null);
            }
            if (result == null || !result.isValid()) {
                onInvalidResponse();
                return CompletableFuture.completedFuture(null);
            }

            List<LlmToolInvocation> calls = settleableCalls(result.toolInvocations());

            // The input remains only in ThoughtContext until settlement. Pure LLM speech commits a complete
            // commander->companion pair; QUERY files its completed question/answer pair; every action/service-only
            // outcome leaves conversational memory untouched.
            if (isStopped()) {
                discardIncompleteTurn();
                return CompletableFuture.completedFuture(null);
            }

            // A dangerous action is held until the commander confirms it. settleableCalls has already made such a
            // call the turn's only one, so the confirmation still blocks nothing but this lane.
            LlmToolInvocation first = calls.get(0);
            if (dependencies.dangerousActionPolicy().isDangerous(first)) {
                handleDangerousConfirmation(first);
                return CompletableFuture.completedFuture(null); // a dangerous turn is terminal
            }

            // Execute the validated settling calls, one after another, and end.
            return executeBatch(tools, calls);
        } catch (RuntimeException unexpected) {
            // An unexpected failure (e.g. during prompt assembly) is diagnosed and voiced, but is not dialogue.
            onInvalidResponse();
            throw unexpected;
        }
    }

    // Game-tool categories are the access policy's default for COMMANDER (QUERY/ACTION/MACRO); inherited.

    @Override
    protected List<LlmToolDefinition> systemTools(List<LlmToolDefinition> gameTools) {
        return dependencies.systemFunctionProvider().systemFunctions(source(), gameTools);
    }

    /**
     * One utterance can hold more than one request, so a commander turn settles a bounded batch.
     */
    @Override
    protected int maxToolCallsPerRound() {
        return CompanionConfig.maxCommanderToolCalls();
    }

    /**
     * Reduces the model's calls to the ones this turn will actually settle. A single call is always kept as it
     * is; the rules below exist only because a response with several calls can combine things that do not
     * combine.
     * <ul>
     *   <li>A gating call - {@code request_input}, which suspends the turn until the commander answers, or a
     *       dangerous action, which must not run before confirmation - reduces the response to itself. Whatever
     *       the model paired it with belongs to a turn that can complete, and running calls around a suspension
     *       would settle them against state the commander has not agreed to yet.</li>
     *   <li>{@code speak} alongside a game call is dropped: the game call's outcome is the answer, and the
     *       prompt already says speaking is the fallback for when no function fits, not a preface to one.</li>
     *   <li>Nothing but {@code speak} keeps the first call, since the turn has one conversational reply.</li>
     * </ul>
     */
    private List<LlmToolInvocation> settleableCalls(List<LlmToolInvocation> calls) {
        if (calls.size() <= 1) {
            return calls;
        }
        Optional<LlmToolInvocation> gating = calls.stream().filter(this::isGating).findFirst();
        if (gating.isPresent()) {
            CompanionDiagnostics.debug(trace(), "settle", "batch of " + calls.size()
                    + " reduced to gating call " + gating.get().name());
            return List.of(gating.get());
        }
        List<LlmToolInvocation> gameCalls = calls.stream()
                .filter(call -> dependencies.actionTypeResolver().resolve(call.name()).isGameAction())
                .toList();
        if (gameCalls.isEmpty()) {
            CompanionDiagnostics.debug(trace(), "settle",
                    "batch of " + calls.size() + " reduced to one reply " + calls.get(0).name());
            return List.of(calls.get(0));
        }
        if (gameCalls.size() < calls.size()) {
            CompanionDiagnostics.debug(trace(), "settle", "dropped "
                    + (calls.size() - gameCalls.size()) + " non-action call(s) alongside "
                    + CompanionDiagnostics.calls(gameCalls));
        }
        return gameCalls;
    }

    /**
     * Whether this call must own the whole turn: it suspends the turn or waits on the commander's confirmation.
     */
    private boolean isGating(LlmToolInvocation call) {
        return RequestInputFunction.ID.equals(call.name())
                || dependencies.dangerousActionPolicy().isDangerous(call);
    }

    /**
     * Settles the calls strictly in model order, each starting only once the one before it has finished, so a
     * commander who asked for two things hears the answers in the order they were asked and never has two
     * handlers running against the same game state. A call that fails does not cancel the ones behind it: the
     * batch is several independent requests, not one transaction. The returned future completes when the last
     * one settles, which is what keeps the whole batch inside this thought's lifecycle for cancellation and the
     * watchdog.
     */
    private CompletableFuture<Void> executeBatch(
            List<LlmToolDefinition> tools,
            List<LlmToolInvocation> calls
    ) {
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (LlmToolInvocation call : calls) {
            chain = chain.thenCompose(ignored -> isStopped()
                    ? CompletableFuture.completedFuture(null)
                    : executeRound(tools, call));
            chain = chain.exceptionally(failure -> {
                reportUnsettledCall(call, failure);
                return null;
            });
        }
        return chain;
    }

    /**
     * Reports a call whose settlement threw. Execution failures are already turned into structured results by
     * {@link #dispatchGameCall}, so anything arriving here is cancellation or a defect in settlement itself, and
     * a defect that only ever reached the UI log surface would be invisible in the log file. The cause is
     * unwrapped because the value is normally a {@link CompletionException} whose own message is just the
     * cause's {@code toString()}.
     */
    private void reportUnsettledCall(LlmToolInvocation call, Throwable failure) {
        Throwable cause = failure.getCause() != null ? failure.getCause() : failure;
        CompanionDiagnostics.debug(trace(), "settle",
                call.name() + " did not settle: " + CompanionDiagnostics.truncate(String.valueOf(cause.getMessage())));
        if (cause instanceof CancellationException) {
            return; // an interrupted turn cancels its own work; that is not a failure to report
        }
        log.warn("Settling {} threw; the remaining calls of this turn continue", call.name(), cause);
    }

    /**
     * Publishes this turn's query answers as its one completed record. A record per call would spell one
     * utterance out as several identical questions in the history the next turn reads, so the pair is the
     * commander's input against the answers in the order they were voiced.
     */
    private void publishTurnAnswers() {
        List<String> answers;
        synchronized (queryAnswers) {
            answers = List.copyOf(queryAnswers);
        }
        if (answers.isEmpty()) {
            return;
        }
        recordQueryAnswerWithoutVoicing(String.join(ANSWER_SEPARATOR, answers));
    }

    /**
     * Voices each answer the moment its handler finishes, so the commander hears them as they arrive, and holds
     * it for the turn's single record, which {@link #publishTurnAnswers} writes once the turn ends.
     */
    @Override
    protected void publishQueryAnswer(String answer) {
        voice(answer, false);
        queryAnswers.add(answer);
    }

    /** Host-provided live facts for this commander input, appended to the system prompt as {@code <facts>}. */
    @Override
    protected List<Fact> factCandidates() {
        return MergedFactCandidates.forInput(new MemoryFactContext(context.matchInput(), source(), urgency()));
    }

    /** Settles the one validated call; game handlers return a detached lifecycle future. */
    private CompletableFuture<Void> executeRound(
            List<LlmToolDefinition> tools,
            LlmToolInvocation invocation
    ) {
        if (RequestInputFunction.ID.equals(invocation.name())) {
            Optional<LlmToolInvocation> alreadySpoken = recoverSpokenArgument(invocation, tools);
            if (alreadySpoken.isPresent()) {
                return dispatchRecovered(alreadySpoken.get(), "spoken argument recovered");
            }
            Optional<LlmToolInvocation> nothingMissing = recoverParameterlessAction(invocation, tools);
            if (nothingMissing.isPresent()) {
                return dispatchRecovered(nothingMissing.get(), "no parameter to request");
            }
            handleInputRequest(invocation, tools);
            return CompletableFuture.completedFuture(null);
        }
        if (SpeakFunction.ID.equals(invocation.name())) {
            String reply = spokenTextOf(invocation);
            if (reply.isBlank()) {
                CompanionDiagnostics.debug(trace(), "settle", "no reply");
                return CompletableFuture.completedFuture(null);
            }
            CompanionDiagnostics.info(trace(), "settle",
                    "speak \"" + CompanionDiagnostics.truncate(reply) + "\"");
            execute(invocation);
            recordDialoguePair(reply);
            return CompletableFuture.completedFuture(null);
        }
        IntelActionType settledType = dependencies.actionTypeResolver().resolve(invocation.name());
        if (settledType.isGameAction()) {
            CompanionDiagnostics.info(trace(), "settle", settledType + " " + invocation.name());
            return dispatchGameCall(invocation, settledType);
        }
        settleGameCall(invocation);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Recovers an argument the commander already spoke, so a model that asks for it anyway does not strand the
     * turn ("find market where I can buy tritium" answered with "what commodity do you want to find?"). It reuses
     * the reflex gate's deterministic extraction: the utterance must begin with one of the target action's own
     * localized triggers whose final placeholder is that very parameter, which is what makes the trailing words
     * its value rather than a guess.
     * <p>
     * Deliberately narrow. It never fires while a clarification is pending (there the answer is the current
     * utterance, which the model already owns), never when the action needs a second required argument the
     * trigger cannot supply, and never for a dangerous action, which keeps its confirmation flow.
     */
    private Optional<LlmToolInvocation> recoverSpokenArgument(LlmToolInvocation request,
                                                              List<LlmToolDefinition> tools) {
        if (context.pendingClarification() != null) {
            return Optional.empty();
        }
        String actionId = JsonUtils.getAsStringOrEmpty(request.arguments(), RequestInputFunction.PARAM_ACTION_ID);
        String parameterName = JsonUtils.getAsStringOrEmpty(
                request.arguments(), RequestInputFunction.PARAM_PARAMETER_NAME);
        Optional<LlmToolDefinition> target = tools.stream()
                .filter(tool -> actionId.equals(tool.name()))
                .filter(tool -> dependencies.actionTypeResolver().resolve(tool.name()).isGameAction())
                .findFirst();
        if (target.isEmpty() || parameterName.isBlank() || !onlyRequiredParameter(target.get(), parameterName)) {
            return Optional.empty();
        }
        Optional<String> spokenValue = TrailingStringAliasMatcher.findBestMatch(
                        target.get().localizedTrainingPhrases(), target.get().parameters(), context.matchInput())
                .filter(match -> parameterName.equals(match.parameterName()))
                .map(TrailingStringAliasMatcher.Match::value)
                .filter(value -> !value.isBlank());
        if (spokenValue.isEmpty()) {
            return Optional.empty();
        }
        JsonObject arguments = new JsonObject();
        arguments.addProperty(parameterName, spokenValue.get());
        LlmToolInvocation recovered = new LlmToolInvocation(request.id(), actionId, arguments);
        return dependencies.dangerousActionPolicy().isDangerous(recovered)
                ? Optional.empty()
                : Optional.of(recovered);
    }

    /**
     * Recovers the action itself when the model asks for input on a target that declares no required parameter.
     * Nothing can be missing there, so the request is provably wrong, and without this the order comes back as a
     * question ("enter next fleet carrier destination" answered with "which system?" for an action that reads the
     * next leg from the stored carrier route).
     * <p>
     * Shares {@link #recoverSpokenArgument}'s guards: never while a clarification is pending, where the turn's
     * meaning belongs to the pending action, and never for a dangerous action, which keeps its confirmation flow.
     */
    private Optional<LlmToolInvocation> recoverParameterlessAction(LlmToolInvocation request,
                                                                   List<LlmToolDefinition> tools) {
        if (context.pendingClarification() != null) {
            return Optional.empty();
        }
        String actionId = JsonUtils.getAsStringOrEmpty(request.arguments(), RequestInputFunction.PARAM_ACTION_ID);
        boolean nothingToRequest = tools.stream()
                .filter(tool -> actionId.equals(tool.name()))
                .filter(tool -> dependencies.actionTypeResolver().resolve(tool.name()).isGameAction())
                .anyMatch(tool -> tool.parameters().stream().noneMatch(ActionParameterSpec::isRequired));
        if (!nothingToRequest) {
            return Optional.empty();
        }
        LlmToolInvocation recovered = new LlmToolInvocation(request.id(), actionId, new JsonObject());
        return dependencies.dangerousActionPolicy().isDangerous(recovered)
                ? Optional.empty()
                : Optional.of(recovered);
    }

    /**
     * Dispatches an action recovered from a misdirected {@code request_input}, recording why it was recovered.
     */
    private CompletableFuture<Void> dispatchRecovered(LlmToolInvocation recovered, String reason) {
        IntelActionType recoveredType = dependencies.actionTypeResolver().resolve(recovered.name());
        CompanionDiagnostics.info(trace(), "settle",
                reason + " -> " + recoveredType + " " + recovered.name());
        return dispatchGameCall(recovered, recoveredType);
    }

    /**
     * True when the named parameter is the only argument the action cannot run without.
     */
    private static boolean onlyRequiredParameter(LlmToolDefinition tool, String parameterName) {
        return tool.parameters().stream()
                .filter(ActionParameterSpec::isRequired)
                .allMatch(parameter -> parameterName.equals(parameter.getName()));
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
    private CompletableFuture<Void> dispatchGameCall(LlmToolInvocation inv, IntelActionType settledType) {
        if (settledType == IntelActionType.COMMAND && !acknowledged) {
            acknowledged = true;
            String acknowledgement = StringUtls.affirmative();
            voice(acknowledgement, false);
            // The acknowledgement means only that execution was accepted. It is code-generated action feedback,
            // not an LLM dialogue reply, so it never enters conversational memory. One turn acknowledges once,
            // however many commands it carries - the commander gave one order, and each command still speaks its
            // own outcome when it finishes.
        }

        CompletableFuture<JsonObject> execution = submitExecution(inv);
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
                settleToolOutcome(inv, settled);
                return null;
            } finally {
                if (inFlight == execution) {
                    inFlight = null;
                }
            }
        });
    }

    /**
     * Settles one tool call and applies its type-specific outcome policy. QUERY publishes a completed record;
     * commands, macros and ordinary system functions do not write conversation memory. Shared with dangerous
     * confirmation so execution sequencing has one owner.
     */
    private void settleGameCall(LlmToolInvocation inv) {
        JsonObject result = execute(inv);
        settleToolOutcome(inv, result);
    }

    // Tool-outcome settlement lives on Thought so deterministic reflexes follow the same memory rules.

    /**
     * Holds the validated tool call and waits for the commander's confirmation. The model
     * is never told an action is dangerous: the thought detects it from the danger policy after the response
     * and voices a fixed, localized confirmation prompt itself (no LLM). On confirm the call runs; on
     * cancel/timeout it is discarded. Confirmation and execution remain runtime/diagnostic state and
     * contribute no conversational memory.
     */
    private void handleDangerousConfirmation(LlmToolInvocation invocation) {
        if (!isRuntimeActive()) {
            return;
        }
        CompanionDiagnostics.info(trace(), "confirm", "dangerous action detected: " + invocation.name());

        // Code-voiced confirmation prompt (no LLM); urgent so it preempts before anything runs.
        String prompt = confirmDangerousActionPhrase();
        voice(prompt, true);

        ConfirmationOutcome outcome = awaitConfirmationOutcome();
        if (!isRuntimeActive()) {
            return;
        }
        CompanionDiagnostics.info(trace(), "confirm", "outcome=" + outcome.name().toLowerCase(Locale.ROOT));
        if (outcome == ConfirmationOutcome.CONFIRMED) {
            settleGameCall(invocation);
        }
    }

    /** Blocks on the confirmation coordinator; maps confirm/cancel/timeout/overlap to its typed runtime outcome. */
    private ConfirmationOutcome awaitConfirmationOutcome() {
        ConfirmationCoordinator coordinator = dependencies.confirmationCoordinator();
        CompletableFuture<Boolean> wait = coordinator.open();
        if (wait == null) {
            return ConfirmationOutcome.CANCELLED; // an overlapping confirmation is already pending (§1.6.25)
        }
        inFlight = wait;
        if (isStopped()) {
            wait.cancel(true);
        }
        try {
            return wait.get(CONFIRMATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    ? ConfirmationOutcome.CONFIRMED
                    : ConfirmationOutcome.CANCELLED;
        } catch (TimeoutException timedOut) {
            return ConfirmationOutcome.TIMED_OUT;
        } catch (CancellationException interruptedWait) {
            return ConfirmationOutcome.INTERRUPTED;
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            return ConfirmationOutcome.INTERRUPTED;
        } catch (ExecutionException failed) {
            return ConfirmationOutcome.CANCELLED;
        } finally {
            inFlight = null;
            coordinator.close(wait);
        }
    }

    /**
     * Handles an unrecoverable LLM response: speaks a fixed service phrase (no LLM) and leaves
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
     * Interrupt discard: an incomplete commander turn has no LLM reply, so there is deliberately no memory
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
        return ResponseTextProvider.getText(language, CONFIRM_DANGEROUS_KEY);
    }

}
