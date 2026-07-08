package elite.intel.ai.brain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The supported-local-model match is a case-insensitive substring on the root name {@code gemma-4-e4b},
 * so namespaced/tagged variants (e.g. {@code google/gemma-4-e4b}, {@code gemma-4-e4b:latest}) still pass
 * while V1.0's {@code tulu} model and any other model fail.
 */
class LocalLlmModelCheckTest {

    @Test
    void acceptsSupportedRootWithAnyNamespaceOrTag() {
        assertTrue(LocalLlmModelCheck.isSupported("gemma-4-e4b"));
        assertTrue(LocalLlmModelCheck.isSupported("google/gemma-4-e4b"));
        assertTrue(LocalLlmModelCheck.isSupported("gemma-4-e4b:latest"));
        assertTrue(LocalLlmModelCheck.isSupported("  GOOGLE/Gemma-4-E4B  "));
    }

    @Test
    void rejectsOtherModelsAndBlanks() {
        assertFalse(LocalLlmModelCheck.isSupported("tulu3.1:8b-supernova"));
        assertFalse(LocalLlmModelCheck.isSupported("gemma-2-9b"));
        assertFalse(LocalLlmModelCheck.isSupported(""));
        assertFalse(LocalLlmModelCheck.isSupported("   "));
        assertFalse(LocalLlmModelCheck.isSupported(null));
    }
}
