package elite.intel.bindforge.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The one backup system: what a backup zip holds, the four kinds and their folders, how the folders are read back as
 * the list, what retention deletes, and Delete (storage.md rules 11-12; file-manager.md, <em>Four kinds of
 * backup</em> and <em>How the backup folder is laid out</em>).
 */
class BackupsTest {

    private static final Backups.Retention DEFAULT_RETENTION = new Backups.Retention(30, 100);

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
    void aBackupIsOneZipHoldingEveryFileFromEveryInstallationLaidOutAsTheSpecSays() throws IOException {
        Backup backup = backups("2026-10-24T18:30:00Z", sources()).backUpEverything(Backups.Kind.USER);

        assertEquals(root.resolve("User").resolve("2026-10-24_18-30-00.zip"), backup.zip());
        assertEquals(Backups.Kind.USER, backup.kind());
        assertTrue(backup.full());
        assertEquals("<Root custom/>", entry(backup, "Bindings/Custom.4.2.binds"));
        assertEquals("<Root testing/>", entry(backup, "Bindings/Testing.4.2.binds"),
                "every binds file, not only the active one");
        assertNotNull(entry(backup, "Bindings/StartPreset.4.start"));
        assertEquals("<Steam mappings/>", entry(backup, "Installations/1-STEAM/ControlSchemes/DeviceMappings.xml"));
        assertNotNull(entry(backup, "Installations/1-STEAM/ControlSchemes/DeviceButtonMaps/LVWAP.buttonMap"));
        assertNotNull(entry(backup, "Installations/1-STEAM/ControlSchemes/DeviceButtonMaps/VPCThrottle.buttonMap"),
                "Frontier's shipped button maps too");
        assertEquals("<Epic mappings/>", entry(backup, "Installations/2-EPIC/ControlSchemes/DeviceMappings.xml"),
                "every installation, even when they match - no comparison first");
        assertNotNull(entry(backup, "Installations/2-EPIC/ControlSchemes/DeviceButtonMaps/RVWAP.buttonMap"));

        String info = entry(backup, Backups.INFO_FILE);
        assertNotNull(info, "every backup holds backup-info.txt");
        assertTrue(info.contains("Status: Full"), info);
        assertTrue(info.contains("Kind: User"), info);
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

        Backup backup = backups("2026-10-24T18:30:00Z", sources()).backUpEverything(Backups.Kind.USER);

        assertEquals(List.of(
                "Bindings/Custom.4.2.binds",
                "Bindings/StartPreset.4.start",
                "Bindings/Testing.4.2.binds",
                "Installations/1-STEAM/ControlSchemes/DeviceButtonMaps/LVWAP.buttonMap",
                "Installations/1-STEAM/ControlSchemes/DeviceButtonMaps/VPCThrottle.buttonMap",
                "Installations/1-STEAM/ControlSchemes/DeviceMappings.xml",
                "Installations/2-EPIC/ControlSchemes/DeviceButtonMaps/RVWAP.buttonMap",
                "Installations/2-EPIC/ControlSchemes/DeviceMappings.xml",
                Backups.INFO_FILE), entries(backup));
    }

    @Test
    void eachKindGoesInItsOwnFolder() throws IOException {
        Backups backups = backups("2026-10-24T18:30:00Z", sources());

        assertEquals(root.resolve("Daily/2026-10-24_18-30-00.zip"), backups.backUpEverything(Backups.Kind.DAILY).zip());
        assertEquals(root.resolve("BeforeChanges/2026-10-24_18-30-00.zip"),
                backups.backUpBeforeChange(Backups.Before.APPLY, sources()).zip());
        assertEquals(root.resolve("User/2026-10-24_18-30-00.zip"), backups.backUpEverything(Backups.Kind.USER).zip());
        assertEquals(root.resolve("FirstSetup/2026-10-24_18-30-00.zip"),
                backups.backUpEverything(Backups.Kind.FIRST_SETUP).zip());
    }

    @Test
    void aBeforeChangesBackupRecordsWhatItWasTakenBefore() throws IOException {
        Backups backups = backups("2026-10-24T18:30:00Z", sources());
        Backup apply = backups.backUpBeforeChange(Backups.Before.APPLY, sources());
        backups("2026-10-24T18:31:00Z", sources()).backUpBeforeChange(Backups.Before.RESTORE, sources());
        backups("2026-10-24T18:32:00Z", sources()).backUpBeforeChange(Backups.Before.CHECK, sources());

        String info = entry(apply, Backups.INFO_FILE);
        assertNotNull(info, "every backup holds backup-info.txt");
        assertTrue(info.contains("Taken before: APPLY"), info);
        List<Backups.Before> listed = backups.list().stream().map(Backup::before).toList();
        assertEquals(List.of(Backups.Before.CHECK, Backups.Before.RESTORE, Backups.Before.APPLY), listed,
                "read back from backup-info.txt");
    }

    @Test
    void aBeforeChangesBackupIsNotTakenWithoutSayingWhatItIsBefore() {
        Backups backups = backups("2026-10-24T18:30:00Z", sources());

        assertThrows(IllegalArgumentException.class, () -> backups.backUpEverything(Backups.Kind.BEFORE_CHANGES));
        assertThrows(IllegalArgumentException.class, () -> backups.backUpBeforeChange(null, sources()));
    }

    @Test
    void anInstallationThatCannotBeReachedMakesItPartialAndIsNamed() throws IOException {
        BackupSources from = new BackupSources(bindings, List.of(
                new BackupSources.Installation("STEAM", steam, false, null),
                new BackupSources.Installation("EPIC", temp.resolve("unplugged drive"), true, null)));

        Backup backup = backups("2026-10-24T18:30:00Z", from).backUpEverything(Backups.Kind.DAILY);

        assertFalse(backup.full());
        assertEquals(1, backup.notRead().size());
        assertTrue(backup.notRead().getFirst().contains("unplugged drive"), backup.notRead().toString());
        assertNotNull(entry(backup, "Installations/1-STEAM/ControlSchemes/DeviceMappings.xml"),
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

        Backup backup = backups("2026-10-24T18:30:00Z", from).backUpEverything(Backups.Kind.USER);

        assertEquals("<Root proton/>", entry(backup, "Installations/1-STEAM/Bindings/Proton.4.2.binds"));
        assertTrue(entries(backup).stream().noneMatch(name -> name.startsWith("Installations/2-EPIC/Bindings/")),
                "the main bindings folder is taken once, under Bindings");
    }

    @Test
    void whenNothingCanBeReadNoBackupIsWritten() throws IOException {
        BackupSources from = new BackupSources(temp.resolve("no bindings"),
                List.of(new BackupSources.Installation("STEAM", temp.resolve("gone"), true, null)));

        Backups.NothingToBackUpException thrown = assertThrows(Backups.NothingToBackUpException.class,
                () -> backups("2026-10-24T18:30:00Z", from).backUpEverything(Backups.Kind.DAILY));

        assertEquals(2, thrown.notRead().size());
        assertEquals(List.of(), names(root.resolve("Daily")), "no zip, not even a half-written one");
    }

    @Test
    void twoBackupsOfOneKindInOneSecondGetTwoZips() throws IOException {
        Backups backups = backups("2026-10-24T18:30:00Z", sources());

        Path first = backups.backUpEverything(Backups.Kind.USER).zip();
        Path second = backups.backUpEverything(Backups.Kind.USER).zip();

        assertEquals(root.resolve("User/2026-10-24_18-30-00.zip"), first);
        assertEquals(root.resolve("User/2026-10-24_18-30-00-2.zip"), second);
        assertEquals(2, backups.list().size());
    }

    @Test
    void theListIsEveryKindNewestFirstAndLeavesOutFolderBackups() throws IOException {
        backups("2026-10-20T10:00:00Z", sources()).backUpEverything(Backups.Kind.DAILY);
        backups("2026-10-24T09:00:00Z", sources()).backUpEverything(Backups.Kind.USER);
        backups("2026-10-22T10:00:00Z", sources()).backUpBeforeChange(Backups.Before.APPLY, sources());
        backups("2026-10-13T10:00:00Z", sources()).backUpEverything(Backups.Kind.FIRST_SETUP);
        write(root.resolve("2026-08-01_12-00-00/Custom.4.2.binds"), "<Root/>");
        write(root.resolve("2026-10-13_09-00-00_before-bindforge/bindings/Custom.4.2.binds"), "<Root/>");
        write(root.resolve("2026-10-24_08-00-00/backup-info.txt"), "Status: Full");
        write(root.resolve("2026-10-24_08-00-00/Bindings/Custom.4.2.binds"), "<Root/>");
        write(root.resolve("Daily/notes.zip"), "not a backup");

        List<Backup> listed = backups("2026-10-24T10:00:00Z", sources()).list();

        assertEquals(List.of(LocalDateTime.parse("2026-10-24T09:00:00"), LocalDateTime.parse("2026-10-22T10:00:00"),
                LocalDateTime.parse("2026-10-20T10:00:00"), LocalDateTime.parse("2026-10-13T10:00:00")),
                listed.stream().map(Backup::taken).toList(), "S1's dated folders and V1.1's are not listed");
        assertEquals(List.of(Backups.Kind.USER, Backups.Kind.BEFORE_CHANGES, Backups.Kind.DAILY, Backups.Kind.FIRST_SETUP),
                listed.stream().map(Backup::kind).toList());
    }

    @Test
    void aDailyBackupStampedTodayIsFoundAndYesterdaysIsNot() throws IOException {
        Backups morning = backups("2026-10-24T08:00:00Z", sources());
        assertFalse(morning.dailyTakenToday());

        backups("2026-10-23T23:59:59Z", sources()).backUpEverything(Backups.Kind.DAILY);
        backups("2026-10-24T07:00:00Z", sources()).backUpEverything(Backups.Kind.USER);
        assertFalse(morning.dailyTakenToday(), "yesterday's daily and today's user backup are not today's daily");

        morning.backUpEverything(Backups.Kind.DAILY);
        assertTrue(backups("2026-10-24T22:00:00Z", sources()).dailyTakenToday());
        assertFalse(backups("2026-10-25T00:00:01Z", sources()).dailyTakenToday());
    }

    @Test
    void retentionDeletesOldDailyBackupsAndKeepsEveryOtherKind() throws IOException {
        Path oldDaily = backups("2026-09-01T10:00:00Z", sources()).backUpEverything(Backups.Kind.DAILY).zip();
        Path oldUser = backups("2026-09-01T10:00:00Z", sources()).backUpEverything(Backups.Kind.USER).zip();
        Path oldFirstSetup = backups("2026-09-02T10:00:00Z", sources()).backUpEverything(Backups.Kind.FIRST_SETUP).zip();
        Path oldBeforeChanges = backups("2026-09-01T10:00:00Z", sources()).backUpBeforeChange(Backups.Before.APPLY, sources()).zip();
        Path recentDaily = backups("2026-10-20T10:00:00Z", sources()).backUpEverything(Backups.Kind.DAILY).zip();
        Path s1Folder = root.resolve("2026-08-01_12-00-00");
        write(s1Folder.resolve("backup-info.txt"), "Status: Full");

        List<Path> deleted = backups("2026-10-24T10:00:00Z", sources()).prune(DEFAULT_RETENTION);

        assertEquals(List.of(oldDaily), deleted);
        assertTrue(Files.exists(oldUser), "a user backup is kept until the user deletes it");
        assertTrue(Files.exists(oldFirstSetup), "first setup's backup is kept forever");
        assertTrue(Files.exists(oldBeforeChanges), "before-changes backups are kept by count, not age");
        assertTrue(Files.exists(recentDaily));
        assertTrue(Files.exists(s1Folder.resolve("backup-info.txt")), "S1's folder backups are never deleted");
    }

    @Test
    void dailyRetentionCountsDaysFromTheStampInTheName() throws IOException {
        Path justInside = backups("2026-09-24T10:00:01Z", sources()).backUpEverything(Backups.Kind.DAILY).zip();
        Path justOutside = backups("2026-09-24T09:59:59Z", sources()).backUpEverything(Backups.Kind.DAILY).zip();

        backups("2026-10-24T10:00:00Z", sources()).prune(DEFAULT_RETENTION);

        assertTrue(Files.exists(justInside));
        assertFalse(Files.exists(justOutside));
    }

    @Test
    void beforeChangesRetentionKeepsTheNewestN() throws IOException {
        List<Path> taken = new ArrayList<>();
        for (int minute = 0; minute < 12; minute++) {
            taken.add(backups("2026-10-24T18:%02d:00Z".formatted(minute), sources())
                    .backUpBeforeChange(Backups.Before.APPLY, sources()).zip());
        }

        List<Path> deleted = backups("2026-10-24T19:00:00Z", sources()).prune(new Backups.Retention(30, 10));

        assertEquals(List.of(taken.get(1), taken.get(0)), deleted, "the two oldest go");
        for (Path kept : taken.subList(2, 12)) assertTrue(Files.exists(kept), kept.toString());
    }

    @Test
    void retentionRemovesAHalfWrittenZipACrashLeft() throws IOException {
        Path leftover = root.resolve("Daily/.2026-10-23_10-00-00.zip.elite-intel-1234.tmp");
        write(leftover, "half a zip");

        backups("2026-10-24T10:00:00Z", sources()).prune(DEFAULT_RETENTION);

        assertFalse(Files.exists(leftover));
    }

    @Test
    void aUserBackupCanBeDeletedAndNoOtherKindCan() throws IOException {
        Backups backups = backups("2026-10-24T18:30:00Z", sources());
        Backup user = backups.backUpEverything(Backups.Kind.USER);
        Backup daily = backups.backUpEverything(Backups.Kind.DAILY);
        Backup firstSetup = backups.backUpEverything(Backups.Kind.FIRST_SETUP);
        Backup beforeChanges = backups.backUpBeforeChange(Backups.Before.APPLY, sources());

        backups.delete(user);

        assertFalse(Files.exists(user.zip()));
        for (Backup other : List.of(daily, firstSetup, beforeChanges)) {
            assertThrows(IllegalArgumentException.class, () -> backups.delete(other));
            assertTrue(Files.exists(other.zip()));
        }
    }

    /**
     * A stamp-named file that is not a zip is still listed, as Partial - it is in the folder, and only its summary cannot
     * be read - and counts toward the before-changes N, so retention can delete it.
     */
    @Test
    void aDamagedZipIsListedAsPartialAndRetentionCountsIt() throws IOException {
        Path damaged = root.resolve("BeforeChanges/2026-10-24_10-00-00.zip");
        write(damaged, "not a zip");
        Path newer = backups("2026-10-24T11:00:00Z", sources()).backUpBeforeChange(Backups.Before.APPLY, sources()).zip();

        Backup listed = backups("2026-10-24T12:00:00Z", sources()).list().getLast();
        assertEquals(damaged, listed.zip());
        assertFalse(listed.full());
        assertNull(listed.before(), "what it came before cannot be read");
        assertTrue(listed.notRead().getFirst().startsWith(Backups.INFO_FILE), listed.notRead().toString());

        List<Path> deleted = backups("2026-10-24T12:00:00Z", sources()).prune(new Backups.Retention(30, 1));

        assertEquals(List.of(damaged), deleted);
        assertTrue(Files.exists(newer));
    }

    @Test
    void retentionBelowOneIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new Backups.Retention(0, 100));
        assertThrows(IllegalArgumentException.class, () -> new Backups.Retention(30, 0));
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

    /** The content of one entry in the backup's zip, or {@code null} when it has none by that name. */
    private static String entry(Backup backup, String name) throws IOException {
        try (ZipFile zip = new ZipFile(backup.zip().toFile())) {
            ZipEntry entry = zip.getEntry(name);
            if (entry == null) return null;
            try (InputStream in = zip.getInputStream(entry)) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
    }

    private static List<String> entries(Backup backup) throws IOException {
        try (ZipFile zip = new ZipFile(backup.zip().toFile())) {
            List<String> names = new ArrayList<>(Collections.list(zip.entries()).stream().map(ZipEntry::getName).toList());
            Collections.sort(names);
            return names;
        }
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
}
