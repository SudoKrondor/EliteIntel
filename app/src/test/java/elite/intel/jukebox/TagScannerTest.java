package elite.intel.jukebox;

import elite.intel.db.dao.JukeboxDao;
import elite.intel.db.managers.JukeboxManager;
import elite.intel.db.util.Database;
import elite.intel.util.Cypher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Filling in the playlist's columns after the fact.
 *
 * <p>The behaviour that matters is not "tags get read" - it is what happens around the edges: a library
 * has to become usable before the reading finishes, a file with nothing to say must not be re-read on
 * every launch, and one bad file must not stop the rest.
 */
class TagScannerTest {

    private static final long AWAIT_MS = 5_000;

    private RecordingReader reader;
    private TagScanner scanner;

    @BeforeAll
    static void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
    }

    @BeforeEach
    void freshLibrary() {
        JukeboxManager.getInstance().clear();
        reader = new RecordingReader();
        scanner = new TagScanner(JukeboxManager.getInstance(), reader);
    }

    @AfterEach
    void stopScanning() {
        scanner.shutdown();
    }

    @Test
    void tracksGetTheTagsTheirFilesCarry() {
        library().add(List.of("/music/one.mp3"));
        reader.willReport("/music/one.mp3",
                new TrackTags("Aphelion Drift", "Stellar Cartography", "Deep Black", 7, 245_000L));

        scanner.start();

        await(() -> library().playlist().get(0).isTagsScanned(), "the track was never scanned");
        JukeboxDao.Track track = library().playlist().get(0);
        assertEquals("Aphelion Drift", track.getTitle());
        assertEquals("Stellar Cartography", track.getArtist());
        assertEquals("Deep Black", track.getAlbum());
        assertEquals(7, track.getTrackNumber());
        assertEquals(245_000L, track.getDurationMs());
    }

    @Test
    void everyTrackInTheLibraryIsEventuallyRead() {
        List<String> many = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            many.add("/music/track" + i + ".mp3");
        }
        library().add(many);

        scanner.start();

        await(() -> library().awaitingTagScan().isEmpty(),
                "the scanner stopped before finishing the library");
        assertEquals(150, reader.readCount(), "every file should have been opened exactly once");
    }

    @Test
    void aFileWithNothingToSayIsStillMarkedReadSoItIsNotOpenedAgain() {
        library().add(List.of("/music/untagged.mp3"));
        reader.willReport("/music/untagged.mp3", TrackTags.UNKNOWN);

        scanner.start();
        await(() -> library().awaitingTagScan().isEmpty(), "the untagged file was never marked read");

        JukeboxDao.Track track = library().playlist().get(0);
        assertTrue(track.isTagsScanned());
        assertNull(track.getTitle(), "no title was invented for a file that has none");
    }

    @Test
    void oneUnreadableFileDoesNotStopTheRest() {
        library().add(List.of("/music/good1.mp3", "/music/broken.mp3", "/music/good2.mp3"));
        reader.willReport("/music/good1.mp3", new TrackTags("First", null, null, null, 1000L));
        reader.willFail("/music/broken.mp3");
        reader.willReport("/music/good2.mp3", new TrackTags("Second", null, null, null, 2000L));

        scanner.start();

        await(() -> library().awaitingTagScan().isEmpty(), "a corrupt file stalled the scan");
        List<JukeboxDao.Track> tracks = library().playlist();
        assertEquals("First", tracks.get(0).getTitle());
        assertNull(tracks.get(1).getTitle(), "the broken file has no title");
        assertTrue(tracks.get(1).isTagsScanned(),
                "and is still marked read, or it would be retried on every launch forever");
        assertEquals("Second", tracks.get(2).getTitle());
    }

    @Test
    void filesAddedAfterTheScanStartedAreReadToo() {
        library().add(List.of("/music/first.mp3"));
        scanner.start();
        await(() -> library().awaitingTagScan().isEmpty(), "the first pass never finished");

        library().add(List.of("/music/second.mp3"));
        scanner.requestScan();

        await(() -> library().awaitingTagScan().isEmpty(),
                "a folder added later must be read without restarting the app");
    }

    @Test
    void anAlreadyScannedLibraryIsNotReadAgainOnStart() {
        library().add(List.of("/music/one.mp3"));
        library().recordTags(library().playlist().get(0).getId(), "Known", null, null, null, 1000L);

        scanner.start();
        await(() -> library().awaitingTagScan().isEmpty(), "unexpected pending work");

        assertEquals(0, reader.readCount(),
                "a library scanned last session must not be re-opened file by file on every launch");
    }

    private static JukeboxManager library() {
        return JukeboxManager.getInstance();
    }

    private static void await(BooleanSupplier condition, String failure) {
        long deadline = System.currentTimeMillis() + AWAIT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) return;
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("interrupted while waiting: " + failure);
            }
        }
        fail(failure);
    }

    /**
     * A tag reader with scripted answers, so the scanner can be tested with no audio on disk.
     */
    private static final class RecordingReader implements TrackTagReader {
        private final java.util.Map<String, TrackTags> answers = new ConcurrentHashMap<>();
        private final Set<String> failures = ConcurrentHashMap.newKeySet();
        private final Set<String> opened = ConcurrentHashMap.newKeySet();
        private final java.util.concurrent.atomic.AtomicInteger reads = new java.util.concurrent.atomic.AtomicInteger();

        void willReport(String path, TrackTags tags) {
            answers.put(path, tags);
        }

        void willFail(String path) {
            failures.add(path);
        }

        int readCount() {
            return reads.get();
        }

        @Override
        public TrackTags read(Path file) throws IOException {
            String path = file.toString();
            reads.incrementAndGet();
            opened.add(path);
            if (failures.contains(path)) throw new IOException("cannot read " + path);
            return answers.getOrDefault(path, TrackTags.UNKNOWN);
        }
    }
}
