package elite.intel.ai.brain.i18n;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Applies only per-language acoustic STT corrections to companion command-match text.
 * <p>
 * Replacements are whole Unicode words or phrases, so a correction such as {@code of -> off} cannot alter
 * a larger word such as {@code profile}. The caller keeps the original transcript for display and memory.
 */
public final class PhoneticInputNormalizer {

    private static final int MATCH_FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;

    private PhoneticInputNormalizer() {
    }

    /** Applies the current language's ordered acoustic corrections to a single command-match input. */
    public static String normalize(String input) {
        return normalize(input, InputNormalizerLocalizations.phoneticMap());
    }

    /**
     * Applies a supplied ordered correction map. Package-visible for focused regression tests without session state.
     */
    static String normalize(String input, Map<String, String> corrections) {
        if (input == null || input.isBlank() || corrections.isEmpty()) {
            return input;
        }
        String normalized = input;
        for (Map.Entry<String, String> correction : corrections.entrySet()) {
            String heard = correction.getKey();
            String intended = correction.getValue();
            if (heard == null || heard.isBlank() || intended == null || intended.isBlank()) {
                continue;
            }
            Pattern phrase = Pattern.compile(
                    "(?<![\\p{L}\\p{N}])" + Pattern.quote(heard.trim()) + "(?![\\p{L}\\p{N}])", MATCH_FLAGS);
            normalized = phrase.matcher(normalized).replaceAll(Matcher.quoteReplacement(intended.trim()));
        }
        return normalized;
    }
}
