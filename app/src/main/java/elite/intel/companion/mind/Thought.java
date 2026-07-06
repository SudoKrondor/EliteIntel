package elite.intel.companion.mind;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.AIConstants;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.companion.diag.CompanionDiagnostics;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.IntelActionCategory;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.Urgency;
import elite.intel.companion.execution.ActiveToolCall;
import elite.intel.companion.model.execution.ExecutionRequest;
import elite.intel.companion.model.llm.*;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemoryImportance;
import elite.intel.companion.model.memory.MemorySource;
import elite.intel.companion.model.memory.ToolLink;
import elite.intel.companion.model.speech.SpeechRequest;
import elite.intel.companion.prompt.ComposedPrompt;
import elite.intel.companion.prompt.Fact;
import elite.intel.companion.tools.SpeakFunction;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.journal.events.BaseEvent;
import elite.intel.util.StringUtls;
import elite.intel.util.json.GsonFactory;
import elite.intel.util.json.JsonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A unit of work of the consciousness: the parts shared by every kind of thought. It holds the source,
 * urgency, current input and collaborators, and provides the building blocks - assembling the prompt,
 * running a single interruptible LLM round, executing a tool-call, and recording the input or the
 * companion's own speech to memory.
 * <p>
 * It owns no thinking loop. Each concrete kind drives its own {@link #run}: {@link CommanderThought} the full
 * tool-calling loop with dangerous-action confirmation, {@link NarrationThought} a single short round,
 * {@link EventThought} a memory-only write. There is no per-thought topic field: the memory tag is the global
 * conversation topic for COMMANDER and the event's static topic for EVENT/narration (§2.4/§2.5).
 * <p>
 * Threading: {@link #run} executes on a dispatcher lane thread; {@link #interrupt} is called from another
 * thread and cooperates via a volatile flag and by cancelling the awaited future (§2.7).
 */
public abstract class Thought {

    private static final Logger log = LogManager.getLogger(Thought.class);

    /** Monotonic sequence backing the short per-thought {@link #trace} id, so concurrent lanes are told apart in the log. */
    private static final AtomicInteger TRACE_SEQ = new AtomicInteger();

    private final ThoughtSource source;
    private final Urgency urgency;
    /** Stable per-thought diagnostic tag ({@code SOURCE#n}), correlating every SYSTEM LOG line of this one thought. */
    private final String trace;
    protected final String currentInput;
    /**
     * Canonical form of {@link #currentInput} used only for command matching (the reducer) and as the
     * LLM-visible current input - this is what lets a normalized synonym ("combat mode" -> "switch to combat
     * mode") steer tool selection. Memory always records the raw {@link #currentInput}, never this. Defaults
     * to the raw input when no separate canonical form is supplied.
     */
    protected final String matchInput;
    protected final ThoughtContext ctx;

    /** Set by {@link #interrupt} from another thread; a run honors it at step boundaries (§2.7). */
    protected volatile boolean interrupted;
    /** The future the lane thread is currently awaiting (LLM round / confirmation wait), or null. */
    protected volatile CompletableFuture<?> inFlight;

    protected Thought(ThoughtSource source, Urgency urgency, String currentInput, ThoughtContext ctx) {
        this(source, urgency, currentInput, currentInput, ctx);
    }

    /** As above, but with a separate canonical {@link #matchInput} for command matching / the LLM prompt. */
    protected Thought(ThoughtSource source, Urgency urgency, String currentInput, String matchInput, ThoughtContext ctx) {
        this.source = source;
        this.urgency = urgency;
        this.currentInput = currentInput;
        this.matchInput = matchInput;
        this.ctx = ctx;
        this.trace = source + "#" + TRACE_SEQ.incrementAndGet();
    }

    /** The per-thought diagnostic tag ({@code SOURCE#n}); every {@link CompanionDiagnostics} line of this thought carries it. */
    public final String trace() {
        return trace;
    }

    // --- factories (the public construction API; each returns the matching concrete kind) ---

    /**
     * Creates a thought from a commander reply. Its memory tag is the live global conversation topic
     * (which a {@code classify_turn} call may move during the thought).
     */
    public static Thought commander(Urgency urgency, String input, ThoughtContext ctx) {
        return commander(urgency, input, input, ctx);
    }

    /**
     * As above, but with a separate canonical {@code matchInput} (e.g. the synonym-normalized form) used for
     * tool selection and as the LLM-visible current input; memory still records the raw {@code input}.
     */
    public static Thought commander(Urgency urgency, String input, String matchInput, ThoughtContext ctx) {
        return new CommanderThought(urgency, input, matchInput, ctx);
    }

    /**
     * Creates a thought from a filtered game event. Its memory tag is fixed at birth from the static
     * event-type map; an EVENT thought never moves the global conversation topic. It is memory-only: the
     * event's readable {@code summary} ({@code memorySummary()}) is recorded if non-blank, otherwise nothing,
     * and the LLM is never engaged.
     */
    public static Thought event(Urgency urgency, String summary, ConversationTopic eventTopic,
                                ThoughtContext ctx) {
        return new EventThought(urgency, summary, eventTopic, ctx);
    }

    /**
     * Creates a thought from subscriber-prepared sensor narration. The subscriber layer already decided this
     * is worth saying, so this kind skips game tools; the LLM's only job is to phrase the provided data.
     */
    public static Thought sensorNarration(Urgency urgency, String summary, ConversationTopic eventTopic, ThoughtContext ctx) {
        return new NarrationThought(urgency, summary, eventTopic, ctx);
    }

    /**
     * Creates a thought from a curated announcement that already carries finished text. It is voiced verbatim
     * (no LLM phrasing) in the companion's voice and recorded as the companion's own words under the topic.
     */
    public static Thought verbatimNarration(Urgency urgency, String text, ConversationTopic topic, ThoughtContext ctx) {
        return new VerbatimNarrationThought(urgency, text, topic, ctx);
    }

    /**
     * Verbatim narration whose {@code spokenSignal} is completed when the companion's playback finishes, for a
     * synchronous caller (e.g. a bridged macro SPEAK step) that blocks until the line is actually spoken.
     */
    public static Thought verbatimNarration(Urgency urgency, String text, ConversationTopic topic,
                                            ThoughtContext ctx, java.util.concurrent.CompletableFuture<Void> spokenSignal) {
        return new VerbatimNarrationThought(urgency, text, topic, ctx, spokenSignal);
    }

    /**
     * Verbatim narration that is the voiced outcome of a model tool-call: it is recorded as that call's tool
     * result (linked by {@code toolCallId}), so the timeline replays it as the RESULT half of an
     * {@code assistant(tool_calls) -> tool} pair rather than as free-standing companion speech.
     */
    public static Thought verbatimNarration(Urgency urgency, String text, ConversationTopic topic,
                                            ThoughtContext ctx, java.util.concurrent.CompletableFuture<Void> spokenSignal,
                                            String toolCallId) {
        return new VerbatimNarrationThought(urgency, text, topic, ctx, spokenSignal, toolCallId);
    }

    /**
     * Creates a reflex thought: a commander input the {@code ReflexResolver} matched verbatim to exactly one
     * safe, parameterless command. It runs on the commander lane like a {@link CommanderThought} but skips the
     * LLM entirely - it just records the input, executes the resolved command, and voices/remembers its
     * outcome ({@link #recordOutcome}). Anything ambiguous, parameterized or dangerous is never a reflex.
     */
    public static Thought reflex(Urgency urgency, String input, String commandId, ThoughtContext ctx) {
        return new ReflexThought(urgency, input, commandId, ctx);
    }

    /** Runs this thought on the lane thread. Each concrete kind drives its own lifecycle. */
    public abstract void run();

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
     * access policy's categories for this thought's source (COMMANDER -> QUERY/ACTION/MACRO, EVENT -> QUERY).
     * A subclass narrows it: {@link NarrationThought} returns none, so the one reducer call offers no game
     * tools (the subscriber already calculated and filtered the data to narrate).
     */
    protected Set<IntelActionCategory> allowedCategories() {
        return ctx.intelActionAccessPolicy().allowedCategories(source);
    }

    /**
     * System tools offered to the LLM for this thought. An LLM-driven thought overrides this; a memory-only
     * thought never composes a prompt, so it inherits the empty default.
     */
    protected List<LlmToolDefinition> systemTools() {
        return List.of();
    }

    /**
     * One LLM round, registered as the interruptible in-flight handle. A provider/transport failure or an
     * interrupt-driven cancellation (exceptional future) is treated as no usable result (§2.9/§2.7).
     */
    protected LlmResult submitRound(List<LlmMessage> flow, List<LlmToolDefinition> tools, PromptCacheProfile profile) {
        CompanionDiagnostics.debug(trace, "llm", "request: tools=" + tools.size() + " messages=" + flow.size());
        CompletableFuture<LlmResult> future = ctx.llmGateway()
                .submit(new LlmRequest(newId(), List.copyOf(flow), tools, profile));
        inFlight = future;
        if (interrupted) {
            future.cancel(true); // interrupt raced ahead of registration: cancel now so join unblocks
        }
        try {
            LlmResult result = future.join();
            CompanionDiagnostics.debug(trace, "llm", describeResult(result));
            return result;
        } catch (RuntimeException llmFailure) {
            if (!interrupted) {
                // A provider/transport failure (not an interrupt-driven cancel) - surface the cause.
                log.warn("Companion LLM round failed; treating as no usable result", llmFailure);
            }
            CompanionDiagnostics.debug(trace, "llm", interrupted ? "response: cancelled" : "response: failed");
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
        String dropped = result.droppedText();
        if (dropped != null && !dropped.isBlank()) {
            sb.append(" | dropped-text=").append(dropped.length())
                    .append(": \"").append(CompanionDiagnostics.truncate(dropped)).append("\"");
        } else {
            sb.append(" | dropped-text=0");
        }
        return sb.toString();
    }

    /** Assembles the seed prompt: reduced game tools + system tools + memory snapshot + answer candidates. */
    protected ComposedPrompt composeInitialPrompt() {
        List<LlmToolDefinition> gameTools = selectedGameTools();
        List<LlmToolDefinition> sysTools = systemTools();
        List<MemoryEntry> timeline = ctx.memoryGateway().readShortTermTimeline();
        List<Fact> candidates = memoryCandidates();
        CompanionDiagnostics.debug(trace, "compose",
                "gameTools=" + CompanionDiagnostics.names(gameTools)
                        + " sysTools=" + CompanionDiagnostics.names(sysTools)
                        + " facts=" + candidates.size() + " timeline=" + timeline.size());
        // Show the actual inlined facts (memory core plus source-tagged live facts), one per line as in the prompt,
        // so the log reveals what grounded the turn, not just the count.
        for (Fact fact : candidates) {
            CompanionDiagnostics.debug(trace, "facts", CompanionDiagnostics.fact(fact));
        }
        return ctx.promptComposer().compose(source, matchInput, gameTools, sysTools, timeline, candidates);
    }

    /**
     * Pre-turn clean answer facts to inline in the prompt (see {@code MergedFactCandidates}). Default
     * none; a COMMANDER thought overrides it. A memory-only or narration thought carries no candidates.
     */
    protected List<Fact> memoryCandidates() {
        return List.of();
    }

    /** The single point where game tools are formed: the thought's allowed categories reduced by the input. */
    private List<LlmToolDefinition> selectedGameTools() {
        return ctx.reducer().selectTools(allowedCategories(), matchInput);
    }

    /** Runs one tool-call via the execution gateway; a failed call becomes an error result the LLM can read. */
    protected JsonObject execute(LlmToolInvocation inv) {
        return execute(inv, null);
    }

    /**
     * As {@link #execute(LlmToolInvocation)}, but tags the run with the tool-call id, so a command handler's
     * own narration (emitted during {@code handle()} on the lane thread) is recorded as this call's tool result
     * (see {@link ActiveToolCall}). A {@code null} id runs with no pairing (system functions, plain reflexes).
     */
    protected JsonObject execute(LlmToolInvocation inv, String toolCallId) {
        // Only game tools (command/query/macro) dump their call+args here; classify_turn / speak / memory_search
        // each have a dedicated, cleaner diagnostic line (classify / settle / memory-search), so their raw call
        // would only be a redundant third copy. A failure is always surfaced, whatever the tool.
        if (ctx.actionTypeResolver().resolve(inv.name()).isGameAction()) {
            CompanionDiagnostics.debug(trace, "exec", inv.name() + CompanionDiagnostics.args(inv.arguments()));
        }
        try {
            return ctx.executionGateway()
                    .submit(new ExecutionRequest(newId(), inv.name(), inv.arguments(), toolCallId))
                    .join();
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

    /** Records the current input (verbatim ground truth) under the resolved topic before tool-calls run (§2.6). */
    protected void recordCurrentInput() {
        String canonical = memoryCanonicalFact();
        CompanionDiagnostics.debug(trace, "memory",
                "record input [" + memoryTopic() + "/" + memoryImportance() + "]: \"" + CompanionDiagnostics.truncate(currentInput) + "\"");
        ctx.memoryGateway().write(new MemoryEntry(
                Instant.now(), memoryTopic(), memorySource(), currentInput, memoryImportance(),
                null, canonical == null || canonical.isBlank() ? null : canonical));
    }

    /**
     * Optional clean one-line restatement of a durable fact stated this turn, used only as the memory
     * candidate/embedding text (the verbatim input stays the ground truth). Empty by default; a COMMANDER
     * thought supplies it from {@code classify_turn}.
     */
    protected String memoryCanonicalFact() {
        return "";
    }

    /**
     * Records what the companion actually said as its own {@code COMPANION} timeline entry - the spoken text
     * itself, not a {@code {"status":"spoken"}} ack - so a future thought (which reads the past only through
     * memory) knows it already answered. A blank utterance is not recorded.
     * <p>
     * Stamped {@link MemoryImportance#LOW}, not the turn's importance: the companion's own words are never a
     * durable fact (only the commander's statements and events are). It stays in the recent timeline for
     * continuity but never surfaces as a memory candidate, so a fact turn's reply/acknowledgement ("understood,
     * noted...") cannot pollute recall.
     */
    protected void recordCompanionSpeech(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        CompanionDiagnostics.debug(trace, "memory", "record reply: \"" + CompanionDiagnostics.truncate(text) + "\"");
        ctx.memoryGateway().write(new MemoryEntry(
                Instant.now(), memoryTopic(), MemorySource.COMPANION, text, MemoryImportance.LOW));
    }

    /** The text a {@code speak} invocation carries (the words to vocalize), or empty when absent. */
    protected static String spokenTextOf(LlmToolInvocation speak) {
        return JsonUtils.getAsStringOrEmpty(speak.arguments(), SpeakFunction.PARAM_TEXT);
    }

    /**
     * Records a tool outcome by action type as the RESULT half of a replayed {@code assistant(tool_calls) ->
     * tool} pair (its CALL half is written by {@link #recordCall} before the call runs). Commands and macros
     * are self-narrating: the handler voices its own outcome, which the {@code CompanionAnnouncementBridge}
     * records as this call's tool result via the active tool-call id the execution gateway set around
     * {@code handle()} - so nothing is written here (a silent command leaves the call resultless; the composer
     * synthesizes one so the pair stays valid). A query answer is self-narrating too: it is published as an
     * {@link AiVoxResponseEvent} with the active id set, so the bridge records the answer as this call's tool
     * result rather than free-standing companion speech.
     */
    protected void recordOutcome(LlmToolInvocation inv, JsonObject result, List<LlmToolDefinition> tools,
                                 String toolCallId) {
        switch (ctx.actionTypeResolver().resolve(inv.name())) {
            case COMMAND, MACRO -> { /* handler-voiced; recorded as this call's RESULT via ActiveToolCall */ }
            case QUERY -> {
                // WHY publish instead of calling the dispatcher directly: the CompanionAnnouncementBridge is
                // the single owner of verbatim narration (topic tagging + completion-future handling), so the
                // answer converges with command/macro narration on one path, mirroring the legacy router.
                // The active tool-call id makes the bridge record it as this call's RESULT, not loose speech.
                String answer = spokenTextOf(result);
                if (!answer.isBlank()) {
                    ActiveToolCall.runWith(toolCallId, () -> GameEventBus.publish(new AiVoxResponseEvent(answer)));
                }
            }
            case SYSTEM, UNKNOWN -> { /* no speech, no timeline entry; the result only feeds the flow */ }
        }
    }

    /**
     * Records the model's tool-call as a {@code COMPANION} entry carrying a {@link ToolLink.Kind#CALL} link, so
     * the timeline replays as an {@code assistant(tool_calls)} message paired with its {@code tool} result.
     * Written before the call runs (LOW importance: a call is bookkeeping, never a durable fact).
     */
    protected void recordCall(String toolCallId, LlmToolInvocation inv) {
        String argumentsJson = GsonFactory.getGson().toJson(inv.arguments());
        ctx.memoryGateway().write(new MemoryEntry(
                Instant.now(), memoryTopic(), MemorySource.COMPANION, inv.name(), MemoryImportance.LOW,
                null, null, ToolLink.call(toolCallId, inv.name(), argumentsJson)));
    }

    /**
     * Records a tool result as a {@code TOOL_RESULT} entry linked to its call by {@code toolCallId} - the RESULT
     * half of the replayed pair. A blank result is not recorded (the composer synthesizes one if the call has none).
     */
    protected void recordToolResult(String toolCallId, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        ctx.memoryGateway().write(new MemoryEntry(
                Instant.now(), memoryTopic(), MemorySource.TOOL_RESULT, text, memoryImportance(),
                null, null, ToolLink.result(toolCallId)));
    }

    /** The handler-provided spoken text in a tool result, or empty when absent. */
    protected static String spokenTextOf(JsonObject result) {
        return JsonUtils.getAsStringOrEmpty(result, AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE);
    }

    /** Voices a non-blank phrase through the speech gateway (mission-critical -> urgent/preempting channel). */
    protected void voice(String text, boolean critical) {
        if (text == null || text.isBlank()) {
            return;
        }
        CompanionDiagnostics.debug(trace, "voice", (critical ? "urgent " : "") + "\"" + CompanionDiagnostics.truncate(text) + "\"");
        ctx.speechGateway().submit(new SpeechRequest(newId(), text, critical ? Urgency.URGENT : Urgency.NORMAL));
    }

    /**
     * Interrupts the thought from another thread (§2.7): raises the interrupt flag and cancels the awaited
     * future so the lane thread unblocks and dies. It never cancels a started action/macro (§1.9.41) and
     * writes no memory itself - the owning thread owns any safe-flush.
     */
    public final void interrupt() {
        interrupted = true;
        CompletableFuture<?> current = inFlight;
        if (current != null) {
            current.cancel(true);
        }
    }

    public final ThoughtSource source() {
        return source;
    }

    public final Urgency urgency() {
        return urgency;
    }

    /** The memory source marker for this thought's own input (COMMANDER vs EVENT). */
    private MemorySource memorySource() {
        return source == ThoughtSource.COMMANDER ? MemorySource.COMMANDER : MemorySource.EVENT;
    }

    /** Compact JSON of a tool result, for both the tool-result message and the memory entry. */
    protected static String stringify(JsonObject result) {
        return GsonFactory.getGson().toJson(result);
    }

    protected static String newId() {
        return UUID.randomUUID().toString();
    }
}
