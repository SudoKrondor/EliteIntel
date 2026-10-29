package elite.intel.bindforge.io;

/**
 * Outcome of one binding edit.
 * <p>
 * Every result but {@link #SAVED} leaves the file as it was. The edits' own failures are exceptions, not results
 * (Storage S5: the writer reads and writes no file, so the stale-file, backup and write-failed results went with the
 * file it wrote - the last in S6R).
 */
public enum BindingSaveResult {
    SAVED,
    NO_CHANGE,
    UNKNOWN_KEY,
    KEY_OCCUPIED,
    BINDING_NOT_FOUND,
    UNSUPPORTED_XML
}
