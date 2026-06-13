package elite.intel.ai.brain;

import elite.intel.ai.brain.i18n.InputNormalizerLocalizations;

import java.util.Map;

/**
 * Normalizes raw STT user input by applying the synonym map for the current
 * session language, then stripping any language-specific noise words.
 * <p>
 * Language rules live in per-language files under
 * {@code elite.intel.ai.brain.i18n}  one file per language, no shared state.
 * Add or edit synonyms in the appropriate {@code *InputNormalizerRules} class.
 */
public class InputNormalizer {

    private static final InputNormalizer INSTANCE = new InputNormalizer();

    private InputNormalizer() {
    }

    public static InputNormalizer getInstance() {
        return INSTANCE;
    }

    /**
     * Returns a normalized version of the input with synonyms replaced by their
     * canonical forms for the current session language. The original input is
     * returned unchanged if no synonyms match. Matching is case-insensitive;
     * output case follows the canonical form for replaced segments.
     */
    public String normalize(String input) {
        if (input == null || input.isBlank()) return input;

        String lower = input.toLowerCase();
        for (Map.Entry<String, String> entry : InputNormalizerLocalizations.synonymMap().entrySet()) {
            String synonym = entry.getKey();
            String canonical = entry.getValue();
            int idx = lower.indexOf(synonym);
            if (idx >= 0) {
                input = input.substring(0, idx) + canonical + input.substring(idx + synonym.length());
                lower = input.toLowerCase();
            }
        }

        String noisePattern = InputNormalizerLocalizations.noiseWordPattern();
        if (noisePattern != null && !noisePattern.isBlank()) {
            input = input.replaceAll(noisePattern, "");
        }

        return input.replaceAll("\\s{2,}", " ").trim();
    }
}
