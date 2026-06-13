package elite.intel.ai.mouth.google;

/**
 * Interface for providing voice configurations and selection logic for TTS providers.
 * Generic type T represents the provider-specific voice parameter object.
 */
public interface VoiceProvider<T> {

    /**
     * Retrieves the user-selected AI voice as an AiVoices enum.
     *
     * @return AiVoices for the current AI voice, or default if none selected.
     */
    GoogleVoices getUserSelectedVoice();

    /**
     * Retrieves a random AiVoices enum value, excluding the user-selected AI voice.
     *
     * @return AiVoices for a random voice, or default if none available.
     */
    GoogleVoices getRandomVoice();

    /**
     * Retrieves the speech rate for a given AiVoices voice name.
     *
     * @param voiceName The AiVoices enum name (e.g., "Jennifer").
     * @return The speech rate for the voice, or default if not found.
     */
    double getSpeechRate(String voiceName);

    /**
     * Retrieves the provider-specific voice parameters for a given AiVoices voice name.
     *
     * @param voiceName The AiVoices enum name (e.g., "Jennifer").
     * @return Provider-specific voice parameters T for the voice, or default if not found.
     */
    T getVoiceParams(String voiceName);
}