package elite.intel.ui.support;

import elite.intel.ai.LlmProviderResolver;
import elite.intel.ai.ProviderEnum;
import elite.intel.ai.brain.vega.llm.VegaLlmGatewayFactory;
import elite.intel.ai.mouth.TtsProvider;
import elite.intel.session.SystemSession;

import javax.annotation.Nullable;

/**
 * The AI half of the support bundle's manifest: which language model VEGA talks to, and which engine voices it.
 * <p>
 * WHY it is stated: the model used to reach a bundle only by accident, in {@code session.log}'s line for each
 * LLM call - so a session that made no call carried none, and no bundle said local or cloud at all. A weak 8B
 * model, the same model on a cloud, and a local setup with no model picked each fail in their own way, and the
 * reply to a report starts with which one it was.
 * <p>
 * Names only. API keys are never read here, nor whether one is set.
 */
public final class AiSetupReport {

    private AiSetupReport() {
    }

    /**
     * Reads the commander's stored settings. It touches the database, so the bundle worker calls it, not the EDT.
     */
    public static String render() {
        SystemSession session = SystemSession.getInstance();
        return describe(session.useLocalCommandLlm(), session.useLocalQueryLlm(),
                LlmProviderResolver.cloudProvider().orElse(null),
                session.getLmStudioAddress(), session.getLmStudioCommandModel(),
                session.getTtsProvider());
    }

    /**
     * @param localCommands   VEGA's turns go to LM Studio
     * @param localQueries    the analysis endpoint goes to LM Studio; reported only when it differs from
     *                        {@code localCommands}, since the settings screen sets both together
     * @param cloudProvider   the selected cloud provider, or null when none is
     * @param lmStudioAddress the LM Studio address; may be blank
     * @param lmStudioModel   the LM Studio command model; blank on a fresh install
     * @param tts             the engine voicing VEGA, as corrected for the session's languages
     */
    static String describe(boolean localCommands, boolean localQueries, @Nullable ProviderEnum cloudProvider,
                           @Nullable String lmStudioAddress, @Nullable String lmStudioModel, TtsProvider tts) {
        StringBuilder text = new StringBuilder("AI setup:\n")
                .append("  LLM: ").append(llm(localCommands, cloudProvider, lmStudioAddress, lmStudioModel)).append('\n');
        if (localQueries != localCommands) {
            text.append("  Queries LLM: ").append(llm(localQueries, cloudProvider, lmStudioAddress, lmStudioModel))
                    .append('\n');
        }
        return text.append("  Text-to-speech: ").append(tts.name())
                .append(tts.isLocal() ? " (local)" : " (cloud)").append('\n')
                .toString();
    }

    private static String llm(boolean local, @Nullable ProviderEnum cloudProvider,
                              @Nullable String lmStudioAddress, @Nullable String lmStudioModel) {
        if (local) {
            String address = isBlank(lmStudioAddress) ? "no address configured" : lmStudioAddress.trim();
            String model = isBlank(lmStudioModel) ? "no model configured" : "model " + lmStudioModel.trim();
            return "local - LM Studio at " + address + ", " + model;
        }
        if (cloudProvider == null) {
            return "cloud - no provider selected";
        }
        return "cloud - " + cloudProvider.name() + ", model " + VegaLlmGatewayFactory.cloudModel(cloudProvider);
    }

    private static boolean isBlank(@Nullable String value) {
        return value == null || value.isBlank();
    }
}
