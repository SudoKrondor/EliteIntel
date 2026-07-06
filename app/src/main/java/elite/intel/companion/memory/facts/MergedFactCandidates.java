package elite.intel.companion.memory.facts;

import elite.intel.companion.memory.MemoryGateway;
import elite.intel.companion.prompt.Fact;
import elite.intel.companion.prompt.MemoryFactCandidates;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Single owner of the pre-turn {@code <facts>} candidate list: merges the durable memory facts
 * ({@link MemoryFactCandidates}, the store-backed core) with the facts contributed by every registered
 * {@link MemoryFactSource} plugin. Memory facts go first - they are relevance-ranked, and leading position guards
 * the answer against the model's lost-in-the-middle bias; plugin facts follow, each tagged with its source id. The
 * total is capped so the block stays lean for the small companion model, and no single plugin may crowd out the rest.
 */
public final class MergedFactCandidates {

    private static final Logger log = LogManager.getLogger(MergedFactCandidates.class);

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
        return forInput(memory, context, MemoryFactSourceRegistry.getInstance().sources());
    }

    /**
     * Testable seam: merges against an explicit source list instead of the global registry. De-duplicates by text
     * (case-insensitive) so the same fact surfaced by two sources is inlined once.
     */
    static List<Fact> forInput(MemoryGateway memory, MemoryFactContext context, List<MemoryFactSource> pluginSources) {
        List<Fact> merged = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        // Memory is the ranked core; it keeps its own internal cap well under MAX_TOTAL.
        for (Fact fact : MemoryFactCandidates.forInput(memory, context.query())) {
            addUnique(merged, seen, fact);
        }
        // Plugins contribute on top, each bounded, tagged with its own provenance.
        for (MemoryFactSource source : pluginSources) {
            if (merged.size() >= MAX_TOTAL) break;
            appendFromSource(merged, seen, source, context);
        }
        return List.copyOf(merged);
    }

    private static void appendFromSource(List<Fact> merged, Set<String> seen, MemoryFactSource source, MemoryFactContext context) {
        int taken = 0;
        for (String text : offeredFacts(source, context)) {
            if (merged.size() >= MAX_TOTAL || taken >= MAX_PER_SOURCE) return;
            if (text == null || text.isBlank()) continue;
            if (addUnique(merged, seen, new Fact(text.strip(), source.id()))) {
                taken++;
            }
        }
    }

    /**
     * The facts a source offers this turn, isolated from the turn's fate. A fact source is an optional contributor:
     * a broken one must drop only its own facts, never fail the whole commander turn.
     */
    private static List<String> offeredFacts(MemoryFactSource source, MemoryFactContext context) {
        try {
            List<String> offered = source.factsFor(context);
            // WHY: the contract is non-null, but degrade to "no facts" rather than NPE the turn on a violation.
            return offered != null ? offered : List.of();
        } catch (RuntimeException e) {
            // WHY: an optional fact source must not take down the turn; log and skip its contribution.
            log.warn("MemoryFactSource '{}' failed; skipping its facts", source.id(), e);
            return List.of();
        }
    }

    /** Adds the fact unless the block is already full or an equal text is present; returns whether it was added. */
    private static boolean addUnique(List<Fact> merged, Set<String> seen, Fact fact) {
        if (merged.size() >= MAX_TOTAL) return false;
        if (!seen.add(fact.text().toLowerCase(Locale.ROOT))) return false;
        merged.add(fact);
        return true;
    }
}
