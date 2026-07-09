package elite.intel.companion.memory;

import elite.intel.ai.embed.SemanticPhraseMatcher;
import elite.intel.ai.embed.SemanticSearchProvider;
import elite.intel.companion.CompanionConfig;
import elite.intel.companion.diag.CompanionDiagnostics;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemoryImportance;
import elite.intel.companion.model.memory.MemorySource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

/**
 * Default {@link MemoryGateway} implementation. Composes the session memory areas
 * (short-term, mid-term topic, long-term summary), owns the eviction transitions between them, and embeds and
 * de-duplicates entries on write. The recall ranking ({@code memory_search}) lives in {@link MemorySearch};
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

    private final Supplier<SemanticPhraseMatcher> matcherSource;
    private final ShortTermMemory shortTerm;
    private final MidTermTopicMemory midTerm = new MidTermTopicMemory();
    private final LongTermMemory longTerm = new LongTermMemory();

    // Hands mid-term overflow to the consolidator; no-op until wired at subsystem start. The gateway stays
    // mechanical (it never calls the LLM) - it only forwards evicted entries.
    private volatile MidTermEvictionListener evictionListener = entry -> {};
    // Hands an over-long write off for silent compression; no-op until wired at subsystem start (then the
    // entry is simply dropped). The gateway never calls the LLM itself - the listener owns that.
    private volatile OversizedMemoryListener oversizedListener = entry -> {};

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
        this.matcherSource = matcherSource;
        this.shortTerm = new ShortTermMemory(tokenEstimator);
    }

    /** Registers the consolidator that consumes mid-term overflow; defaults to a no-op until set. */
    public void setMidTermEvictionListener(MidTermEvictionListener listener) {
        this.evictionListener = listener == null ? entry -> {} : listener;
    }

    /** Registers the handler that compresses an over-long write; defaults to a no-op (the entry is dropped). */
    public void setOversizedMemoryListener(OversizedMemoryListener listener) {
        this.oversizedListener = listener == null ? entry -> {} : listener;
    }

    @Override
    public synchronized void write(MemoryEntry entry) {
        // Too long to store as-is (it would bloat the prompt timeline): hand it off for silent LLM compression,
        // which re-writes a short gist here. Checked before embedding/dedup so a doomed long entry costs nothing.
        if (entry.content() != null && entry.content().length() > CompanionConfig.memoryEntryMaxChars()) {
            oversizedListener.onOversized(entry);
            return;
        }
        // Stored lower-cased with its meaning-vector attached once (see prepareForStore). New entries land in
        // short-term first; whatever overflows the count/token bounds is moved into mid-term topic memory by
        // topic (never duplicated across both levels).
        MemoryEntry stored = prepareForStore(entry);
        // Collapse a fact that is already in memory under near-identical meaning into one fresh copy, so a
        // re-stated or re-asked fact (commander fact + the companion's echo, repeated questions, repeated
        // "I didn't find it" replies) does not pile up near-duplicate entries that later crowd out recall.
        stored = mergeDuplicate(stored);
        shortTerm.add(stored);
        for (MemoryEntry evicted : shortTerm.evictOverflow()) {
            // LOW entries (idle banter and the companion's own speech) exist only for hot-timeline continuity;
            // they are dropped when they age out of short-term, never promoted to mid-term.
            if (evicted.importance() != MemoryImportance.LOW) {
                // Collapse against mid-term duplicates at the hand-off: the hot window is verbatim, so two
                // near-identical copies can ride it together - merging here keeps mid-term (and thus the
                // <facts> candidates) free of duplicates instead of waiting for a future matching write.
                midTerm.add(mergeDuplicate(evicted));
            }
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
    public List<String> recallMatching(String query, int limit) {
        // Diagnostic emitted outside the lock: UiBus is synchronous, so a subscriber runs on this thread and
        // must not execute while the memory monitor is held (a future subscriber touching memory would deadlock).
        List<String> hits = recallMatchingLocked(query, limit);
        CompanionDiagnostics.debugAmbient("memory-search",
                "\"" + CompanionDiagnostics.truncate(query) + "\" -> " + hits.size() + " hit(s)");
        return hits;
    }

    private synchronized List<String> recallMatchingLocked(String query, int limit) {
        // Ranking and de-duplication live in MemorySearch; the gateway only supplies the current memory areas
        // and the shared matcher. A given entry lives in exactly one of short-term / mid-term, so no double-count.
        // The long-term summary is no longer inlined into every prompt - it is reached here, as a searchable entry.
        return MemorySearch.recall(query, limit, shortTerm.timeline(), midTerm.allEntries(),
                longTermSummaryAsSearchable(), longTerm.pinnedFacts(), matcherSource);
    }

    @Override
    public List<MemoryEntry> recallCandidates(String query, int limit) {
        // Diagnostic emitted outside the lock, for the same reason as recallMatching.
        List<MemoryEntry> hits = recallCandidatesLocked(query, limit);
        // The query here is the turn's input, already echoed by the intake line; log only the outcome, spelled out
        // so it reads on its own: how many remembered facts were pulled in to ground this turn's answer. Grouped
        // under the "memory" stage with the record lines. The facts themselves appear as the compose "facts:" lines.
        CompanionDiagnostics.debugAmbient("memory", "recalled " + hits.size() + " fact candidate(s) for grounding");
        return hits;
    }

    private synchronized List<MemoryEntry> recallCandidatesLocked(String query, int limit) {
        // Same ranking/sources as recallMatching, but returns entries (with source/importance) for the
        // pre-turn candidate filter; a given entry lives in exactly one area, so no double-count.
        return MemorySearch.recallEntries(query, limit, shortTerm.timeline(), midTerm.allEntries(),
                longTermSummaryAsSearchable(), longTerm.pinnedFacts(), matcherSource);
    }

    /**
     * The session long-term summary wrapped as a single searchable entry, or empty when nothing has been
     * consolidated yet. It carries no meaning-vector (it is replaced as a plain string), so it recalls by
     * word-overlap; an old timestamp keeps it from winning recency ties against specific recent facts.
     */
    private List<MemoryEntry> longTermSummaryAsSearchable() {
        String summary = longTerm.get();
        if (summary == null || summary.isBlank()) {
            return List.of();
        }
        return List.of(new MemoryEntry(Instant.EPOCH, ConversationTopic.SYSTEM, MemorySource.SYSTEM, summary.strip()));
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
    public synchronized MemorySnapshot snapshot() {
        // Regroup the flat mid-term view by topic for the dump; each entry already carries its topic, and an
        // EnumMap keeps the natural topic order. Everything handed to the snapshot is an unmodifiable copy detached
        // from the live stores (short-term/pinned are already List.copyOf), so the snapshot is truly immutable.
        Map<ConversationTopic, List<MemoryEntry>> byTopic = new EnumMap<>(ConversationTopic.class);
        for (MemoryEntry entry : midTerm.allEntries()) {
            byTopic.computeIfAbsent(entry.topic(), t -> new ArrayList<>()).add(entry);
        }
        byTopic.replaceAll((topic, entries) -> List.copyOf(entries));
        return new MemorySnapshot(shortTerm.timeline(), Collections.unmodifiableMap(byTopic),
                longTerm.get(), longTerm.pinnedFacts());
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
     * Collapses every entry already in <em>mid-term</em> that means the same thing as {@code incoming}
     * (cosine &ge; {@link CompanionConfig#semanticDedupFloor()}) into a single surviving copy: the most
     * important wording (the newest when importance ties), stamped with the newest mention so a re-confirmed
     * fact is fresh again. The superseded copies are removed from their store; the survivor is returned to be
     * stored by the caller - as the one short-term copy on a write, or as the one mid-term copy at the
     * eviction hand-off. Short-term itself is never scanned - the hot window is kept verbatim
     * (see {@link ShortTermMemory#add}). A no-op when {@code incoming} has no vector (semantic search off).
     */
    private MemoryEntry mergeDuplicate(MemoryEntry incoming) {
        // An event is a discrete occurrence, not a restatement of a fact: "docked at A" and "docked at B" are
        // near-identical in meaning (they differ only in a name), so vector dedup would collapse them and make an
        // enumeration ("which stations did we dock at") impossible. An event therefore dedups only against a
        // LITERAL repeat (identical content, e.g. the same station twice); every other entry dedups by meaning.
        boolean literal = incoming.source() == MemorySource.EVENT;
        if (!literal && incoming.embedding() == null) {
            return incoming; // meaning dedup needs a vector; with none (semantic search off) there is nothing to do
        }
        double floor = CompanionConfig.semanticDedupFloor();
        MemoryEntry keep = incoming;
        Instant freshest = incoming.timestamp();
        boolean merged = false;
        for (MemoryEntry existing : duplicatesOf(incoming, floor)) {
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

    /** Every stored mid-term entry that is a duplicate of {@code probe} (short-term is kept verbatim; the MAX archive is left intact). */
    private List<MemoryEntry> duplicatesOf(MemoryEntry probe, double floor) {
        List<MemoryEntry> out = new ArrayList<>();
        // Short-term is intentionally not scanned: the hot window keeps every entry, including repeated
        // boundary markers, so only durable mid-term facts are collapsed by meaning.
        collectDuplicates(out, midTerm.allEntries(), probe, floor);
        return out;
    }

    private static void collectDuplicates(List<MemoryEntry> out, List<MemoryEntry> entries,
                                          MemoryEntry probe, double floor) {
        for (MemoryEntry entry : entries) {
            if (isDuplicate(probe, entry, floor)) {
                out.add(entry);
            }
        }
    }

    /**
     * Whether {@code entry} duplicates {@code probe}. Anything involving an event uses LITERAL equality
     * (identical content) - a discrete occurrence is only a duplicate of an exact repeat, never of a
     * merely similar-meaning line; every other pair dedups by meaning (cosine &ge; {@code floor}).
     */
    private static boolean isDuplicate(MemoryEntry probe, MemoryEntry entry, double floor) {
        if (probe.source() == MemorySource.EVENT || entry.source() == MemorySource.EVENT) {
            return Objects.equals(probe.content(), entry.content());
        }
        return MemorySearch.sameMeaning(probe, entry, floor);
    }

    /** Removes a superseded entry from whichever store holds it (short-term first, then mid-term). */
    private void removeStored(MemoryEntry entry) {
        if (!shortTerm.remove(entry)) {
            midTerm.remove(entry);
        }
    }

    /**
     * Normalizes an entry for storage (used by {@link #write}): both texts are stored
     * lower-cased (case carries no recall signal; search lower-cases anyway) and the meaning-vector is computed
     * once here on the clean candidate text ({@link MemoryEntry#embeddingText}: the canonical fact when present,
     * else the verbatim content), so semantic recall reads it for free. A null-content entry is returned as-is.
     */
    private MemoryEntry prepareForStore(MemoryEntry entry) {
        if (entry.content() == null) {
            return entry;
        }
        String lowerContent = entry.content().toLowerCase(Locale.ROOT);
        String lowerCanonical = entry.canonicalFact() == null ? null : entry.canonicalFact().toLowerCase(Locale.ROOT);
        MemoryEntry lowered = new MemoryEntry(entry.timestamp(), entry.topic(), entry.source(), lowerContent,
                entry.importance(), null, lowerCanonical, entry.toolLink());
        return lowered.withEmbedding(embed(lowered.embeddingText()));
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
