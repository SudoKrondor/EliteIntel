package elite.intel.db.util;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Two transactions writing the shared file and the commander's file in opposite orders must both commit.
 * <p>
 * A location save writes the shared file first and the commander's second; a player save does the reverse.
 * Under SQLite's default deferred transactions the two deadlocked and both failed with SQLITE_BUSY once the
 * busy timeout ran out. Needs real files: an in-memory database has no file locks to contend for.
 */
class CrossFileWriteLockTest {

    @TempDir
    Path dir;

    @Test
    void oppositeOrderWritersBothCommit() {
        String commanderFile = dir.resolve("cmdr_test.db").toString();
        Jdbi jdbi = Jdbi.create("jdbc:sqlite:" + dir.resolve("database.db") + Database.CONNECTION_OPTIONS);
        jdbi.useHandle(h -> {
            attach(h, commanderFile);
            // The commander file is WAL in the app too (see Database).
            h.execute("PRAGMA cmdr.journal_mode = WAL");
            h.execute("CREATE TABLE main.location (name TEXT PRIMARY KEY)");
            h.execute("CREATE TABLE cmdr.location_visit (name TEXT PRIMARY KEY)");
        });

        // Each writer takes its first lock, then waits for the other to take theirs before reaching for the
        // second. The wait is bounded: with the fix the second writer is held at BEGIN and never arrives.
        CountDownLatch bothStarted = new CountDownLatch(2);
        CompletableFuture<Void> sharedFirst = CompletableFuture.runAsync(() ->
                writeBoth(jdbi, commanderFile, "main.location", "cmdr.location_visit", bothStarted));
        CompletableFuture<Void> commanderFirst = CompletableFuture.runAsync(() ->
                writeBoth(jdbi, commanderFile, "cmdr.location_visit", "main.location", bothStarted));

        assertDoesNotThrow(() -> CompletableFuture.allOf(sharedFirst, commanderFirst).get(30, TimeUnit.SECONDS));
        jdbi.useHandle(h -> {
            attach(h, commanderFile);
            assertEquals(2, h.createQuery("SELECT COUNT(*) FROM main.location").mapTo(Integer.class).one());
            assertEquals(2, h.createQuery("SELECT COUNT(*) FROM cmdr.location_visit").mapTo(Integer.class).one());
        });
    }

    private static void writeBoth(Jdbi jdbi, String commanderFile, String first, String second,
                                  CountDownLatch bothStarted) {
        String writer = first;
        jdbi.useHandle(h -> {
            attach(h, commanderFile);
            h.useTransaction(tx -> {
                tx.execute("INSERT INTO " + first + " (name) VALUES (?)", writer);
                bothStarted.countDown();
                try {
                    bothStarted.await(300, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                tx.execute("INSERT INTO " + second + " (name) VALUES (?)", writer);
            });
        });
    }

    private static void attach(Handle h, String commanderFile) {
        h.execute("ATTACH DATABASE '" + commanderFile.replace("'", "''") + "' AS " + CommanderSplit.SCHEMA);
    }
}
