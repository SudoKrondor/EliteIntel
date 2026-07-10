package elite.intel.companion.input;

import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.IntelAction;
import elite.intel.ai.brain.actions.command.CommandRegistry;
import elite.intel.ai.brain.actions.handlers.CommandHandlerFactory;
import elite.intel.ai.brain.actions.handlers.QueryHandlerFactory;
import elite.intel.ai.brain.actions.query.QueryRegistry;
import elite.intel.companion.CompanionRuntime;
import elite.intel.companion.diag.CompanionDiagnostics;
import elite.intel.companion.execution.ExecutionGateway;
import elite.intel.companion.mind.ThoughtDispatcher;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.prompt.IntelActionAccessPolicy;
import elite.intel.companion.tools.SpeakFunction;
import elite.intel.companion.tools.SystemFunction;
import elite.intel.companion.tools.SystemFunctionRegistry;
import elite.intel.util.json.JsonUtils;
import elite.intel.db.util.Database;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.UserInputEvent;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.i18n.Language;
import elite.intel.session.Status;
import elite.intel.session.SystemSession;
import elite.intel.util.Cypher;
import elite.intel.ui.event.AppLogDebugEvent;
import elite.intel.ui.event.AppLogEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    /** End-to-end timing for one routed input, measured after the harness has already completed {@link #boot()}. */
    public record TurnTiming(long timeToFirstToolMillis, long timeToIdleMillis, boolean settled) {}

    // StatusFlags bit values (mirror elite.intel.session.StatusFlags). The companion path enforces
    // isVisibleForLLM, so each test forces the game context its target command needs (see applyStateFor).
    private static final long FLAG_DOCKED = 1L;
    private static final long FLAG_LANDED = 2L;
    private static final long FLAG_SUPERCRUISE = 16L;
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
    };

    private final Language language;
    private final List<String> turnTools = new CopyOnWriteArrayList<>();
    // The phrases the companion LLM spoke this turn (speak.text), captured so a failed assertion can show the
    // model's actual answer, not just the dispatched tool names.
    private final List<String> turnSpeech = new CopyOnWriteArrayList<>();
    // Timing diagnostics are emitted through UiBus in production; capture just the latency-related subset here
    // so Gradle/IDE local-integration output can show it through stdout.
    private final List<String> turnLatencyDiagnostics = new CopyOnWriteArrayList<>();
    private final Map<String, IntelAction> actionsById = new HashMap<>();
    private final AtomicLong firstToolNanos = new AtomicLong();

    private volatile TurnTiming lastTurnTiming = new TurnTiming(-1, -1, false);

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
        // FUNCTIONS (classify_turn, speak, memory_search) are internal companion mechanics with no external
        // side effect (speak only publishes an event and no TTS engine is started), and the turn flow depends
        // on their real results - e.g. classify_turn returns isQuestion, which drives whether the companion
        // dispatches a data query or just speaks - so they run for real. Every tool name is still recorded.
        ExecutionGateway recording = request -> {
            String toolName = request.toolName();
            firstToolNanos.compareAndSet(0L, System.nanoTime());
            turnTools.add(toolName);
            if (SpeakFunction.ID.equals(toolName)) {
                String spoken = JsonUtils.getAsStringOrEmpty(request.arguments(), SpeakFunction.PARAM_TEXT);
                if (!spoken.isBlank()) {
                    turnSpeech.add(spoken);
                }
            }
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

        gate = new CompanionSubsystemGate(null, recording); // null LLM override → production companion LLM gateway
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
        turnSpeech.clear();
        turnLatencyDiagnostics.clear();
        firstToolNanos.set(0L);
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
        return List.copyOf(turnTools);
    }

    /** Returns the end-to-end measurement recorded by the most recent {@link #route(String)} call. */
    public TurnTiming lastTurnTiming() {
        return lastTurnTiming;
    }

    /** Captures info-level companion latency diagnostics for the active local-integration turn. */
    @Subscribe
    public void onAppLog(AppLogEvent event) {
        captureLatencyDiagnostic(event.getData());
    }

    /** Captures detail-level companion latency diagnostics for the active local-integration turn. */
    @Subscribe
    public void onAppLogDebug(AppLogDebugEvent event) {
        captureLatencyDiagnostic(event.getData());
    }

    private void captureLatencyDiagnostic(String line) {
        if (isLatencyDiagnostic(line)) {
            turnLatencyDiagnostics.add(line);
        }
    }

    private static boolean isLatencyDiagnostic(String line) {
        return line != null && line.startsWith("Companion ")
                && (line.contains(" intake:")
                || line.contains(" semantic-reflex:")
                || line.contains(" compose:")
                || line.contains(" llm:")
                || line.contains(" llm-http:")
                || line.contains(" latency:")
                || line.contains(" exec-time:")
                || line.contains(" done:"));
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
        Status status = Status.getInstance();
        GameEvents.StatusEvent snapshot = status.getStatus();
        long savedFlags = snapshot.getFlags();
        long savedFlags2 = snapshot.getFlags2();
        try {
            applyStateFor(expectedAction, status, snapshot);
            List<String> tools = route(input);
            if (!tools.contains(expectedAction)) {
                // On failure also surface what the semantic reducer offered for this input, so a miss can be told
                // apart at a glance: the reducer never proposed the target (reducer/alias problem) vs. it proposed
                // it but the companion LLM did not pick it (prompt/model problem).
                fail("Input: \"" + input + "\" → dispatched " + tools + " but expected \"" + expectedAction
                        + "\"; reducer selected " + reducerSelection()
                        + System.lineSeparator() + "LLM said: " + (turnSpeech.isEmpty() ? "<nothing>" : String.join(" | ", turnSpeech))
                        + System.lineSeparator() + "Timing: " + lastTurnTiming);
            }
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
     * categories over {@link elite.intel.companion.mind.CompanionState#lastCommanderMatchInput() the last
     * commander match input} (the exact normalized phrase the turn's reducer saw) reproduces that turn's
     * selection. Best-effort: never masks the routing assertion, so any lookup failure renders as a note.
     */
    private String reducerSelection() {
        try {
            String matchInput = CompanionRuntime.state().lastCommanderMatchInput();
            return CompanionDiagnostics.names(CompanionRuntime.reducer().selectTools(
                    new IntelActionAccessPolicy().allowedCategories(ThoughtSource.COMMANDER), matchInput));
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
