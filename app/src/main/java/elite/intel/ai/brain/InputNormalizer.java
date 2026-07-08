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
            // Idempotency guard: skip when the canonical form is already present. Many rules expand a synonym
            // into a canonical that CONTAINS that synonym (e.g. "режим анализа" -> "переключись в режим анализа",
            // "боевой" -> "боевой режим"). Without this guard a blind substring replace duplicates text the input
            // already carries ("переключись в переключись в режим анализа", "боевой режим режим"), which corrupts
            // the match text fed to the semantic reducer and the LLM and defeats the exact-alias match. If the
            // canonical is already there, the concept is normalized - do nothing. Substring (not word-boundary)
            // matching is deliberate: it mirrors the substring replacement just below, so the guard covers
            // exactly the cases that replacement would otherwise double.
            if (canonical != null && lower.contains(canonical.toLowerCase())) {
                continue;
            }
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
