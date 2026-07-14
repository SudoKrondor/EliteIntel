package elite.intel.companion.input;

import com.google.common.eventbus.Subscribe;
import elite.intel.companion.CompanionConfig;
import elite.intel.companion.CompanionRuntime;
import elite.intel.companion.CompanionRuntimeGraph;
import elite.intel.companion.CompanionRuntimeGraphFactory;
import elite.intel.companion.confirm.DangerousActionConfirmedEvent;
import elite.intel.companion.execution.ExecutionGateway;
import elite.intel.companion.llm.LlmGateway;
import elite.intel.companion.mind.ThoughtDispatcher;
import elite.intel.companion.speech.SpeechGateway;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.NormalizedUserInputEvent;
import elite.intel.gameapi.UserInputEvent;
import elite.intel.ui.controller.ManagedService;

/**
 * The single gate seam between existing input and the companion subsystem, and the owner of the subsystem
 * lifecycle. It transactionally builds one {@link CompanionRuntimeGraph}, publishes it only after its workers
 * have started, and closes that exact generation on stop or startup rollback.
 * <p>
 * Raw game events are not subscribed here. Gameplay subscribers reach the running companion only through
 * {@link CompanionRuntime#narrator()}.
 */
public final class CompanionSubsystemGate implements ManagedService {

    private volatile CompanionRuntimeGraph activeGraph;
    private boolean inputSubscriptionRegistered;
    private boolean bargeInSubscriptionRegistered;

    private final LlmGateway llmOverride;
    private final ExecutionGateway executionOverride;
    private final SpeechGateway speechOverride;

    public CompanionSubsystemGate() {
        this(null, null);
    }

    /**
     * Test/eval seam. Non-null overrides transfer lifecycle ownership to this gate for the duration of a start.
     */
    public CompanionSubsystemGate(LlmGateway llmOverride, ExecutionGateway executionOverride) {
        this(llmOverride, executionOverride, null);
    }

    /**
     * As above, plus a speech override. Diagnostics uses this to retain the real audible/chat path while game
     * command execution is recorded. LLM and execution overrides are closed through their gateway contracts.
     */
    public CompanionSubsystemGate(
            LlmGateway llmOverride,
            ExecutionGateway executionOverride,
            SpeechGateway speechOverride
    ) {
        this.llmOverride = llmOverride;
        this.executionOverride = executionOverride;
        this.speechOverride = speechOverride;
    }

    /** Commander voice input gate. A spoken confirmation code word confirms a frozen dangerous action. */
    @Subscribe
    public void onUserInput(UserInputEvent event) {
        CompanionRuntimeGraph runtimeGraph = activeGraph;
        if (runtimeGraph == null) {
            return;
        }
        String input = event.getUserInput();
        // Mirror the legacy command path: surface the commander's spoken words to the UI/OBS listeners.
        if (input != null && !input.isBlank()) {
            GameEventBus.publish(new NormalizedUserInputEvent(input));
        }
        if (CompanionConfig.isConfirmationCodeWord(input)) {
            runtimeGraph.confirmationCoordinator().confirm();
            return;
        }
        runtimeGraph.thoughtDispatcher().submitCommanderInput(input);
    }

    /** Routes a confirmation-bus signal to the coordinator owned by the currently active graph. */
    @Subscribe
    public void onDangerousActionConfirmed(DangerousActionConfirmedEvent event) {
        CompanionRuntimeGraph runtimeGraph = activeGraph;
        if (runtimeGraph != null) {
            runtimeGraph.confirmationCoordinator().confirm();
        }
    }

    @Override
    public synchronized void start() {
        if (activeGraph != null) {
            return;
        }

        CompanionRuntimeGraph runtimeGraph = null;
        boolean graphInstalled = false;
        boolean inputRegistered = false;
        boolean bargeInRegistered = false;
        try {
            runtimeGraph = CompanionRuntimeGraphFactory.create(llmOverride, executionOverride, speechOverride);
            runtimeGraph.start();
            CompanionRuntime.installGraph(runtimeGraph);
            graphInstalled = true;

            // Publish to this gate before subscribing: a synchronously delivered event always sees a complete,
            // started graph. CompanionRuntime is already installed for any system function the event reaches.
            activeGraph = runtimeGraph;
            GameEventBus.register(this);
            inputRegistered = true;
            GameEventBus.register(runtimeGraph.bargeInController());
            bargeInRegistered = true;

            inputSubscriptionRegistered = true;
            bargeInSubscriptionRegistered = true;
        } catch (RuntimeException | Error startupFailure) {
            activeGraph = null;
            unregisterAfterFailedStart(startupFailure, runtimeGraph, inputRegistered, bargeInRegistered);
            if (graphInstalled) {
                CompanionRuntime.uninstallGraph(runtimeGraph);
            }
            closeAfterFailedStart(startupFailure, runtimeGraph);
            throw startupFailure;
        }
    }

    @Override
    public synchronized void stop() {
        CompanionRuntimeGraph runtimeGraph = activeGraph;
        if (runtimeGraph == null) {
            return;
        }

        // Stop new intake first. A racing EventBus call observes null and becomes a no-op before resources close.
        activeGraph = null;
        Throwable cleanupFailure = null;

        if (bargeInSubscriptionRegistered) {
            bargeInSubscriptionRegistered = false;
            cleanupFailure = runCleanup(cleanupFailure,
                    () -> GameEventBus.unregister(runtimeGraph.bargeInController()));
        }
        if (inputSubscriptionRegistered) {
            inputSubscriptionRegistered = false;
            cleanupFailure = runCleanup(cleanupFailure, () -> GameEventBus.unregister(this));
        }

        CompanionRuntime.uninstallGraph(runtimeGraph);
        cleanupFailure = runCleanup(cleanupFailure, runtimeGraph::close);
        rethrowCleanupFailure(cleanupFailure);
    }

    /** Test access to the live dispatcher, or {@code null} while the subsystem is stopped. */
    public ThoughtDispatcher dispatcher() {
        CompanionRuntimeGraph runtimeGraph = activeGraph;
        return runtimeGraph == null ? null : runtimeGraph.thoughtDispatcher();
    }

    private void unregisterAfterFailedStart(
            Throwable startupFailure,
            CompanionRuntimeGraph runtimeGraph,
            boolean inputRegistered,
            boolean bargeInRegistered
    ) {
        if (bargeInRegistered && runtimeGraph != null) {
            addCleanupFailure(startupFailure, () -> GameEventBus.unregister(runtimeGraph.bargeInController()));
        }
        if (inputRegistered) {
            addCleanupFailure(startupFailure, () -> GameEventBus.unregister(this));
        }
    }

    private static void closeAfterFailedStart(Throwable startupFailure, CompanionRuntimeGraph runtimeGraph) {
        if (runtimeGraph != null) {
            addCleanupFailure(startupFailure, runtimeGraph::close);
        }
    }

    private static void addCleanupFailure(Throwable startupFailure, Runnable cleanup) {
        try {
            cleanup.run();
        } catch (RuntimeException | Error cleanupFailure) {
            startupFailure.addSuppressed(cleanupFailure);
        }
    }

    private static Throwable runCleanup(Throwable previousFailure, Runnable cleanup) {
        try {
            cleanup.run();
            return previousFailure;
        } catch (RuntimeException | Error cleanupFailure) {
            if (previousFailure == null) {
                return cleanupFailure;
            }
            previousFailure.addSuppressed(cleanupFailure);
            return previousFailure;
        }
    }

    private static void rethrowCleanupFailure(Throwable cleanupFailure) {
        if (cleanupFailure instanceof RuntimeException runtimeFailure) {
            throw runtimeFailure;
        }
        if (cleanupFailure instanceof Error error) {
            throw error;
        }
    }
}
