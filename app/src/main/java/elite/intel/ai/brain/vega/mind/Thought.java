package elite.intel.ai.brain.vega.mind;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.AIConstants;
import elite.intel.ai.brain.commons.AiResponseLanguagePolicy;
import elite.intel.ai.brain.i18n.LlmTextProvider;
import elite.intel.ai.brain.vega.diag.CompanionDiagnostics;
import elite.intel.ai.brain.vega.memory.CompanionMemoryPolicy;
import elite.intel.ai.brain.vega.model.IntelActionCategory;
import elite.intel.ai.brain.vega.model.ThoughtSource;
import elite.intel.ai.brain.vega.model.Urgency;
import elite.intel.ai.brain.vega.model.execution.ExecutionRequest;
import elite.intel.ai.brain.vega.model.llm.*;
import elite.intel.ai.brain.vega.model.memory.MemoryRecord;
import elite.intel.ai.brain.vega.model.speech.SpeechRequest;
import elite.intel.ai.brain.vega.prompt.ComposedPrompt;
import elite.intel.ai.brain.vega.prompt.Fact;
import elite.intel.ai.brain.vega.prompt.PromptXml;
import elite.intel.ai.brain.vega.tools.SpeakFunction;
import elite.intel.ai.brain.vega.tools.SystemFunctionResultFields;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.util.json.JsonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A unit of work of the consciousness: the parts shared by every kind of thought. It holds its turn-scoped
 * input signals and shared collaborators, and provides the building blocks - assembling the prompt,
 * running a single interruptible LLM round, executing a tool call, and publishing completed memory records.
 * <p>
 * It owns no thinking loop. Each concrete kind drives its own {@link #run}: {@link CommanderThought} the full
 * tool-calling round with dangerous-action confirmation, {@link EventThought} a single short round (or a verbatim
 * line) that phrases a subscriber's reaction. Completed dialogue, query, and event records are published atomically.
 * <p>
 * Threading: the cognitive part starts on a dispatcher lane thread; detached completion may run on an execution
 * thread. {@link #interrupt} cooperates through a volatile flag and cancellation of the owned future.
 */
public abstract class Thought {

    private static final Logger log = LogManager.getLogger(Thought.class);

    /** Monotonic sequence backing the short per-thought {@link #trace} id, so concurrent lanes are told apart in the log. */
    private static final AtomicInteger TRACE_SEQ = new AtomicInteger();
    /** Existing localized service phrase for a command/query/macro execution that could not complete. */
    private static final String CANNOT_EXECUTE_KEY = "handler.common.cantDoNow";

    /** Stable per-thought diagnostic tag ({@code SOURCE#n}), correlating every SYSTEM LOG line of this one thought. */
    private final String trace;
    /** Immutable input-side signals born with this thought; distinct from service-owning {@link #dependencies}. */
    protected final ThoughtContext context;
    protected final ThoughtDependencies dependencies;

    /** Set by {@link #interrupt} from another thread; a run honors it at step boundaries. */
    protected volatile boolean interrupted;
    /** The currently owned LLM, confirmation, or detached-handler future, or null. */
    protected volatile CompletableFuture<?> inFlight;
    /** Linearizes a completed memory publication against interruption of this thought. */
    private final Object settlementPublicationLock = new Object();
    /** Lane-thread-confined marker used to report the latency until this turn first begins tool execution. */
    private boolean firstToolStarted;

    /** Creates a thought from its immutable turn signals and the shared service context. */
    protected Thought(ThoughtContext context, ThoughtDependencies dependencies) {
        this.context = context;
        this.dependencies = dependencies;
        this.trace = context.source() + "#" + TRACE_SEQ.incrementAndGet();
    }

    /** The per-thought diagnostic tag ({@code SOURCE#n}); every {@link CompanionDiagnostics} line of this thought carries it. */
    public final String trace() {
        return trace;
    }

    /** Returns the elapsed time since the dispatcher accepted this turn, for diagnostics only. */
    final long elapsedSinceAcceptanceMillis() {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - context.acceptedAtNanos());
    }

    // --- factories (the public construction API; each returns the matching concrete kind) ---

    /** Creates a thought from a commander reply. */
    public static Thought commander(Urgency urgency, String input, ThoughtDependencies dependencies) {
        return commander(ThoughtContext.commander(urgency, input, input), dependencies);
    }

    /**
     * As above, but with a separate canonical {@code matchInput} (e.g. an STT-corrected form) used for
     * tool selection, the LLM-visible current input, and commander memory; raw {@code input} stays with execution.
     */
    public static Thought commander(Urgency urgency, String input, String matchInput, ThoughtDependencies dependencies) {
        return commander(ThoughtContext.commander(urgency, input, matchInput), dependencies);
    }

    /** Creates a commander thought from turn signals already prepared by the dispatcher. */
    static Thought commander(ThoughtContext context, ThoughtDependencies dependencies) {
        return new CommanderThought(context, dependencies);
    }

    /**
     * Creates a reactive thought from a gameplay subscriber's {@code CompanionReactionEvent}: the companion
     * phrases the supplied event data and speaks it. Data and instructions exist only in this LLM request; after a
     * successful round, only the final narration is stored as the EVENT fact.
     */
    public static Thought eventReaction(Urgency urgency, String stimulus, String instructions,
                                        ThoughtDependencies dependencies) {
        String eventData = boundTransientInput(stimulus, CompanionMemoryPolicy.eventDataMaxChars());
        StringBuilder promptInput = new StringBuilder(PromptXml.element("event_data", eventData));
        if (instructions != null && !instructions.isBlank()) {
            promptInput.append("\n\n")
                    .append(PromptXml.element("narration_instructions", boundTransientInput(
                            instructions, CompanionMemoryPolicy.eventInstructionsMaxChars())));
        }
        return new EventThought(ThoughtContext.event(urgency, eventData, promptInput.toString()), dependencies);
    }

    /**
     * Creates a verbatim reactive thought from a gameplay subscriber that already has a finished phrase: it is
     * voiced as-is (no LLM) and stored as the EVENT fact.
     */
    public static Thought eventVerbatim(Urgency urgency, String phrase, ThoughtDependencies dependencies) {
        return new EventThought(ThoughtContext.event(urgency, phrase, phrase), phrase, dependencies);
    }

    /**
     * Creates a reflex thought for one safe action selected by an exact reflex. It runs on the commander lane
     * like a {@link CommanderThought} but skips the LLM entirely. Commands remain outside memory; completed
     * queries publish a commander/companion QUERY pair.
     */
    public static Thought reflex(Urgency urgency, String input, String commandId, ThoughtDependencies dependencies) {
        return reflex(ThoughtContext.commander(urgency, input, input), commandId, new JsonObject(), dependencies);
    }

    /**
     * Creates a reflex thought from turn signals already prepared by the dispatcher, running the action with the
     * arguments its alias supplied (empty when the action takes none).
     */
    static Thought reflex(ThoughtContext context, String commandId, JsonObject arguments,
                          ThoughtDependencies dependencies) {
        return new ReflexThought(context, commandId, arguments, dependencies);
    }

    /** Runs this thought on the lane thread. Each concrete kind drives its own lifecycle. */
    public abstract void run();

    /**
     * Starts the lane-owned lifecycle and returns its real completion. The default is synchronous; thoughts that
     * detach slow handler work override this so the lane worker may accept the next turn while lifecycle tracking,
     * watchdog interruption, and {@code isIdle()} continue until the detached work settles.
     */
    CompletableFuture<Void> startLifecycle() {
        if (isStopped()) {
            return CompletableFuture.completedFuture(null);
        }
        try {
            run();
            return CompletableFuture.completedFuture(null);
        } catch (Throwable failure) {
            return CompletableFuture.failedFuture(failure);
        }
    }

    /**
     * IntelAction categories this thought may use - the single input to game-tool selection. Default: the
     * access policy's categories for this thought's source (COMMANDER -> QUERY/ACTION/MACRO; EVENT -> none,
     * since a reactive {@link EventThought} only phrases pre-digested subscriber data, so the one reducer call
     * offers no game tools).
     */
    protected Set<IntelActionCategory> allowedCategories() {
        return dependencies.intelActionAccessPolicy().allowedCategories(source());
    }

    /**
     * System tools offered to the LLM for this thought. An LLM-driven thought overrides this; other thought types
     * inherit the empty default.
     */
    protected List<LlmToolDefinition> systemTools(List<LlmToolDefinition> gameTools) {
        return List.of();
    }

    /**
     * One LLM round, registered as the interruptible in-flight handle. A provider/transport failure or an
     * interrupt-driven cancellation (exceptional future) is treated as no usable result.
     */
    protected LlmResult submitRound(List<LlmMessage> flow, List<LlmToolDefinition> tools, PromptCacheProfile profile) {
        if (isStopped()) {
            return null;
        }
        CompanionDiagnostics.debug(trace, "llm", "request: tools=" + tools.size() + " messages=" + flow.size());
        CompletableFuture<LlmResult> future = dependencies.llmGateway()
                .submit(new LlmRequest(newId(), List.copyOf(flow), tools, profile, trace));
        inFlight = future;
        if (isStopped()) {
            future.cancel(true); // interrupt raced ahead of registration: cancel now so join unblocks
        }
        // Time the whole round: the gateway runs its repair/retry synchronously within this one future, so the
        // elapsed covers every physical call, separating LLM latency from the turn's exec/confirmation time.
        long startedMillis = System.currentTimeMillis();
        try {
            LlmResult result = future.join();
            CompanionDiagnostics.debug(trace, "llm",
                    describeResult(result) + " | " + (System.currentTimeMillis() - startedMillis) + " ms");
            return result;
        } catch (RuntimeException llmFailure) {
            if (!isStopped()) {
                // A provider/transport failure (not an interrupt-driven cancel) - surface the cause.
                log.warn("Companion LLM round failed; treating as no usable result", llmFailure);
            }
            CompanionDiagnostics.debug(trace, "llm", isStopped() ? "response: cancelled" : "response: failed");
            return null;
        } finally {
            inFlight = null;
        }
    }

    /**
     * One-line diagnostic for an LLM result: the tool-calls (or {@code none}/{@code INVALID}), the provider
     * finish reason, and the length + snippet of any free text the model returned alongside the tool-calls -
     * which the consciousness turn discards. A non-zero {@code dropped-text} means the model "answered" as plain
     * text instead of a {@code speak} call, which reads in the log as a silent turn (see LlmResult.droppedText).
     */
    private static String describeResult(LlmResult result) {
        if (result == null) {
            return "response: none";
        }
        StringBuilder sb = new StringBuilder(result.isValid()
                ? "response: " + CompanionDiagnostics.calls(result.toolInvocations())
                : "response: INVALID");
        if (result.finishReason() != null) {
            sb.append(" | finish=").append(result.finishReason());
        }
        // Only surface dropped free text when there is some: a silent turn (model "answered" as plain text
        // instead of a speak call) is the anomaly worth seeing; the common zero case is noise.
        String dropped = result.droppedText();
        if (dropped != null && !dropped.isBlank()) {
            sb.append(" | dropped-text=").append(dropped.length())
                    .append(": \"").append(CompanionDiagnostics.truncate(dropped)).append("\"");
        }
        return sb.toString();
    }

    /** Assembles the seed prompt: reduced game tools, system tools, recent history, and live fact candidates. */
    protected ComposedPrompt composeInitialPrompt() {
        long composeStartedNanos = System.nanoTime();
        long reducerStartedNanos = System.nanoTime();
        List<LlmToolDefinition> gameTools = selectedGameTools();
        long reducerMillis = elapsedMillis(reducerStartedNanos);
        List<LlmToolDefinition> sysTools = systemTools(gameTools);
        long historyStartedNanos = System.nanoTime();
        List<MemoryRecord> history = dependencies.memoryGateway().readRecentHistory();
        long historyMillis = elapsedMillis(historyStartedNanos);
        long factsStartedNanos = System.nanoTime();
        List<Fact> candidates = factCandidates();
        long factsMillis = elapsedMillis(factsStartedNanos);
        long promptStartedNanos = System.nanoTime();
        ComposedPrompt composed = dependencies.promptComposer().compose(
                source(), context.matchInput(), gameTools, sysTools, history, candidates,
                context.pendingClarification());
        long promptMillis = elapsedMillis(promptStartedNanos);
        // The game-tool count and list are already owned by the reduce line (kept=N -> [...]) and the total sent is
        // owned by the llm request line (tools=N); compose reports only what it adds to the prompt - the system
        // tools, grounding facts, and recent-history depth - so no tool count is repeated across lines.
        CompanionDiagnostics.debug(trace, "compose",
                "sysTools=" + CompanionDiagnostics.names(sysTools)
                        + " facts=" + candidates.size() + " history=" + history.size()
                        + " | reduce=" + reducerMillis + " ms"
                        + " history=" + historyMillis + " ms"
                        + " facts=" + factsMillis + " ms"
                        + " prompt=" + promptMillis + " ms"
                        + " total=" + elapsedMillis(composeStartedNanos) + " ms");
        // Show the actual source-tagged live facts, one per line as in the prompt, numbered i/total so multiple
        // grounding facts are easy to count and reference.
        int factNo = 0;
        for (Fact fact : candidates) {
            CompanionDiagnostics.debug(trace, "facts",
                    (++factNo) + "/" + candidates.size() + " " + CompanionDiagnostics.fact(fact));
        }
        return composed;
    }

    /**
     * Host-provided live facts to append to the system prompt. Default none; a COMMANDER thought overrides it.
     */
    protected List<Fact> factCandidates() {
        return List.of();
    }

    /**
     * The single point where game tools are formed. Normal candidates are reduced from the current input; a
     * claimed clarification target is re-resolved by id against this turn's visibility snapshot and prepended.
     */
    private List<LlmToolDefinition> selectedGameTools() {
        Set<IntelActionCategory> categories = allowedCategories();
        List<LlmToolDefinition> selected = dependencies.reducer().selectTools(
                categories, context.matchInput(), null, context.gameStateSnapshot());
        var pending = context.pendingClarification();
        if (pending == null) {
            return selected;
        }

        var target = dependencies.reducer().findToolById(
                categories, pending.actionId(), context.gameStateSnapshot());
        if (target.isEmpty()) {
            CompanionDiagnostics.debug(trace, "clarify",
                    "target unavailable in current state: " + pending.actionId());
            return selected;
        }

        Map<String, LlmToolDefinition> merged = new LinkedHashMap<>();
        merged.put(target.get().name(), target.get());
        selected.forEach(tool -> merged.putIfAbsent(tool.name(), tool));
        CompanionDiagnostics.debug(trace, "clarify", "re-offered target " + pending.actionId());
        return List.copyOf(merged.values());
    }

    private static long elapsedMillis(long startedNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);
    }

    /** Submits one tool-call and attributes its execution latency to this thought without awaiting it. */
    protected CompletableFuture<JsonObject> submitExecution(LlmToolInvocation inv) {
        if (isStopped()) {
            return CompletableFuture.failedFuture(
                    new CancellationException("Companion runtime generation is no longer active"));
        }
        // Only game tools (command/query/macro) dump their call+args here. System functions have dedicated
        // diagnostic lines, so their raw call would only be a redundant copy. Every failure is still surfaced.
        if (dependencies.actionTypeResolver().resolve(inv.name()).isGameAction()) {
            CompanionDiagnostics.debug(trace, "exec", inv.name() + CompanionDiagnostics.args(inv.arguments()));
        }
        if (!firstToolStarted) {
            firstToolStarted = true;
            CompanionDiagnostics.debug(trace, "latency", "time-to-first-tool=" + elapsedSinceAcceptanceMillis() + " ms");
        }
        long executionStartedNanos = System.nanoTime();
        CompletableFuture<JsonObject> future = dependencies.executionGateway()
                .submit(new ExecutionRequest(newId(), inv.name(), inv.arguments(), originalExecutionInputFor(inv),
                        matchExecutionInputFor(inv), dependencies.runtimeGeneration().generationId()));
        future.whenComplete((ignored, failure) -> CompanionDiagnostics.debug(trace, "exec-time",
                inv.name() + "=" + elapsedMillis(executionStartedNanos) + " ms"));
        return future;
    }

    /**
     * Gives a resumed target both the originating order and the terse parameter reply. A superseding action sees
     * only the current words, so handler-level fallback parsing cannot accidentally inherit the abandoned order.
     */
    private String originalExecutionInputFor(LlmToolInvocation inv) {
        var pending = context.pendingClarification();
        return pending != null && pending.actionId().equals(inv.name())
                ? pending.originalInput() + "\n" + context.currentInput()
                : context.currentInput();
    }

    /** Mirrors the execution input using the canonical wording shown to the model. */
    private String matchExecutionInputFor(LlmToolInvocation inv) {
        var pending = context.pendingClarification();
        return pending != null && pending.actionId().equals(inv.name())
                ? pending.originalInput() + "\n" + context.matchInput()
                : context.matchInput();
    }

    /** Runs one tool call via the execution gateway; a failed call becomes a structured local error result. */
    protected JsonObject execute(LlmToolInvocation inv) {
        try {
            return submitExecution(inv).join();
        } catch (RuntimeException failed) {
            CompanionDiagnostics.debug(trace, "exec", inv.name() + " failed: " + CompanionDiagnostics.truncate(String.valueOf(failed.getMessage())));
            return executionError(inv.name(), failed);
        }
    }

    /**
     * A failed execution rendered as an error result the LLM can read (the cause is unwrapped if present).
     */
    protected static JsonObject executionError(String tool, Throwable failed) {
        Throwable cause = failed.getCause() != null ? failed.getCause() : failed;
        JsonObject error = new JsonObject();
        error.addProperty("error", String.valueOf(cause.getMessage()));
        error.addProperty("tool", tool);
        return error;
    }

    /**
     * Commits one complete conversational turn from the input retained by this thought's context and the LLM's
     * non-blank spoken reply. Both entries are built before publication and admitted through one runtime-generation
     * fence; command feedback, clarification prompts, and service phrases must not call this method because none is
     * an LLM dialogue reply.
     */
    protected void recordDialoguePair(String reply) {
        if (isStopped() || reply == null || reply.isBlank()) {
            return;
        }
        MemoryRecord record = MemoryRecord.dialogue(Instant.now(), context.memoryInput(), reply);
        boolean recorded = publishSettlement(() -> dependencies.memoryGateway().write(record));
        if (recorded) {
            CompanionDiagnostics.debug(trace, "memory", "record dialogue");
        }
    }

    /** Stores a successful final event narration without retaining its potentially large source data. */
    protected void recordEventResult(String result) {
        if (isStopped() || result == null || result.isBlank()) {
            return;
        }
        MemoryRecord record = MemoryRecord.event(Instant.now(), result);
        boolean recorded = publishSettlement(() -> dependencies.memoryGateway().write(record));
        if (recorded) {
            CompanionDiagnostics.debug(trace, "memory", "record event");
        }
    }

    /** The text a {@code speak} invocation carries (the words to vocalize), or empty when absent. */
    protected static String spokenTextOf(LlmToolInvocation speak) {
        return JsonUtils.getAsStringOrEmpty(speak.arguments(), SpeakFunction.PARAM_TEXT);
    }

    /** Applies the shared memory and speech policy for completed game-tool outcomes. */
    protected void settleToolOutcome(LlmToolInvocation inv, JsonObject result) {
        if (!isRuntimeActive()) {
            return;
        }
        switch (dependencies.actionTypeResolver().resolve(inv.name())) {
            case QUERY -> settleQueryOutcome(result);
            case COMMAND -> {
                String outcome = spokenOutcomeText(result);
                if (!outcome.isBlank()) {
                    voice(outcome, false);
                }
            }
            case MACRO -> {
                // Successful macros narrate their own SPEAK steps. A failed macro otherwise has no user-visible
                // completion at all, so publish the shared failure phrase instead.
                if (isExecutionFailure(result)) {
                    String failure = spokenOutcomeText(result);
                    voice(failure, false);
                }
            }
            case SYSTEM, UNKNOWN -> { /* no generic speech or memory write; the function owns any side effects */ }
        }
    }

    /**
     * Settles every QUERY path. Failures are voiced but never remembered; a successful non-blank answer is stored
     * with the commander input as one pair and then voiced. Interrupted, cancelled and empty queries leave no
     * memory. The settlement lock prevents a late handler from publishing after interruption won the race.
     *
     */
    private void settleQueryOutcome(JsonObject result) {
        if (!isRuntimeActive()) {
            return;
        }
        if (isExecutionFailure(result)) {
            String failure = spokenOutcomeText(result);
            if (!failure.isBlank()) {
                voice(failure, false);
            }
            return;
        }

        String answer = spokenTextOf(result);
        if (answer.isBlank()) {
            return;
        }
        MemoryRecord record = MemoryRecord.query(Instant.now(), context.memoryInput(), answer);

        boolean published = publishSettlement(() -> {
            dependencies.memoryGateway().write(record);
            voice(answer, false);
        });
        if (published) {
            CompanionDiagnostics.debug(trace, "memory", "record query");
        }
    }

    /** The handler-provided spoken text in a tool result, or empty when absent. */
    protected static String spokenTextOf(JsonObject result) {
        return result == null ? "" : JsonUtils.getAsStringOrEmpty(result, AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE);
    }

    /**
     * Returns the handler-provided reply, or the localized failure phrase for an error result that supplied no
     * speakable text. This keeps execution details in diagnostics while the commander always receives a clear
     * outcome.
     */
    protected static String spokenOutcomeText(JsonObject result) {
        String handlerText = spokenTextOf(result);
        return !handlerText.isBlank() || !isExecutionFailure(result) ? handlerText : executionFailurePhrase();
    }

    private static boolean isExecutionFailure(JsonObject result) {
        return result != null && result.has(SystemFunctionResultFields.ERROR);
    }

    /** Returns the fixed localized phrase used when a command, query, or macro execution fails. */
    protected static String executionFailurePhrase() {
        Language language = AiResponseLanguagePolicy.resolveEffectiveAiResponseLanguage(SystemSession.getInstance());
        return LlmTextProvider.getText(language, CANNOT_EXECUTE_KEY);
    }

    /** Voices a non-blank phrase through the speech gateway (mission-critical -> urgent/preempting channel). */
    protected void voice(String text, boolean critical) {
        if (!isRuntimeActive() || text == null || text.isBlank()) {
            return;
        }
        CompanionDiagnostics.debug(trace, "voice", (critical ? "urgent " : "") + "\"" + CompanionDiagnostics.truncate(text) + "\"");
        dependencies.speechGateway().submit(new SpeechRequest(newId(), text, critical ? Urgency.URGENT : Urgency.NORMAL));
    }

    /**
     * Interrupts the thought from another thread: raises the interrupt flag and cancels the awaited
     * future so the lane thread unblocks and dies. It never cancels a started action or macro and
     * writes no memory itself; an incomplete commander turn has no pair to publish.
     */
    public final void interrupt() {
        CompletableFuture<?> current;
        synchronized (settlementPublicationLock) {
            interrupted = true;
            current = inFlight;
        }
        if (current != null) {
            current.cancel(true);
        }
    }

    /** Runs one all-or-nothing settlement publication unless interruption/runtime shutdown won first. */
    private boolean publishSettlement(Runnable publication) {
        synchronized (settlementPublicationLock) {
            if (isStopped()) {
                return false;
            }
            return dependencies.runtimeGeneration().runIfActive(publication);
        }
    }

    /** Whether this thought's runtime generation still permits memory, execution, and speech side effects. */
    protected final boolean isRuntimeActive() {
        return dependencies.runtimeGeneration().isActive();
    }

    /** Whether interruption or runtime shutdown requires this thought to stop at the current boundary. */
    protected final boolean isStopped() {
        return interrupted || !isRuntimeActive();
    }

    public final ThoughtSource source() {
        return context.source();
    }

    public final Urgency urgency() {
        return context.urgency();
    }

    protected static String newId() {
        return UUID.randomUUID().toString();
    }

    /** Bounds transient prompt data without creating a memory representation of the source payload. */
    private static String boundTransientInput(String text, int maxChars) {
        String value = text == null ? "" : text;
        if (value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, Math.max(0, maxChars - 3)).stripTrailing() + "...";
    }
}
