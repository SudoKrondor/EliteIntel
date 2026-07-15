package elite.intel.companion.mind;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.AIConstants;
import elite.intel.ai.brain.commons.AiResponseLanguagePolicy;
import elite.intel.ai.brain.i18n.LlmTextProvider;
import elite.intel.companion.diag.CompanionDiagnostics;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.IntelActionCategory;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.Urgency;
import elite.intel.companion.model.execution.ExecutionRequest;
import elite.intel.companion.model.llm.*;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemoryImportance;
import elite.intel.companion.model.memory.MemorySource;
import elite.intel.companion.model.memory.ToolLink;
import elite.intel.companion.model.speech.SpeechRequest;
import elite.intel.companion.prompt.ComposedPrompt;
import elite.intel.companion.prompt.Fact;
import elite.intel.companion.prompt.PromptXml;
import elite.intel.companion.tools.SpeakFunction;
import elite.intel.companion.tools.SystemFunctionResultFields;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.JsonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A unit of work of the consciousness: the parts shared by every kind of thought. It holds its turn-scoped
 * input signals and shared collaborators, and provides the building blocks - assembling the prompt,
 * running a single interruptible LLM round, executing a tool-call, and recording the input or the
 * companion's own speech to memory.
 * <p>
 * It owns no thinking loop. Each concrete kind drives its own {@link #run}: {@link CommanderThought} the full
 * tool-calling loop with dangerous-action confirmation, {@link EventThought} a single short round (or a verbatim
 * line) that phrases a subscriber's reaction. COMMANDER/reflex thoughts freeze their topic before a handler
 * detaches; EVENT uses the subscriber-supplied topic (§2.4/§2.5).
 * <p>
 * Threading: the cognitive part starts on a dispatcher lane thread; detached completion may run on an execution
 * thread. {@link #interrupt} cooperates through a volatile flag and cancellation of the owned future (§2.7).
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

    /** Set by {@link #interrupt} from another thread; a run honors it at step boundaries (§2.7). */
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

    /**
     * Creates a thought from a commander reply. Its memory tag is the live global conversation topic
     * (which a {@code classify_turn} call may move during the thought).
     */
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
     * phrases the pre-digested {@code stimulus} and speaks it. The stimulus is recorded verbatim as the
     * {@code user} turn; the LLM-visible input wraps it as {@code <event_data>} and wraps {@code instructions}
     * as {@code <narration_instructions>} so data and phrasing guidance are not read as commander speech.
     * Instructions steer only this turn's phrasing and are never remembered. Its memory tag is the
     * subscriber-supplied topic; it never moves the global conversation topic and gets no game tools.
     */
    public static Thought eventReaction(Urgency urgency, String stimulus, String instructions,
                                        ConversationTopic eventTopic, ThoughtDependencies dependencies) {
        StringBuilder promptInput = new StringBuilder(PromptXml.element("event_data", stimulus));
        if (instructions != null && !instructions.isBlank()) {
            promptInput.append("\n\n")
                    .append(PromptXml.element("narration_instructions", instructions));
        }
        return new EventThought(ThoughtContext.event(urgency, stimulus, promptInput.toString()), eventTopic,
                dependencies);
    }

    /**
     * Creates a verbatim reactive thought from a gameplay subscriber that already has a finished phrase: it is
     * voiced as-is (no LLM) and recorded as the companion's reply, paired with the short {@code sourceId} as the
     * {@code user} turn (never the raw data - a huge list would bloat the prompt). Its memory tag is the
     * subscriber-supplied topic; it never moves the global conversation topic and gets no game tools.
     */
    public static Thought eventVerbatim(Urgency urgency, String sourceId, String phrase,
                                        ConversationTopic eventTopic, ThoughtDependencies dependencies) {
        return new EventThought(ThoughtContext.event(urgency, sourceId, sourceId), phrase, eventTopic,
                dependencies);
    }

    /**
     * Creates a reflex thought: a commander input the {@code ReflexResolver} matched verbatim to exactly one
     * safe, parameterless command. It runs on the commander lane like a {@link CommanderThought} but skips the
     * LLM entirely - it executes the resolved command and voices a non-blank outcome without filing command
     * execution as dialogue ({@link #recordOutcome}). Anything ambiguous, parameterized or dangerous is never a
     * reflex.
     */
    public static Thought reflex(Urgency urgency, String input, String commandId, ThoughtDependencies dependencies) {
        return reflex(ThoughtContext.commander(urgency, input, input), commandId, dependencies);
    }

    /** Creates a reflex thought from turn signals already prepared by the dispatcher. */
    static Thought reflex(ThoughtContext context, String commandId, ThoughtDependencies dependencies) {
        return new ReflexThought(context, commandId, dependencies);
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

    /** COMMANDER: the live global conversation topic; EVENT/narration: the event's fixed topic. */
    protected abstract ConversationTopic memoryTopic();

    /**
     * Importance stamped on this thought's memory entries, resolved per source exactly like
     * {@link #memoryTopic()}: {@code CommanderThought} reflects the level the consciousness set for the turn
     * via {@code classify_turn}; the others are {@link MemoryImportance#NORMAL}.
     */
    protected abstract MemoryImportance memoryImportance();

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
     * System tools offered to the LLM for this thought. An LLM-driven thought overrides this; a memory-only
     * thought never composes a prompt, so it inherits the empty default.
     */
    protected List<LlmToolDefinition> systemTools(List<LlmToolDefinition> gameTools) {
        return List.of();
    }

    /**
     * One LLM round, registered as the interruptible in-flight handle. A provider/transport failure or an
     * interrupt-driven cancellation (exceptional future) is treated as no usable result (§2.9/§2.7).
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

    /** Assembles the seed prompt: reduced game tools + system tools + memory snapshot + answer candidates. */
    protected ComposedPrompt composeInitialPrompt() {
        long composeStartedNanos = System.nanoTime();
        long reducerStartedNanos = System.nanoTime();
        List<LlmToolDefinition> gameTools = selectedGameTools();
        long reducerMillis = elapsedMillis(reducerStartedNanos);
        List<LlmToolDefinition> sysTools = systemTools(gameTools);
        long timelineStartedNanos = System.nanoTime();
        List<MemoryEntry> timeline = dependencies.memoryGateway().readShortTermTimeline();
        long timelineMillis = elapsedMillis(timelineStartedNanos);
        long factsStartedNanos = System.nanoTime();
        List<Fact> candidates = memoryCandidates();
        long factsMillis = elapsedMillis(factsStartedNanos);
        long promptStartedNanos = System.nanoTime();
        ComposedPrompt composed = dependencies.promptComposer().compose(
                source(), context.matchInput(), gameTools, sysTools, timeline, candidates,
                context.pendingClarification());
        long promptMillis = elapsedMillis(promptStartedNanos);
        // The game-tool count and list are already owned by the reduce line (kept=N -> [...]) and the total sent is
        // owned by the llm request line (tools=N); compose reports only what it adds to the prompt - the system
        // tools, the grounding facts, and the recalled timeline depth - so no tool count is repeated across lines.
        CompanionDiagnostics.debug(trace, "compose",
                "sysTools=" + CompanionDiagnostics.names(sysTools)
                        + " facts=" + candidates.size() + " timeline=" + timeline.size()
                        + " | reduce=" + reducerMillis + " ms"
                        + " timeline=" + timelineMillis + " ms"
                        + " facts=" + factsMillis + " ms"
                        + " prompt=" + promptMillis + " ms"
                        + " total=" + elapsedMillis(composeStartedNanos) + " ms");
        // Show the actual inlined facts (memory core plus source-tagged live facts), one per line as in the prompt,
        // numbered i/total so multiple grounding facts are easy to count and reference.
        int factNo = 0;
        for (Fact fact : candidates) {
            CompanionDiagnostics.debug(trace, "facts",
                    (++factNo) + "/" + candidates.size() + " " + CompanionDiagnostics.fact(fact));
        }
        return composed;
    }

    /**
     * Pre-turn clean answer facts to inline in the prompt (see {@code MergedFactCandidates}). Default
     * none; a COMMANDER thought overrides it. A memory-only or narration thought carries no candidates.
     */
    protected List<Fact> memoryCandidates() {
        return List.of();
    }

    /**
     * The single point where game tools are formed. Normal candidates are reduced from the current input; a
     * claimed clarification target is re-resolved by id against this turn's visibility snapshot and prepended.
     */
    private List<LlmToolDefinition> selectedGameTools() {
        Set<IntelActionCategory> categories = allowedCategories();
        List<LlmToolDefinition> selected = dependencies.reducer().selectTools(
                categories, context.matchInput(), context.semanticQuery(), context.gameStateSnapshot());
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
        // Only game tools (command/query/macro) dump their call+args here; classify_turn / speak / memory_search
        // each have a dedicated, cleaner diagnostic line (classify / settle / memory-search), so their raw call
        // would only be a redundant third copy. A failure is always surfaced, whatever the tool.
        if (dependencies.actionTypeResolver().resolve(inv.name()).isGameAction()) {
            CompanionDiagnostics.debug(trace, "exec", inv.name() + CompanionDiagnostics.args(inv.arguments()));
        }
        if (!firstToolStarted) {
            firstToolStarted = true;
            CompanionDiagnostics.debug(trace, "latency", "time-to-first-tool=" + elapsedSinceAcceptanceMillis() + " ms");
        }
        long executionStartedNanos = System.nanoTime();
        CompletableFuture<JsonObject> future = dependencies.executionGateway()
                .submit(new ExecutionRequest(newId(), inv.name(), inv.arguments(), executionInputFor(inv),
                        dependencies.runtimeGeneration().generationId()));
        future.whenComplete((ignored, failure) -> CompanionDiagnostics.debug(trace, "exec-time",
                inv.name() + "=" + elapsedMillis(executionStartedNanos) + " ms"));
        return future;
    }

    /**
     * Gives a resumed target both the originating order and the terse parameter reply. A superseding action sees
     * only the current words, so handler-level fallback parsing cannot accidentally inherit the abandoned order.
     */
    private String executionInputFor(LlmToolInvocation inv) {
        var pending = context.pendingClarification();
        return pending != null && pending.actionId().equals(inv.name())
                ? pending.originalInput() + "\n" + context.currentInput()
                : context.currentInput();
    }

    /** Runs one tool-call via the execution gateway; a failed call becomes an error result the LLM can read. */
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
     * Records a standalone memory-visible input for an EVENT workflow. Ordinary commander conversation is
     * committed only through {@link #recordDialoguePair(String)}, and QUERY is committed only through
     * {@link #publishCompletedQuery(LlmToolInvocation, JsonObject, String)}, so memory never observes a
     * commander turn before its matching reply/result exists.
     */
    protected void recordCurrentInput() {
        if (!isRuntimeActive()) {
            return;
        }
        String canonical = memoryCanonicalFact();
        // The raw intake is already shown by the intake line; commander memory keeps its STT-corrected wording.
        CompanionDiagnostics.debug(trace, "memory",
                "record input [" + memoryTopic() + "/" + memoryImportance() + "]");
        writeMemory(new MemoryEntry(
                Instant.now(), memoryTopic(), memorySource(), context.memoryInput(), memoryImportance(),
                null, canonical == null || canonical.isBlank() ? null : canonical));
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
        Instant now = Instant.now();
        ConversationTopic topic = memoryTopic();
        MemoryImportance importance = memoryImportance();
        String canonical = memoryCanonicalFact();
        MemoryEntry input = new MemoryEntry(
                now, topic, memorySource(), context.memoryInput(), importance,
                null, canonical == null || canonical.isBlank() ? null : canonical);
        MemoryEntry companion = new MemoryEntry(
                now, topic, MemorySource.COMPANION, reply, MemoryImportance.LOW);
        boolean recorded = publishSettlement(() ->
                dependencies.memoryGateway().writeBatch(List.of(input, companion)));
        if (recorded) {
            CompanionDiagnostics.debug(trace, "memory",
                    "record dialogue pair [" + topic + "/" + importance + "]");
        }
    }

    /**
     * Optional clean one-line restatement of a durable fact stated this turn, used only as the memory
     * candidate/embedding text (the memory-visible input remains the normalized commander wording). Empty by default; a COMMANDER
     * thought supplies it from {@code classify_turn}.
     */
    protected String memoryCanonicalFact() {
        return "";
    }

    /**
     * Records an EVENT thought's completed companion half as its own {@code COMPANION} timeline entry - the spoken
     * text itself, not a {@code {"status":"spoken"}} marker. Commander conversation uses
     * {@link #recordDialoguePair(String)} instead. A blank utterance is not recorded.
     * <p>
     * Stamped {@link MemoryImportance#LOW}, not the turn's importance: the companion's own words are never a
     * durable fact (only the commander's statements and events are). It stays in the recent timeline for
     * continuity but never surfaces as a memory candidate.
     */
    protected void recordCompanionSpeech(String text) {
        if (!isRuntimeActive() || text == null || text.isBlank()) {
            return;
        }
        // The spoken text is already shown by the settle line; log only that the reply was filed to memory.
        CompanionDiagnostics.debug(trace, "memory", "record reply");
        writeMemory(new MemoryEntry(
                Instant.now(), memoryTopic(), MemorySource.COMPANION, text, MemoryImportance.LOW));
    }

    /** The text a {@code speak} invocation carries (the words to vocalize), or empty when absent. */
    protected static String spokenTextOf(LlmToolInvocation speak) {
        return JsonUtils.getAsStringOrEmpty(speak.arguments(), SpeakFunction.PARAM_TEXT);
    }

    /**
     * Records a tool outcome by action type, voicing it directly (no {@code AiVoxResponseEvent} detour - that
     * event is now system-only and the companion owns its own speech). A <b>query</b> answer is carried in the
     * execution result and publishes the retained input plus linked CALL/RESULT as one completed batch via
     * {@link #publishCompletedQuery}.
     * A <b>command</b> declares its outcome in the result too (its {@code execute} return value, wrapped by
     * {@code IntelCommand#handle}), but command execution is not dialogue: its outcome is voiced without entering
     * conversational memory. A <b>macro</b> stays self-narrating (its SPEAK steps carry completion futures); only
     * its failure is voiced here, likewise without a memory write. A failed command/query/macro receives a fixed
     * localized failure reply when its handler provided no own text.
     * {@code SYSTEM} functions leave no timeline entry.
     */
    protected void recordOutcome(LlmToolInvocation inv, JsonObject result, List<LlmToolDefinition> tools,
                                 String toolCallId) {
        if (!isRuntimeActive()) {
            return;
        }
        switch (dependencies.actionTypeResolver().resolve(inv.name())) {
            case QUERY -> {
                publishCompletedQuery(inv, result, toolCallId);
            }
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
            case SYSTEM, UNKNOWN -> { /* no speech, no timeline entry; the result only feeds the flow */ }
        }
    }

    /**
     * Publishes a QUERY only after its non-blank result exists. Input, CALL and RESULT are built first and committed
     * through one gateway batch; an interrupted, cancelled or blank query therefore leaves no conversational-memory
     * trace. The same settlement lock linearizes this publication with {@link #interrupt()}, so a late handler
     * cannot commit after interruption won the race.
     *
     * @return true when the query produced and published a textual answer
     */
    protected boolean publishCompletedQuery(LlmToolInvocation inv, JsonObject result, String toolCallId) {
        if (toolCallId == null || toolCallId.isBlank()) {
            return false;
        }
        String answer = spokenOutcomeText(result);
        if (answer.isBlank()) {
            return false;
        }
        Instant now = Instant.now();
        ConversationTopic topic = memoryTopic();
        MemoryImportance importance = memoryImportance();
        String canonical = memoryCanonicalFact();
        MemoryEntry input = new MemoryEntry(
                now, topic, memorySource(), context.memoryInput(), importance,
                null, canonical == null || canonical.isBlank() ? null : canonical);
        String argumentsJson = GsonFactory.getGson().toJson(inv.arguments());
        MemoryEntry call = new MemoryEntry(
                now, topic, MemorySource.COMPANION, inv.name(), MemoryImportance.LOW,
                null, null, ToolLink.call(toolCallId, inv.name(), argumentsJson));
        MemoryEntry toolResult = new MemoryEntry(
                now, topic, MemorySource.TOOL_RESULT, answer, importance,
                null, null, ToolLink.result(toolCallId));
        List<MemoryEntry> contract = List.of(input, call, toolResult);

        boolean published = publishSettlement(() -> {
            dependencies.memoryGateway().writeBatch(contract);
            voice(answer, false);
        });
        if (published) {
            CompanionDiagnostics.debug(trace, "memory",
                    "record query contract [" + topic + "/" + importance + "] entries=" + contract.size());
        }
        return published;
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
     * Interrupts the thought from another thread (§2.7): raises the interrupt flag and cancels the awaited
     * future so the lane thread unblocks and dies. It never cancels a started action/macro (§1.9.41) and
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

    /** Atomically fences a memory publication against runtime shutdown. */
    protected final boolean writeMemory(MemoryEntry entry) {
        return dependencies.runtimeGeneration().runIfActive(
                () -> dependencies.memoryGateway().write(entry));
    }

    public final ThoughtSource source() {
        return context.source();
    }

    public final Urgency urgency() {
        return context.urgency();
    }

    /** The memory source marker for this thought's own input (COMMANDER vs EVENT). */
    private MemorySource memorySource() {
        return source() == ThoughtSource.COMMANDER ? MemorySource.COMMANDER : MemorySource.EVENT;
    }

    /** Compact JSON of a tool result, for both the tool-result message and the memory entry. */
    protected static String stringify(JsonObject result) {
        return GsonFactory.getGson().toJson(result);
    }

    protected static String newId() {
        return UUID.randomUUID().toString();
    }
}
