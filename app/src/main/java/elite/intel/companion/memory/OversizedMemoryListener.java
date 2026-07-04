package elite.intel.companion.memory;

import elite.intel.companion.model.memory.MemoryEntry;

/**
 * Receives a memory entry the gateway declined to store because its content exceeds
 * {@code CompanionConfig.memoryEntryMaxChars()}. The implementation compresses it off the write path (a silent
 * LLM round) and re-writes a short gist, so an over-long line never bloats the prompt. Wired at subsystem
 * start; defaults to a no-op (the over-long entry is simply dropped) until then.
 * <p>
 * Storage of an over-long entry is therefore <em>eventual</em>, not synchronous: the gist lands only once
 * compression completes, and the entry is dropped entirely if compression fails, stays oversized, or the
 * subsystem stops before the work runs. This is acceptable because the content was prompt-bloat, but a caller
 * must not assume a long entry is readable immediately after {@code write}.
 */
public interface OversizedMemoryListener {

    /** Hands off an over-long entry for compression-then-rewrite; must not block the caller's write. */
    void onOversized(MemoryEntry entry);
}
