package elite.intel.ai.brain.i18n;

import elite.intel.i18n.Language;
import elite.intel.session.SystemSession;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Every word the authored action aliases are built from, per language - the command vocabulary of the
 * language the commander is speaking.
 * <p>
 * Its purpose is to say whether a heard word is <em>already a real command word</em>. That is the guard that
 * makes fuzzy matching safe here: the English alias vocabulary alone holds 94 pairs of words one edit apart
 * that belong to <em>different</em> actions (dump/jump, cold/hold, head/heat, full/hull, dock/lock,
 * half/halt), so "correcting" a word toward its nearest neighbour would sometimes swap the command outright -
 * "dump heat" becoming a hyperspace jump. A word that is in here was authored by us and is taken as heard;
 * only a word that is in no alias at all (like the misheard "blanding") may be treated as damaged.
 * <p>
 * Built from the alias bundles rather than the registries, so it covers every action whether or not it is
 * currently visible: visibility decides what may fire, never what counts as a real word. Cached per language.
 *
 * @see elite.intel.ai.brain.vega.prompt.FuzzyAliasMatch
 */
public final class AliasVocabulary {

    /**
     * Words, in any script: the alias bundles hold Latin and Cyrillic, and French elides with apostrophes.
     */
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}]+");

    private static final Map<Language, Set<String>> CACHE = new ConcurrentHashMap<>();

    private AliasVocabulary() {
    }

    /**
     * The command vocabulary of the session language.
     */
    public static Set<String> forCurrentLanguage() {
        return forLanguage(SystemSession.getInstance().getLanguage());
    }

    /**
     * The command vocabulary of one language, built once and cached.
     */
    public static Set<String> forLanguage(Language language) {
        return CACHE.computeIfAbsent(language, AliasVocabulary::build);
    }

    /**
     * Lower-cased words of a phrase, punctuation dropped. Shared by the vocabulary and by anything matching
     * against it, so both sides tokenize a phrase exactly the same way.
     */
    public static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return tokens;
        }
        Matcher matcher = WORD.matcher(text.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        return tokens;
    }

    private static Set<String> build(Language language) {
        Set<String> words = new HashSet<>();
        for (String key : AiActionAliasTextProvider.keys(language)) {
            String group = AiActionAliasTextProvider.getText(language, key);
            for (String phrase : AiActionLocalizations.splitPhraseGroup(group)) {
                words.addAll(tokenize(AliasPhrase.parse(phrase).spokenText()));
            }
        }
        return Set.copyOf(words);
    }
}
