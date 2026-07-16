package elite.intel.ai.brain.i18n;

import elite.intel.ai.brain.actions.ActionParameterSpec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Matches localized aliases whose final placeholder is one required string parameter. */
public final class TrailingStringAliasMatcher {

    private static final Pattern TRAILING_PARAMETER_BLOCK = Pattern.compile("^(.*?)\\{([^{}]+)}\\s*$");
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}_]+");
    private static final Pattern VALUE_SEPARATOR = Pattern.compile("^[\\s,:;\\-\\u2013\\u2014]+");

    private TrailingStringAliasMatcher() {
    }

    /**
     * Finds the matching alias with the longest literal prefix. The returned value preserves the input suffix
     * except for whitespace and separator punctuation at the trigger boundary.
     */
    public static Optional<Match> findBestMatch(
            String localizedAliasGroup,
            List<ActionParameterSpec> parameters,
            String input
    ) {
        if (localizedAliasGroup == null || localizedAliasGroup.isBlank()
                || input == null || input.isBlank()) {
            return Optional.empty();
        }

        Map<String, String> requiredStrings = requiredStringParameters(parameters);
        if (requiredStrings.isEmpty()) {
            return Optional.empty();
        }

        List<InputWord> inputWords = wordsWithOffsets(input);
        Match best = null;
        for (String alias : AiActionLocalizations.splitPhraseGroup(localizedAliasGroup)) {
            Matcher trailingBlock = TRAILING_PARAMETER_BLOCK.matcher(alias);
            if (!trailingBlock.matches() || trailingBlock.group(1).contains("{")) {
                continue;
            }
            String parameterName = singleRequiredString(trailingBlock.group(2), requiredStrings);
            if (parameterName == null) {
                continue;
            }

            List<String> prefixWords = normalizedWords(trailingBlock.group(1));
            if (prefixWords.isEmpty() || !startsWith(inputWords, prefixWords)) {
                continue;
            }
            int valueStart = inputWords.get(prefixWords.size() - 1).end();
            String suffix = VALUE_SEPARATOR.matcher(input.substring(valueStart)).replaceFirst("").stripTrailing();
            Match candidate = new Match(parameterName, suffix, prefixWords.size());
            if (best == null || candidate.prefixWordCount() > best.prefixWordCount()) {
                best = candidate;
            }
        }
        return Optional.ofNullable(best);
    }

    /** One deterministic parameter extraction from a localized alias. */
    public record Match(String parameterName, String value, int prefixWordCount) {
    }

    private static Map<String, String> requiredStringParameters(List<ActionParameterSpec> parameters) {
        Map<String, String> result = new HashMap<>();
        if (parameters == null) {
            return result;
        }
        for (ActionParameterSpec parameter : parameters) {
            if (parameter.isRequired() && "string".equals(parameter.getType()) && parameter.getName() != null) {
                result.put(parameter.getName().toLowerCase(Locale.ROOT), parameter.getName());
            }
        }
        return result;
    }

    private static String singleRequiredString(String parameterBlock, Map<String, String> requiredStrings) {
        String match = null;
        for (String token : parameterBlock.split(",")) {
            String name = token.split(":", 2)[0].trim().toLowerCase(Locale.ROOT);
            String canonicalName = requiredStrings.get(name);
            if (canonicalName == null) {
                continue;
            }
            if (match != null) {
                return null;
            }
            match = canonicalName;
        }
        return match;
    }

    private static boolean startsWith(List<InputWord> inputWords, List<String> prefixWords) {
        if (inputWords.size() < prefixWords.size()) {
            return false;
        }
        for (int i = 0; i < prefixWords.size(); i++) {
            if (!inputWords.get(i).normalized().equals(prefixWords.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static List<String> normalizedWords(String text) {
        return wordsWithOffsets(text).stream().map(InputWord::normalized).toList();
    }

    private static List<InputWord> wordsWithOffsets(String text) {
        List<InputWord> words = new ArrayList<>();
        Matcher matcher = WORD.matcher(text);
        while (matcher.find()) {
            words.add(new InputWord(matcher.group().toLowerCase(Locale.ROOT), matcher.end()));
        }
        return words;
    }

    private record InputWord(String normalized, int end) {
    }
}
