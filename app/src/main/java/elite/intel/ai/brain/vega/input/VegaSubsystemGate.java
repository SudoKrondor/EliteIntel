package elite.intel.ai.brain.vega.input;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.brain.i18n.ConfirmationReplies;
import elite.intel.ai.brain.vega.VegaAddressing;
import elite.intel.ai.brain.vega.VegaRuntime;
import elite.intel.ai.brain.vega.VegaRuntimeGraph;
import elite.intel.ai.brain.vega.VegaRuntimeGraphFactory;
import elite.intel.ai.brain.vega.confirm.ConfirmationCoordinator;
import elite.intel.ai.brain.vega.confirm.DangerousActionConfirmedEvent;
import elite.intel.ai.brain.vega.execution.ExecutionGateway;
import elite.intel.ai.brain.vega.llm.LlmGateway;
import elite.intel.ai.brain.vega.mind.ThoughtDispatcher;
import elite.intel.ai.brain.vega.speech.SpeechGateway;
import elite.intel.ai.mouth.subscribers.events.MissionCriticalAnnouncementEvent;
import elite.intel.ai.mouth.subscribers.events.VocalisationRequestEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.gameapi.NormalizedUserInputEvent;
import elite.intel.gameapi.UserInputEvent;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.setup.SetupCheck;
import elite.intel.ui.controller.ManagedService;
import elite.intel.ui.event.AppLogEvent;
import elite.intel.util.StringUtls;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * The single gate seam between existing input and VEGA subsystem, and the owner of the subsystem
 * lifecycle. It transactionally builds one {@link VegaRuntimeGraph}, publishes it only after its workers
 * have started, and closes that exact generation on stop or startup rollback.
 * <p>
 * Raw game events are not subscribed here. Gameplay subscribers reach the running VEGA only through
 * {@link VegaRuntime#narrator()}.
 */
public final class VegaSubsystemGate implements ManagedService {

    private volatile VegaRuntimeGraph activeGraph;
    private boolean inputSubscriptionRegistered;
    private boolean bargeInSubscriptionRegistered;

    private final LlmGateway llmOverride;
    private final ExecutionGateway executionOverride;
    private final SpeechGateway speechOverride;
    private final Supplier<Optional<String>> llmGap;
    private final AtomicBoolean llmGapAnnounced = new AtomicBoolean();

    public VegaSubsystemGate() {
        this(null, null, null, SetupCheck::llmGapSpeechKey);
    }

    /**
     * Test/eval seam. Non-null overrides transfer lifecycle ownership to this gate for the duration of a start.
     */
    public VegaSubsystemGate(LlmGateway llmOverride, ExecutionGateway executionOverride) {
        this(llmOverride, executionOverride, null);
    }

    /**
     * As above, plus a speech override. Diagnostics uses this to retain the real audible/chat path while game
     * command execution is recorded. LLM and execution overrides are closed through their gateway contracts.
     */
    public VegaSubsystemGate(
            LlmGateway llmOverride,
            ExecutionGateway executionOverride,
            SpeechGateway speechOverride
    ) {
        this(llmOverride, executionOverride, speechOverride, Optional::empty);
    }

    /**
     * As above, plus what stops a model running, as the key of the line that says so. Only the production gate reads
     * the settings: every override runs on a model it brought or on a harness that asks for whatever is loaded.
     */
    VegaSubsystemGate(
            LlmGateway llmOverride,
            ExecutionGateway executionOverride,
            SpeechGateway speechOverride,
            Supplier<Optional<String>> llmGap
    ) {
        this.llmOverride = llmOverride;
        this.executionOverride = executionOverride;
        this.speechOverride = speechOverride;
        this.llmGap = llmGap;
    }

    /**
     * Commander voice input gate. With no model set up nothing goes further, not even a phrase the reflex could
     * resolve: a few commands working out of the many gives the impression of an app that works badly, when what it
     * needs is a language model.
     * <p>
     * While a dangerous action waits for confirmation, the next utterance answers it:
     * a yes runs it, a no discards it, and anything else discards it and is then handled as the new request it
     * is - the commander has moved on, and a destructive action never runs on anything short of a clear yes.
     */
    @Subscribe
    public void onUserInput(UserInputEvent event) {
        VegaRuntimeGraph runtimeGraph = activeGraph;
        if (runtimeGraph == null) {
            return;
        }
        Optional<String> gap = llmGap.get();
        if (gap.isPresent()) {
            sayWhatIsMissing(gap.get());
            return;
        }
        String input = event.getUserInput();
        ConfirmationCoordinator confirmation = runtimeGraph.confirmationCoordinator();
        if (confirmation.isPending()) {
            Language language = SystemSession.getInstance().getLanguage();
            switch (ConfirmationReplies.classify(language, VegaAddressing.stripLeadingName(input))) {
                case AFFIRMATIVE -> {
                    // An answer never reaches the dispatcher, so this branch echoes it to the UI/OBS listeners.
                    GameEventBus.publish(new NormalizedUserInputEvent(input));
                    confirmation.confirm();
                    return;
                }
                case NEGATIVE -> {
                    GameEventBus.publish(new NormalizedUserInputEvent(input));
                    confirmation.cancel();
                    return;
                }
                case OTHER -> confirmation.cancel(); // frees the commander lane before the new request queues on it
            }
        }
        // Everything else is echoed by the dispatcher, which alone knows the canonical form of the words.
        runtimeGraph.thoughtDispatcher().submitCommanderInput(input);
    }

    /**
     * Spoken once per service start, straight to TTS because the thing it is about is the missing model. The log line
     * is every time, so the input never just disappears.
     */
    private void sayWhatIsMissing(String speechKey) {
        UiBus.publish(new AppLogEvent("Input ignored: no language model is set up."));
        if (llmGapAnnounced.compareAndSet(false, true)) {
            GameEventBus.publish(new VocalisationRequestEvent(StringUtls.localizedSpeech(speechKey),
                    MissionCriticalAnnouncementEvent.class, false));
        }
    }

    /**
     * Routes a confirmation-bus signal to the coordinator owned by the currently active graph.
     */
    @Subscribe
    public void onDangerousActionConfirmed(DangerousActionConfirmedEvent event) {
        VegaRuntimeGraph runtimeGraph = activeGraph;
        if (runtimeGraph != null) {
            runtimeGraph.confirmationCoordinator().confirm();
        }
    }

    @Override
    public synchronized void start() {
        if (activeGraph != null) {
            return;
        }

        VegaRuntimeGraph runtimeGraph = null;
        boolean graphInstalled = false;
        boolean inputRegistered = false;
        boolean bargeInRegistered = false;
        try {
            runtimeGraph = VegaRuntimeGraphFactory.create(llmOverride, executionOverride, speechOverride);
            runtimeGraph.start();
            VegaRuntime.installGraph(runtimeGraph);
            graphInstalled = true;

            // Publish to this gate before subscribing: a synchronously delivered event always sees a complete,
            // started graph. VegaRuntime is already installed for any system function the event reaches.
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
                VegaRuntime.uninstallGraph(runtimeGraph);
            }
            closeAfterFailedStart(startupFailure, runtimeGraph);
            throw startupFailure;
        }
    }

    @Override
    public synchronized void stop() {
        VegaRuntimeGraph runtimeGraph = activeGraph;
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

        VegaRuntime.uninstallGraph(runtimeGraph);
        cleanupFailure = runCleanup(cleanupFailure, runtimeGraph::close);
        rethrowCleanupFailure(cleanupFailure);
    }

    /**
     * Test access to the live dispatcher, or {@code null} while the subsystem is stopped.
     */
    public ThoughtDispatcher dispatcher() {
        VegaRuntimeGraph runtimeGraph = activeGraph;
        return runtimeGraph == null ? null : runtimeGraph.thoughtDispatcher();
    }

    /**
     * Test access: whether a dangerous action is waiting on the commander's answer; {@code false} while stopped.
     */
    boolean hasPendingDangerousConfirmation() {
        VegaRuntimeGraph runtimeGraph = activeGraph;
        return runtimeGraph != null && runtimeGraph.confirmationCoordinator().isPending();
    }

    private void unregisterAfterFailedStart(
            Throwable startupFailure,
            VegaRuntimeGraph runtimeGraph,
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

    private static void closeAfterFailedStart(Throwable startupFailure, VegaRuntimeGraph runtimeGraph) {
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
