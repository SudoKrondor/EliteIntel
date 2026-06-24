package elite.intel.companion.input;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.CommandRegistry;
import elite.intel.ai.brain.actions.query.QueryRegistry;
import elite.intel.ai.brain.inference.lmstudio.LMStudioClient;
import elite.intel.companion.CompanionRuntime;
import elite.intel.companion.execution.ExecutionGateway;
import elite.intel.companion.llm.CompanionLlmGateway;
import elite.intel.companion.llm.LlmGateway;
import elite.intel.companion.llm.LlmTransport;
import elite.intel.companion.llm.LmStudioLlmAdapter;
import elite.intel.companion.memory.MemoryGateway;
import elite.intel.companion.mind.CompanionState;
import elite.intel.companion.mind.ThoughtDispatcher;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.tools.SystemFunction;
import elite.intel.companion.tools.SystemFunctionRegistry;
import elite.intel.db.util.Database;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.UserInputEvent;
import elite.intel.gameapi.journal.events.BaseEvent;
import elite.intel.session.SystemSession;
import elite.intel.util.Cypher;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Shared boot and driving harness for the opt-in ({@code @Tag("local-integration")}) companion evals.
 * Lives in {@code companion.input} so it can use the package-private {@link CompanionSubsystemGate} test
 * seam, and exposes a language-agnostic public surface so the per-theme, per-language eval classes (e.g.
 * {@code companion.input.en.*}) only supply the conversation script and the assertions.
 * <p>
 * It boots the real subsystem (DB, registries, dispatcher, lanes, memory, state) the production way - input
 * over {@code GameEventBus} - with two seams: a recording execution gateway (game commands are recorded,
 * never executed; system functions run) and a latency-tracing LLM transport. Turn boundaries come from the
 * real {@link ThoughtDispatcher#isIdle()}. Each turn's executed tool-calls are captured for scoring.
 */
public final class CompanionEvalHarness {

    private static final long TURN_TIMEOUT_MS = 90_000;
    private static final long POLL_MS = 150;

    /** One executed tool-call captured from the recording gateway: name, arguments, and the result returned. */
    public record Executed(String tool, JsonObject args, JsonObject result) {}

    private final Path traceFile;
    private final List<Executed> turnCalls = new CopyOnWriteArrayList<>();
    private final List<Long> latenciesMs = new CopyOnWriteArrayList<>();
    private final AtomicLong rounds = new AtomicLong();

    private CompanionSubsystemGate gate;
    private ThoughtDispatcher dispatcher;
    private MemoryGateway memory;
    private CompanionState state;
    private String model;

    /** @param traceFileName the file name written under {@code build/} for this eval's trace. */
    public CompanionEvalHarness(String traceFileName) {
        this.traceFile = Paths.get("build", traceFileName).toAbsolutePath();
    }

    /** Boots the full companion subsystem and starts a fresh trace file. */
    public void boot() throws Exception {
        Cypher.initializeKey();
        Database.init();
        CommandRegistry.getInstance().load();
        QueryRegistry.getInstance().load();
        SystemFunctionRegistry registry = SystemFunctionRegistry.getInstance();
        if (registry.byId().isEmpty()) {
            registry.load();
        }
        Map<String, SystemFunction> systemFunctions = registry.byId();

        model = SystemSession.getInstance().getLmStudioCommandModel().trim();
        LlmTransport tracing = body -> {
            long t0 = System.nanoTime();
            JsonObject response = LMStudioClient.getInstance().sendJsonRequest(body);
            latenciesMs.add((System.nanoTime() - t0) / 1_000_000);
            rounds.incrementAndGet();
            return response;
        };
        LlmGateway llm = new CompanionLlmGateway(new LmStudioLlmAdapter(model), tracing);

        // Recording execution: game commands recorded but never executed (no keystrokes); system functions
        // (speak, recall, remember, change_global_topic, ...) run so memory/topic/verbosity/speech evolve.
        ExecutionGateway recordingExecution = request -> {
            SystemFunction fn = systemFunctions.get(request.toolName());
            JsonObject result = null;
            if (fn != null) {
                try {
                    result = fn.handle(request.toolName(), request.arguments(), "");
                } catch (Exception ignored) {
                    // a system-function failure in the eval must not abort the turn
                }
            }
            if (result == null) {
                result = new JsonObject();
                result.addProperty("status", fn != null ? "ok" : "recorded");
            }
            turnCalls.add(new Executed(request.toolName(), request.arguments(), result));
            return CompletableFuture.completedFuture(result);
        };

        gate = new CompanionSubsystemGate(llm, recordingExecution);
        gate.start();
        dispatcher = gate.dispatcher();
        memory = CompanionRuntime.memory();
        state = CompanionRuntime.state();

        Files.createDirectories(traceFile.getParent());
        Files.writeString(traceFile, "Companion eval - " + Instant.now() + " (model=" + model + ")\n",
                StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        Thread.sleep(1500);
    }

    /** Stops the subsystem; safe to call when never booted. */
    public void shutdown() {
        if (gate != null) {
            gate.stop();
        }
    }

    // --- driving the real system over the bus ---

    /** Speaks a commander phrase the production way and waits for the turn to settle. */
    public void say(String input) throws Exception {
        beginTurn();
        GameEventBus.publish(new UserInputEvent(input));
        awaitIdle();
    }

    /** Publishes a filtered game event the production way and waits for the turn to settle. */
    public void gameEvent(String type, String summary) throws Exception {
        beginTurn();
        GameEventBus.publish(gameEventOf(type, summary));
        awaitIdle();
    }

    /** Publishes a commander phrase without waiting - for real-time stream / keep-up measurement. */
    public void publishInput(String input) {
        GameEventBus.publish(new UserInputEvent(input));
    }

    /** Publishes a game event without waiting - for real-time stream / keep-up measurement. */
    public void publishEvent(String type, String summary) {
        GameEventBus.publish(gameEventOf(type, summary));
    }

    /** Clears the per-turn capture; call before driving input that should be scored in isolation. */
    public void beginTurn() {
        turnCalls.clear();
    }

    /** Blocks until both lanes are idle (the real turn-boundary signal) or the per-turn timeout elapses. */
    public void awaitIdle() throws InterruptedException {
        long deadline = System.currentTimeMillis() + TURN_TIMEOUT_MS;
        while (!dispatcher.isIdle() && System.currentTimeMillis() < deadline) {
            Thread.sleep(POLL_MS);
        }
    }

    // --- per-turn capture / scoring helpers ---

    /** All tool-calls executed during the current turn. */
    public List<Executed> turnCalls() {
        return List.copyOf(turnCalls);
    }

    /** Executed tool-calls of the given name this turn. */
    public List<Executed> callsNamed(String tool) {
        return turnCalls.stream().filter(c -> c.tool().equals(tool)).toList();
    }

    /** Whether a tool of the given name was called this turn. */
    public boolean called(String tool) {
        return !callsNamed(tool).isEmpty();
    }

    /** The text passed to every speak call this turn. */
    public List<String> spokenTexts() {
        return callsNamed("speak").stream()
                .filter(c -> c.args().has("text") && !c.args().get("text").isJsonNull())
                .map(c -> c.args().get("text").getAsString())
                .toList();
    }

    /** Whether any spoken phrase this turn contains the token (case-insensitive). */
    public boolean spokenContains(String token) {
        String needle = token.toLowerCase(Locale.ROOT);
        return spokenTexts().stream().anyMatch(s -> s.toLowerCase(Locale.ROOT).contains(needle));
    }

    // --- memory-tier scoring (shared by the memory evals) ---

    /** Where a fact (matched by a distinctive lowercase token) currently lives across the memory tiers. */
    public String locateTier(String token) {
        String tok = token.toLowerCase(Locale.ROOT);
        List<String> tiers = new ArrayList<>();
        if (memory.readShortTermTimeline().stream().anyMatch(e -> contains(e.content(), tok))) {
            tiers.add("short-term");
        }
        String midTopic = midTermTopic(tok);
        if (midTopic != null) {
            tiers.add("mid-term[" + midTopic + "]");
        }
        if (memory.readLlmMemory().stream().anyMatch(s -> contains(s, tok))) {
            tiers.add("llm_memory");
        }
        String summary = memory.longTermSummary();
        if (summary != null && contains(summary, tok)) {
            tiers.add("long-term");
        }
        return tiers.isEmpty() ? "LOST" : String.join("+", tiers);
    }

    /** The mid-term topic id (lowercase enum name) whose memory holds the token, or null. */
    public String midTermTopic(String token) {
        String tok = token.toLowerCase(Locale.ROOT);
        for (ConversationTopic topic : memory.indexes().topicsWithMemory()) {
            if (memory.recallTopicMemory(topic, null, 100).stream().anyMatch(e -> contains(e.content(), tok))) {
                return topic.name().toLowerCase(Locale.ROOT);
            }
        }
        return null;
    }

    /** Whether the model called search_in_memory this turn. */
    public boolean recalled() {
        return called("search_in_memory");
    }

    /** The query passed to the first search_in_memory call this turn, or empty when it was not called. */
    public String recalledQuery() {
        List<Executed> recalls = callsNamed("search_in_memory");
        return recalls.isEmpty() ? "" : str(recalls.get(0).args(), "query");
    }

    /** The items returned by the first search_in_memory call this turn (the recall result), or empty. */
    public List<String> recallResult() {
        List<Executed> recalls = callsNamed("search_in_memory");
        if (recalls.isEmpty() || !recalls.get(0).result().has("items")) {
            return List.of();
        }
        List<String> items = new ArrayList<>();
        recalls.get(0).result().getAsJsonArray("items").forEach(e -> items.add(e.getAsString()));
        return items;
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }

    private static boolean contains(String haystack, String needleLower) {
        return haystack != null && haystack.toLowerCase(Locale.ROOT).contains(needleLower);
    }

    public MemoryGateway memory() {
        return memory;
    }

    public CompanionState state() {
        return state;
    }

    public List<Long> latencies() {
        return List.copyOf(latenciesMs);
    }

    public long roundCount() {
        return rounds.get();
    }

    public String model() {
        return model;
    }

    // --- trace ---

    /** Appends a block to the eval's trace file and echoes it to stdout. */
    public void trace(String block) throws Exception {
        System.out.print(block);
        Files.writeString(traceFile, block, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }

    public Path traceFile() {
        return traceFile;
    }

    /** A minimal game event carrying a type and a one-line English summary (journal-style {@code BaseEvent}). */
    private static BaseEvent gameEventOf(String type, String summary) {
        return new BaseEvent(Instant.now().toString(), Duration.ofMinutes(1), type) {
            @Override public String getEventType() {
                return type;
            }
            @Override public String toJson() {
                JsonObject o = new JsonObject();
                o.addProperty("event", type);
                o.addProperty("detail", summary);
                return o.toString();
            }
            @Override public JsonObject toJsonObject() {
                JsonObject o = new JsonObject();
                o.addProperty("event", type);
                o.addProperty("detail", summary);
                return o;
            }
        };
    }
}
