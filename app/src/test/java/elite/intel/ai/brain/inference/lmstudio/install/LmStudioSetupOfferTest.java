package elite.intel.ai.brain.inference.lmstudio.install;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.inference.lmstudio.LMStudioClient.ModelListing;
import elite.intel.db.managers.LmStudioInstallManager;
import elite.intel.i18n.Language;
import elite.intel.ui.i18n.MultiLingualTextProvider;
import elite.intel.util.OsDetector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The decision table in {@code docs/LM_STUDIO_INSTALL_ASSISTANCE.md}, "What the button says", for a PC that already
 * meets the GPU bar and keeps LM Studio on this machine.
 */
class LmStudioSetupOfferTest {

    private static final String MODEL = "google/gemma-4-e4b";

    @Test
    void noLmStudioAndNothingAnsweringOffersTheInstall() {
        assertEquals(LmStudioSetupOffer.INSTALL,
                LmStudioSetupOffer.decide(false, new ModelListing.NotAnswering(), ""));
    }

    @Test
    void somethingAnsweringWithoutAnLmsHereIsLeftAlone() {
        assertEquals(LmStudioSetupOffer.NONE,
                LmStudioSetupOffer.decide(false, new ModelListing.Listed(Set.of(MODEL)), MODEL));
        assertEquals(LmStudioSetupOffer.NONE,
                LmStudioSetupOffer.decide(false, new ModelListing.Unknown(), MODEL));
    }

    @Test
    void anExistingLmStudioWithoutTheModelOffersTheSetUp() {
        assertEquals(LmStudioSetupOffer.SET_UP_EXISTING,
                LmStudioSetupOffer.decide(true, new ModelListing.NotAnswering(), MODEL));
        assertEquals(LmStudioSetupOffer.SET_UP_EXISTING,
                LmStudioSetupOffer.decide(true, new ModelListing.Listed(Set.of("qwen/qwen3-8b")), MODEL));
    }

    @Test
    void anExistingLmStudioWithNoModelConfiguredOffersTheSetUp() {
        assertEquals(LmStudioSetupOffer.SET_UP_EXISTING,
                LmStudioSetupOffer.decide(true, new ModelListing.Listed(Set.of(MODEL)), ""));
    }

    @Test
    void anExistingLmStudioAlreadyServingTheConfiguredModelNeedsNothing() {
        assertEquals(LmStudioSetupOffer.NONE,
                LmStudioSetupOffer.decide(true, new ModelListing.Listed(Set.of(MODEL)), MODEL));
        assertEquals(LmStudioSetupOffer.NONE,
                LmStudioSetupOffer.decide(true, new ModelListing.Listed(Set.of(MODEL)), "gemma-4-e4b"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://localhost:1234/v1/chat/completions",
            "http://127.0.0.1:1234/v1/chat/completions",
            "http://[::1]:1234/v1/chat/completions",
            "http://LOCALHOST:1234/v1/chat/completions",
            "",
    })
    void anAddressOnThisPc(String address) {
        assertTrue(LmStudioSetupOffer.isOnThisPc(address));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://192.168.1.20:1234/v1/chat/completions",
            "http://gamebox.local:1234/v1/chat/completions",
            "not a url at all",
    })
    void anAddressOnAnotherMachine(String address) {
        assertFalse(LmStudioSetupOffer.isOnThisPc(address));
    }

    @Test
    void anUnfinishedOwnedInstallIsContinuedAndAFinishedOneUninstalled() {
        assertEquals(LmStudioSetupOffer.CONTINUE_SETUP, LmStudioSetupOffer.forOwnedInstall(install(false)));
        assertEquals(LmStudioSetupOffer.UNINSTALL, LmStudioSetupOffer.forOwnedInstall(install(true)));
    }

    @Nested
    class OwnedInstall {

        private final LmStudioInstallManager installs = LmStudioInstallManager.getInstance();

        // WHY: the suite shares one in-memory database, so a record left behind would leak into other tests.
        @BeforeEach
        @AfterEach
        void noRecord() {
            installs.forget();
        }

        // WHY a home of the test's own: ownedInstall() reads the real one, and a developer's PC may hold a marker.
        @TempDir
        Path userHome;

        private Path defaultHome() {
            return userHome.resolve(LmStudioHome.DEFAULT_DIR);
        }

        private final SavedFields fields = new SavedFields();

        private Optional<LmStudioInstallManager.Install> owned() {
            return LmStudioSetupOffer.ownedInstall(installs, userHome, fields);
        }

        @Test
        void aRecordWhoseFolderHoldsItsMarkerIsTheAppsInstall() throws IOException {
            Path home = Files.createDirectories(defaultHome());
            LmStudioInstallManager.Install recorded = installs.recordStarted(home, false);
            InstallMarker.write(home, recorded.installId());

            assertEquals(Optional.of(recorded), owned());
        }

        @Test
        void aRecordWhoseFolderHasNoMarkerIsNotTheAppsInstall(@TempDir Path elsewhere) {
            installs.recordStarted(elsewhere, false);

            assertTrue(owned().isEmpty(), "same path, someone else's LM Studio");
            assertTrue(installs.find().isPresent(), "ignored, not forgotten: the drive may only be unplugged");
        }

        @Test
        void noRecordAndNoMarkerIsNoInstallOfTheApps() throws IOException {
            installedLmStudio(Files.createDirectories(defaultHome()));

            assertTrue(owned().isEmpty(), "an LM Studio the commander set up themselves");
            assertTrue(installs.find().isEmpty(), "nothing recorded for it");
        }

        @Test
        void aMarkerWithNoRecordIsRecordedAgainAndOffersTheUninstall() throws IOException {
            Path home = Files.createDirectories(defaultHome());
            installedLmStudio(home);
            downloadedModel(home);
            UUID installId = UUID.randomUUID();
            InstallMarker.write(home, installId);

            LmStudioInstallManager.Install reclaimed = owned().orElseThrow();

            assertEquals(installId, reclaimed.installId(), "the id the marker holds, so the marker still proves it");
            assertEquals(home, reclaimed.homeDir());
            assertFalse(reclaimed.pointerWritten(), "the usual home needs no pointer of the app's");
            assertEquals(LmStudioSetupOffer.UNINSTALL, LmStudioSetupOffer.forOwnedInstall(reclaimed));
            assertEquals(Optional.of(reclaimed), installs.find(), "recorded, so the services run it again");
        }

        @Test
        void aMarkerWithNoRecordAndNoModelDownloadedIsContinued() throws IOException {
            Path home = Files.createDirectories(defaultHome());
            installedLmStudio(home);
            InstallMarker.write(home, UUID.randomUUID());

            assertEquals(LmStudioSetupOffer.CONTINUE_SETUP, LmStudioSetupOffer.forOwnedInstall(owned().orElseThrow()));
            assertEquals("", fields.model, "Continue setup saves the Model once it has found the model");
        }

        @Test
        void aFinishedInstallRecordedAgainFillsInTheBlankModel() throws IOException {
            Path home = Files.createDirectories(defaultHome());
            installedLmStudio(home);
            downloadedModel(home);
            InstallMarker.write(home, UUID.randomUUID());
            fields.address = "";

            owned().orElseThrow();

            assertEquals(MODEL, fields.model, "the database was erased, the model was not");
            assertEquals("http://localhost:1234/v1/chat/completions", fields.address);
        }

        @Test
        void aFilledInModelKeepsTheCommandersAddressOnThisPc() throws IOException {
            Path home = Files.createDirectories(defaultHome());
            installedLmStudio(home);
            downloadedModel(home);
            InstallMarker.write(home, UUID.randomUUID());
            fields.address = "http://127.0.0.1:1234/v1/chat/completions";

            owned().orElseThrow();

            assertEquals("http://127.0.0.1:1234/v1/chat/completions", fields.address);
            assertEquals(MODEL, fields.model);
        }

        @Test
        void aModelTheCommanderTypedIsNeverReplaced() throws IOException {
            Path home = Files.createDirectories(defaultHome());
            installedLmStudio(home);
            downloadedModel(home);
            InstallMarker.write(home, UUID.randomUUID());
            fields.model = "gemma-4-26b-a4b";

            owned().orElseThrow();

            assertEquals("gemma-4-26b-a4b", fields.model);
        }

        @Test
        void aBlankModelForAnotherMachineIsLeftBlank() throws IOException {
            Path home = Files.createDirectories(defaultHome());
            installedLmStudio(home);
            downloadedModel(home);
            InstallMarker.write(home, UUID.randomUUID());
            fields.address = "http://192.168.1.20:1234/v1/chat/completions";

            owned().orElseThrow();

            assertEquals("", fields.model, "not this install's model to name");
        }

        @Test
        void aRecordThatStillMatchesLeavesTheFieldsAlone() throws IOException {
            Path home = Files.createDirectories(defaultHome());
            installedLmStudio(home);
            downloadedModel(home);
            LmStudioInstallManager.Install recorded = installs.recordStarted(home, false);
            InstallMarker.write(home, recorded.installId());

            owned().orElseThrow();

            assertEquals("", fields.model, "a Model the commander cleared on a live install stays cleared");
        }

        @Test
        void aMarkerWithNoRecordAndNoLmStudioLeftIsContinued() throws IOException {
            Path home = Files.createDirectories(defaultHome());
            InstallMarker.write(home, UUID.randomUUID());

            assertEquals(LmStudioSetupOffer.CONTINUE_SETUP, LmStudioSetupOffer.forOwnedInstall(owned().orElseThrow()));
        }

        @Test
        void aHomeTheAppPointedLmStudioAtKeepsItsPointerForTheUninstall(@TempDir Path drive) throws IOException {
            LmStudioHome.writePointer(userHome, drive);
            InstallMarker.write(drive, UUID.randomUUID());

            LmStudioInstallManager.Install reclaimed = owned().orElseThrow();

            assertEquals(drive, reclaimed.homeDir());
            assertTrue(reclaimed.pointerWritten());
        }

        @Test
        void aRecordOfAnEarlierInstallInTheSameHomeIsReplaced() throws IOException {
            Path home = Files.createDirectories(defaultHome());
            installs.recordStarted(home, false);
            UUID current = UUID.randomUUID();
            InstallMarker.write(home, current);

            assertEquals(current, owned().orElseThrow().installId());
            assertEquals(current, installs.find().orElseThrow().installId());
        }

        @Test
        void aRecordForAnotherFolderIsNotReplacedByAMarkerHere(@TempDir Path unplugged) throws IOException {
            LmStudioInstallManager.Install recorded = installs.recordStarted(unplugged, false);
            InstallMarker.write(Files.createDirectories(defaultHome()), UUID.randomUUID());

            assertTrue(owned().isEmpty());
            assertEquals(Optional.of(recorded), installs.find(), "one record at most, and its drive may come back");
        }

        private static void downloadedModel(Path home) throws IOException {
            Files.writeString(Files.createDirectories(home.resolve("hub").resolve("models").resolve(MODEL))
                    .resolve("manifest.json"), "{}");
        }

        private static void installedLmStudio(Path home) throws IOException {
            String lms = OsDetector.getOs() == OsDetector.OS.WINDOWS ? "lms.exe" : "lms";
            Files.createFile(Files.createDirectories(home.resolve("bin")).resolve(lms));
            Path llmster = Files.createFile(Files.createDirectories(home.resolve("llmster")).resolve("llmster"));
            Path internal = Files.createDirectories(home.resolve(".internal"));
            JsonObject record = new JsonObject();
            record.addProperty("path", llmster.toString());
            Files.writeString(internal.resolve("llmster-install-location.json"), record.toString());
        }
    }

    /**
     * The LMStudio Setup fields of a fresh database: LM Studio's local Address and no Model.
     */
    private static final class SavedFields implements InstallReclaim.LmStudioFields {
        String address = "http://localhost:1234/v1/chat/completions";
        String model = "";

        @Override
        public String address() {
            return address;
        }

        @Override
        public String model() {
            return model;
        }

        @Override
        public void save(String address, String model) {
            this.address = address;
            this.model = model;
        }
    }

    private static LmStudioInstallManager.Install install(boolean setupComplete) {
        return new LmStudioInstallManager.Install(UUID.randomUUID(), Path.of("/home/cmdr/.lmstudio"), false,
                setupComplete, Instant.now());
    }

    @ParameterizedTest
    @EnumSource(Language.class)
    void everyCaptionResolvesInEveryLanguage(Language language) {
        for (LmStudioSetupOffer offer : LmStudioSetupOffer.values()) {
            offer.captionKey().ifPresent(key -> {
                String text = MultiLingualTextProvider.getText(language, key);
                assertNotEquals(key, text, "no bundle defines " + key + " for " + language);
                assertFalse(text.isBlank(), key + " is empty in " + language);
            });
        }
    }
}
