package elite.intel.ai.brain.vega.model;

/**
 * Category of an {@code IntelAction} game tool. {@code IntelActionAccessPolicy} maps a thought source
 * to the set of allowed categories; {@code Reducer} then selects concrete tools only from allowed
 * categories.
 */
public enum IntelActionCategory {
    /** Read-only data lookups. Allowed for both COMMANDER and EVENT thoughts. */
    QUERY,
    /** Built-in game actions (produce game input). COMMANDER only. */
    ACTION,
    /** User-defined macros. COMMANDER only. */
    MACRO
}
