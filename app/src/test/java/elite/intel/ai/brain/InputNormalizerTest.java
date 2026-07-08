package elite.intel.ai.brain;

import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the {@link InputNormalizer} idempotency rule against regression: when a synonym expands into a
 * canonical that CONTAINS the synonym (the RU rule "боевой" -> "боевой режим"), an input that already carries
 * the canonical must not be expanded again into duplicated text ("боевой режим режим"), and the guard must not
 * over-skip a bare synonym that has not been expanded yet. Uses the real RU rule set (no seam to inject).
 */
class InputNormalizerTest {

    private final InputNormalizer normalizer = InputNormalizer.getInstance();

    @BeforeEach
    void useRussian() {
        SystemSession.getInstance().setLanguage(Language.RU);
    }

    @Test
    void doesNotDuplicateWhenCanonicalAlreadyPresent() {
        // "боевой" -> "боевой режим" must be skipped here: its canonical is already in the input.
        String result = normalizer.normalize("боевой режим");
        assertFalse(result.contains("режим режим"), "guard must not duplicate the contained synonym: " + result);
    }

    @Test
    void stillExpandsWhenCanonicalAbsent() {
        // The guard must not over-skip: a bare synonym whose canonical is NOT yet present still expands.
        String result = normalizer.normalize("боевой");
        assertTrue(result.contains("боевой режим"), "bare synonym should still expand: " + result);
    }
}
