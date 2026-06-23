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
                && (entries.size() > CompanionMemoryLimits.SHORT_TERM_MAX_ENTRIES
                || (estimatedTokens > CompanionMemoryLimits.SHORT_TERM_TOKEN_BUDGET && entries.size() > 1))) {
            MemoryEntry oldest = entries.remove(0);
            estimatedTokens -= cost(oldest);
            evicted.add(oldest);
        }
        return evicted;
    }

    /** Estimated prompt token cost of one entry: its content plus the fixed framing overhead. */
    private int cost(MemoryEntry entry) {
        return tokenEstimator.estimate(entry.content()) + CompanionMemoryLimits.SHORT_TERM_ENTRY_FRAMING_OVERHEAD_TOKENS;
    }
}
