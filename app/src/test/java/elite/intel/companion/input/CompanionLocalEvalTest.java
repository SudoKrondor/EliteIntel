package elite.intel.companion.input;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elite.intel.ai.brain.actions.command.CommandRegistry;
import elite.intel.ai.brain.actions.query.QueryRegistry;
import elite.intel.ai.brain.inference.lmstudio.LMStudioClient;
import elite.intel.companion.CompanionRuntime;
import elite.intel.companion.llm.CompanionLlmGateway;
import elite.intel.companion.llm.LlmGateway;
import elite.intel.companion.llm.LlmTransport;
import elite.intel.companion.llm.LmStudioLlmAdapter;
import elite.intel.companion.execution.ExecutionGateway;
import elite.intel.companion.memory.MemoryGateway;
import elite.intel.companion.mind.CompanionState;
import elite.intel.companion.mind.ThoughtDispatcher;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.execution.ExecutionRequest;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.tools.SystemFunction;
import elite.intel.companion.tools.SystemFunctionRegistry;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.UserInputEvent;
import elite.intel.gameapi.journal.events.BaseEvent;
import elite.intel.session.SystemSession;
import elite.intel.util.Cypher;
import elite.intel.db.util.Database;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Manual (opt-in) eval of the FULL companion against a real local LLM (LM Studio with a loaded Gemma): the
 * real {@link CompanionSubsystemGate} boots the real graph (dispatcher, lanes, filter, barge-in, prompt,
 * memory, system functions, reducer with the actual game-command catalog), and input is fed the production
 * way - over {@code GameEventBus} as {@code UserInputEvent}s and game {@code BaseEvent}s. Only two seams are
 * test-injected: a recording execution gateway (game commands are recorded, never executed - no keystrokes;
 * system functions do run) and a tracing LLM transport (captures the exact prompt, raw response and per-round
 * latency). Turn boundaries come from the real {@link ThoughtDispatcher#isIdle()}.
 * <p>
 * It plays a long multi-scenario ~conversation (commands, queries, memory, topic, verbosity, game events,
 * a dangerous action + spoken confirmation) deliberately sized to exercise the full memory eviction chain:
 * two long single-topic blocks (navigation, then trade) overflow short-term into mid-term and then mid-term
 * past its per-topic cap into the consolidation buffer, firing the LLM compression into the long-term
 * summary. It writes a full trace - prompts, responses, tool-calls, speech, a memory snapshot of every tier,
 * dispatcher timing, per-round and aggregate LLM latency plus token usage (prompt / completion / cached
 * tokens) - to stdout and to {@code build/companion-eval-trace.txt}.
 * <p>
 * Run: {@code ./gradlew :app:localIntegrationTest --tests "...CompanionLocalEvalTest"}.
 * REQUIREMENTS: LM Studio running with a tool-use model loaded as the command model; settings: local command
 * LLM on, provider LM Studio, address + command model; the app's SQLite DB populated; game running for data.
 */
@Tag("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CompanionLocalEvalTest {

    private static final long TURN_TIMEOUT_MS = 90_000;
    private static final long CONFIRM_WAIT_MS = 8_000;
    private static final long POLL_MS = 150;
    private static final Path TRACE_FILE = Paths.get("build", "companion-eval-trace.txt").toAbsolutePath();
    private static final com.google.gson.Gson PRETTY = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /**
     * One captured LLM round: the exact request body, the raw response, the round-trip latency, and the
     * token usage reported in the response ({@code usage.prompt_tokens} / {@code completion_tokens} and the
     * cache-reused {@code usage.prompt_tokens_details.cached_tokens}).
     */
    private record Round(JsonObject request, JsonObject response, long latencyMs,
                         int promptTokens, int completionTokens, int cachedTokens) {}

    private final List<ExecutionRequest> toolCalls = new CopyOnWriteArrayList<>();
    private final List<String> spoken = new CopyOnWriteArrayList<>();
    private final List<Round> rounds = new CopyOnWriteArrayList<>();
    // Background memory-compression calls (no tools), recorded async on the consolidator thread. Kept apart
    // from per-turn rounds so they are not misattributed to whatever commander turn happens to be tracing.
    private final List<Round> consolidationRounds = new CopyOnWriteArrayList<>();
    private final List<Long> allLatenciesMs = new CopyOnWriteArrayList<>();
    // Per-turn-round latencies only (excludes the background consolidation calls), for the responsiveness stat.
    private final List<Long> turnLatenciesMs = new CopyOnWriteArrayList<>();
    // Token totals accumulate across the whole conversation (rounds are cleared every turn).
    private final AtomicLong totalPromptTokens = new AtomicLong();
    private final AtomicLong totalCompletionTokens = new AtomicLong();
    private final AtomicLong totalCachedTokens = new AtomicLong();

    private CompanionSubsystemGate gate;
    private ThoughtDispatcher dispatcher;
    private MemoryGateway memory;
    private CompanionState state;

    @BeforeAll
    void boot() throws Exception {
        Cypher.initializeKey();
        Database.init();
        CommandRegistry.getInstance().load();
        QueryRegistry.getInstance().load();
        SystemFunctionRegistry registry = SystemFunctionRegistry.getInstance();
        if (registry.byId().isEmpty()) {
            registry.load();
        }
        Map<String, SystemFunction> systemFunctions = registry.byId();

        String model = SystemSession.getInstance().getLmStudioCommandModel().trim();
        LlmTransport tracing = body -> {
            long t0 = System.nanoTime();
            try {
                JsonObject response = LMStudioClient.getInstance().sendJsonRequest(body);
                record(body, response, t0);
                return response;
            } catch (RuntimeException transportError) {
                JsonObject err = new JsonObject();
                err.addProperty("transport_error", String.valueOf(transportError.getMessage()));
                record(body, err, t0);
                throw transportError;
            }
        };
        LlmGateway llm = new CompanionLlmGateway(new LmStudioLlmAdapter(model), tracing);

        // Recording execution: game commands are recorded but never executed (no keystrokes); system
        // functions run so memory/topic/verbosity/speech evolve - the companion's dry-run equivalent.
        ExecutionGateway recordingExecution = request -> {
            toolCalls.add(request);
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
            return CompletableFuture.completedFuture(result);
        };

        gate = new CompanionSubsystemGate(llm, recordingExecution);
        gate.start(); // real dispatcher/lanes/filter/barge-in/memory/state/speech, installs runtime, registers on the bus
        dispatcher = gate.dispatcher();
        memory = CompanionRuntime.memory();
        state = CompanionRuntime.state();

        Files.createDirectories(TRACE_FILE.getParent());
        Files.writeString(TRACE_FILE, "Companion full-system eval - " + Instant.now() + " (model=" + model + ")\n",
                StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        System.out.println("Companion eval trace -> " + TRACE_FILE);
        Thread.sleep(1500);
    }

    @AfterAll
    void shutdown() {
        if (gate != null) {
            gate.stop();
        }
    }

    @Test
    void runsTheFullSystemThroughAConversation() throws Exception {
        // Opening + a fact to remember (recalled much later, after it has been evicted from short-term).
        say("what is our current location", "location");
        say("remember that I prefer trading and exploration over combat", "remember");

        // NAVIGATION block: pin the global topic, then a long single-topic run. Each commander turn records
        // its input plus its tool results under the global topic, so short-term (cap 20) overflows into
        // mid-term[navigation]; once mid-term[navigation] passes its per-topic cap (30) the overflow fills
        // the consolidation buffer (20) and fires the LLM compression into the long-term summary. This is
        // the full short -> mid -> long eviction chain we want the trace to show.
        say("let's talk about navigation", "navigation");
        runTopicBlock(NAVIGATION_PHRASES);

        // TRADE block: switch the global topic and run another single-topic stretch (a second mid-term topic).
        say("now let's talk about trading", "trade");
        runTopicBlock(TRADE_PHRASES);

        // Recall the fact stated at the start - by now it should live in mid-term, not short-term.
        say("what did I tell you to remember", "recall");

        // Verbosity gate + game events (QUIET should stay silent on routine, CHATTY may comment).
        say("keep quiet about routine events", "quiet");
        gameEvent("FSDJump", "jumped to the next system");
        say("alright, be chatty again", "chatty");
        gameEvent("MarketSell", "sold 40 tons of gold for profit");

        // Dangerous action + spoken confirmation (best-effort: confirm via the code word).
        dangerousThenConfirm("self destruct the ship");

        writeStats();
        assertFalse(allLatenciesMs.isEmpty(), "the local model was never reached - see the trace and LM Studio settings");
    }

    /** Replays every phrase of a single-topic block as commander input, one turn each. */
    private void runTopicBlock(List<String> phrases) throws Exception {
        for (String phrase : phrases) {
            say(phrase, "topic-block");
        }
    }

    /** Navigation-domain phrases - a long single-topic run to overflow mid-term[navigation] into long-term. */
    private static final List<String> NAVIGATION_PHRASES = List.of(
            "plot a route to Sol",
            "how far is the next star system",
            "what's our current fuel level",
            "find the nearest scoopable star",
            "how many jumps to the destination",
            "what's the distance remaining on the route",
            "engage the next jump when ready",
            "are there any neutron stars on our path",
            "what's our maximum jump range",
            "recalculate the route in economic mode",
            "how long until we reach the destination system",
            "is the destination a planetary base",
            "what's the security level of the system ahead",
            "scan for nearby points of interest",
            "what's our current heading",
            "align us with the next jump target",
            "how much fuel does the next jump cost",
            "are we in supercruise or normal space",
            "what's the closest inhabited system",
            "does the route pass through any anarchy systems",
            "what are the coordinates of our destination",
            "drop us out of supercruise near the station",
            "how many light years have we travelled this session",
            "set the next waypoint on the galaxy map");

    /** Trade-domain phrases - a second single-topic block, building a distinct mid-term topic. */
    private static final List<String> TRADE_PHRASES = List.of(
            "what are the commodity prices at this station",
            "where can I sell gold for the best profit",
            "what's the demand for palladium nearby",
            "find a profitable trade route from here",
            "how much cargo space do we have left",
            "what's the buy price for tritium",
            "is there a black market in this system",
            "compare painite prices within twenty light years",
            "what commodities are in high demand right now",
            "sell all of our current cargo",
            "what's the average profit per ton on this route",
            "find the nearest commodity market",
            "what's the galactic average price for silver",
            "how much would a full hold of platinum cost",
            "what's the best place to buy low and sell high",
            "are there any rare goods available here");

    // --- driving the real system over the bus ---

    /** Speaks a commander phrase the production way and traces the turn. */
    private void say(String input, String routingHint) throws Exception {
        beginTurn();
        GameEventBus.publish(new UserInputEvent(input));
        awaitIdle(TURN_TIMEOUT_MS);
        trace("COMMANDER \"" + input + "\"", routingHint);
    }

    /** Publishes a filtered game event the production way and traces the turn. */
    private void gameEvent(String type, String summary) throws Exception {
        beginTurn();
        GameEventBus.publish(gameEventOf(type, summary));
        awaitIdle(TURN_TIMEOUT_MS);
        trace("EVENT " + type, type);
    }

    /** Dangerous command, then confirm with the spoken code word while the thought is frozen (best-effort). */
    private void dangerousThenConfirm(String input) throws Exception {
        beginTurn();
        GameEventBus.publish(new UserInputEvent(input));
        // Give the model time to choose a dangerous action and freeze; then confirm via the code word.
        long deadline = System.currentTimeMillis() + CONFIRM_WAIT_MS;
        while (!awaitingConfirmation() && System.currentTimeMillis() < deadline) {
            Thread.sleep(POLL_MS);
        }
        GameEventBus.publish(new UserInputEvent("password")); // CompanionConfig code word -> coordinator.confirm()
        awaitIdle(TURN_TIMEOUT_MS);
        trace("COMMANDER (dangerous) \"" + input + "\" + confirm", "confirm");
    }

    private void beginTurn() {
        toolCalls.clear();
        spoken.clear();
        rounds.clear();
    }

    private void awaitIdle(long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (!dispatcher.isIdle() && System.currentTimeMillis() < deadline) {
            Thread.sleep(POLL_MS);
        }
    }

    private boolean awaitingConfirmation() {
        return memory.readShortTermTimeline().stream()
                .anyMatch(e -> e.processingState().name().equals("AWAITING_CONFIRMATION"));
    }

    // --- tracing ---

    private synchronized void record(String requestBody, JsonObject response, long startNanos) {
        long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
        int promptTokens = usageInt(response, "prompt_tokens");
        int completionTokens = usageInt(response, "completion_tokens");
        int cachedTokens = cachedTokens(response);
        JsonObject request = JsonParser.parseString(requestBody).getAsJsonObject();
        Round round = new Round(request, response, latencyMs, promptTokens, completionTokens, cachedTokens);
        // A consciousness turn always offers tools; a memory-compression call carries none. The compression
        // call runs async on the consolidator thread, so route it to its own bucket instead of the current turn.
        if (request.has("tools")) {
            rounds.add(round);
            turnLatenciesMs.add(latencyMs);
        } else {
            consolidationRounds.add(round);
        }
        allLatenciesMs.add(latencyMs);
        totalPromptTokens.addAndGet(promptTokens);
        totalCompletionTokens.addAndGet(completionTokens);
        totalCachedTokens.addAndGet(cachedTokens);
    }

    /** The {@code usage} object of a response, or null (a transport-error stub carries none). */
    private static JsonObject usageOf(JsonObject response) {
        return response.has("usage") && response.get("usage").isJsonObject() ? response.getAsJsonObject("usage") : null;
    }

    /** A top-level {@code usage} integer (e.g. prompt_tokens / completion_tokens), or 0 when absent. */
    private static int usageInt(JsonObject response, String key) {
        JsonObject usage = usageOf(response);
        return usage != null && usage.has(key) && !usage.get(key).isJsonNull() ? usage.get(key).getAsInt() : 0;
    }

    /** Prompt tokens reused from the provider's cache ({@code usage.prompt_tokens_details.cached_tokens}), or 0. */
    private static int cachedTokens(JsonObject response) {
        JsonObject usage = usageOf(response);
        if (usage != null && usage.has("prompt_tokens_details") && usage.get("prompt_tokens_details").isJsonObject()) {
            JsonObject details = usage.getAsJsonObject("prompt_tokens_details");
            if (details.has("cached_tokens") && !details.get("cached_tokens").isJsonNull()) {
                return details.get("cached_tokens").getAsInt();
            }
        }
        return 0;
    }

    private void trace(String header, String routingHint) throws Exception {
        StringBuilder t = new StringBuilder();
        t.append("\n========================================================================\n");
        t.append(header).append("\n");
        for (int i = 0; i < rounds.size(); i++) {
            Round r = rounds.get(i);
            t.append("\n--- LLM round ").append(i + 1).append("  (").append(r.latencyMs()).append(" ms, ")
                    .append(r.promptTokens()).append(" prompt / ").append(r.completionTokens()).append(" completion / ")
                    .append(r.cachedTokens()).append(" cached tokens) ---\n");
            t.append("PROMPT:\n").append(formatPrompt(r.request())).append("\n");
            t.append("RESPONSE: ").append(summarizeResponse(r.response())).append("\n");
        }
        t.append("\nEXECUTED tool-calls: ").append(toolCalls.stream().map(ExecutionRequest::toolName).toList()).append("\n");
        t.append("SPOKEN: ").append(spoken).append("\n");
        t.append(memorySnapshot());
        t.append("ROUTING hint '").append(routingHint).append("': ").append(matchesHint(routingHint) ? "present" : "MISSING").append("\n");
        System.out.print(t);
        Files.writeString(TRACE_FILE, t.toString(), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }

    private void writeStats() throws Exception {
        traceConsolidation(); // background compression calls get their own section, not a commander turn

        long total = allLatenciesMs.stream().mapToLong(Long::longValue).sum();
        List<Long> turns = turnLatenciesMs;
        long turnMax = turns.stream().mapToLong(Long::longValue).max().orElse(0);
        long turnMin = turns.stream().mapToLong(Long::longValue).min().orElse(0);
        long turnAvg = turns.isEmpty() ? 0 : turns.stream().mapToLong(Long::longValue).sum() / turns.size();
        long consTime = consolidationRounds.stream().mapToLong(Round::latencyMs).sum();
        long consMax = consolidationRounds.stream().mapToLong(Round::latencyMs).max().orElse(0);
        long consPrompt = consolidationRounds.stream().mapToLong(Round::promptTokens).sum();
        long consCompletion = consolidationRounds.stream().mapToLong(Round::completionTokens).sum();
        long promptTok = totalPromptTokens.get();
        long completionTok = totalCompletionTokens.get();
        long cachedTok = totalCachedTokens.get();
        long cachePct = promptTok == 0 ? 0 : cachedTok * 100 / promptTok;
        String s = "\n======== LLM STATS ========\n"
                + "rounds: " + allLatenciesMs.size() + " (turn " + turns.size()
                + ", background consolidation " + consolidationRounds.size() + ")\n"
                + "total LLM time: " + total + " ms\n"
                + "per-turn-round latency: avg " + turnAvg + " ms, min " + turnMin + " ms, max " + turnMax + " ms\n"
                + "consolidation: " + consolidationRounds.size() + " call(s), " + consTime + " ms total, slowest "
                + consMax + " ms, " + consPrompt + "/" + consCompletion + " prompt/completion tokens\n"
                + "tokens: prompt " + promptTok + ", completion " + completionTok
                + ", total " + (promptTok + completionTok) + "\n"
                + "cached prompt tokens: " + cachedTok + " (" + cachePct + "% of prompt tokens reused from cache)\n";
        System.out.print(s);
        Files.writeString(TRACE_FILE, s, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }

    /** Traces the background memory-compression calls (long-term consolidation) in their own section. */
    private void traceConsolidation() throws Exception {
        if (consolidationRounds.isEmpty()) {
            return;
        }
        StringBuilder t = new StringBuilder();
        t.append("\n========================================================================\n");
        t.append("BACKGROUND CONSOLIDATION (long-term memory compression - runs off the turn loop)\n");
        for (int i = 0; i < consolidationRounds.size(); i++) {
            Round r = consolidationRounds.get(i);
            t.append("\n--- consolidation call ").append(i + 1).append("  (").append(r.latencyMs()).append(" ms, ")
                    .append(r.promptTokens()).append(" prompt / ").append(r.completionTokens()).append(" completion / ")
                    .append(r.cachedTokens()).append(" cached tokens) ---\n");
            t.append("PROMPT:\n").append(formatPrompt(r.request())).append("\n");
            t.append("RESPONSE (new summary): ").append(responseText(r.response())).append("\n");
        }
        System.out.print(t);
        Files.writeString(TRACE_FILE, t.toString(), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }

    /** Plain-text content of a response (a compression turn returns text, not tool-calls). */
    private static String responseText(JsonObject response) {
        if (response.has("transport_error")) {
            return "TRANSPORT ERROR: " + str(response, "transport_error");
        }
        JsonArray choices = response.getAsJsonArray("choices");
        if (choices == null || choices.isEmpty()) {
            return "(no choices)";
        }
        JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
        return message != null && message.has("content") && !message.get("content").isJsonNull()
                ? message.get("content").getAsString() : "(no content)";
    }

    private boolean matchesHint(String hint) {
        String needle = hint.toLowerCase(Locale.ROOT);
        return toolCalls.stream().anyMatch(r -> r.toolName().toLowerCase(Locale.ROOT).contains(needle))
                || rounds.stream().anyMatch(r -> r.response().toString().toLowerCase(Locale.ROOT).contains(needle));
    }

    private static String formatPrompt(JsonObject body) {
        StringBuilder p = new StringBuilder();
        for (JsonElement m : body.getAsJsonArray("messages")) {
            JsonObject msg = m.getAsJsonObject();
            p.append("  [").append(str(msg, "role")).append("] ");
            if (msg.has("tool_call_id")) {
                p.append("(tool_call_id=").append(str(msg, "tool_call_id")).append(") ");
            }
            if (msg.has("content") && !msg.get("content").isJsonNull()) {
                p.append(msg.get("content").getAsString());
            }
            if (msg.has("tool_calls")) {
                p.append("tool_calls=").append(toolCallNames(msg.getAsJsonArray("tool_calls")));
            }
            p.append("\n");
        }
        if (body.has("tools")) {
            List<String> names = new CopyOnWriteArrayList<>();
            for (JsonElement tool : body.getAsJsonArray("tools")) {
                names.add(str(tool.getAsJsonObject().getAsJsonObject("function"), "name"));
            }
            p.append("  tools offered (").append(names.size()).append("): ").append(names).append("\n");
        }
        return p.append("  tool_choice=").append(body.has("tool_choice") ? str(body, "tool_choice") : "(none)").toString();
    }

    private static String summarizeResponse(JsonObject response) {
        if (response.has("transport_error")) {
            return "TRANSPORT ERROR: " + str(response, "transport_error");
        }
        JsonArray choices = response.getAsJsonArray("choices");
        if (choices == null || choices.isEmpty()) {
            return "no choices - raw: " + PRETTY.toJson(response);
        }
        JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
        if (message != null && message.has("tool_calls")) {
            JsonArray calls = message.getAsJsonArray("tool_calls");
            StringBuilder s = new StringBuilder("tool-calls " + toolCallNames(calls) + "\n");
            for (JsonElement c : calls) {
                JsonObject fn = c.getAsJsonObject().getAsJsonObject("function");
                s.append("    ").append(str(fn, "name")).append(" args=").append(str(fn, "arguments")).append("\n");
            }
            return s.toString().stripTrailing();
        }
        String content = message != null && message.has("content") && !message.get("content").isJsonNull()
                ? message.get("content").getAsString() : "";
        return "NO TOOL-CALLS (the model returned text instead): " + content;
    }

    private String memorySnapshot() {
        StringBuilder s = new StringBuilder("MEMORY:\n");
        s.append("  global topic: ").append(state.globalTopic()).append(", verbosity: ").append(state.verbosity()).append("\n");
        s.append("  llm_memory: ").append(memory.readLlmMemory()).append("\n");
        String summary = memory.longTermSummary();
        s.append("  long-term summary: ").append(summary == null || summary.isBlank() ? "(none)" : summary).append("\n");
        s.append("  short-term timeline:\n");
        for (MemoryEntry e : memory.readShortTermTimeline()) {
            s.append("    [").append(e.source()).append("][").append(e.topic()).append("][")
                    .append(e.processingState()).append("] ").append(e.content()).append("\n");
        }
        List<ConversationTopic> midTopics = memory.indexes().topicsWithMemory();
        s.append("  mid-term topic memory: ").append(midTopics.isEmpty() ? "(empty)" : "").append("\n");
        for (ConversationTopic topic : midTopics) {
            for (MemoryEntry e : memory.recallTopicMemory(topic, null, 50)) {
                s.append("    [").append(topic).append("][").append(e.source()).append("][")
                        .append(e.processingState()).append("] ").append(e.content()).append("\n");
            }
        }
        return s.toString();
    }

    private static List<String> toolCallNames(JsonArray toolCalls) {
        List<String> names = new CopyOnWriteArrayList<>();
        for (JsonElement c : toolCalls) {
            names.add(str(c.getAsJsonObject().getAsJsonObject("function"), "name"));
        }
        return names;
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }

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
