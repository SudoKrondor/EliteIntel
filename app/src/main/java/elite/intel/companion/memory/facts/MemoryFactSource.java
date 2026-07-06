package elite.intel.companion.memory.facts;

import java.util.List;

/**
 * A pluggable source of pre-turn answer facts for the commander prompt's {@code <facts>} block, discovered by
 * annotation ({@link RegisterMemoryFactSource}) exactly like commands and queries. Given the {@link MemoryFactContext}
 * for the turn, it returns the facts it deems relevant as plain text lines; the aggregator ({@link FactCandidates})
 * tags each with this source's {@link #id()} as its provenance, so a source cannot spoof another's origin.
 * <p>
 * Implementations must be stateless and cheap: {@link #factsFor} runs on the prompt-composition path of every
 * commander turn. Return an empty list to contribute nothing this turn.
 */
public interface MemoryFactSource {

    /** Stable provenance label rendered in the {@code <fact source="...">} attribute and used in logs (e.g. "ship"). */
    String id();

    /**
     * The facts this source offers for the given turn, most relevant first, already bounded by the source. Plain
     * text lines; provenance is assigned by the aggregator from {@link #id()}. Never {@code null}; empty means none.
     */
    List<String> factsFor(MemoryFactContext context);
}
