package elite.intel.ai.brain.commons;

import org.junit.jupiter.api.Test;

import java.text.Normalizer;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the forgiving action-id matching used by {@link ResponseRouter} to tolerate benign
 * differences between the id a (possibly small, local) model echoes and the registered handler id.
 */
class ResponseRouterMatchTest {

    @Test
    void matchesAcrossUnicodeNormalizationForms() {
        // Build the two forms explicitly so the distinction survives source-file encoding.
        String composed = Normalizer.normalize("café_command", Normalizer.Form.NFC);    // 'é' precomposed
        String decomposed = Normalizer.normalize("café_command", Normalizer.Form.NFD);  // 'e' + combining acute
        assertNotEquals(composed, decomposed, "test precondition: the two forms must differ byte-wise");

        String match = ResponseRouter.matchNormalized(ResponseRouter.normalizeId(decomposed), Set.of(composed));

        assertEquals(composed, match);
    }

    @Test
    void matchesIgnoringCaseAndSurroundingWhitespace() {
        String match = ResponseRouter.matchNormalized(
                ResponseRouter.normalizeId("  Deploy_Landing_Gear "), Set.of("deploy_landing_gear"));

        assertEquals("deploy_landing_gear", match);
    }

    @Test
    void matchesCyrillicEchoedInDifferentCase() {
        String match = ResponseRouter.matchNormalized(
                ResponseRouter.normalizeId("ЛЕТЕТЬ_К_МИССИИ"),
                Set.of("лететь_к_миссии"));

        assertEquals("лететь_к_миссии", match);
    }

    @Test
    void returnsNullWhenNoMatch() {
        assertNull(ResponseRouter.matchNormalized(ResponseRouter.normalizeId("unknown_action"), Set.of("a", "b")));
    }

    @Test
    void returnsNullWhenAmbiguous() {
        // Two registered keys collapse to the same normalized form - refuse to guess.
        assertNull(ResponseRouter.matchNormalized("key", Set.of("Key", "key")));
    }
}
