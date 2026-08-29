package elite.intel.ai.brain.vega.memory.facts;

import elite.intel.ai.brain.vega.diag.CompanionDiagnostics;
import elite.intel.ai.brain.vega.prompt.Fact;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Single owner of running the registered {@link MemoryFactSource}s for a context: it asks every source for its facts,
 * isolates a failing or contract-violating source (it drops only its own facts, never the caller's turn), and
 * returns them flat, each tagged with its source {@link MemoryFactSource#id()} as provenance. It applies no cap and no
 * de-duplication - each consumer applies its own policy.
 * Registered sources contribute only live current-state facts to the per-turn block
 * ({@link MergedFactCandidates}).
 */
public final class MemoryFactGatherer {

    private static final Logger log = LogManager.getLogger(MemoryFactGatherer.class);

    private MemoryFactGatherer() {
    }

    /** Relevant current-state facts from registered sources, each source applying its own relevance policy. */
    public static List<Fact> gather(MemoryFactContext context) {
        return gatherRelevant(context, MemoryFactSourceRegistry.getInstance().sources());
    }

    /** Testable collection seam for sources already selected by the caller. */
    static List<Fact> gather(MemoryFactContext context, List<MemoryFactSource> sources) {
        return collect(sources, context);
    }

    /** Testable seam that asks each explicit source to decide its own relevance. */
    static List<Fact> gatherRelevant(MemoryFactContext context, List<MemoryFactSource> sources) {
        if (sources == null || sources.isEmpty()) {
            return List.of();
        }
        // Subject-relevant sources first, standing context after: the block's total cap is small, and a fact that
        // answers the commander's actual subject must not be crowded out by one that speaks every turn.
        List<MemoryFactSource> relevant = sources.stream()
                .filter(source -> isRelevant(source, context))
                .sorted(Comparator.comparing(MemoryFactSource::isAmbient))
                .toList();
        CompanionDiagnostics.debugAmbient("facts", "ambient relevance -> "
                + relevant.stream().map(MemoryFactSource::id).toList());
        return collect(relevant, context);
    }

    private static List<Fact> collect(List<MemoryFactSource> sources, MemoryFactContext context) {
        List<Fact> collected = new ArrayList<>();
        for (MemoryFactSource source : sources) {
            for (String text : offeredFacts(source, context)) {
                if (text == null || text.isBlank()) {
                    continue;
                }
                collected.add(new Fact(text.strip(), source.id()));
            }
        }
        return List.copyOf(collected);
    }

    /**
     * The facts a source offers for the current context, isolated from the caller's fate. A fact source is an optional
     * contributor: a broken one must drop only its own facts, never fail the turn that asked.
     */
    private static List<String> offeredFacts(MemoryFactSource source, MemoryFactContext context) {
        try {
            List<String> offered = source.factsFor(context);
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
