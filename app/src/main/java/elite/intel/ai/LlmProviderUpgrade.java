package elite.intel.ai;

import elite.intel.session.SystemSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

/**
 * Carries an install from before the provider was a setting across to it, silently: a commander who has an
 * API key and no provider gets the provider their key's format names, so the update does not cost them a
 * working setup. It is a Java step rather than a migration because the key is encrypted and only the retired
 * patterns in {@link KeyDetector} can read a provider from it.
 * <p>
 * Runs on every start, and is a no-op once a provider is stored or when there is no key. It fills the provider
 * in whether the commander currently uses the local model or the cloud one, so a commander who flips to the
 * cloud later still finds their provider. A key no pattern recognises is left alone, and
 * {@link elite.intel.setup.SetupCheck} asks the commander to pick a provider.
 */
public final class LlmProviderUpgrade {

    private static final Logger log = LogManager.getLogger(LlmProviderUpgrade.class);

    private LlmProviderUpgrade() {
    }

    public static void run() {
        SystemSession session = SystemSession.getInstance();
        adopt(session.getLlmProvider(), session.getAiApiKey()).ifPresent(provider -> {
            session.setLlmProvider(provider);
            log.warn("Cloud LLM provider set to {} from the format of the stored API key", provider);
        });
    }

    /**
     * The provider to store, or empty when there is nothing to write: a provider is already selected, there is
     * no key, or the key's format names no single provider.
     */
    static Optional<ProviderEnum> adopt(Optional<ProviderEnum> stored, String apiKey) {
        if (stored.isPresent() || apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }
        return KeyDetector.detectLegacyProvider(apiKey);
    }
}
