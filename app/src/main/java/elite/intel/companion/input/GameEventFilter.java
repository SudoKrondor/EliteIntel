package elite.intel.companion.input;

import elite.intel.companion.mind.ThoughtDispatcher;
import elite.intel.gameapi.journal.events.BaseEvent;

/**
 * Mechanical noise filter for game events. It only decides whether a game event is worth attention
 * and forwards accepted events to the {@code ThoughtDispatcher}. It does not write memory, determine
 * urgency, call the LLM, run tools, or change topic (see COMPANION_ARCHITECTURE.md §2.2).
 */
public final class GameEventFilter {

    private final ThoughtDispatcher dispatcher;

    public GameEventFilter(ThoughtDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    /** Receives a game event; if it survives the noise filter, forwards it to the dispatcher. */
    public void onGameEvent(BaseEvent event) {
        // TODO: Phase 3 - noise filtering of the journal/status/game event stream.
        throw new UnsupportedOperationException("TODO: Phase 3");
    }

    /** Whether the event is worthy of attention (noise rejection only). */
    public boolean accept(BaseEvent event) {
        // TODO: Phase 3 - decide which BaseEvent types pass.
        throw new UnsupportedOperationException("TODO: Phase 3");
    }
}
