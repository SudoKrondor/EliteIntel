package elite.intel.ai.mouth.google;

import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;

/**
 * Chooses a Cloud TTS input format that is compatible with the resolved Google voice.
 * Chirp-HD and Chirp3-HD voices accept plain text only; Standard voices retain the SSML pause treatment.
 */
final class GoogleSynthesisInputFactory {
    private static final String LEGACY_CHIRP_HD_MARKER = "-Chirp-HD-";
    private static final String CHIRP3_HD_MARKER = "-Chirp3-HD-";

    private GoogleSynthesisInputFactory() {
    }

    /**
     * Creates a synthesis input without sending unsupported SSML to Chirp voices.
     *
     * @param text sanitized, speakable text
     * @param voice resolved Google voice parameters
     * @return plain text for Chirp voices; otherwise punctuation-aware SSML
     */
    static SynthesisInput create(String text, VoiceSelectionParams voice) {
        if (requiresPlainText(voice)) {
            return SynthesisInput.newBuilder().setText(text).build();
        }
        return SynthesisInput.newBuilder().setSsml(GoogleSsml.wrap(text)).build();
    }

    private static boolean requiresPlainText(VoiceSelectionParams voice) {
        if (voice == null) {
            return false;
        }
        String name = voice.getName();
        return name.contains(LEGACY_CHIRP_HD_MARKER) || name.contains(CHIRP3_HD_MARKER);
    }
}
