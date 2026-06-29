package elite.intel.companion.llm;

import elite.intel.ai.LlmProviderResolver;
import elite.intel.ai.ProviderEnum;
import elite.intel.ai.brain.LocalLlmProvider;
import elite.intel.ai.brain.inference.lmstudio.LMStudioClient;
import elite.intel.ai.brain.inference.mistral.MistralClient;
import elite.intel.session.SystemSession;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Builds the {@link LlmGateway} for the active provider. Companion mode is built on the OpenAI-compatible
 * tool-calling protocol ({@link OpenAiCompatibleLlmAdapter}), but providers are wired one at a time. The two
 * maps below are the single source of truth for <em>both</em> gateway construction and the user-facing
 * "unsupported provider" message (each entry carries its display label), so adding a provider in one place
 * keeps the supported-list message accurate.
 * <p>
 * Companion mode replaces the legacy command mode, so it uses the command-side local toggle to decide
 * local vs cloud, and {@link LlmProviderResolver} to detect the cloud provider (shared with ApiFactory).
 */
public final class CompanionLlmGatewayFactory {

    /** A wired companion provider: its user-facing label and gateway builder (cloud builders ignore the session). */
    private record WiredProvider(String label, Function<SystemSession, LlmGateway> builder) {}

    /** Cloud providers with a wired companion adapter. Add an entry to wire one; its label joins the message. */
    private static final Map<ProviderEnum, WiredProvider> CLOUD_GATEWAYS = Map.of(
            ProviderEnum.MISTRAL, new WiredProvider("Mistral", session -> new CompanionLlmGateway(
                    new MistralLlmAdapter(), body -> MistralClient.getInstance().sendJsonRequest(body))));

    /**
     * Local providers with a wired companion adapter. Only LM Studio (OpenAI-compatible,
     * {@code tool_choice=required}, no Mistral cache key) is wired; Ollama is not, because its native API is
     * not OpenAI-compatible in the same way.
     */
    private static final Map<LocalLlmProvider, WiredProvider> LOCAL_GATEWAYS = Map.of(
            LocalLlmProvider.LMSTUDIO, new WiredProvider("LM Studio (Gemma 4)", session -> new CompanionLlmGateway(
                    new LmStudioLlmAdapter(session.getLmStudioCommandModel().trim()),
                    body -> LMStudioClient.getInstance().sendJsonRequest(body))));

    private CompanionLlmGatewayFactory() {
    }

    /** Creates the gateway for the configured provider, or fails with the dynamic supported-provider message. */
    public static LlmGateway create() {
        SystemSession session = SystemSession.getInstance();
        if (session.useLocalCommandLlm()) {
            LocalLlmProvider local = session.getLocalLlmProvider();
            return build(LOCAL_GATEWAYS.get(local), local, session);
        }
        ProviderEnum provider = LlmProviderResolver.detectCloudProvider();
        return build(CLOUD_GATEWAYS.get(provider), provider, session);
    }

    /** Runs the wired builder, or fails fast with the supported-provider message naming what was configured. */
    private static LlmGateway build(WiredProvider wired, Enum<?> configured, SystemSession session) {
        if (wired == null) {
            throw new UnsupportedOperationException(unsupportedMessage(String.valueOf(configured)));
        }
        return wired.builder().apply(session);
    }

    /**
     * The user-facing message naming the configured (unsupported) provider and the providers companion mode
     * supports right now. The supported labels are derived from the wired-adapter maps, so they stay correct
     * as providers are added.
     */
    static String unsupportedMessage(String configured) {
        return "Companion mode does not support the " + configured + " provider yet. Supported now - cloud: "
                + labels(CLOUD_GATEWAYS.values()) + "; local: " + labels(LOCAL_GATEWAYS.values())
                + ". Configure a supported provider to start companion mode.";
    }

    /** Comma-joined, alphabetically-ordered provider labels for a stable, readable supported list. */
    private static String labels(Collection<WiredProvider> providers) {
        return providers.stream().map(WiredProvider::label).sorted().collect(Collectors.joining(", "));
    }
}
