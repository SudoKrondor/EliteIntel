package elite.intel.companion.llm;

import elite.intel.ai.LlmProviderResolver;
import elite.intel.ai.ProviderEnum;
import elite.intel.ai.brain.inference.mistral.MistralClient;
import elite.intel.session.SystemSession;

/**
 * Builds the {@link LlmGateway} for the active provider. The provider seam is in place, but only
 * Mistral is wired so far; any other provider (including local LM Studio / Ollama) fails fast with a
 * clear message until its {@link CompanionLlmDialect} is added.
 * <p>
 * Companion mode replaces the legacy command mode, so it uses the command-side local toggle to decide
 * local vs cloud, and {@link LlmProviderResolver} to detect the cloud provider (shared with ApiFactory).
 */
public final class CompanionLlmGatewayFactory {

    private CompanionLlmGatewayFactory() {
    }

    /** Creates the gateway for the configured provider, or fails if it is not yet supported. */
    public static LlmGateway create() {
        if (SystemSession.getInstance().useLocalCommandLlm()) {
            throw new UnsupportedOperationException(
                    "Companion mode currently supports only Mistral; local LLMs are not wired yet.");
        }
        ProviderEnum provider = LlmProviderResolver.detectCloudProvider();
        if (provider != ProviderEnum.MISTRAL) {
            throw new UnsupportedOperationException(
                    "Companion mode currently supports only Mistral; configured provider: " + provider);
        }
        return new CompanionLlmGateway(
                new MistralToolCallDialect(),
                body -> MistralClient.getInstance().sendJsonRequest(body));
    }
}
