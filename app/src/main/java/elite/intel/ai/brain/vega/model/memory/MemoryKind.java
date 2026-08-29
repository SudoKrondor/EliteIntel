package elite.intel.ai.brain.vega.model.memory;

/**
 * The shape of a completed companion-memory record. Each kind has one fixed shape; the model does not classify it.
 * <p>
 * Both surviving kinds are replayed as conversation turns in the next prompt. A gameplay EVENT kind existed here
 * until the retained history was removed: the prompt never replayed one, so every event record spent a slot and a
 * share of the window's token budget evicting an exchange that would have been replayed.
 */
public enum MemoryKind {
    /** A completed commander/companion conversational exchange. */
    DIALOGUE,
    /** A completed commander query and its spoken answer. */
    QUERY
}
