package elite.intel.companion.memory;

import elite.intel.companion.CompanionConfig;
import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.ConversationTopic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Topic-keyed archive of entries evicted from short-term memory. Bounded by max entries per topic;
 * overflow is evicted into the consolidation buffer. Never returned whole.
 * Package-private internal of {@link SessionMemoryGateway}.
 */
class MidTermTopicMemory {

    // EnumMap keeps topics in natural enum order, so the prompt topic index is stable.
    private final Map<ConversationTopic, List<MemoryEntry>> byTopic = new EnumMap<>(ConversationTopic.class);

    /** Stores an evicted entry under its topic. */
    void add(MemoryEntry entry) {
        byTopic.computeIfAbsent(entry.topic(), t -> new ArrayList<>()).add(entry);
    }

    /**
     * Topic-scoped recall: the most recent entries of the topic (up to {@code limit}), optionally filtered
     * by a case-insensitive plain-text {@code query} on content; returned oldest-to-newest. Empty when the
     * topic holds nothing or {@code limit <= 0}.
     */
    List<MemoryEntry> recall(ConversationTopic topic, String query, int limit) {
        List<MemoryEntry> entries = byTopic.get(topic);
        if (entries == null || entries.isEmpty() || limit <= 0) {
            return List.of();
        }
        String filter = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        List<MemoryEntry> matched = new ArrayList<>();
        // Walk newest-first to take the latest N, then return chronological order.
        for (int i = entries.size() - 1; i >= 0 && matched.size() < limit; i--) {
            MemoryEntry entry = entries.get(i);
            if (filter.isEmpty() || entry.content().toLowerCase(Locale.ROOT).contains(filter)) {
                matched.add(entry);
            }
        }
        Collections.reverse(matched);
        return matched;
    }

    /** Every entry across all topics; the gateway's unified search does the query matching. */
    List<MemoryEntry> allEntries() {
        List<MemoryEntry> all = new ArrayList<>();
        for (List<MemoryEntry> entries : byTopic.values()) {
            all.addAll(entries);
        }
        return all;
    }

    /** Topics that currently hold entries (for the prompt topic-memory index). */
    List<ConversationTopic> topicsWithMemory() {
        List<ConversationTopic> topics = new ArrayList<>();
        for (Map.Entry<ConversationTopic, List<MemoryEntry>> e : byTopic.entrySet()) {
            if (!e.getValue().isEmpty()) {
                topics.add(e.getKey());
            }
        }
        return topics;
    }

    /** Evicts per-topic overflow (oldest beyond the per-topic cap) and returns it for consolidation. */
    List<MemoryEntry> evictOverflow() {
        List<MemoryEntry> evicted = new ArrayList<>();
        for (List<MemoryEntry> entries : byTopic.values()) {
            while (entries.size() > CompanionConfig.midTermMemorySizePerTopic()) {
                evicted.add(entries.remove(0)); // oldest of the topic first
            }
        }
        return evicted;
    }
}
