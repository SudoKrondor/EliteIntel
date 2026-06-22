package elite.intel.companion.mind;

import elite.intel.gameapi.journal.events.BaseEvent;
import elite.intel.ui.controller.ManagedService;

/**
 * The accounting/scheduling node of the consciousness. Owns the commander/event queues, at most one
 * live thought per source, and urgency-driven interrupts. It does not interpret meaning and does not
 * know a thought's internal state (see COMPANION_ARCHITECTURE.md §2.3).
 * <p>
 * Urgency is assigned here at thought birth: commander input by urgent-phrase matchers, events by
 * urgent event-type list.
 */
public final class ThoughtDispatcher implements ManagedService {

    private final ThoughtContext ctx;

    public ThoughtDispatcher(ThoughtContext ctx) {
        this.ctx = ctx;
    }

    /** Accepts a commander reply, creates a COMMANDER thought, and queues/interrupts accordingly. */
    public void submitCommanderInput(String input) {
        // TODO: Phase 2 - create COMMANDER thought, assign urgency, queue, run when slot free.
        throw new UnsupportedOperationException("TODO: Phase 2");
    }

    /** Accepts a filtered game event, creates an EVENT thought, and queues/interrupts accordingly. */
    public void submitEvent(BaseEvent event) {
        // TODO: Phase 3 - create EVENT thought, assign urgency by event type, queue, run.
        throw new UnsupportedOperationException("TODO: Phase 3");
    }

    @Override
    public void start() {
        // TODO: Phase 2 - initialize queues/worker(s).
    }

    @Override
    public void stop() {
        // TODO: Phase 2 - drain/cancel live thoughts and queues.
    }
}
