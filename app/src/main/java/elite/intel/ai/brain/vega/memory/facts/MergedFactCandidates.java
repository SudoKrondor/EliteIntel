package elite.intel.ai.brain.vega.memory.facts;

import elite.intel.ai.brain.vega.prompt.Fact;

import java.util.*;

/**
 * Single owner of the live pre-turn {@code <facts>} candidate list gathered from registered
 * {@link MemoryFactSource} plugins. Facts are bounded per source and in total so the block stays lean for the small
 * companion model and no single plugin dominates. Stored conversation is deliberately excluded: the block carries
 * live game state only.
 */
public final class MergedFactCandidates {

    /** Total facts inlined across all sources: a lean cap so the small model is not flooded (lost-in-the-middle). */
    private static final int MAX_TOTAL = 6;
    /** Per-plugin cap so one chatty source cannot crowd out the other live sources. */
    private static final int MAX_PER_SOURCE = 2;

    private MergedFactCandidates() {
    }

    /**
     * The live facts to append to this turn's system prompt, at most {@value #MAX_TOTAL}, in registry order.
     */
    public static List<Fact> forInput(MemoryFactContext context) {
        return merge(MemoryFactGatherer.gather(context));
    }

    /**
     * Testable seam: bounds and de-duplicates an already-gathered source-fact list by text (case-insensitive).
     */
    static List<Fact> merge(List<Fact> pluginFacts) {
        List<Fact> merged = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Map<String, Integer> perSource = new HashMap<>();
        for (Fact fact : pluginFacts) {
            if (merged.size() >= MAX_TOTAL) break;
            if (perSource.getOrDefault(fact.source(), 0) >= MAX_PER_SOURCE) continue;
            if (addUnique(merged, seen, fact)) {
                perSource.merge(fact.source(), 1, Integer::sum);
            }
        }
        return List.copyOf(merged);
    }

    /** Adds the fact unless the block is already full or an equal text is present; returns whether it was added. */
    private static boolean addUnique(List<Fact> merged, Set<String> seen, Fact fact) {
        if (merged.size() >= MAX_TOTAL) return false;
        if (!seen.add(fact.text().toLowerCase(Locale.ROOT))) return false;
        merged.add(fact);
        return true;
    }
}
