package elite.intel.ai.mouth.google;

import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;

import java.util.*;
import java.util.function.Function;

/**
 * Singleton class responsible for managing Google TTS voice mappings and selection logic.
 * Maps GoogleVoices enum names (e.g., "Jennifer") to Google VoiceSelectionParams and provides
 * methods to retrieve user-selected and random voices dynamically as GoogleVoices enums.
 */
public class GoogleVoiceProvider implements VoiceProvider<VoiceSelectionParams> {
    private static final GoogleVoiceProvider INSTANCE = new GoogleVoiceProvider();
    /** Languages with Chirp3-HD coverage: the selected voice's character is kept, only the locale prefix changes. */
    private static final EnumSet<Language> CHIRP3_HD_LANGUAGES =
            EnumSet.of(Language.RU, Language.UK, Language.DE, Language.FR, Language.ES, Language.IT,
                    Language.PTBZ);
    private final Map<String, VoiceSelectionParams> voiceMap;
    /**
     * Reports the voices Google offers for a BCP-47 language code (e.g. "ru-RU"), injected by the TTS engine
     * that owns the API client. Null until wired (unit tests, or before the engine starts): the desired voice
     * name is then used as-is. The Chirp3-HD character roster differs per locale, so this both guards against
     * requesting a voice that does not exist there (a hard API error) and lets an unavailable character fall
     * back to another HD voice of the same gender instead of a lower-quality Standard voice.
     */
    private volatile Function<String, List<AvailableVoice>> availableVoices;

    /** A voice Google offers for a language: its provider name and whether it is a male voice (from listVoices). */
    public record AvailableVoice(String name, boolean male) {}

    private GoogleVoiceProvider() {
        // Initialize the English voice mappings by GoogleVoices display name.
        voiceMap = new HashMap<>();
        voiceMap.put(GoogleVoices.ANNA.getName(), VoiceSelectionParams.newBuilder().setLanguageCode("en-GB").setName("en-GB-Chirp-HD-F").build());
        voiceMap.put(GoogleVoices.EMMA.getName(), VoiceSelectionParams.newBuilder().setLanguageCode("en-US").setName("en-US-Chirp3-HD-Despina").build());
        voiceMap.put(GoogleVoices.JAKE.getName(), VoiceSelectionParams.newBuilder().setLanguageCode("en-US").setName("en-US-Chirp3-HD-Iapetus").build());
        voiceMap.put(GoogleVoices.JAMES.getName(), VoiceSelectionParams.newBuilder().setLanguageCode("en-AU").setName("en-AU-Chirp3-HD-Algieba").build());
        voiceMap.put(GoogleVoices.JENNIFER.getName(), VoiceSelectionParams.newBuilder().setLanguageCode("en-US").setName("en-US-Chirp3-HD-Sulafat").build());
        voiceMap.put(GoogleVoices.JOSEPH.getName(), VoiceSelectionParams.newBuilder().setLanguageCode("en-US").setName("en-US-Chirp3-HD-Sadachbia").build());
        voiceMap.put(GoogleVoices.MARY.getName(), VoiceSelectionParams.newBuilder().setLanguageCode("en-US").setName("en-US-Chirp3-HD-Zephyr").build());
        voiceMap.put(GoogleVoices.MICHAEL.getName(), VoiceSelectionParams.newBuilder().setLanguageCode("en-US").setName("en-US-Chirp3-HD-Charon").build());
        voiceMap.put(GoogleVoices.OLIVIA.getName(), VoiceSelectionParams.newBuilder().setLanguageCode("en-GB").setName("en-GB-Chirp3-HD-Aoede").build());
        voiceMap.put(GoogleVoices.RACHEL.getName(), VoiceSelectionParams.newBuilder().setLanguageCode("en-US").setName("en-US-Chirp3-HD-Zephyr").build());
        voiceMap.put(GoogleVoices.STEVE.getName(), VoiceSelectionParams.newBuilder().setLanguageCode("en-US").setName("en-US-Chirp3-HD-Algenib").build());
    }

    public static GoogleVoiceProvider getInstance() {
        return INSTANCE;
    }

    /**
     * Wires the available-voice lookup (backed by the TTS client's {@code listVoices}), so a localized voice
     * name is validated against what the selected language actually offers before it is requested.
     */
    public void setAvailableVoices(Function<String, List<AvailableVoice>> resolver) {
        this.availableVoices = resolver;
    }

    /**
     * Whether the available-voice lookup is wired (the TTS engine has started). Before that, voice resolution
     * is optimistic, so callers that show the resolved quality should wait for this to be true.
     */
    public boolean hasVoiceLookup() {
        return availableVoices != null;
    }

    /**
     * Retrieves the user-selected AI voice as an GoogleVoices enum.
     *
     * @return GoogleVoices for the current AI voice, or default (Jennifer) if none selected.
     */
    @Override
    public GoogleVoices getUserSelectedVoice() {
        GoogleVoices aiVoice = SystemSession.getInstance().getGoogleVoice();
        return aiVoice != null ? aiVoice : GoogleVoices.JENNIFER; // Default to Jennifer
    }

    /**
     * Retrieves a random GoogleVoices enum value, excluding the user-selected AI voice.
     *
     * @return GoogleVoices for a random voice, or default (Jennifer) if none available.
     */
    @Override
    public GoogleVoices getRandomVoice() {
        GoogleVoices currentAiVoice = SystemSession.getInstance().getGoogleVoice();
        GoogleVoices[] availableVoices = Arrays.stream(GoogleVoices.values())
                .filter(voice -> !voice.getName().equals(currentAiVoice.getName()))
                .toArray(GoogleVoices[]::new);
        if (availableVoices.length == 0) {
            return GoogleVoices.JENNIFER; // Default to Jennifer
        }
        return availableVoices[new Random().nextInt(availableVoices.length)];
    }

    /**
     * Retrieves VoiceSelectionParams for a given GoogleVoices voice name.
     *
     * @param voiceName The GoogleVoices enum name (e.g., "Jennifer").
     * @return VoiceSelectionParams for the voice, or default (Jennifer) if not found.
     */
    @Override
    public VoiceSelectionParams getVoiceParams(String voiceName) {
        GoogleVoices voice = GoogleVoices.JENNIFER;
        if (voiceName != null) {
            try {
                voice = GoogleVoices.valueOf(voiceName);
                voiceName = voice.getName();
            } catch (IllegalArgumentException ignored) {
                // voiceName may already be the provider display name.
                for (GoogleVoices candidate : GoogleVoices.values()) {
                    if (candidate.getName().equals(voiceName)) {
                        voice = candidate;
                        break;
                    }
                }
            }
        }

        Language language = SystemSession.getInstance().getLanguage();
        String languageCode = googleLanguageCode(language);
        if (languageCode != null) {
            return VoiceSelectionParams.newBuilder()
                    .setLanguageCode(languageCode)
                    .setName(localizedVoiceName(language, voice))
                    .build();
        }

        VoiceSelectionParams params = voiceMap.get(voiceName);
        if (params == null) {
            // voiceName may be an enum name (e.g. "EMMA") rather than a display name ("Emma")
            try {
                params = voiceMap.get(GoogleVoices.valueOf(voiceName).getName());
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (params == null) {
            params = voiceMap.get(GoogleVoices.JENNIFER.getName());
        }
        return params;
    }

    private static String googleLanguageCode(Language language) {
        return switch (language) {
            case EN -> null;
            case RU -> "ru-RU";
            case UK -> "uk-UA";
            case DE -> "de-DE";
            case FR -> "fr-FR";
            case ES -> "es-ES";
            case IT -> "it-IT";
            case PT -> "pt-PT";
            case PTBZ -> "pt-BR";
        };
    }

    /**
     * Builds the provider voice name for a non-English language. Chirp3-HD voice characters are shared across
     * many locales, so a language with Chirp3-HD coverage keeps the selected voice's character and only swaps
     * the locale prefix - but the roster is not identical everywhere, so the name is used only when the API
     * actually offers it; otherwise (or for pt-PT, which has no Chirp3-HD voices) it falls back to a voice
     * known to exist for that language.
     */
    private String localizedVoiceName(Language language, GoogleVoices voice) {
        if (CHIRP3_HD_LANGUAGES.contains(language)) {
            String code = googleLanguageCode(language);
            List<AvailableVoice> offered = offeredVoices(code);
            String desired = code + "-Chirp3-HD-" + voice.getCharacter();
            // No lookup wired (unit tests / before start): optimistic. Otherwise use the exact character only if
            // the locale offers it, else another same-gender Chirp3-HD voice it does offer (keeps HD quality).
            if (offered == null || containsName(offered, desired)) {
                return desired;
            }
            String sameGenderHd = sameGenderChirp3HdVoice(offered, voice);
            if (sameGenderHd != null) {
                return sameGenderHd;
            }
        }
        // pt-PT (no Chirp3-HD), or a Chirp3-HD language that offers no HD voice of this gender: known-good Standard.
        return safeVoiceName(language, voice.isMale());
    }

    /** Voices Google offers for the language code, or {@code null} when no lookup is wired (optimistic path). */
    private List<AvailableVoice> offeredVoices(String languageCode) {
        Function<String, List<AvailableVoice>> resolver = availableVoices;
        return resolver == null ? null : resolver.apply(languageCode);
    }

    private static boolean containsName(List<AvailableVoice> offered, String voiceName) {
        return offered.stream().anyMatch(v -> v.name().equals(voiceName));
    }

    /**
     * A Chirp3-HD voice of the requested gender that the locale actually offers, chosen deterministically and
     * spread by the selected voice's position so different ships keep distinct voices. Null when the locale
     * offers no Chirp3-HD voice of that gender (the caller then falls back to a Standard voice).
     */
    private static String sameGenderChirp3HdVoice(List<AvailableVoice> offered, GoogleVoices voice) {
        List<String> candidates = offered.stream()
                .filter(v -> v.male() == voice.isMale())
                .map(AvailableVoice::name)
                .filter(name -> name.contains("-Chirp3-HD-"))
                .sorted()
                .toList();
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(voice.ordinal() % candidates.size());
    }

    /**
     * A voice known to exist for the given non-English language, used when the selected voice's Chirp3-HD
     * character is not available in that locale (the roster differs per locale) or the language has no Chirp3-HD
     * voices at all (pt-PT). A Standard voice is used because it is guaranteed to exist regardless of the
     * locale's Chirp3-HD roster; the HD path above already covers the common case, so this stays a safe
     * last resort rather than another HD voice that might itself be absent.
     */
    private static String safeVoiceName(Language language, boolean male) {
        return switch (language) {
            case EN -> "";
            case RU -> male ? "ru-RU-Standard-B" : "ru-RU-Standard-A";
            case UK -> "uk-UA-Standard-B";
            case DE -> male ? "de-DE-Standard-H" : "de-DE-Standard-G";
            case FR -> male ? "fr-FR-Standard-G" : "fr-FR-Standard-E";
            case ES -> male ? "es-ES-Standard-B" : "es-ES-Standard-E";
            case IT -> male ? "it-IT-Standard-C" : "it-IT-Standard-A";
            case PT -> male ? "pt-PT-Standard-B" : "pt-PT-Standard-A";
            case PTBZ -> male ? "pt-BR-Standard-B" : "pt-BR-Standard-A";
        };
    }
}
