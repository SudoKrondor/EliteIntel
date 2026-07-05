package elite.intel.companion.memory;

import elite.intel.companion.model.ConversationTopic;
import elite.intel.companion.model.memory.MemoryEntry;

import java.util.List;
import java.util.Map;

/**
 * Immutable, read-only snapshot of every companion memory area at one instant, produced by
 * {@link MemoryGateway#snapshot()} for diagnostics/export. It carries the same {@link MemoryEntry entries}
 * the gateway holds internally (the gateway never leaks its live stores), so a caller can render or serialize
 * the whole memory without reaching past the single door.
 *
 * @param shortTerm       the hot short-term timeline, oldest-to-newest
 * @param midTermByTopic  mid-term entries grouped by their {@link ConversationTopic}, in natural enum order
 * @param longTermSummary the single session-wide long-term summary (empty string when none consolidated yet)
 * @param longTermPinned  the pinned MAX-importance facts, oldest-to-newest
 */
public record MemorySnapshot(
        List<MemoryEntry> shortTerm,
        Map<ConversationTopic, List<MemoryEntry>> midTermByTopic,
        String longTermSummary,
        List<MemoryEntry> longTermPinned
) {
}
