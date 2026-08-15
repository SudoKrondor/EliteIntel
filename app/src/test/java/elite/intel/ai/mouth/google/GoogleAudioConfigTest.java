package elite.intel.ai.mouth.google;

import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import elite.intel.ai.mouth.kokoro.KokoroVoices;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GoogleAudioConfigTest {

    @Test
    void configuredPitchIsAppliedToResolvedWaveNetVoice() {
        AudioConfig config = GoogleTTSImpl.createAudioConfig(voice("en-GB-Wavenet-F"), 1.25, 7);

        assertEquals(7.0, config.getPitch());
        assertEquals(1.25, config.getSpeakingRate(), "existing speech-rate behavior must be preserved");
    }

    @Test
    void configuredPitchIsNotAppliedToNonWaveNetGoogleVoice() {
        AudioConfig config = GoogleTTSImpl.createAudioConfig(voice("en-US-Chirp3-HD-Zephyr"), 1.25, 7);

        assertEquals(0.0, config.getPitch());
        assertFalse(hasPitch(config));
        assertEquals(1.25, config.getSpeakingRate());
    }

    @Test
    void zeroPitchLeavesWaveNetAtNativePitch() {
        AudioConfig config = GoogleTTSImpl.createAudioConfig(voice("en-GB-Wavenet-N"), 0.8, 0);

        assertEquals(0.0, config.getPitch());
        assertFalse(hasPitch(config));
        assertEquals(0.8, config.getSpeakingRate());
    }

    @Test
    void localVoiceIdentifiersCannotReceiveGooglePitch() {
        AudioConfig config = GoogleTTSImpl.createAudioConfig(voice(KokoroVoices.NOVA.name()), 1.0, -9);

        assertEquals(0.0, config.getPitch());
        assertFalse(hasPitch(config));
    }

    private static boolean hasPitch(AudioConfig config) {
        return config.getAllFields().containsKey(AudioConfig.getDescriptor().findFieldByName("pitch"));
    }

    private static VoiceSelectionParams voice(String name) {
        return VoiceSelectionParams.newBuilder().setName(name).build();
    }
}
