package elite.intel.companion.memory;

import elite.intel.companion.model.memory.MemoryKind;
import elite.intel.companion.model.memory.MemorySearchMatch;
import elite.intel.companion.model.memory.MemoryRecord;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Long-term summaries plus bounded explicitly saved text that is never compressed. */
final class LongTermMemory {

    private final Map<MemoryKind, LongTermSummary> summaries = new EnumMap<>(MemoryKind.class);
    private final List<MemoryRecord> savedTexts = new ArrayList<>();

    /** Current summary text by summarized memory kind. */
    Map<MemoryKind, String> summaries() {
        Map<MemoryKind, String> textByKind = new EnumMap<>(MemoryKind.class);
        summaries.forEach((kind, summary) -> textByKind.put(kind, summary.text()));
        return Map.copyOf(textByKind);
    }

    /** Searchable summary entries dated by their newest source record. */
    List<MemorySearchMatch> summaryMatches() {
        return summaries.entrySet().stream()
                .map(summary -> new MemorySearchMatch(
                        summary.getKey(), summary.getValue().evidenceAt(), summary.getValue().entry()))
                .toList();
    }

    /** Timestamp of the newest source record represented by the current summary, or null when absent. */
    Instant summaryEvidenceAt(MemoryKind kind) {
        LongTermSummary summary = summaries.get(kind);
        return summary == null ? null : summary.evidenceAt();
    }

    /** Replaces the summary of one supported memory kind. */
    void replaceSummary(MemoryKind kind, LongTermSummary summary) {
        if (!kind.hasLongTermSummary()) {
            throw new IllegalArgumentException("No long-term summary for " + kind);
        }
        summaries.put(kind, summary);
    }

    /** Explicitly saved texts in insertion order. */
    List<MemoryRecord> savedTexts() {
        return List.copyOf(savedTexts);
    }

    /** Saves one exact commander phrase; an exact duplicate is ignored. */
    void saveText(MemoryRecord record) {
        if (record.kind() != MemoryKind.SAVED_TEXT) {
            throw new IllegalArgumentException("Only SAVED_TEXT records can enter explicitly saved memory");
        }
        String text = record.savedText();
        for (MemoryRecord existing : savedTexts) {
            if (existing.savedText().equals(text)) {
                return;
            }
        }
        if (savedTexts.size() >= CompanionMemoryPolicy.savedTextRecordLimit()) {
            throw new IllegalStateException("Saved-text session limit reached");
        }
        savedTexts.add(record);
    }
}
