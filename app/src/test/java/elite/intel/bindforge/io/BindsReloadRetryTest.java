package elite.intel.bindforge.io;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BindsReloadRetryTest {

    private static final File BINDS = new File("Custom.4.2.binds");

    /**
     * Each lookup takes the next answer: a file, or null for "the preset file is being rewritten" - hence a LinkedList,
     * which holds nulls.
     */
    private final Deque<File> lookups = new LinkedList<>();
    private final List<String> reloads = new ArrayList<>();
    private final BindsReloadRetry retry = new BindsReloadRetry(this::lookup,
            (file, changed) -> reloads.add(file.getName() + " after " + changed), 3, Duration.ZERO);

    private File lookup() throws IOException {
        File next = lookups.removeFirst();
        if (next == null) throw new IOException("StartPreset.4.start holds no preset");
        return next;
    }

    @Test
    void aChangeWhoseFileIsFoundIsReloadedAtOnceAndOwesNoRetry() throws InterruptedException {
        lookups.add(BINDS);

        retry.changed("Custom.4.2.binds");

        assertEquals(List.of("Custom.4.2.binds after Custom.4.2.binds"), reloads);
        assertFalse(retry.quietSecond(), "nothing is owed, so a quiet second looks nothing up");
    }

    @Test
    void aLookupThatFailsWhileTheGameSavesIsTriedAgainInTheNextQuietSecond() throws InterruptedException {
        // The save seen 2026-11-11: the game empties StartPreset.4.start and writes it again. The lookup used to throw
        // out of the watch loop and end it, and VEGA kept the old file's keys until a restart.
        lookups.add(null);
        lookups.add(null);
        lookups.add(BINDS);

        retry.changed("Custom.4.2.binds");
        assertTrue(reloads.isEmpty());
        assertFalse(retry.quietSecond(), "still being written");
        assertTrue(retry.quietSecond(), "a retry that reads the file tells the caller, which takes in its conflicts");

        assertEquals(List.of("Custom.4.2.binds after null"), reloads, "a retry reads whatever file the lookup names");
        assertFalse(retry.quietSecond(), "the retries end once one has read the file");
    }

    @Test
    void theRetriesStopAtTheirLimit() throws InterruptedException {
        for (int i = 0; i < 4; i++) lookups.add(null);

        retry.changed("Custom.4.2.binds");
        for (int i = 0; i < 3; i++) assertFalse(retry.quietSecond());

        assertTrue(lookups.isEmpty(), "the change and its three retries looked the file up, no more");
        assertFalse(retry.quietSecond(), "given up: the next quiet second looks nothing up");
        assertTrue(reloads.isEmpty());
    }

    @Test
    void aNewChangeStartsTheRetriesAfresh() throws InterruptedException {
        lookups.add(null);
        lookups.add(BINDS);

        retry.changed("Custom.4.2.binds");
        retry.changed("Custom.4.2.binds");

        assertEquals(List.of("Custom.4.2.binds after Custom.4.2.binds"), reloads);
        assertFalse(retry.quietSecond(), "the later change read the file, so nothing is owed");
    }
}
