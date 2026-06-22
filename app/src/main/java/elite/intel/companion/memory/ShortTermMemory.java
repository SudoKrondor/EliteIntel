package elite.intel.companion.memory;

import elite.intel.companion.model.memory.MemoryEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Hot chronological timeline of recent entries, inserted directly into the prompt as a context block.
 * Bounded by max entry count and a token budget; overflow is evicted into mid-term topic memory.
 * Package-private internal of {@link SessionMemoryGateway}.
 */
class ShortTermMemory {

    /** Maximum entries kept in the hot timeline; the primary eviction control. */
    static final int MAX_ENTRIES = 20;
    /** Soft token ceiling for the timeline block; a backstop against unusually long entries. */
    static final int TOKEN_BUDGET = 1200;
    /** Per-entry token overhead for the framing each entry adds when rendered into the prompt. */
    static final int ENTRY_FRAMING_OVERHEAD_TOKENS = 4;

    private final TokenEstimator tokenEstimator;
    private final List<MemoryEntry> entries = new ArrayList<>();
    private int estimatedTokens;

    ShortTermMemory(TokenEstimator tokenEstimator) {
        this.tokenEstimator = tokenEstimator;
    }

    /** Appends a new entry. */
    void add(MemoryEntry entry) {
        entries.add(entry);
        estimatedTokens += cost(entry);
    }

    /** Current timeline, oldest-to-newest. */
    List<MemoryEntry> timeline() {
        return List.copyOf(entries);
    }

    /** Evicts entries that exceed the count/token limits and returns them for mid-term storage. */
    List<MemoryEntry> evictOverflow() {
        List<MemoryEntry> evicted = new ArrayList<>();
        // The count cap is the hard limit. The token budget only evicts while more than the newest
        // entry remains, so the hot timeline always keeps at least the latest entry even if that one
        // entry alone exceeds the budget.
        while (!entries.isEmpty()
                && (entries.size() > MAX_ENTRIES
                || (estimatedTokens > TOKEN_BUDGET && entries.size() > 1))) {
            MemoryEntry oldest = entries.remove(0);
            estimatedTokens -= cost(oldest);
            evicted.add(oldest);
        }
        return evicted;
    }

    /** Estimated prompt token cost of one entry: its content plus the fixed framing overhead. */
    private int cost(MemoryEntry entry) {
        return tokenEstimator.estimate(entry.content()) + ENTRY_FRAMING_OVERHEAD_TOKENS;
    }
}
