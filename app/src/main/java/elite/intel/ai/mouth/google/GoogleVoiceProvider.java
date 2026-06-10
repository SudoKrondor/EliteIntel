package elite.intel.ai.mouth.google;

import com.google.cloud.texttospeech.v1.SsmlVoiceGender;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import elite.intel.ai.mouth.GoogleVoices;
import elite.intel.ai.mouth.VoiceProvider;
import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Singleton class responsible for managing Google TTS voice mappings and selection logic.
 * Maps AiVoices enum names (e.g., "Jennifer") to Google VoiceSelectionParams and provides
 * methods to retrieve user-selected and random voices dynamically as AiVoices enums.
 */
public class GoogleVoiceProvider implements VoiceProvider<VoiceSelectionParams> {
    private static final GoogleVoiceProvider INSTANCE = new GoogleVoiceProvider();
    private final Map<String, VoiceSelectionParams> voiceMap;

    private GoogleVoiceProvider() {
        // Initialize voice mappings using AiVoices names
        voiceMap = new HashMap<>();
        //voiceMap.put(AiVoices.BETTY.getName(), VoiceSelectionParams.newBuilder().setLanguageCode("en-GB").setName("en-GB-Chirp3-HD-Aoede").build());
        //voiceMap.put(AiVoices.CHARLES.getName(), VoiceSelectionParams.newBuilder().setLanguageCode("en-GB").setName("en-GB-Chirp3-HD-Algenib").build());
        //voiceMap.put(AiVoices.KAREN.getName(), VoiceSelectionParams.newBuilder().setLanguageCode("en-GB").setName("en-GB-Neural2-A").build());
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
     * Retrieves the user-selected AI voice as an AiVoices enum.
     *
     * @return AiVoices for the current AI voice, or default (Jennifer) if none selected.
     */
    @Override
    public GoogleVoices getUserSelectedVoice() {
        GoogleVoices aiVoice = SystemSession.getInstance().getGoogleVoice();
        return aiVoice != null ? aiVoice : GoogleVoices.JENNIFER; // Default to Jennifer
    }

    /**
     * Retrieves a random AiVoices enum value, excluding the user-selected AI voice.
     *
     * @return AiVoices for a random voice, or default (Jennifer) if none available.
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
     * Retrieves the speech rate for a given AiVoices voice name.
     *
     * @param voiceName The AiVoices enum name (e.g., "Jennifer").
     * @return The speech rate for the voice, or default (1.2) if not found.
     */
    @Override
    public double getSpeechRate(String voiceName) {
        for (GoogleVoices voice : GoogleVoices.values()) {
            if (voice.getName().equals(voiceName)) {
                return voice.getSpeechRate();
            }
        }
        return 1.2; // Default speech rate
    }

    /**
     * Retrieves VoiceSelectionParams for a given AiVoices voice name.
     *
     * @param voiceName The AiVoices enum name (e.g., "Jennifer").
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

        String languageCode = googleLanguageCode(SystemSession.getInstance().getLanguage());
        if (languageCode != null) {
            String providerVoiceName = googleVoiceName(SystemSession.getInstance().getLanguage(), voice.isMale());
            return VoiceSelectionParams.newBuilder()
                    .setLanguageCode(languageCode)
                    .setName(providerVoiceName)
                    .setSsmlGender(voice.isMale() ? SsmlVoiceGender.MALE : SsmlVoiceGender.FEMALE)
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
        };
    }

    private static String googleVoiceName(Language language, boolean male) {
        return switch (language) {
            case EN -> "";
            case RU -> male ? "ru-RU-Standard-B" : "ru-RU-Standard-E";
            case UK -> "uk-UA-Standard-B";
            case DE -> male ? "de-DE-Standard-H" : "de-DE-Standard-G";
            case FR -> male ? "fr-FR-Standard-G" : "fr-FR-Standard-E";
            case ES -> male ? "es-ES-Standard-B" : "es-ES-Standard-E";
        };
    }
}
