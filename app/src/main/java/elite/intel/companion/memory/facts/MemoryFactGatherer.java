package elite.intel.companion.memory.facts;

import elite.intel.companion.prompt.Fact;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Single owner of running the registered {@link MemoryFactSource}s for a context: it asks every source for its
 * facts, isolates a failing or contract-violating source (it drops only its own facts, never the caller's turn or
 * query), and returns them flat, each tagged with its source {@link MemoryFactSource#id()} as provenance. It applies
 * no cap and no de-duplication - each consumer applies its own policy: {@link MergedFactCandidates} caps the lean
 * pre-turn block, while the {@code memory_search} query takes the comprehensive set.
 */
public final class MemoryFactGatherer {

    private static final Logger log = LogManager.getLogger(MemoryFactGatherer.class);

    private MemoryFactGatherer() {
    }

    /** Facts from every registered source for this context, flat and tagged; failures isolated, uncapped. */
    public static List<Fact> gather(MemoryFactContext context) {
        return gather(context, MemoryFactSourceRegistry.getInstance().sources());
    }

    /** Testable seam: gathers from an explicit source list instead of the global registry. */
    static List<Fact> gather(MemoryFactContext context, List<MemoryFactSource> sources) {
        List<Fact> facts = new ArrayList<>();
        for (MemoryFactSource source : sources) {
            for (String text : offeredFacts(source, context)) {
                if (text == null || text.isBlank()) continue;
                facts.add(new Fact(text.strip(), source.id()));
            }
        }
        return List.copyOf(facts);
    }

    /**
     * The facts a source offers for this context, isolated from the caller's fate. A fact source is an optional
     * contributor: a broken one must drop only its own facts, never fail the turn or query that asked.
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
}
