package elite.intel.ai.brain.vega.memory;

import elite.intel.ai.brain.vega.model.memory.MemoryRecord;

import java.util.List;

/**
 * Immutable snapshot of session memory.
 *
 * @param recent completed records currently replayed in prompts - the whole store
 */
public record MemorySnapshot(List<MemoryRecord> recent) {
    public MemorySnapshot {
        recent = List.copyOf(recent);
    }
}
