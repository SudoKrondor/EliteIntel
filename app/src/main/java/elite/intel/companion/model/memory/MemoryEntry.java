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
 */
public record MemoryEntry(
        Instant timestamp,
        ConversationTopic topic,
        MemorySource source,
        String content
) {}
