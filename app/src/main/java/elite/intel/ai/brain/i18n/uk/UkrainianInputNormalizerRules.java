package elite.intel.ai.brain.i18n.uk;

import elite.intel.ai.brain.i18n.InputNormalizerProvider;
import elite.intel.ai.brain.i18n.ru.RussianInputNormalizerRules;

import java.util.LinkedHashMap;

/**
 * Ukrainian synonym substitution rules for the InputNormalizer.
 * <p>
 * See {@link RussianInputNormalizerRules} for the morphology warning that applies
 * equally to Ukrainian. Add only complete, standalone phrases.
 * <p>
 * When in doubt, add the synonym to {@link UkrainianAiActionAliases} instead.
 */
public class UkrainianInputNormalizerRules implements InputNormalizerProvider {

    @Override
    public LinkedHashMap<String, String> buildSynonymMap() {
        LinkedHashMap<String, String> m = new LinkedHashMap<>();
        // Add Ukrainian synonym rules here as they are identified during testing.
        return m;
    }
}
