package elite.intel.companion.memory.facts;

import elite.intel.ai.embed.SemanticQuery;
import elite.intel.companion.memory.MemoryGateway;
import elite.intel.companion.prompt.Fact;
import elite.intel.companion.prompt.MemoryFactCandidates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Single owner of the pre-turn {@code <facts>} candidate list: merges the durable memory facts
 * ({@link MemoryFactCandidates}, the store-backed core) with the facts gathered from the registered
 * {@link MemoryFactSource} plugins ({@link MemoryFactGatherer}). Memory facts go first - they are relevance-ranked,
 * and leading position guards the answer against the model's lost-in-the-middle bias; plugin facts follow, bounded
 * per source and in total so the block stays lean for the small companion model and no single plugin dominates.
 */
public final class MergedFactCandidates {

    /** Total facts inlined across all sources: a lean cap so the small model is not flooded (lost-in-the-middle). */
    private static final int MAX_TOTAL = 6;
    /** Per-plugin cap so one chatty source cannot crowd out memory or the other sources. */
    private static final int MAX_PER_SOURCE = 2;

    private MergedFactCandidates() {
    }

    /**
     * The clean answer facts to inline for this turn, most relevant first, at most {@value #MAX_TOTAL}: the memory
     * core followed by the registered plugin sources' contributions.
     */
    public static List<Fact> forInput(MemoryGateway memory, MemoryFactContext context) {
        return forInput(memory, context, MemoryFactGatherer.gather(context), null);
    }

    /**
     * The same per-turn fact merge, preserving a semantic query prepared during intake for durable-memory recall.
     * Each ambient source owns its deterministic relevance check; no source consumes the prepared embedding.
     */
    public static List<Fact> forInput(MemoryGateway memory, MemoryFactContext context, SemanticQuery semanticQuery) {
        return forInput(memory, context, MemoryFactGatherer.gather(context), semanticQuery);
    }

    /**
     * Testable seam: merges the memory core with an already-gathered plugin-fact list (each tagged by source id).
     * De-duplicates by text (case-insensitive) so the same fact from two producers is inlined once.
     */
    static List<Fact> forInput(MemoryGateway memory, MemoryFactContext context, List<Fact> pluginFacts) {
        return forInput(memory, context, pluginFacts, null);
    }

    /** Merges already-gathered plugin facts while passing the optional query only to ranked memory recall. */
    static List<Fact> forInput(MemoryGateway memory, MemoryFactContext context, List<Fact> pluginFacts,
                               SemanticQuery semanticQuery) {
        List<Fact> merged = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        // Memory is the ranked core; it keeps its own internal cap well under MAX_TOTAL.
        for (Fact fact : MemoryFactCandidates.forInput(memory, context.query(), semanticQuery)) {
            addUnique(merged, seen, fact);
        }
        // Plugin facts follow, bounded per source and in total.
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
