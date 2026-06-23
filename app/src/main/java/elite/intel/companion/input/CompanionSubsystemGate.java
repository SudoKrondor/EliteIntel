package elite.intel.companion.input;

import com.google.common.eventbus.Subscribe;
import elite.intel.companion.CompanionRuntime;
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
import elite.intel.gameapi.UserInputEvent;
import elite.intel.gameapi.journal.events.BaseEvent;
import elite.intel.session.SystemSession;
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

    /** Commander voice input gate. */
    @Subscribe
    public void onUserInput(UserInputEvent event) {
        if (!isCompanionModeOn()) {
            return;
        }
        dispatcher.submitCommanderInput(event.getUserInput());
    }

    /** Game event gate (journal/status events arrive here as {@code BaseEvent}). */
    @Subscribe
    public void onGameEvent(BaseEvent event) {
        if (!isCompanionModeOn()) {
            return;
        }
        gameEventFilter.onGameEvent(event);
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
        LlmGateway llm = CompanionLlmGatewayFactory.create();
        SpeechGateway speech = new CompanionSpeechGateway();
        ExecutionGateway execution = new CompanionExecutionGateway();
        SessionMemoryGateway memory = new SessionMemoryGateway();
        // Long-term consolidation: hand mid-term overflow to the LLM-backed consolidator (§3.7/§10.3).
        memory.setMidTermEvictionListener(new MidTermToLongTermConsolidator(memory, llm, speech));
        CompanionRuntime.install(llm, speech, execution, memory, reducer, state);

        ThoughtContext ctx = new ThoughtContext(llm, speech, execution, memory,
                new PromptComposer(), new IntelActionAccessPolicy(), new SystemFunctionProvider(), reducer, state);
        dispatcher = new ThoughtDispatcher(ctx);
        dispatcher.start();
        gameEventFilter = new GameEventFilter(dispatcher);

        // Subscribe last, so events only flow once the whole graph is live.
        GameEventBus.register(this);
    }

    @Override
    public void stop() {
        if (dispatcher == null) {
            return; // never started (companion mode was off)
        }
        GameEventBus.unregister(this);
        dispatcher.stop();
        dispatcher = null;
        gameEventFilter = null;
        CompanionRuntime.clear();
    }

    /** Reads the {@code companionModeOn} gate flag. */
    private boolean isCompanionModeOn() {
        return SystemSession.getInstance().companionModeOn();
    }
}
