package elite.intel.companion.input;

import elite.intel.companion.mind.ThoughtDispatcher;
import elite.intel.gameapi.journal.events.BaseEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Mechanical noise filter for game events. It only decides whether a game event is worth attention
 * and forwards accepted events to the {@code ThoughtDispatcher}. It does not write memory, determine
 * urgency, call the LLM, run tools, or change topic (see COMPANION_ARCHITECTURE.md §2.2).
 * <p>
 * Two mechanical gates: an allow-list by event type ({@link EventTopicMap}) and a per-type cooldown.
 * Each accepted EVENT spawns an LLM thought, so the cooldown collapses bursts of the same high-frequency
 * type (e.g. {@code ShipTargeted}, {@code Cargo}) - the first occurrence passes immediately, repeats
 * within {@link #COOLDOWN_MILLIS} are dropped. Per-type cooldown tuning is a later refinement.
 */
public final class GameEventFilter {

    /** Minimum spacing between forwarded events of the same type; a coarse first-cut, single value for all types. */
    private static final long COOLDOWN_MILLIS = 5000;

    private final ThoughtDispatcher dispatcher;
    private final LongSupplier clock;
    private final Map<String, Long> lastForwardedAt = new ConcurrentHashMap<>();

    public GameEventFilter(ThoughtDispatcher dispatcher) {
        this(dispatcher, System::currentTimeMillis);
    }

    /** Test seam: inject the time source so cooldown behaviour is deterministic. */
    GameEventFilter(ThoughtDispatcher dispatcher, LongSupplier clock) {
        this.dispatcher = dispatcher;
        this.clock = clock;
    }

    /** Receives a game event; if it passes the allow-list and the per-type cooldown, forwards it. */
    public void onGameEvent(BaseEvent event) {
        if (accept(event) && passesCooldown(event)) {
            dispatcher.submitEvent(event);
        }
    }

    /**
     * Whether the event type is worthy of attention. Mechanical allow-list: only event types in the
     * companion's gameplay taxonomy ({@link EventTopicMap}) pass. This deliberately rejects the
     * non-gameplay {@code BaseEvent}s that share the bus - notably {@code UserInput} (the commander's
     * voice line, already handled by the commander path), {@code SaveSession} and {@code ClearSessionCache}.
     * <p>
     * The rest is already filtered upstream: {@code EventRegistry} drops unregistered event types and
     * {@code JournalParser} drops replay/expired/stale events before publishing, and the high-frequency
     * status stream (PlayerMoved/InGlide/...) is not a {@code BaseEvent} and never reaches here.
     */
    public boolean accept(BaseEvent event) {
        return event != null && EventTopicMap.isMapped(event.getEventType());
    }

    /**
     * Per-type cooldown (side-effecting): the first event of a type passes and records its time; another
     * event of the same type within {@link #COOLDOWN_MILLIS} is dropped. Event delivery is effectively
     * serial (journal events arrive on the parser thread), so the read-then-record is safe in practice.
     */
    private boolean passesCooldown(BaseEvent event) {
        long now = clock.getAsLong();
        Long previous = lastForwardedAt.get(event.getEventType());
        if (previous != null && now - previous < COOLDOWN_MILLIS) {
            return false;
        }
        lastForwardedAt.put(event.getEventType(), now);
        return true;
    }
}
