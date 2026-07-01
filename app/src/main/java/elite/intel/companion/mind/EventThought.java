package elite.intel.companion.mind;

import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.Urgency;
import elite.intel.companion.model.memory.MemoryImportance;

/**
 * A thought born from a filtered game event. It is purely a knowledge channel: it records the event to
 * memory and ends, never engaging the LLM, never speaking, never running tools (spontaneous event speech now
 * belongs solely to {@link NarrationThought} via the curated subscriber layer - see
 * COMPANION_CURATED_NARRATION_PROPOSAL.md §2.1/§2.2). Its memory tag is fixed at birth from the static
 * event-type map; it never moves the global conversation topic.
 * <p>
 * An event is remembered only when it provides a readable {@code memorySummary()} (carried here as the
 * {@code currentInput}): that line is the event's lived-experience record. An event with no summary writes
 * nothing - this is the opt-in that keeps high-frequency telemetry out of the timeline, in place of the older
 * importance gate. {@code LOW} events never reach here - the {@code GameEventFilter} drops them upstream.
 */
public final class EventThought extends Thought {

    private final ConversationTopic eventTopic;

    EventThought(Urgency urgency, String summary, ConversationTopic eventTopic, ThoughtContext ctx) {
        super(ThoughtSource.EVENT, urgency, summary, ctx);
        this.eventTopic = eventTopic;
    }

    /**
     * Memory-only: records the event's readable summary under its static topic, or does nothing when the event
     * provides no summary. The LLM is never engaged and nothing is spoken.
     */
    @Override
    public void run() {
        if (currentInput != null && !currentInput.isBlank()) {
            recordCurrentInput();
        }
    }

    @Override
    protected ConversationTopic memoryTopic() {
        return eventTopic;
    }

    /** Events carry ordinary importance; only the commander rates a turn (classify_turn). */
    @Override
    protected MemoryImportance memoryImportance() {
        return MemoryImportance.NORMAL;
    }
}
