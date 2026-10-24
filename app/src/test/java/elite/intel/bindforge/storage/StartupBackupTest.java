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

import static org.junit.jupiter.api.Assertions.*;

/**
 * The backup at every Elite-Intel start, then retention - and nothing pruned when the backup failed (storage.md
 * rules 11-12).
 */
class StartupBackupTest {

    @TempDir
    Path temp;

    @Test
    void takesABackupThenDeletesOnesOlderThanTheSetting() throws IOException {
        Path root = temp.resolve("backups");
        BackupSources sources = sourcesWithOneBindsFile();
        Path old = backups(root, sources, "2026-09-01T10:00:00Z").backUpEverything().folder();
        AtomicInteger retentionAsked = new AtomicInteger();

        StartupBackup startup = new StartupBackup(() -> backups(root, sources, "2026-10-24T10:00:00Z"), () -> {
            retentionAsked.incrementAndGet();
            return 30;
        });
        startup.run();

        assertTrue(startup.awaitTaken());
        assertEquals(1, retentionAsked.get());
        assertFalse(Files.exists(old));
        assertTrue(Files.exists(root.resolve("2026-10-24_10-00-00")));
    }

    @Test
    void aFailedBackupIsReportedAndNothingIsPruned() throws IOException {
        Path root = temp.resolve("backups");
        Path old = backups(root, sourcesWithOneBindsFile(), "2026-09-01T10:00:00Z").backUpEverything().folder();
        BackupSources nothing = new BackupSources(temp.resolve("no bindings"), List.of());

        StartupBackup startup = new StartupBackup(() -> backups(root, nothing, "2026-10-24T10:00:00Z"), () -> 30);
        startup.run();

        assertFalse(startup.awaitTaken(), "a write waiting on it is told not to go ahead");
        assertTrue(Files.exists(old), "today's backup is missing, so the older ones are kept");
    }

    @Test
    void anythingThrownIsCaughtSoStartupGoesOn() {
        StartupBackup startup = new StartupBackup(() -> {
            throw new IllegalStateException("database is gone");
        }, () -> 30);

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
}
