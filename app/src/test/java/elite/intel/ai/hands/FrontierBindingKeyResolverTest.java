package elite.intel.ai.hands;

import org.junit.jupiter.api.Test;

import java.awt.event.KeyEvent;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrontierBindingKeyResolverTest {

    @Test
    void resolvesNamedFrontierCharactersAsNativeCharacterPositions() {
        int comma = FrontierBindingKeyResolver.resolve("Key_Comma", Map.of());
        int circumflex = FrontierBindingKeyResolver.resolve("Key_Circumflex", Map.of());
        int superscriptTwo = FrontierBindingKeyResolver.resolve("Key_SuperscriptTwo", Map.of());

        assertTrue(KeyProcessor.isNativeCharacterCode(comma));
        assertEquals(',', KeyProcessor.nativeCharacter(comma));
        assertEquals('^', KeyProcessor.nativeCharacter(circumflex));
        assertEquals('²', KeyProcessor.nativeCharacter(superscriptTwo));
    }

    @Test
    void preservesStandardMappingsForLettersAndDedicatedKeys() {
        Map<String, Integer> mappings = Map.of(
                "KEY_M", KeyEvent.VK_M,
                "KEY_NUMPAD_DIVIDE", KeyProcessor.NATIVE_NUMPAD_DIVIDE
        );

        assertEquals(KeyEvent.VK_M, FrontierBindingKeyResolver.resolve("Key_M", mappings));
        assertEquals(KeyProcessor.NATIVE_NUMPAD_DIVIDE,
                FrontierBindingKeyResolver.resolve("Key_Numpad_Divide", mappings));
    }

    @Test
    void resolvesLiteralLocalizedCharacters() {
        int eAcute = FrontierBindingKeyResolver.resolve("Key_é", Map.of());
        assertEquals('é', KeyProcessor.nativeCharacter(eAcute));
    }

    @Test
    void rejectsUnknownMultiCharacterTokens() {
        assertNull(FrontierBindingKeyResolver.resolve("Key_NotARealKey", Map.of()));
    }
}
