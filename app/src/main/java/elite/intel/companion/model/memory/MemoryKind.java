package elite.intel.companion.model.memory;

/**
 * The retention contract of a completed companion-memory record.
 * Each kind has one fixed shape and one fixed eviction policy; the model does not classify it.
 */
public enum MemoryKind {
    /** A completed commander/companion conversational exchange. */
    DIALOGUE,
    /** A completed commander query and its spoken answer. */
    QUERY,
    /** A final fact produced from a gameplay event. */
    EVENT,
    /** Commander-provided text explicitly saved verbatim through the {@code remember(text)} command. */
    SAVED_TEXT;

    /** Whether records of this kind move from recent memory into retained memory. */
    public boolean movesToMidTerm() {
        return this == DIALOGUE || this == EVENT;
    }

    /** Whether retained records of this kind are eligible for long-term summarization. */
    public boolean hasLongTermSummary() {
        return movesToMidTerm();
    }
}
