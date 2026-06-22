package elite.intel.companion.memory;

import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.ConversationTopic;

import java.util.List;

/**
 * Topic-keyed archive of entries evicted from short-term memory. Bounded by max entries per topic;
 * overflow is evicted into the consolidation buffer. Never returned whole.
 * Package-private internal of {@link SessionMemoryGateway}.
 */
class MidTermTopicMemory {

    /** Stores an evicted entry under its topic. */
    void add(MemoryEntry entry) {
        throw new UnsupportedOperationException("TODO: Phase 2");
    }

    /** Topic-scoped recall (optional plain-text filter, capped at limit). */
    List<MemoryEntry> recall(ConversationTopic topic, String query, int limit) {
        throw new UnsupportedOperationException("TODO: Phase 4");
    }

    /** Topics that currently hold entries (for the prompt topic-memory index). */
    List<ConversationTopic> topicsWithMemory() {
        throw new UnsupportedOperationException("TODO: Phase 2");
    }

    /** Evicts per-topic overflow and returns it for consolidation. */
    List<MemoryEntry> evictOverflow() {
        throw new UnsupportedOperationException("TODO: Phase 4");
    }
}
