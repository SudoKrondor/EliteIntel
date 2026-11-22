package elite.intel.gameapi.journalimport;

import elite.intel.gameapi.JournalFiles;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Every journal goes to the commander who flew it - including the ones that do not say so outright.
 */
class JournalOwnersTest {

    @Test
    void aJournalNamingAnFidBelongsToIt() {
        assertEquals(List.of(Optional.of("F1")),
                JournalOwners.resolve(List.of(journal("Journal.2024-01-01T100000.01.log")),
                        List.of(new JournalHeaderInfo("English/UK", "F1", "Jameson"))));
    }

    @Test
    void aJournalFromBeforeFidsIsMatchedByNameWhenOnlyOneCommanderEverWentByIt() {
        List<FoundJournal> journals = List.of(
                journal("Journal.170101100000.01.log"),
                journal("Journal.170102100000.01.log"),
                journal("Journal.2024-01-01T100000.01.log"),
                journal("Journal.2024-01-02T100000.01.log"),
                journal("Journal.2024-01-03T100000.01.log"));
        List<JournalHeaderInfo> headers = List.of(
                new JournalHeaderInfo("English/UK", null, "Jameson"),
                new JournalHeaderInfo("English/UK", null, "Common Name"),
                new JournalHeaderInfo("English/UK", "F1", "Jameson"),
                new JournalHeaderInfo("English/UK", "F2", "Common Name"),
                new JournalHeaderInfo("English/UK", "F3", "Common Name"));

        assertEquals(List.of(Optional.of("F1"), Optional.empty(), Optional.of("F1"), Optional.of("F2"), Optional.of("F3")),
                JournalOwners.resolve(journals, headers),
                "two commanders went by Common Name, so an old journal under that name could be either's");
    }

    @Test
    void aContinuationPartBelongsToThePartBeforeIt() {
        List<FoundJournal> journals = List.of(
                journal("Journal.2024-01-01T100000.01.log"),
                journal("Journal.2024-01-01T160000.02.log"),
                journal("Journal.2024-01-02T100000.02.log"));
        List<JournalHeaderInfo> headers = List.of(
                new JournalHeaderInfo("English/UK", "F1", "Jameson"),
                new JournalHeaderInfo("English/UK", null, null),
                new JournalHeaderInfo("English/UK", null, null));

        assertEquals(List.of(Optional.of("F1"), Optional.of("F1"), Optional.empty()),
                JournalOwners.resolve(journals, headers),
                "the third is a second part whose first part was not found, so whose it is cannot be told");
    }

    private static FoundJournal journal(String name) {
        return new FoundJournal(name, JournalFiles.startStamp(name).orElseThrow(), 100, () -> null, name);
    }
}
