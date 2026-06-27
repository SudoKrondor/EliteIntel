package elite.intel.companion.memory;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

/**
 * Small cyclic LLM scratch memory. Not split by topic, not consolidated. Bounds come from
 * {@link CompanionMemoryLimits} (max entries, max chars each); a new entry past the cap evicts the
 * oldest; exact duplicates are not added. Each item carries its write time so a unified recall can sort
 * conscious facts together with mid-term entries. Package-private internal of {@link SessionMemoryGateway}.
 */
final class LlmMemory {

    /** One remembered fact with the time it was stored, for unified time-ordered recall. */
    record Item(Instant at, String content) {}

    // Oldest-to-newest; a new entry past the cap evicts the oldest (cyclic).
    private final Deque<Item> items = new ArrayDeque<>();

    /** Package-private: only the memory package constructs this internal store. */
    LlmMemory() {
    }

    /** Returns the content of every item, oldest-to-newest (small enough to return entirely). */
    List<String> all() {
        List<String> contents = new ArrayList<>(items.size());
        for (Item item : items) {
            contents.add(item.content());
        }
        return contents;
    }

    /** Every item (content + write time), oldest-to-newest; the gateway's unified search does the matching. */
    List<Item> allItems() {
        return List.copyOf(items);
    }

    /**
     * Adds a fact: blank input is ignored, content longer than the per-item cap is truncated, an exact
     * duplicate (trim + collapsed spaces + case-insensitive) is not re-added, and a new entry past the
     * entry cap evicts the oldest (see {@link CompanionMemoryLimits}).
     */
    void add(String content) {
        if (content == null) {
            return;
        }
        String stored = content.strip();
        if (stored.isEmpty()) {
            return;
        }
        if (stored.length() > CompanionMemoryLimits.LLM_MEMORY_MAX_CONTENT_LENGTH) {
            stored = stored.substring(0, CompanionMemoryLimits.LLM_MEMORY_MAX_CONTENT_LENGTH);
        }
        String key = normalize(stored);
        for (Item existing : items) {
            if (normalize(existing.content()).equals(key)) {
                return; // exact duplicate
            }
        }
        if (items.size() >= CompanionMemoryLimits.LLM_MEMORY_MAX_ENTRIES) {
            items.removeFirst();
        }
        items.addLast(new Item(Instant.now(), stored));
    }

    /** Current item count. */
    int size() {
        return items.size();
    }

    /** Dedup key: trimmed, internal whitespace collapsed, lower-cased. */
    private static String normalize(String s) {
        return s.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
