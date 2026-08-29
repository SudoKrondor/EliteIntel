package elite.intel.ai.brain.vega.memory.facts;

import java.util.List;

/**
 * A pluggable source of live facts for the companion, discovered by annotation ({@link RegisterMemoryFactSource})
 * exactly like commands and queries. The source decides whether its current-state facts belong in the per-turn
 * {@code <facts>} block, and the aggregator tags each returned line with the source's {@link #id()} as provenance so
 * one source cannot spoof another's origin. Implementations must be stateless and cheap because they run during
 * prompt composition. Sources report live current state only; they never replay stored conversation.
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
     * Whether this source speaks on every commander turn rather than in answer to a subject. Ambient sources are
     * gathered after the subject-relevant ones, so that when the block fills up the fact the commander actually
     * asked about is the one that survives and the standing context is what gets dropped.
     */
    default boolean isAmbient() {
        return false;
    }

    /**
     * Current-state facts for the per-turn {@code <facts>} block, most relevant first, already bounded by the source.
     * Called after this source returned {@code true} from {@link #isRelevant}; implementations may then focus on
     * live-state availability. Never {@code null}; empty means none.
     */
    List<String> factsFor(MemoryFactContext context);

}
