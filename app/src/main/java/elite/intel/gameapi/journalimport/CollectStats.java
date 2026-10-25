package elite.intel.gameapi.journalimport;

/**
 * What the collector set aside while finding journals. Every count is a file or archive entry, not a line.
 */
final class CollectStats {

    /**
     * Files and entries that are not journals: screenshots, Status.json, notes, anything at all.
     */
    int notJournals;
    /**
     * Journals or archives that broke off part way. What was read before the break is kept.
     */
    int damaged;
    /**
     * Archives, or entries in them, that could not be opened at all: password protected, or not what their
     * signature promised.
     */
    int unreadable;
    /**
     * RAR archives, which there is no free decoder for.
     */
    int unsupported;
    /**
     * The same journal found more than once - in a folder and in a backup of that folder, say.
     */
    int duplicates;
}
