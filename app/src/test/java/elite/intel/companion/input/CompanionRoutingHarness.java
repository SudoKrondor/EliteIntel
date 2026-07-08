package elite.intel.companion.input;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.IntelAction;
import elite.intel.ai.brain.actions.command.CommandRegistry;
import elite.intel.ai.brain.actions.handlers.CommandHandlerFactory;
import elite.intel.ai.brain.actions.handlers.QueryHandlerFactory;
import elite.intel.ai.brain.actions.query.QueryRegistry;
import elite.intel.companion.execution.ExecutionGateway;
import elite.intel.companion.mind.ThoughtDispatcher;
import elite.intel.companion.tools.SystemFunction;
import elite.intel.companion.tools.SystemFunctionRegistry;
import elite.intel.db.util.Database;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.UserInputEvent;
import elite.intel.gameapi.gamestate.dtos.GameEvents;
import elite.intel.i18n.Language;
import elite.intel.session.Status;
import elite.intel.session.SystemSession;
import elite.intel.util.Cypher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
    private final Map<String, IntelAction> actionsById = new HashMap<>();

    private CompanionSubsystemGate gate;
    private ThoughtDispatcher dispatcher;
    private Language previousLanguage;
    private long bootSavedFlags;
    private long bootSavedFlags2;
    private boolean statusSaved;

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
            turnTools.add(toolName);
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
        return List.copyOf(turnTools);
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
            assertTrue(tools.contains(expectedAction),
                    "Input: \"" + input + "\" → dispatched " + tools + " but expected \"" + expectedAction + "\"");
        } finally {
            snapshot.setFlags(savedFlags);
            snapshot.setFlags2(savedFlags2);
            status.setStatus(snapshot);
            status.setFighterOut(false);
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
