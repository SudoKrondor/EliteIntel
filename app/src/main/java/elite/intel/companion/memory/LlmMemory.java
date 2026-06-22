package elite.intel.companion.memory;

import java.util.List;

/**
 * Small cyclic LLM scratch memory. Not split by topic, not consolidated. Fixed limits (max 15
 * entries, max 50 chars each); a new 16th entry evicts the oldest; exact duplicates are not added.
 * Package-private internal of {@link SessionMemoryGateway}.
 */
class LlmMemory {

    /** Maximum number of items. */
    static final int MAX_ENTRIES = 15;
    /** Maximum characters per item (code truncates longer content). */
    static final int MAX_CONTENT_LENGTH = 50;

    /** Returns the whole list (small enough to return entirely). */
    List<String> all() {
        throw new UnsupportedOperationException("TODO: Phase 4");
    }

    /** Adds a fact (cyclic eviction, dedup, truncation handled here). */
    void add(String content) {
        throw new UnsupportedOperationException("TODO: Phase 4");
    }

    /** Current item count. */
    int size() {
        throw new UnsupportedOperationException("TODO: Phase 4");
    }
}
