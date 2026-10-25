package elite.intel.gameapi.journalimport;

import java.util.List;

/**
 * What one journal import did, for the dialog's summary.
 *
 * @param journalsRead          journals read through, damaged ones included
 * @param commanders            the commanders whose journals were filed, by name where a journal gave one
 * @param unownedJournals       journals no commander could be told for, which taught only shared knowledge
 * @param notJournals           files and archive entries that were not journals
 * @param damaged               journals or archives that broke off part way; what came before the break was kept
 * @param unreadable            archives or entries that could not be opened (password protected, say)
 * @param unsupported           RAR archives
 * @param duplicates            journals found more than once and read once
 * @param huntingGroundsLearned resource-site systems the app did not know before
 * @param contractsLearned      pirate massacre contracts the app did not know before, all commanders together
 * @param bioSamples            species sampled out, as the journals record them
 * @param bodiesScanned         body scans read
 */
public record ImportReport(int journalsRead,
                           List<String> commanders,
                           int unownedJournals,
                           int notJournals,
                           int damaged,
                           int unreadable,
                           int unsupported,
                           int duplicates,
                           int huntingGroundsLearned,
                           int contractsLearned,
                           int bioSamples,
                           int bodiesScanned) {
}
