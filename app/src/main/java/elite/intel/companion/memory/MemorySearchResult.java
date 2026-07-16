package elite.intel.companion.memory;

import java.util.List;

/** Character-bounded recall units plus an exact record count when no matching summary is involved. */
public record MemorySearchResult(int matchingUnits, Integer exactRecordCount, List<String> items) {

    public MemorySearchResult {
        if (matchingUnits < 0) {
            throw new IllegalArgumentException("matchingUnits must not be negative");
        }
        if (exactRecordCount != null && exactRecordCount != matchingUnits) {
            throw new IllegalArgumentException("An exact record count must equal the matching granular units");
        }
        items = List.copyOf(items);
        if (items.size() > matchingUnits) {
            throw new IllegalArgumentException("items cannot exceed matchingUnits");
        }
    }

    /** Empty search result. */
    public static MemorySearchResult empty() {
        return new MemorySearchResult(0, 0, List.of());
    }

    /** Whether matching records were omitted from the bounded item list. */
    public boolean truncated() {
        return items.size() < matchingUnits;
    }
}
