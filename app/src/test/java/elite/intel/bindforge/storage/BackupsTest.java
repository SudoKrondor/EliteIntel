package elite.intel.bindforge.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The one backup system: what a backup holds, how the folder is read back as the list, and what retention deletes
 * (storage.md rules 11-12; file-manager.md, <em>How a backup folder is laid out</em>).
 */
class BackupsTest {

    @TempDir
    Path temp;

    private Path bindings;
    private Path steam;
    private Path epic;
    private Path root;

    @BeforeEach
    void aBindingsFolderAndTwoInstallations() throws IOException {
        bindings = temp.resolve("Options").resolve("Bindings");
        write(bindings.resolve("Custom.4.2.binds"), "<Root custom/>");
        write(bindings.resolve("Testing.4.2.binds"), "<Root testing/>");
        write(bindings.resolve("StartPreset.4.start"), "Custom\r\nCustom\r\nCustom\r\nCustom");
        steam = installation("steam", "<Steam mappings/>", "LVWAP.buttonMap", "VPCThrottle.buttonMap");
        epic = installation("epic", "<Epic mappings/>", "RVWAP.buttonMap");
        root = temp.resolve("backups");
    }

    @Test
    void aBackupHoldsEveryFileFromEveryInstallationLaidOutAsTheSpecSays() throws IOException {
        Backup backup = backups("2026-10-24T18:30:00Z", sources()).backUpEverything();

        Path folder = root.resolve("2026-10-24_18-30-00");
        assertEquals(folder, backup.folder());
        assertTrue(backup.full());
        assertFalse(backup.firstSetup());
        assertEquals("<Root custom/>", read(folder.resolve("Bindings/Custom.4.2.binds")));
        assertEquals("<Root testing/>", read(folder.resolve("Bindings/Testing.4.2.binds")),
                "every binds file, not only the active one");
        assertTrue(Files.exists(folder.resolve("Bindings/StartPreset.4.start")));
        assertEquals("<Steam mappings/>", read(folder.resolve("Installations/1-STEAM/ControlSchemes/DeviceMappings.xml")));
        assertTrue(Files.exists(folder.resolve("Installations/1-STEAM/ControlSchemes/DeviceButtonMaps/LVWAP.buttonMap")));
        assertTrue(Files.exists(folder.resolve("Installations/1-STEAM/ControlSchemes/DeviceButtonMaps/VPCThrottle.buttonMap")),
                "Frontier's shipped button maps too");
        assertEquals("<Epic mappings/>", read(folder.resolve("Installations/2-EPIC/ControlSchemes/DeviceMappings.xml")),
                "every installation, even when they match - no comparison first");
        assertTrue(Files.exists(folder.resolve("Installations/2-EPIC/ControlSchemes/DeviceButtonMaps/RVWAP.buttonMap")));

        String info = read(folder.resolve(Backups.INFO_FILE));
        assertTrue(info.contains("Status: Full"), info);
        assertTrue(info.contains(steam.toString()) && info.contains(epic.toString()),
                "each installation's folder is named, for a person reading the backup");
    }

    @Test
    void onlyTheGameInputConfigurationFilesAreTaken() throws IOException {
        write(bindings.resolve("StartPreset.start"), "old");
        write(bindings.resolve("Custom.4.2.binds.backup"), "copy");
        write(bindings.resolve("BindingLoadingErrors.log"), "log");
        write(bindings.resolve("Unversioned.binds"), "<Root/>");
        write(steam.resolve("Products/elite-dangerous-odyssey-64/ControlSchemes/KeyboardMouseOnly.binds"), "factory");
        write(steam.resolve("Products/elite-dangerous-odyssey-64/ControlSchemes/DeviceButtonMaps/notes.txt"), "x");

        Backup backup = backups("2026-10-24T18:30:00Z", sources()).backUpEverything();

        assertEquals(List.of("Custom.4.2.binds", "StartPreset.4.start", "Testing.4.2.binds"),
                names(backup.folder().resolve("Bindings")));
        assertEquals(List.of("DeviceButtonMaps", "DeviceMappings.xml"),
                names(backup.folder().resolve("Installations/1-STEAM/ControlSchemes")));
        assertEquals(List.of("LVWAP.buttonMap", "VPCThrottle.buttonMap"),
                names(backup.folder().resolve("Installations/1-STEAM/ControlSchemes/DeviceButtonMaps")));
    }

    @Test
    void anInstallationThatCannotBeReachedMakesItPartialAndIsNamed() throws IOException {
        BackupSources from = new BackupSources(bindings, List.of(
                new BackupSources.Installation("STEAM", steam, false, null),
                new BackupSources.Installation("EPIC", temp.resolve("unplugged drive"), true, null)));

        Backup backup = backups("2026-10-24T18:30:00Z", from).backUpEverything();

        assertFalse(backup.full());
        assertEquals(1, backup.notRead().size());
        assertTrue(backup.notRead().getFirst().contains("unplugged drive"), backup.notRead().toString());
        assertTrue(Files.exists(backup.folder().resolve("Installations/1-STEAM/ControlSchemes/DeviceMappings.xml")),
                "everything that could be read is still taken");

        Backup listed = backups("2026-10-24T18:31:00Z", from).list().getFirst();
        assertFalse(listed.full(), "Partial is read back from backup-info.txt");
        assertEquals(backup.notRead(), listed.notRead());
    }

    @Test
    void anInstallationsOwnBindingsFolderIsTakenWhenItIsNotTheMainOne() throws IOException {
        Path ownBindings = temp.resolve("pfx").resolve("Bindings");
        write(ownBindings.resolve("Proton.4.2.binds"), "<Root proton/>");
        BackupSources from = new BackupSources(bindings, List.of(
                new BackupSources.Installation("STEAM", steam, false, ownBindings),
                new BackupSources.Installation("EPIC", epic, false, bindings)));

        Backup backup = backups("2026-10-24T18:30:00Z", from).backUpEverything();

        assertEquals("<Root proton/>", read(backup.folder().resolve("Installations/1-STEAM/Bindings/Proton.4.2.binds")));
        assertFalse(Files.exists(backup.folder().resolve("Installations/2-EPIC/Bindings")),
                "the main bindings folder is taken once, under Bindings");
    }

    @Test
    void whenNothingCanBeReadNoBackupIsWritten() throws IOException {
        BackupSources from = new BackupSources(temp.resolve("no bindings"),
                List.of(new BackupSources.Installation("STEAM", temp.resolve("gone"), true, null)));

        Backups.NothingToBackUpException thrown = assertThrows(Backups.NothingToBackUpException.class,
                () -> backups("2026-10-24T18:30:00Z", from).backUpEverything());

        assertEquals(2, thrown.notRead().size());
        assertEquals(List.of(), names(root), "no folder, not even a half-taken one");
    }

    @Test
    void twoBackupsInOneSecondGetTwoFolders() throws IOException {
        Backups backups = backups("2026-10-24T18:30:00Z", sources());

        Path first = backups.backUpEverything().folder();
        Path second = backups.backUpEverything().folder();

        assertEquals(root.resolve("2026-10-24_18-30-00"), first);
        assertEquals(root.resolve("2026-10-24_18-30-00-2"), second);
        assertEquals(2, backups.list().size());
    }

    @Test
    void firstSetupsBackupIsMarkedInItsName() throws IOException {
        Backup backup = backups("2026-10-24T18:30:00Z", sources()).backUpEverything(Backups.Kind.FIRST_SETUP);

        assertEquals(root.resolve("2026-10-24_18-30-00_first-setup"), backup.folder());
        assertTrue(backup.firstSetup());
        assertTrue(backups("2026-10-24T18:31:00Z", sources()).list().getFirst().firstSetup());
    }

    @Test
    void theListIsNewestFirstAndLeavesOutOldFolderBackups() throws IOException {
        backups("2026-10-20T10:00:00Z", sources()).backUpEverything();
        backups("2026-10-24T09:00:00Z", sources()).backUpEverything();
        backups("2026-10-22T10:00:00Z", sources()).backUpEverything();
        Path v11 = root.resolve("2026-08-01_12-00-00");
        write(v11.resolve("Custom.4.2.binds"), "<Root/>");
        write(root.resolve("2026-10-13_09-00-00_before-bindforge/bindings/Custom.4.2.binds"), "<Root/>");
        write(root.resolve("2026-10-14_09-00-00_first-setup/1-steam/DeviceMappings.xml"), "<x/>");

        List<LocalDateTime> taken = backups("2026-10-24T10:00:00Z", sources()).list().stream()
                .map(Backup::taken).toList();

        assertEquals(List.of(LocalDateTime.parse("2026-10-24T09:00:00"), LocalDateTime.parse("2026-10-22T10:00:00"),
                LocalDateTime.parse("2026-10-20T10:00:00")), taken);
    }

    @Test
    void retentionDeletesOldBackupsButKeepsFirstSetupsRecentOnesAndOldFolderBackups() throws IOException {
        Path old = backups("2026-09-01T10:00:00Z", sources()).backUpEverything().folder();
        Path firstSetup = backups("2026-09-02T10:00:00Z", sources()).backUpEverything(Backups.Kind.FIRST_SETUP).folder();
        Path recent = backups("2026-10-20T10:00:00Z", sources()).backUpEverything().folder();
        Path v11 = root.resolve("2026-08-01_12-00-00");
        write(v11.resolve("Custom.4.2.binds"), "<Root/>");

        List<Path> deleted = backups("2026-10-24T10:00:00Z", sources()).prune(30);

        assertEquals(List.of(old), deleted);
        assertFalse(Files.exists(old));
        assertTrue(Files.exists(firstSetup), "first setup's backup is kept forever");
        assertTrue(Files.exists(recent));
        assertTrue(Files.exists(v11.resolve("Custom.4.2.binds")), "V1.1's folder backups are never deleted");
    }

    @Test
    void retentionCountsDaysFromTheStampInTheName() throws IOException {
        Path justInside = backups("2026-09-24T10:00:01Z", sources()).backUpEverything().folder();
        Path justOutside = backups("2026-09-24T09:59:59Z", sources()).backUpEverything().folder();

        backups("2026-10-24T10:00:00Z", sources()).prune(30);

        assertTrue(Files.exists(justInside));
        assertFalse(Files.exists(justOutside));
    }

    @Test
    void retentionRemovesAHalfTakenBackupACrashLeft() throws IOException {
        Path leftover = root.resolve(".taking-2026-10-23_10-00-00");
        write(leftover.resolve("Bindings/Custom.4.2.binds"), "<Root/>");

        backups("2026-10-24T10:00:00Z", sources()).prune(30);

        assertFalse(Files.exists(leftover));
    }

    /**
     * Review, 2026-10-24: a delete that stops part way - after taking {@code backup-info.txt}, which on Linux goes
     * first - must not leave a folder that is no longer a backup and so is never pruned again.
     */
    @Test
    void aDeleteThatStopsPartWayIsFinishedByTheNextPrune() throws IOException {
        Path old = backups("2026-09-01T10:00:00Z", sources()).backUpEverything().folder();
        Backups failingPartWay = new Backups(this::sources, () -> root,
                Clock.fixed(Instant.parse("2026-10-24T10:00:00Z"), ZoneOffset.UTC), folder -> {
            try (Stream<Path> found = Files.walk(folder)) {
                for (Path info : found.filter(p -> p.endsWith(Backups.INFO_FILE)).toList()) Files.delete(info);
            }
            throw new IOException("a file in the backup is locked");
        });

        failingPartWay.prune(30);

        assertFalse(Files.exists(old));
        List<String> leftover = names(root);
        assertEquals(1, leftover.size(), leftover.toString());
        assertTrue(leftover.getFirst().startsWith(".taking-"), "left for the sweep, not as a folder nobody prunes");

        backups("2026-10-25T10:00:00Z", sources()).prune(30);

        assertEquals(List.of(), names(root));
    }

    @Test
    void retentionBelowOneDayIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> backups("2026-10-24T10:00:00Z", sources()).prune(0));
    }

    // ---- helpers

    private Backups backups(String instant, BackupSources sources) {
        return new Backups(() -> sources, () -> {
            Files.createDirectories(root);
            return root;
        }, Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
    }

    private BackupSources sources() {
        return new BackupSources(bindings, List.of(
                new BackupSources.Installation("STEAM", steam, false, bindings),
                new BackupSources.Installation("EPIC", epic, false, bindings)));
    }

    private Path installation(String name, String mappings, String... buttonMaps) throws IOException {
        Path installRoot = temp.resolve(name);
        Path controlSchemes = installRoot.resolve("Products/elite-dangerous-odyssey-64/ControlSchemes");
        write(controlSchemes.resolve("DeviceMappings.xml"), mappings);
        for (String buttonMap : buttonMaps) {
            write(controlSchemes.resolve("DeviceButtonMaps").resolve(buttonMap), "<ButtonMap/>");
        }
        return installRoot;
    }

    private static List<String> names(Path folder) throws IOException {
        if (!Files.exists(folder)) return List.of();
        try (Stream<Path> files = Files.list(folder)) {
            return files.map(p -> p.getFileName().toString()).sorted().toList();
        }
    }

    private static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private static String read(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }
}
