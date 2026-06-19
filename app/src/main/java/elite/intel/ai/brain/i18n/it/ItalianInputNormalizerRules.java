package elite.intel.ai.brain.i18n.it;

import elite.intel.ai.brain.i18n.InputNormalizerProvider;

import java.util.LinkedHashMap;

public class ItalianInputNormalizerRules implements InputNormalizerProvider {

    @Override
    public LinkedHashMap<String, String> buildSynonymMap() {
        LinkedHashMap<String, String> m = new LinkedHashMap<>();
        // Add Italian synonym rules here as they are identified during testing.
        return m;
    }
}
