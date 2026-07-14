package elite.intel.companion.memory.facts;

import elite.intel.companion.model.ThoughtSource;
import elite.intel.companion.model.Urgency;

/**
 * The pre-turn signals handed to every {@link MemoryFactSource} when the commander prompt's {@code <facts>} block
 * is assembled. It carries only turn-scoped signals that are not globally reachable; live game/ship/system state is
 * read by a source directly from the session singletons, so it is deliberately not duplicated here.
 * <p>
 * Each ambient source decides its own relevance from {@link #query()}; after it opts in, its {@code factsFor}
 * implementation may ignore the query and only read live state. {@link #source()} and
 * {@link #urgency()} are the other turn-scoped signals available at this point. The turn kind
 * (question vs command) is intentionally absent: the model decides it mid-turn via {@code classify_turn}, after this
 * block is built, so it does not exist yet at fact-selection time.
 *
 * @param query   the commander's current input (matchInput); blank selects no ambient sources
 * @param source  the thought source assembling the prompt (COMMANDER today)
 * @param urgency whether the turn was born urgent
 */
public record MemoryFactContext(String query, ThoughtSource source, Urgency urgency) {

    /**
     * Context for a query-time source lookup (e.g. ambient selection or {@code memory_search}) where thought signals
     * are not threaded through: it is a COMMANDER turn and urgency is treated as NORMAL. Use when only the query
     * text is known.
     */
    public static MemoryFactContext forQuery(String query) {
        return new MemoryFactContext(query, ThoughtSource.COMMANDER, Urgency.NORMAL);
    }
}
