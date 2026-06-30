package elite.intel.companion.memory;

import elite.intel.ai.embed.SemanticPhraseMatcher;
import elite.intel.ai.embed.SemanticSearchProvider;
import elite.intel.companion.CompanionConfig;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemoryImportance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

/**
 * Default {@link MemoryGateway} implementation. Composes the session memory areas
 * (short-term, mid-term topic, long-term summary), owns the eviction transitions between them, and embeds and
 * de-duplicates entries on write. The recall ranking ({@code search_in_memory}) lives in {@link MemorySearch};
 * the internal stores are package-private; nothing outside this package touches them.
 * <p>
 * Session-only: nothing is persisted to disk.
 * <p>
 * Thread-safety: the public methods are {@code synchronized} because writers arrive from several threads -
 * the EVENT/NARRATION lane workers and the bounded pool of COMMANDER lane workers (several commander
 * thoughts run at once). The internal stores are plain collections, so all access is serialized here; reads
 * return snapshots ({@code List.copyOf}), so a caller iterates outside the lock safely.
 */
public final class SessionMemoryGateway implements MemoryGateway {

    private static final Logger log = LogManager.getLogger(SessionMemoryGateway.class);

    private final TokenEstimator tokenEstimator;
    private final Supplier<SemanticPhraseMatcher> matcherSource;
    private final ShortTermMemory shortTerm;
    private final MidTermTopicMemory midTerm = new MidTermTopicMemory();
    private final LongTermMemory longTerm = new LongTermMemory();

    // Hands mid-term overflow to the consolidator; no-op until wired at subsystem start. The gateway stays
    // mechanical (it never calls the LLM) - it only forwards evicted entries.
    private volatile MidTermEvictionListener evictionListener = entry -> {};

    /** Production constructor: heuristic token estimator and the process-wide shared semantic matcher. */
    public SessionMemoryGateway() {
        this(SemanticSearchProvider::matcher);
    }

    /**
     * Chooses the semantic-search source (with the heuristic token estimator). Production passes the shared
     * provider; a caller that must stay off the embedding model - e.g. the default-suite integration test -
     * passes {@code () -> null}, which keeps recall on word matching alone.
     */
    public SessionMemoryGateway(Supplier<SemanticPhraseMatcher> matcherSource) {
        this(new HeuristicTokenEstimator(), matcherSource);
    }

    /** Injectable token estimator for tests; word-only recall (no semantic matcher). */
    SessionMemoryGateway(TokenEstimator tokenEstimator) {
        this(tokenEstimator, () -> null);
    }

    /** Canonical constructor: injectable token estimator and semantic-matcher source. */
    SessionMemoryGateway(TokenEstimator tokenEstimator, Supplier<SemanticPhraseMatcher> matcherSource) {
        this.tokenEstimator = tokenEstimator;
        this.matcherSource = matcherSource;
        this.shortTerm = new ShortTermMemory(tokenEstimator);
    }

    /** Registers the consolidator that consumes mid-term overflow; defaults to a no-op until set. */
    public void setMidTermEvictionListener(MidTermEvictionListener listener) {
        this.evictionListener = listener == null ? entry -> {} : listener;
    }

    @Override
    public synchronized void write(MemoryEntry entry) {
        // Stored lower-cased: case carries no recall signal (search lower-cases anyway) and it keeps the
        // inlined timeline uniform. The meaning-vector is computed here, once, on the lower-cased text so
        // semantic recall reads it for free. New entries land in short-term first; whatever overflows the
        // count/token bounds is moved into mid-term topic memory by topic (never duplicated across both levels).
        MemoryEntry stored = entry;
        if (entry.content() != null) {
            String lower = entry.content().toLowerCase(Locale.ROOT);
            stored = new MemoryEntry(entry.timestamp(), entry.topic(), entry.source(), lower,
                    entry.importance(), embed(lower));
        }
        // Collapse a fact that is already in memory under near-identical meaning into one fresh copy, so a
        // re-stated or re-asked fact (commander fact + the companion's echo, repeated questions, repeated
        // "I didn't find it" replies) does not pile up near-duplicate entries that later crowd out recall.
        stored = mergeDuplicate(stored);
        shortTerm.add(stored);
        for (MemoryEntry evicted : shortTerm.evictOverflow()) {
            midTerm.add(evicted);
        }
        // Per-topic mid-term overflow is handed to the consolidator (long-term summary lives behind the LLM).
        for (MemoryEntry overflow : midTerm.evictOverflow()) {
            evictionListener.onEvicted(overflow);
        }
    }

    @Override
    public synchronized List<MemoryEntry> readShortTermTimeline() {
        return shortTerm.timeline();
    }

    @Override
    public synchronized List<MemoryEntry> recallTopicMemory(ConversationTopic topic, String query, int limit) {
        return midTerm.recall(topic, query, limit);
    }

    @Override
    public synchronized List<String> recallMatching(String query, int limit) {
        // Ranking and de-duplication live in MemorySearch; the gateway only supplies the current memory areas
        // and the shared matcher. A given entry lives in exactly one of short-term / mid-term, so no double-count.
        return MemorySearch.recall(query, limit, shortTerm.timeline(), midTerm.allEntries(),
                longTerm.pinnedFacts(), matcherSource);
    }

    @Override
    public synchronized List<MemoryEntry> importantWorkingSet(int maxEntries, int tokenBudget) {
        // Important mid-term entries only: short-term is already inlined whole (and searched), so re-including it
        // would duplicate the prompt. Take the most recent HIGH/MAX, capped by count and token budget, then
        // return oldest-to-newest for a stable, timeline-like ordering.
        List<MemoryEntry> important = new ArrayList<>();
        for (MemoryEntry entry : midTerm.allEntries()) {
            if (entry.importance().compareTo(MemoryImportance.HIGH) >= 0) {
                important.add(entry);
            }
        }
        important.sort(Comparator.comparing(MemoryEntry::timestamp).reversed());
        List<MemoryEntry> selected = new ArrayList<>();
        int tokens = 0;
        for (MemoryEntry entry : important) {
            if (selected.size() >= maxEntries) {
                break;
            }
            int cost = tokenEstimator.estimate(entry.content()) + CompanionMemoryLimits.SHORT_TERM_ENTRY_FRAMING_OVERHEAD_TOKENS;
            if (!selected.isEmpty() && tokens + cost > tokenBudget) {
                break; // keep at least the newest, then stop once the budget would overflow
            }
            tokens += cost;
            selected.add(entry);
        }
        Collections.reverse(selected); // oldest-to-newest, matching the timeline block
        return selected;
    }

    @Override
    public synchronized MemoryAvailabilitySnapshot indexes() {
        return new MemoryAvailabilitySnapshot(midTerm.topicsWithMemory());
    }

    @Override
    public synchronized String longTermSummary() {
        return longTerm.get();
    }

    @Override
    public synchronized void replaceLongTermSummary(String summary) {
        longTerm.replace(summary);
    }

    @Override
    public synchronized List<MemoryEntry> longTermPinnedFacts() {
        return longTerm.pinnedFacts();
    }

    @Override
    public synchronized void addLongTermPinned(MemoryEntry fact) {
        // Pinned facts are searched too, so they need a meaning-vector; attach one if the consolidator did not.
        MemoryEntry withVector = fact;
        if (fact != null && fact.content() != null && fact.embedding() == null) {
            withVector = fact.withEmbedding(embed(fact.content()));
        }
        longTerm.pin(withVector);
    }

    /**
     * Collapses every entry already in short-term/mid-term that means the same thing as {@code incoming}
     * (cosine &ge; {@link CompanionConfig#semanticDedupFloor()}) into a single surviving copy: the most
     * important wording (the newest when importance ties), stamped with the newest mention so a re-confirmed
     * fact is fresh again. The superseded copies are removed from their stores; the survivor is returned to be
     * stored as the one short-term copy. A no-op when {@code incoming} has no vector (semantic search off).
     */
    private MemoryEntry mergeDuplicate(MemoryEntry incoming) {
        if (incoming.embedding() == null) {
            return incoming;
        }
        double floor = CompanionConfig.semanticDedupFloor();
        MemoryEntry keep = incoming;
        Instant freshest = incoming.timestamp();
        boolean merged = false;
        for (MemoryEntry existing : sameByMeaning(incoming, floor)) {
            removeStored(existing);
            merged = true;
            if (existing.importance().compareTo(keep.importance()) > 0) {
                keep = existing; // a strictly more important wording wins; an equal-importance tie keeps the incoming (newest)
            }
            if (existing.timestamp().isAfter(freshest)) {
                freshest = existing.timestamp();
            }
        }
        return merged ? keep.withTimestamp(freshest) : incoming;
    }

    /** Every stored short-term/mid-term entry whose meaning matches {@code probe} (the MAX archive is left intact). */
    private List<MemoryEntry> sameByMeaning(MemoryEntry probe, double floor) {
        List<MemoryEntry> out = new ArrayList<>();
        collectSameByMeaning(out, shortTerm.timeline(), probe, floor);
        collectSameByMeaning(out, midTerm.allEntries(), probe, floor);
        return out;
    }

    private static void collectSameByMeaning(List<MemoryEntry> out, List<MemoryEntry> entries,
                                             MemoryEntry probe, double floor) {
        for (MemoryEntry entry : entries) {
            if (MemorySearch.sameMeaning(probe, entry, floor)) {
                out.add(entry);
            }
        }
    }

    /** Removes a superseded entry from whichever store holds it (short-term first, then mid-term). */
    private void removeStored(MemoryEntry entry) {
        if (!shortTerm.remove(entry)) {
            midTerm.remove(entry);
        }
    }

    /**
     * Computes the meaning-vector for an entry's text once, via the shared semantic matcher, or {@code null}
     * when semantic search is unavailable.
     */
    private float[] embed(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        SemanticPhraseMatcher matcher = matcherSource.get();
        if (matcher == null) {
            return null;
        }
        try {
            return matcher.embedQuery(text);
        } catch (RuntimeException e) {
            // WHY: a transient embed failure must not block storing the memory; the entry is kept without a
            // vector (word-only recall). The matcher exists only after a successful model load, so a throw here
            // is unexpected - log it rather than hide it.
            log.warn("Embedding a memory entry failed; storing it without a meaning-vector", e);
            return null;
        }
    }
}
