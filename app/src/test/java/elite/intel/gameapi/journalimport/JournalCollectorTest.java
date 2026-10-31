package elite.intel.gameapi.journalimport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static elite.intel.gameapi.journalimport.TestArchives.*;
import static elite.intel.gameapi.journalimport.TestJournals.header;
import static elite.intel.gameapi.journalimport.TestJournals.journal;
import static org.junit.jupiter.api.Assertions.*;

/**
 * However the commander kept their journals, the collector finds them - and nothing it is handed, however broken or
 * hostile, gets further than being counted and passed over.
 */
class JournalCollectorTest {

    private static final String OLDEST = "Journal.2019-03-01T100000.01.log";
    private static final String MIDDLE = "Journal.2021-07-15T183000.01.log";
    private static final String NEWEST = "Journal.2024-11-30T091500.01.log";
    /**
     * The name the game used before 2022: same journal, two-digit year.
     */
    private static final String PRE_2022 = "Journal.180102030405.01.log";

    @TempDir
    Path work;

    @Test
    void journalsInAFolderAreFoundAtAnyDepthAndEverythingElseIsPassedOver() throws IOException {
        Path backup = work.resolve("backup");
        write(backup.resolve("2019/03").resolve(OLDEST), body("2019-03-01T10:00:00Z"));
        write(backup.resolve(NEWEST), body("2024-11-30T09:15:00Z"));
        write(backup.resolve("Status.json"), "{}".getBytes(StandardCharsets.UTF_8));
        write(backup.resolve("screenshot.png"), new byte[]{(byte) 0x89, 'P', 'N', 'G'});

        Result result = collect(backup);

        assertEquals(List.of(OLDEST, NEWEST), result.names());
        assertEquals(2, result.stats().notJournals);
    }

    @Test
    void aZipIsReadWhateverFoldersItsJournalsSitInAndWhateverElseItHolds() throws IOException {
        Path zip = write(work.resolve("journals.zip"), zip(entries(
                "Saved Games/Frontier Developments/Elite Dangerous/" + MIDDLE, body("2021-07-15T18:30:00Z"),
                "Saved Games\\Frontier Developments\\" + OLDEST, body("2019-03-01T10:00:00Z"),
                "Saved Games/Frontier Developments/Elite Dangerous/Cargo.json", "{}",
                "readme.txt", "my journals")));

        Result result = collect(zip);

        assertEquals(List.of(OLDEST, MIDDLE), result.names(), "oldest first, the order the import reads them in");
        assertEquals(2, result.stats().notJournals);
        assertEquals(header("2019-03-01T10:00:00Z", "English/UK"), firstLine(result.journals().getFirst()),
                "a journal copied out of an archive reads back exactly as it went in");
    }

    @Test
    void tarballsSevenZipsAndALoneGzippedJournalAreAllRead() throws IOException {
        Path tarGz = write(work.resolve("2019.tar.gz"), tarGz(entries("journals/" + OLDEST, body("2019-03-01T10:00:00Z"))));
        Path tar = write(work.resolve("2018.tar"), tar(entries(PRE_2022, body("2018-01-02T03:04:05Z"))));
        Path sevenZ = work.resolve("2021.7z");
        sevenZ(sevenZ, entries("logs/" + MIDDLE, body("2021-07-15T18:30:00Z")));
        Path loneGzip = write(work.resolve(NEWEST + ".gz"), gzip(body("2024-11-30T09:15:00Z")));

        Result result = collect(tarGz, tar, sevenZ, loneGzip);

        assertEquals(List.of(PRE_2022, OLDEST, MIDDLE, NEWEST), result.names());
    }

    @Test
    void anArchiveIsRecognisedByItsContentNotItsName() throws IOException {
        Path misnamed = write(work.resolve("journals.dat"), zip(entries(OLDEST, body("2019-03-01T10:00:00Z"))));

        assertEquals(List.of(OLDEST), collect(misnamed).names());
    }

    @Test
    void anArchiveInsideAnArchiveIsOpenedToo() throws IOException {
        byte[] inner = zip(entries(MIDDLE, body("2021-07-15T18:30:00Z")));
        Path outer = write(work.resolve("everything.tar.gz"), tarGz(entries(
                "old/2021.zip", inner,
                OLDEST, body("2019-03-01T10:00:00Z"))));

        assertEquals(List.of(OLDEST, MIDDLE), collect(outer).names());
    }

    @Test
    void anEntryNamedToEscapeTheStagingFolderGoesNowhere() throws IOException {
        Path zip = write(work.resolve("hostile.zip"), zip(entries(
                "../../../../" + OLDEST, body("2019-03-01T10:00:00Z"),
                "../../../../evil.sh", "rm -rf /")));
        Path staging = work.resolve("staging");
        Files.createDirectories(staging);

        Result result = collectInto(staging, zip);

        assertEquals(List.of(OLDEST), result.names(), "the journal's own name is all that is taken from the entry");
        try (Stream<Path> everything = Files.walk(work)) {
            assertTrue(everything.noneMatch(path -> path.getFileName().toString().equals("evil.sh")));
        }
        try (Stream<Path> staged = Files.list(staging)) {
            assertTrue(staged.allMatch(path -> path.getFileName().toString().matches("\\d+\\.log\\.gz")),
                    "a staged copy is named by the collector, never by the archive");
        }
    }

    @Test
    void theSameJournalFoundTwiceIsKeptOnceAndTheLongerCopyWins() throws IOException {
        byte[] whole = body("2019-03-01T10:00:00Z");
        byte[] copiedMidSession = Arrays.copyOf(whole, whole.length / 2);
        write(work.resolve("folder").resolve(OLDEST), copiedMidSession);
        Path zip = write(work.resolve("backup.zip"), zip(entries(OLDEST, whole)));

        Result result = collect(work.resolve("folder"), zip);

        assertEquals(1, result.journals().size());
        assertEquals(whole.length, result.journals().getFirst().size());
        assertEquals(1, result.stats().duplicates);
    }

    @Test
    void aCorruptArchiveIsCountedAndWhatCameOutOfItBeforeTheDamageIsKept() throws IOException {
        // The second journal is noise that will not compress, so cutting the archive at two thirds lands inside it.
        byte[] healthy = tarGz(entries(OLDEST, body("2019-03-01T10:00:00Z"),
                MIDDLE, noisyBody("2021-07-15T18:30:00Z", 200_000)));
        byte[] truncated = Arrays.copyOf(healthy, healthy.length * 2 / 3);
        Path broken = write(work.resolve("broken.tar.gz"), truncated);
        Path garbage = write(work.resolve("garbage.zip"), new byte[]{'P', 'K', 3, 4, 1, 2, 3, 4, 5, 6, 7});
        Path rar = write(work.resolve("journals.rar"), new byte[]{'R', 'a', 'r', '!', 0x1A, 0x07, 0, 0});

        Result result = collect(broken, garbage, rar);

        assertEquals(List.of(OLDEST, MIDDLE), result.names(),
                "the whole first journal, and as much of the second as the archive still held");
        assertTrue(result.journals().get(1).size() < 200_000);
        assertTrue(result.stats().damaged >= 2, "the cut tarball and the zip that is no zip");
        assertEquals(1, result.stats().unsupported);
    }

    @Test
    void cancellingStopsTheCollection() throws IOException {
        Path folder = work.resolve("folder");
        write(folder.resolve(OLDEST), body("2019-03-01T10:00:00Z"));
        Path staging = Files.createDirectories(work.resolve("staging"));

        JournalCollector collector = new JournalCollector(staging, (done, total) -> {
        }, () -> true);

        assertThrows(ImportCancelledException.class, () -> collector.collect(List.of(folder)));
    }

    // ---------------------------------------------------------------- helpers

    private record Result(List<FoundJournal> journals, CollectStats stats) {
        List<String> names() {
            return journals.stream().map(FoundJournal::name).toList();
        }
    }

    private Result collect(Path... sources) throws IOException {
        Path staging = Files.createDirectories(work.resolve("staging-" + System.nanoTime()));
        return collectInto(staging, sources);
    }

    private Result collectInto(Path staging, Path... sources) throws IOException {
        JournalCollector collector = new JournalCollector(staging, (done, total) -> {
        }, () -> false);
        return new Result(collector.collect(List.of(sources)), collector.stats());
    }

    private static byte[] body(String timestamp) {
        return journal(header(timestamp, "English/UK"),
                "{ \"timestamp\":\"" + timestamp + "\", \"event\":\"Music\", \"MusicTrack\":\"MainMenu\" }")
                .getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] noisyBody(String timestamp, int noiseBytes) {
        StringBuilder noise = new StringBuilder();
        java.util.Random random = new java.util.Random(42);
        while (noise.length() < noiseBytes) {
            noise.append("{ \"event\":\"Music\", \"MusicTrack\":\"").append(Long.toHexString(random.nextLong()))
                    .append("\" }\n");
        }
        return (header(timestamp, "English/UK") + "\n" + noise).getBytes(StandardCharsets.UTF_8);
    }

    private static String firstLine(FoundJournal journal) throws IOException {
        try (InputStream in = journal.open(); BoundedLineReader reader = new BoundedLineReader(in)) {
            return reader.readLine();
        }
    }
}
