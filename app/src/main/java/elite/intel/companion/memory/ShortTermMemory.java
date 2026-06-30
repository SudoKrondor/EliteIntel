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
     * Appends an entry, collapsing an exact duplicate already in the timeline: if the same author, topic, and
     * (normalized) content is already present, the older copy is removed first so only the most recent stays.
     * This keeps a command cycled during combat ("target drive" ... "target power plant" ... "target drive")
     * from filling the hot timeline, while the surviving copy reflects the current state. The dropped copy is
     * discarded, not evicted to mid-term - a duplicate carries no new information.
     */
    void add(MemoryEntry entry) {
        removeDuplicate(entry);
        entries.add(entry);
        estimatedTokens += cost(entry);
    }

    /** Removes an entry already in the timeline that duplicates {@code entry} (same source, topic, content). */
    private void removeDuplicate(MemoryEntry entry) {
        for (int i = 0; i < entries.size(); i++) {
            MemoryEntry existing = entries.get(i);
            if (existing.source() == entry.source()
                    && existing.topic() == entry.topic()
                    && normalize(existing.content()).equals(normalize(entry.content()))) {
                estimatedTokens -= cost(existing);
                entries.remove(i);
                return; // every add dedups, so at most one prior copy can exist
            }
        }
    }

    /** Dedup key: trimmed, internal whitespace collapsed (content is already lower-cased on write). */
    private static String normalize(String content) {
        return content == null ? "" : content.trim().replaceAll("\\s+", " ");
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
