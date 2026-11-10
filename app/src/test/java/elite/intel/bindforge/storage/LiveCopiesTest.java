package elite.intel.bindforge.storage;

import elite.intel.bindforge.install.GameInstallation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final List<Path> protectedFiles = new ArrayList<>();

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

        WriteReport report = liveCopies(steam, epic).write(set(), LiveCopies.LeaveAlone.NONE, Set.of());

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

        WriteReport report = liveCopies(steam).write(set(), LiveCopies.LeaveAlone.NONE, Set.of());

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

        liveCopies(steam).write(set(), LiveCopies.LeaveAlone.NONE, Set.of());

        assertTrue(Files.exists(buttonMap(steam, "VPCThrottle.buttonMap")));
        assertTrue(Files.exists(buttonMap(steam, "readme.txt")));
        assertTrue(Files.exists(controlSchemes(steam).resolve("KeyboardMouseOnly.binds")));
        assertTrue(Files.exists(bindings.resolve("Testing.4.2.binds")),
                "a binds file the set lacks stays - removing one is a rename's (S12)");
    }

    /** The working copy's spelling is the one left, on every file system (Alan, 2026-11-07, Storage S13a D4). */
    @Test
    void anOldSpellingOfAKeptButtonMapEndsAsTheOneNewFile() throws IOException {
        write(buttonMap(steam, "lvwap.buttonMap"), "<old/>");

        WriteReport report = liveCopies(steam).write(set(), LiveCopies.LeaveAlone.NONE, Set.of());

        assertEquals(List.of("LVWAP.buttonMap"), names(GameInstallation.deviceButtonMapsIn(controlSchemes(steam))));
        assertEquals("<LVWAP/>", Files.readString(buttonMap(steam, "LVWAP.buttonMap")));
        assertTrue(report.complete(), report.installations().getFirst().failures().toString());
    }

    /**
     * A live file whose name differs from the set's only in case is that file: renamed to the set's spelling, even when
     * it already holds the bytes, and named as written (Alan, 2026-11-07, Storage S13a D2).
     */
    @Test
    void aLiveFileInAnotherCaseIsLeftUnderTheSetsSpelling() throws IOException {
        write(bindings.resolve("custom.4.2.binds"), "<Custom/>");

        WriteReport report = liveCopies().write(set(), LiveCopies.LeaveAlone.NONE, Set.of());

        assertEquals(List.of("Custom.4.2.binds", "StartPreset.4.start"), names(bindings));
        assertEquals("<Custom/>", Files.readString(bindings.resolve("Custom.4.2.binds")));
        assertTrue(report.bindings().written().contains("Custom.4.2.binds"));
        assertTrue(report.bindings().removed().isEmpty(), "renamed, not removed");
        assertTrue(report.bindings().updated(), report.bindings().failures().toString());
    }

    /** A {@code devicemappings.xml} is the installation's file, replaced under the game's spelling (Storage S13a D5). */
    @Test
    void aDeviceMappingsInAnotherCaseIsReplacedUnderTheGamesSpelling() throws IOException {
        write(controlSchemes(steam).resolve("devicemappings.xml"), "<Old/>");

        liveCopies(steam).write(set(), LiveCopies.LeaveAlone.NONE, Set.of());

        assertEquals(List.of(GameInstallation.DEVICE_MAPPINGS), names(controlSchemes(steam)));
        assertEquals("<Mappings/>", Files.readString(GameInstallation.deviceMappingsIn(controlSchemes(steam))));
    }

    /**
     * Two spellings of one file - only where the file system heeds case, made outside BindForge: the set's spelling is
     * written and the other removed and reported (Alan, 2026-11-07, Storage S13a D3).
     */
    @Test
    void twoSpellingsOfOneFileLeaveOne() throws IOException {
        write(bindings.resolve("Custom.4.2.binds"), "<Mine/>");
        write(bindings.resolve("custom.4.2.binds"), "<Theirs/>");
        Assumptions.assumeTrue(names(bindings).size() == 2, "the file system ignores case, so it holds one file");

        WriteReport report = liveCopies().write(set(), LiveCopies.LeaveAlone.NONE, Set.of());

        assertEquals(List.of("Custom.4.2.binds", "StartPreset.4.start"), names(bindings));
        assertEquals("<Custom/>", Files.readString(bindings.resolve("Custom.4.2.binds")));
        assertEquals(List.of("custom.4.2.binds"), report.bindings().removed());
    }

    /**
     * A respelling whose first step fails: the file is written under its old spelling, never as a second file beside
     * it, the failure is reported, and the next write renames it (Storage S13aR, D1).
     */
    @Test
    void aRespellingThatCannotStartWritesTheFileUnderItsOldSpelling() throws IOException {
        write(bindings.resolve("custom.4.2.binds"), "<Old/>");
        LiveCopies.Mover refusing = (from, to) -> {
            throw new IOException("refused");
        };

        WriteReport report = liveCopies(refusing).write(set(), LiveCopies.LeaveAlone.NONE, Set.of());

        assertOldSpellingWritten(report);
        assertSpelledAsTheSetAtTheNextWrite();
    }

    /**
     * A respelling whose second step fails: the file is put back under its old spelling and written there (Storage
     * S13aR, D1).
     */
    @Test
    void aRespellingThatCannotFinishWritesTheFileUnderItsOldSpelling() throws IOException {
        write(bindings.resolve("custom.4.2.binds"), "<Old/>");
        LiveCopies.Mover refusingTheNewSpelling = (from, to) -> {
            if (to.getFileName().toString().equals("Custom.4.2.binds")) throw new IOException("refused");
            Files.move(from, to);
        };

        WriteReport report = liveCopies(refusingTheNewSpelling).write(set(), LiveCopies.LeaveAlone.NONE, Set.of());

        assertOldSpellingWritten(report);
        assertSpelledAsTheSetAtTheNextWrite();
    }

    private void assertOldSpellingWritten(WriteReport report) throws IOException {
        // WHY: the preset file is held back, as after any failure in the bindings folder.
        assertEquals(List.of("custom.4.2.binds"), names(bindings), "one file, under the old spelling");
        assertEquals("<Custom/>", Files.readString(bindings.resolve("custom.4.2.binds")));
        assertTrue(report.bindings().failures().stream().anyMatch(line -> line.contains("refused")),
                report.bindings().failures().toString());
        assertTrue(report.bindings().removed().isEmpty());
    }

    private void assertSpelledAsTheSetAtTheNextWrite() throws IOException {
        WriteReport again = liveCopies().write(set(), LiveCopies.LeaveAlone.NONE, Set.of());

        assertEquals(List.of("Custom.4.2.binds", "StartPreset.4.start"), names(bindings));
        assertTrue(again.bindings().updated(), again.bindings().failures().toString());
    }

    /** A file named for removal is removed in whatever case it is spelled on disk (Storage S13a). */
    @Test
    void aRemovalFindsTheFileInAnotherCase() throws IOException {
        write(bindings.resolve("spare.4.2.binds"), "<Spare/>");

        WriteReport report = liveCopies().write(set(), LiveCopies.LeaveAlone.NONE, Set.of("Spare.4.2.binds"));

        assertEquals(List.of("Custom.4.2.binds", "StartPreset.4.start"), names(bindings));
        assertEquals(List.of("spare.4.2.binds"), report.bindings().removed());
    }

    @Test
    void anInstallationThatCannotBeWrittenIsReportedAndTheOthersAreStillWritten() {
        Path gone = temp.resolve("Unplugged/EliteDangerous");

        WriteReport report = liveCopies(steam, gone, epic).write(set(), LiveCopies.LeaveAlone.NONE, Set.of());

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
        }, Set.of());

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

        WriteReport report = liveCopies(steam).write(set(), LiveCopies.LeaveAlone.NONE, Set.of());

        assertEquals(List.of(LiveCopies.FOLDER_NOT_FOUND), report.bindings().failures());
        assertFalse(Files.exists(bindings), "creating it would put the files where the game never reads them");
        assertTrue(report.installations().getFirst().updated(), "the installations are still written");
    }

    @Test
    void aSetWithNoDeviceMappingsLeavesTheLiveOneAlone() throws IOException {
        write(GameInstallation.deviceMappingsIn(controlSchemes(steam)), "<Theirs/>");

        liveCopies(steam).write(new ConfigurationFiles(Map.of(), null, Map.of()), LiveCopies.LeaveAlone.NONE, Set.of());

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

        WriteReport report = liveCopies().write(set(), LiveCopies.LeaveAlone.NONE, Set.of());

        assertTrue(report.bindings().updated(), report.bindings().failures().toString());
        assertEquals("<Custom/>", Files.readString(binds));
        assertFalse(Files.isWritable(binds), "the protection is put back - asked of the file system, not ReadOnlyFlag");
    }

    /** A read-only file that already holds the bytes is not touched - its flag is not even cleared. */
    @Test
    void aReadOnlyFileWithTheSameBytesIsLeftAlone() throws IOException {
        Path binds = bindings.resolve("Custom.4.2.binds");
        write(binds, "<Custom/>");
        protect(binds);

        WriteReport report = liveCopies().write(set(), LiveCopies.LeaveAlone.NONE, Set.of());

        assertFalse(report.bindings().written().contains("Custom.4.2.binds"));
        assertFalse(Files.isWritable(binds));
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

        WriteReport report = liveCopies(steam).write(set(), LiveCopies.LeaveAlone.NONE, Set.of());

        assertTrue(Files.exists(old));
        WriteReport.Place place = report.installations().getFirst();
        assertEquals(List.of("Old.buttonMap"), place.keptReadOnly());
        assertTrue(place.failures().isEmpty(), place.failures().toString());
        assertFalse(place.updated(), "the installation does not match the set");
    }

    /**
     * The check removes a live-only binds file the user kept the working copy's side of; nothing else the set lacks is
     * touched (Alan, 2026-10-30, Storage S8).
     */
    @Test
    void aBindsFileNamedForRemovalIsRemovedAndOthersTheSetLacksStay() throws IOException {
        write(bindings.resolve("Spare.4.2.binds"), "<Spare/>");
        write(bindings.resolve("Other.4.2.binds"), "<Other/>");

        WriteReport report = liveCopies().write(set(), LiveCopies.LeaveAlone.NONE, Set.of("Spare.4.2.binds"));

        assertFalse(Files.exists(bindings.resolve("Spare.4.2.binds")));
        assertTrue(Files.exists(bindings.resolve("Other.4.2.binds")), "APPLY's rule: a .binds the set lacks stays");
        assertEquals(List.of("Spare.4.2.binds"), report.bindings().removed());
        assertTrue(report.bindings().updated());
    }

    @Test
    void aReadOnlyBindsFileNamedForRemovalIsKeptAndReported() throws IOException {
        Path spare = bindings.resolve("Spare.4.2.binds");
        write(spare, "<Spare/>");
        protect(spare);

        WriteReport report = liveCopies().write(set(), LiveCopies.LeaveAlone.NONE, Set.of("Spare.4.2.binds"));

        assertTrue(Files.exists(spare));
        assertEquals(List.of("Spare.4.2.binds"), report.bindings().keptReadOnly());
        assertFalse(report.bindings().updated());
    }

    /**
     * A removal differing only in case from a file written is skipped: on Windows it is the file just written
     * (Storage S12).
     */
    @Test
    void aRemovalDifferingOnlyInCaseFromAFileWrittenIsSkipped() {
        WriteReport report = liveCopies().write(set(), LiveCopies.LeaveAlone.NONE, Set.of("custom.4.2.binds"));

        assertTrue(Files.exists(bindings.resolve("Custom.4.2.binds")));
        assertEquals(List.of(), report.bindings().removed());
    }

    /**
     * A removal differing only in case from a file written leaves one file, under the set's spelling, on any file system
     * (review, 2026-11-03; Storage S13a).
     */
    @Test
    void aCaseOnlyChangeLeavesOneFile() throws IOException {
        write(bindings.resolve("Custom.4.2.binds"), "<Custom/>");
        ConfigurationFiles renamed = new ConfigurationFiles(
                Map.of("custom.4.2.binds", bytes("<custom/>"), "StartPreset.4.start", bytes("custom")),
                bytes("<Mappings/>"), Map.of());

        WriteReport report = liveCopies().write(renamed, LiveCopies.LeaveAlone.NONE, Set.of("Custom.4.2.binds"));

        assertEquals(List.of("StartPreset.4.start", "custom.4.2.binds"), names(bindings));
        assertEquals("<custom/>", Files.readString(bindings.resolve("custom.4.2.binds")));
        assertTrue(report.bindings().updated(), report.bindings().failures().toString());
    }

    /**
     * A new file that cannot be written: the old one stays, and the preset file keeps naming it, so the game is not left
     * on a line naming a file that is not there (review, 2026-11-03).
     */
    @Test
    void aBindsFileThatFailsHoldsBackThePresetFileAndTheRemoval() throws IOException {
        write(bindings.resolve("Custom.4.2.binds"), "<Custom/>");
        write(bindings.resolve("StartPreset.4.start"), "Custom");
        write(bindings.resolve("Mine.4.2.binds").resolve("blocker"), "a folder sits where the new file goes");
        ConfigurationFiles renamed = new ConfigurationFiles(
                Map.of("Mine.4.2.binds", bytes("<Mine/>"), "StartPreset.4.start", bytes("Mine")), bytes("<Mappings/>"),
                Map.of());

        WriteReport report = liveCopies().write(renamed, LiveCopies.LeaveAlone.NONE, Set.of("Custom.4.2.binds"));

        assertEquals("<Custom/>", Files.readString(bindings.resolve("Custom.4.2.binds")));
        assertEquals("Custom", Files.readString(bindings.resolve("StartPreset.4.start")));
        assertEquals(List.of(), report.bindings().removed());
        assertFalse(report.bindings().updated());
        assertTrue(report.bindings().failures().contains("StartPreset.4.start: " + LiveCopies.HELD_BACK));
        assertTrue(report.bindings().failures().contains("Custom.4.2.binds: " + LiveCopies.HELD_BACK));
    }

    /** A name that is not a binds or preset file's is refused - the removal never leaves the bindings folder. */
    @Test
    void aRemovalThatIsNotABindsFileNameIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> liveCopies().write(set(), LiveCopies.LeaveAlone.NONE, Set.of("..\\Custom.4.2.binds")));
        assertThrows(IllegalArgumentException.class,
                () -> liveCopies().write(set(), LiveCopies.LeaveAlone.NONE, Set.of("notes.txt")));
        assertThrows(IllegalArgumentException.class,
                () -> liveCopies().write(set(), LiveCopies.LeaveAlone.NONE, Set.of("Custom.binds")),
                "a plain binds file is moved, never removed (Storage S13d)");
    }

    /**
     * What does not belong in the bindings folder is moved, never deleted, into one date-stamped folder in Leftover User
     * Files for the write (Storage S13d) - a read-only one too: the flag protects a file from being deleted, and this
     * one is kept (Alan, 2026-11-09, Storage S13eR). The move changes no flag, so it arrives read-only (Storage S13eR2).
     */
    @Test
    void filesNamedToMoveGoToOneStampedFolderWhateverTheirReadOnlyFlag() throws IOException {
        write(bindings.resolve("Spare.binds"), "<Spare/>");
        write(bindings.resolve("Custom.4.2 (2).binds"), "<copy/>");
        Path readOnly = bindings.resolve("KeyboardMouseOnly.binds");
        write(readOnly, "<KeyboardMouseOnly/>");
        protect(readOnly);
        Path leftovers = temp.resolve("Leftover User Files");

        WriteReport report = movingTo(leftovers).write(set(), LiveCopies.LeaveAlone.NONE, Set.of(),
                Set.of("Spare.binds", "Custom.4.2 (2).binds", "KeyboardMouseOnly.binds"));

        WriteReport.Place place = report.bindings();
        // WHY: the moved copy keeps its flag, so it is unprotected after the test too, or @TempDir cannot delete it.
        if (place.movedTo() != null) protectedFiles.add(place.movedTo().resolve("KeyboardMouseOnly.binds"));
        assertEquals(List.of("Custom.4.2 (2).binds", "KeyboardMouseOnly.binds", "Spare.binds"),
                place.moved().stream().sorted().toList());
        try (Stream<Path> made = Files.list(leftovers)) {
            assertEquals(List.of(place.movedTo()), made.toList(), "one folder for the write");
        }
        assertTrue(place.movedTo().getFileName().toString().matches("\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}"),
                place.movedTo().toString());
        assertEquals("<Spare/>", Files.readString(place.movedTo().resolve("Spare.binds")));
        assertFalse(Files.exists(bindings.resolve("Spare.binds")));
        assertFalse(Files.exists(bindings.resolve("Custom.4.2 (2).binds")));
        assertFalse(Files.exists(readOnly));
        assertEquals("<KeyboardMouseOnly/>", Files.readString(place.movedTo().resolve("KeyboardMouseOnly.binds")));
        assertTrue(ReadOnlyFlag.isSet(place.movedTo().resolve("KeyboardMouseOnly.binds")),
                "the move changes no flag (Storage S13eR2)");
        assertEquals(List.of(), place.keptReadOnly());
        assertEquals(List.of(), place.failures());
        assertEquals(List.of(), place.removed());
    }

    /** Each write gets a folder of its own, two in one second included. */
    @Test
    void aSecondMoveInTheSameSecondGetsAFolderOfItsOwn() throws IOException {
        Path leftovers = temp.resolve("Leftover User Files");
        write(bindings.resolve("a.txt"), "a");
        WriteReport first = movingTo(leftovers).write(set(), LiveCopies.LeaveAlone.NONE, Set.of(), Set.of("a.txt"));
        Path taken = first.bindings().movedTo();
        write(bindings.resolve("b.txt"), "b");

        WriteReport second = movingTo(leftovers).write(set(), LiveCopies.LeaveAlone.NONE, Set.of(), Set.of("b.txt"));

        assertNotEquals(taken, second.bindings().movedTo());
        assertTrue(Files.exists(second.bindings().movedTo().resolve("b.txt")));
    }

    /** Nothing moved, no folder made - and none named. */
    @Test
    void nothingToMoveMakesNoFolder() {
        Path leftovers = temp.resolve("Leftover User Files");

        WriteReport report = movingTo(leftovers).write(set(), LiveCopies.LeaveAlone.NONE, Set.of(),
                Set.of("gone.txt"));

        assertEquals(List.of(), report.bindings().moved());
        assertNull(report.bindings().movedTo());
        assertFalse(Files.exists(leftovers));
    }

    /**
     * A move that fails leaves the file where it is, reported, and no empty date-stamped folder behind - the next check
     * tries the file again, and each try would leave one more.
     */
    @Test
    void aMoveThatFailsLeavesTheFileAndIsReported() throws IOException {
        write(bindings.resolve("a.txt"), "a");
        Path leftovers = temp.resolve("Leftover User Files");
        LiveCopies failing = new LiveCopies(new BackupSources(bindings, List.of()), (from, to) -> {
            throw new IOException("disk full");
        }, () -> leftovers);

        WriteReport report = failing.write(set(), LiveCopies.LeaveAlone.NONE, Set.of(), Set.of("a.txt"));

        assertTrue(Files.exists(bindings.resolve("a.txt")));
        assertEquals(List.of(), report.bindings().moved());
        assertNull(report.bindings().movedTo());
        assertTrue(report.bindings().failures().stream().anyMatch(line -> line.startsWith("a.txt: not moved")),
                report.bindings().failures().toString());
        try (Stream<Path> made = Files.list(leftovers)) {
            assertEquals(List.of(), made.toList(), "the folder made for the failed move is taken away again");
        }
    }

    /** A later move that fails keeps the folder: what moved before it is there. */
    @Test
    void aMoveThatFailsAfterOneThatWorkedKeepsTheFolder() throws IOException {
        write(bindings.resolve("a.txt"), "a");
        write(bindings.resolve("b.txt"), "b");
        LiveCopies.Mover refusingB = (from, to) -> {
            if (from.getFileName().toString().equals("b.txt")) throw new IOException("disk full");
            Files.move(from, to);
        };
        LiveCopies partly = new LiveCopies(new BackupSources(bindings, List.of()), refusingB,
                () -> temp.resolve("Leftover User Files"));

        WriteReport report = partly.write(set(), LiveCopies.LeaveAlone.NONE, Set.of(),
                new LinkedHashSet<>(List.of("a.txt", "b.txt")));

        assertEquals(List.of("a.txt"), report.bindings().moved());
        assertEquals("a", Files.readString(report.bindings().movedTo().resolve("a.txt")));
        assertTrue(Files.exists(bindings.resolve("b.txt")));
    }

    /** A binds file that fails holds the moves back too: a converted plain file's binds file may be the one. */
    @Test
    void aBindsFileThatFailsHoldsBackTheMoves() throws IOException {
        write(bindings.resolve("Spare.binds"), "<Spare/>");
        write(bindings.resolve("Custom.4.2.binds").resolve("blocker"), "a folder sits where the file goes");

        WriteReport report = movingTo(temp.resolve("Leftover User Files")).write(set(), LiveCopies.LeaveAlone.NONE,
                Set.of(), Set.of("Spare.binds"));

        assertTrue(Files.exists(bindings.resolve("Spare.binds")));
        assertTrue(report.bindings().failures().contains("Spare.binds: " + LiveCopies.HELD_BACK));
    }

    /** Only a file that does not belong is moved - never the game's own, and never out of the folder. */
    @Test
    void aMoveOfAGameFileOrAPathIsRefused() throws IOException {
        LiveCopies moving = movingTo(temp.resolve("Leftover User Files"));
        assertThrows(IllegalArgumentException.class,
                () -> moving.write(set(), LiveCopies.LeaveAlone.NONE, Set.of(), Set.of("custom.4.2.binds")));
        assertThrows(IllegalArgumentException.class,
                () -> moving.write(set(), LiveCopies.LeaveAlone.NONE, Set.of(), Set.of("StartPreset.4.start")));
        assertThrows(IllegalArgumentException.class,
                () -> moving.write(set(), LiveCopies.LeaveAlone.NONE, Set.of(), Set.of("..\\notes.txt")));
        assertThrows(IllegalArgumentException.class,
                () -> moving.write(set(), LiveCopies.LeaveAlone.NONE, Set.of("notes.txt"), Set.of()));
        assertEquals(List.of(), names(bindings), "refused before anything is written (the 2026-11-09 review)");
    }

    /**
     * Every protected file goes back to writable, whether the test passed or not - else {@code @TempDir} cannot delete it
     * on Windows, and a failed assertion turns into a cleanup error that hides it (Krondor's review, 2026-10-27).
     */
    @AfterEach
    void unprotectEverything() throws IOException {
        for (Path file : protectedFiles) {
            if (Files.exists(file)) ReadOnlyFlag.set(file, false);
        }
    }

    /** Protects a file as a player would, or skips the test where the file system cannot. */
    private void protect(Path file) throws IOException {
        // WHY: root on Linux writes whatever a file's permission says, so nothing here could be seen.
        Assumptions.assumeFalse("root".equals(System.getProperty("user.name")), "root writes a protected file anyway");
        protectedFiles.add(file);
        ReadOnlyFlag.set(file, true);
        Assumptions.assumeTrue(ReadOnlyFlag.isSet(file), "the file system keeps no read-only flag");
        // WHY: asked of the file system itself, so a ReadOnlyFlag looking at the wrong attribute fails here rather than
        // agreeing with itself (review, 2026-10-25).
        assertFalse(Files.isWritable(file), "ReadOnlyFlag protected the file the way the system sees it");
    }

    private LiveCopies liveCopies(Path... installations) {
        return liveCopies(LiveCopies.Mover.FILES, installations);
    }

    /** No installations, moving to {@code leftovers}. */
    private LiveCopies movingTo(Path leftovers) {
        return new LiveCopies(new BackupSources(bindings, List.of()), LiveCopies.Mover.FILES, () -> leftovers);
    }

    private LiveCopies liveCopies(LiveCopies.Mover mover, Path... installations) {
        List<BackupSources.Installation> list = Stream.of(installations)
                .map(root -> new BackupSources.Installation("STEAM", root, false, null))
                .toList();
        return new LiveCopies(new BackupSources(bindings, list), mover);
    }

    private static ConfigurationFiles set() {
        return new ConfigurationFiles(
                Map.of("Custom.4.2.binds", bytes("<Custom/>"), "StartPreset.4.start", bytes("Custom")),
                bytes("<Mappings/>"), Map.of("LVWAP.buttonMap", bytes("<LVWAP/>")));
    }

    /** The files in a folder as spelled on disk, in plain name order. */
    private static List<String> names(Path folder) throws IOException {
        try (Stream<Path> files = Files.list(folder)) {
            return files.filter(Files::isRegularFile).map(file -> file.getFileName().toString()).sorted().toList();
        }
    }

    private static Path controlSchemes(Path installation) {
        return GameInstallation.controlSchemesUnder(installation);
    }

    private static Path buttonMap(Path installation, String name) {
        return GameInstallation.deviceButtonMapsIn(controlSchemes(installation)).resolve(name);
    }
}
