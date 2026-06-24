package elite.intel.ai.brain.i18n;

import elite.intel.ai.brain.i18n.en.EnglishInputNormalizerRules;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * Per-language synonym substitution rules for {@link elite.intel.ai.brain.InputNormalizer}.
 * <p>
 * One implementation per language lives in this package. A localizer only needs to
 * edit their own file  no shared state, no merge conflicts.
 * <p>
 * <strong>Ordering contract:</strong> entries in the returned map are applied in
 * insertion order. More-specific (longer) phrases MUST be added before any shorter
 * phrase that is a substring of them. See {@link EnglishInputNormalizerRules} for
 * documented ordering constraints.
 * <p>
 * <strong>Morphological languages (RU, UK, DE, ...):</strong> simple substring
 * replacement does not respect word boundaries or inflectional endings. Add only
 * complete, standalone phrases where you are certain the substitution is safe.
 * When in doubt, add the synonym to the alias file instead.
 */
public interface InputNormalizerProvider {

    /**
     * Builds the ordered synonym map. Called once per language; the result is cached.
     *
     * @return a {@link LinkedHashMap} mapping synonym phrases to their canonical forms.
     * Return an empty map if no normalization is needed for this language.
     */
    LinkedHashMap<String, String> buildSynonymMap();

    /**
     * Optional regex pattern for noise words to strip from the normalized input
     * after synonym substitution. Return {@code null} to skip stripping.
     * <p>
     * Use Unicode-aware boundary assertions ({@code (?<![\\p{L}])} / {@code (?![\\p{L}])})
     * for non-Latin scripts  Java's {@code \b} is ASCII-only.
     */
    default String noiseWordPattern() {
        return null;
    }

    /**
     * Short noise/filler phrases that the STT engine produces as standalone utterances.
     * Any transcript whose tokens consist entirely of these phrases is discarded before
     * it reaches the AI pipeline. Matching is case-insensitive and punctuation-tolerant.
     * <p>
     * Return an empty list if no filtering is needed for this language.
     */
    default List<String> trashPhrases() {
        return List.of();
    }

    /**
     * Function words that carry no action-intent signal and should be excluded from
     * the word-overlap scoring in the Reducer. Matching is case-sensitive after
     * lower-casing the input token.
     * <p>
     * Return an empty set if no stop-word filtering is needed for this language.
     */
    default Set<String> stopWords() {
        return Set.of();
    }
}
