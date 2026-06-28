package elite.intel.companion.model;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

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
    CHATTY;

    /** The mode ids (lowercase) accepted by {@code change_verbosity} - the verbosity enum. */
    public static List<String> ids() {
        return Arrays.stream(values()).map(v -> v.name().toLowerCase(Locale.ROOT)).toList();
    }
}
