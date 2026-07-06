package elite.intel.companion.memory.facts;

import java.util.List;

/**
 * A pluggable source of facts for the companion, discovered by annotation ({@link RegisterMemoryFactSource}) exactly
 * like commands and queries. A source has two independent roles, and the aggregator tags each returned line with this
 * source's {@link #id()} as its provenance, so a source cannot spoof another's origin:
 * <ul>
 *   <li>{@link #factsFor} - ambient current-state facts inlined into the per-turn {@code <facts>} block;</li>
 *   <li>{@link #searchFacts} - facts relevant to an explicit {@code memory_search}, keyed off the query.</li>
 * </ul>
 * Implementations must be stateless and cheap: both run on hot paths (prompt composition / a query round).
 */
public interface MemoryFactSource {

    /** Stable provenance label rendered in the {@code <fact source="...">} attribute and used in logs (e.g. "ship"). */
    String id();

    /**
     * Ambient current-state facts for the per-turn {@code <facts>} block, most relevant first, already bounded by the
     * source. An always-on or context-gated source may ignore the query. Never {@code null}; empty means none.
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
