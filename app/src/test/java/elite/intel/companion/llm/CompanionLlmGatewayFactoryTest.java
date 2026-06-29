package elite.intel.companion.llm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the user-facing "unsupported provider" message: it names the configured provider and lists the
 * providers companion mode currently supports, derived dynamically from the wired-adapter maps (so the list
 * stays correct as providers are added one at a time).
 */
class CompanionLlmGatewayFactoryTest {

    @Test
    void unsupportedMessageNamesConfiguredProviderAndTheSupportedOnes() {
        String message = CompanionLlmGatewayFactory.unsupportedMessage("OPENAI");

        // The provider the user actually configured is named, so they know what was rejected.
        assertTrue(message.contains("OPENAI"), message);
        // The currently-wired providers are listed dynamically by their friendly labels (cloud + local).
        assertTrue(message.contains("Mistral"), message);
        assertTrue(message.contains("LM Studio (Gemma 4)"), message);
    }
}
