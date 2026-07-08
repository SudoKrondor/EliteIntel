package elite.intel.ai.brain.inference.ollama;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Covers {@link OllamaClient#openAiCompatibleUrl}: companion mode derives Ollama's OpenAI-compatible
 * endpoint from the configured native address so a single Ollama address setting serves both paths.
 */
class OllamaClientTest {

    @Test
    void derivesCompatibleEndpointFromDefaultNativeAddress() {
        assertEquals("http://localhost:11434/v1/chat/completions",
                OllamaClient.openAiCompatibleUrl("http://localhost:11434/api/chat"));
    }

    @Test
    void keepsCustomHostAndPortWhenDeriving() {
        assertEquals("http://192.168.1.5:11434/v1/chat/completions",
                OllamaClient.openAiCompatibleUrl("http://192.168.1.5:11434/api/chat"));
    }

    @Test
    void trimsWhitespaceAroundTheConfiguredAddress() {
        assertEquals("http://localhost:11434/v1/chat/completions",
                OllamaClient.openAiCompatibleUrl("  http://localhost:11434/api/chat  "));
    }
}
