package elite.intel.bindforge.storage;

import elite.intel.bindforge.install.GameInstallation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RESTORE (storage.md rule 10; Storage S3, Alan, 2026-10-25): the whole backup goes back - binds and preset files to
 * the bindings folder, one set of device files to every installation, one it does not hold included - and replaces
 * the working copy, after a before-RESTORE backup of what was there. Refused before anything is written with no
 * bindings folder or a backup missing a half.
 */
class RestoreTest {

    private static final String STEAM_NOW = mappings("<RVWAP><PID>03F5</PID><VID>3344</VID></RVWAP>");
    private static final String THEN = mappings("<LVWAP><PID>83F4</PID><VID>3344</VID></LVWAP>");
    private static final String OTHER = mappings("<RightStick><PID>03F5</PID><VID>3344</VID></RightStick>");

    @TempDir
    Path temp;

    private Path bindings;
    private Path steam;
    private Path epic;
    private Path backups;
    private Path workingCopy;
    private final FakeFlag flag = new FakeFlag();

    @BeforeEach
    void whatIsThereNow() throws IOException {
        bindings = temp.resolve("Options/Bindings");
        write(bindings.resolve("Custom.4.2.binds"), "<Now/>");
        write(bindings.resolve("StartPreset.4.start"), "Now");
        steam = installation("steam", STEAM_NOW, "Now.buttonMap");
        epic = installation("epic", null);
        backups = temp.resolve("backups");
        workingCopy = temp.resolve("working");
    }

    @Test
    void theWholeBackupGoesBackToEveryInstallationAndTheWorkingCopy() throws IOException {
        Path zip = backupZip(true, THEN);
        Restore restore = restore(List.of(steam, epic));

        Restore.Prepared prepared = restore.prepare(BackupContents.read(zip));
        assertTrue(prepared.identical(), "one installation in the backup - nothing to reconcile");
        Written written = restore.write(prepared, prepared.files());

        assertEquals("<Then/>", Files.readString(bindings.resolve("Custom.4.2.binds")));
        assertEquals("Then", Files.readString(bindings.resolve("StartPreset.4.start")));
        for (Path installation : List.of(steam, epic)) {
            assertEquals(THEN, Files.readString(GameInstallation.deviceMappingsIn(controlSchemes(installation))),
                    "every installation, one the backup does not hold included");
            assertTrue(Files.exists(buttonMap(installation, "Then.buttonMap")));
        }
        assertFalse(Files.exists(buttonMap(steam, "Now.buttonMap")), "the backup's set, whole");
        assertTrue(written.report().complete(), written.report().toString());

        ConfigurationFiles saved = new WorkingCopy(workingCopy).read();
        assertEquals("<Then/>", new String(saved.bindings().get("Custom.4.2.binds"), StandardCharsets.UTF_8),
                "the working copy is replaced too");
        assertEquals(THEN, new String(saved.deviceMappings(), StandardCharsets.UTF_8));
    }

    /** Principle 3: what was there is backed up first, as a before-RESTORE backup. */
    @Test
    void whatWasThereIsBackedUpFirstAsBeforeRestore() throws IOException {
        Restore restore = restore(List.of(steam, epic));
        Restore.Prepared prepared = restore.prepare(BackupContents.read(backupZip(true, THEN)));

        Written written = restore.write(prepared, prepared.files());

        assertEquals(Backups.Kind.BEFORE_CHANGES, written.backup().kind());
        assertEquals(Backups.Before.RESTORE, written.backup().before());
        assertEquals("<Now/>", entry(written.backup().zip(), "Bindings/Custom.4.2.binds"));
        assertEquals(STEAM_NOW, entry(written.backup().zip(), "Installations/1-STEAM/ControlSchemes/DeviceMappings.xml"));
    }

    /** RESTORE leaves the working copy and the live copies matching, as first-time startup does (Alan, 2026-10-25). */
    @Test
    void restoringBeforeFirstTimeStartupMarksItComplete() throws IOException {
        Restore restore = restore(List.of(steam));
        Restore.Prepared prepared = restore.prepare(BackupContents.read(backupZip(true, THEN)));
        assertFalse(flag.complete);

        restore.write(prepared, prepared.files());

        assertTrue(flag.complete);
    }

    /** Review, 2026-10-25: the user is told when first-time startup will be offered again. */
    @Test
    void aFlagThatCannotBeSetIsReportedAndTheRestoreStillStands() throws IOException {
        flag.failing = true;
        Restore restore = restore(List.of(steam));
        Restore.Prepared prepared = restore.prepare(BackupContents.read(backupZip(true, THEN)));

        Written restored = restore.write(prepared, prepared.files());

        assertTrue(restored.setUpAskedAgain());
        assertEquals("<Then/>", Files.readString(bindings.resolve("Custom.4.2.binds")), "the restore is written");
    }

    @Test
    void restoringWhenAlreadySetUpAsksNothingAgain() throws IOException {
        flag.complete = true;
        Restore restore = restore(List.of(steam));
        Restore.Prepared prepared = restore.prepare(BackupContents.read(backupZip(true, THEN)));

        assertFalse(restore.write(prepared, prepared.files()).setUpAskedAgain());
    }

    /** Review, 2026-10-25: whether each installation is there is decided while preparing, off the EDT. */
    @Test
    void theConfirmationsInstallationsSayWhetherEachWasFound() throws IOException {
        Path unplugged = temp.resolve("unplugged");
        Restore.Prepared prepared = restore(List.of(steam, unplugged)).prepare(BackupContents.read(backupZip(true, THEN)));

        assertEquals(List.of(true, false), prepared.targets().stream().map(Restore.Target::found).toList());
        assertEquals(bindings, prepared.bindingsFolder());
    }

    @Test
    void installationsHoldingDifferentDeviceFilesAreNotIdentical() throws IOException {
        Map<String, String> entries = baseEntries();
        entries.put("Installations/1-STEAM/ControlSchemes/DeviceMappings.xml", THEN);
        entries.put("Installations/2-EPIC/ControlSchemes/DeviceMappings.xml", OTHER);
        Restore restore = restore(List.of(steam, epic));

        Restore.Prepared prepared = restore.prepare(BackupContents.read(zip(entries)));

        assertFalse(prepared.identical(), "the user reconciles them, as in first setup");
        assertEquals(2, prepared.reading().installations().size());
    }

    @Test
    void noBindingsFolderIsRefusedBeforeAnythingIsWritten() throws IOException {
        Path zip = backupZip(true, THEN);
        bindings = temp.resolve("no such folder");
        Restore restore = restore(List.of(steam));

        assertThrows(FirstTimeStartup.NoBindingsYetException.class, () -> restore.prepare(BackupContents.read(zip)));
        assertFalse(Files.exists(backups), "no backup was taken");
    }

    @Test
    void aBackupWithNoBindsOrPresetFileIsRefused() throws IOException {
        Path zip = backupZip(false, THEN);

        Restore.MissingHalfException thrown = assertThrows(Restore.MissingHalfException.class,
                () -> restore(List.of(steam)).prepare(BackupContents.read(zip)));

        assertEquals(Restore.Half.BINDINGS, thrown.half());
    }

    @Test
    void aBackupWithNoDeviceFilesIsRefusedWhereThereAreInstallations() throws IOException {
        Path zip = backupZip(true, null);

        Restore.MissingHalfException thrown = assertThrows(Restore.MissingHalfException.class,
                () -> restore(List.of(steam)).prepare(BackupContents.read(zip)));

        assertEquals(Restore.Half.DEVICE_FILES, thrown.half());
        assertEquals("<Now/>", Files.readString(bindings.resolve("Custom.4.2.binds")), "nothing written");
    }

    /** With no installation known, a backup of the binds and preset files alone is all there is to restore. */
    @Test
    void aBackupWithNoDeviceFilesIsRestoredWhereThereAreNoInstallations() throws IOException {
        Restore restore = restore(List.of());
        Restore.Prepared prepared = restore.prepare(BackupContents.read(backupZip(true, null)));

        restore.write(prepared, prepared.files());

        assertEquals("<Then/>", Files.readString(bindings.resolve("Custom.4.2.binds")));
    }

    /**
     * Krondor's review, 2026-10-27: the write covers the installations RESTORE's confirmation named - one found only
     * after it was shown is not written.
     */
    @Test
    void anInstallationFoundAfterTheConfirmationIsNotWritten() throws IOException {
        List<BackupSources> found = new ArrayList<>(List.of(
                new BackupSources(bindings, List.of(new BackupSources.Installation("STEAM", steam, false, null))),
                new BackupSources(bindings, List.of(new BackupSources.Installation("STEAM", steam, false, null),
                        new BackupSources.Installation("EPIC", epic, false, null)))));
        Restore restore = restore(List.of(steam), found::removeFirst);
        Restore.Prepared prepared = restore.prepare(BackupContents.read(backupZip(true, THEN)));

        Written written = restore.write(prepared, prepared.files());

        assertEquals(1, written.report().installations().size());
        assertFalse(Files.exists(GameInstallation.deviceMappingsIn(controlSchemes(epic))), "never shown, never written");
    }

    // ---- helpers

    private Restore restore(List<Path> installations) {
        return restore(installations, null);
    }

    /** @param found what each look for installations finds, or {@code null} for the installations given, every time */
    private Restore restore(List<Path> installations, java.util.function.Supplier<BackupSources> found) {
        List<BackupSources.Installation> list = new ArrayList<>();
        String[] storefronts = {"STEAM", "EPIC"};
        for (int i = 0; i < installations.size(); i++) {
            list.add(new BackupSources.Installation(storefronts[i], installations.get(i), false, null));
        }
        BackupSources places = new BackupSources(bindings, list);
        Backups backupsOf = new Backups(() -> places, () -> backups,
                Clock.fixed(Instant.parse("2026-10-25T10:00:00Z"), ZoneOffset.UTC));
        WritePath writePath = new WritePath(new WorkingCopy(workingCopy), () -> places, files -> {
        });
        return new Restore(found != null ? found : () -> places, writePath::write,
                sources -> backupsOf.backUpBeforeChange(Backups.Before.RESTORE, sources), flag);
    }

    /** A backup holding the binds files or not, and one installation's device files or none. */
    private Path backupZip(boolean withBindings, String deviceMappings) throws IOException {
        Map<String, String> entries = withBindings ? baseEntries() : new LinkedHashMap<>(Map.of(Backups.INFO_FILE,
                "Taken: 2026-10-20 08:00:00\n"));
        if (deviceMappings != null) {
            entries.put("Installations/1-STEAM/ControlSchemes/DeviceMappings.xml", deviceMappings);
            entries.put("Installations/1-STEAM/ControlSchemes/DeviceButtonMaps/Then.buttonMap", "<Then/>");
        }
        return zip(entries);
    }

    private static Map<String, String> baseEntries() {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put(Backups.INFO_FILE, "Taken: 2026-10-20 08:00:00\n");
        entries.put("Bindings/Custom.4.2.binds", "<Then/>");
        entries.put("Bindings/StartPreset.4.start", "Then");
        return entries;
    }

    private Path zip(Map<String, String> entries) throws IOException {
        Path zip = temp.resolve("backup-" + System.nanoTime() + ".zip");
        try (OutputStream out = Files.newOutputStream(zip); ZipOutputStream stream = new ZipOutputStream(out)) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                stream.putNextEntry(new ZipEntry(entry.getKey()));
                stream.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                stream.closeEntry();
            }
        }
        return zip;
    }

    private Path installation(String name, String deviceMappings, String... buttonMaps) throws IOException {
        Path root = temp.resolve(name);
        Files.createDirectories(controlSchemes(root));
        if (deviceMappings != null) write(GameInstallation.deviceMappingsIn(controlSchemes(root)), deviceMappings);
        for (String buttonMap : buttonMaps) write(buttonMap(root, buttonMap), "<B/>");
        return root;
    }

    private static Path controlSchemes(Path installation) {
        return GameInstallation.controlSchemesUnder(installation);
    }

    private static Path buttonMap(Path installation, String name) {
        return GameInstallation.deviceButtonMapsIn(controlSchemes(installation)).resolve(name);
    }

    private static String mappings(String elements) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n<Root>" + elements + "</Root>\r\n";
    }

    private static String entry(Path zip, String name) throws IOException {
        try (ZipFile file = new ZipFile(zip.toFile())) {
            ZipEntry entry = file.getEntry(name);
            if (entry == null) return null;
            try (InputStream in = file.getInputStream(entry)) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
    }

    private static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private static final class FakeFlag implements FirstTimeStartup.Flag {
        boolean complete;
        boolean failing;

        @Override
        public boolean complete() {
            return complete;
        }

        @Override
        public void markComplete() {
            if (failing) throw new IllegalStateException("the database could not be written");
            complete = true;
        }

        @Override
        public void clear() {
            complete = false;
        }
    }
}
