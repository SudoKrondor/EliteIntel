package elite.intel.bindforge.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The daily backup at the first Elite-Intel start each day, then retention - and nothing pruned when the backup failed
 * (storage.md rules 11-12).
 */
class StartupBackupTest {

    private static final Backups.Retention RETENTION = new Backups.Retention(30, 100);

    @TempDir
    Path temp;

    @Test
    void takesTheDailyBackupThenDeletesOnesOlderThanTheSetting() throws IOException {
        Path root = temp.resolve("backups");
        BackupSources sources = sourcesWithOneBindsFile();
        Path old = backups(root, sources, "2026-09-01T10:00:00Z").backUpEverything(Backups.Kind.DAILY).zip();
        AtomicInteger retentionAsked = new AtomicInteger();

        StartupBackup startup = new StartupBackup(() -> backups(root, sources, "2026-10-24T10:00:00Z"), () -> {
            retentionAsked.incrementAndGet();
            return RETENTION;
        });
        startup.run();

        assertTrue(startup.awaitTaken());
        assertEquals(1, retentionAsked.get());
        assertFalse(Files.exists(old));
        assertTrue(Files.exists(root.resolve("Daily/2026-10-24_10-00-00.zip")));
    }

    @Test
    void aLaterStartTheSameDayTakesNoneButStillPrunesAndSaysItIsTaken() throws IOException {
        Path root = temp.resolve("backups");
        BackupSources sources = sourcesWithOneBindsFile();
        Path old = backups(root, sources, "2026-09-01T10:00:00Z").backUpEverything(Backups.Kind.DAILY).zip();
        new StartupBackup(() -> backups(root, sources, "2026-10-24T08:00:00Z"), () -> RETENTION).run();

        StartupBackup later = new StartupBackup(() -> backups(root, sources, "2026-10-24T20:00:00Z"), () -> RETENTION);
        later.run();

        assertTrue(later.awaitTaken(), "today's backup is there, so a write waiting on it may go ahead");
        assertEquals(List.of("2026-10-24_08-00-00.zip"), names(root.resolve("Daily")), "one daily backup a day");
        assertFalse(Files.exists(old));
    }

    @Test
    void theNextDayTakesAnother() throws IOException {
        Path root = temp.resolve("backups");
        BackupSources sources = sourcesWithOneBindsFile();
        new StartupBackup(() -> backups(root, sources, "2026-10-24T08:00:00Z"), () -> RETENTION).run();

        new StartupBackup(() -> backups(root, sources, "2026-10-25T08:00:00Z"), () -> RETENTION).run();

        assertEquals(List.of("2026-10-24_08-00-00.zip", "2026-10-25_08-00-00.zip"), names(root.resolve("Daily")));
    }

    @Test
    void aFailedBackupIsReportedNothingIsPrunedAndTheNextStartTriesAgain() throws IOException {
        Path root = temp.resolve("backups");
        Path old = backups(root, sourcesWithOneBindsFile(), "2026-09-01T10:00:00Z")
                .backUpEverything(Backups.Kind.DAILY).zip();
        BackupSources nothing = new BackupSources(temp.resolve("no bindings"), List.of());

        StartupBackup startup = new StartupBackup(() -> backups(root, nothing, "2026-10-24T10:00:00Z"), () -> RETENTION);
        startup.run();

        assertFalse(startup.awaitTaken(), "a write waiting on it is told not to go ahead");
        assertTrue(Files.exists(old), "today's backup is missing, so the older ones are kept");

        BackupSources readable = sourcesWithOneBindsFile();
        new StartupBackup(() -> backups(root, readable, "2026-10-24T11:00:00Z"), () -> RETENTION).run();
        assertTrue(Files.exists(root.resolve("Daily/2026-10-24_11-00-00.zip")), "tried again the same day");
    }

    @Test
    void anythingThrownIsCaughtSoStartupGoesOn() {
        StartupBackup startup = new StartupBackup(() -> {
            throw new IllegalStateException("database is gone");
        }, () -> RETENTION);

        assertDoesNotThrow(startup::run);
        assertFalse(startup.awaitTaken());
    }

    private BackupSources sourcesWithOneBindsFile() throws IOException {
        Path bindings = temp.resolve("Bindings");
        Files.createDirectories(bindings);
        Files.writeString(bindings.resolve("Custom.4.2.binds"), "<Root/>");
        return new BackupSources(bindings, List.of());
    }

    private static Backups backups(Path root, BackupSources sources, String instant) {
        return new Backups(() -> sources, () -> Files.createDirectories(root),
                Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
    }

    private static List<String> names(Path folder) throws IOException {
        try (Stream<Path> files = Files.list(folder)) {
            return files.map(p -> p.getFileName().toString()).sorted().toList();
        }
    }
}
