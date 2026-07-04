package elite.intel.companion.memory;

import elite.intel.companion.CompanionConfig;
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

    /**
     * Appends an entry to the hot timeline verbatim - the short-term window is not de-duplicated. Every turn is
     * kept as its own entry until it ages out by the count/token bounds, so repeated commands and, crucially,
     * repeated structural boundary markers survive (a second {@code <no_reply/>} never deletes an earlier one,
     * which would re-merge the two commander turns it separated). Fact de-duplication happens only in mid-term
     * (see {@code SessionMemoryGateway.mergeDuplicate}), where near-duplicates would otherwise crowd out recall.
     */
    void add(MemoryEntry entry) {
        entries.add(entry);
        estimatedTokens += cost(entry);
    }

    /** Current timeline, oldest-to-newest. */
    List<MemoryEntry> timeline() {
        return List.copyOf(entries);
    }

    /**
     * Removes the given entry (by identity) if present, keeping the token estimate in sync. Used by the
     * gateway's semantic de-duplication when a re-stated fact supersedes this copy. Returns whether it removed.
     */
    boolean remove(MemoryEntry entry) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i) == entry) {
                estimatedTokens -= cost(entries.get(i));
                entries.remove(i);
                return true;
            }
        }
        return false;
    }

    /** Evicts entries that exceed the count/token limits and returns them for mid-term storage. */
    List<MemoryEntry> evictOverflow() {
        List<MemoryEntry> evicted = new ArrayList<>();
        // The count cap is the hard limit. The token budget only evicts while more than the newest
        // entry remains, so the hot timeline always keeps at least the latest entry even if that one
        // entry alone exceeds the budget.
        while (!entries.isEmpty()
                && (entries.size() > CompanionConfig.shortTermMemorySize()
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
