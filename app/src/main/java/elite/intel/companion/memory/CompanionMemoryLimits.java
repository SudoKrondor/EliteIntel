package elite.intel.companion.memory;

/**
 * Single source of truth for the companion session-memory limits, so every area's bounds live in one
 * place instead of being scattered across the memory classes. Values are provisional (see
 * COMPANION_ARCHITECTURE.md §7.2) and may later be promoted to user/runtime settings.
 */
public final class CompanionMemoryLimits {

    // --- short-term (hot timeline) ---
    /** Max entries kept in the hot timeline; the primary eviction control. */
    public static final int SHORT_TERM_MAX_ENTRIES = 20;
    /** Soft token ceiling for the timeline block; a backstop against unusually long entries. */
    public static final int SHORT_TERM_TOKEN_BUDGET = 1200;
    /** Per-entry token overhead for the framing each entry adds when rendered into the prompt. */
    public static final int SHORT_TERM_ENTRY_FRAMING_OVERHEAD_TOKENS = 4;

    // --- mid-term (topic archive) ---
    /** Max entries kept per topic; older ones overflow into the consolidation buffer. */
    public static final int MID_TERM_MAX_ENTRIES_PER_TOPIC = 30;

    // --- llm_memory (cyclic scratch) ---
    /** Max items in the cyclic llm_memory. */
    public static final int LLM_MEMORY_MAX_ENTRIES = 15;
    /** Max characters per llm_memory item (longer content is truncated). */
    public static final int LLM_MEMORY_MAX_CONTENT_LENGTH = 50;

    private CompanionMemoryLimits() {
    }
}
