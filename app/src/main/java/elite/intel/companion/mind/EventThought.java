package elite.intel.companion.mind;

import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.Urgency;
import elite.intel.companion.model.memory.MemoryImportance;
import elite.intel.gameapi.journal.events.BaseEvent;

/**
 * A thought born from a filtered game event. It is purely a knowledge channel: it records the event to
 * memory and ends, never engaging the LLM, never speaking, never running tools (spontaneous event speech now
 * belongs solely to {@link NarrationThought} via the curated subscriber layer - see
 * COMPANION_CURATED_NARRATION_PROPOSAL.md §2.1/§2.2). Its memory tag is fixed at birth from the static
 * event-type map; it never moves the global conversation topic.
 * <p>
 * Only {@code HIGH}-importance events are retained: they are few and worth remembering. {@code NORMAL} events
 * are dropped (not recorded) so high-frequency telemetry does not clutter the timeline; {@code LOW} never
 * reaches here - the {@code GameEventFilter} drops it upstream.
 */
public final class EventThought extends Thought {

    private final ConversationTopic eventTopic;
    private final BaseEvent.Importance importance;

    EventThought(Urgency urgency, String summary, ConversationTopic eventTopic,
                 BaseEvent.Importance importance, ThoughtContext ctx) {
        super(ThoughtSource.EVENT, urgency, summary, ctx);
        this.eventTopic = eventTopic;
        this.importance = importance;
    }

    /**
     * Memory-only: a {@code HIGH} event is recorded under its static topic; a {@code NORMAL} event is dropped.
     * The LLM is never engaged and nothing is spoken.
     */
    @Override
    public void run() {
        if (importance == BaseEvent.Importance.HIGH) {
            recordCurrentInput();
        }
    }

    @Override
    protected ConversationTopic memoryTopic() {
        return eventTopic;
    }

    /** Events carry ordinary importance; only the commander rates a turn (set_importance). */
    @Override
    protected MemoryImportance memoryImportance() {
        return MemoryImportance.NORMAL;
    }
}
