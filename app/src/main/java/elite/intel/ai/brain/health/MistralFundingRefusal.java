package elite.intel.ai.brain.health;

import elite.intel.ai.LlmProviderResolver;
import elite.intel.ai.ProviderEnum;
import elite.intel.session.SystemSession;
import elite.intel.util.StringUtls;

import java.util.Optional;

/**
 * Mistral refusing an account that has no money on it - the one refusal whose cause the app names outright.
 * <p>
 * Mistral ended its free access in November 2026: an unfunded account is now answered {@code 402
 * billing_no_api_budget_configured} (and {@code 401} on some accounts) on every request. Commanders who
 * set it up while it was free took the generic "refused, try again" line for a bug in the app. For any other
 * provider a 401 or 402 does not reliably mean money, so this rule stays Mistral's alone
 * ({@code project_cloud_error_codes_lie}).
 * <p>
 * The phrase also points to Claude: the model the app uses on Mistral is a small one, and a commander who
 * is going to pay gets a far better VEGA from Anthropic.
 */
public final class MistralFundingRefusal {

    private static final String PHRASE_KEY = "handler.common.mistralNeedsFunding";

    private MistralFundingRefusal() {
    }

    /**
     * True when a refusal with {@code statusCode} came from Mistral asking for a funded account.
     *
     * @param provider   the cloud provider the request went to, empty when none is chosen
     * @param local      the request went to LM Studio rather than the cloud
     * @param statusCode the refusal's HTTP status, or null when there was none
     */
    public static boolean matches(Optional<ProviderEnum> provider, boolean local, Integer statusCode) {
        return !local
                && provider.filter(ProviderEnum.MISTRAL::equals).isPresent()
                && statusCode != null && (statusCode == 401 || statusCode == 402);
    }

    /**
     * True when the last exchange the wire recorded for the command model was Mistral asking for a funded
     * account. Read right after a turn ended without an answer, to choose what VEGA says about it.
     */
    public static boolean lastRefusal() {
        return matches(LlmProviderResolver.cloudProvider(), SystemSession.getInstance().useLocalCommandLlm(),
                AiServiceHealth.getInstance().snapshot().lastRefusalCode());
    }

    /**
     * What the commander hears, in their language.
     */
    public static String phrase() {
        return StringUtls.localizedResponse(PHRASE_KEY);
    }
}
