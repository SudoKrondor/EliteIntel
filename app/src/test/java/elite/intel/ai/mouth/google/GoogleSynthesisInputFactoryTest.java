package elite.intel.ai.mouth.google;

import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies that each Google voice family receives an input format it supports. */
class GoogleSynthesisInputFactoryTest {

    @Test
    void legacyChirpHdReceivesPlainText() {
        SynthesisInput input = GoogleSynthesisInputFactory.create(
                "Status ready.", voice("en-GB-Chirp-HD-F"));

        assertEquals(SynthesisInput.InputSourceCase.TEXT, input.getInputSourceCase());
        assertEquals("Status ready.", input.getText());
    }

    @Test
    void chirp3HdRetainsPunctuationAwareSsml() {
        SynthesisInput input = GoogleSynthesisInputFactory.create(
                "Status ready.", voice("en-US-Chirp3-HD-Sulafat"));

        assertEquals(SynthesisInput.InputSourceCase.SSML, input.getInputSourceCase());
        assertEquals("<speak>Status ready.<break time=\"300ms\"/></speak>", input.getSsml());
    }

    @Test
    void standardVoiceRetainsPunctuationAwareSsml() {
        SynthesisInput input = GoogleSynthesisInputFactory.create(
                "Status ready.", voice("ru-RU-Standard-A"));

        assertEquals(SynthesisInput.InputSourceCase.SSML, input.getInputSourceCase());
    }

    private static VoiceSelectionParams voice(String name) {
        return VoiceSelectionParams.newBuilder().setName(name).build();
    }
}
