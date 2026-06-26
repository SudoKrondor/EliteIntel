package elite.intel.companion.mind;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.commons.AiResponseLanguagePolicy;
import elite.intel.ai.brain.i18n.LlmTextProvider;
import elite.intel.companion.confirm.ConfirmationCoordinator;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.Urgency;
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
    /** llm.properties key for the fixed, code-voiced dangerous-action confirmation prompt (§2.13). */
    private static final String CONFIRM_DANGEROUS_KEY = "handler.common.confirmDangerousAction";

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
                    handleDangerousConfirmation(tools, invocations, preExecuted);
                    return; // a dangerous turn is terminal
                }

                if (executeRound(flow, tools, invocations, preExecuted)) {
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
     * Executes the round's tool-calls in LLM order, synchronously, and voices/remembers each outcome by its
     * action type via {@link #recordOutcome} (the handler owns speech, not the LLM). The result always feeds
     * the flow so the LLM can chain on it next round; {@code speak} is voiced or withheld here, and
     * {@code nothing_to_do} is the lifecycle terminator.
     * <p>
     * Synchronous on purpose (the fire-and-forget dispatch was reverted): a long command holds the lane until
     * it finishes. Decoupling a slow command's outcome from the thought is a separate, cause-level change.
     *
     * @return {@code true} if {@code nothing_to_do} ended the turn
     */
    private boolean executeRound(List<LlmMessage> flow, List<LlmToolDefinition> tools,
                                 List<LlmToolInvocation> invocations,
                                 Map<LlmToolInvocation, JsonObject> preExecuted) {
        boolean suppressSpeak = shouldSuppressSpeak(invocations);
        boolean terminate = false;
        List<LlmMessage> toolResults = new ArrayList<>();
        for (LlmToolInvocation inv : invocations) {
            if (NothingToDoFunction.ID.equals(inv.name())) {
                terminate = true;
                continue;
            }
            if (SpeakFunction.ID.equals(inv.name())) {
                if (suppressSpeak) {
                    // A game action already owns the spoken outcome this turn, so the LLM's speak fires no
                    // TTS - nothing is said, nothing is recorded. The synthetic result keeps the
                    // assistant/tool-result pairing intact and tells the LLM the narration was withheld.
                    toolResults.add(LlmMessage.toolResult(inv.id(), stringify(narrationSuppressedResult(inv.name()))));
                } else {
                    // The companion's own voice (pure conversation / memory recall): vocalize and record the
                    // words said as a COMPANION entry, so a later turn knows it already answered.
                    JsonObject result = execute(inv);
                    recordCompanionSpeech(spokenTextOf(inv));
                    toolResults.add(LlmMessage.toolResult(inv.id(), stringify(result)));
                }
                continue;
            }
            // Game tool / system function: execute synchronously. The result always feeds the flow; speech
            // and timeline memory depend on the action type.
            JsonObject result = preExecuted.containsKey(inv) ? preExecuted.get(inv) : execute(inv);
            recordOutcome(inv, result, tools);
            toolResults.add(LlmMessage.toolResult(inv.id(), stringify(result)));
        }
        if (!terminate) {
            flow.add(LlmMessage.assistantToolCalls(invocations));
            flow.addAll(toolResults);
        }
        return terminate;
    }

    /**
     * Decides whether the round's {@code speak} should be withheld. A command, query or macro owns its spoken
     * outcome deterministically (the handler's text, an ack, or its own steps), so once any of them has run
     * this turn the LLM's own {@code speak} is dropped. A turn that ran no game action (only system functions
     * or pure conversation) still speaks.
     */
    private boolean shouldSuppressSpeak(List<LlmToolInvocation> invocations) {
        for (LlmToolInvocation inv : invocations) {
            if (SpeakFunction.ID.equals(inv.name()) || NothingToDoFunction.ID.equals(inv.name())) {
                continue;
            }
            switch (ctx.actionTypeResolver().resolve(inv.name())) {
                case COMMAND, QUERY, MACRO -> turnRanGameAction = true;
                default -> { }
            }
        }
        return turnRanGameAction;
    }

    // recordOutcome / voice / description / rememberAction now live on the base Thought - shared with the
    // deterministic ReflexThought, which runs the same per-type outcome handling without an LLM round.

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
     * Freezes the validated tool-call set and waits for the commander's confirmation (§2.13/§5.3). The model
     * is never told an action is dangerous: the thought detects it from the danger policy after the response
     * and voices a fixed, localized confirmation prompt itself (no LLM), recorded as the companion's own
     * words. On confirm the whole set runs in LLM order; on cancel/timeout it is discarded. The outcome is
     * recorded and the turn ends (terminal).
     */
    private void handleDangerousConfirmation(List<LlmToolDefinition> tools, List<LlmToolInvocation> invocations,
                                             Map<LlmToolInvocation, JsonObject> preExecuted) {
        ctx.memoryGateway().write(new MemoryEntry(Instant.now(), memoryTopic(), MemorySource.SYSTEM,
                "dangerous action requires confirmation"));

        // Code-voiced confirmation prompt (no LLM), recorded as the companion's own COMPANION line; urgent so
        // it preempts, mirroring how the confirmation question reaches the commander before anything runs.
        String prompt = confirmDangerousActionPhrase();
        voice(prompt, true);
        recordCompanionSpeech(prompt);

        MemoryProcessingState outcome = awaitConfirmationOutcome();
        if (outcome == MemoryProcessingState.CONFIRMED) {
            // Execute the frozen set in LLM order. Each outcome is voiced and remembered by its action type,
            // exactly like a normal turn (§recordOutcome).
            for (LlmToolInvocation inv : invocations) {
                if (NothingToDoFunction.ID.equals(inv.name())) {
                    continue;
                }
                JsonObject result = preExecuted.containsKey(inv) ? preExecuted.get(inv) : execute(inv);
                recordOutcome(inv, result, tools);
            }
        }
        ctx.memoryGateway().write(new MemoryEntry(Instant.now(), memoryTopic(), MemorySource.SYSTEM,
                "dangerous action " + outcome.name().toLowerCase(Locale.ROOT)));
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

    /**
     * Handles an unrecoverable LLM response (§2.9): records the still-unwritten input as unresolved and speaks
     * a fixed service phrase (no LLM). The turn ends.
     */
    private void onInvalidResponse(boolean inputRecorded) {
        if (!inputRecorded) {
            ctx.memoryGateway().write(new MemoryEntry(Instant.now(), ConversationTopic.UNRESOLVED_COMMANDER_INPUT,
                    MemorySource.COMMANDER, currentInput));
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
                    MemorySource.COMMANDER, currentInput));
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
