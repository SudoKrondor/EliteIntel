package elite.intel.companion.input;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.command.CommandRegistry;
import elite.intel.ai.brain.actions.handlers.QueryHandlerFactory;
import elite.intel.ai.brain.actions.query.IntelQuery;
import elite.intel.ai.brain.actions.query.QueryRegistry;
import elite.intel.ai.brain.inference.lmstudio.LMStudioClient;
import elite.intel.companion.CompanionConfig;
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
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.tools.SystemFunction;
import elite.intel.companion.tools.SystemFunctionRegistry;
import elite.intel.db.util.Database;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.UserInputEvent;
import elite.intel.gameapi.journal.events.BaseEvent;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.util.Cypher;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
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

    private static final Gson TRACE_JSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    private static final long TURN_TIMEOUT_MS = 90_000;
    private static final long POLL_MS = 150;

    /** One executed tool-call captured from the recording gateway: name, arguments, and the result returned. */
    public record Executed(String tool, JsonObject args, JsonObject result) {}

    private final Path traceFile;
    private final Language language;
    private final List<Executed> turnCalls = new CopyOnWriteArrayList<>();
    private final List<Long> latenciesMs = new CopyOnWriteArrayList<>();
    private final AtomicLong rounds = new AtomicLong();
    // Identities of short-term entries already reported, so each memoryDeltaBlock() shows only new writes.
    private final Set<String> seenMemory = new HashSet<>();

    private CompanionSubsystemGate gate;
    private ThoughtDispatcher dispatcher;
    private MemoryGateway memory;
    private CompanionState state;
    private String model;
    private Language previousLanguage;

    /** @param traceFileName the file name written under {@code build/} for this eval's trace. */
    public CompanionEvalHarness(String traceFileName) {
        this(traceFileName, Language.EN);
    }

    /**
     * Creates an eval harness pinned to the requested UI/AI language. The language is set before the
     * companion graph boots, so the system prompt and localized tool aliases match the scripted input.
     */
    public CompanionEvalHarness(String traceFileName, Language language) {
        this.traceFile = Paths.get("build", traceFileName).toAbsolutePath();
        this.language = language;
    }

    /** Boots the full companion subsystem and starts a fresh trace file. */
    public void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close(); // init() returns an open pooled handle; close it so the pool isn't starved
        previousLanguage = SystemSession.getInstance().getLanguage();
        SystemSession.getInstance().setLanguage(language);
        CommandRegistry.getInstance().load();
        QueryRegistry.getInstance().load();
        SystemFunctionRegistry registry = SystemFunctionRegistry.getInstance();
        if (registry.byId().isEmpty()) {
            registry.load();
        }
        Map<String, SystemFunction> systemFunctions = registry.byId();
        // Queries are read-only (they press no keys), so the eval runs them for real - see recordingExecution.
        Map<String, IntelQuery> queryHandlers = QueryHandlerFactory.getInstance().registerQueryHandlers();

        model = SystemSession.getInstance().getLmStudioCommandModel().trim();
        LlmTransport tracing = body -> {
            long round = rounds.incrementAndGet();
            traceRaw("\n======== LLM REQUEST #" + round + " ========\n" + body + "\n");
            long t0 = System.nanoTime();
            try {
                JsonObject response = LMStudioClient.getInstance().sendJsonRequest(body);
                latenciesMs.add((System.nanoTime() - t0) / 1_000_000);
                traceRaw("\n======== LLM RESPONSE #" + round + " ========\n" + TRACE_JSON.toJson(response) + "\n");
                return response;
            } catch (RuntimeException failure) {
                traceRaw("\n======== LLM RESPONSE #" + round + " FAILED ========\n" + failure + "\n");
                throw failure;
            }
        };
        LlmGateway llm = new CompanionLlmGateway(new LmStudioLlmAdapter(model), tracing);

        // Recording execution with one real seam for read-only work: game COMMANDS are recorded but never
        // executed (they would press keys); QUERIES and system functions (speak, recall, remember,
        // change_global_topic, ...) run for real, so the LLM and memory get the actual query result and the
        // topic/verbosity/speech state evolves the production way.
        ExecutionGateway recordingExecution = request -> {
            String toolName = request.toolName();
            SystemFunction fn = systemFunctions.get(toolName);
            IntelQuery query = queryHandlers.get(toolName);
            JsonObject result = null;
            try {
                if (fn != null) {
                    result = fn.handle(toolName, request.arguments(), "");
                } else if (query != null) {
                    result = query.handle(toolName, request.arguments(), ""); // read-only: safe to run in the eval
                }
            } catch (Exception ignored) {
                // a system-function/query failure in the eval must not abort the turn
            }
            if (result == null) {
                result = new JsonObject();
                result.addProperty("status", (fn != null || query != null) ? "ok" : "recorded");
            }
            turnCalls.add(new Executed(toolName, request.arguments(), result));
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
        // Treat any boot-time entries as already seen, so the first memoryDeltaBlock() reports only real input.
        memory.readShortTermTimeline().forEach(e -> seenMemory.add(memoryKey(e)));
    }

    /** Stops the subsystem; safe to call when never booted. */
    public void shutdown() {
        if (gate != null) {
            gate.stop();
        }
        if (previousLanguage != null) {
            SystemSession.getInstance().setLanguage(previousLanguage);
            previousLanguage = null;
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

    /** The tool names executed this turn, in execution order (for tracing the full turn flow). */
    public List<String> turnToolNames() {
        return turnCalls.stream().map(Executed::tool).toList();
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

    // --- memory-fill tracing (shared by every theme eval) ---

    /**
     * The short-term writes since the previous call, formatted as the prompt shows them
     * ({@code [SOURCE][topic] content}), with the running short-term total - so each turn's report can show
     * exactly what that input wrote to memory and how the hot timeline accumulates and evicts over the run.
     */
    public String memoryDeltaBlock() {
        List<MemoryEntry> added = newMemoryThisTurn();
        StringBuilder block = new StringBuilder(
                String.format("    memory +%d (short-term total: %d):%n", added.size(), memory.readShortTermTimeline().size()));
        for (MemoryEntry e : added) {
            block.append("      ").append(renderEntry(e)).append("\n");
        }
        return block.toString();
    }

    /** The whole short-term timeline at the end of the run, oldest-to-newest - the accumulated memory state. */
    public String shortTermDumpBlock() {
        List<MemoryEntry> timeline = memory.readShortTermTimeline();
        StringBuilder dump = new StringBuilder(
                String.format("%n---- short-term timeline at end (%d entries, oldest first) ----%n", timeline.size()));
        for (MemoryEntry e : timeline) {
            dump.append("  ").append(renderEntry(e)).append("\n");
        }
        return dump.toString();
    }

    /** Short-term entries written since the previous {@link #memoryDeltaBlock()} call (matched by identity). */
    private List<MemoryEntry> newMemoryThisTurn() {
        List<MemoryEntry> added = new ArrayList<>();
        for (MemoryEntry e : memory.readShortTermTimeline()) {
            if (seenMemory.add(memoryKey(e))) {
                added.add(e);
            }
        }
        return added;
    }

    /** Renders a memory entry exactly as the prompt shows it: {@code [SOURCE][topic] content}. */
    private static String renderEntry(MemoryEntry e) {
        return String.format("[%s][%s] %s",
                e.source().displayLabel(CompanionConfig.companionName()),
                e.topic().name().toLowerCase(Locale.ROOT), e.content());
    }

    private static String memoryKey(MemoryEntry e) {
        return e.timestamp() + "|" + e.source() + "|" + e.content();
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

    /** Appends low-level LLM request/response details to the trace file without flooding stdout. */
    private synchronized void traceRaw(String block) {
        try {
            Files.writeString(traceFile, block, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {
            // Raw tracing is diagnostic only; never let it change eval behavior.
        }
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
