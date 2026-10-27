package elite.intel.bindforge.storage;

import elite.intel.bindforge.install.GameInstallation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static elite.intel.bindforge.storage.WorkingCopyTest.bytes;
import static elite.intel.bindforge.storage.WorkingCopyTest.text;
import static elite.intel.bindforge.storage.WorkingCopyTest.write;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The one write: back up, save the working copy, write the live copies - in that order, and nothing after a step that
 * failed (principle 3; storage.md rules 7-8).
 */
class WritePathTest {

    @TempDir
    Path temp;

    private Path bindings;
    private Path steam;
    private WorkingCopy workingCopy;
    private final List<String> order = new ArrayList<>();

    @BeforeEach
    void aBindingsFolderAnInstallationAndAnEmptyWorkingCopy() throws IOException {
        bindings = temp.resolve("Options/Bindings");
        write(bindings.resolve("Custom.4.2.binds"), "<live/>");
        steam = temp.resolve("Steam/EliteDangerous");
        Files.createDirectories(GameInstallation.controlSchemesUnder(steam));
        workingCopy = new WorkingCopy(temp.resolve("working"));
    }

    @Test
    void theBackupIsTakenFirstThenTheWorkingCopyAndTheLiveCopiesHoldTheSet() throws IOException {
        WriteReport report = writePath().write(set(), sources -> {
            assertFalse(workingCopy.exists(), "the backup comes before anything is written");
            assertEquals("<live/>", Files.readString(bindings.resolve("Custom.4.2.binds")));
            order.add("backup");
            return backup();
        });

        assertEquals(List.of("backup"), order);
        assertTrue(report.complete());
        assertEquals("<new/>", text(workingCopy.read().bindings().get("Custom.4.2.binds")));
        assertEquals("<new/>", Files.readString(bindings.resolve("Custom.4.2.binds")));
        assertEquals("<Mappings/>",
                Files.readString(GameInstallation.deviceMappingsIn(GameInstallation.controlSchemesUnder(steam))));
    }

    @Test
    void aBackupThatFailsStopsTheWriteWithNothingWritten() throws IOException {
        assertThrows(IOException.class, () -> writePath().write(set(), sources -> {
            throw new IOException("the backup folder cannot be written");
        }));

        assertFalse(workingCopy.exists());
        assertEquals("<live/>", Files.readString(bindings.resolve("Custom.4.2.binds")));
    }

    @Test
    void aWorkingCopyThatCannotBeSavedStopsTheWriteBeforeAnyLiveCopy() throws IOException {
        // WHY: a file where the working copy's folder goes, so it cannot be saved.
        Path blocked = temp.resolve("blocked");
        Files.writeString(blocked, "in the way");
        WritePath writePath = new WritePath(new WorkingCopy(blocked), this::sources);

        assertThrows(IOException.class, () -> writePath.write(set(), sources -> backup()));

        assertEquals("<live/>", Files.readString(bindings.resolve("Custom.4.2.binds")));
        assertFalse(Files.exists(GameInstallation.deviceMappingsIn(GameInstallation.controlSchemesUnder(steam))));
    }

    @Test
    void theBackupIsOfExactlyThePlacesTheWriteWrites() throws IOException {
        List<BackupSources> found = new ArrayList<>();
        WritePath writePath = new WritePath(workingCopy, () -> {
            BackupSources sources = sources();
            found.add(sources);
            return sources;
        });
        List<BackupSources> backedUp = new ArrayList<>();

        WriteReport report = writePath.write(set(), sources -> {
            backedUp.add(sources);
            return backup();
        });

        assertEquals(1, found.size(), "the installations are found once, for the backup and the write alike");
        assertSame(found.getFirst(), backedUp.getFirst());
        assertEquals(steam, report.installations().getFirst().folder());
    }

    @Test
    void twoWritesAtOnceRunOneAfterTheOther() throws Exception {
        CountDownLatch firstBackingUp = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        List<String> seenBySecond = Collections.synchronizedList(new ArrayList<>());
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());
        WritePath writePath = writePath();
        Thread first = new Thread(() -> writeOrRecord(writePath, "<first/>", () -> {
            firstBackingUp.countDown();
            if (!releaseFirst.await(10, TimeUnit.SECONDS)) throw new IOException("never released");
        }, failures));
        // WHY: what the second write's backup finds in the game's file says whether the first write had finished.
        Thread second = new Thread(() -> writeOrRecord(writePath, "<second/>",
                () -> seenBySecond.add(Files.readString(bindings.resolve("Custom.4.2.binds"))), failures));

        first.start();
        assertTrue(firstBackingUp.await(10, TimeUnit.SECONDS));
        second.start();
        waitUntilBlocked(second);
        releaseFirst.countDown();
        first.join(10_000);
        second.join(10_000);

        assertEquals(List.of(), failures);
        assertEquals(List.of("<first/>"), seenBySecond, "the second write began only once the first had finished");
        assertEquals("<second/>", text(workingCopy.read().bindings().get("Custom.4.2.binds")));
        assertEquals("<second/>", Files.readString(bindings.resolve("Custom.4.2.binds")),
                "the working copy and the game's file hold the same set");
    }

    /** The set written is handed on once the working copy holds it - the edits then start from it (Storage S4a). */
    @Test
    void theSetWrittenIsHandedOnOnceTheWorkingCopyHoldsIt() throws IOException {
        List<String> handed = new ArrayList<>();
        WritePath writePath = new WritePath(workingCopy, this::sources, files -> {
            try {
                handed.add(text(workingCopy.read().bindings().get("Custom.4.2.binds")));
            } catch (IOException e) {
                throw new AssertionError(e);
            }
        });

        writePath.write(set(), sources -> backup());

        assertEquals(List.of("<new/>"), handed);
    }

    /**
     * APPLY leaves alone an installation whose device files differ from the working copy (Alan, 2026-10-26): nothing is
     * written to it, the report says it differs, and everything else is still written.
     */
    @Test
    void anInstallationLeftAloneIsNotWrittenAndIsReportedAsDiffering() throws IOException {
        WriteReport report = writePath().write(set(), sources -> backup(), installation -> true);

        WriteReport.Place place = report.installations().getFirst();
        assertTrue(place.differs());
        assertFalse(place.updated());
        assertFalse(Files.exists(GameInstallation.deviceMappingsIn(GameInstallation.controlSchemesUnder(steam))));
        assertEquals("<new/>", Files.readString(bindings.resolve("Custom.4.2.binds")), "the bindings folder is written");
        assertEquals("<new/>", text(workingCopy.read().bindings().get("Custom.4.2.binds")));
    }

    private void writeOrRecord(WritePath writePath, String binds, DuringBackup duringBackup, List<Throwable> failures) {
        try {
            writePath.write(new ConfigurationFiles(Map.of("Custom.4.2.binds", bytes(binds)), null, Map.of()),
                    sources -> {
                        try {
                            duringBackup.run();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IOException(e);
                        }
                        return backup();
                    });
        } catch (IOException | RuntimeException e) {
            failures.add(e);
        }
    }

    @FunctionalInterface
    private interface DuringBackup {
        void run() throws IOException, InterruptedException;
    }

    // WHY: the second write is waiting for the first when its thread is blocked on the write path's lock - asked of the
    // thread rather than waited out with a sleep.
    private static void waitUntilBlocked(Thread thread) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (thread.getState() != Thread.State.BLOCKED) {
            if (System.nanoTime() > deadline) fail("the second write never waited for the first");
            Thread.onSpinWait();
        }
    }

    private WritePath writePath() {
        return new WritePath(workingCopy, this::sources);
    }

    private BackupSources sources() {
        return new BackupSources(bindings, List.of(new BackupSources.Installation("STEAM", steam, false, null)));
    }

    private Backup backup() {
        return new Backup(temp.resolve("backups/BeforeChanges/2026-10-25_10-00-00.zip"), LocalDateTime.now(),
                Backups.Kind.BEFORE_CHANGES, Backups.Before.APPLY, true, List.of());
    }

    private static ConfigurationFiles set() {
        return new ConfigurationFiles(Map.of("Custom.4.2.binds", bytes("<new/>")), bytes("<Mappings/>"), Map.of());
    }
}
