package elite.intel.companion.model.memory;

import elite.intel.companion.model.ConversationTopic;

import java.time.Instant;

/**
 * One entry in the companion's experience timeline. Ordered by the actual write time at the
 * {@code MemoryGateway}, not by thought birth time.
 *
 * @param timestamp  actual write time
 * @param topic      topic this entry belongs to
 * @param source     where the information came from
 * @param content    the recorded text
 * @param importance how important this entry is to the companion's memory (drives retention and consolidation)
 */
public record MemoryEntry(
        Instant timestamp,
        ConversationTopic topic,
        MemorySource source,
        String content,
        MemoryImportance importance
) {
    /** Convenience constructor defaulting to {@link MemoryImportance#NORMAL} - the level when none is assigned. */
    public MemoryEntry(Instant timestamp, ConversationTopic topic, MemorySource source, String content) {
        this(timestamp, topic, source, content, MemoryImportance.NORMAL);
    }
}
