package elite.intel.bindforge.install;

import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import elite.intel.db.managers.BindForgeInstallationsManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A rescan folds detection into the stored list without ever rebuilding it, which is what keeps hand-added
 * rows and device records alive across restarts.
 */
class InstallationRegistryTest {

    @TempDir
    Path machine;

    private final BindForgeInstallationsManager installations = BindForgeInstallationsManager.getInstance();
    private final List<GameInstallation> detected = new ArrayList<>();

    @BeforeEach
    void emptyTheList() {
        installations.findAll().forEach(row -> installations.remove(row.id()));
        detected.clear();
    }

    @Test
    void aRescanRecordsWhatDetectionFound() throws IOException {
        detect(Storefront.STEAM, gameAt("steam"));

        List<InstallationRow> rows = registry().rescan();

        assertEquals(1, rows.size());
        assertEquals("STEAM", rows.get(0).storefront());
        assertFalse(rows.get(0).addedByHand());
    }

    @Test
    void rescanningTwiceDoesNotDuplicateAnything() throws IOException {
        detect(Storefront.STEAM, gameAt("steam"));

        registry().rescan();
        List<InstallationRow> rows = registry().rescan();

        assertEquals(1, rows.size(), "the same folder is the same installation, however often it is found");
    }

    /**
     * The case the whole design turns on: detection cannot find a Frontier install that was moved, so absence
     * from its results must not mean missing. Judged by the folder instead.
     */
    @Test
    void aHandAddedInstallSurvivesRescansThatDoNotFindIt() throws IOException {
        Path frontier = gameAt("frontier");
        InstallationRegistry registry = registry();
        registry.addByHand(frontier);

        List<InstallationRow> rows = registry.rescan();

        assertEquals(1, rows.size(), "detection returned nothing, and the row is still here");
        assertTrue(rows.get(0).addedByHand());
        assertFalse(rows.get(0).missing(), "its folder is right there");
    }

    @Test
    void aRowWhoseFolderHasGoneIsMarkedMissingRatherThanDropped() throws IOException {
        Path steam = gameAt("steam");
        detect(Storefront.STEAM, steam);
        registry().rescan();

        detected.clear();
        deleteTree(steam);
        List<InstallationRow> rows = registry().rescan();

        assertEquals(1, rows.size(), "an unmounted drive and an uninstall look the same, so the row stays");
        assertTrue(rows.get(0).missing());
    }

    @Test
    void aFolderThatComesBackIsMarkedPresentAgain() throws IOException {
        Path steam = gameAt("steam");
        detect(Storefront.STEAM, steam);
        registry().rescan();
        deleteTree(steam);
        detected.clear();
        registry().rescan();

        gameAt("steam");
        List<InstallationRow> rows = registry().rescan();

        assertFalse(rows.get(0).missing(), "the drive was plugged back in");
    }

    @Test
    void addingAFolderThatIsNotAnInstallIsRefused() throws IOException {
        Path notAGame = Files.createDirectories(machine.resolve("Documents"));

        assertThrows(IllegalArgumentException.class, () -> registry().addByHand(notAGame));
        assertTrue(installations.findAll().isEmpty(), "nothing was recorded");
    }

    @Test
    void aHandAddedInstallIsNotDemotedWhenDetectionLaterFindsIt() throws IOException {
        Path folder = gameAt("frontier");
        InstallationRegistry registry = registry();
        registry.addByHand(folder);

        detect(Storefront.FRONTIER, folder);
        List<InstallationRow> rows = registry.rescan();

        assertEquals(1, rows.size());
        assertTrue(rows.get(0).addedByHand(), "it is still the only reason BindForge knows about it");
    }

    private InstallationRegistry registry() {
        return new InstallationRegistry(() -> List.copyOf(detected), installations);
    }

    private void detect(Storefront storefront, Path root) {
        detected.add(new GameInstallation(storefront, root, GameInstallation.controlSchemesUnder(root),
                machine.resolve("Bindings")));
    }

    /** Creates the Live product's ControlSchemes folder, which is what makes a folder an installation. */
    private Path gameAt(String name) throws IOException {
        Path root = machine.resolve(name);
        Files.createDirectories(GameInstallation.controlSchemesUnder(root));
        return root;
    }

    private void deleteTree(Path root) throws IOException {
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }
}
