package elite.intel.gameapi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The newest journal is decided by the stamp in its name, never by the modification time Windows reports
 * stale for the file the game is still writing - see {@link JournalFiles}.
 */
class JournalFilesTest {

    @TempDir
    Path folder;

    private Path journal(String name, Instant modified) throws IOException {
        Path file = folder.resolve(name);
        Files.writeString(file, "{ \"event\":\"Fileheader\" }\n");
        Files.setLastModifiedTime(file, FileTime.from(modified));
        return file;
    }

    /**
     * The reported case: the live journal's directory entry still says the moment it was opened, while the
     * journal the game closed later carries a fresher time. The name says which is newer, and wins.
     */
    @Test
    void theLiveJournalWinsOverAnOlderOneWithAFresherModificationTime() throws IOException {
        Instant t = Instant.parse("2026-09-14T20:05:41Z");
        Path closedLater = journal("Journal.2026-09-14T195002.01.log", t.plusSeconds(3600));
        Path live = journal("Journal.2026-09-14T220541.01.log", t);

        assertEquals(live, JournalFiles.newest(folder).orElseThrow());
        assertEquals(List.of(closedLater, live), JournalFiles.listOldestFirst(folder));
    }

    @Test
    void aRolledJournalOrdersByItsPartAfterItsStamp() throws IOException {
        Instant t = Instant.parse("2026-09-14T20:05:41Z");
        Path part2 = journal("Journal.2026-09-14T220541.02.log", t);
        Path part1 = journal("Journal.2026-09-14T220541.01.log", t.plusSeconds(60));

        assertEquals(List.of(part1, part2), JournalFiles.listOldestFirst(folder));
    }

    /**
     * Before the 2022 rename the game wrote {@code Journal.YYMMDDHHMMSS.NN.log}; long-lived installs still
     * have those beside the new form, and they must sort before it rather than after by string order.
     */
    @Test
    void theOldNameFormSortsBeforeTheNewOneByDate() throws IOException {
        Instant t = Instant.parse("2026-09-14T20:05:41Z");
        Path old = journal("Journal.210914220541.01.log", t.plusSeconds(600));
        Path recent = journal("Journal.2026-09-14T220541.01.log", t);

        assertEquals(List.of(old, recent), JournalFiles.listOldestFirst(folder));
        assertEquals(recent, JournalFiles.newest(folder).orElseThrow());
    }

    @Test
    void aStrayLogFileNeverOutranksAJournalWhateverItsModificationTime() throws IOException {
        Instant t = Instant.parse("2026-09-14T20:05:41Z");
        Path stray = journal("Journal.old.log", t.plusSeconds(86400));
        Path notes = journal("notes.log", t.plusSeconds(90000));
        Path live = journal("Journal.2026-09-14T220541.01.log", t);

        assertEquals(live, JournalFiles.newest(folder).orElseThrow());
        // Strays keep their old mtime order among themselves, below every real journal.
        assertEquals(List.of(stray, notes, live), JournalFiles.listOldestFirst(folder));
    }

    @Test
    void theLastNAreTheNewestOldestFirst() throws IOException {
        Instant t = Instant.parse("2026-09-14T20:05:41Z");
        journal("Journal.2026-09-12T100000.01.log", t);
        Path b = journal("Journal.2026-09-13T100000.01.log", t);
        Path c = journal("Journal.2026-09-14T100000.01.log", t);

        assertEquals(List.of(b, c), JournalFiles.newest(folder, 2));
        assertEquals(3, JournalFiles.newest(folder, 10).size());
    }

    @Test
    void anEmptyFolderHasNoNewestJournal() throws IOException {
        assertTrue(JournalFiles.newest(folder).isEmpty());
        assertTrue(JournalFiles.newest(folder, 2).isEmpty());
    }

    @Test
    void startStampReadsBothFormsAndTheBetaSuffix() {
        assertEquals(new JournalFiles.StartStamp(20260914220541L, 1),
                JournalFiles.startStamp("Journal.2026-09-14T220541.01.log").orElseThrow());
        assertEquals(new JournalFiles.StartStamp(20210914220541L, 3),
                JournalFiles.startStamp("Journal.210914220541.03.log").orElseThrow());
        assertEquals(new JournalFiles.StartStamp(20260914220541L, 1),
                JournalFiles.startStamp("JournalBeta.2026-09-14T220541.01.log").orElseThrow());
        assertTrue(JournalFiles.startStamp("Journal.2026-08-24T05.log").isEmpty());
        assertTrue(JournalFiles.startStamp("elite-intel.log").isEmpty());
    }
}
