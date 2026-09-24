package elite.intel.gameapi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Which commander the newest journal belongs to decides which database file opens, so a wrong answer would show
 * one commander another's ships.
 */
class JournalCommanderTest {

    private static final String HEADER = "{ \"timestamp\":\"2026-09-20T10:00:00Z\", \"event\":\"Fileheader\", \"part\":1 }";

    @TempDir
    Path dir;

    @Test
    void theNewestJournalNamesTheCommander() throws IOException {
        journal("Journal.2026-09-19T100000.01.log", commander("F1111111", "Alpha"));
        journal("Journal.2026-09-20T100000.01.log", commander("F2222222", "Bravo"));

        assertEquals(Optional.of("F2222222"), JournalCommander.newestFid(dir));
    }

    @Test
    void aContinuationPartTakesTheCommanderOfThePartBeforeIt() throws IOException {
        journal("Journal.2026-09-20T100000.01.log", commander("F1111111", "Alpha"));
        // The .02 part opens with only a Fileheader: the game does not repeat the Commander event.
        Files.writeString(dir.resolve("Journal.2026-09-20T100000.02.log"),
                "{ \"timestamp\":\"2026-09-20T12:00:00Z\", \"event\":\"Fileheader\", \"part\":2 }\n"
                        + "{ \"timestamp\":\"2026-09-20T12:00:01Z\", \"event\":\"Music\", \"MusicTrack\":\"Exploration\" }\n");

        assertEquals(Optional.of("F1111111"), JournalCommander.newestFid(dir));
    }

    @Test
    void aSessionThatQuitAtTheMenuIsSkipped() throws IOException {
        journal("Journal.2026-09-19T100000.01.log", commander("F1111111", "Alpha"));
        Files.writeString(dir.resolve("Journal.2026-09-20T100000.01.log"), HEADER + "\n");

        assertEquals(Optional.of("F1111111"), JournalCommander.newestFid(dir));
    }

    @Test
    void loadGameAloneIsEnough() throws IOException {
        journal("Journal.2026-09-20T100000.01.log",
                "{ \"timestamp\":\"2026-09-20T10:00:05Z\", \"event\":\"LoadGame\", \"FID\":\"F3333333\", \"Commander\":\"Charlie\" }");

        assertEquals(Optional.of("F3333333"), JournalCommander.newestFid(dir));
    }

    @Test
    void anEmptyOrMissingFolderNamesNoOne() {
        assertEquals(Optional.empty(), JournalCommander.newestFid(dir));
        assertEquals(Optional.empty(), JournalCommander.newestFid(dir.resolve("missing")));
    }

    @Test
    void thePreScanReadsOnlyTheCommandersOwnJournals() throws IOException {
        journal("Journal.2026-09-17T100000.01.log", commander("F1111111", "Alpha"));
        journal("Journal.2026-09-18T100000.01.log", commander("F2222222", "Bravo"));
        Files.writeString(dir.resolve("Journal.2026-09-18T100000.02.log"), HEADER + "\n");
        journal("Journal.2026-09-19T100000.01.log", commander("F1111111", "Alpha"));
        Files.writeString(dir.resolve("Journal.2026-09-20T100000.01.log"), HEADER + "\n"); // quit at the menu

        assertEquals(List.of(dir.resolve("Journal.2026-09-17T100000.01.log"), dir.resolve("Journal.2026-09-19T100000.01.log")),
                JournalCommander.newestOwnedBy(dir, "F1111111", 2), "Alpha's two newest, oldest first");
        assertEquals(List.of(dir.resolve("Journal.2026-09-18T100000.01.log"), dir.resolve("Journal.2026-09-18T100000.02.log")),
                JournalCommander.newestOwnedBy(dir, "F2222222", 5), "Bravo's session, continuation part included");
        assertEquals(List.of(), JournalCommander.newestOwnedBy(dir, "F9999999", 2));
    }

    private void journal(String name, String commanderLine) throws IOException {
        Files.writeString(dir.resolve(name), HEADER + "\n" + commanderLine + "\n");
    }

    private static String commander(String fid, String name) {
        return "{ \"timestamp\":\"2026-09-20T10:00:01Z\", \"event\":\"Commander\", \"FID\":\"" + fid
                + "\", \"Name\":\"" + name + "\" }";
    }
}
