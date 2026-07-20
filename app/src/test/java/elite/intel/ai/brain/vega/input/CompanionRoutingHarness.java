package elite.intel.ai.brain.vega.input;

import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.IntelAction;
import elite.intel.ai.brain.actions.handlers.commands.CommandRegistry;
import elite.intel.ai.brain.actions.handlers.CommandHandlerFactory;
import elite.intel.ai.brain.actions.handlers.QueryHandlerFactory;
import elite.intel.ai.brain.actions.handlers.queries.QueryRegistry;
import elite.intel.ai.brain.vega.CompanionRuntime;
import elite.intel.ai.brain.vega.diag.CompanionDiagnostics;
import elite.intel.ai.brain.vega.execution.ExecutionGateway;
import elite.intel.ai.brain.vega.input.CompanionSubsystemGate;
import elite.intel.ai.brain.vega.memory.facts.MemoryFactSourceRegistry;
import elite.intel.ai.brain.vega.mind.ThoughtDispatcher;
import elite.intel.ai.brain.vega.model.GameStateSnapshot;
import elite.intel.ai.brain.vega.model.ThoughtSource;
import elite.intel.ai.brain.vega.prompt.IntelActionAccessPolicy;
import elite.intel.ai.brain.vega.speech.SpeechGateway;
import elite.intel.ai.brain.vega.tools.RequestInputFunction;
import elite.intel.ai.brain.vega.tools.SystemFunction;
import elite.intel.ai.brain.vega.tools.SystemFunctionRegistry;
import elite.intel.db.util.Database;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.UserInputEvent;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.i18n.Language;
import elite.intel.session.Status;
import elite.intel.session.SystemSession;
import elite.intel.ui.event.AppLogDebugEvent;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.ui.event.CommanderMatchInputChangedEvent;
import elite.intel.util.Cypher;
import elite.intel.util.json.JsonUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Boots the real companion subsystem the production way and drives commander input over the bus, so the
 * {@code NaturalSpeech*IntegrationTest} routing suites exercise the actual companion path (reflex gate →
 * semantic reducer → companion LLM → execution) rather than the retiring legacy brain.
 * <p>
 * Lives in {@code companion.input} alongside {@link CompanionEvalHarness}, whose recording-gateway pattern it
 * mirrors: the companion LLM is the production one ({@code CompanionLlmGatewayFactory.create()} via the gate's
 * null-override), but execution is a recording gateway so game COMMANDS are captured, never executed (no
 * keystrokes), while QUERIES and system functions run for real (read-only) so a turn settles the production
 * way. Every dispatched tool name - reflex or LLM-selected - is captured, since {@code Thought.execute} routes
 * all game actions through this gateway.
 */
public final class CompanionRoutingHarness {

    private static final long TURN_START_GRACE_MS = 15_000;
    private static final long TURN_TIMEOUT_MS = 90_000;
    private static final long POLL_MS = 50;

    /**
     * End-to-end timing for one routed input, measured after the harness has already completed {@link #boot()}.
     */
    public record TurnTiming(long timeToFirstToolMillis, long timeToIdleMillis, boolean settled) {
    }

    // StatusFlags bit values (mirror elite.intel.session.StatusFlags). The companion path enforces
    // isVisibleForLLM, so each test forces the game context its target command needs (see applyStateFor).
    private static final long FLAG_DOCKED = 1L;
    private static final long FLAG_LANDED = 2L;
    private static final long FLAG_SUPERCRUISE = 16L;
    private static final long FLAG_IN_WING = 128L;
    private static final long FLAG_HAS_LAT_LONG = 2_097_152L;
    private static final long FLAG_IN_MAIN_SHIP = 16_777_216L;
    private static final long FLAG_IN_SRV = 67_108_864L;
    private static final long FLAG2_ON_FOOT = 1L; // flags2

    /**
     * Candidate {@code {flags, flags2}} game contexts, tried in order; the first in which the target command
     * reports {@code isVisibleForLLM} is applied for that test. Main-ship-in-normal-space is first, so all
     * state-independent and main-ship commands take it; the rarer contexts cover the gated commands.
     */
    private static final long[][] PROBE_STATES = {
            {FLAG_IN_MAIN_SHIP, 0L},                     // piloting the main ship, normal space
            {FLAG_IN_MAIN_SHIP | FLAG_SUPERCRUISE, 0L},  // in supercruise
            {FLAG_IN_MAIN_SHIP | FLAG_DOCKED, 0L},       // docked
            {FLAG_IN_MAIN_SHIP | FLAG_LANDED, 0L},       // landed
            {FLAG_IN_SRV, 0L},                           // in the SRV
            {0L, FLAG2_ON_FOOT},                         // on foot
            {FLAG_IN_MAIN_SHIP | FLAG_HAS_LAT_LONG, 0L}, // flying over a planetary surface
            {FLAG_IN_MAIN_SHIP | FLAG_IN_WING, 0L},      // in a wing
    };

    private final Language language;
    private final List<String> turnTools = new CopyOnWriteArrayList<>();
    private final List<RoutedCall> turnCalls = new CopyOnWriteArrayList<>();
    // The phrases submitted to speech this turn, captured so a failed assertion can show the model's actual
    // answer, including a direct request_input question, not just dispatched tool names.
    private final List<String> turnSpeech = new CopyOnWriteArrayList<>();
    private final List<String> turnClarificationDiagnostics = new CopyOnWriteArrayList<>();
    // Turn diagnostics are emitted through UiBus in production; capture latency plus the exact inlined facts here
    // so Gradle/IDE local-integration output shows the model context used for routing.
    private final List<String> turnLatencyDiagnostics = new CopyOnWriteArrayList<>();
    // A failed assertion needs every model attempt made during THIS route, including retry/continuation calls.
    private final List<String> turnLlmTranscript = new CopyOnWriteArrayList<>();
    private final Map<String, IntelAction> actionsById = new HashMap<>();
    private final AtomicLong firstToolNanos = new AtomicLong();

    private volatile TurnTiming lastTurnTiming = new TurnTiming(-1, -1, false);
    private volatile String lastMatchInput = "";
    private volatile GameStateSnapshot lastGameStateSnapshot;

    private CompanionSubsystemGate gate;
    private ThoughtDispatcher dispatcher;
    private Language previousLanguage;
    private long bootSavedFlags;
    private long bootSavedFlags2;
    private boolean statusSaved;
    private boolean uiRegistered;

    public CompanionRoutingHarness(Language language) {
        this.language = language;
    }

    private record RoutedCall(String toolName, JsonObject arguments) {
    }

    /**
     * Rebuilds the runtime graph so an ordered live scenario starts with empty session conversation state.
     */
    public void restart() throws Exception {
        shutdown();
        boot();
    }

    /**
     * Boots the companion subsystem pinned to this harness's language; call once per suite.
     */
    public void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close(); // init() returns an open pooled handle; close it so the pool isn't starved
        previousLanguage = SystemSession.getInstance().getLanguage();
        SystemSession.getInstance().setLanguage(language);
        CommandRegistry.getInstance().load();
        QueryRegistry.getInstance().load();
        MemoryFactSourceRegistry.getInstance().load();
        SystemFunctionRegistry registry = SystemFunctionRegistry.getInstance();
        if (registry.byId().isEmpty()) {
            registry.load();
        }
        Map<String, SystemFunction> systemFunctions = registry.byId();
        // id -> action, used ONLY to read each target command's isVisibleForLLM (a pure predicate, no execution)
        // when choosing the test's game context.
        actionsById.putAll(CommandHandlerFactory.getInstance().registerCommandHandlers());
        QueryHandlerFactory.getInstance().registerQueryHandlers().forEach(actionsById::put);

        // Routing-only execution. Game COMMANDS and QUERIES are recorded but NEVER executed - running them
        // would press keys (commands) or call third-party REST APIs (queries: EDSM/Spansh). But SYSTEM
        // FUNCTIONS are internal companion mechanics with no external game side effect, so they run for real.
        // Every tool name is still recorded. memory_search is a game query and therefore remains recording-only.
        ExecutionGateway recording = request -> {
            String toolName = request.toolName();
            firstToolNanos.compareAndSet(0L, System.nanoTime());
            turnTools.add(toolName);
            JsonObject arguments = request.arguments() == null
                    ? new JsonObject()
                    : request.arguments().deepCopy();
            turnCalls.add(new RoutedCall(toolName, arguments));
            SystemFunction fn = systemFunctions.get(toolName);
            if (fn != null) {
                try {
                    JsonObject result = fn.handle(toolName, request.arguments(), "");
                    if (result != null) {
                        return CompletableFuture.completedFuture(result);
                    }
                } catch (Exception ignored) {
                    // a system-function failure must not abort the turn
                }
            }
            JsonObject recorded = new JsonObject();
            recorded.addProperty("status", "recorded");
            return CompletableFuture.completedFuture(recorded);
        };

        // Capture every phrase at the actual speech boundary. request_input is intentionally handled inside
        // CommanderThought rather than through ExecutionGateway, so this is the observable proof that its
        // question was voiced; it also keeps local-integration runs silent.
        SpeechGateway recordingSpeech = request -> {
            turnSpeech.add(request.text());
            return CompletableFuture.completedFuture(null);
        };

        // null LLM override → production companion LLM gateway
        gate = new CompanionSubsystemGate(null, recording, recordingSpeech);
        gate.start();
        dispatcher = gate.dispatcher();
        UiBus.register(this);
        uiRegistered = true;

        // Remember the tester's real status so it can be restored on shutdown; each test sets its own context.
        GameEvents.StatusEvent snapshot = Status.getInstance().getStatus();
        bootSavedFlags = snapshot.getFlags();
        bootSavedFlags2 = snapshot.getFlags2();
        statusSaved = true;

        Thread.sleep(1500); // let the graph settle before the first turn
    }

    /**
     * Stops the subsystem and restores the previous language and status snapshot; safe if never booted.
     */
    public void shutdown() {
        if (uiRegistered) {
            UiBus.unregister(this);
            uiRegistered = false;
        }
        if (gate != null) {
            gate.stop();
            gate = null;
        }
        if (statusSaved) {
            GameEvents.StatusEvent snapshot = Status.getInstance().getStatus();
            snapshot.setFlags(bootSavedFlags);
            snapshot.setFlags2(bootSavedFlags2);
            Status.getInstance().setStatus(snapshot);
            statusSaved = false;
        }
        if (previousLanguage != null) {
            SystemSession.getInstance().setLanguage(previousLanguage);
            previousLanguage = null;
        }
    }

    /**
     * Publishes the commander input the production way, waits for the turn to settle, and returns the tool
     * names the companion dispatched this turn (game action plus any system functions such as speak).
     */
    public List<String> route(String input) throws InterruptedException {
        turnTools.clear();
        turnCalls.clear();
        turnSpeech.clear();
        turnClarificationDiagnostics.clear();
        turnLatencyDiagnostics.clear();
        turnLlmTranscript.clear();
        firstToolNanos.set(0L);
        lastMatchInput = "";
        lastGameStateSnapshot = null;
        lastTurnTiming = new TurnTiming(-1, -1, false);
        long turnStartedNanos = System.nanoTime();
        GameEventBus.publish(new UserInputEvent(input));
        // Wait for the turn to start: the dispatcher goes busy, or a tool is already recorded (a fast reflex may
        // finish before we observe "busy"). The grace tolerates a slow LLM first token; exiting on a recorded
        // tool keeps the reflex path fast. Without this a slow start returned an empty [] dispatch.
        long startBy = System.currentTimeMillis() + TURN_START_GRACE_MS;
        while (dispatcher.isIdle() && turnTools.isEmpty() && System.currentTimeMillis() < startBy) {
            Thread.sleep(POLL_MS);
        }
        // Then wait for it to finish.
        long deadline = System.currentTimeMillis() + TURN_TIMEOUT_MS;
        while (!dispatcher.isIdle() && System.currentTimeMillis() < deadline) {
            Thread.sleep(POLL_MS);
        }
        long firstToolAtNanos = firstToolNanos.get();
        long firstToolMillis = firstToolAtNanos == 0L ? -1
                : TimeUnit.NANOSECONDS.toMillis(firstToolAtNanos - turnStartedNanos);
        long idleMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - turnStartedNanos);
        boolean settled = dispatcher.isIdle();
        lastTurnTiming = new TurnTiming(firstToolMillis, idleMillis, settled);
        printTurnLatency(input, lastTurnTiming);
        captureLlmTranscript(input);
        return List.copyOf(turnTools);
    }

    /**
     * Returns the end-to-end measurement recorded by the most recent {@link #route(String)} call.
     */
    public TurnTiming lastTurnTiming() {
        return lastTurnTiming;
    }

    /**
     * Returns the phrases submitted to the speech gateway during the most recent turn.
     */
    public List<String> lastTurnSpeech() {
        return List.copyOf(turnSpeech);
    }

    /**
     * Whether the most recent turn opened the expected validated request_input continuation.
     */
    public boolean lastTurnRequestedInput(String actionId, String parameterName) {
        String expected = " settle: request_input " + actionId + "." + parameterName;
        return turnClarificationDiagnostics.stream().anyMatch(line -> line.contains(expected));
    }

    /**
     * Returns one argument from the most recent recorded invocation of {@code toolName}.
     */
    public Optional<String> lastArgument(String toolName, String parameterName) {
        for (int i = turnCalls.size() - 1; i >= 0; i--) {
            RoutedCall call = turnCalls.get(i);
            if (toolName.equals(call.toolName())) {
                String value = JsonUtils.getAsStringOrEmpty(call.arguments(), parameterName);
                return value.isBlank() ? Optional.empty() : Optional.of(value);
            }
        }
        return Optional.empty();
    }

    /**
     * Captures info-level companion latency diagnostics for the active local-integration turn.
     */
    @Subscribe
    public void onAppLog(AppLogEvent event) {
        captureTurnDiagnostic(event.getData());
    }

    /**
     * Captures detail-level companion latency diagnostics for the active local-integration turn.
     */
    @Subscribe
    public void onAppLogDebug(AppLogDebugEvent event) {
        captureTurnDiagnostic(event.getData());
    }

    /**
     * Captures the exact per-turn normalized input without storing it in shared companion runtime state.
     */
    @Subscribe
    public void onCommanderMatchInputChanged(CommanderMatchInputChangedEvent event) {
        lastMatchInput = event.text();
        lastGameStateSnapshot = event.gameStateSnapshot();
    }

    private void captureTurnDiagnostic(String line) {
        if (line != null && line.contains(" settle: request_input ")) {
            turnClarificationDiagnostics.add(line);
            firstToolNanos.compareAndSet(0L, System.nanoTime());
            turnTools.add(RequestInputFunction.ID);
        }
        if (isLatencyDiagnostic(line)) {
            turnLatencyDiagnostics.add(line);
        }
    }

    private static boolean isLatencyDiagnostic(String line) {
        return line != null && line.startsWith("Companion ")
                && (line.contains(" intake:")
                || line.contains(" compose:")
                || line.contains(" facts:")
                || line.contains(" llm:")
                || line.contains(" llm-http:")
                || line.contains(" latency:")
                || line.contains(" exec-time:")
                || line.contains(" done:"));
    }

    /**
     * Retains every model-facing diagnostic emitted during the completed turn for a later assertion failure.
     */
    private void captureLlmTranscript(String input) {
        List<String> llmLines = turnLatencyDiagnostics.stream()
                .filter(CompanionRoutingHarness::isLlmDiagnostic)
                .toList();
        if (llmLines.isEmpty()) {
            return;
        }
        turnLlmTranscript.add("=== LLM turn: \"" + input + "\" ===");
        turnLlmTranscript.addAll(llmLines);
    }

    private static boolean isLlmDiagnostic(String line) {
        return line != null && (line.contains(" compose:")
                || line.contains(" facts:")
                || line.contains(" llm:")
                || line.contains(" llm-http:"));
    }

    private String llmTranscript() {
        return turnLlmTranscript.isEmpty()
                ? "<no LLM calls in current turn>"
                : String.join(System.lineSeparator(), turnLlmTranscript);
    }

    private void printTurnLatency(String input, TurnTiming timing) {
        System.out.printf("%n=== Companion turn latency: \"%s\" ===%n", input);
        turnLatencyDiagnostics.forEach(System.out::println);
        System.out.printf("Companion routing timing: first-tool=%d ms, idle=%d ms, settled=%s%n",
                timing.timeToFirstToolMillis(), timing.timeToIdleMillis(), timing.settled());
    }

    /**
     * Asserts the expected action id is among the tools the companion dispatched for the input. The game
     * context is set, for this test only, to the first {@link #PROBE_STATES state} in which the target command
     * is visible to the router (the companion path enforces {@code isVisibleForLLM}); the prior status snapshot
     * is restored afterwards.
     */
    public void assertRouted(String input, String expectedAction) throws InterruptedException {
        List<String> tools = routeWithActionVisible(input, expectedAction);
        if (!tools.contains(expectedAction)) {
            // On failure also surface what the semantic reducer offered for this input, so a miss can be told
            // apart at a glance: the reducer never proposed the target (reducer/alias problem) vs. it proposed
            // it but the companion LLM did not pick it (prompt/model problem).
            fail("Input: \"" + input + "\" → dispatched " + tools + " but expected \"" + expectedAction
                    + "\"; reducer selected " + reducerSelection()
                    + System.lineSeparator() + "LLM said: " + (turnSpeech.isEmpty() ? "<nothing>" : String.join(" | ", turnSpeech))
                    + System.lineSeparator() + "Timing: " + lastTurnTiming
                    + System.lineSeparator() + "LLM transcript for current turn:"
                    + System.lineSeparator() + llmTranscript());
        }
    }

    /**
     * Routes one input while forcing a game state in which {@code visibleAction} is offered to the LLM.
     */
    public List<String> routeWithActionVisible(String input, String visibleAction) throws InterruptedException {
        Status status = Status.getInstance();
        GameEvents.StatusEvent snapshot = status.getStatus();
        long savedFlags = snapshot.getFlags();
        long savedFlags2 = snapshot.getFlags2();
        try {
            applyStateFor(visibleAction, status, snapshot);
            return route(input);
        } finally {
            snapshot.setFlags(savedFlags);
            snapshot.setFlags2(savedFlags2);
            status.setStatus(snapshot);
            status.setFighterOut(false);
        }
    }

    /**
     * Re-runs the semantic reducer for the turn that just failed and renders the tool names it selected. The
     * reducer is deterministic on {@code (allowedCategories, matchInput)}, so replaying it with the commander
     * categories over the last {@link CommanderMatchInputChangedEvent} value (the exact normalized phrase the
     * turn's reducer saw) reproduces that turn's selection. Best-effort: never masks the routing assertion, so
     * any lookup failure renders as a note.
     */
    private String reducerSelection() {
        try {
            return CompanionDiagnostics.names(CompanionRuntime.reducer().selectTools(
                    new IntelActionAccessPolicy().allowedCategories(ThoughtSource.COMMANDER),
                    lastMatchInput, null, lastGameStateSnapshot));
        } catch (RuntimeException unavailable) {
            return "<reducer unavailable: " + unavailable.getMessage() + ">";
        }
    }

    /**
     * Sets the game status to the first probe state in which {@code actionId} is visible (else main ship).
     */
    private void applyStateFor(String actionId, Status status, GameEvents.StatusEvent snapshot) {
        IntelAction action = actionsById.get(actionId);
        status.setFighterOut(false);
        if (action != null) {
            for (long[] candidate : PROBE_STATES) {
                snapshot.setFlags(candidate[0]);
                snapshot.setFlags2(candidate[1]);
                status.setStatus(snapshot);
                if (action.isVisibleForLLM(status)) {
                    return; // this context makes the target visible
                }
            }
            // Fighter orders gate on isFighterOut (a Status boolean, not a flag): try with a fighter deployed.
            snapshot.setFlags(FLAG_IN_MAIN_SHIP);
            snapshot.setFlags2(0L);
            status.setStatus(snapshot);
            status.setFighterOut(true);
            if (action.isVisibleForLLM(status)) {
                return;
            }
            status.setFighterOut(false);
        }
        snapshot.setFlags(FLAG_IN_MAIN_SHIP); // fallback
        snapshot.setFlags2(0L);
        status.setStatus(snapshot);
    }
}
