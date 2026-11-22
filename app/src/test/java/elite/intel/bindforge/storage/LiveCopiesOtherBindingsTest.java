package elite.intel.bindforge.storage;

import elite.intel.bindforge.install.GameInstallation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static elite.intel.bindforge.storage.WorkingCopyTest.bytes;
import static elite.intel.bindforge.storage.WorkingCopyTest.write;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Each other installation's own bindings folder takes the write only while it holds what BindForge last wrote
 * (linux-several-installations.md, rule 5).
 */
class LiveCopiesOtherBindingsTest {

    /** What BindForge wrote last time, and what a folder that matches still holds. */
    private static final Map<String, byte[]> LAST = Map.of("Custom.4.2.binds", bytes("<Old/>"),
            "StartPreset.4.start", bytes("Custom"));

    @TempDir
    Path temp;

    private Path played;
    private Path steam;
    private Path epic;
    private Path epicBindings;

    @BeforeEach
    void aPlayedSteamInstallationAndAnEpicOneWithItsOwnFolder() throws IOException {
        played = temp.resolve("SteamPrefix/Bindings");
        epicBindings = temp.resolve("EpicPrefix/Bindings");
        Files.createDirectories(played);
        Files.createDirectories(epicBindings);
        steam = temp.resolve("Steam/Elite Dangerous");
        epic = temp.resolve("Epic/EliteDangerous");
        Files.createDirectories(GameInstallation.controlSchemesUnder(steam));
        Files.createDirectories(GameInstallation.controlSchemesUnder(epic));
    }

    @Test
    void aFolderThatStillHoldsTheLastWriteTakesTheNewSet() throws IOException {
        holdsLastWrite(epicBindings);

        WriteReport report = liveCopies().write(set(), LiveCopies.LeaveAlone.NONE, Set.of(), Set.of(),
                Optional.of(LAST));

        assertEquals("<Custom/>", Files.readString(epicBindings.resolve("Custom.4.2.binds")));
        WriteReport.Place place = report.otherBindings().getFirst();
        assertEquals(WriteReport.Kind.INSTALLATION_BINDINGS, place.kind());
        assertEquals("EPIC", place.storefront());
        assertEquals(epicBindings, place.folder());
        assertTrue(place.updated());
        assertTrue(report.complete());
    }

    @Test
    void aFolderChangedOutsideBindForgeIsLeftAlone() throws IOException {
        holdsLastWrite(epicBindings);
        write(epicBindings.resolve("Custom.4.2.binds"), "<Rebound in the game/>");

        WriteReport report = liveCopies().write(set(), LiveCopies.LeaveAlone.NONE, Set.of(), Set.of(),
                Optional.of(LAST));

        assertEquals("<Rebound in the game/>", Files.readString(epicBindings.resolve("Custom.4.2.binds")));
        WriteReport.Place place = report.otherBindings().getFirst();
        assertTrue(place.differs());
        assertNull(place.notWritten(), "changed outside BindForge is its own reason");
        assertFalse(report.complete(), "the report says so");
    }

    @Test
    void beforeAnyWorkingCopyEveryOtherFolderIsLeftAloneAsNotCompared() throws IOException {
        holdsLastWrite(epicBindings);

        WriteReport report = liveCopies().write(set(), LiveCopies.LeaveAlone.NONE, Set.of(), Set.of(),
                Optional.empty());

        assertEquals("<Old/>", Files.readString(epicBindings.resolve("Custom.4.2.binds")));
        WriteReport.Place place = report.otherBindings().getFirst();
        assertFalse(place.differs(), "nothing says the user changed it");
        assertEquals(WriteReport.NotWritten.NOT_COMPARED, place.notWritten());
        assertFalse(place.updated());
    }

    @Test
    void anInstallationWhoseDeviceFilesWereLeftAloneKeepsItsBindsFilesToo() throws IOException {
        holdsLastWrite(epicBindings);

        WriteReport report = liveCopies().write(set(), installation -> installation.root().equals(epic), Set.of(),
                Set.of(), Optional.of(LAST));

        assertEquals("<Old/>", Files.readString(epicBindings.resolve("Custom.4.2.binds")),
                "a binds file naming a device the installation does not know is rejected by the game");
        WriteReport.Place place = report.otherBindings().getFirst();
        assertEquals(WriteReport.NotWritten.DEVICE_FILES_NOT_TAKEN, place.notWritten());
        assertEquals(List.of(), place.failures(), "one reason, not a line per file");
        assertFalse(place.updated());
    }

    @Test
    void theRemovalsReachTheOtherFolderAndTheMovesDoNot() throws IOException {
        holdsLastWrite(epicBindings);
        write(epicBindings.resolve("Gone.4.0.binds"), "<renamed away/>");
        write(epicBindings.resolve("notes.txt"), "the user's");
        Map<String, byte[]> last = Map.of("Custom.4.2.binds", bytes("<Old/>"), "StartPreset.4.start", bytes("Custom"),
                "Gone.4.0.binds", bytes("<renamed away/>"));

        liveCopies().write(set(), LiveCopies.LeaveAlone.NONE, Set.of("Gone.4.0.binds"), Set.of(),
                Optional.of(last));

        assertFalse(Files.exists(epicBindings.resolve("Gone.4.0.binds")));
        assertTrue(Files.exists(epicBindings.resolve("notes.txt")), "only the checked folder's leftovers move");
    }

    @Test
    void aFolderThatIsTheBindingsFolderOrIsNotThereIsNoOtherPlace() throws IOException {
        Files.delete(epicBindings);
        List<BackupSources.Installation> installations = List.of(
                new BackupSources.Installation("STEAM", steam, false, played),
                new BackupSources.Installation("EPIC", epic, false, epicBindings));

        WriteReport report = new LiveCopies(new BackupSources(played, installations))
                .write(set(), LiveCopies.LeaveAlone.NONE, Set.of(), Set.of(), Optional.of(LAST));

        assertEquals(List.of(), report.otherBindings(), "the game makes the folder the first time it runs there");
        assertFalse(Files.exists(epicBindings), "never created");
    }

    private LiveCopies liveCopies() {
        return new LiveCopies(new BackupSources(played, List.of(
                new BackupSources.Installation("STEAM", steam, false, played),
                new BackupSources.Installation("EPIC", epic, false, epicBindings))));
    }

    private static void holdsLastWrite(Path folder) throws IOException {
        write(folder.resolve("Custom.4.2.binds"), "<Old/>");
        write(folder.resolve("StartPreset.4.start"), "Custom");
    }

    private static ConfigurationFiles set() {
        return new ConfigurationFiles(
                Map.of("Custom.4.2.binds", bytes("<Custom/>"), "StartPreset.4.start", bytes("Custom")),
                bytes("<Mappings/>"), Map.of());
    }
}
