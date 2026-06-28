package elite.intel.companion.memory;

/**
 * Single source of truth for the companion's internal/derived session-memory limits, so every area's
 * bounds live in one place instead of being scattered across the memory classes. Values are provisional
 * (see COMPANION_ARCHITECTURE.md §7.2). The user-tunable area sizes (short-term entries, mid-term entries
 * per topic) live in {@link elite.intel.companion.CompanionConfig}; the bounds here pair with them.
 */
public final class CompanionMemoryLimits {

    // --- short-term (hot timeline) ---
    // Size (max entries) is a user setting: CompanionConfig.shortTermMemorySize().
    /** Soft token ceiling for the timeline block; a backstop against unusually long entries. */
    public static final int SHORT_TERM_TOKEN_BUDGET = 1200;
    /** Per-entry token overhead for the framing each entry adds when rendered into the prompt. */
    public static final int SHORT_TERM_ENTRY_FRAMING_OVERHEAD_TOKENS = 4;

    // --- mid-term (topic archive) ---
    // Size (max entries per topic) is a user setting: CompanionConfig.midTermMemorySizePerTopic().
    /** Max entries returned by a single topic-memory recall. */
    public static final int MID_TERM_RECALL_LIMIT = 10;

    // --- long-term consolidation ---
    /** Buffered mid-term entries that trigger a compression pass. */
    public static final int CONSOLIDATION_BUFFER_THRESHOLD = 20;
    /** Max characters for the long-term summary; a longer compression output is treated as a failure. */
    public static final int SUMMARY_MAX_CHARS = 1500;

    private CompanionMemoryLimits() {
    }
}
