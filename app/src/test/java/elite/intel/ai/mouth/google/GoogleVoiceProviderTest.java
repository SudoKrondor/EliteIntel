package elite.intel.ai.mouth.google;

import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Maps a selected voice to a real Google voice name. For a non-English language with Chirp3-HD coverage the
 * voice keeps its character and only swaps the locale prefix, so voices stay distinct (and HD) in that
 * language instead of collapsing to one voice per gender. Because the Chirp3-HD roster differs per locale, an
 * injected lookup validates the name against what the language actually offers and falls back to a known-good
 * voice otherwise; with no lookup wired (as in most of these tests) the character is used optimistically.
 * English uses the explicit provider mapping, and a language without Chirp3-HD coverage (pt-PT) falls back to
 * a Standard voice. Language is DB-backed and the provider is a singleton, so each test resets both afterwards
 * to avoid leaking into the shared test session.
 */
class GoogleVoiceProviderTest {

    @AfterEach
    void resetState() {
        SystemSession.getInstance().setLanguage(Language.EN);
        GoogleVoiceProvider.getInstance().setAvailableVoices(null);
    }

    @Test
    void russianKeepsTheVoiceCharacterAtItsLocale() {
        SystemSession.getInstance().setLanguage(Language.RU);

        VoiceSelectionParams params = GoogleVoiceProvider.getInstance().getVoiceParams("MARY");

        assertEquals("ru-RU", params.getLanguageCode());
        assertEquals("ru-RU-Chirp3-HD-Zephyr", params.getName(), "the Zephyr character is rendered in Russian");
    }

    @Test
    void withoutAWiredLookupEachCharacterIsUsedAsIs() {
        // No availability lookup wired (as before the TTS engine starts): resolution is optimistic and maps
        // each voice to its own character. The wired path validates these against the real ru-RU roster - one
        // of those names (Despina) does not actually exist in ru-RU, which is exactly why the lookup exists.
        SystemSession.getInstance().setLanguage(Language.RU);
        GoogleVoiceProvider provider = GoogleVoiceProvider.getInstance();

        assertEquals("ru-RU-Chirp3-HD-Charon", provider.getVoiceParams("MICHAEL").getName());
        assertEquals("ru-RU-Chirp3-HD-Despina", provider.getVoiceParams("EMMA").getName());
    }

    @Test
    void europeanPortugueseFallsBackToAStandardVoice() {
        SystemSession.getInstance().setLanguage(Language.PT);

        VoiceSelectionParams params = GoogleVoiceProvider.getInstance().getVoiceParams("MARY");

        assertEquals("pt-PT", params.getLanguageCode());
        assertEquals("pt-PT-Standard-A", params.getName(), "pt-PT has no Chirp3-HD, so it uses a Standard voice");
    }

    @Test
    void englishUsesTheMappedProviderVoice() {
        SystemSession.getInstance().setLanguage(Language.EN);

        VoiceSelectionParams params = GoogleVoiceProvider.getInstance().getVoiceParams("MARY");

        assertEquals("en-US", params.getLanguageCode());
        assertEquals("en-US-Chirp3-HD-Zephyr", params.getName());
    }

    @Test
    void waveNetVoicesUseTheirExactBritishProviderMappings() {
        SystemSession.getInstance().setLanguage(Language.EN);
        GoogleVoiceProvider provider = GoogleVoiceProvider.getInstance();

        assertVoice(provider.getVoiceParams("WAVENET_F"), "en-GB", "en-GB-Wavenet-F");
        assertVoice(provider.getVoiceParams("WAVENET_N"), "en-GB", "en-GB-Wavenet-N");
    }

    @Test
    void waveNetVoicesAreFemaleAndResolveToThemselves() {
        assertFalse(GoogleVoices.WAVENET_F.isMale());
        assertFalse(GoogleVoices.WAVENET_N.isMale());
        assertEquals(GoogleVoices.WAVENET_F, GoogleVoices.voiceOrDefault("WAVENET_F"));
        assertEquals(GoogleVoices.WAVENET_N, GoogleVoices.voiceOrDefault("WAVENET_N"));
    }

    @Test
    void maleShipVoicesResolveToThemselvesAndTheDefaultStaysFemale() {
        assertEquals(GoogleVoices.JAKE, GoogleVoices.voiceOrDefault("JAKE"));
        assertEquals(GoogleVoices.DEFAULT_VOICE, GoogleVoices.voiceOrDefault("not-a-google-voice"));
        assertFalse(GoogleVoices.DEFAULT_VOICE.isMale(), "the default ship voice stays female");
    }

    @Test
    void waveNetVoicesUseTheSafeFemaleFallbackOutsideEnglish() {
        SystemSession.getInstance().setLanguage(Language.DE);
        GoogleVoiceProvider provider = GoogleVoiceProvider.getInstance();

        VoiceSelectionParams waveNetF = provider.getVoiceParams("WAVENET_F");
        VoiceSelectionParams waveNetN = provider.getVoiceParams("WAVENET_N");

        assertVoice(waveNetF, "de-DE", "de-DE-Standard-G");
        assertVoice(waveNetN, "de-DE", "de-DE-Standard-G");
        assertFalse(waveNetF.getName().contains("Wavenet"));
        assertFalse(waveNetN.getName().contains("Wavenet"));
    }

    @Test
    void existingEnglishMappingsRemainUnchanged() {
        SystemSession.getInstance().setLanguage(Language.EN);
        GoogleVoiceProvider provider = GoogleVoiceProvider.getInstance();
        Map<GoogleVoices, String> expectedNames = Map.ofEntries(
                Map.entry(GoogleVoices.MARY, "en-US-Chirp3-HD-Zephyr"),
                Map.entry(GoogleVoices.ANNA, "en-GB-Chirp-HD-F"),
                Map.entry(GoogleVoices.EMMA, "en-US-Chirp3-HD-Despina"),
                Map.entry(GoogleVoices.JAKE, "en-US-Chirp3-HD-Iapetus"),
                Map.entry(GoogleVoices.JAMES, "en-AU-Chirp3-HD-Algieba"),
                Map.entry(GoogleVoices.JENNIFER, "en-US-Chirp3-HD-Sulafat"),
                Map.entry(GoogleVoices.JOSEPH, "en-US-Chirp3-HD-Sadachbia"),
                Map.entry(GoogleVoices.MICHAEL, "en-US-Chirp3-HD-Charon"),
                Map.entry(GoogleVoices.OLIVIA, "en-GB-Chirp3-HD-Aoede"),
                Map.entry(GoogleVoices.RACHEL, "en-US-Chirp3-HD-Zephyr"),
                Map.entry(GoogleVoices.STEVE, "en-US-Chirp3-HD-Algenib"));

        expectedNames.forEach((voice, providerName) ->
                assertEquals(providerName, provider.getVoiceParams(voice.name()).getName(), voice.name()));
    }

    @Test
    void anAvailableCharacterIsKeptWhenTheLookupOffersIt() {
        SystemSession.getInstance().setLanguage(Language.RU);
        GoogleVoiceProvider.getInstance().setAvailableVoices(code -> List.of(
                new GoogleVoiceProvider.AvailableVoice("ru-RU-Chirp3-HD-Zephyr", false)));

        VoiceSelectionParams mary = GoogleVoiceProvider.getInstance().getVoiceParams("MARY");

        assertEquals("ru-RU-Chirp3-HD-Zephyr", mary.getName(), "the character is used when the locale offers it");
    }

    @Test
    void anUnavailableCharacterFallsBackToAnAvailableHdVoiceOfTheSameGender() {
        SystemSession.getInstance().setLanguage(Language.RU);
        // ru-RU offers these female HD voices but not Despina (EMMA's character), so EMMA must map to an offered
        // HD voice rather than drop to a lower-quality Standard voice.
        GoogleVoiceProvider.getInstance().setAvailableVoices(code -> List.of(
                new GoogleVoiceProvider.AvailableVoice("ru-RU-Chirp3-HD-Aoede", false),
                new GoogleVoiceProvider.AvailableVoice("ru-RU-Chirp3-HD-Leda", false)));

        String emma = GoogleVoiceProvider.getInstance().getVoiceParams("EMMA").getName();

        assertTrue(emma.equals("ru-RU-Chirp3-HD-Aoede") || emma.equals("ru-RU-Chirp3-HD-Leda"),
                "an unavailable female character maps to an offered HD voice, keeping HD quality: " + emma);
    }

    @Test
    void withoutAnHdVoiceOfThatGenderItFallsBackToStandard() {
        SystemSession.getInstance().setLanguage(Language.RU);
        // Only a female HD voice is offered, so a male voice has no HD option and drops to the known-good Standard.
        GoogleVoiceProvider.getInstance().setAvailableVoices(code -> List.of(
                new GoogleVoiceProvider.AvailableVoice("ru-RU-Chirp3-HD-Leda", false)));

        assertEquals("ru-RU-Standard-B", GoogleVoiceProvider.getInstance().getVoiceParams("MICHAEL").getName(),
                "no male HD voice offered -> known-good Standard male voice");
    }

    @Test
    void aFailedVoiceLookupFallsBackToAKnownGoodVoice() {
        SystemSession.getInstance().setLanguage(Language.RU);
        // An empty list models a listVoices failure: fall back to a guaranteed Standard voice rather than risk
        // requesting an invalid voice name.
        GoogleVoiceProvider.getInstance().setAvailableVoices(code -> List.of());

        assertEquals("ru-RU-Standard-A", GoogleVoiceProvider.getInstance().getVoiceParams("MARY").getName());
    }

    private static void assertVoice(VoiceSelectionParams voice, String languageCode, String name) {
        assertEquals(languageCode, voice.getLanguageCode());
        assertEquals(name, voice.getName());
    }
}
