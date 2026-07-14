package elite.intel.ai.mouth.google;

import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;

/**
 * Chooses a Cloud TTS input format that is compatible with the resolved Google voice.
 * Legacy Chirp-HD voices reject SSML, whereas Chirp3-HD and Standard voices retain the SSML pause treatment.
 */
final class GoogleSynthesisInputFactory {
    private static final String LEGACY_CHIRP_HD_MARKER = "-Chirp-HD-";

    private GoogleSynthesisInputFactory() {
    }

    /**
     * Creates a synthesis input for one resolved voice without sending unsupported SSML to legacy Chirp-HD.
     *
     * @param text sanitized, speakable text
     * @param voice resolved Google voice parameters
     * @return plain text for legacy Chirp-HD; otherwise punctuation-aware SSML
     */
    static SynthesisInput create(String text, VoiceSelectionParams voice) {
        if (isLegacyChirpHd(voice)) {
            return SynthesisInput.newBuilder().setText(text).build();
        }
        return SynthesisInput.newBuilder().setSsml(GoogleSsml.wrap(text)).build();
    }

    private static boolean isLegacyChirpHd(VoiceSelectionParams voice) {
        return voice != null && voice.getName().contains(LEGACY_CHIRP_HD_MARKER);
    }
}
