package elite.intel.ai.brain.vega.memory;

/**
 * Single owner of the session-memory window's limits and the transient narration bounds.
 */
public final class CompanionMemoryPolicy {

    private static final int RECENT_RECORD_LIMIT = 15;
    private static final int ENTRY_MAX_CHARS = 200;
    private static final int EVENT_DATA_MAX_CHARS = 4000;
    private static final int EVENT_INSTRUCTIONS_MAX_CHARS = 1000;
    private static final int RECENT_TOKEN_BUDGET = 1200;
    private static final int RECORD_FRAMING_TOKENS = 2;
    private static final int ENTRY_FRAMING_TOKENS = 4;

    private CompanionMemoryPolicy() {
    }

    /** Maximum records replayed as recent conversation history. */
    public static int recentRecordLimit() {
        return RECENT_RECORD_LIMIT;
    }

    /** Maximum characters accepted for an automatically created memory entry. */
    public static int entryMaxChars() {
        return ENTRY_MAX_CHARS;
    }

    /** Maximum transient event-data characters sent to the narration model. */
    public static int eventDataMaxChars() {
        return EVENT_DATA_MAX_CHARS;
    }

    /** Maximum transient narration-instruction characters sent to the model. */
    public static int eventInstructionsMaxChars() {
        return EVENT_INSTRUCTIONS_MAX_CHARS;
    }

    /** Approximate token budget for recent conversation history. */
    public static int recentTokenBudget() {
        return RECENT_TOKEN_BUDGET;
    }

    /** Approximate tokens added around each replayed record. */
    static int recordFramingTokens() {
        return RECORD_FRAMING_TOKENS;
    }

    /** Approximate tokens added around each replayed entry. */
    static int entryFramingTokens() {
        return ENTRY_FRAMING_TOKENS;
    }
}
