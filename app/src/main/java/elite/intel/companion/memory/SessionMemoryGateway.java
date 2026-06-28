package elite.intel.companion.memory;

import elite.intel.ai.brain.i18n.InputNormalizerLocalizations;
import elite.intel.companion.CompanionConfig;
import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.prompt.CompanionWordMatch;

import java.util.*;

/**
 * Default {@link MemoryGateway} implementation. Composes the four session memory areas
 * (short-term, mid-term topic, long-term summary, llm_memory) and owns the eviction transitions
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

    private final ShortTermMemory shortTerm;
    private final MidTermTopicMemory midTerm = new MidTermTopicMemory();
    private final LongTermMemory longTerm = new LongTermMemory();
    private final LlmMemory llmMemory = new LlmMemory();

    // Hands mid-term overflow to the consolidator; no-op until wired at subsystem start. The gateway stays
    // mechanical (it never calls the LLM) - it only forwards evicted entries.
    private volatile MidTermEvictionListener evictionListener = entry -> {};

    /** Production constructor: uses the default heuristic token estimator. */
    public SessionMemoryGateway() {
        this(new HeuristicTokenEstimator());
    }

    /** Injectable constructor for tests and a future provider-accurate tokenizer swap. */
    SessionMemoryGateway(TokenEstimator tokenEstimator) {
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
                entry.timestamp(), entry.topic(), entry.source(), entry.content().toLowerCase(Locale.ROOT));
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
        // Unified search across short-term timeline + mid-term (all topics) + conscious llm_memory, returned
        // newest-first. Matching is word-overlap (does the entry share a meaningful word with the query), NOT a
        // contiguous substring, so the model's paraphrased or whole-question query still finds the stored fact.
        Set<String> queryTokens = tokens(query);
        List<TimedHit> hits = new ArrayList<>();
        // Short-term is already inlined into the prompt, but search it too: if the model does decide to recall,
        // it then gets the whole picture instead of missing the most recent facts. A given entry lives in exactly
        // one of short-term / mid-term (mid-term only receives short-term overflow), so the two never double-count.
        for (MemoryEntry entry : shortTerm.timeline()) {
            if (matches(queryTokens, entry.content())) {
                hits.add(new TimedHit(entry.timestamp(),
                        "[" + entry.source().displayLabel(CompanionConfig.companionName()) + "] " + entry.content()));
            }
        }
        for (MemoryEntry entry : midTerm.allEntries()) {
            if (matches(queryTokens, entry.content())) {
                // Carry the speaker tag so the model knows whose words it recalled (same speaker-tag
                // convention as the timeline via MemorySource.displayLabel), matching its prompt legend.
                hits.add(new TimedHit(entry.timestamp(),
                        "[" + entry.source().displayLabel(CompanionConfig.companionName()) + "] " + entry.content()));
            }
        }
        for (LlmMemory.Item item : llmMemory.allItems()) {
            if (matches(queryTokens, item.content())) {
                hits.add(new TimedHit(item.at(), item.content()));
            }
        }
        hits.sort(Comparator.comparing(TimedHit::at).reversed());
        return hits.stream().limit(Math.max(0, limit)).map(TimedHit::content).distinct().toList();
    }

    /** A matched memory entry with its write time, for merging the two memory areas by recency. */
    private record TimedHit(java.time.Instant at, String content) {}

    /** An entry matches when it shares at least one meaningful word with the query; a query with no meaningful
     *  words (blank / all stop words) matches everything, i.e. returns simply the most recent entries. Two words
     *  are compared with the companion's shared inflection-tolerant rule ({@link CompanionWordMatch}): the same
     *  word up to an appended/changed ending or a small typo, so a paraphrase in a different word form
     *  ("jump"/"jumps", "звезда"/"звезду") still recalls the stored fact. Recall favours catching a match, so the
     *  tolerant rule is used for every language (over-recall is cheap here). */
    private static boolean matches(Set<String> queryTokens, String content) {
        if (queryTokens.isEmpty()) {
            return true;
        }
        Set<String> contentTokens = tokens(content);
        for (String q : queryTokens) {
            for (String c : contentTokens) {
                if (CompanionWordMatch.similar(q, c)) {
                    return true;
                }
            }
        }
        return false;
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
    public synchronized List<String> readLlmMemory() {
        return llmMemory.all();
    }

    @Override
    public synchronized void writeLlmMemory(String content) {
        llmMemory.add(content == null ? null : content.toLowerCase(Locale.ROOT));
    }

    @Override
    public synchronized MemoryAvailabilitySnapshot indexes() {
        return new MemoryAvailabilitySnapshot(llmMemory.size(), CompanionMemoryLimits.LLM_MEMORY_MAX_ENTRIES, midTerm.topicsWithMemory());
    }

    @Override
    public synchronized String longTermSummary() {
        return longTerm.get();
    }

    @Override
    public synchronized void replaceLongTermSummary(String summary) {
        longTerm.replace(summary);
    }
}
