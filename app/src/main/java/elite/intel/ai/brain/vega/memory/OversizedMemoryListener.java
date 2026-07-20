package elite.intel.ai.brain.vega.memory;

import elite.intel.ai.brain.vega.model.memory.MemoryRecord;

/**
 * Accepts a completed record whose ordinary entry exceeds the prompt-visible memory limit. The listener owns
 * asynchronous compression of the whole record, so a DIALOGUE or QUERY pair can never become partially visible.
 */
@FunctionalInterface
public interface OversizedMemoryListener {

    /**
     * Attempts to accept the whole record for eventual compression and atomic re-write.
     *
     * @return {@code true} when the listener owns the record; {@code false} asks the gateway to store its bounded
     *         fallback synchronously
     */
    boolean onOversized(MemoryRecord record);
}
