package elite.intel.ai.brain.i18n.de;

import elite.intel.ai.brain.i18n.InputNormalizerProvider;

import java.util.LinkedHashMap;

/**
 * German synonym substitution rules for the InputNormalizer.
 * <p>
 * German is a compounding language  a normalizer rule that matches a short word
 * can corrupt a longer compound word that contains it. Keep entries to complete,
 * unambiguous phrases. Prefer adding variants to {@link GermanAiActionAliases}.
 */
public class GermanInputNormalizerRules implements InputNormalizerProvider {

    @Override
    public LinkedHashMap<String, String> buildSynonymMap() {
        LinkedHashMap<String, String> m = new LinkedHashMap<>();
        // Add German synonym rules here as they are identified during testing.
        return m;
    }
}
