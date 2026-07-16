package elite.intel.companion.memory.facts;

import java.util.List;

/**
 * A pluggable source of facts for the companion, discovered by annotation ({@link RegisterMemoryFactSource}) exactly
 * like commands and queries. A source has two independent roles, and the aggregator tags each returned line with this
 * source's {@link #id()} as its provenance, so a source cannot spoof another's origin:
 * <ul>
 *   <li>{@link #isRelevant} / {@link #factsFor} - the source itself decides whether its current-state facts belong in
 *       the per-turn {@code <facts>} block, then produces them;</li>
 *   <li>{@link #searchFacts} - facts relevant to an explicit {@code memory_search}, keyed off the query.</li>
 * </ul>
 * Implementations must be stateless and cheap: both run on hot paths (prompt composition / a query round).
 */
public interface MemoryFactSource {

    /** Lowercase provenance id ({@code [a-z][a-z0-9_]*}) used in the fact attribute and logs. */
    String id();

    /**
     * Whether this source's live facts are relevant to the current turn. Discovery only registers the source; this
     * method owns its subject and relevance policy. The safe default is opt-out for search-only sources.
     */
    default boolean isRelevant(MemoryFactContext context) {
        return false;
    }

    /**
     * Current-state facts for the per-turn {@code <facts>} block, most relevant first, already bounded by the source.
     * Called after this source returned {@code true} from {@link #isRelevant}; implementations may then focus on
     * live-state availability. Never {@code null}; empty means none.
     */
    List<String> factsFor(MemoryFactContext context);

    /**
     * Facts relevant to an explicit {@code memory_search}, given the search query in {@code context}. Defaults to none:
     * an ambient/state source (current system, current body) has nothing to recall <em>by query</em>, so it opts out
     * here until a source implements query relevance. Never {@code null}; empty means none.
     */
    default List<String> searchFacts(MemoryFactContext context) {
        return List.of();
    }
}
