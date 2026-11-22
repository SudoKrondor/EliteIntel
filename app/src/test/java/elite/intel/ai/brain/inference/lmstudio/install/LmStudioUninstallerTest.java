package elite.intel.ai.brain.inference.lmstudio.install;

import elite.intel.ai.brain.inference.lmstudio.LmStudioDefaults;
import elite.intel.db.managers.LmStudioInstallManager;
import elite.intel.db.managers.LmStudioInstallManager.Install;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Removing the app's own LM Studio, against temporary folders, a {@link FakeLmStudioMachine} and a recorded PATH.
 */
class LmStudioUninstallerTest {

    @TempDir
    Path userHome;

    private final LmStudioInstallManager installs = LmStudioInstallManager.getInstance();
    private final List<String> saved = new ArrayList<>();
    private final List<Path> pathEntriesRemoved = new ArrayList<>();
    private final List<String> reported = new ArrayList<>();
    private boolean hostStopped;
    private Path home;
    private FakeLmStudioMachine machine;

    // WHY: the suite shares one in-memory database, so a record left behind would leak into other tests.
    @BeforeEach
    void anInstalledLmStudio() throws IOException {
        installs.forget();
        home = userHome.resolve("games").resolve("LMStudio");
        machine = new FakeLmStudioMachine(home);
        machine.installLms();
        Files.createDirectories(home.resolve("models/lmstudio-community/gemma-4-E4B-it-GGUF"));
        Files.write(home.resolve("models/lmstudio-community/gemma-4-E4B-it-GGUF/gemma-4-E4B-it-Q4_K_M.gguf"), new byte[300]);
        Files.write(home.resolve("models/lmstudio-community/gemma-4-E4B-it-GGUF/mmproj-gemma-4-E4B-it-BF16.gguf"), new byte[100]);
        Files.writeString(home.resolve("models/lmstudio-community/gemma-4-E4B-it-GGUF/README.md"), "x");
    }

    @AfterEach
    void noRecordLeft() {
        installs.forget();
    }

    @Test
    void uninstallingRemovesEverythingTheInstallMade() throws IOException {
        Install owned = recordedInstall(true);
        LmStudioHome.writePointer(userHome, home);

        LmStudioUninstaller.Outcome outcome = uninstaller().uninstall(owned);

        assertEquals(new LmStudioUninstaller.Outcome.Uninstalled(), outcome);
        assertFalse(Files.exists(home), "the LM Studio home, models and all");
        assertTrue(Files.isDirectory(userHome.resolve("games")), "the folder the commander chose is left alone");
        assertFalse(Files.exists(userHome.resolve(".lmstudio-home-pointer")));
        assertEquals(List.of(home.resolve("bin")), pathEntriesRemoved);
        assertTrue(installs.find().isEmpty());
        assertEquals(List.of(LmStudioDefaults.ADDRESS + " "), saved, "Model cleared, Address left as LM Studio's own");
    }

    @Test
    void lmStudioIsStoppedBeforeItsFilesAreDeleted() {
        Install owned = recordedInstall(false);
        machine.serverAnswering = true;

        uninstaller().uninstall(owned);

        assertTrue(hostStopped, "the service running it goes first");
        assertEquals(List.of("unload all", "server stop", "daemon down"), machine.calls);
    }

    @Test
    void aPointerTheCommanderHasSinceChangedIsLeftAlone() throws IOException {
        Install owned = recordedInstall(true);
        Path theirs = userHome.resolve("their-own-lmstudio");
        LmStudioHome.writePointer(userHome, theirs);

        uninstaller().uninstall(owned);

        assertEquals(theirs, LmStudioHome.locate(userHome).dir());
    }

    @Test
    void aFolderWithoutThisInstallsMarkerIsNotTouched() {
        Install owned = installs.recordStarted(home, false);

        LmStudioUninstaller.Outcome outcome = uninstaller().uninstall(owned);

        assertInstanceOf(LmStudioUninstaller.Outcome.Failed.class, outcome);
        assertTrue(Files.exists(home.resolve("models")), "an LM Studio the app cannot prove it installed");
        assertTrue(installs.find().isPresent());
        assertFalse(hostStopped);
    }

    @Test
    void theCommandersHomeIsNeverAnInstallFolder() throws IOException {
        UUID id = UUID.randomUUID();
        InstallMarker.write(userHome, id);
        Install pointedAtHome = new Install(id, userHome, false, true, java.time.Instant.now());

        assertTrue(uninstaller().refusal(pointedAtHome).isPresent());
        assertTrue(Files.isDirectory(userHome));
    }

    @Test
    void aPathEntryThatCannotBeRemovedIsReportedButDoesNotStopTheUninstall() {
        Install owned = recordedInstall(false);
        LmStudioUninstaller uninstaller = new LmStudioUninstaller(() -> hostStopped = true, this::save, machine,
                folder -> {
                    throw new InstallException("Could not take " + folder + " out of your PATH");
                }, userHome, installs, Duration.ZERO, reported::add);

        assertEquals(new LmStudioUninstaller.Outcome.Uninstalled(), uninstaller.uninstall(owned));
        assertTrue(reported.stream().anyMatch(line -> line.contains("out of your PATH")));
    }

    @Test
    void theModelsAreListedLargestFirstWithoutOtherFiles() {
        Install owned = recordedInstall(false);

        List<LmStudioUninstaller.ModelFile> models = LmStudioUninstaller.modelsIn(owned);

        assertEquals(List.of(
                new LmStudioUninstaller.ModelFile("gemma-4-E4B-it-Q4_K_M.gguf", 300),
                new LmStudioUninstaller.ModelFile("mmproj-gemma-4-E4B-it-BF16.gguf", 100)), models);
    }

    // -------------------------------------------------------------------------

    private Install recordedInstall(boolean pointerWritten) {
        Install owned = installs.recordStarted(home, pointerWritten);
        try {
            InstallMarker.write(home, owned.installId());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        installs.markSetupComplete();
        return installs.find().orElseThrow();
    }

    private LmStudioUninstaller uninstaller() {
        return new LmStudioUninstaller(() -> hostStopped = true, this::save, machine, pathEntriesRemoved::add,
                userHome, installs, Duration.ZERO, reported::add);
    }

    private void save(String address, String model) {
        saved.add(address + " " + model);
    }
}
