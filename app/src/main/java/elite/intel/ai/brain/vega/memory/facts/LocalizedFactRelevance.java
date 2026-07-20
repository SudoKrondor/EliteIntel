package elite.intel.ai.brain.vega.memory.facts;

import elite.intel.ai.brain.i18n.AiActionAliasTextProvider;
import elite.intel.ai.brain.i18n.InputNormalizerLocalizations;
import elite.intel.ai.brain.vega.prompt.AliasEmbeddingText;
import elite.intel.ai.brain.vega.prompt.CompanionWordMatch;
import elite.intel.session.SystemSession;

import java.util.*;

/**
 * Shared localized phrase-matching primitive for {@link MemoryFactSource#isRelevant}. It contains no source catalog
 * or source-to-subject mapping: each source chooses the existing alias groups it understands and calls this helper
 * itself. A complete alias phrase matches directly (including stop words such as "where are we"); otherwise a
 * source-selected minimum of meaningful inflection-tolerant word matches is required.
 */
public final class LocalizedFactRelevance {

    private static final int MIN_WORD_LEN = 3;

    private LocalizedFactRelevance() {
    }

    /** Whether the current query matches at least one phrase from the source-selected localized alias groups. */
    public static boolean matches(MemoryFactContext context, int minimumMeaningfulMatches, List<String> aliasKeys) {
        if (context == null || context.query() == null || context.query().isBlank()
                || aliasKeys == null || aliasKeys.isEmpty()) {
            return false;
        }
        List<String> inputTokens = tokens(context.query());
        Set<String> inputWords = significantWords(inputTokens);
        int minimum = Math.max(1, minimumMeaningfulMatches);
        for (String aliasKey : aliasKeys) {
            String localized = AiActionAliasTextProvider.getText(
                    SystemSession.getInstance().getLanguage(), aliasKey);
            for (String phrase : AliasEmbeddingText.phrases(localized, List.of())) {
                List<String> phraseTokens = tokens(phrase);
                if (containsPhrase(inputTokens, phraseTokens)
                        || matchedWords(inputWords, significantWords(phraseTokens)) >= minimum) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Contiguous full-phrase match, tolerant of inflected word endings. */
    private static boolean containsPhrase(List<String> input, List<String> phrase) {
        if (phrase.isEmpty() || phrase.size() > input.size()) {
            return false;
        }
        for (int start = 0; start <= input.size() - phrase.size(); start++) {
            boolean matches = true;
            for (int offset = 0; offset < phrase.size(); offset++) {
                if (!CompanionWordMatch.similar(input.get(start + offset), phrase.get(offset))) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return true;
            }
        }
        return false;
    }

    private static int matchedWords(Set<String> inputWords, Set<String> phraseWords) {
        int matched = 0;
        for (String phraseWord : phraseWords) {
            if (inputWords.stream().anyMatch(input -> CompanionWordMatch.similar(input, phraseWord))) {
                matched++;
            }
        }
        return matched;
    }

    private static List<String> tokens(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}_]+"))
                .filter(word -> !word.isBlank())
                .toList();
    }

    private static Set<String> significantWords(List<String> tokens) {
        Set<String> stopWords = InputNormalizerLocalizations.stopWords();
        Set<String> words = new LinkedHashSet<>();
        tokens.stream()
                .filter(word -> word.length() >= MIN_WORD_LEN)
                .filter(word -> !stopWords.contains(word))
                .forEach(words::add);
        return words;
    }
}
