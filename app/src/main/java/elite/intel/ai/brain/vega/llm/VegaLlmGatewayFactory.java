package elite.intel.ai.brain.vega.llm;

import com.google.gson.JsonObject;
import elite.intel.ai.LlmProviderResolver;
import elite.intel.ai.ProviderEnum;
import elite.intel.ai.brain.AiTransportResult;
import elite.intel.ai.brain.inference.anthropic.AnthropicClient;
import elite.intel.ai.brain.inference.deepseek.DeepSeekClient;
import elite.intel.ai.brain.inference.gemini.GeminiClient;
import elite.intel.ai.brain.inference.lmstudio.LMStudioClient;
import elite.intel.ai.brain.inference.mistral.MistralClient;
import elite.intel.ai.brain.inference.openai.OpenAiClient;
import elite.intel.ai.brain.inference.xai.GrokClient;
import elite.intel.session.SystemSession;

import java.util.function.Function;

/**
 * Builds the {@link LlmGateway} for the active provider. Most providers ride the OpenAI-compatible
 * tool-calling protocol ({@link OpenAiCompatibleLlmAdapter}); Anthropic ({@link AnthropicLlmAdapter}) and
 * Gemini ({@link GeminiLlmAdapter}) have native adapters. The cloud switch is exhaustive over
 * {@link ProviderEnum}, so every provider the commander can pick has a gateway - a new provider does not
 * compile until it is wired here. Locally there is one host, LM Studio.
 * <p>
 * VEGA replaces the legacy command mode, so it uses the command-side local toggle to decide local vs cloud,
 * and {@link LlmProviderResolver} for the cloud provider (shared with ApiFactory).
 */
public final class VegaLlmGatewayFactory {

    private VegaLlmGatewayFactory() {
    }

    /**
     * Bridges a provider client that exposes typed HTTP outcomes into VEGA transport seam.
     */
    private static LlmTransport typedTransport(Function<String, AiTransportResult> sender) {
        return new LlmTransport() {
            @Override
            public JsonObject send(String requestBody) {
                AiTransportResult outcome = sendOutcome(requestBody);
                if (outcome instanceof AiTransportResult.Success success) {
                    return success.response();
                }
                AiTransportResult.Failure failure = (AiTransportResult.Failure) outcome;
                throw new IllegalStateException("VEGA transport failed: " + failure.diagnostic());
            }

            @Override
            public AiTransportResult sendOutcome(String requestBody) {
                return sender.apply(requestBody);
            }
        };
    }

    /**
     * Creates the gateway for the configured provider. A cloud setup with no provider selected gets
     * {@link UnselectedProviderGateway} rather than a failure: the services still start, so the commander hears
     * {@link elite.intel.setup.SetupCheck} say what is missing, and the reflex commands that need no model keep
     * working until they pick one.
     */
    public static LlmGateway create() {
        SystemSession session = SystemSession.getInstance();
        if (session.useLocalCommandLlm()) {
            return localGateway(session);
        }
        return LlmProviderResolver.cloudProvider()
                .map(VegaLlmGatewayFactory::cloudGateway)
                .orElseGet(UnselectedProviderGateway::new);
    }

    /**
     * The local host: LM Studio, riding the shared OpenAI-compatible protocol ({@code tool_choice=required},
     * no Mistral cache key) and serving the locally configured command model (e.g. Gemma 4). Ollama was
     * dropped in V1.1 maintenance - too slow to be usable, and support for it amounted to telling commanders
     * to switch to LM Studio.
     */
    private static LlmGateway localGateway(SystemSession session) {
        return new VegaLlmGateway(
                new LmStudioLlmAdapter(session.getLmStudioCommandModel().trim()),
                typedTransport(body -> LMStudioClient.getInstance().sendVegaRequest(body)));
    }

    private static LlmGateway cloudGateway(ProviderEnum provider) {
        return switch (provider) {
            case MISTRAL -> new VegaLlmGateway(
                    new MistralLlmAdapter(), typedTransport(body -> MistralClient.getInstance().sendVegaRequest(body)));
            case OPENAI -> new VegaLlmGateway(
                    new OpenAiLlmAdapter(), typedTransport(body -> OpenAiClient.getInstance().sendVegaRequest(body)));
            case GROK -> new VegaLlmGateway(
                    new GrokLlmAdapter(), typedTransport(body -> GrokClient.getInstance().sendVegaRequest(body)));
            case DEEPSEEK -> new VegaLlmGateway(
                    new DeepSeekLlmAdapter(), typedTransport(body -> DeepSeekClient.getInstance().sendVegaRequest(body)));
            case ANTHROPIC -> new VegaLlmGateway(
                    new AnthropicLlmAdapter(), typedTransport(body -> AnthropicClient.getInstance().sendVegaRequest(body)));
            case GEMINI -> new VegaLlmGateway(
                    new GeminiLlmAdapter(), typedTransport(body -> GeminiClient.getInstance()
                    .sendVegaRequest(body, GeminiClient.MODEL_FLASH)));
        };
    }
}
