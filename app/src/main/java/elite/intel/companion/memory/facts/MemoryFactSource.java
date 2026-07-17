package elite.intel.companion.memory.facts;

import java.util.List;

/**
 * A pluggable source of live facts for the companion, discovered by annotation ({@link RegisterMemoryFactSource})
 * exactly like commands and queries. The source decides whether its current-state facts belong in the per-turn
 * {@code <facts>} block, and the aggregator tags each returned line with the source's {@link #id()} as provenance so
 * one source cannot spoof another's origin. Implementations must be stateless and cheap because they run during
 * prompt composition. Durable-memory recall is not part of this contract; it belongs to {@code memory_search}.
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

}
