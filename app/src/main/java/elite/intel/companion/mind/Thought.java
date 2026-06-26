package elite.intel.companion.mind;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.AIConstants;
import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.IntelActionCategory;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.Urgency;
import elite.intel.companion.model.execution.ExecutionRequest;
import elite.intel.companion.model.llm.*;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemorySource;
import elite.intel.companion.model.speech.SpeechRequest;
import elite.intel.companion.prompt.ComposedPrompt;
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

    private final ThoughtSource source;
    private final Urgency urgency;
    protected final String currentInput;
    protected final ThoughtContext ctx;

    /** Set by {@link #interrupt} from another thread; a run honors it at step boundaries (§2.7). */
    protected volatile boolean interrupted;
    /** The future the lane thread is currently awaiting (LLM round / confirmation wait), or null. */
    protected volatile CompletableFuture<?> inFlight;

    protected Thought(ThoughtSource source, Urgency urgency, String currentInput, ThoughtContext ctx) {
        this.source = source;
        this.urgency = urgency;
        this.currentInput = currentInput;
        this.ctx = ctx;
    }

    // --- factories (the public construction API; each returns the matching concrete kind) ---

    /**
     * Creates a thought from a commander reply. Its memory tag is the live global conversation topic
     * (which a {@code change_global_topic} call may move during the thought).
     */
    public static Thought commander(Urgency urgency, String input, ThoughtContext ctx) {
        return new CommanderThought(urgency, input, ctx);
    }

    /**
     * Creates a thought from a filtered game event. Its memory tag is fixed at birth from the static
     * event-type map; an EVENT thought never moves the global conversation topic. It is memory-only: a
     * {@code HIGH} event is recorded, a {@code NORMAL} event is dropped, and the LLM is never engaged.
     */
    public static Thought event(Urgency urgency, String summary, ConversationTopic eventTopic,
                                BaseEvent.Importance importance, ThoughtContext ctx) {
        return new EventThought(urgency, summary, eventTopic, importance, ctx);
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
        CompletableFuture<LlmResult> future = ctx.llmGateway()
                .submit(new LlmRequest(newId(), List.copyOf(flow), tools, profile));
        inFlight = future;
        if (interrupted) {
            future.cancel(true); // interrupt raced ahead of registration: cancel now so join unblocks
        }
        try {
            return future.join();
        } catch (RuntimeException llmFailure) {
            if (!interrupted) {
                // A provider/transport failure (not an interrupt-driven cancel) - surface the cause.
                log.warn("Companion LLM round failed; treating as no usable result", llmFailure);
            }
            return null;
        } finally {
            inFlight = null;
        }
    }

    /** Assembles the seed prompt: reduced game tools + system tools + memory snapshot. */
    protected ComposedPrompt composeInitialPrompt() {
        return ctx.promptComposer().compose(
                source, urgency, ctx.state().globalTopic(), currentInput,
                selectedGameTools(), systemTools(),
                ctx.memoryGateway().readShortTermTimeline(),
                ctx.memoryGateway().indexes(),
                ctx.memoryGateway().longTermSummary());
    }

    /** The single point where game tools are formed: the thought's allowed categories reduced by the input. */
    private List<LlmToolDefinition> selectedGameTools() {
        return ctx.reducer().selectTools(allowedCategories(), currentInput);
    }

    /** Runs one tool-call via the execution gateway; a failed call becomes an error result the LLM can read. */
    protected JsonObject execute(LlmToolInvocation inv) {
        try {
            return ctx.executionGateway()
                    .submit(new ExecutionRequest(newId(), inv.name(), inv.arguments()))
                    .join();
        } catch (RuntimeException failed) {
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

    /** Records the current input under the resolved topic before tool-calls run (§2.6). */
    protected void recordCurrentInput() {
        ctx.memoryGateway().write(new MemoryEntry(
                Instant.now(), memoryTopic(), memorySource(), currentInput));
    }

    /**
     * Records what the companion actually said as its own {@code COMPANION} timeline entry - the spoken text
     * itself, not a {@code {"status":"spoken"}} ack - so a future thought (which reads the past only through
     * memory) knows it already answered. A blank utterance is not recorded.
     */
    protected void recordCompanionSpeech(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        ctx.memoryGateway().write(new MemoryEntry(
                Instant.now(), memoryTopic(), MemorySource.COMPANION, text));
    }

    /** The text a {@code speak} invocation carries (the words to vocalize), or empty when absent. */
    protected static String spokenTextOf(LlmToolInvocation speak) {
        return JsonUtils.getAsStringOrEmpty(speak.arguments(), SpeakFunction.PARAM_TEXT);
    }

    /**
     * Records a tool outcome by action type. Commands are self-narrating after the command-outcome revert, so
     * blank command results are remembered but not acknowledged here. A query answer is self-narrating too: it
     * is published as an {@link AiVoxResponseEvent} (mirroring the legacy router), so the companion's
     * {@code CompanionAnnouncementBridge} voices and remembers it via a verbatim narration - it is not voiced
     * or recorded here.
     */
    protected void recordOutcome(LlmToolInvocation inv, JsonObject result, List<LlmToolDefinition> tools) {
        String text = spokenTextOf(result);
        switch (ctx.actionTypeResolver().resolve(inv.name())) {
            case COMMAND -> {
                if (text.isBlank()) {
                    rememberAction("command " + inv.name() + " executed", description(inv.name(), tools));
                } else {
                    voice(text, isMissionCritical(result));
                    rememberAction("command " + inv.name() + " executed", text);
                }
            }
            case QUERY -> {
                // Self-narrating: the answer rides the AiVoxResponseEvent path and is owned by the bridge.
                if (!text.isBlank()) {
                    GameEventBus.publish(new AiVoxResponseEvent(text));
                }
            }
            case MACRO -> rememberAction("macro " + inv.name() + " executed", description(inv.name(), tools));
            case SYSTEM, UNKNOWN -> { /* no speech, no timeline entry; the result only feeds the flow */ }
        }
    }

    /** The handler-provided spoken text in a tool result, or empty when absent. */
    protected static String spokenTextOf(JsonObject result) {
        return JsonUtils.getAsStringOrEmpty(result, AIConstants.PROPERTY_TEXT_TO_SPEECH_RESPONSE);
    }

    /** Backward-compatible mission-critical marker for any remaining structured tool result. */
    private static boolean isMissionCritical(JsonObject result) {
        return result != null
                && result.has("mission_critical")
                && result.get("mission_critical").isJsonPrimitive()
                && result.get("mission_critical").getAsBoolean();
    }

    /** Voices a non-blank phrase through the speech gateway (mission-critical -> urgent/preempting channel). */
    protected void voice(String text, boolean critical) {
        if (text == null || text.isBlank()) {
            return;
        }
        ctx.speechGateway().submit(new SpeechRequest(newId(), text, critical ? Urgency.URGENT : Urgency.NORMAL));
    }

    /** The description shown to the LLM for a tool id (its {@code llmDescription} / fallback), or empty. */
    protected static String description(String id, List<LlmToolDefinition> tools) {
        return tools.stream().filter(tool -> id.equals(tool.name())).findFirst()
                .map(LlmToolDefinition::description).orElse("");
    }

    /** A compact timeline entry ("lead" + optional detail) as TOOL_RESULT - no raw {@code {data:...}}. */
    protected void rememberAction(String lead, String detail) {
        String content = detail == null || detail.isBlank() ? lead : lead + ": " + detail;
        ctx.memoryGateway().write(new MemoryEntry(
                Instant.now(), memoryTopic(), MemorySource.TOOL_RESULT, content));
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
