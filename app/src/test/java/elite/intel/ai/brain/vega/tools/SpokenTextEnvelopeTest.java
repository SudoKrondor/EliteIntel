package elite.intel.ai.brain.vega.tools;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Guards the narrow shape of the envelope unwrap: it must free wrapped prose and leave ordinary prose alone.
 */
class SpokenTextEnvelopeTest {

    @Test
    @DisplayName("ordinary spoken prose is returned untouched")
    void leavesProseAlone() {
        String prose = "Dex, Tubus Compagibus logged. First of three samples; 800 meters between samples.";
        assertEquals(prose, SpokenTextEnvelope.unwrap(prose));
    }

    @Test
    @DisplayName("prose that merely contains braces is not treated as an envelope")
    void leavesBracesInsideProseAlone() {
        String prose = "The signal reads {unknown} on the scanner.";
        assertEquals(prose, SpokenTextEnvelope.unwrap(prose));
    }

    @Test
    @DisplayName("a well-formed single-field JSON envelope yields its payload")
    void unwrapsJsonEnvelope() {
        assertEquals("Docking granted, pad seven.",
                SpokenTextEnvelope.unwrap("{\"text_to_speech_response\": \"Docking granted, pad seven.\"}"));
    }

    @Test
    @DisplayName("the key is not trusted by name - the model invents it")
    void unwrapsJsonEnvelopeUnderAnyKey() {
        assertEquals("Fuel scoop retracted.",
                SpokenTextEnvelope.unwrap("{\"speech\": \"Fuel scoop retracted.\"}"));
    }

    @Test
    @DisplayName("the improvised, non-JSON envelope seen in the field yields its payload")
    void unwrapsLooseEnvelope() {
        assertEquals("Dex, Tubus Compagibus logged. First of three samples; 800 meters between samples.",
                SpokenTextEnvelope.unwrap(
                        "{texttospeech_response - Dex, Tubus Compagibus logged. First of three samples; 800 meters between samples.}"));
    }

    @Test
    @DisplayName("a colon-separated improvised envelope yields its payload")
    void unwrapsLooseColonEnvelope() {
        assertEquals("Hardpoints deployed.", SpokenTextEnvelope.unwrap("{response: Hardpoints deployed.}"));
    }

    @Test
    @DisplayName("a multi-field object is left alone rather than guessed at")
    void leavesMultiFieldObjectAlone() {
        String multi = "{\"text\": \"one\", \"urgency\": \"high\"}";
        assertEquals(multi, SpokenTextEnvelope.unwrap(multi));
    }

    @Test
    @DisplayName("braced text with no key/value shape is left alone")
    void leavesUnrecognisedBracedTextAlone() {
        String braced = "{ scanning the surface for anomalies }";
        assertEquals(braced, SpokenTextEnvelope.unwrap(braced));
    }

    @Test
    @DisplayName("null and blank survive the guard")
    void toleratesNullAndBlank() {
        assertNull(SpokenTextEnvelope.unwrap(null));
        assertEquals("", SpokenTextEnvelope.unwrap(""));
    }
}
