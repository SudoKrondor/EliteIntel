package elite.intel.companion.model.memory;

import java.time.Instant;
import java.util.Objects;

/** One ranked entry selected from a memory record. */
public record MemorySearchMatch(MemoryKind kind, Instant timestamp, MemoryEntry entry) {

    public MemorySearchMatch {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(timestamp, "timestamp");
        Objects.requireNonNull(entry, "entry");
    }
}
