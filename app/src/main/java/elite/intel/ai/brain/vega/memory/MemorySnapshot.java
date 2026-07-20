package elite.intel.ai.brain.vega.memory;

import elite.intel.ai.brain.vega.model.memory.MemoryKind;
import elite.intel.ai.brain.vega.model.memory.MemoryRecord;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable snapshot of every session-memory area.
 *
 * @param recent          completed records currently replayed in prompts
 * @param retainedByKind retained DIALOGUE/EVENT records, grouped by their eviction policy
 * @param pendingByKind  records still searchable while their long-term summary is being prepared
 * @param summaries      long-term summaries grouped by retained kind
 * @param savedTexts     explicitly saved verbatim text
 */
public record MemorySnapshot(
        List<MemoryRecord> recent,
        Map<MemoryKind, List<MemoryRecord>> retainedByKind,
        Map<MemoryKind, List<MemoryRecord>> pendingByKind,
        Map<MemoryKind, String> summaries,
        List<MemoryRecord> savedTexts
) {
    public MemorySnapshot {
        recent = List.copyOf(recent);
        retainedByKind = immutableCopy(retainedByKind);
        pendingByKind = immutableCopy(pendingByKind);
        summaries = Map.copyOf(summaries);
        savedTexts = List.copyOf(savedTexts);
    }

    private static Map<MemoryKind, List<MemoryRecord>> immutableCopy(
            Map<MemoryKind, List<MemoryRecord>> source
    ) {
        Map<MemoryKind, List<MemoryRecord>> copy = new EnumMap<>(MemoryKind.class);
        source.forEach((kind, records) -> copy.put(kind, List.copyOf(records)));
        return Map.copyOf(copy);
    }
}
