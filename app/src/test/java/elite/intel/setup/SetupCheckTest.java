package elite.intel.setup;

import com.google.common.eventbus.Subscribe;
import elite.intel.ai.hands.BindingsLoader;
import elite.intel.ai.mouth.subscribers.events.VocalisationRequestEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import elite.intel.setup.SetupCheck.LlmSetup;
import elite.intel.ui.i18n.MultiLingualTextProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A fresh install has to say what is missing. The migrations ship {@code useLocalCommandLlm} true with an
 * empty model name and no key, so out of the box the app is pointed at a local LLM that was never chosen -
 * which reads as "nothing configured", not as "wrong model".
 */
class SetupCheckTest {

    @AfterEach
    void restoreLanguage() {
        SystemSession.getInstance().setLanguage(Language.EN);
    }

    /**
     * The shipped state, read from a database built by the real migrations: local LLM lanes selected with no
     * model named and no key stored. This is the case the whole class exists for, so it is asserted against the
     * migrations rather than described in a comment - if a future migration ships a default model or key, the
     * onboarding warning silently stops firing and this fails instead.
     * <p>
     * No test writes LLM settings, so the shared fork database still holds the shipped values here.
     */
    @Test
    void theShippedDatabaseLooksLikeAFreshInstall() {
        SystemSession session = SystemSession.getInstance();

        assertTrue(session.useLocalCommandLlm(), "a fresh install points at a local LLM");
        assertTrue(session.getLmStudioCommandModel() == null || session.getLmStudioCommandModel().isBlank());
        assertTrue(session.getAiApiKey() == null || session.getAiApiKey().isBlank());
        assertTrue(SetupCheck.isLlmUnconfigured(), "the warning has to fire on a fresh install");
    }

    /**
     * The warning is spoken, so a fresh install has to have a voice to speak it with. Local TTS ships on for
     * exactly that reason: a commander with no cloud key would otherwise be told about it in silence.
     */
    @Test
    void aFreshInstallCanSpeakWithoutAnyCloudAccount() {
        assertTrue(SystemSession.getInstance().useLocalTTS());
    }

    // ── is there a model at all ──────────────────────────────────────────────

    @Test
    void aFreshInstallHasNothingConfigured() {
        // Exactly what the migrations leave behind: no local model named, no cloud key stored.
        assertTrue(new LlmSetup("", "").nothingConfigured());
        assertTrue(new LlmSetup(null, null).nothingConfigured());
        assertTrue(new LlmSetup("   ", "  ").nothingConfigured());
    }

    @Test
    void aConfiguredLocalModelIsNotAFreshInstall() {
        assertFalse(new LlmSetup("google/gemma-4-e4b", "").nothingConfigured());
    }

    @Test
    void aStoredCloudKeyIsNotAFreshInstall() {
        assertFalse(new LlmSetup("", "sk-whatever").nothingConfigured());
    }

    /**
     * Deliberate: a commander who set either kind up is not lectured about a fresh install, even if the lane
     * they are currently on is the other one. A selected-but-broken lane is the connection check's business,
     * and it can say something far more specific.
     */
    @Test
    void anyConfigurationAtAllSilencesTheOnboardingWarning() {
        assertFalse(new LlmSetup("gemma-4-e4b", "sk-whatever").nothingConfigured());
    }

    // ── journals ─────────────────────────────────────────────────────────────

    @Test
    void aFolderWithAJournalIsAccepted(@TempDir Path dir) throws IOException {
        Files.createFile(dir.resolve("Journal.2026-08-11T200621.01.log"));

        assertTrue(SetupCheck.hasJournalFiles(dir));
    }

    @Test
    void anEmptyFolderHasNoJournals(@TempDir Path dir) {
        assertFalse(SetupCheck.hasJournalFiles(dir));
    }

    @Test
    void aFolderOfUnrelatedLogsIsNotAJournalFolder(@TempDir Path dir) throws IOException {
        // The wrong-folder mistake this exists to catch: .log files, but nothing Frontier wrote.
        Files.createFile(dir.resolve("application.log"));
        Files.createFile(dir.resolve("crash.log"));

        assertFalse(SetupCheck.hasJournalFiles(dir));
    }

    @Test
    void aMissingFolderHasNoJournals(@TempDir Path dir) {
        assertFalse(SetupCheck.hasJournalFiles(dir.resolve("nope")));
        assertFalse(SetupCheck.hasJournalFiles(null));
    }

    // ── bindings ─────────────────────────────────────────────────────────────

    @Test
    void aFolderWithBindsIsAccepted(@TempDir Path dir) throws IOException {
        Files.createFile(dir.resolve("Custom.4.0.binds"));

        assertTrue(checkWith(false, dir, dir).hasBindingsFiles(dir));
    }

    @Test
    void anEmptyFolderHasNoBindings(@TempDir Path dir) {
        assertFalse(checkWith(false, dir, dir).hasBindingsFiles(dir));
    }

    @Test
    void aMissingFolderHasNoBindings(@TempDir Path dir) {
        assertFalse(checkWith(false, dir, dir).hasBindingsFiles(dir.resolve("nope")));
        assertFalse(checkWith(false, dir, dir).hasBindingsFiles(null));
    }

    // ── what actually gets said ──────────────────────────────────────────────

    /**
     * The whole point, end to end: a fresh install speaks all three, straight to the TTS engine. The channel
     * matters as much as the words - routing these through the companion would ask the missing language model
     * to announce its own absence.
     */
    @Test
    void aFreshInstallSpeaksAllThreeWarnings(@TempDir Path empty) {
        List<String> spoken = capture(() -> checkWith(true, empty, empty).check());

        assertEquals(List.of(
                        text("speech.setup.noLlm"),
                        text("speech.setup.noJournals"),
                        text("speech.setup.noBindings")),
                spoken);
    }

    @Test
    void aFullyConfiguredInstallSaysNothing(@TempDir Path dir) throws IOException {
        Files.createFile(dir.resolve("Journal.2026-08-11T200621.01.log"));
        Files.createFile(dir.resolve("Custom.4.0.binds"));

        assertTrue(capture(() -> checkWith(false, dir, dir).check()).isEmpty());
    }

    @Test
    void aConfiguredModelWithTheWrongJournalFolderOnlyMentionsTheJournals(
            @TempDir Path journals, @TempDir Path bindings) throws IOException {
        Files.createFile(bindings.resolve("Custom.4.0.binds"));

        List<String> spoken = capture(() -> checkWith(false, journals, bindings).check());

        assertEquals(List.of(text("speech.setup.noJournals")), spoken);
    }

    @Test
    void theWarningsAreSpokenUninterruptibly(@TempDir Path empty) {
        // A commander who talks over the one message telling them nothing works is no better off.
        List<VocalisationRequestEvent> events = captureEvents(() -> checkWith(true, empty, empty).check());

        assertFalse(events.isEmpty());
        events.forEach(event -> assertFalse(event.canBeInterrupted(), "setup warnings must not be interruptible"));
    }

    private static SetupCheck checkWith(boolean llmUnconfigured, Path journals, Path bindings) {
        return new SetupCheck(new BindingsLoader(), () -> llmUnconfigured, () -> journals, () -> bindings);
    }

    private static String text(String key) {
        return MultiLingualTextProvider.getText(Language.EN, key);
    }

    private static List<String> capture(Runnable action) {
        return captureEvents(action).stream().map(VocalisationRequestEvent::getText).toList();
    }

    private static List<VocalisationRequestEvent> captureEvents(Runnable action) {
        List<VocalisationRequestEvent> heard = new ArrayList<>();
        Object listener = new Object() {
            @Subscribe
            public void onVocalisation(VocalisationRequestEvent event) {
                heard.add(event);
            }
        };
        GameEventBus.register(listener);
        try {
            action.run();
        } finally {
            GameEventBus.unregister(listener);
        }
        return heard;
    }

    // ── the commander has to be able to understand it ────────────────────────

    @Test
    void everyWarningIsTranslatedInEveryLanguage() {
        for (String key : new String[]{"speech.setup.noLlm", "speech.setup.noJournals", "speech.setup.noBindings"}) {
            String english = MultiLingualTextProvider.getText(Language.EN, key);
            assertFalse(english.isBlank(), key);
            assertNotEquals(key, english, "missing from the base bundle: " + key);

            for (Language language : Language.values()) {
                String text = MultiLingualTextProvider.getText(language, key);
                assertNotEquals(key, text, language + " has no " + key);
                assertFalse(text.isBlank(), language + " " + key);
                if (language != Language.EN) {
                    assertNotEquals(english, text, language + " left " + key + " in English");
                }
            }
        }
    }

    /**
     * The onboarding line has to name the two ways out and where to read about them, in every language -
     * a commander who cannot act on it is no better off than one who heard nothing.
     * <p>
     * The site is spoken, never shown, so Cyrillic locales say it phonetically ("элит интел точка орг"): a
     * Russian voice handed a Latin domain either spells it out or mangles it. Hence the expected token is
     * per-language rather than the Latin name everywhere.
     */
    @Test
    void theOnboardingLineNamesBothOptionsAndTheSite() {
        for (Language language : Language.values()) {
            String text = MultiLingualTextProvider.getText(language, "speech.setup.noLlm");

            assertTrue(text.contains("Mistral"), language + " does not name Mistral");
            assertTrue(text.contains("Gemma 4 E4B"), language + " does not name the local model");
            assertTrue(text.toLowerCase().contains(spokenSiteToken(language)),
                    language + " does not point at the site");
        }
    }

    /**
     * How this language says the "intel" of the site name aloud.
     */
    private static String spokenSiteToken(Language language) {
        return switch (language) {
            case RU -> "интел";
            case UK -> "інтел";
            default -> "intel";
        };
    }

    @Test
    void theWarningsAreSpokenInTheSelectedLanguage() {
        SystemSession.getInstance().setLanguage(Language.DE);

        assertEquals(MultiLingualTextProvider.getText(Language.DE, "speech.setup.noLlm"),
                elite.intel.util.StringUtls.localizedSpeech("speech.setup.noLlm"));
    }
}
