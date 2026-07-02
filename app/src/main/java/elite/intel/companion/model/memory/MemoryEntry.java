package elite.intel.companion.model.memory;

import elite.intel.companion.model.ConversationTopic;

import java.time.Instant;
import java.util.Objects;

/**
 * One entry in the companion's experience timeline. Ordered by the actual write time at the
 * {@code MemoryGateway}, not by thought birth time.
 *
 * @param timestamp     actual write time
 * @param topic         topic this entry belongs to
 * @param source        where the information came from
 * @param content       the recorded text, verbatim - the ground truth (a MAX "remember word-for-word" fact is
 *                      kept only here)
 * @param importance    how important this entry is to the companion's memory (drives retention and consolidation)
 * @param embedding     cached meaning-vector, computed once by the gateway on write over {@link #embeddingText()}
 *                      for semantic recall; {@code null} when semantic search is unavailable or not yet computed.
 *                      Read-only: the embedder returns a fresh array per call, so callers must not mutate it.
 * @param canonicalFact optional clean one-line restatement of a durable fact (from {@code classify_turn}), used
 *                      only as the searchable/candidate text; {@code null}/blank for chatter, questions, and MAX
 *                      facts. Never the ground truth - {@link #content} is.
 */
public record MemoryEntry(
        Instant timestamp,
        ConversationTopic topic,
        MemorySource source,
        String content,
        MemoryImportance importance,
        float[] embedding,
        String canonicalFact
) {
    /** Convenience constructor with no meaning-vector or canonical fact yet (the gateway fills the vector on write). */
    public MemoryEntry(Instant timestamp, ConversationTopic topic, MemorySource source, String content, MemoryImportance importance, float[] embedding) {
        this(timestamp, topic, source, content, importance, embedding, null);
    }

    /** Convenience constructor with no meaning-vector and no canonical fact. */
    public MemoryEntry(Instant timestamp, ConversationTopic topic, MemorySource source, String content, MemoryImportance importance) {
        this(timestamp, topic, source, content, importance, null, null);
    }

    /** Convenience constructor defaulting to {@link MemoryImportance#NORMAL} - the level when none is assigned. */
    public MemoryEntry(Instant timestamp, ConversationTopic topic, MemorySource source, String content) {
        this(timestamp, topic, source, content, MemoryImportance.NORMAL, null, null);
    }

    /** Returns a copy carrying the given meaning-vector; used by the gateway to attach the embedding on write. */
    public MemoryEntry withEmbedding(float[] embedding) {
        return new MemoryEntry(timestamp, topic, source, content, importance, embedding, canonicalFact);
    }

    /** Returns a copy stamped with the given time; used to refresh a fact's freshness when a re-statement is merged in. */
    public MemoryEntry withTimestamp(Instant timestamp) {
        return new MemoryEntry(timestamp, topic, source, content, importance, embedding, canonicalFact);
    }

    /**
     * The text used for semantic recall and as the candidate/display line: the clean {@link #canonicalFact} when
     * present, otherwise the verbatim {@link #content}. Ground-truth recall still reads {@link #content}.
     */
    public String embeddingText() {
        return canonicalFact != null && !canonicalFact.isBlank() ? canonicalFact : content;
    }

    // Equality deliberately ignores the embedding and the canonicalFact: both are data derived from the entry
    // (a vector, a cleaned restatement), not part of its identity, so two entries are equal exactly when their
    // recorded fields match (the prior record semantics). This keeps content-based de-duplication unaffected.
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
