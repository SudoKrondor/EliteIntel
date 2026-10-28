package elite.intel.bindforge.io;

/**
 * Outcome of one binding edit.
 * <p>
 * Every result but {@link #SAVED} leaves the file as it was. {@link #WRITE_FAILED} is the edits' own failure - the
 * working copy could not be read, so nothing was changed (Storage S5: the writer reads and writes no file, so the
 * stale-file and backup results went with the file it wrote).
 */
public enum BindingSaveResult {
    SAVED,
    NO_CHANGE,
    UNKNOWN_KEY,
    KEY_OCCUPIED,
    BINDING_NOT_FOUND,
    UNSUPPORTED_XML,
    WRITE_FAILED
}
