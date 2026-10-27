package elite.intel.bindforge.install;

import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import elite.intel.db.managers.BindForgeInstallationsManager;
import elite.intel.db.managers.InstallationsTable;
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
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A rescan folds detection into the stored list without ever rebuilding it, which is what keeps hand-added
 * and removed rows as the user left them across restarts.
 */
class InstallationRegistryTest {

    @TempDir
    Path machine;

    private final BindForgeInstallationsManager installations = BindForgeInstallationsManager.getInstance();
    private final List<GameInstallation> detected = new ArrayList<>();

    @BeforeEach
    void emptyTheList() {
        InstallationsTable.empty();
        detected.clear();
        InstallationRegistry.forgetStartupScan();
    }

    /**
     * A storefront installed since the last run is otherwise invisible until the user happens to press
     * Rescan, and nothing on screen would tell them to.
     */
    @Test
    void theFirstReadOfTheRunDetects() throws IOException {
        detect(Storefront.STEAM, gameAt("steam"));

        assertEquals(1, registry().currentWithStartupScan().size());
    }

    /**
     * Called again on every ship-profile change, so detection must not run again with it - the registry,
     * VDF and manifest reads would repeat for nothing.
     */
    @Test
    void laterReadsInTheSameRunDoNotDetectAgain() throws IOException {
        InstallationRegistry registry = registry();
        detect(Storefront.STEAM, gameAt("steam"));
        registry.currentWithStartupScan();

        detect(Storefront.EPIC, gameAt("epic"));
        List<InstallationRow> rows = registry.currentWithStartupScan();

        assertEquals(1, rows.size(), "the second storefront waits for a rescan rather than appearing by itself");
    }

    /**
     * Review, 2026-10-15 (A14): a second reader arriving while the run's detection is still going waits for it - or it
     * reads a folder that came back as still missing, and catch-up's startup line never speaks.
     */
    @Test
    void aReaderArrivingDuringDetectionWaitsForIt() throws Exception {
        Path steam = gameAt("steam");
        installations.recordDetected("STEAM", steam);
        installations.setMissing(installations.findAll().getFirst().id(), true);
        CountDownLatch detecting = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        InstallationRegistry registry = new InstallationRegistry(() -> {
            detecting.countDown();
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return List.of();
        }, installations);

        Thread first = new Thread(registry::currentWithStartupScan);
        first.start();
        detecting.await();
        List<List<InstallationRow>> seen = new ArrayList<>();
        Thread second = new Thread(() -> seen.add(registry.currentWithStartupScan()));
        second.start();
        long deadline = System.currentTimeMillis() + 5_000;
        while (second.getState() != Thread.State.BLOCKED && second.isAlive()
                && System.currentTimeMillis() < deadline) {
            Thread.onSpinWait();
        }
        assertEquals(Thread.State.BLOCKED, second.getState(), "it waits rather than reading the old list");

        release.countDown();
        first.join();
        second.join();

        assertFalse(seen.getFirst().getFirst().missing(), "the detection has marked the folder present");
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

    /** A folder with ControlSchemes but no game executable is not confirmed (storage.md rule 1). */
    @Test
    void addingAFolderWithoutTheGameExecutableIsRefused() throws IOException {
        Path root = machine.resolve("half");
        Files.createDirectories(GameInstallation.controlSchemesUnder(root));

        assertThrows(IllegalArgumentException.class, () -> registry().addByHand(root));
        assertTrue(installations.findAll().isEmpty(), "nothing was recorded");
    }

    /** Storage S2b: a removed installation stays removed, however often detection finds it. */
    @Test
    void aRemovedInstallationIsNotPutBackByDetection() throws IOException {
        Path steam = gameAt("steam");
        detect(Storefront.STEAM, steam);
        InstallationRegistry registry = registry();
        registry.remove(registry.rescan().getFirst().id());

        assertTrue(registry.rescan().isEmpty());
    }

    @Test
    void addMissingBringsBackARemovedInstallation() throws IOException {
        Path steam = gameAt("steam");
        detect(Storefront.STEAM, steam);
        InstallationRegistry registry = registry();
        long id = registry.rescan().getFirst().id();
        registry.remove(id);

        InstallationRow back = registry.addByHand(steam);

        assertEquals(id, back.id());
        assertEquals(1, registry.rescan().size());
    }

    /**
     * Review, 2026-10-24: the backup detects once and hands the result over, and that detection is the run's - the
     * screens' first refresh does not detect again.
     */
    @Test
    void aDetectionHandedInIsRecordedAndCountsAsTheRunsDetection() throws IOException {
        InstallationRegistry registry = registry();
        Path steam = gameAt("steam");
        List<GameInstallation> handedIn = List.of(new GameInstallation(Storefront.STEAM, steam,
                GameInstallation.controlSchemesUnder(steam), machine.resolve("Bindings")));

        assertEquals(1, registry.rescan(handedIn).size());

        detect(Storefront.EPIC, gameAt("epic"));
        assertEquals(1, registry.currentWithStartupScan().size(), "no second detection this run");
    }

    private InstallationRegistry registry() {
        return new InstallationRegistry(() -> List.copyOf(detected), installations);
    }

    private void detect(Storefront storefront, Path root) {
        detected.add(new GameInstallation(storefront, root, GameInstallation.controlSchemesUnder(root),
                machine.resolve("Bindings")));
    }

    /**
     * Creates the Live product's ControlSchemes folder and its game executable, which is what makes a folder an
     * installation Add missing accepts.
     */
    private Path gameAt(String name) throws IOException {
        Path root = machine.resolve(name);
        Files.createDirectories(GameInstallation.controlSchemesUnder(root));
        Path executable = root.resolve("Products").resolve(GameInstallation.LIVE_PRODUCT)
                .resolve(GameInstallation.GAME_EXECUTABLE);
        if (!Files.exists(executable)) Files.createFile(executable);
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
