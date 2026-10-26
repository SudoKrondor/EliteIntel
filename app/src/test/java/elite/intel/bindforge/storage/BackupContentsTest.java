package elite.intel.bindforge.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reading a whole backup zip for RESTORE (storage.md rule 10; file-manager.md, <em>Browse for Backup File</em>): every
 * file in a backup's layout, each installation labelled as {@code backup-info.txt} records it, and only a zip
 * Elite-Intel made (Alan, 2026-10-25).
 */
class BackupContentsTest {

    @TempDir
    Path temp;

    /** What {@link Backups} writes is what is read back - every binds file, and each installation's device files. */
    @Test
    void aBackupBackupsTookIsReadWhole() throws IOException {
        Path bindings = temp.resolve("Bindings");
        write(bindings.resolve("Custom.4.2.binds"), "<Custom/>");
        write(bindings.resolve("StartPreset.4.start"), "Custom");
        Path steam = installation("steam", "<Steam/>", "LVWAP.buttonMap");
        Path epic = installation("epic", "<Epic/>");
        BackupSources sources = new BackupSources(bindings, List.of(
                new BackupSources.Installation("STEAM", steam, false, bindings),
                new BackupSources.Installation("EPIC", epic, false, bindings)));
        Path root = temp.resolve("backups");
        Backup backup = new Backups(() -> sources, () -> root,
                Clock.fixed(Instant.parse("2026-10-25T09:00:00Z"), ZoneOffset.UTC)).backUpEverything(Backups.Kind.USER);

        BackupContents contents = BackupContents.read(backup.zip());

        assertEquals("2026-10-25 09:00:00", contents.taken());
        assertEquals(List.of("Custom.4.2.binds", "StartPreset.4.start"), List.copyOf(contents.bindings().keySet()));
        assertEquals("<Custom/>", text(contents.bindings().get("Custom.4.2.binds")));
        assertEquals(2, contents.installations().size());
        InstallationFiles first = contents.installations().getFirst();
        assertEquals(1, first.installId());
        assertEquals("STEAM", first.storefront());
        assertEquals(steam, first.root(), "the folder backup-info.txt records");
        assertEquals("<Steam/>", text(first.deviceMappings()));
        assertEquals(List.of("LVWAP.buttonMap"), List.copyOf(first.buttonMaps().keySet()));
        assertEquals("EPIC", contents.installations().get(1).storefront());
        assertTrue(contents.hasBindings());
        assertTrue(contents.hasDeviceFiles());
    }

    /** Entries outside the layout, or whose names are paths, are not part of the backup. */
    @Test
    void anEntryOutsideTheLayoutIsSkipped() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put(Backups.INFO_FILE, "Taken: 2026-10-25 09:00:00\n");
        entries.put("Bindings/Custom.4.2.binds", "<Custom/>");
        entries.put("Bindings/Custom.4.2.binds.backup", "not one of the files");
        entries.put("Bindings/..\\evil.4.2.binds", "a path");
        entries.put("Bindings/sub/Other.4.2.binds", "too deep");
        entries.put("Installations/1-STEAM/ControlSchemes/DeviceMappings.xml", "<Steam/>");
        entries.put("Installations/1-STEAM/ControlSchemes/DeviceButtonMaps/..\\x.buttonMap", "a path");
        entries.put("Installations/1-STEAM/Bindings/Linux.4.2.binds", "an installation's own folder");
        entries.put("Installations/not-numbered/ControlSchemes/DeviceMappings.xml", "<Odd/>");
        entries.put("readme.txt", "anything else");
        Path zip = zip("handmade.zip", entries);

        BackupContents contents = BackupContents.read(zip);

        assertEquals(List.of("Custom.4.2.binds"), List.copyOf(contents.bindings().keySet()));
        assertEquals(1, contents.installations().size());
        InstallationFiles steam = contents.installations().getFirst();
        assertEquals("STEAM", steam.storefront(), "from the folder name when backup-info.txt records none");
        assertTrue(steam.buttonMaps().isEmpty());
        assertEquals("2026-10-25 09:00:00", contents.taken());
    }

    /**
     * Review, 2026-10-25: names Windows refuses, a folder number past any number, and a recorded folder that is no path
     * are skipped or fall back - never an unexpected error from a hand-made zip.
     */
    @Test
    void namesThatAreNoFileNameAreSkippedNotThrown() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put(Backups.INFO_FILE, "Taken: x\nInstallations\\1-STEAM: STEAM, D:\\bad|folder?\n");
        entries.put("Bindings/Custom.4.2.binds", "<Custom/>");
        entries.put("Bindings/Bad<name>.4.2.binds", "refused by Windows");
        entries.put("Bindings/Bad\"q\".4.2.binds", "refused by Windows");
        entries.put("Bindings/Star*.4.2.binds", "refused by Windows");
        entries.put("Bindings/Custom.4.2.binds ", "a space at the end, refused by Windows");
        entries.put("Installations/1-STEAM/ControlSchemes/DeviceButtonMaps/Dot.buttonMap.", "a dot at the end");
        entries.put("Installations/1-STEAM/ControlSchemes/DeviceMappings.xml", "<Steam/>");
        entries.put("Installations/1-STEAM/ControlSchemes/DeviceButtonMaps/Pipe|.buttonMap", "refused by Windows");
        entries.put("Installations/99999999999999999999-HUGE/ControlSchemes/DeviceMappings.xml", "<Huge/>");
        entries.put("Installations/2-A?B/ControlSchemes/DeviceMappings.xml", "<Odd/>");

        BackupContents contents = BackupContents.read(zip("odd-names.zip", entries));

        assertEquals(List.of("Custom.4.2.binds"), List.copyOf(contents.bindings().keySet()));
        assertEquals(1, contents.installations().size(), "the huge number and the odd folder are skipped");
        InstallationFiles steam = contents.installations().getFirst();
        assertTrue(steam.buttonMaps().isEmpty());
        assertNotNull(steam.root(), "a recorded folder that is no path falls back to the zip's own name");
    }

    /** Review, 2026-10-25: a zip that unpacks to more than a backup can hold is refused, not read into memory. */
    @Test
    void aZipPastTheSizeLimitIsNotABackup() throws IOException {
        Path zip = zip("big.zip", Map.of(Backups.INFO_FILE, "Taken: x\n", "Bindings/Custom.4.2.binds",
                "x".repeat(2000)));

        assertThrows(BackupContents.NotABackupException.class, () -> BackupContents.read(zip, 1000));
        assertEquals(1, BackupContents.read(zip, 10_000).bindings().size(), "within the limit it is read");
    }

    @Test
    void aFileThatIsNotAZipIsNotABackup() throws IOException {
        Path file = temp.resolve("notes.zip");
        write(file, "not a zip");

        assertThrows(BackupContents.NotABackupException.class, () -> BackupContents.read(file));
    }

    @Test
    void aZipWithoutBackupInfoIsNotABackup() throws IOException {
        Path zip = zip("other.zip", Map.of("Bindings/Custom.4.2.binds", "<Custom/>"));

        assertThrows(BackupContents.NotABackupException.class, () -> BackupContents.read(zip));
    }

    @Test
    void aZipWithBackupInfoAndNoFileIsNotABackup() throws IOException {
        Path zip = zip("empty.zip", Map.of(Backups.INFO_FILE, "Taken: 2026-10-25 09:00:00\n"));

        assertThrows(BackupContents.NotABackupException.class, () -> BackupContents.read(zip));
    }

    @Test
    void aMissingFileIsNotABackup() {
        assertThrows(BackupContents.NotABackupException.class, () -> BackupContents.read(temp.resolve("gone.zip")));
    }

    /** A backup with only one half is still read - RESTORE decides whether it can be restored. */
    @Test
    void aBackupWithOnlyBindingsSaysItHasNoDeviceFiles() throws IOException {
        Path zip = zip("bindings-only.zip", Map.of(Backups.INFO_FILE, "Taken: x\n", "Bindings/StartPreset.4.start",
                "Custom"));

        BackupContents contents = BackupContents.read(zip);

        assertTrue(contents.hasBindings());
        assertFalse(contents.hasDeviceFiles());
    }

    // ---- helpers

    private Path installation(String name, String mappings, String... buttonMaps) throws IOException {
        Path root = temp.resolve(name);
        Path controlSchemes = root.resolve("Products/elite-dangerous-odyssey-64/ControlSchemes");
        write(controlSchemes.resolve("DeviceMappings.xml"), mappings);
        for (String buttonMap : buttonMaps) write(controlSchemes.resolve("DeviceButtonMaps").resolve(buttonMap), "<B/>");
        return root;
    }

    private Path zip(String name, Map<String, String> entries) throws IOException {
        Path zip = temp.resolve(name);
        try (OutputStream out = Files.newOutputStream(zip); ZipOutputStream stream = new ZipOutputStream(out)) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                stream.putNextEntry(new ZipEntry(entry.getKey()));
                stream.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                stream.closeEntry();
            }
        }
        return zip;
    }

    private static String text(byte[] content) {
        return new String(content, StandardCharsets.UTF_8);
    }

    private static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }
}
