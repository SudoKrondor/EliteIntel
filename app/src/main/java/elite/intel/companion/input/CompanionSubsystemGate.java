package elite.intel.companion.input;

import com.google.common.eventbus.Subscribe;
import elite.intel.companion.CompanionConfig;
import elite.intel.companion.CompanionRuntime;
import elite.intel.companion.confirm.CommandFlagDangerousActionPolicy;
import elite.intel.companion.confirm.ConfirmationCoordinator;
import elite.intel.companion.confirm.DangerousActionConfirmedEvent;
import elite.intel.companion.confirm.DangerousActionPolicy;
import elite.intel.companion.execution.CompanionExecutionGateway;
import elite.intel.companion.execution.ExecutionGateway;
import elite.intel.companion.llm.CompanionLlmGatewayFactory;
import elite.intel.companion.llm.LlmGateway;
import elite.intel.companion.memory.MidTermToLongTermConsolidator;
import elite.intel.companion.memory.SessionMemoryGateway;
import elite.intel.companion.mind.CompanionState;
import elite.intel.companion.mind.ThoughtContext;
import elite.intel.companion.mind.ThoughtDispatcher;
import elite.intel.companion.prompt.CompanionActionReducer;
import elite.intel.companion.prompt.IntelActionAccessPolicy;
import elite.intel.companion.prompt.PromptComposer;
import elite.intel.companion.prompt.WordOverlapActionReducer;
import elite.intel.companion.speech.CompanionSpeechGateway;
import elite.intel.companion.speech.SpeechGateway;
import elite.intel.companion.tools.SystemFunctionProvider;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.NormalizedUserInputEvent;
import elite.intel.gameapi.UserInputEvent;
import elite.intel.gameapi.journal.events.BaseEvent;
import elite.intel.ui.controller.ManagedService;

/**
 * The single gate seam between existing input and the companion subsystem, and the owner of the
 * subsystem lifecycle. Bootstraps the whole companion graph (gateways, memory, dispatcher, filter)
 * and, while {@code companionModeOn}, subscribes to the same voice ({@code UserInputEvent}) and
 * game-event ({@code BaseEvent}) streams as the old command mode and routes them into the
 * consciousness instead.
 * <p>
 * Lifecycle is managed by {@code AppController}; only one of the old BRAIN service or this companion
 * service is active at a time. A live {@code dispatcher} doubles as the "started" sentinel.
 */
public final class CompanionSubsystemGate implements ManagedService {

    private ThoughtDispatcher dispatcher;
    private GameEventFilter gameEventFilter;
    private ConfirmationCoordinator confirmationCoordinator;
    private BargeInController bargeInController;

    private final LlmGateway llmOverride;
    private final ExecutionGateway executionOverride;

    public CompanionSubsystemGate() {
        this(null, null);
    }

    /** Test seam: inject a recording execution gateway and/or a tracing LLM gateway for the local eval. */
    public CompanionSubsystemGate(LlmGateway llmOverride, ExecutionGateway executionOverride) {
        this.llmOverride = llmOverride;
        this.executionOverride = executionOverride;
    }

    /** Commander voice input gate. A spoken confirmation code word confirms a frozen dangerous action. */
    @Subscribe
    public void onUserInput(UserInputEvent event) {
        if (!isCompanionModeOn()) {
            return;
        }
        String input = event.getUserInput();
        // Mirror the legacy command path (PromptFactory.normalizeInput): surface the commander's spoken
        // words to the UI ("ВВОД ПОЛЬЗОВАТЕЛЯ" panel / OBS overlay), which listen on NormalizedUserInputEvent.
        if (input != null && !input.isBlank()) {
            GameEventBus.publish(new NormalizedUserInputEvent(input));
        }
        // The code word confirms a pending dangerous action; it is not a new thought (§2.13).
        if (CompanionConfig.isConfirmationCodeWord(input)) {
            confirmationCoordinator.confirm();
            return;
        }
        dispatcher.submitCommanderInput(input);
    }

    /** Game event gate (journal/status events arrive here as {@code BaseEvent}). */
    @Subscribe
    public void onGameEvent(BaseEvent event) {
        if (!isCompanionModeOn()) {
            return;
        }
        gameEventFilter.onGameEvent(event);
    }

    /**
     * Confirmation bus: the commander confirmed a frozen dangerous action. Routed to the coordinator the
     * waiting thought blocks on (§2.13); a no-op when nothing is awaiting confirmation. The actual voice
     * code-word / button that publishes this event is an input-layer task (§7.1), still to be wired.
     */
    @Subscribe
    public void onDangerousActionConfirmed(DangerousActionConfirmedEvent event) {
        if (confirmationCoordinator != null) {
            confirmationCoordinator.confirm();
        }
    }

    @Override
    public void start() {
        if (!isCompanionModeOn()) {
            return; // companion mode off: the legacy command mode serves input instead
        }
        // Assemble the companion graph (all no-arg / factory) and publish it so self-describing tools
        // (system functions reaching CompanionRuntime statically) can reach the gateways and state.
        CompanionState state = new CompanionState();
        CompanionActionReducer reducer = new WordOverlapActionReducer();
        LlmGateway llm = llmOverride != null ? llmOverride : CompanionLlmGatewayFactory.create();
        SpeechGateway speech = new CompanionSpeechGateway();
        ExecutionGateway execution = executionOverride != null ? executionOverride : new CompanionExecutionGateway();
        SessionMemoryGateway memory = new SessionMemoryGateway();
        // Long-term consolidation: hand mid-term overflow to the LLM-backed consolidator (§3.7/§10.3).
        memory.setMidTermEvictionListener(new MidTermToLongTermConsolidator(memory, llm, speech));
        CompanionRuntime.install(llm, speech, execution, memory, reducer, state);

        DangerousActionPolicy dangerousActionPolicy = new CommandFlagDangerousActionPolicy();
        confirmationCoordinator = new ConfirmationCoordinator();
        ThoughtContext ctx = new ThoughtContext(llm, speech, execution, memory,
                new PromptComposer(), new IntelActionAccessPolicy(), new SystemFunctionProvider(), reducer, state,
                dangerousActionPolicy, confirmationCoordinator);
        dispatcher = new ThoughtDispatcher(ctx);
        dispatcher.start();
        gameEventFilter = new GameEventFilter(dispatcher);
        bargeInController = new BargeInController(dispatcher);

        // Subscribe last, so events only flow once the whole graph is live.
        GameEventBus.register(this);
        GameEventBus.register(bargeInController);
    }

    @Override
    public void stop() {
        if (dispatcher == null) {
            return; // never started (companion mode was off)
        }
        GameEventBus.unregister(this);
        GameEventBus.unregister(bargeInController);
        dispatcher.stop();
        dispatcher = null;
        gameEventFilter = null;
        bargeInController = null;
        confirmationCoordinator = null;
        CompanionRuntime.clear();
    }

    /** Reads the {@code companionModeOn} gate flag. */
    private boolean isCompanionModeOn() {
        return CompanionConfig.companionModeOn();
    }

    /** Test access to the live dispatcher (e.g. for an idle-based turn boundary in the local eval). */
    public ThoughtDispatcher dispatcher() {
        return dispatcher;
    }
}
