package elite.intel.ai.brain.i18n;

import elite.intel.ai.brain.i18n.en.EnglishInputNormalizerRules;

import java.util.LinkedHashMap;

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
}
