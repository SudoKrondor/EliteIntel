package elite.intel.companion.memory;

import java.util.List;

/**
 * Small cyclic LLM scratch memory. Not split by topic, not consolidated. Fixed limits (max 15
 * entries, max 50 chars each); a new 16th entry evicts the oldest; exact duplicates are not added.
 * <p>
 * The class is public only so its agreed limit constants are a single cross-package source of truth
 * (e.g. for the {@code remember} tool description); instantiation and behavior stay internal to
 * {@link SessionMemoryGateway} via the package-private constructor and methods.
 */
public final class LlmMemory {

    /** Maximum number of items. */
    public static final int MAX_ENTRIES = 15;
    /** Maximum characters per item (code truncates longer content). */
    public static final int MAX_CONTENT_LENGTH = 50;

    /** Package-private: only the memory package constructs this internal store. */
    LlmMemory() {
    }

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
