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

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Builds the {@link LlmGateway} for the active provider. Most providers ride the OpenAI-compatible
 * tool-calling protocol ({@link OpenAiCompatibleLlmAdapter}); Anthropic ({@link AnthropicLlmAdapter}) and
 * Gemini ({@link GeminiLlmAdapter}) have native adapters. Cloud providers are wired one at a time: the map
 * below is the single source of truth for <em>both</em> gateway construction and the user-facing
 * "unsupported provider" message (each entry carries its display label), so adding a provider in one place
 * keeps the supported-list message accurate. Locally there is one host, LM Studio.
 * <p>
 * VEGA replaces the legacy command mode, so it uses the command-side local toggle to decide
 * local vs cloud, and {@link LlmProviderResolver} to detect the cloud provider (shared with ApiFactory).
 */
public final class VegaLlmGatewayFactory {

    /**
     * A wired VEGA provider: its user-facing label and gateway builder (cloud builders ignore the session).
     */
    private record WiredProvider(String label, Function<SystemSession, LlmGateway> builder) {
    }

    /**
     * Cloud providers with a wired VEGA adapter. Add an entry to wire one; its label joins the message.
     */
    private static final Map<ProviderEnum, WiredProvider> CLOUD_GATEWAYS = Map.of(
            ProviderEnum.MISTRAL, new WiredProvider("Mistral", session -> new VegaLlmGateway(
                    new MistralLlmAdapter(), typedTransport(body -> MistralClient.getInstance().sendVegaRequest(body)))),
            ProviderEnum.OPENAI, new WiredProvider("OpenAI", session -> new VegaLlmGateway(
                    new OpenAiLlmAdapter(), typedTransport(body -> OpenAiClient.getInstance().sendVegaRequest(body)))),
            ProviderEnum.GROK, new WiredProvider("Grok", session -> new VegaLlmGateway(
                    new GrokLlmAdapter(), typedTransport(body -> GrokClient.getInstance().sendVegaRequest(body)))),
            ProviderEnum.DEEPSEEK, new WiredProvider("DeepSeek", session -> new VegaLlmGateway(
                    new DeepSeekLlmAdapter(), typedTransport(body -> DeepSeekClient.getInstance().sendVegaRequest(body)))),
            ProviderEnum.ANTHROPIC, new WiredProvider("Claude", session -> new VegaLlmGateway(
                    new AnthropicLlmAdapter(), typedTransport(body -> AnthropicClient.getInstance().sendVegaRequest(body)))),
            ProviderEnum.GEMINI, new WiredProvider("Gemini", session -> new VegaLlmGateway(
                    new GeminiLlmAdapter(), typedTransport(body -> GeminiClient.getInstance()
                    .sendVegaRequest(body, GeminiClient.MODEL_FLASH)))));

    /**
     * The local host: LM Studio, riding the shared OpenAI-compatible protocol ({@code tool_choice=required},
     * no Mistral cache key) and serving the locally configured command model (e.g. Gemma 4). Ollama was
     * dropped in V1.1 maintenance - too slow to be usable, and support for it amounted to telling commanders
     * to switch to LM Studio.
     */
    private static final WiredProvider LOCAL_GATEWAY =
            new WiredProvider("LM Studio (Gemma 4)", session -> new VegaLlmGateway(
                    new LmStudioLlmAdapter(session.getLmStudioCommandModel().trim()),
                    typedTransport(body -> LMStudioClient.getInstance().sendVegaRequest(body))));

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
     * Creates the gateway for the configured provider, or fails with the dynamic supported-provider message.
     */
    public static LlmGateway create() {
        SystemSession session = SystemSession.getInstance();
        if (session.useLocalCommandLlm()) {
            return LOCAL_GATEWAY.builder().apply(session);
        }
        ProviderEnum provider = LlmProviderResolver.detectCloudProvider();
        return build(CLOUD_GATEWAYS.get(provider), provider, session);
    }

    /**
     * Runs the wired builder, or fails fast with the supported-provider message naming what was configured.
     */
    private static LlmGateway build(WiredProvider wired, ProviderEnum configured, SystemSession session) {
        if (wired == null) {
            throw new UnsupportedOperationException(unsupportedMessage(String.valueOf(configured)));
        }
        return wired.builder().apply(session);
    }

    /**
     * The user-facing message naming the configured (unsupported) provider and the providers VEGA
     * supports right now. The supported labels are derived from the wired adapters, so they stay correct
     * as providers are added.
     */
    static String unsupportedMessage(String configured) {
        return "VEGA mode does not support the " + configured + " provider yet. Supported now - cloud: "
                + labels(CLOUD_GATEWAYS.values()) + "; local: " + LOCAL_GATEWAY.label()
                + ". Configure a supported provider to start VEGA.";
    }

    /**
     * Comma-joined, alphabetically-ordered provider labels for a stable, readable supported list.
     */
    private static String labels(Collection<WiredProvider> providers) {
        return providers.stream().map(WiredProvider::label).sorted().collect(Collectors.joining(", "));
    }
}
