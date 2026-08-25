package elite.intel.gameapi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reading the game's auxiliary files without losing one.
 * <p>
 * The game writes these while we may be halfway through reading them, and {@code Market.json} at a big
 * station runs to a hundred kilobytes. A read caught mid-write used to be logged and thrown away, and
 * nothing asked again until the game wrote that file afresh - so a market the commander was standing in
 * stayed unknown to the app, and the construction card fell back to naming a commodity that station did
 * not sell. Measured at Breguet Manufacturing.
 */
class AuxiliaryFileRetryTest {

    @TempDir
    Path journal;

    private static final String WHOLE = """
            { "timestamp":"2026-08-25T20:08:41Z", "event":"Market", "MarketID":4247795459,
              "StationName":"Breguet Manufacturing", "StarSystem":"HIP 24191", "Items":[] }
            """;

    /**
     * What a reader sees partway through the game's write.
     */
    private static final String HALF_WRITTEN = WHOLE.substring(0, 60);

    private void write(String name, String content) throws IOException {
        Files.write(journal.resolve(name), content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void aFileCaughtMidWriteIsReadAgainRatherThanLost() throws IOException {
        AuxiliaryFilesMonitor monitor = new AuxiliaryFilesMonitor(journal);
        write("Market.json", HALF_WRITTEN);

        assertFalse(monitor.publishIfChanged("Market.json"), "half a file is not an update");

        write("Market.json", WHOLE);
        assertTrue(monitor.publishIfChanged("Market.json"), "the finished write is picked up");
    }

    /**
     * The retry has to survive the game finishing its write without changing the stamp we saw - otherwise
     * "read it again next cycle" quietly becomes "never read it again".
     */
    @Test
    void aFailedReadDoesNotCountAsHavingBeenRead() throws IOException {
        AuxiliaryFilesMonitor monitor = new AuxiliaryFilesMonitor(journal);
        write("Market.json", HALF_WRITTEN);

        assertFalse(monitor.publishIfChanged("Market.json"));
        assertFalse(monitor.publishIfChanged("Market.json"), "still half a file");

        Files.write(journal.resolve("Market.json"), WHOLE.getBytes(StandardCharsets.UTF_8));
        assertTrue(monitor.publishIfChanged("Market.json"));
    }

    @Test
    void anUnchangedFileIsNotPublishedTwice() throws IOException {
        AuxiliaryFilesMonitor monitor = new AuxiliaryFilesMonitor(journal);
        write("Market.json", WHOLE);

        assertTrue(monitor.publishIfChanged("Market.json"));
        assertFalse(monitor.publishIfChanged("Market.json"), "nothing has moved");
    }

    @Test
    void aRewrittenFileIsPublishedAgain() throws IOException {
        AuxiliaryFilesMonitor monitor = new AuxiliaryFilesMonitor(journal);
        write("Market.json", WHOLE);
        assertTrue(monitor.publishIfChanged("Market.json"));

        write("Market.json", WHOLE.replace("4247795459", "3712500736"));
        assertTrue(monitor.publishIfChanged("Market.json"), "a different market at the same path");
    }

    @Test
    void aFileTheGameHasNeverWrittenIsNothingToPublish() {
        assertFalse(new AuxiliaryFilesMonitor(journal).publishIfChanged("Market.json"));
    }
}
