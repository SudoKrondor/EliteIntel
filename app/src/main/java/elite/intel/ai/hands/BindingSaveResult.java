package elite.intel.ai.hands;

/**
 * Outcome of a single-slot keyboard binding save attempt.
 * <p>
 * The UI should treat every non-success result as non-destructive: the writer
 * either did not write, or failed while using a backup/temp-file flow.
 */
public enum BindingSaveResult {
    SAVED,
    NO_CHANGE,
    STALE_FILE,
    UNKNOWN_KEY,
    KEY_OCCUPIED,
    BINDING_NOT_FOUND,
    UNSUPPORTED_XML,
    BACKUP_FAILED,
    WRITE_FAILED
}
