package elite.intel.companion.model;

/**
 * Talkativeness slot for the companion. Controls how freely EVENT thoughts may comment.
 * Changed only by the COMMANDER {@code change_verbosity} tool.
 */
public enum Verbosity {
    /** Speaks only on urgent events / direct commander interaction. */
    QUIET,
    /** Default: comments on noteworthy events. */
    NORMAL,
    /** Comments freely. */
    CHATTY
}
