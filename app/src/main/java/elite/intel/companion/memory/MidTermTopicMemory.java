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

    /**
     * Removes the given entry (by identity) from its topic if present. Used by the gateway's semantic
     * de-duplication when a re-stated fact supersedes this copy. Returns whether it removed.
     */
    boolean remove(MemoryEntry entry) {
        List<MemoryEntry> entries = byTopic.get(entry.topic());
        if (entries != null) {
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i) == entry) {
                    entries.remove(i);
                    return true;
                }
            }
        }
        return false;
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

    /**
     * Evicts per-topic overflow and returns it for consolidation. Importance-aware: the least important entry
     * leaves first (oldest among equal importance), so HIGH/MAX facts outlast routine ones - MAX leaves last
     * and is carried verbatim into long-term by the consolidator.
     */
    List<MemoryEntry> evictOverflow() {
        List<MemoryEntry> evicted = new ArrayList<>();
        for (List<MemoryEntry> entries : byTopic.values()) {
            while (entries.size() > CompanionConfig.midTermMemorySizePerTopic()) {
                evicted.add(entries.remove(lowestImportanceOldestIndex(entries)));
            }
        }
        return evicted;
    }

    /** Index of the least-important entry, earliest among equal importance (the list is oldest-first). */
    private static int lowestImportanceOldestIndex(List<MemoryEntry> entries) {
        int victim = 0;
        for (int i = 1; i < entries.size(); i++) {
            if (entries.get(i).importance().compareTo(entries.get(victim).importance()) < 0) {
                victim = i;
            }
        }
        return victim;
    }
}
