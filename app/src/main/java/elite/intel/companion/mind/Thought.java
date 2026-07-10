package elite.intel.companion.mind;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.AIConstants;
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
 * A unit of work of the consciousness: the parts shared by every kind of thought. It holds its turn-scoped
 * input signals and shared collaborators, and provides the building blocks - assembling the prompt,
 * running a single interruptible LLM round, executing a tool-call, and recording the input or the
 * companion's own speech to memory.
 * <p>
 * It owns no thinking loop. Each concrete kind drives its own {@link #run}: {@link CommanderThought} the full
 * tool-calling loop with dangerous-action confirmation, {@link EventThought} a single short round (or a verbatim
 * line) that phrases a subscriber's reaction. There is no per-thought topic field: the memory tag is the global
 * conversation topic for COMMANDER and the subscriber-supplied topic for EVENT (§2.4/§2.5).
 * <p>
 * Threading: {@link #run} executes on a dispatcher lane thread; {@link #interrupt} is called from another
 * thread and cooperates via a volatile flag and by cancelling the awaited future (§2.7).
 */
public abstract class Thought {

    private static final Logger log = LogManager.getLogger(Thought.class);

    /** Monotonic sequence backing the short per-thought {@link #trace} id, so concurrent lanes are told apart in the log. */
    private static final AtomicInteger TRACE_SEQ = new AtomicInteger();

    /** Stable per-thought diagnostic tag ({@code SOURCE#n}), correlating every SYSTEM LOG line of this one thought. */
    private final String trace;
    /** Immutable input-side signals born with this thought; distinct from service-owning {@link #dependencies}. */
    protected final ThoughtContext context;
    protected final ThoughtDependencies dependencies;

    /** Set by {@link #interrupt} from another thread; a run honors it at step boundaries (§2.7). */
    protected volatile boolean interrupted;
    /** The future the lane thread is currently awaiting (LLM round / confirmation wait), or null. */
    protected volatile CompletableFuture<?> inFlight;

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

    // --- factories (the public construction API; each returns the matching concrete kind) ---

    /**
     * Creates a thought from a commander reply. Its memory tag is the live global conversation topic
     * (which a {@code classify_turn} call may move during the thought).
     */
    public static Thought commander(Urgency urgency, String input, ThoughtDependencies dependencies) {
        return commander(ThoughtContext.commander(urgency, input, input), dependencies);
    }

    /**
     * As above, but with a separate canonical {@code matchInput} (e.g. the synonym-normalized form) used for
     * tool selection and as the LLM-visible current input; memory still records the raw {@code input}.
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
     * LLM entirely - it just records the input, executes the resolved command, and voices/remembers its
     * outcome ({@link #recordOutcome}). Anything ambiguous, parameterized or dangerous is never a reflex.
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
    protected List<LlmToolDefinition> systemTools() {
        return List.of();
    }

    /**
     * One LLM round, registered as the interruptible in-flight handle. A provider/transport failure or an
     * interrupt-driven cancellation (exceptional future) is treated as no usable result (§2.9/§2.7).
     */
    protected LlmResult submitRound(List<LlmMessage> flow, List<LlmToolDefinition> tools, PromptCacheProfile profile) {
        CompanionDiagnostics.debug(trace, "llm", "request: tools=" + tools.size() + " messages=" + flow.size());
        CompletableFuture<LlmResult> future = dependencies.llmGateway()
                .submit(new LlmRequest(newId(), List.copyOf(flow), tools, profile, trace));
        inFlight = future;
        if (interrupted) {
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
        List<LlmToolDefinition> gameTools = selectedGameTools();
        List<LlmToolDefinition> sysTools = systemTools();
        List<MemoryEntry> timeline = dependencies.memoryGateway().readShortTermTimeline();
        List<Fact> candidates = memoryCandidates();
        // The game-tool count and list are already owned by the reduce line (kept=N -> [...]) and the total sent is
        // owned by the llm request line (tools=N); compose reports only what it adds to the prompt - the system
        // tools, the grounding facts, and the recalled timeline depth - so no tool count is repeated across lines.
        CompanionDiagnostics.debug(trace, "compose",
                "sysTools=" + CompanionDiagnostics.names(sysTools)
                        + " facts=" + candidates.size() + " timeline=" + timeline.size());
        // Show the actual inlined facts (memory core plus source-tagged live facts), one per line as in the prompt,
        // numbered i/total so multiple grounding facts are easy to count and reference.
        int factNo = 0;
        for (Fact fact : candidates) {
            CompanionDiagnostics.debug(trace, "facts",
                    (++factNo) + "/" + candidates.size() + " " + CompanionDiagnostics.fact(fact));
        }
        return dependencies.promptComposer().compose(source(), context.matchInput(), gameTools, sysTools, timeline, candidates);
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
        return dependencies.reducer().selectTools(allowedCategories(), context.matchInput(), context.semanticQuery());
    }

    /** Runs one tool-call via the execution gateway; a failed call becomes an error result the LLM can read. */
    protected JsonObject execute(LlmToolInvocation inv) {
        // Only game tools (command/query/macro) dump their call+args here; classify_turn / speak / memory_search
        // each have a dedicated, cleaner diagnostic line (classify / settle / memory-search), so their raw call
        // would only be a redundant third copy. A failure is always surfaced, whatever the tool.
        if (dependencies.actionTypeResolver().resolve(inv.name()).isGameAction()) {
            CompanionDiagnostics.debug(trace, "exec", inv.name() + CompanionDiagnostics.args(inv.arguments()));
        }
        try {
            return dependencies.executionGateway()
                    .submit(new ExecutionRequest(newId(), inv.name(), inv.arguments(), context.currentInput()))
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
        // The verbatim input is already shown by the intake line; log only that it was filed and under which stamp.
        CompanionDiagnostics.debug(trace, "memory",
                "record input [" + memoryTopic() + "/" + memoryImportance() + "]");
        dependencies.memoryGateway().write(new MemoryEntry(
                Instant.now(), memoryTopic(), memorySource(), context.currentInput(), memoryImportance(),
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
        // The spoken text is already shown by the settle line; log only that the reply was filed to memory.
        CompanionDiagnostics.debug(trace, "memory", "record reply");
        dependencies.memoryGateway().write(new MemoryEntry(
                Instant.now(), memoryTopic(), MemorySource.COMPANION, text, MemoryImportance.LOW));
    }

    /** The text a {@code speak} invocation carries (the words to vocalize), or empty when absent. */
    protected static String spokenTextOf(LlmToolInvocation speak) {
        return JsonUtils.getAsStringOrEmpty(speak.arguments(), SpeakFunction.PARAM_TEXT);
    }

    /**
     * Records a tool outcome by action type, voicing it directly (no {@code AiVoxResponseEvent} detour - that
     * event is now system-only and the companion owns its own speech). A <b>query</b> answer is carried in the
     * execution result and recorded as this call's tool RESULT (paired with the CALL {@link #recordCall} wrote).
     * A <b>command</b> declares its outcome in the result too (its {@code execute} return value, wrapped by
     * {@code IntelCommand#handle}); a command turn files no call to pair with, so its outcome is remembered as a
     * free-standing companion line. A <b>macro</b> stays
     * self-narrating (its SPEAK steps carry completion futures), so its outcome is not handled here. {@code SYSTEM}
     * functions leave no timeline entry.
     */
    protected void recordOutcome(LlmToolInvocation inv, JsonObject result, List<LlmToolDefinition> tools,
                                 String toolCallId) {
        switch (dependencies.actionTypeResolver().resolve(inv.name())) {
            case QUERY -> {
                String answer = spokenTextOf(result);
                if (!answer.isBlank()) {
                    recordToolResult(toolCallId, answer);  // RESULT half, paired with the recorded CALL
                    voice(answer, false);
                }
            }
            case COMMAND -> {
                String outcome = spokenTextOf(result);
                if (!outcome.isBlank()) {
                    recordCompanionSpeech(outcome);        // free-standing line: a command turn files no call to pair
                    voice(outcome, false);
                }
            }
            case MACRO -> { /* self-narrating: SPEAK steps carry completion futures; handled on their own path */ }
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
        dependencies.memoryGateway().write(new MemoryEntry(
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
        // An over-long answer (e.g. a full system briefing) is handed by the gateway to background gist
        // compression, which re-writes a shorter line carrying this same toolCallId - so the call stays paired
        // once the gist lands (see OversizedMemoryCompressor), rather than being orphaned as "(no textual result)".
        dependencies.memoryGateway().write(new MemoryEntry(
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
        dependencies.speechGateway().submit(new SpeechRequest(newId(), text, critical ? Urgency.URGENT : Urgency.NORMAL));
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
