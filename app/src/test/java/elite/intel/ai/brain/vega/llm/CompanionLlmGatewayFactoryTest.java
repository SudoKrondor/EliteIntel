package elite.intel.ai.brain.vega.llm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the user-facing "unsupported provider" message: it names the configured provider and lists the
 * providers companion mode currently supports, derived dynamically from the wired adapters (so the list
 * stays correct as providers are added one at a time).
 */
class CompanionLlmGatewayFactoryTest {

    @Test
    void unsupportedMessageNamesConfiguredProviderAndTheSupportedOnes() {
        // COHERE stands in for any provider with no wired companion adapter (unsupportedMessage just formats
        // whatever configured name it is given).
        String message = CompanionLlmGatewayFactory.unsupportedMessage("COHERE");

        // The provider the user actually configured is named, so they know what was rejected.
        assertTrue(message.contains("COHERE"), message);
        // The currently-wired providers are listed dynamically by their friendly labels (cloud + local).
        assertTrue(message.contains("Mistral"), message);
        assertTrue(message.contains("OpenAI"), message);
        assertTrue(message.contains("Grok"), message);
        assertTrue(message.contains("DeepSeek"), message);
        assertTrue(message.contains("Claude"), message);
        assertTrue(message.contains("Gemini"), message);
        assertTrue(message.contains("LM Studio (Gemma 4)"), message);
        // Ollama was dropped; naming it as supported would send commanders back to the host we removed.
        assertFalse(message.contains("Ollama"), message);
    }
}
