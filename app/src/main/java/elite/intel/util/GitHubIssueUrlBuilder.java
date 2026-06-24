package elite.intel.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Builds prefilled GitHub issue URLs for flows that intentionally delegate final submission to the browser.
 */
public final class GitHubIssueUrlBuilder {

    private static final String GITHUB_NEW_ISSUE_URL = "https://github.com/stone-alex/EliteIntel/issues/new";

    private GitHubIssueUrlBuilder() {
    }

    /**
     * Builds a GitHub issue URL for command phrase correction suggestions.
     */
    public static String buildPhraseCorrectionIssueUrl(
            String commandId,
            String commandName,
            String language,
            String currentPhrases,
            String suggestedPhrases,
            String userComment
    ) {
        String normalizedCommandId = valueOrNone(commandId);
        String normalizedLanguage = valueOrNone(language);
        String title = "Phrase correction: %s (%s)".formatted(normalizedCommandId, normalizedLanguage);
        String body = """
                Command id: %s
                Command name: %s
                Language: %s
                
                Current phrases:
                ```text
                %s
                ```
                
                Suggested phrases:
                ```text
                %s
                ```
                
                User comment:
                ```text
                %s
                ```
                
                This issue was generated from the EliteIntel suggestion flow.
                """.formatted(
                normalizedCommandId,
                valueOrNone(commandName),
                normalizedLanguage,
                blockValue(currentPhrases),
                blockValue(suggestedPhrases),
                blockValue(userComment)
        );
        return GITHUB_NEW_ISSUE_URL
                + "?title=" + encodeUrlComponent(title)
                + "&body=" + encodeUrlComponent(body);
    }

    private static String encodeUrlComponent(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String valueOrNone(String value) {
        String normalizedValue = trimmed(value);
        return normalizedValue.isBlank() ? "(none)" : normalizedValue;
    }

    private static String blockValue(String value) {
        String normalizedValue = trimmed(value);
        return normalizedValue.isBlank() ? "(none)" : normalizedValue;
    }

    private static String trimmed(String value) {
        return value == null ? "" : value.trim();
    }
}
