package elite.intel.gameapi.journalimport;

/**
 * A second import was asked for while one runs. Two would read the same journals into the same files at once.
 */
public final class ImportAlreadyRunningException extends IllegalStateException {
    public ImportAlreadyRunningException() {
        super("A journal import is already running");
    }
}
