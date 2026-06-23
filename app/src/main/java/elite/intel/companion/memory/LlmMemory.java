package elite.intel.companion.memory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

/**
 * Small cyclic LLM scratch memory. Not split by topic, not consolidated. Fixed limits (max 15
 * entries, max 50 chars each); a new 16th entry evicts the oldest; exact duplicates are not added.
 * <p>
 * The class is public only so its agreed limit constants are a single cross-package source of truth
 * (e.g. for the {@code remember} tool description); instantiation and behavior stay internal to
 * {@link SessionMemoryGateway} via the package-private constructor and methods.
 */
public final class LlmMemory {

    /** Maximum number of items. */
    public static final int MAX_ENTRIES = 15;
    /** Maximum characters per item (code truncates longer content). */
    public static final int MAX_CONTENT_LENGTH = 50;

    // Oldest-to-newest; a new entry past MAX_ENTRIES evicts the oldest (cyclic).
    private final Deque<String> items = new ArrayDeque<>();

    /** Package-private: only the memory package constructs this internal store. */
    LlmMemory() {
    }

    /** Returns the whole list, oldest-to-newest (small enough to return entirely). */
    List<String> all() {
        return List.copyOf(items);
    }

    /**
     * Adds a fact: blank input is ignored, content longer than {@link #MAX_CONTENT_LENGTH} is truncated,
     * an exact duplicate (trim + collapsed spaces + case-insensitive) is not re-added, and a new entry past
     * {@link #MAX_ENTRIES} evicts the oldest.
     */
    void add(String content) {
        if (content == null) {
            return;
        }
        String stored = content.strip();
        if (stored.isEmpty()) {
            return;
        }
        if (stored.length() > MAX_CONTENT_LENGTH) {
            stored = stored.substring(0, MAX_CONTENT_LENGTH);
        }
        String key = normalize(stored);
        for (String existing : items) {
            if (normalize(existing).equals(key)) {
                return; // exact duplicate
            }
        }
        if (items.size() >= MAX_ENTRIES) {
            items.removeFirst();
        }
        items.addLast(stored);
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
