package elite.intel.ai.mouth.edge;

import elite.intel.ai.mouth.google.GoogleVoices;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pins the invariant the fleet grid relies on: every {@link EdgeVoices} constant has a {@link GoogleVoices}
 * twin under the same name, with the same display name and gender.
 * <p>
 * A ship stores one voice name whichever engine is active, and {@code CommanderTabPanel.edgeVoiceLabel} reads
 * an Edge voice's accent descriptor straight out of {@link GoogleVoices} rather than duplicating "American
 * female" and "British female" literals in both enums. Adding an Edge voice with no Google twin would throw
 * {@link IllegalArgumentException} on the EDT while painting the fleet grid, a long way from the edit that
 * caused it. This test fails at the edit instead.
 * <p>
 * The correspondence is one-way: Google may carry voices Edge does not offer (the WaveNet pair), and a stored
 * voice with no Edge twin resolves to {@link EdgeVoices#DEFAULT_VOICE} through {@code voiceOrDefault}.
 */
class EdgeVoicesTest {

    @Test
    void everyEdgeVoiceHasAGoogleTwinWithTheSameIdentity() {
        for (EdgeVoices edge : EdgeVoices.values()) {
            GoogleVoices google = googleTwin(edge);
            assertNotNull(google, "no GoogleVoices constant named " + edge.name()
                    + "; the fleet grid resolves an Edge voice's descriptor through GoogleVoices");
            assertEquals(google.getDisplayName(), edge.displayName(),
                    "display name differs for " + edge.name());
            assertEquals(google.isMale(), edge.male(), "gender differs for " + edge.name());
        }
    }

    /**
     * Ship voices are the commander's pick, male or female - but the default is not part of that change:
     * a fleet nobody has touched must keep sounding exactly as it did, on every engine.
     */
    @Test
    void theDefaultVoiceIsStillFemaleAndSharedWithGoogle() {
        assertFalse(EdgeVoices.DEFAULT_VOICE.male(), "the default ship voice stays female");
        assertEquals(GoogleVoices.DEFAULT_VOICE.name(), EdgeVoices.DEFAULT_VOICE.name(),
                "switching engines must not move a fleet left on the default voice");
    }

    /**
     * A male selection is a real selection: it must survive the ship-voice seam rather than collapse.
     */
    @Test
    void aMaleSelectionKeepsItsOwnVoice() {
        assertEquals(EdgeVoices.JAKE, EdgeVoices.voiceOrDefault(EdgeVoices.JAKE.name()));
        assertEquals(EdgeVoices.JAKE, EdgeVoices.voiceOrDefault(EdgeVoices.JAKE.defaultShortName()));
        assertEquals(EdgeVoices.DEFAULT_VOICE, EdgeVoices.voiceOrDefault("not-an-edge-voice"));
    }

    /**
     * Resolves by name without {@code valueOf}, so a missing twin fails the assertion instead of throwing.
     */
    private static GoogleVoices googleTwin(EdgeVoices edge) {
        for (GoogleVoices google : GoogleVoices.values()) {
            if (google.name().equals(edge.name())) {
                return google;
            }
        }
        return null;
    }
}
