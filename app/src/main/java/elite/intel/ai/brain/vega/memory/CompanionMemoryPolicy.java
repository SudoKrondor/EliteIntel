package elite.intel.ai.brain.vega.memory;

import elite.intel.ai.brain.vega.model.memory.MemoryKind;

/** Single owner of session-memory retention, recall, and consolidation limits. */
public final class CompanionMemoryPolicy {

    private static final int RECENT_RECORD_LIMIT = 15;
    private static final int MID_TERM_DIALOGUE_RECORD_LIMIT = 60;
    private static final int MID_TERM_EVENT_RECORD_LIMIT = 120;
    private static final int ENTRY_MAX_CHARS = 200;
    private static final int SAVED_TEXT_MAX_CHARS = 1000;
    private static final int SAVED_TEXT_RECORD_LIMIT = 500;
    private static final int EVENT_DATA_MAX_CHARS = 4000;
    private static final int EVENT_INSTRUCTIONS_MAX_CHARS = 1000;
    private static final int SEARCH_ITEM_MAX_CHARS = 1000;
    private static final int SEARCH_RESULT_MAX_CHARS = 6000;
    private static final int RECENT_TOKEN_BUDGET = 1200;
    private static final int RECORD_FRAMING_TOKENS = 2;
    private static final int ENTRY_FRAMING_TOKENS = 4;
    private static final double SEMANTIC_RECALL_FLOOR = 0.85;
    private static final double SEMANTIC_DEDUP_FLOOR = 0.95;
    private static final int CONSOLIDATION_BATCH_SIZE = 10;
    private static final int SUMMARY_MAX_CHARS = 1500;

    private CompanionMemoryPolicy() {
    }

    /** Maximum records replayed as recent conversation history. */
    public static int recentRecordLimit() {
        return RECENT_RECORD_LIMIT;
    }

    /** Maximum retained records before this kind starts consolidation. */
    public static int midTermRecordLimit(MemoryKind kind) {
        return switch (kind) {
            case DIALOGUE -> MID_TERM_DIALOGUE_RECORD_LIMIT;
            case EVENT -> MID_TERM_EVENT_RECORD_LIMIT;
            case QUERY, SAVED_TEXT -> 0;
        };
    }

    /** Maximum characters accepted for an automatically created memory entry. */
    public static int entryMaxChars() {
        return ENTRY_MAX_CHARS;
    }

    /** Maximum characters accepted in one explicitly saved text. */
    public static int savedTextMaxChars() {
        return SAVED_TEXT_MAX_CHARS;
    }

    /** Maximum explicitly saved records retained in one session. */
    static int savedTextRecordLimit() {
        return SAVED_TEXT_RECORD_LIMIT;
    }

    /** Maximum transient event-data characters sent to the narration model. */
    public static int eventDataMaxChars() {
        return EVENT_DATA_MAX_CHARS;
    }

    /** Maximum transient narration-instruction characters sent to the model. */
    public static int eventInstructionsMaxChars() {
        return EVENT_INSTRUCTIONS_MAX_CHARS;
    }

    /** Maximum rendered characters for one explicit memory-search item. */
    static int searchItemMaxChars() {
        return SEARCH_ITEM_MAX_CHARS;
    }

    /** Maximum combined item characters returned by one explicit memory search. */
    static int searchResultMaxChars() {
        return SEARCH_RESULT_MAX_CHARS;
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

    /** Minimum similarity for ambient semantic recall. */
    public static double semanticRecallFloor() {
        return SEMANTIC_RECALL_FLOOR;
    }

    /** Minimum similarity used to remove duplicate ambient facts. */
    public static double semanticDedupFloor() {
        return SEMANTIC_DEDUP_FLOOR;
    }

    /** Number of overflow records summarized in one consolidation request. */
    public static int consolidationBatchSize() {
        return CONSOLIDATION_BATCH_SIZE;
    }

    /** Maximum accepted size of a generated long-term summary. */
    public static int summaryMaxChars() {
        return SUMMARY_MAX_CHARS;
    }
}
