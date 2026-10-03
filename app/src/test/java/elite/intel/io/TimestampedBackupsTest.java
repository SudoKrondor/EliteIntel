package elite.intel.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TimestampedBackupsTest {

    @TempDir
    Path dir;

    @Test
    void createCopiesTheFileUnderATimestampedBakName() throws IOException {
        Path original = original("Custom.4.2.binds", "bindings");

        Path backup = backupsAt("2026-10-03T12:00:00Z").create(original, dir.resolve("backups"));

        assertEquals("Custom.4.2.binds.20261003-120000.bak", backup.getFileName().toString());
        assertEquals("bindings", Files.readString(backup));
    }

    @Test
    void createInTheSameSecondNumbersTheNextCopy() throws IOException {
        Path original = original("Custom.4.2.binds", "bindings");
        TimestampedBackups backups = backupsAt("2026-10-03T12:00:00Z");

        backups.create(original, dir.resolve("backups"));
        Path second = backups.create(original, dir.resolve("backups"));

        assertEquals("Custom.4.2.binds.20261003-120000-1.bak", second.getFileName().toString());
    }

    @Test
    void pruneKeepsTheNewestCopiesCountingSameSecondOrder() throws IOException {
        Path original = original("DeviceMappings.xml", "map");
        Path backupDir = dir.resolve("backups");
        backupsAt("2026-10-01T09:00:00Z").create(original, backupDir);
        TimestampedBackups later = backupsAt("2026-10-03T12:00:00Z");
        later.create(original, backupDir);
        later.create(original, backupDir);

        later.prune(backupDir, "DeviceMappings.xml", 2);

        assertEquals(List.of(
                "DeviceMappings.xml.20261003-120000-1.bak",
                "DeviceMappings.xml.20261003-120000.bak"), names(backupDir));
    }

    @Test
    void pruneLeavesOtherFilesInTheFolderAlone() throws IOException {
        Path backupDir = dir.resolve("backups");
        backupsAt("2026-10-03T12:00:00Z").create(original("a.binds", "a"), backupDir);
        Files.writeString(backupDir.resolve("notes.txt"), "keep me");
        backupsAt("2026-10-03T12:00:00Z").create(original("b.binds", "b"), backupDir);

        backupsAt("2026-10-03T12:00:00Z").prune(backupDir, "a.binds", 0);

        assertEquals(List.of("b.binds.20261003-120000.bak", "notes.txt"), names(backupDir));
    }

    @Test
    void pruneRejectsANegativeCount() {
        TimestampedBackups backups = backupsAt("2026-10-03T12:00:00Z");

        assertThrows(IllegalArgumentException.class, () -> backups.prune(dir, "a.binds", -1));
    }

    private Path original(String name, String content) throws IOException {
        return Files.writeString(dir.resolve(name), content);
    }

    private static TimestampedBackups backupsAt(String instant) {
        return new TimestampedBackups(Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
    }

    private static List<String> names(Path directory) throws IOException {
        try (Stream<Path> stream = Files.list(directory)) {
            return stream.map(path -> path.getFileName().toString()).sorted().toList();
        }
    }
}
