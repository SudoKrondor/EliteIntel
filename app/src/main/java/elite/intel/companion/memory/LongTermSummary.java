package elite.intel.companion.memory;

import elite.intel.companion.model.memory.MemoryEntry;
import elite.intel.companion.model.memory.MemorySource;

import java.time.Instant;
import java.util.Objects;

/** Searchable long-term summary dated by its newest source record, not by compression time. */
record LongTermSummary(Instant evidenceAt, MemoryEntry entry) {

    LongTermSummary {
        Objects.requireNonNull(evidenceAt, "evidenceAt");
        Objects.requireNonNull(entry, "entry");
        if (entry.source() != MemorySource.SYSTEM || entry.content().isBlank()) {
            throw new IllegalArgumentException("A long-term summary requires non-blank SYSTEM text");
        }
    }

    String text() {
        return entry.content();
    }
}
