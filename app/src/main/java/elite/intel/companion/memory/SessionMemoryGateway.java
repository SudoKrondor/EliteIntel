package elite.intel.companion.memory;

import elite.intel.ai.brain.i18n.InputNormalizerLocalizations;
import elite.intel.companion.CompanionConfig;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemoryImportance;
import elite.intel.companion.prompt.CompanionWordMatch;

import java.util.*;

/**
 * Default {@link MemoryGateway} implementation. Composes the session memory areas
 * (short-term, mid-term topic, long-term summary) and owns the eviction transitions
 * between them. The internal stores are package-private; nothing outside this package touches them.
 * <p>
 * Session-only: nothing is persisted to disk.
 * <p>
 * Thread-safety: the public methods are {@code synchronized} because writers arrive from several threads -
 * the EVENT/NARRATION lane workers and the bounded pool of COMMANDER lane workers (several commander
 * thoughts run at once). The internal stores are plain collections, so all access is serialized here; reads
 * return snapshots ({@code List.copyOf}), so a caller iterates outside the lock safely.
 */
public final class SessionMemoryGateway implements MemoryGateway {

    private final TokenEstimator tokenEstimator;
    private final ShortTermMemory shortTerm;
    private final MidTermTopicMemory midTerm = new MidTermTopicMemory();
    private final LongTermMemory longTerm = new LongTermMemory();

    // Hands mid-term overflow to the consolidator; no-op until wired at subsystem start. The gateway stays
    // mechanical (it never calls the LLM) - it only forwards evicted entries.
    private volatile MidTermEvictionListener evictionListener = entry -> {};

    /** Production constructor: uses the default heuristic token estimator. */
    public SessionMemoryGateway() {
        this(new HeuristicTokenEstimator());
    }

    /** Injectable constructor for tests and a future provider-accurate tokenizer swap. */
    SessionMemoryGateway(TokenEstimator tokenEstimator) {
        this.tokenEstimator = tokenEstimator;
        this.shortTerm = new ShortTermMemory(tokenEstimator);
    }

    /** Registers the consolidator that consumes mid-term overflow; defaults to a no-op until set. */
    public void setMidTermEvictionListener(MidTermEvictionListener listener) {
        this.evictionListener = listener == null ? entry -> {} : listener;
    }

    @Override
    public synchronized void write(MemoryEntry entry) {
        // Stored lower-cased: case carries no recall signal (search lower-cases anyway) and it keeps the
        // inlined timeline uniform. New entries land in short-term first; whatever overflows the count/token
        // bounds is moved into mid-term topic memory by topic (never duplicated across both levels).
        MemoryEntry stored = entry.content() == null ? entry : new MemoryEntry(
                entry.timestamp(), entry.topic(), entry.source(), entry.content().toLowerCase(Locale.ROOT),
                entry.importance());
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
        // Unified search across short-term timeline + mid-term (all topics) + the MAX archive, ranked by
        // relevance first. Matching is word-overlap (does the entry share a meaningful word with the query),
        // NOT a contiguous substring, so the model's paraphrased or whole-question query still finds the fact.
        Set<String> queryTokens = tokens(query);
        List<Hit> hits = new ArrayList<>();
        // Short-term is already inlined into the prompt, but search it too: if the model does decide to recall,
        // it then gets the whole picture instead of missing the most recent facts. A given entry lives in exactly
        // one of short-term / mid-term (mid-term only receives short-term overflow), so the two never double-count.
        addHits(hits, queryTokens, shortTerm.timeline());
        addHits(hits, queryTokens, midTerm.allEntries());
        // The MAX archive (pinned facts) is searchable but no longer force-fed into every prompt. Cap how many
        // archive matches are eligible so an accumulating archive of old MAX facts cannot crowd the more
        // relevant short/mid-term matches out of the result limit: only the best ARCHIVE_RECALL_LIMIT enter.
        List<Hit> archive = new ArrayList<>();
        addHits(archive, queryTokens, longTerm.pinnedFacts());
        archive.sort(BY_RANK);
        archive.stream().limit(CompanionMemoryLimits.ARCHIVE_RECALL_LIMIT).forEach(hits::add);
        // Relevance first (a strongly-matching routine fact beats a weakly-matching MAX), then importance (a
        // real MAX/HIGH fact beats trivia that shares as many words), then recency.
        hits.sort(BY_RANK);
        return hits.stream().limit(Math.max(0, limit)).map(Hit::content).distinct().toList();
    }

    /**
     * Adds every entry that shares at least one meaningful word with the query (all entries, relevance 0, for a
     * blank query - which then degenerates to importance-then-recency). The speaker tag is carried so the model
     * knows whose words it recalled (same {@link MemorySource#displayLabel} convention as the timeline legend).
     */
    private void addHits(List<Hit> out, Set<String> queryTokens, List<MemoryEntry> entries) {
        boolean blank = queryTokens.isEmpty();
        for (MemoryEntry entry : entries) {
            int relevance = blank ? 0 : overlap(queryTokens, entry.content());
            if (blank || relevance > 0) {
                out.add(new Hit(relevance, entry.importance(), entry.timestamp(),
                        "[" + entry.source().displayLabel(CompanionConfig.companionName()) + "] " + entry.content()));
            }
        }
    }

    /** A matched memory entry: its relevance score, importance and write time, for ranking the memory areas. */
    private record Hit(int relevance, MemoryImportance importance, java.time.Instant at, String content) {}

    /** Relevance first, then importance, then recency - the single recall ranking shared by every source. */
    private static final Comparator<Hit> BY_RANK = Comparator
            .comparingInt(Hit::relevance).reversed()
            .thenComparing(Hit::importance, Comparator.reverseOrder())
            .thenComparing(Hit::at, Comparator.reverseOrder());

    /**
     * The relevance score of an entry: how many distinct query tokens have an inflection-tolerant match in the
     * content. Two words are compared with the companion's shared rule ({@link CompanionWordMatch}): the same
     * word up to an appended/changed ending or a small typo, so a paraphrase in a different word form
     * ("jump"/"jumps", "звезда"/"звезду") still recalls the stored fact. Over-recall is cheap here, so the
     * tolerant rule is used for every language.
     */
    private static int overlap(Set<String> queryTokens, String content) {
        Set<String> contentTokens = tokens(content);
        int score = 0;
        for (String q : queryTokens) {
            for (String c : contentTokens) {
                if (CompanionWordMatch.similar(q, c)) {
                    score++;
                    break; // count each query token at most once
                }
            }
        }
        return score;
    }

    /** Meaningful lower-cased word tokens: length > 2 and not a stop word (same filter as the action reducer). */
    private static Set<String> tokens(String text) {
        if (text == null) {
            return Set.of();
        }
        Set<String> set = new HashSet<>();
        for (String word : text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}_]+")) {
            if (word.length() > 2 && !InputNormalizerLocalizations.stopWords().contains(word)) {
                set.add(word);
            }
        }
        return set;
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
        longTerm.pin(fact);
    }
}
