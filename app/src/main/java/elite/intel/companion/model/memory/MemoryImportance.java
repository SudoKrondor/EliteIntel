package elite.intel.companion.model.memory;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * How important a {@link MemoryEntry} is to the companion's own memory. The consciousness assigns it: the LLM
 * rates a turn through the {@code set_importance} system function, and an entry defaults to {@link #NORMAL}
 * when no level is assigned.
 * <p>
 * It drives mid-term retention (the lowest level is evicted first) and long-term consolidation: {@link #LOW}
 * is dropped, {@link #NORMAL} is summarized, {@link #HIGH} is condensed but kept, and {@link #MAX} is carried
 * verbatim into long-term memory and never compressed - reserved for an explicit commander instruction to
 * remember.
 * <p>
 * Declared low-to-high so the natural enum order is the importance order (usable for comparisons).
 */
public enum MemoryImportance {
    LOW,
    NORMAL,
    HIGH,
    MAX;

    /** The level ids (lowercase, in importance order) accepted by {@link #fromId} - the set_importance enum. */
    public static List<String> ids() {
        return Arrays.stream(values()).map(v -> v.name().toLowerCase(Locale.ROOT)).toList();
    }

    /** Parses a level id (case-insensitive, e.g. from {@code set_importance}); null when blank or unknown. */
    public static MemoryImportance fromId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        try {
            return valueOf(id.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }
}
