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

    private final ShortTermMemory shortTerm = new ShortTermMemory();
    private final MidTermTopicMemory midTerm = new MidTermTopicMemory();
    private final LongTermMemory longTerm = new LongTermMemory();
    private final LlmMemory llmMemory = new LlmMemory();

    @Override
    public void write(MemoryEntry entry) {
        // TODO: Phase 2 - append to short-term; on overflow evict oldest into mid-term by topic.
        throw new UnsupportedOperationException("TODO: Phase 2");
    }

    @Override
    public List<MemoryEntry> readShortTermTimeline() {
        // TODO: Phase 2
        throw new UnsupportedOperationException("TODO: Phase 2");
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
        // TODO: Phase 2 - cheap index metadata.
        throw new UnsupportedOperationException("TODO: Phase 2");
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
