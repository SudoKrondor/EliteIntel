package elite.intel.ai.brain.vega.memory;

import elite.intel.ai.brain.vega.model.memory.MemoryEntry;
import elite.intel.ai.brain.vega.model.memory.MemoryRecord;

import java.util.ArrayList;
import java.util.List;

/** Recent completed records replayed in the prompt; overflow is evicted only as whole records. */
public final class RecentMemory {

    private final TokenEstimator tokenEstimator;
    private final List<MemoryRecord> records = new ArrayList<>();
    private int estimatedTokens;

    public RecentMemory(TokenEstimator tokenEstimator) {
        this.tokenEstimator = tokenEstimator;
    }

    /** Inserts one already-completed record by completion time, including a gist that arrived asynchronously. */
    public void add(MemoryRecord record) {
        insertChronologically(records, record);
        estimatedTokens += cost(record);
    }

    /** Current recent history, oldest-to-newest. */
    public List<MemoryRecord> records() {
        return List.copyOf(records);
    }

    /** Evicts oldest whole records until both recent-memory limits are satisfied. */
    public List<MemoryRecord> evictOverflow() {
        List<MemoryRecord> evicted = new ArrayList<>();
        while (!records.isEmpty()
                && (records.size() > CompanionMemoryPolicy.recentRecordLimit()
                || (estimatedTokens > CompanionMemoryPolicy.recentTokenBudget() && records.size() > 1))) {
            MemoryRecord oldest = records.remove(0);
            estimatedTokens -= cost(oldest);
            evicted.add(oldest);
        }
        return evicted;
    }

    private int cost(MemoryRecord record) {
        int tokens = CompanionMemoryPolicy.recordFramingTokens();
        for (MemoryEntry entry : record.entries()) {
            tokens += tokenEstimator.estimate(entry.content())
                    + CompanionMemoryPolicy.entryFramingTokens();
        }
        return tokens;
    }

    private static void insertChronologically(List<MemoryRecord> records, MemoryRecord record) {
        int index = records.size();
        while (index > 0 && records.get(index - 1).timestamp().isAfter(record.timestamp())) {
            index--;
        }
        records.add(index, record);
    }
}
