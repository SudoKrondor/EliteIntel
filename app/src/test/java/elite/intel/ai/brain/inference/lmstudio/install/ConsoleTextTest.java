package elite.intel.ai.brain.inference.lmstudio.install;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Lines as {@code lms} and LM Studio's install scripts printed them in the smoke tests.
 */
class ConsoleTextTest {

    @Test
    void plainTextIsKept() {
        assertEquals(Optional.of("Install completed at D:\\LMStudio\\llmster\\0.0.27-1."),
                ConsoleText.clean("Install completed at D:\\LMStudio\\llmster\\0.0.27-1."));
    }

    @Test
    void coloursAndCursorCodesAreRemoved() {
        assertEquals(Optional.of("lms daemon up"), ConsoleText.clean("    \u001B[33mlms daemon up\u001B[0m"));
        assertEquals(Optional.of("google/gemma-4-e4b - Resolving..."),
                ConsoleText.clean("\u001B[?25l   \u2826 google/gemma-4-e4b - Resolving...\u001B[0K"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "##O#-",
            "#=#=#",
            "##################################################### 100.0%",
            "####                                                    8.4%",
            "                                                        0.0%",
            "\u001B[0K",
            "   ",
    })
    void barsAndBlankRedrawsAreDropped(String line) {
        assertEquals(Optional.empty(), ConsoleText.clean(line));
    }
}
