package elite.intel.ai;

import elite.intel.ai.mouth.subscribers.events.AiVoxResponseEvent;
import elite.intel.eventbus.GameEventBus;
import elite.intel.eventbus.UiBus;
import elite.intel.ui.event.AppLogEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class KeyDetector {
    /**
     * A mapping of ProviderEnum constants to their corresponding regular expression patterns used
     * for validating API key formats for specific providers. Each entry maps a provider to its key
     * format validation pattern.
     * <p>
     * The patterns are designed to match the unique structure of API keys associated with various
     * providers and their respective functionalities, such as LLM (Large Language Model), STT
     * (Speech-to-Text), or TTS (Text-to-Speech).
     * <p>
     * Example provider mappings and their corresponding patterns:
     * - GROK: Matches keys consisting of 40 to 100 alphanumeric characters or hyphens.
     * - GOOGLE_STT and GOOGLE_TTS: Matches keys starting with "AIzaSy" followed by 33 alphanumeric
     * characters, underscores, or hyphens.
     * - ANTHROPIC: Matches keys starting with "sk-ant-" followed by 80 to 90 alphanumeric characters
     * or hyphens.
     * - AWS_LLM, AWS_STT, AWS_TTS: Matches keys starting with "AKIA" or "ASIA," followed by 16
     * alphanumeric characters.
     * - AZURE_LLM, AZURE_STT, AZURE_TTS: Matches keys consisting of exactly 32 hexadecimal characters.
     * - ELEVEN_LABS: Matches keys consisting of exactly 32 characters in hexadecimal or mixed case.
     * - DEEP_GRAM: Matches keys in a UUID format (e.g., 8-4-4-4-12 hexadecimal segments).
     * - IBM_WATSON_STT and IBM_WATSON_TTS: Matches keys containing 40 to 50 characters, which may
     * include alphanumeric characters, plus signs (+), forward slashes (/), or equals signs (=).
     * <p>
     * This map is primarily used to validate and detect the provider of a given API key based on
     * its format.
     */
    private static final Map<ProviderEnum, Pattern> PATTERNS = Map.ofEntries(
            Map.entry(ProviderEnum.GROK, Pattern.compile("^xai-[a-zA-Z0-9_-]{40,100}$")),
            Map.entry(ProviderEnum.DEEPSEEK, Pattern.compile("^sk-[a-f0-9]{32}$")),
            Map.entry(ProviderEnum.MISTRAL, Pattern.compile("^[a-zA-Z0-9]{32}$")),
            Map.entry(ProviderEnum.OPENAI, Pattern.compile("^sk-[a-zA-Z0-9_-]{161}$")),
            Map.entry(ProviderEnum.GOOGLE_STT, Pattern.compile("^AIzaSy[a-zA-Z0-9_-]{33}$")),
            Map.entry(ProviderEnum.GOOGLE_TTS, Pattern.compile("^AIzaSy[a-zA-Z0-9_-]{33}$")),
            Map.entry(ProviderEnum.GEMINI, Pattern.compile("^(AIzaSy[a-zA-Z0-9_-]{33}|AQ\\.[a-zA-Z0-9_.-]{30,150})$")),
            Map.entry(ProviderEnum.ANTHROPIC, Pattern.compile("^sk-ant-api\\d{2}-[a-zA-Z0-9_-]{93,97}$")),
            Map.entry(ProviderEnum.AWS_LLM, Pattern.compile("^(AKIA|ASIA)[A-Z0-9]{16}$")),
            Map.entry(ProviderEnum.AWS_STT, Pattern.compile("^(AKIA|ASIA)[A-Z0-9]{16}$")),
            Map.entry(ProviderEnum.AWS_TTS, Pattern.compile("^(AKIA|ASIA)[A-Z0-9]{16}$")),
            Map.entry(ProviderEnum.AZURE_LLM, Pattern.compile("^[0-9a-f]{32}$")),
            Map.entry(ProviderEnum.AZURE_STT, Pattern.compile("^[0-9a-f]{32}$")),
            Map.entry(ProviderEnum.AZURE_TTS, Pattern.compile("^[0-9a-f]{32}$")),
            Map.entry(ProviderEnum.ELEVEN_LABS, Pattern.compile("^[0-9a-fA-F]{32}$")),
            Map.entry(ProviderEnum.DEEP_GRAM, Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")),
            Map.entry(ProviderEnum.IBM_WATSON_STT, Pattern.compile("^[a-zA-Z0-9+/=]{40,50}$")),
            Map.entry(ProviderEnum.IBM_WATSON_TTS, Pattern.compile("^[a-zA-Z0-9+/=]{40,50}$"))
    );

    /**
     * Detects the provider based on the API key format and category.
     *
     * @param key      The API key to analyze.
     * @param category The expected category ("LLM", "STT", "TTS").
     * @return The matched ProviderEnum or UNKNOWN if no match.
     */
    public static ProviderEnum detectProvider(String key, String category) {
        if (key == null || key.isBlank()) {
            return ProviderEnum.UNKNOWN;
        }
        List<ProviderEnum> matches = new ArrayList<>();
        for (Map.Entry<ProviderEnum, Pattern> entry : PATTERNS.entrySet()) {
            if (entry.getValue().matcher(key).matches() && entry.getKey().supportsCategory(category)) {
                matches.add(entry.getKey());
            }
        }
        if (matches.size() == 1) return matches.get(0);
        if (matches.size() > 1) {
            UiBus.publish(new AppLogEvent("Ambiguous key matches: " + matches));
            GameEventBus.publish(new AiVoxResponseEvent("Multiple providers detected for category " + category));
        }
        return ProviderEnum.UNKNOWN;
    }
}
