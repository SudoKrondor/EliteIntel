package elite.intel.companion.mind;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.commons.AiResponseLanguagePolicy;
import elite.intel.ai.brain.i18n.LlmTextProvider;
import elite.intel.companion.confirm.ConfirmationCoordinator;
import elite.intel.companion.diag.CompanionDiagnostics;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.Urgency;
import elite.intel.companion.model.llm.LlmMessage;
import elite.intel.companion.model.llm.LlmResult;
import elite.intel.companion.model.llm.LlmToolDefinition;
import elite.intel.companion.model.llm.LlmToolInvocation;
import elite.intel.companion.model.llm.PromptCacheProfile;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemoryImportance;
import elite.intel.companion.model.memory.MemoryProcessingState;
import elite.intel.companion.model.memory.MemorySource;
import elite.intel.companion.model.speech.SpeechRequest;
import elite.intel.companion.prompt.ComposedPrompt;
import elite.intel.companion.prompt.MemoryFactCandidates;
import elite.intel.companion.tools.ClassifyTurnFunction;
import elite.intel.companion.tools.IntelActionTypeResolver.IntelActionType;
import elite.intel.companion.tools.SpeakFunction;
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
 * {@code classify_turn} and file the input (a command / custom command turn is a side effect and files nothing)
 * -> dangerous-action confirmation -> execute tool-calls, then the turn ends. It is single-round by design - a command/query/macro or speak settles it (memory is retrieved
 * before the turn as inlined answer facts, so no in-turn lookup round is needed) - or an unrecoverable
 * response stops it (§2.5/§2.6/§2.8/§5.1).
 * <p>
 * It has the full commander tool set and the COMMANDER-only paths an EVENT/narration thought cannot reach:
 * applying {@code classify_turn} (topic + importance + is_question) before the input is filed, dispatching commands/queries
 * fire-and-forget, and vocalizing their outcome deterministically. Narration ownership (§2.14): a
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
     * System turn-boundary marker recorded when a commander turn produced no reply (the model chose not to
     * answer). Explained to the model literally in {@code CompanionSystemPromptPart.FUNCTION_CALLING} - keep in sync.
     */
    private static final String NO_ANSWER_NOTE = "<no_reply/>";
    /**
     * System turn-boundary marker recorded when a commander turn was interrupted before it could reply.
     * Explained to the model literally in {@code CompanionSystemPromptPart.FUNCTION_CALLING} - keep in sync.
     */
    private static final String INTERRUPTED_NOTE = "<cut_off/>";

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

    CommanderThought(Urgency urgency, String input, String matchInput, ThoughtContext ctx) {
        super(ThoughtSource.COMMANDER, urgency, input, matchInput, ctx);
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

            // Single-round by design: one LLM round settles the turn (memory is retrieved before the turn as
            // inlined answer facts, so there is no in-turn lookup round).
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

            // Classify the turn (topic + importance + is_question) before any tool runs (§2.6).
            Map<LlmToolInvocation, JsonObject> preExecuted = applyClassification(invocations);
            // A command / custom command turn is a side effect, not dialogue: its imperative ("optimal speed",
            // "request docking") carries nothing worth recalling and would only clutter the short-term timeline
            // and the prompt, so it is not filed (its call echo is dropped too - see gameToolCallId). A query,
            // statement, or pure-conversation turn is still recorded so the dialogue history keeps its
            // user/assistant alternation: a question is stamped LOW in applyClassification and filtered out of
            // fact-recall candidates, a statement keeps its rated importance.
            if (!isPureCommandTurn(invocations)) {
                recordCurrentInput();
            }
            // Mark the input handled either way - filed, or deliberately skipped for a command turn - so an
            // interrupt/error does not fall back to re-filing it as unresolved input.
            inputRecorded = true;

            // An interrupt landing after the input was handled (filed, or skipped for a command turn) but before
            // the turn replies is a cut-off turn: route it through safeFlush so it drops a <cut_off/> boundary
            // marker instead of ending silently.
            if (interrupted) {
                safeFlush(inputRecorded);
                return;
            }

            // §2.13: a dangerous action freezes the whole validated set for the commander's confirmation.
            List<LlmToolInvocation> dangerous = dangerousActions(invocations);
            if (!dangerous.isEmpty()) {
                handleDangerousConfirmation(tools, invocations, preExecuted, dangerous);
                return; // a dangerous turn is terminal
            }

            // Execute the settling call(s) - a command/query/macro, a speak, or a bare classify_turn - and end.
            executeRound(tools, invocations, preExecuted);
        } catch (RuntimeException unexpected) {
            // An unexpected failure (e.g. during prompt assembly) must leave no memory hole; the lane logs and survives.
            onInvalidResponse(inputRecorded);
            throw unexpected;
        }
    }

    /** The live global conversation topic (a {@code classify_turn} call may move it during the thought). */
    @Override
    protected ConversationTopic memoryTopic() {
        return ctx.state().globalTopic();
    }

    /** The importance the consciousness set for this turn via {@code classify_turn} (default NORMAL). */
    @Override
    protected MemoryImportance memoryImportance() {
        return turnImportance;
    }

    // Game-tool categories are the access policy's default for COMMANDER (QUERY/ACTION/MACRO); inherited.

    @Override
    protected List<LlmToolDefinition> systemTools() {
        return ctx.systemFunctionProvider().systemFunctions(source());
    }

    /** Pre-turn clean answer facts for this commander input, inlined in the prompt as a {@code <facts>} block. */
    @Override
    protected List<MemoryFactCandidates.Fact> memoryCandidates() {
        return MemoryFactCandidates.forInput(ctx.memoryGateway(), matchInput);
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
                    // fact-recall candidate (the MemoryFactCandidates filter drops LOW commander lines).
                    turnImportance = MemoryImportance.LOW;
                }
                turnCanonicalFact = cleanCanonicalFact(JsonUtils.getAsStringOrEmpty(
                        inv.arguments(), ClassifyTurnFunction.PARAM_CANONICAL_FACT));
                preExecuted.put(inv, execute(inv)); // runs classify_turn's handle, which moves the global topic
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
     * Executes the round's tool-calls in LLM order, synchronously, and voices/remembers each outcome by its
     * action type via {@link #recordOutcome} (the handler owns speech, not the LLM). Single-round by design: a
     * command, query, macro, {@code speak} - or a bare {@code classify_turn} - completes the turn. {@code speak}
     * is voiced or withheld here.
     * <p>
     * Synchronous on purpose (the fire-and-forget dispatch was reverted): a long command holds the lane until
     * it finishes. Decoupling a slow command's outcome from the thought is a separate, cause-level change.
     */
    private void executeRound(List<LlmToolDefinition> tools,
                              List<LlmToolInvocation> invocations,
                              Map<LlmToolInvocation, JsonObject> preExecuted) {
        boolean suppressSpeak = shouldSuppressSpeak(invocations);
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
            // Game tool / system function: acknowledge a command immediately (before it runs), then settle it.
            if (!preExecuted.containsKey(inv) && isCommand(inv)) {
                voice(StringUtls.affirmative(), false);
            }
            IntelActionType settledType = ctx.actionTypeResolver().resolve(inv.name());
            if (settledType.isGameAction()) {
                // The turn-settling game action - the headline of what the companion actually did this turn.
                CompanionDiagnostics.info(trace(), "settle", settledType + " " + inv.name());
            }
            settleGameCall(inv, tools, preExecuted);
        }
        // No game action owned the outcome and no non-blank speak was voiced (bare classify_turn, or an empty
        // speak): the turn drew no reply - record that so the timeline keeps a distinct turn boundary here.
        if (!suppressSpeak && !spokeToCommander(invocations)) {
            CompanionDiagnostics.debug(trace(), "settle", "no reply");
            recordSystemNote(NO_ANSWER_NOTE);
        }
    }

    /** Whether the round emitted a non-blank {@code speak} - i.e. the companion actually said something this turn. */
    private static boolean spokeToCommander(List<LlmToolInvocation> invocations) {
        return invocations.stream()
                .anyMatch(inv -> SpeakFunction.ID.equals(inv.name()) && !spokenTextOf(inv).isBlank());
    }

    /**
     * Records a turn-boundary marker ({@link #NO_ANSWER_NOTE} / {@link #INTERRUPTED_NOTE}) as a {@code SYSTEM}
     * short-term entry. It keeps a distinct boundary between two commander turns instead of leaving them
     * adjacent to be coalesced into one blurred {@code user} message, which erodes anaphora resolution across
     * turns (an unanswered "why?" then a follow-up would otherwise fuse). It is written as SYSTEM, not as a
     * fabricated companion reply, so the model reads it as an out-of-band observation rather than an
     * in-character example of staying silent (which would normalize non-answers, against the always-answer
     * rule); a self-closing tag is used - not prose - so it never reads as speech or a stray instruction.
     * Stamped LOW: transient bookkeeping, never a durable fact or a recall candidate.
     */
    private void recordSystemNote(String marker) {
        ctx.memoryGateway().write(new MemoryEntry(Instant.now(), memoryTopic(), MemorySource.SYSTEM,
                marker, MemoryImportance.LOW));
    }

    /**
     * Settles one game tool-call: record the model's call (for pair replay), run it (reusing a pre-executed
     * result when present), then record its outcome - all under one tool-call id linking the call to its result.
     * A system function (classify_turn) gets no id and no CALL entry. Shared by the normal round and the
     * dangerous-confirmation round so the recording sequence lives in one place.
     */
    private void settleGameCall(LlmToolInvocation inv, List<LlmToolDefinition> tools,
                                Map<LlmToolInvocation, JsonObject> preExecuted) {
        String toolCallId = gameToolCallId(inv);
        if (toolCallId != null) {
            recordCall(toolCallId, inv);
        }
        JsonObject result = preExecuted.containsKey(inv) ? preExecuted.get(inv) : execute(inv, toolCallId);
        recordOutcome(inv, result, tools, toolCallId);
    }

    /**
     * A fresh tool-call id for a QUERY, so its recorded call pairs with its answer on replay; {@code null} for a
     * COMMAND / custom command and for a system function (classify_turn). A command turn is a side effect, not
     * dialogue - its call echo is not recorded - so it gets no id; with none, any handler-voiced outcome is
     * remembered as a plain companion line rather than a linked tool result, leaving nothing orphaned in the
     * replayed timeline.
     */
    private String gameToolCallId(LlmToolInvocation inv) {
        return ctx.actionTypeResolver().resolve(inv.name()) == IntelActionType.QUERY ? newId() : null;
    }

    /**
     * Whether this turn is a pure command / custom command (MACRO) turn - a side effect, not dialogue - whose
     * imperative and call echo are not filed to memory (they carry nothing worth recalling and would only
     * clutter the short-term timeline and the prompt). True when the turn runs a command or custom command and
     * no QUERY. A co-occurring QUERY vetoes the suppression: a query records a call/result pair that must keep
     * its preceding user turn (else the pair replays as an {@code assistant(tool_calls)} with no user before it),
     * so the input is filed. A query, statement, or pure-conversation turn is recorded normally.
     */
    private boolean isPureCommandTurn(List<LlmToolInvocation> invocations) {
        boolean hasCommand = false;
        for (LlmToolInvocation inv : invocations) {
            IntelActionType type = ctx.actionTypeResolver().resolve(inv.name());
            if (type == IntelActionType.QUERY) {
                return false; // a query records a pair needing its preceding user turn, so keep the input
            }
            if (type == IntelActionType.COMMAND || type == IntelActionType.MACRO) {
                hasCommand = true;
            }
        }
        return hasCommand;
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
                    && ctx.actionTypeResolver().resolve(inv.name()).isGameAction()) {
                turnRanGameAction = true;
            }
        }
        return turnRanGameAction;
    }

    /** COMMANDER-only immediate acknowledgement before an LLM-selected command starts executing. */
    private boolean isCommand(LlmToolInvocation inv) {
        return ctx.actionTypeResolver().resolve(inv.name()) == IntelActionType.COMMAND;
    }

    // recordOutcome / recordCall / recordToolResult / voice now live on the base Thought - shared with the
    // deterministic ReflexThought, which runs the same per-type outcome handling without an LLM round.

    /** The tool-calls in the validated set that require dangerous-action confirmation, in LLM order (empty when none) (§2.13). */
    private List<LlmToolInvocation> dangerousActions(List<LlmToolInvocation> invocations) {
        return invocations.stream()
                .filter(inv -> ctx.dangerousActionPolicy().isDangerous(inv))
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
        CompanionDiagnostics.info(trace(), "confirm", "dangerous action detected: " + CompanionDiagnostics.calls(dangerous));
        ctx.memoryGateway().write(new MemoryEntry(Instant.now(), memoryTopic(), MemorySource.SYSTEM,
                "dangerous action requires confirmation"));

        // Code-voiced confirmation prompt (no LLM), recorded as the companion's own COMPANION line; urgent so
        // it preempts, mirroring how the confirmation question reaches the commander before anything runs.
        String prompt = confirmDangerousActionPhrase();
        voice(prompt, true);
        recordCompanionSpeech(prompt);

        MemoryProcessingState outcome = awaitConfirmationOutcome();
        CompanionDiagnostics.info(trace(), "confirm", "outcome=" + outcome.name().toLowerCase(Locale.ROOT));
        if (outcome == MemoryProcessingState.CONFIRMED) {
            // Execute the frozen set in LLM order. Each call is recorded then voiced and remembered by its
            // action type, exactly like a normal turn (§settleGameCall / §recordOutcome).
            for (LlmToolInvocation inv : invocations) {
                settleGameCall(inv, tools, preExecuted);
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
        CompanionDiagnostics.info(trace(), "settle", "cannot execute (unrecoverable LLM response)");
        if (!inputRecorded) {
            ctx.memoryGateway().write(new MemoryEntry(Instant.now(), ConversationTopic.UNRESOLVED_COMMANDER_INPUT,
                    MemorySource.COMMANDER, currentInput));
        }
        ctx.speechGateway().submit(new SpeechRequest(newId(), cannotExecutePhrase(), urgency()));
    }

    /**
     * Safe-flush on interrupt (§2.7): never leave a memory hole. If the input was not yet recorded, write it
     * under the unresolved-commander-input fallback as INTERRUPTED; tool results are written as they execute,
     * so nothing is batched to flush. If the input WAS already recorded (the turn was cut off after filing it
     * but before replying), drop a {@link #INTERRUPTED_NOTE} boundary marker so the interrupted turn is not left
     * adjacent to the next commander turn and coalesced with it. No new LLM/query/action/speech is started here.
     */
    private void safeFlush(boolean inputRecorded) {
        CompanionDiagnostics.debug(trace(), "flush",
                inputRecorded ? "cut off after filing input" : "interrupted before filing (input saved as unresolved)");
        if (!inputRecorded) {
            ctx.memoryGateway().write(new MemoryEntry(Instant.now(), ConversationTopic.UNRESOLVED_COMMANDER_INPUT,
                    MemorySource.COMMANDER, currentInput));
        } else {
            recordSystemNote(INTERRUPTED_NOTE);
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
