package elite.intel.ai.brain.vega.memory;

import elite.intel.ai.brain.vega.model.memory.MemoryEntry;
import elite.intel.ai.brain.vega.model.memory.MemoryRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Session-only memory owner: the bounded window of completed exchanges the next prompt replays, and nothing else.
 * Mutations are serialized; a record that overflows the window is dropped, because the window is the whole store.
 * <p>
 * There is deliberately no tier below this one. A retained history and its LLM-written summaries existed here until
 * the only thing that read them - an explicit recall query - was removed; everything they collected was then written
 * and never read again, at the cost of a model call per batch. What the companion knows about the game comes from
 * the live {@code <facts>} block and the game queries, both of which read the database, not from stored conversation.
 */
public final class SessionMemoryGateway implements MemoryGateway {

    private static final Logger log = LogManager.getLogger(SessionMemoryGateway.class);

    private final RecentMemory recent;
    private volatile OversizedMemoryListener oversizedListener;

    /**
     * Production constructor.
     */
    public SessionMemoryGateway() {
        this(new HeuristicTokenEstimator());
    }

    /** Test seam with an injectable token estimator. */
    SessionMemoryGateway(TokenEstimator tokenEstimator) {
        this.recent = new RecentMemory(Objects.requireNonNull(tokenEstimator, "tokenEstimator"));
    }

    /** Registers the background owner of whole-record oversized compression; null restores bounded fallback. */
    public void setOversizedMemoryListener(OversizedMemoryListener listener) {
        oversizedListener = listener;
    }

    /**
     * Stores one completed record atomically. An oversized record is handed off whole before the first mutation;
     * when no compressor accepts it, every entry is bounded synchronously and the complete record still enters
     * recent memory together.
     */
    @Override
    public void write(MemoryRecord record) {
        Objects.requireNonNull(record, "record");
        if (hasOversizedEntry(record) && handOffOversized(record)) {
            return;
        }
        MemoryRecord stored = prepareForStore(record);
        synchronized (this) {
            recent.add(stored);
            recent.evictOverflow();
        }
    }

    @Override
    public synchronized List<MemoryRecord> readRecentHistory() {
        return recent.records();
    }

    @Override
    public synchronized MemorySnapshot snapshot() {
        return new MemorySnapshot(recent.records());
    }

    private boolean handOffOversized(MemoryRecord record) {
        OversizedMemoryListener listener = oversizedListener;
        if (listener == null) {
            return false;
        }
        try {
            return listener.onOversized(record);
        } catch (RuntimeException failure) {
            log.warn("Oversized memory handoff failed; storing the bounded record synchronously", failure);
            return false;
        }
    }

    private static boolean hasOversizedEntry(MemoryRecord record) {
        int max = CompanionMemoryPolicy.entryMaxChars();
        return record.entries().stream().anyMatch(entry -> entry.content().length() > max);
    }

    /**
     * Bounds prompt-visible entries as a safety fallback.
     */
    private static MemoryRecord prepareForStore(MemoryRecord record) {
        List<MemoryEntry> prepared = new ArrayList<>(record.entries().size());
        for (MemoryEntry entry : record.entries()) {
            prepared.add(new MemoryEntry(entry.source(), MemoryTextBounds.entry(entry.content())));
        }
        return record.withEntries(prepared);
    }
}
