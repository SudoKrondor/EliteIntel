package elite.intel.ai.brain.i18n.es;

import elite.intel.ai.brain.i18n.InputNormalizerProvider;

import java.util.LinkedHashMap;

/**
 * Spanish synonym substitution rules for the InputNormalizer.
 * <p>
 * Prefer adding variants to {@link SpanishAiActionAliases}  Spanish already has
 * rich alias coverage. Add entries here only for ordering-sensitive cases or
 * phonetic corrections specific to the Spanish STT engine.
 */
public class SpanishInputNormalizerRules implements InputNormalizerProvider {

    @Override
    public java.util.Set<String> stopWords() {
        return java.util.Set.of(
                "el", "la", "los", "las", "un", "una", "unos", "unas", "de", "del", "al", "en", "con",
                "por", "para", "sin", "sobre", "bajo", "entre", "pero", "que",
                "mi", "mis", "nuestro", "nuestra", "este", "esta", "estos", "estas",
                "son", "yo", "él", "ella", "nosotros");
    }

    @Override
    public LinkedHashMap<String, String> buildSynonymMap() {
        LinkedHashMap<String, String> m = new LinkedHashMap<>();
        // Add Spanish synonym rules here as they are identified during testing.
        return m;
    }
}
