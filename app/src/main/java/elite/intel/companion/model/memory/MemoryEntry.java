package elite.intel.companion.model.memory;

import elite.intel.companion.model.ConversationTopic;

import java.time.Instant;
import java.util.Objects;

/**
 * One entry in the companion's experience timeline. Ordered by the actual write time at the
 * {@code MemoryGateway}, not by thought birth time.
 *
 * @param timestamp  actual write time
 * @param topic      topic this entry belongs to
 * @param source     where the information came from
 * @param content    the recorded text
 * @param importance how important this entry is to the companion's memory (drives retention and consolidation)
 * @param embedding  cached meaning-vector of {@link #content}, computed once by the gateway on write for
 *                   semantic recall; {@code null} when semantic search is unavailable or not yet computed.
 *                   Read-only: the embedder returns a fresh array per call, so callers must not mutate it.
 */
public record MemoryEntry(
        Instant timestamp,
        ConversationTopic topic,
        MemorySource source,
        String content,
        MemoryImportance importance,
        float[] embedding
) {
    /** Convenience constructor with no meaning-vector yet (the gateway fills it on write). */
    public MemoryEntry(Instant timestamp, ConversationTopic topic, MemorySource source, String content, MemoryImportance importance) {
        this(timestamp, topic, source, content, importance, null);
    }

    /** Convenience constructor defaulting to {@link MemoryImportance#NORMAL} - the level when none is assigned. */
    public MemoryEntry(Instant timestamp, ConversationTopic topic, MemorySource source, String content) {
        this(timestamp, topic, source, content, MemoryImportance.NORMAL);
    }

    /** Returns a copy carrying the given meaning-vector; used by the gateway to attach the embedding on write. */
    public MemoryEntry withEmbedding(float[] embedding) {
        return new MemoryEntry(timestamp, topic, source, content, importance, embedding);
    }

    /** Returns a copy stamped with the given time; used to refresh a fact's freshness when a re-statement is merged in. */
    public MemoryEntry withTimestamp(Instant timestamp) {
        return new MemoryEntry(timestamp, topic, source, content, importance, embedding);
    }

    // Equality deliberately ignores the embedding: it is data derived from content, not part of an entry's
    // identity, so two entries are equal exactly when their recorded fields match (the prior record semantics).
    // This also keeps content-based de-duplication unaffected by the added vector.
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MemoryEntry other)) {
            return false;
        }
        return Objects.equals(timestamp, other.timestamp)
                && topic == other.topic
                && source == other.source
                && Objects.equals(content, other.content)
                && importance == other.importance;
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, topic, source, content, importance);
    }
}
