package elite.intel.ai.mouth.supertonic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pins the cast to what the model actually produces. The sex of each speaker index was measured (median
 * fundamental in English and Russian - see the enum's notes), and the ship voice's sex drives how VEGA
 * refers to itself, so a name that says F while the flag says male would misgender every line.
 */
class SupertonicVoicesTest {

    @Test
    void theNameTheFlagAndTheSpeakerIndexAgree() {
        for (SupertonicVoices voice : SupertonicVoices.values()) {
            boolean namedMale = voice.name().startsWith("M");
            assertEquals(namedMale, voice.isMale(), voice + " is named one sex and flagged the other");
            assertEquals(namedMale ? "Male" : "Female", voice.getDescription(), voice.name());
            // Measured: sids 0-4 speak at 165-233 Hz, sids 5-9 at 84-140 Hz.
            assertEquals(voice.getSid() >= 5, voice.isMale(), voice + " sid " + voice.getSid());
        }
    }

    @Test
    void theShipDefaultIsAFemaleVoiceLikeEveryOtherEngine() {
        assertFalse(SupertonicVoices.DEFAULT_VOICE.isMale());
        assertSame(SupertonicVoices.DEFAULT_VOICE, SupertonicVoices.voiceOrDefault(null));
        assertSame(SupertonicVoices.DEFAULT_VOICE, SupertonicVoices.voiceOrDefault("GEORGE"), "a Kokoro name collapses");
        assertSame(SupertonicVoices.M3, SupertonicVoices.voiceOrDefault("M3"));
    }

    @Test
    void tenSpeakersWithDistinctIndices() {
        assertEquals(10, SupertonicVoices.values().length);
        assertEquals(10, java.util.Arrays.stream(SupertonicVoices.values()).mapToInt(SupertonicVoices::getSid).distinct().count());
    }
}
