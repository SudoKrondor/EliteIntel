package elite.intel.companion.memory;

import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.ConversationTopic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Default {@link MemoryGateway} implementation. Composes the four session memory areas
 * (short-term, mid-term topic, long-term summary, llm_memory) and owns the eviction transitions
 * between them. The internal stores are package-private; nothing outside this package touches them.
 * <p>
 * Session-only: nothing is persisted to disk.
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
    public void write(MemoryEntry entry) {
        // New entries land in short-term first; whatever overflows the count/token bounds is moved
        // into mid-term topic memory by topic (never duplicated across both levels).
        shortTerm.add(entry);
        for (MemoryEntry evicted : shortTerm.evictOverflow()) {
            midTerm.add(evicted);
        }
        // Per-topic mid-term overflow is handed to the consolidator (long-term summary lives behind the LLM).
        for (MemoryEntry overflow : midTerm.evictOverflow()) {
            evictionListener.onEvicted(overflow);
        }
    }

    @Override
    public List<MemoryEntry> readShortTermTimeline() {
        return shortTerm.timeline();
    }

    @Override
    public List<MemoryEntry> recallTopicMemory(ConversationTopic topic, String query, int limit) {
        return midTerm.recall(topic, query, limit);
    }

    @Override
    public List<String> recallMatching(String query, int limit) {
        String q = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        // One unified search: mid-term across all topics + the conscious llm_memory facts, merged and
        // returned newest-first so the model never has to choose a scope or topic.
        List<TimedHit> hits = new ArrayList<>();
        for (MemoryEntry entry : midTerm.matchingAcrossTopics(q)) {
            hits.add(new TimedHit(entry.timestamp(), entry.content()));
        }
        for (LlmMemory.Item item : llmMemory.matching(q)) {
            hits.add(new TimedHit(item.at(), item.content()));
        }
        hits.sort(Comparator.comparing(TimedHit::at).reversed());
        return hits.stream().limit(Math.max(0, limit)).map(TimedHit::content).distinct().toList();
    }

    /** A matched memory entry with its write time, for merging the two memory areas by recency. */
    private record TimedHit(java.time.Instant at, String content) {}

    @Override
    public List<String> readLlmMemory() {
        return llmMemory.all();
    }

    @Override
    public void writeLlmMemory(String content) {
        llmMemory.add(content);
    }

    @Override
    public MemoryAvailabilitySnapshot indexes() {
        return new MemoryAvailabilitySnapshot(llmMemory.size(), CompanionMemoryLimits.LLM_MEMORY_MAX_ENTRIES, midTerm.topicsWithMemory());
    }

    @Override
    public String longTermSummary() {
        return longTerm.get();
    }

    @Override
    public void replaceLongTermSummary(String summary) {
        longTerm.replace(summary);
    }
}
