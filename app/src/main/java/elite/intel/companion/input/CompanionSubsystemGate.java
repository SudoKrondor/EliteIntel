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
import elite.intel.companion.memory.OversizedMemoryCompressor;
import elite.intel.companion.memory.SessionMemoryGateway;
import elite.intel.companion.mind.CompanionState;
import elite.intel.companion.mind.DispatcherCompanionNarrator;
import elite.intel.companion.mind.ThoughtDependencies;
import elite.intel.companion.mind.ThoughtDispatcher;
import elite.intel.companion.prompt.CompanionActionReducer;
import elite.intel.companion.prompt.IntelActionAccessPolicy;
import elite.intel.companion.prompt.PromptComposer;
import elite.intel.companion.prompt.SemanticActionReducer;
import elite.intel.companion.speech.CompanionSpeechGateway;
import elite.intel.companion.speech.SpeechGateway;
import elite.intel.companion.tools.SystemFunctionProvider;
import elite.intel.eventbus.GameEventBus;
import elite.intel.gameapi.NormalizedUserInputEvent;
import elite.intel.gameapi.UserInputEvent;
import elite.intel.ui.controller.ManagedService;

/**
 * The single gate seam between existing input and the companion subsystem, and the owner of the
 * subsystem lifecycle. Bootstraps the whole companion graph (gateways, memory, dispatcher, narrator) and,
 * while {@code companionModeOn}, subscribes to the commander voice ({@code UserInputEvent}) and routes it into
 * the consciousness. Raw game events are <b>not</b> subscribed here: they never reach the companion directly.
 * A gameplay subscriber decides what, if anything, the companion does with an event - it calls
 * {@code CompanionRuntime.narrator()} to voice a reaction, or writes durable knowledge - so events participate
 * in the companion only through their subscribers.
 * <p>
 * Lifecycle is managed by {@code AppController}; only one of the old BRAIN service or this companion
 * service is active at a time. A live {@code dispatcher} doubles as the "started" sentinel.
 */
public final class CompanionSubsystemGate implements ManagedService {

    private ThoughtDispatcher dispatcher;
    private ConfirmationCoordinator confirmationCoordinator;
    private BargeInController bargeInController;

    private final LlmGateway llmOverride;
    private final ExecutionGateway executionOverride;
    private final SpeechGateway speechOverride;

    public CompanionSubsystemGate() {
        this(null, null);
    }

    /** Test seam: inject a recording execution gateway and/or a tracing LLM gateway for the local eval. */
    public CompanionSubsystemGate(LlmGateway llmOverride, ExecutionGateway executionOverride) {
        this(llmOverride, executionOverride, null);
    }

    /**
     * As above, plus a speech-gateway override. Diagnostics mode injects a speech gateway that delegates to the
     * real TTS path, so the companion voice stays audible and the chat panel shows replies exactly as in
     * production; only the microphone (EARS) is stubbed.
     */
    public CompanionSubsystemGate(LlmGateway llmOverride, ExecutionGateway executionOverride, SpeechGateway speechOverride) {
        this.llmOverride = llmOverride;
        this.executionOverride = executionOverride;
        this.speechOverride = speechOverride;
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
    public synchronized void start() {
        if (dispatcher != null) {
            return;
        }
        if (!isCompanionModeOn()) {
            return; // companion mode off: the legacy command mode serves input instead
        }
        // Assemble the companion graph (all no-arg / factory) and publish it so self-describing tools
        // (system functions reaching CompanionRuntime statically) can reach the gateways and state.
        CompanionState state = new CompanionState();
        CompanionActionReducer reducer = new SemanticActionReducer();
        LlmGateway llm = llmOverride != null ? llmOverride : CompanionLlmGatewayFactory.create();
        SpeechGateway speech = speechOverride != null ? speechOverride : new CompanionSpeechGateway();
        ExecutionGateway execution = executionOverride != null ? executionOverride : new CompanionExecutionGateway();
        SessionMemoryGateway memory = new SessionMemoryGateway();
        // Long-term consolidation: hand mid-term overflow to the LLM-backed consolidator (§3.7/§10.3).
        memory.setMidTermEvictionListener(new MidTermToLongTermConsolidator(memory, llm, speech));
        // Over-long writes are gist-compressed off the write path on a dedicated executor (§1.10.52b), so they
        // never bloat the prompt and never block a write or a narration lane.
        memory.setOversizedMemoryListener(new OversizedMemoryCompressor(memory, llm));
        CompanionRuntime.install(llm, speech, execution, memory, reducer, state);

        DangerousActionPolicy dangerousActionPolicy = new CommandFlagDangerousActionPolicy();
        confirmationCoordinator = new ConfirmationCoordinator();
        ThoughtDependencies dependencies = new ThoughtDependencies(llm, speech, execution, memory,
                new PromptComposer(), new IntelActionAccessPolicy(), new SystemFunctionProvider(), reducer, state,
                dangerousActionPolicy, confirmationCoordinator);
        dispatcher = new ThoughtDispatcher(dependencies);
        dispatcher.start();
        // The single door gameplay subscribers use to voice reactions (filler/narrate/announce), wrapping the
        // dispatcher and speech gateway; published statically so a subscriber reaches it via CompanionRuntime.
        CompanionRuntime.installNarrator(new DispatcherCompanionNarrator(dispatcher, speech));
        bargeInController = new BargeInController(dispatcher);

        // Subscribe last, so events only flow once the whole graph is live.
        GameEventBus.register(this);
        GameEventBus.register(bargeInController);
    }

    @Override
    public synchronized void stop() {
        if (dispatcher == null) {
            return; // never started (companion mode was off)
        }
        GameEventBus.unregister(this);
        GameEventBus.unregister(bargeInController);
        dispatcher.stop();
        dispatcher = null;
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
