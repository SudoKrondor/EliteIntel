package elite.intel.ai.brain.i18n;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * Per-language input filters and acoustic STT corrections.
 * <p>
 * One implementation per language lives in this package, so language-specific STT
 * behaviour remains isolated from companion routing.
 */
public interface InputNormalizerProvider {

    /**
     * Builds ordered acoustic STT corrections only. These corrections are safe to apply to command-match text,
     * while broader synonym substitutions remain unavailable to the companion routing pipeline.
     *
     * @return a {@link LinkedHashMap} mapping recognized acoustic confusions to the intended words or phrases.
     */
    default LinkedHashMap<String, String> buildPhoneticMap() {
        return new LinkedHashMap<>();
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
