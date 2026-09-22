package elite.intel.ai.brain.i18n;

import elite.intel.i18n.Language;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Reads the commander's answer to "shall I proceed?" - yes, no, or something else entirely.
 *
 * <p>Deterministic on purpose: the LLM never decides whether a destructive action runs. The answer must OPEN with
 * one of the language's authored yes or no phrases ({@code ai_confirmation_replies*.properties}), and stay short -
 * "yes please", "no, belay that" - because a long sentence that merely starts with "yes" is more likely a new
 * request than an answer. A no wins over a yes, so a hesitant "no... yes" never fires the action. English phrases
 * are understood in every language, since commanders mix English into their own.
 */
public final class ConfirmationReplies {

    /**
     * How the commander answered.
     */
    public enum Reply {AFFIRMATIVE, NEGATIVE, OTHER}

    private static final String BUNDLE_NAME = "i18n.ai_confirmation_replies";
    private static final String AFFIRMATIVE_KEY = "affirmative";
    private static final String NEGATIVE_KEY = "negative";
    /**
     * Longest utterance still read as an answer rather than a new request.
     */
    private static final int MAX_REPLY_WORDS = 6;
    private static final ResourceBundle.Control NO_FALLBACK_CONTROL =
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT);
    private static final Pattern NON_LETTER = Pattern.compile("[^\\p{L}\\p{M}]+");

    private ConfirmationReplies() {
    }

    /**
     * Classifies one utterance as the commander's answer to a pending confirmation.
     */
    public static Reply classify(Language language, String input) {
        List<String> words = words(input);
        if (words.isEmpty() || words.size() > MAX_REPLY_WORDS) {
            return Reply.OTHER;
        }
        if (opensWithAny(words, phrases(language, NEGATIVE_KEY))) {
            return Reply.NEGATIVE;
        }
        if (opensWithAny(words, phrases(language, AFFIRMATIVE_KEY))) {
            return Reply.AFFIRMATIVE;
        }
        return Reply.OTHER;
    }

    private static boolean opensWithAny(List<String> words, List<List<String>> phrases) {
        for (List<String> phrase : phrases) {
            if (!phrase.isEmpty() && words.size() >= phrase.size() && words.subList(0, phrase.size()).equals(phrase)) {
                return true;
            }
        }
        return false;
    }

    private static List<List<String>> phrases(Language language, String key) {
        Set<List<String>> phrases = new LinkedHashSet<>(read(Locale.ROOT, key));
        phrases.addAll(read(AiActionAliasTextProvider.locale(language), key));
        return List.copyOf(phrases);
    }

    private static List<List<String>> read(Locale locale, String key) {
        ResourceBundle bundle;
        try {
            bundle = ResourceBundle.getBundle(BUNDLE_NAME, locale, NO_FALLBACK_CONTROL);
        } catch (MissingResourceException e) {
            return List.of();
        }
        if (!bundle.containsKey(key)) {
            return List.of();
        }
        List<List<String>> phrases = new ArrayList<>();
        for (String phrase : bundle.getString(key).split(",")) {
            List<String> words = words(phrase);
            if (!words.isEmpty()) {
                phrases.add(words);
            }
        }
        return phrases;
    }

    /**
     * Lower-cased words with punctuation dropped, so "Yes." and "d'accord" compare as authored.
     */
    private static List<String> words(String text) {
        if (text == null) {
            return List.of();
        }
        return Arrays.stream(NON_LETTER.split(text.toLowerCase(Locale.ROOT)))
                .filter(word -> !word.isEmpty())
                .toList();
    }
}
