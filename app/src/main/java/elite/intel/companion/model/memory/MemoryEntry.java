package elite.intel.companion.model.memory;

import java.util.Objects;

/**
 * One role entry inside a completed {@link MemoryRecord}. Ordinary text may be bounded when admitted to session
 * memory; explicitly saved text remains verbatim. The optional embedding is immutable derived search data.
 *
 * @param source    where the text came from
 * @param content   recorded text
 * @param embedding cached semantic-search vector
 */
public record MemoryEntry(
        MemorySource source,
        String content,
        float[] embedding
) {

    public MemoryEntry {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(content, "content");
        if (content.isBlank()) {
            throw new IllegalArgumentException("Memory entry content must not be blank");
        }
        embedding = embedding == null ? null : embedding.clone();
    }

    /** Creates an ordinary entry without derived search data. */
    public MemoryEntry(MemorySource source, String content) {
        this(source, content, null);
    }

    /** Returns a copy carrying the given semantic-search vector. */
    public MemoryEntry withEmbedding(float[] embedding) {
        return new MemoryEntry(source, content, embedding);
    }

    /** Returns a defensive copy of the derived semantic vector. */
    @Override
    public float[] embedding() {
        return embedding == null ? null : embedding.clone();
    }

    // The vector is derived data; identity is the recorded source and text.
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof MemoryEntry entry
                && source == entry.source
                && Objects.equals(content, entry.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, content);
    }
}
