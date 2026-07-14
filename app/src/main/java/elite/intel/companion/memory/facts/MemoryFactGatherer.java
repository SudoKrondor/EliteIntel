package elite.intel.companion.memory.facts;

import elite.intel.companion.diag.CompanionDiagnostics;
import elite.intel.companion.prompt.Fact;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Single owner of running the registered {@link MemoryFactSource}s for a context: it asks every source for its facts,
 * isolates a failing or contract-violating source (it drops only its own facts, never the caller's turn or query), and
 * returns them flat, each tagged with its source {@link MemoryFactSource#id()} as provenance. It applies no cap and no
 * de-duplication - each consumer applies its own policy.
 * <p>
 * Two entry points select which role the sources play: {@link #gather} relevance-gates current-state facts for the
 * per-turn block ({@link MergedFactCandidates}), and {@link #gatherForSearch} pulls the query-relevant facts for the
 * {@code memory_search} query.
 */
public final class MemoryFactGatherer {

    private static final Logger log = LogManager.getLogger(MemoryFactGatherer.class);

    private MemoryFactGatherer() {
    }

    /** Relevant current-state facts from registered sources, each source applying its own relevance policy. */
    public static List<Fact> gather(MemoryFactContext context) {
        return gatherRelevant(context, MemoryFactSourceRegistry.getInstance().sources());
    }

    /** Query-relevant facts from every registered source, for {@code memory_search}; flat, tagged, isolated. */
    public static List<Fact> gatherForSearch(MemoryFactContext context) {
        return gatherForSearch(context, MemoryFactSourceRegistry.getInstance().sources());
    }

    /** Testable collection seam for sources already selected by the caller. */
    static List<Fact> gather(MemoryFactContext context, List<MemoryFactSource> sources) {
        return collect(sources, source -> source.factsFor(context));
    }

    /** Testable seam that asks each explicit source to decide its own relevance. */
    static List<Fact> gatherRelevant(MemoryFactContext context, List<MemoryFactSource> sources) {
        if (sources == null || sources.isEmpty()) {
            return List.of();
        }
        List<MemoryFactSource> relevant = sources.stream()
                .filter(source -> isRelevant(source, context))
                .toList();
        CompanionDiagnostics.debugAmbient("facts", "ambient relevance -> "
                + relevant.stream().map(MemoryFactSource::id).toList());
        return collect(relevant, source -> source.factsFor(context));
    }

    /** Testable seam for the search role: gathers from an explicit source list instead of the global registry. */
    static List<Fact> gatherForSearch(MemoryFactContext context, List<MemoryFactSource> sources) {
        return collect(sources, source -> source.searchFacts(context));
    }

    private static List<Fact> collect(List<MemoryFactSource> sources, Function<MemoryFactSource, List<String>> facts) {
        List<Fact> collected = new ArrayList<>();
        for (MemoryFactSource source : sources) {
            for (String text : offeredFacts(source, facts)) {
                if (text == null || text.isBlank()) {
                    continue;
                }
                collected.add(new Fact(text.strip(), source.id()));
            }
        }
        return List.copyOf(collected);
    }

    /**
     * The facts a source offers for the selected role, isolated from the caller's fate. A fact source is an optional
     * contributor: a broken one must drop only its own facts, never fail the turn or query that asked.
     */
    private static List<String> offeredFacts(MemoryFactSource source, Function<MemoryFactSource, List<String>> facts) {
        try {
            List<String> offered = facts.apply(source);
            // WHY: the contract is non-null, but degrade to "no facts" rather than NPE the caller on a violation.
            return offered != null ? offered : List.of();
        } catch (RuntimeException e) {
            // WHY: an optional fact source must not take down the caller; log and skip its contribution.
            log.warn("MemoryFactSource '{}' failed; skipping its facts", source.id(), e);
            return List.of();
        }
    }

    /** A broken optional source drops only itself during relevance selection. */
    private static boolean isRelevant(MemoryFactSource source, MemoryFactContext context) {
        try {
            return source.isRelevant(context);
        } catch (RuntimeException e) {
            log.warn("MemoryFactSource '{}' relevance check failed; skipping its facts", source.id(), e);
            return false;
        }
    }
}
