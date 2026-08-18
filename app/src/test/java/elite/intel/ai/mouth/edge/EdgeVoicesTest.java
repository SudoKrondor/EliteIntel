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
 * voice with no Edge twin resolves to {@link EdgeVoices#DEFAULT_FEMALE} through {@code femaleOrDefault}.
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

    @Test
    void theDefaultFemaleIsFemaleAndSharedWithGoogle() {
        assertTrue(!EdgeVoices.DEFAULT_FEMALE.male(), "the default ship voice has to be female");
        assertEquals(GoogleVoices.DEFAULT_FEMALE.name(), EdgeVoices.DEFAULT_FEMALE.name(),
                "switching engines must not move a fleet left on the default voice");
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
