package elite.intel.ai.brain.vega.input;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.ShipPersonality;
import elite.intel.ai.brain.actions.handlers.QueryHandlerFactory;
import elite.intel.ai.brain.actions.handlers.commands.CommandRegistry;
import elite.intel.ai.brain.actions.handlers.queries.IntelQuery;
import elite.intel.ai.brain.actions.handlers.queries.QueryRegistry;
import elite.intel.ai.brain.inference.lmstudio.LMStudioClient;
import elite.intel.ai.brain.inference.mistral.MistralClient;
import elite.intel.ai.brain.vega.CompanionConfig;
import elite.intel.ai.brain.vega.CompanionRuntime;
import elite.intel.ai.brain.vega.execution.ExecutionGateway;
import elite.intel.ai.brain.vega.llm.*;
import elite.intel.ai.brain.vega.memory.MemoryGateway;
import elite.intel.ai.brain.vega.memory.facts.MemoryFactSourceRegistry;
import elite.intel.ai.brain.vega.mind.CompanionState;
import elite.intel.ai.brain.vega.mind.ThoughtDispatcher;
import elite.intel.ai.brain.vega.model.memory.MemoryEntry;
import elite.intel.ai.brain.vega.model.memory.MemoryRecord;
import elite.intel.ai.brain.vega.tools.SystemFunction;
import elite.intel.ai.brain.vega.tools.SystemFunctionRegistry;
import elite.intel.db.dao.ShipDao;
import elite.intel.db.managers.ShipManager;
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

    /**
     * One executed tool-call captured from the recording gateway: name, arguments, and the result returned.
     */
    public record Executed(String tool, JsonObject args, JsonObject result) {
    }

    private final Path traceFile;
    private final Language language;
    private final List<Executed> turnCalls = new CopyOnWriteArrayList<>();
    private final List<Long> latenciesMs = new CopyOnWriteArrayList<>();
    private final AtomicLong rounds = new AtomicLong();
    // Identities of recent records already reported, so each memoryDeltaBlock() shows only new atomic writes.
    private final Set<String> seenMemory = new HashSet<>();
    // The raw body of the last LLM request this turn, so recall scoring can read the injected candidate block.
    private volatile String lastRequestBody;

    /**
     * The LLM backend the eval drives: the local LM Studio endpoint (default) or the Mistral cloud endpoint.
     */
    public enum Backend {LMSTUDIO, MISTRAL}

    private final Backend backend;

    private CompanionSubsystemGate gate;
    private ThoughtDispatcher dispatcher;
    private MemoryGateway memory;
    private CompanionState state;
    private String model;
    private Language previousLanguage;
    private ShipPersonality previousPersonality;

    /**
     * @param traceFileName the file name written under {@code build/} for this eval's trace.
     */
    public CompanionEvalHarness(String traceFileName) {
        this(traceFileName, Language.EN, Backend.LMSTUDIO);
    }

    /**
     * Creates an eval harness pinned to the requested UI/AI language on the default LM Studio backend.
     */
    public CompanionEvalHarness(String traceFileName, Language language) {
        this(traceFileName, language, Backend.LMSTUDIO);
    }

    /**
     * Creates an eval harness pinned to the requested UI/AI language and LLM backend. The language is set
     * before the companion graph boots, so the system prompt and localized tool aliases match the scripted
     * input; the backend selects which provider's adapter and client the traced transport drives.
     */
    public CompanionEvalHarness(String traceFileName, Language language, Backend backend) {
        this.traceFile = Paths.get("build", traceFileName).toAbsolutePath();
        this.language = language;
        this.backend = backend;
    }

    /**
     * Boots the full companion subsystem and starts a fresh trace file.
     */
    public void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close(); // init() returns an open pooled handle; close it so the pool isn't starved
        previousLanguage = SystemSession.getInstance().getLanguage();
        SystemSession.getInstance().setLanguage(language);
        CommandRegistry.getInstance().load();
        QueryRegistry.getInstance().load();
        MemoryFactSourceRegistry.getInstance().load(); // so the <facts> block sees the fact sources, as in App.main
        SystemFunctionRegistry registry = SystemFunctionRegistry.getInstance();
        if (registry.byId().isEmpty()) {
            registry.load();
        }
        Map<String, SystemFunction> systemFunctions = registry.byId();
        // Queries are read-only (they press no keys), so the eval runs them for real - see recordingExecution.
        Map<String, IntelQuery> queryHandlers = QueryHandlerFactory.getInstance().registerQueryHandlers();

        previousPersonality = SystemSession.getInstance().getAIPersonality();

        // Provider wiring by backend: the adapter plus the raw transport to that provider's client (mirrors
        // CompanionLlmGatewayFactory). The tracing transport wraps the raw send, so every backend is traced.
        LlmProviderAdapter adapter;
        LlmTransport rawSend;
        switch (backend) {
            case MISTRAL -> {
                model = "mistral (cloud)";
                adapter = new MistralLlmAdapter();
                rawSend = body -> MistralClient.getInstance().sendJsonRequest(body);
            }
            default -> {
                model = SystemSession.getInstance().getLmStudioCommandModel().trim();
                adapter = new LmStudioLlmAdapter(model);
                rawSend = body -> LMStudioClient.getInstance().sendJsonRequest(body);
            }
        }
        LlmTransport tracing = body -> {
            long round = rounds.incrementAndGet();
            lastRequestBody = body; // captured for live-fact injection scoring
            traceRaw("\n======== LLM REQUEST #" + round + " ========\n" + body + "\n");
            long t0 = System.nanoTime();
            try {
                JsonObject response = rawSend.send(body);
                latenciesMs.add((System.nanoTime() - t0) / 1_000_000);
                traceRaw("\n======== LLM RESPONSE #" + round + " ========\n" + TRACE_JSON.toJson(response) + "\n");
                return response;
            } catch (RuntimeException failure) {
                traceRaw("\n======== LLM RESPONSE #" + round + " FAILED ========\n" + failure + "\n");
                throw failure;
            }
        };
        LlmGateway llm = new CompanionLlmGateway(adapter, tracing);

        // Recording execution with one real seam for read-only work: game COMMANDS are recorded but never
        // executed (they would press keys); QUERIES and system functions run for real, so the LLM and memory
        // receive the actual query result while external game state remains untouched.
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
        // Treat any boot-time records as already seen, so the first memoryDeltaBlock() reports only real input.
        memory.readRecentHistory().forEach(record -> seenMemory.add(memoryKey(record)));
    }

    /**
     * Stops the subsystem; safe to call when never booted.
     */
    public void shutdown() {
        if (gate != null) {
            gate.stop();
        }
        if (previousLanguage != null) {
            SystemSession.getInstance().setLanguage(previousLanguage);
            previousLanguage = null;
        }
        if (previousPersonality != null) {
            applyPersonality(previousPersonality);
            previousPersonality = null;
        }
    }

    /**
     * Switches the commander-chosen AI personality for the following turns (the prompt reads it live per render).
     */
    public void setPersonality(ShipPersonality personality) {
        applyPersonality(personality);
    }

    /**
     * Writes the personality onto the active ship (personality is per-ship). {@link SystemSession#getAIPersonality()}
     * reads it back from the same ship, so this is how the harness drives the setting production reads.
     */
    private static void applyPersonality(ShipPersonality personality) {
        ShipDao.Ship ship = ShipManager.getInstance().getShip();
        if (ship == null) return;
        ship.setPersonality(personality.name());
        ShipManager.getInstance().saveShip(ship);
    }

    // --- driving the real system over the bus ---

    /**
     * Speaks a commander phrase the production way and waits for the turn to settle.
     */
    public void say(String input) throws Exception {
        beginTurn();
        GameEventBus.publish(new UserInputEvent(input));
        awaitIdle();
    }

    /**
     * Publishes a filtered game event the production way and waits for the turn to settle.
     */
    public void gameEvent(String type, String summary) throws Exception {
        beginTurn();
        GameEventBus.publish(gameEventOf(type, summary));
        awaitIdle();
    }

    /**
     * Publishes a game event with explicit journal importance through the production path and waits for the turn
     * to settle. A mapped event that produces a reaction is stored as one completed EVENT record.
     */
    public void gameEvent(String type, String summary, BaseEvent.Importance importance) throws Exception {
        beginTurn();
        GameEventBus.publish(gameEventOf(type, summary, importance));
        awaitIdle();
    }

    /**
     * Publishes a commander phrase without waiting - for real-time stream / keep-up measurement.
     */
    public void publishInput(String input) {
        GameEventBus.publish(new UserInputEvent(input));
    }

    /**
     * Publishes a game event without waiting - for real-time stream / keep-up measurement.
     */
    public void publishEvent(String type, String summary) {
        GameEventBus.publish(gameEventOf(type, summary));
    }

    /**
     * Clears the per-turn capture; call before driving input that should be scored in isolation.
     */
    public void beginTurn() {
        turnCalls.clear();
        lastRequestBody = null;
    }

    /**
     * Blocks until both lanes are idle (the real turn-boundary signal) or the per-turn timeout elapses.
     */
    public void awaitIdle() throws InterruptedException {
        long deadline = System.currentTimeMillis() + TURN_TIMEOUT_MS;
        while (!dispatcher.isIdle() && System.currentTimeMillis() < deadline) {
            Thread.sleep(POLL_MS);
        }
    }

    // --- per-turn capture / scoring helpers ---

    /**
     * All tool-calls executed during the current turn.
     */
    public List<Executed> turnCalls() {
        return List.copyOf(turnCalls);
    }

    /**
     * The tool names executed this turn, in execution order (for tracing the full turn flow).
     */
    public List<String> turnToolNames() {
        return turnCalls.stream().map(Executed::tool).toList();
    }

    /**
     * Executed tool-calls of the given name this turn.
     */
    public List<Executed> callsNamed(String tool) {
        return turnCalls.stream().filter(c -> c.tool().equals(tool)).toList();
    }

    /**
     * Whether a tool of the given name was called this turn.
     */
    public boolean called(String tool) {
        return !callsNamed(tool).isEmpty();
    }

    /**
     * Everything the commander actually hears this turn: the LLM's own {@code speak} calls plus the spoken
     * answer of any self-narrating query/command (a query voices its {@code text_to_speech_response} through the
     * announcement path, not a speak call), so scoring reflects what was said regardless of which tool said it.
     */
    public List<String> spokenTexts() {
        List<String> spoken = new ArrayList<>();
        for (Executed c : callsNamed("speak")) {
            if (c.args().has("text") && !c.args().get("text").isJsonNull()) {
                spoken.add(c.args().get("text").getAsString());
            }
        }
        for (Executed c : turnCalls) {
            String tts = str(c.result(), "text_to_speech_response");
            if (!tts.isBlank()) {
                spoken.add(tts);
            }
        }
        return spoken;
    }

    /**
     * Whether any spoken phrase this turn contains the token (case-insensitive).
     */
    public boolean spokenContains(String token) {
        String needle = token.toLowerCase(Locale.ROOT);
        return spokenTexts().stream().anyMatch(s -> s.toLowerCase(Locale.ROOT).contains(needle));
    }

    /**
     * The raw body of the last LLM request this turn, for inspecting its history, context, and live facts.
     */
    public String lastRequestBody() {
        return lastRequestBody == null ? "" : lastRequestBody;
    }

    /**
     * The live fact-source values inlined into this turn's system prompt, or empty.
     */
    public List<String> injectedFacts() {
        if (lastRequestBody == null) {
            return List.of();
        }
        int start = lastRequestBody.indexOf("<facts>");
        if (start < 0) {
            return List.of();
        }
        int end = lastRequestBody.indexOf("</facts>", start);
        String block = lastRequestBody.substring(start, end < 0 ? lastRequestBody.length() : end);
        List<String> facts = new ArrayList<>();
        // Facts are XML elements <fact ... >text</fact>; pull the text between each element's tags.
        for (String part : block.split("<fact")) {
            int gt = part.indexOf('>');
            int close = part.indexOf("</fact>");
            if (gt >= 0 && close > gt) {
                facts.add(part.substring(gt + 1, close).strip());
            }
        }
        return facts;
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }

    // --- memory-fill tracing (shared by every theme eval) ---

    /**
     * Completed records added since the previous call, with the running recent-history total.
     */
    public String memoryDeltaBlock() {
        List<MemoryRecord> added = newMemoryThisTurn();
        StringBuilder block = new StringBuilder(
                String.format("    memory +%d record(s) (recent total: %d):%n",
                        added.size(), memory.readRecentHistory().size()));
        for (MemoryRecord record : added) {
            block.append(renderRecord(record, "      "));
        }
        return block.toString();
    }

    /**
     * The whole recent record history at the end of the run, oldest-to-newest.
     */
    public String recentMemoryDumpBlock() {
        List<MemoryRecord> records = memory.readRecentHistory();
        StringBuilder dump = new StringBuilder(
                String.format("%n---- recent memory at end (%d records, oldest first) ----%n", records.size()));
        for (MemoryRecord record : records) {
            dump.append(renderRecord(record, "  "));
        }
        return dump.toString();
    }

    /**
     * Recent records written since the previous {@link #memoryDeltaBlock()} call.
     */
    private List<MemoryRecord> newMemoryThisTurn() {
        List<MemoryRecord> added = new ArrayList<>();
        for (MemoryRecord record : memory.readRecentHistory()) {
            if (seenMemory.add(memoryKey(record))) {
                added.add(record);
            }
        }
        return added;
    }

    private static String renderRecord(MemoryRecord record, String indent) {
        StringBuilder rendered = new StringBuilder(indent)
                .append('[').append(record.kind().name().toLowerCase(Locale.ROOT)).append("] ")
                .append(record.timestamp()).append('\n');
        for (MemoryEntry entry : record.entries()) {
            rendered.append(indent).append("  [")
                    .append(entry.source().displayLabel(CompanionConfig.companionName()))
                    .append("] ").append(entry.content()).append('\n');
        }
        return rendered.toString();
    }

    private static String memoryKey(MemoryRecord record) {
        return record.timestamp() + "|" + record.kind() + "|" + record.entries();
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

    /**
     * Appends a block to the eval's trace file and echoes it to stdout.
     */
    public void trace(String block) throws Exception {
        System.out.print(block);
        Files.writeString(traceFile, block, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }

    /**
     * Appends low-level LLM request/response details to the trace file without flooding stdout.
     */
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

    /**
     * A minimal game event carrying a type and a one-line English summary, at LOW importance (filtered out).
     */
    private static BaseEvent gameEventOf(String type, String summary) {
        return gameEventOf(type, summary, BaseEvent.Importance.LOW);
    }

    /**
     * A minimal game event with an explicit importance, so a HIGH event of a mapped type lands in memory.
     */
    private static BaseEvent gameEventOf(String type, String summary, BaseEvent.Importance importance) {
        return new BaseEvent(Instant.now().toString(), Duration.ofMinutes(1), type) {
            @Override
            public String getEventType() {
                return type;
            }

            @Override
            public BaseEvent.Importance importance() {
                return importance;
            }

            @Override
            public String memorySummary() {
                return summary; // the readable line EventThought records (mirrors a real event's memorySummary)
            }

            @Override
            public String toJson() {
                JsonObject o = new JsonObject();
                o.addProperty("event", type);
                o.addProperty("detail", summary);
                return o.toString();
            }

            @Override
            public JsonObject toJsonObject() {
                JsonObject o = new JsonObject();
                o.addProperty("event", type);
                o.addProperty("detail", summary);
                return o;
            }
        };
    }

}
