package elite.intel.ai.brain.inference.lmstudio.install;

import elite.intel.ai.brain.inference.lmstudio.LMStudioClient.ModelListing;
import elite.intel.db.managers.LmStudioInstallManager;
import elite.intel.i18n.Language;
import elite.intel.ui.i18n.MultiLingualTextProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
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

        @Test
        void aRecordWhoseFolderHoldsItsMarkerIsTheAppsInstall(@TempDir Path home) throws IOException {
            LmStudioInstallManager.Install recorded = installs.recordStarted(home, false);
            InstallMarker.write(home, recorded.installId());

            assertEquals(Optional.of(recorded), LmStudioSetupOffer.ownedInstall());
        }

        @Test
        void aRecordWhoseFolderHasNoMarkerIsNotTheAppsInstall(@TempDir Path home) {
            installs.recordStarted(home, false);

            assertTrue(LmStudioSetupOffer.ownedInstall().isEmpty(), "same path, someone else's LM Studio");
            assertTrue(installs.find().isPresent(), "ignored, not forgotten: the drive may only be unplugged");
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
