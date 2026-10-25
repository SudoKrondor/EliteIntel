package elite.intel.gameapi.journalimport;

/**
 * The commander pressed Cancel. Whatever was filed before then stays filed - every write is one the import could
 * make again without harm.
 */
public final class ImportCancelledException extends RuntimeException {
    public ImportCancelledException() {
        super("Journal import cancelled");
    }
}
