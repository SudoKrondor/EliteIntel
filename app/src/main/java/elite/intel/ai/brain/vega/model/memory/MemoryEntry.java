package elite.intel.ai.brain.vega.model.memory;

import java.util.Objects;

/**
 * One role entry inside a completed {@link MemoryRecord}. Text may be bounded when admitted to session memory.
 * <p>
 * Entries carried a cached semantic vector until the retained history that de-duplicated on it was removed; with
 * nothing left to search or de-duplicate, embedding every stored line was an ONNX call per write whose result was
 * never read.
 *
 * @param source  where the text came from
 * @param content recorded text
 */
public record MemoryEntry(MemorySource source, String content) {

    public MemoryEntry {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(content, "content");
        if (content.isBlank()) {
            throw new IllegalArgumentException("Memory entry content must not be blank");
        }
    }
}
