package elite.intel.ai;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Upgrade-only: names the cloud provider behind an API key stored before the provider became a setting of its
 * own. {@link LlmProviderUpgrade} asks it once, for an install that has a key and no provider; nothing else
 * may. The commander now picks the provider from a list, so a key format a vendor introduces from here on
 * belongs to a commander who picked their provider by hand and never reaches this class.
 * <p>
 * FROZEN. Do not add or widen a pattern for a new key format - that is the maintenance treadmill this class
 * was retired from. It goes away entirely once the upgraded installs have all passed through it.
 */
public final class KeyDetector {

    private static final Map<ProviderEnum, Pattern> PATTERNS = Map.of(
            ProviderEnum.GROK, Pattern.compile("^xai-[a-zA-Z0-9_-]{40,100}$"),
            ProviderEnum.DEEPSEEK, Pattern.compile("^sk-[a-f0-9]{32}$"),
            ProviderEnum.MISTRAL, Pattern.compile("^(?:[a-zA-Z0-9]{32}|mstrl_[a-zA-Z0-9_-]{20,100})$"),
            ProviderEnum.OPENAI,
            Pattern.compile("^sk-(?:(?:proj|svcacct|admin)-[a-zA-Z0-9_-]{20,}|[a-zA-Z0-9]{48})$"),
            ProviderEnum.GEMINI, Pattern.compile("^(AIzaSy[a-zA-Z0-9_-]{33}|AQ\\.[a-zA-Z0-9_.-]{30,150})$"),
            ProviderEnum.ANTHROPIC, Pattern.compile("^sk-ant-api\\d{2}-[a-zA-Z0-9_-]{93,97}$")
    );

    private KeyDetector() {
    }

    /**
     * The one provider whose historical key format matches, or empty when none does - or when more than one
     * does, since a guess between two would send the key to a vendor it does not belong to. Silent either way:
     * what the commander is told about an empty result is {@link elite.intel.setup.SetupCheck}'s to say.
     */
    public static Optional<ProviderEnum> detectLegacyProvider(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        String trimmed = key.trim();
        List<ProviderEnum> matches = PATTERNS.entrySet().stream()
                .filter(entry -> entry.getValue().matcher(trimmed).matches())
                .map(Map.Entry::getKey)
                .toList();
        return matches.size() == 1 ? Optional.of(matches.getFirst()) : Optional.empty();
    }
}
