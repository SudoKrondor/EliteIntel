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
import elite.intel.companion.model.execution.ExecutionRequest;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.tools.SystemFunction;
import elite.intel.companion.tools.SystemFunctionRegistry;
import elite.intel.db.util.Database;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.UserInputEvent;
import elite.intel.session.SystemSession;
import elite.intel.util.Cypher;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Manual (opt-in) behavioral probe of how well the companion <em>remembers</em> and how it <em>searches
 * memory by topic</em> when a fact is no longer in the hot timeline. It runs the real
 * {@link CompanionSubsystemGate} against a local LLM, the production way (input over {@code GameEventBus}),
 * with the same recording execution + tracing seams as {@code CompanionLocalEvalTest}.
 * <p>
 * Method: plant a few distinctive facts (one via {@code remember} -> llm_memory, two stated under a topic ->
 * mid-term topic memory), run filler turns until they are evicted out of short-term, then ask a probe
 * question for each and score, from the trace, three things per probe:
 * <ul>
 *   <li><b>located tier</b> - where the fact actually lives at probe time (short-term / mid-term[topic] /
 *       llm_memory / long-term summary / LOST);</li>
 *   <li><b>recall use</b> - whether the model called {@code recall} and with the right {@code scope}/{@code topic};</li>
 *   <li><b>outcome</b> - HIT (the spoken answer contains the planted detail), or an honest MISS (it says it
 *       does not know) versus a fabricated/wrong answer.</li>
 * </ul>
 * Results are written as a per-probe trace plus an aggregate table to {@code build/companion-memory-probe-trace.txt}.
 * Run: {@code ./gradlew :app:localIntegrationTest --tests "...CompanionMemoryProbeTest"} with LM Studio up.
 */
@Tag("local-integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CompanionMemoryProbeTest {

    private static final long TURN_TIMEOUT_MS = 90_000;
    private static final long POLL_MS = 150;
    private static final Path TRACE_FILE = Paths.get("build", "companion-memory-probe-trace.txt").toAbsolutePath();

    /** Phrases that signal an honest "I don't know" rather than a fabricated answer. */
    private static final List<String> MISS_CUES = List.of(
            "don't", "do not", "cannot", "can't", "not sure", "unsure", "no record", "don't have",
            "do not have", "not available", "unable", "no information", "i don't know", "not in my");

    /** Phrases that signal a request for clarification rather than an answer or a flat refusal. */
    private static final List<String> CLARIFY_CUES = List.of(
            "more information", "more detail", "can you provide", "can you clarify", "could you specify",
            "what do you mean", "need more", "which one");

    /** One planted fact and the question that probes it. */
    private record Probe(String label, ConversationTopic plantedUnder, String plantInput, String probeQuestion,
                         String locatorToken, String answerKeyword) {}

    /** One executed tool-call captured from the recording gateway: name, arguments, and the result returned. */
    private record Executed(String tool, JsonObject args, JsonObject result) {}

    private final List<Executed> turnCalls = new CopyOnWriteArrayList<>();
    private final List<Long> allLatenciesMs = new CopyOnWriteArrayList<>();
    private final AtomicLong rounds = new AtomicLong();

    private CompanionSubsystemGate gate;
    private ThoughtDispatcher dispatcher;
    private MemoryGateway memory;
    private CompanionState state;

    // Probed while still in the hot timeline (no recall needed - it is in the prompt's session memory block).
    private final Probe shortTerm = new Probe("op callsign (short-term)", ConversationTopic.SOCIAL,
            "our callsign for this operation is Nightingale",
            "what's our callsign for this operation", "nightingale", "nightingale");

    private final List<Probe> probes = List.of(
            new Probe("docking code (llm_memory)", ConversationTopic.SYSTEM,
                    "remember that our docking authorization code is Sierra Nine Four",
                    "what is our docking authorization code", "sierra", "sierra"),
            new Probe("painite stash (trade)", ConversationTopic.TRADE,
                    "for the record, we cached the painite in cargo rack seven",
                    "which cargo rack holds the painite", "painite", "seven"),
            new Probe("rendezvous (navigation)", ConversationTopic.NAVIGATION,
                    "the rendezvous point is the third moon of Maia",
                    "where is the rendezvous point", "maia", "maia"));

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
            JsonObject response = LMStudioClient.getInstance().sendJsonRequest(body);
            allLatenciesMs.add((System.nanoTime() - t0) / 1_000_000);
            rounds.incrementAndGet();
            return response;
        };
        LlmGateway llm = new CompanionLlmGateway(new LmStudioLlmAdapter(model), tracing);

        // Recording execution: game commands recorded but never executed; system functions (speak, recall,
        // remember, change_global_topic, ...) run so memory actually evolves - the companion's dry run.
        ExecutionGateway recordingExecution = request -> {
            SystemFunction fn = systemFunctions.get(request.toolName());
            JsonObject result = null;
            if (fn != null) {
                try {
                    result = fn.handle(request.toolName(), request.arguments(), "");
                } catch (Exception ignored) {
                    // a system-function failure in the probe must not abort the turn
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

        Files.createDirectories(TRACE_FILE.getParent());
        Files.writeString(TRACE_FILE, "Companion memory-probe - " + Instant.now() + " (model=" + model + ")\n",
                StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        System.out.println("Companion memory-probe trace -> " + TRACE_FILE);
        Thread.sleep(1500);
    }

    @AfterAll
    void shutdown() {
        if (gate != null) {
            gate.stop();
        }
    }

    @Test
    void probesMemoryRecallAcrossTiers() throws Exception {
        List<String> report = new ArrayList<>();
        report.add(String.format("%-28s | %-24s | %-24s | %-5s | %s", "probe", "located tier", "recall(scope/topic)", "hit", "outcome"));
        report.add("-".repeat(112));

        // Phase A - short-term (hot timeline): plant a fact and probe it immediately, before any eviction.
        // It is still in the prompt's session memory timeline, so a correct answer needs no recall at all.
        say(shortTerm.plantInput());
        scoreProbe(shortTerm, report);

        // Phase B - mid-term / llm_memory: plant facts pinned to their topic, push them out of short-term
        // with filler, then probe. The hot timeline no longer holds them, so the answer must come from recall.
        say("let's talk about trade");
        say(probes.get(1).plantInput());
        say("let's talk about navigation");
        say(probes.get(2).plantInput());
        say(probes.get(0).plantInput()); // remember -> llm_memory (topic-independent)
        for (String filler : FILLER) {
            say(filler);
        }
        for (Probe p : probes) {
            scoreProbe(p, report);
        }

        writeReport(report);
        assertFalse(allLatenciesMs.isEmpty(), "the local model was never reached - see the trace and LM Studio settings");
    }

    /** Asks one probe and scores it from memory state (tier), the turn's recall calls, and the spoken answer. */
    private void scoreProbe(Probe p, List<String> report) throws Exception {
        String tier = locatedTier(p);
        String midTopic = midTermTopic(p.locatorToken());
        String hotBefore = shortTermSnippet(); // the timeline the model will actually see this turn

        beginTurn();
        ask(p.probeQuestion());

        List<String> spoken = spokenTexts();
        List<Executed> recalls = callsNamed("recall");
        boolean hit = containsToken(spoken, p.answerKeyword());
        boolean clarify = !hit && cued(spoken, CLARIFY_CUES);
        boolean honestMiss = !hit && !clarify && cued(spoken, MISS_CUES);
        boolean recallRight = recallIsCorrect(p, recalls, midTopic);
        String outcome = hit ? "HIT" : clarify ? "clarify" : honestMiss ? "honest miss"
                : spoken.isEmpty() ? "silent" : "WRONG/fabricated";

        report.add(String.format("%-28s | %-24s | %-24s | %-5s | %s",
                p.label(), tier, describeRecalls(recalls) + (recallRight ? " ok" : ""), hit ? "yes" : "no", outcome));
        traceProbe(p, tier, midTopic, hotBefore, recalls, recallRight, spoken, hit, outcome);
    }

    private static boolean cued(List<String> texts, List<String> cues) {
        return texts.stream().anyMatch(s -> cues.stream().anyMatch(c -> s.toLowerCase(Locale.ROOT).contains(c)));
    }

    /** Compact view of what is in the hot short-term timeline right now (what the model sees this turn). */
    private String shortTermSnippet() {
        return memory.readShortTermTimeline().stream()
                .map(e -> "[" + e.topic().name().toLowerCase(Locale.ROOT) + "] " + e.content())
                .toList().toString();
    }

    private static final List<String> FILLER = List.of(
            "what is our current location", "set speed to fifty percent", "are there any stations nearby",
            "what's the security level of this system", "how much cargo space do we have", "what time is it",
            "scan for signals in the system", "what's our heading", "show me the navigation panel",
            "are there any missions available", "what's the nearest scoopable star", "how many jumps to the bubble",
            "what's in our cargo hold", "any contacts on radar");

    // --- driving the real system over the bus ---

    private void say(String input) throws Exception {
        beginTurn();
        ask(input);
    }

    private void ask(String input) throws Exception {
        GameEventBus.publish(new UserInputEvent(input));
        awaitIdle();
    }

    private void beginTurn() {
        turnCalls.clear();
    }

    private void awaitIdle() throws InterruptedException {
        long deadline = System.currentTimeMillis() + TURN_TIMEOUT_MS;
        while (!dispatcher.isIdle() && System.currentTimeMillis() < deadline) {
            Thread.sleep(POLL_MS);
        }
    }

    // --- scoring helpers ---

    /** Where the planted fact currently lives (may span tiers); LOST when found nowhere. */
    private String locatedTier(Probe p) {
        String tok = p.locatorToken().toLowerCase(Locale.ROOT);
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

    /** The mid-term topic whose memory holds the token (lowercase enum name), or null. */
    private String midTermTopic(String token) {
        String tok = token.toLowerCase(Locale.ROOT);
        for (ConversationTopic topic : memory.indexes().topicsWithMemory()) {
            if (memory.recallTopicMemory(topic, null, 100).stream().anyMatch(e -> contains(e.content(), tok))) {
                return topic.name().toLowerCase(Locale.ROOT);
            }
        }
        return null;
    }

    /** The text passed to every speak call this turn. */
    private List<String> spokenTexts() {
        List<String> texts = new ArrayList<>();
        for (Executed call : callsNamed("speak")) {
            if (call.args().has("text") && !call.args().get("text").isJsonNull()) {
                texts.add(call.args().get("text").getAsString());
            }
        }
        return texts;
    }

    private List<Executed> callsNamed(String tool) {
        return turnCalls.stream().filter(c -> c.tool().equals(tool)).toList();
    }

    /** Right recall = llm_memory probe recalled scope=llm_memory; topic probe recalled the topic that holds it. */
    private boolean recallIsCorrect(Probe p, List<Executed> recalls, String midTopic) {
        for (Executed r : recalls) {
            String scope = str(r.args(), "scope");
            String topic = str(r.args(), "topic");
            if (midTopic == null) { // fact is in llm_memory (or lost): the right move is an llm_memory recall
                if (scope.equals("llm_memory")) {
                    return true;
                }
            } else if (scope.equals("topic_memory") && topic.equalsIgnoreCase(midTopic)) {
                return true;
            }
        }
        return false;
    }

    private static String describeRecalls(List<Executed> recalls) {
        if (recalls.isEmpty()) {
            return "(none)";
        }
        List<String> parts = new ArrayList<>();
        for (Executed r : recalls) {
            String scope = str(r.args(), "scope");
            String topic = str(r.args(), "topic");
            parts.add(scope + (topic.isEmpty() ? "" : "/" + topic));
        }
        return String.join(",", parts);
    }

    private static boolean containsToken(List<String> texts, String token) {
        return texts.stream().anyMatch(t -> contains(t, token));
    }

    private static boolean contains(String haystack, String needleLower) {
        return haystack != null && haystack.toLowerCase(Locale.ROOT).contains(needleLower);
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }

    // --- trace ---

    private void traceProbe(Probe p, String tier, String midTopic, String hotBefore, List<Executed> recalls,
                            boolean recallRight, List<String> spoken, boolean hit, String outcome) throws Exception {
        StringBuilder t = new StringBuilder();
        t.append("\n========================================================================\n");
        t.append("PROBE: ").append(p.label()).append("\n");
        t.append("  planted: \"").append(p.plantInput()).append("\"\n");
        t.append("  asked  : \"").append(p.probeQuestion()).append("\"\n");
        t.append("  located tier at probe time: ").append(tier);
        if (midTopic != null) {
            t.append(" (mid-term topic = ").append(midTopic).append(")");
        }
        t.append("\n");
        t.append("  hot timeline at probe time: ").append(hotBefore).append("\n");
        t.append("  recall calls: ").append(describeRecalls(recalls)).append(recallRight ? "  [correct scope/topic]" : "").append("\n");
        for (Executed r : recalls) {
            t.append("    recall args=").append(r.args()).append(" -> ").append(r.result()).append("\n");
        }
        t.append("  spoken: ").append(spoken).append("\n");
        t.append("  answer keyword '").append(p.answerKeyword()).append("': ").append(hit ? "present (HIT)" : "absent").append("\n");
        t.append("  outcome: ").append(outcome).append("\n");
        System.out.print(t);
        Files.writeString(TRACE_FILE, t.toString(), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }

    private void writeReport(List<String> report) throws Exception {
        long total = allLatenciesMs.stream().mapToLong(Long::longValue).sum();
        StringBuilder s = new StringBuilder("\n======== MEMORY PROBE REPORT ========\n");
        report.forEach(line -> s.append(line).append("\n"));
        s.append("\nLLM rounds: ").append(rounds.get()).append(", total LLM time: ").append(total).append(" ms\n");
        System.out.print(s);
        Files.writeString(TRACE_FILE, s.toString(), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }
}
