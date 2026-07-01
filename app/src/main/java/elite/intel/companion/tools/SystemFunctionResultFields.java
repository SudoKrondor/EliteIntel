package elite.intel.companion.tools;

/**
 * JSON field names of the result objects returned by companion system-function {@code handle}s (and the
 * dispatch status the {@code ExecutionGateway} builds for side-effect tools). Single source of truth for
 * these keys so a handle that writes a field and a test/reader that reads it cannot drift.
 * <p>
 * These are companion tool-result fields only; legacy {@code IntelAction} (command/query) result formats
 * do not use them.
 */
public final class SystemFunctionResultFields {

    /** Dispatch/outcome status of a side-effecting tool (also reused by the execution gateway). */
    public static final String STATUS = "status";
    /** Array of returned entries (recall, find_action). */
    public static final String ITEMS = "items";
    /** A returned item's tool/action name (find_action). */
    public static final String NAME = "name";
    /** A returned item's description (find_action). */
    public static final String DESCRIPTION = "description";
    /** Error marker when a call could not be honored (recall). */
    public static final String ERROR = "error";
    /** Memory scope echoed back (recall). */
    public static final String SCOPE = "scope";
    /** Topic echoed back (classify_turn). */
    public static final String TOPIC = "topic";
    /** Importance level echoed back (classify_turn). */
    public static final String IMPORTANCE = "importance";
    /** Whether the commander's current phrase is a question, echoed back (classify_turn). */
    public static final String IS_QUESTION = "is_question";

    private SystemFunctionResultFields() {
    }
}
