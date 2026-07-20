package elite.intel.ai.brain.vega.mind;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.ai.brain.commons.AiResponseLanguagePolicy;
import elite.intel.ai.brain.i18n.LlmTextProvider;
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

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.*;

/**
 * A commander tool-calling turn: compose one prompt, receive exactly one function call, and settle it. Conversation,
 * query outcomes publish only their complete record; commands and clarification remain operational state rather
 * than dialogue history. A command may own a non-dialogue side effect such as writing a SAVED_TEXT record. A
 * game handler may finish after later cognitive turns, but its result remains owned by this thought.
 * <p>
 * It has the full commander tool set and detaches commands/queries from the ordered cognitive worker. The single
 * selected call owns the turn: a game handler owns its outcome, while {@code speak} owns a conversational reply.
 */
public final class CommanderThought extends Thought {

    /** How long a dangerous action waits for the commander's confirmation before discard. */
    private static final long CONFIRMATION_TIMEOUT_SECONDS = 30;
    /** llm.properties key for the fixed, code-voiced dangerous-action confirmation prompt. */
    private static final String CONFIRM_DANGEROUS_KEY = "handler.common.confirmDangerousAction";

    private enum ConfirmationOutcome { CONFIRMED, CANCELLED, TIMED_OUT, INTERRUPTED }

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
        try {
            ComposedPrompt prompt = composeInitialPrompt();
            List<LlmMessage> messages = prompt.messages();
            List<LlmToolDefinition> tools = prompt.tools();
            PromptCacheProfile profile = prompt.profile();

            // Single-round by design: one LLM call selects the settling function. Explicit memory_search is an
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

            LlmToolInvocation invocation = result.toolInvocations().get(0);

            // The input remains only in ThoughtContext until settlement. Pure LLM speech commits a complete
            // commander->companion pair; QUERY files its completed question/answer pair; every action/service-only
            // outcome leaves conversational memory untouched.
            if (isStopped()) {
                discardIncompleteTurn();
                return CompletableFuture.completedFuture(null);
            }

            // A dangerous action is held until the commander confirms it.
            if (dependencies.dangerousActionPolicy().isDangerous(invocation)) {
                handleDangerousConfirmation(invocation);
                return CompletableFuture.completedFuture(null); // a dangerous turn is terminal
            }

            // Execute the one validated settling call and end.
            return executeRound(tools, invocation);
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
        if (settledType == IntelActionType.COMMAND) {
            String acknowledgement = StringUtls.affirmative();
            voice(acknowledgement, false);
            // The acknowledgement means only that execution was accepted. It is code-generated action feedback,
            // not an LLM dialogue reply, so it never enters conversational memory.
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
        return LlmTextProvider.getText(language, CONFIRM_DANGEROUS_KEY);
    }

}
