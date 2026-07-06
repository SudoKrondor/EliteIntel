package elite.intel.companion.memory.facts;

import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.Urgency;

/**
 * The pre-turn signals handed to every {@link MemoryFactSource} when the commander prompt's {@code <facts>} block
 * is assembled. It carries only turn-scoped signals that are not globally reachable; live game/ship/system state is
 * read by a source directly from the session singletons, so it is deliberately not duplicated here.
 * <p>
 * A query-driven source (like memory recall) matches on {@link #query()}; an always-on source (current system/ship
 * facts) ignores it. {@link #source()} and {@link #urgency()} are the other turn-scoped signals available at this
 * point, carried for sources that branch on them (e.g. trimming verbose facts on an urgent turn). The turn kind
 * (question vs command) is intentionally absent: the model decides it mid-turn via {@code classify_turn}, after this
 * block is built, so it does not exist yet at fact-selection time.
 *
 * @param query   the commander's current input (matchInput); may be blank for always-on sources
 * @param source  the thought source assembling the prompt (COMMANDER today)
 * @param urgency whether the turn was born urgent
 */
public record MemoryFactContext(String query, ThoughtSource source, Urgency urgency) {

    /**
     * Context for a query-time source lookup (e.g. the {@code memory_search} query) where the thought's own signals
     * are not threaded through: it is a COMMANDER turn and urgency is treated as NORMAL. Use when only the query
     * text is known.
     */
    public static MemoryFactContext forQuery(String query) {
        return new MemoryFactContext(query, ThoughtSource.COMMANDER, Urgency.NORMAL);
    }
}
