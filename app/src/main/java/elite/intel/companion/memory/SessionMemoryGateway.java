package elite.intel.companion.memory;

import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.ConversationTopic;

import java.util.List;

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

    /** Production constructor: uses the default heuristic token estimator. */
    public SessionMemoryGateway() {
        this(new HeuristicTokenEstimator());
    }

    /** Injectable constructor for tests and a future provider-accurate tokenizer swap. */
    SessionMemoryGateway(TokenEstimator tokenEstimator) {
        this.shortTerm = new ShortTermMemory(tokenEstimator);
    }

    @Override
    public void write(MemoryEntry entry) {
        // New entries land in short-term first; whatever overflows the count/token bounds is moved
        // into mid-term topic memory by topic (never duplicated across both levels).
        shortTerm.add(entry);
        for (MemoryEntry evicted : shortTerm.evictOverflow()) {
            midTerm.add(evicted);
        }
    }

    @Override
    public List<MemoryEntry> readShortTermTimeline() {
        return shortTerm.timeline();
    }

    @Override
    public List<MemoryEntry> recallTopicMemory(ConversationTopic topic, String query, int limit) {
        // TODO: Phase 4 - topic-scoped recall.
        throw new UnsupportedOperationException("TODO: Phase 4");
    }

    @Override
    public List<String> readLlmMemory() {
        // TODO: Phase 4 - llm_memory.
        throw new UnsupportedOperationException("TODO: Phase 4");
    }

    @Override
    public void writeLlmMemory(String content) {
        // TODO: Phase 4 - llm_memory (length/dedup enforcement).
        throw new UnsupportedOperationException("TODO: Phase 4");
    }

    @Override
    public MemoryAvailabilitySnapshot indexes() {
        // llm_memory is wired in Phase 4; until then it holds no items, but its capacity is already
        // known and owned by LlmMemory.
        return new MemoryAvailabilitySnapshot(0, LlmMemory.MAX_ENTRIES, midTerm.topicsWithMemory());
    }

    @Override
    public String longTermSummary() {
        // TODO: Phase 4 - long-term summary.
        throw new UnsupportedOperationException("TODO: Phase 4");
    }

    @Override
    public void replaceLongTermSummary(String summary) {
        // TODO: Phase 4 - long-term summary.
        throw new UnsupportedOperationException("TODO: Phase 4");
    }
}
