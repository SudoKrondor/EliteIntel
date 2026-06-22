package elite.intel.companion.input;

import com.google.common.eventbus.Subscribe;
import elite.intel.companion.mind.ThoughtDispatcher;
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
 * service is active at a time.
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
        // TODO: Phase 2 - build the dependency graph (gateways, memory, dispatcher, filter) and
        //       register this service on GameEventBus.
        throw new UnsupportedOperationException("TODO: Phase 2");
    }

    @Override
    public void stop() {
        // TODO: Phase 2 - unregister from GameEventBus and tear down the graph.
        throw new UnsupportedOperationException("TODO: Phase 2");
    }

    /** Reads the {@code companionModeOn} gate flag. */
    private boolean isCompanionModeOn() {
        // TODO: Phase 2 - read SystemSession.companionModeOn().
        return false;
    }
}
