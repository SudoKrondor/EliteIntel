package elite.intel.ai.brain.vega.memory;

import elite.intel.ai.brain.vega.diag.CompanionDiagnostics;
import elite.intel.ai.brain.vega.model.memory.*;
import elite.intel.ai.embed.SemanticPhraseMatcher;
import elite.intel.ai.embed.SemanticSearchProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Session-only memory owner. Mutations are serialized, while embedding and ranking operate on detached snapshots
 * so slow semantic work never holds the memory lock.
 */
public final class SessionMemoryGateway implements MemoryGateway {

    private static final Logger log = LogManager.getLogger(SessionMemoryGateway.class);

    private final Supplier<SemanticPhraseMatcher> matcherSource;
    private final RecentMemory recent;
    private final MidTermMemory midTerm = new MidTermMemory();
    private final LongTermMemory longTerm = new LongTermMemory();
    private volatile PendingConsolidationListener consolidationListener = record -> { };
    private volatile OversizedMemoryListener oversizedListener;

    /** Production constructor using the shared semantic matcher. */
    public SessionMemoryGateway() {
        this(SemanticSearchProvider::matcher);
    }

    /** Chooses the semantic matcher source; {@code () -> null} keeps recall word-only. */
    public SessionMemoryGateway(Supplier<SemanticPhraseMatcher> matcherSource) {
        this(new HeuristicTokenEstimator(), matcherSource);
    }

    /** Test seam with an injectable token estimator and word-only recall. */
    SessionMemoryGateway(TokenEstimator tokenEstimator) {
        this(tokenEstimator, () -> null);
    }

    /** Canonical constructor with both estimators explicit. */
    SessionMemoryGateway(TokenEstimator tokenEstimator, Supplier<SemanticPhraseMatcher> matcherSource) {
        this.matcherSource = Objects.requireNonNull(matcherSource, "matcherSource");
        this.recent = new RecentMemory(Objects.requireNonNull(tokenEstimator, "tokenEstimator"));
    }

    /** Registers the background consumer of records pending consolidation. */
    public void setPendingConsolidationListener(PendingConsolidationListener listener) {
        consolidationListener = listener == null ? record -> { } : listener;
    }

    /** Registers the background owner of whole-record oversized compression; null restores bounded fallback. */
    public void setOversizedMemoryListener(OversizedMemoryListener listener) {
        oversizedListener = listener;
    }

    /**
     * Stores one completed record atomically. SAVED_TEXT uses its own verbatim length/count limits. An oversized
     * ordinary record is handed off whole before the first mutation; when no compressor accepts it, every entry is
     * bounded synchronously and the complete record still enters recent memory together.
     */
    @Override
    public void write(MemoryRecord record) {
        Objects.requireNonNull(record, "record");
        if (record.kind() != MemoryKind.SAVED_TEXT && hasOversizedEntry(record) && handOffOversized(record)) {
            return;
        }
        MemoryRecord stored = prepareForStore(record, record.kind() != MemoryKind.SAVED_TEXT);
        List<MemoryRecord> staged;
        synchronized (this) {
            if (stored.kind() == MemoryKind.SAVED_TEXT) {
                longTerm.saveText(stored);
                return;
            }

            recent.add(stored);
            for (MemoryRecord evicted : recent.evictOverflow()) {
                if (evicted.kind().movesToMidTerm()) {
                    midTerm.add(evicted);
                }
                // QUERY has fulfilled its short-lived prompt role and is deliberately dropped here.
            }
            staged = midTerm.stageOverflow();
        }
        PendingConsolidationListener listener = consolidationListener;
        for (MemoryRecord recordToConsolidate : staged) {
            try {
                listener.onPending(recordToConsolidate);
            } catch (RuntimeException failure) {
                log.error("Pending memory record could not be submitted for consolidation", failure);
            }
        }
    }

    @Override
    public synchronized List<MemoryRecord> readRecentHistory() {
        return recent.records();
    }

    @Override
    public MemorySearchResult recallMatching(String query, int limit) {
        SearchCorpus corpus = searchCorpus();
        MemorySearchResult result = MemorySearch.recall(query, limit, corpus.recent(), corpus.retained(),
                corpus.summaries(), corpus.savedTexts(), matcherSource);
        CompanionDiagnostics.debugAmbient("memory-search",
                "\"" + CompanionDiagnostics.truncate(query) + "\" -> "
                        + result.matchingUnits() + " matching unit(s), " + result.items().size() + " returned");
        return result;
    }

    private synchronized SearchCorpus searchCorpus() {
        return new SearchCorpus(recent.records(), midTerm.allRecords(),
                longTerm.summaryMatches(), longTerm.savedTexts());
    }

    @Override
    public synchronized Map<MemoryKind, String> longTermSummaries() {
        return longTerm.summaries();
    }

    @Override
    public void commitConsolidation(MemoryKind kind, List<MemoryRecord> batch, String summary) {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(batch, "batch");
        if (summary == null || summary.isBlank()
                || summary.length() > CompanionMemoryPolicy.summaryMaxChars()) {
            throw new IllegalArgumentException("A consolidated summary must be non-blank and within its limit");
        }
        Instant batchEvidenceAt = batch.stream()
                .map(MemoryRecord::timestamp)
                .max(Instant::compareTo)
                .orElseThrow(() -> new IllegalArgumentException("A consolidation batch must not be empty"));
        MemoryEntry summaryEntry = new MemoryEntry(MemorySource.SYSTEM, summary.strip(), embed(summary));
        synchronized (this) {
            midTerm.requirePending(kind, batch);
            Instant previousEvidenceAt = longTerm.summaryEvidenceAt(kind);
            Instant evidenceAt = previousEvidenceAt != null && previousEvidenceAt.isAfter(batchEvidenceAt)
                    ? previousEvidenceAt : batchEvidenceAt;
            LongTermSummary replacement = new LongTermSummary(evidenceAt, summaryEntry);
            longTerm.replaceSummary(kind, replacement);
            midTerm.acknowledge(kind, batch);
        }
    }

    @Override
    public synchronized List<MemoryRecord> savedTextRecords() {
        return longTerm.savedTexts();
    }

    @Override
    public synchronized MemorySnapshot snapshot() {
        return new MemorySnapshot(recent.records(), midTerm.retainedSnapshot(), midTerm.pendingSnapshot(),
                longTerm.summaries(), longTerm.savedTexts());
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

    /** Bounds ordinary prompt-visible entries as a safety fallback and attaches semantic vectors. */
    private MemoryRecord prepareForStore(MemoryRecord record, boolean applyEntryLimit) {
        List<MemoryEntry> prepared = new ArrayList<>(record.entries().size());
        for (MemoryEntry entry : record.entries()) {
            if (record.kind() == MemoryKind.SAVED_TEXT
                    && entry.content().length() > CompanionMemoryPolicy.savedTextMaxChars()) {
                throw new IllegalArgumentException("SAVED_TEXT entry exceeds its length limit");
            }
            String content = applyEntryLimit ? MemoryTextBounds.entry(entry.content()) : entry.content();
            prepared.add(new MemoryEntry(entry.source(), content, embed(content)));
        }
        return record.withEntries(prepared);
    }

    private float[] embed(String text) {
        if (text.isBlank()) {
            return null;
        }
        try {
            SemanticPhraseMatcher matcher = matcherSource.get();
            if (matcher == null) {
                return null;
            }
            return matcher.embedQuery(text);
        } catch (RuntimeException failure) {
            log.warn("Embedding a memory entry failed; storing it without a meaning-vector", failure);
            return null;
        }
    }

    private record SearchCorpus(
            List<MemoryRecord> recent,
            List<MemoryRecord> retained,
            List<MemorySearchMatch> summaries,
            List<MemoryRecord> savedTexts
    ) {
    }

}
