package elite.intel.bindforge.storage;

import elite.intel.bindforge.install.GameInstallation;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static elite.intel.bindforge.storage.WorkingCopyTest.bytes;
import static elite.intel.bindforge.storage.WorkingCopyTest.write;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Writing one set to every live copy (storage.md rules 7-8; alias-designer.md, <em>What APPLY does to each
 * installation's device files</em>): device files whole to every installation, a matching file not rewritten, a
 * {@code .buttonMap} the set lacks removed except Frontier's, and everything it can written, reported place by place.
 */
class LiveCopiesTest {

    private static final FileTime LONG_AGO = FileTime.fromMillis(1_000_000_000_000L);

    @TempDir
    Path temp;

    private Path bindings;
    private Path steam;
    private Path epic;

    @BeforeEach
    void aBindingsFolderAndTwoInstallations() throws IOException {
        bindings = temp.resolve("Options/Bindings");
        Files.createDirectories(bindings);
        steam = temp.resolve("Steam/Elite Dangerous");
        epic = temp.resolve("Epic/EliteDangerous");
        Files.createDirectories(controlSchemes(steam));
        Files.createDirectories(controlSchemes(epic));
    }

    @Test
    void theSetGoesToTheBindingsFolderAndWholeToEveryInstallation() throws IOException {
        write(buttonMap(steam, "Old.buttonMap"), "<a device the set no longer has/>");

        WriteReport report = liveCopies(steam, epic).write(set(), LiveCopies.LeaveAlone.NONE);

        assertEquals("<Custom/>", Files.readString(bindings.resolve("Custom.4.2.binds")));
        assertEquals("Custom", Files.readString(bindings.resolve("StartPreset.4.start")));
        for (Path installation : List.of(steam, epic)) {
            assertEquals("<Mappings/>", Files.readString(GameInstallation.deviceMappingsIn(controlSchemes(installation))),
                    "a missing DeviceMappings.xml is simply written");
            assertEquals("<LVWAP/>", Files.readString(buttonMap(installation, "LVWAP.buttonMap")),
                    "DeviceButtonMaps is made where an installation has none");
        }
        assertFalse(Files.exists(buttonMap(steam, "Old.buttonMap")), "a .buttonMap the set lacks is removed");
        assertEquals(List.of("Old.buttonMap"), report.installations().getFirst().removed());
        assertTrue(report.complete());
        assertEquals(2, report.installationsUpdated());
        assertEquals(WriteReport.Kind.BINDINGS_FOLDER, report.bindings().kind());
        assertNull(report.bindings().storefront());
        assertEquals(WriteReport.Kind.INSTALLATION, report.installations().getFirst().kind());
        assertEquals("STEAM", report.installations().getFirst().storefront());
    }

    @Test
    void aFileThatAlreadyMatchesIsNotRewritten() throws IOException {
        write(GameInstallation.deviceMappingsIn(controlSchemes(steam)), "<Mappings/>");
        write(bindings.resolve("Custom.4.2.binds"), "<Custom/>");
        Files.setLastModifiedTime(bindings.resolve("Custom.4.2.binds"), LONG_AGO);

        WriteReport report = liveCopies(steam).write(set(), LiveCopies.LeaveAlone.NONE);

        assertEquals(LONG_AGO, Files.getLastModifiedTime(bindings.resolve("Custom.4.2.binds")));
        assertEquals(List.of("StartPreset.4.start"), report.bindings().written());
        assertEquals(List.of("LVWAP.buttonMap"), report.installations().getFirst().written(),
                "the matching DeviceMappings.xml is not written");
    }

    @Test
    void frontiersShippedButtonMapsAndOtherFilesAreNeverRemoved() throws IOException {
        write(buttonMap(steam, "VPCThrottle.buttonMap"), "<Frontier's/>");
        write(buttonMap(steam, "readme.txt"), "not a button map");
        write(controlSchemes(steam).resolve("KeyboardMouseOnly.binds"), "<factory/>");
        write(bindings.resolve("Testing.4.2.binds"), "<not in the set/>");

        liveCopies(steam).write(set(), LiveCopies.LeaveAlone.NONE);

        assertTrue(Files.exists(buttonMap(steam, "VPCThrottle.buttonMap")));
        assertTrue(Files.exists(buttonMap(steam, "readme.txt")));
        assertTrue(Files.exists(controlSchemes(steam).resolve("KeyboardMouseOnly.binds")));
        assertTrue(Files.exists(bindings.resolve("Testing.4.2.binds")),
                "a binds file the set lacks stays - removing one is a rename's (S12)");
    }

    @Test
    void anOldSpellingOfAKeptButtonMapEndsAsTheOneNewFile() throws IOException {
        write(buttonMap(steam, "lvwap.buttonMap"), "<old/>");

        liveCopies(steam).write(set(), LiveCopies.LeaveAlone.NONE);

        // WHY: holds whether or not the file system ignores case - on one that does, the old spelling is the new
        // file and must not be deleted; on one that heeds it, the old spelling is a file of its own and goes.
        assertEquals("<LVWAP/>", Files.readString(buttonMap(steam, "LVWAP.buttonMap")));
        try (Stream<Path> files = Files.list(GameInstallation.deviceButtonMapsIn(controlSchemes(steam)))) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void anInstallationThatCannotBeWrittenIsReportedAndTheOthersAreStillWritten() {
        Path gone = temp.resolve("Unplugged/EliteDangerous");

        WriteReport report = liveCopies(steam, gone, epic).write(set(), LiveCopies.LeaveAlone.NONE);

        assertEquals(2, report.installationsUpdated(), "updated 2 of 3");
        assertEquals(3, report.installations().size());
        WriteReport.Place missed = report.installations().get(1);
        assertFalse(missed.updated());
        assertEquals(List.of(LiveCopies.FOLDER_NOT_FOUND), missed.failures());
        assertFalse(report.complete());
        assertTrue(Files.exists(buttonMap(epic, "LVWAP.buttonMap")), "the one after it is still written");
    }

    /**
     * APPLY's leave-alone is asked only of an installation that is there: a missing one is not asked about but reported
     * as not found, so the user hears the drive is gone rather than "differs" (Storage S4a).
     */
    @Test
    void aMissingInstallationIsReportedNotFoundAndNeverAskedAbout() {
        Path gone = temp.resolve("Unplugged/EliteDangerous");
        List<Path> asked = new ArrayList<>();

        WriteReport report = liveCopies(steam, gone).write(set(), installation -> {
            asked.add(installation.root());
            return true;
        });

        assertEquals(List.of(steam), asked);
        WriteReport.Place leftAlone = report.installations().getFirst();
        assertTrue(leftAlone.differs());
        assertTrue(leftAlone.written().isEmpty(), "nothing is written to one left alone");
        assertFalse(Files.exists(buttonMap(steam, "LVWAP.buttonMap")));
        WriteReport.Place missing = report.installations().get(1);
        assertFalse(missing.differs());
        assertEquals(List.of(LiveCopies.FOLDER_NOT_FOUND), missing.failures());
    }

    @Test
    void aMissingBindingsFolderIsReportedNotCreated() throws IOException {
        Files.delete(bindings);

        WriteReport report = liveCopies(steam).write(set(), LiveCopies.LeaveAlone.NONE);

        assertEquals(List.of(LiveCopies.FOLDER_NOT_FOUND), report.bindings().failures());
        assertFalse(Files.exists(bindings), "creating it would put the files where the game never reads them");
        assertTrue(report.installations().getFirst().updated(), "the installations are still written");
    }

    @Test
    void aSetWithNoDeviceMappingsLeavesTheLiveOneAlone() throws IOException {
        write(GameInstallation.deviceMappingsIn(controlSchemes(steam)), "<Theirs/>");

        liveCopies(steam).write(new ConfigurationFiles(Map.of(), null, Map.of()), LiveCopies.LeaveAlone.NONE);

        assertEquals("<Theirs/>", Files.readString(GameInstallation.deviceMappingsIn(controlSchemes(steam))));
    }

    /**
     * A read-only binds file - set by players to stop the game resetting it - is written, and is read-only again
     * afterwards (storage.md rule 10; Alan, 2026-10-25) - Windows' flag there, the owner's write permission on Linux
     * (review, 2026-10-25). Only where the file system keeps either.
     */
    @Test
    void aReadOnlyLiveFileIsWrittenAndStaysReadOnly() throws IOException {
        Path binds = bindings.resolve("Custom.4.2.binds");
        write(binds, "<Old/>");
        protect(binds);

        WriteReport report = liveCopies().write(set(), LiveCopies.LeaveAlone.NONE);

        assertTrue(report.bindings().updated(), report.bindings().failures().toString());
        assertEquals("<Custom/>", Files.readString(binds));
        assertFalse(Files.isWritable(binds), "the protection is put back - asked of the file system, not ReadOnlyFlag");
        ReadOnlyFlag.set(binds, false);
    }

    /** A read-only file that already holds the bytes is not touched - its flag is not even cleared. */
    @Test
    void aReadOnlyFileWithTheSameBytesIsLeftAlone() throws IOException {
        Path binds = bindings.resolve("Custom.4.2.binds");
        write(binds, "<Custom/>");
        protect(binds);

        WriteReport report = liveCopies().write(set(), LiveCopies.LeaveAlone.NONE);

        assertFalse(report.bindings().written().contains("Custom.4.2.binds"));
        assertFalse(Files.isWritable(binds));
        ReadOnlyFlag.set(binds, false);
    }

    /**
     * A read-only {@code .buttonMap} the set lacks is kept on every system - on Linux the delete would succeed - and
     * reported so the screen can say how to let it go (Alan, 2026-10-25, from the review).
     */
    @Test
    void aReadOnlyButtonMapTheSetLacksIsKeptAndReported() throws IOException {
        Path old = buttonMap(steam, "Old.buttonMap");
        write(old, "<a device the set no longer has/>");
        protect(old);

        WriteReport report = liveCopies(steam).write(set(), LiveCopies.LeaveAlone.NONE);

        assertTrue(Files.exists(old));
        WriteReport.Place place = report.installations().getFirst();
        assertEquals(List.of("Old.buttonMap"), place.keptReadOnly());
        assertTrue(place.failures().isEmpty(), place.failures().toString());
        assertFalse(place.updated(), "the installation does not match the set");
        ReadOnlyFlag.set(old, false);
    }

    /** Protects a file as a player would, or skips the test where the file system cannot. */
    private static void protect(Path file) throws IOException {
        // WHY: root on Linux writes whatever a file's permission says, so nothing here could be seen.
        Assumptions.assumeFalse("root".equals(System.getProperty("user.name")), "root writes a protected file anyway");
        ReadOnlyFlag.set(file, true);
        Assumptions.assumeTrue(ReadOnlyFlag.isSet(file), "the file system keeps no read-only flag");
        // WHY: asked of the file system itself, so a ReadOnlyFlag looking at the wrong attribute fails here rather than
        // agreeing with itself (review, 2026-10-25).
        assertFalse(Files.isWritable(file), "ReadOnlyFlag protected the file the way the system sees it");
    }

    private LiveCopies liveCopies(Path... installations) {
        List<BackupSources.Installation> list = Stream.of(installations)
                .map(root -> new BackupSources.Installation("STEAM", root, false, null))
                .toList();
        return new LiveCopies(new BackupSources(bindings, list));
    }

    private static ConfigurationFiles set() {
        return new ConfigurationFiles(
                Map.of("Custom.4.2.binds", bytes("<Custom/>"), "StartPreset.4.start", bytes("Custom")),
                bytes("<Mappings/>"), Map.of("LVWAP.buttonMap", bytes("<LVWAP/>")));
    }

    private static Path controlSchemes(Path installation) {
        return GameInstallation.controlSchemesUnder(installation);
    }

    private static Path buttonMap(Path installation, String name) {
        return GameInstallation.deviceButtonMapsIn(controlSchemes(installation)).resolve(name);
    }
}
