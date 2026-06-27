package elite.intel.ai.brain.i18n.it;

import elite.intel.ai.brain.i18n.InputNormalizerProvider;

import java.util.LinkedHashMap;

public class ItalianInputNormalizerRules implements InputNormalizerProvider {

    @Override
    public java.util.Set<String> stopWords() {
        return java.util.Set.of(
                "il", "lo", "la", "gli", "le", "un", "uno", "una", "di", "del", "della", "dei", "delle",
                "da", "in", "con", "per", "su", "tra", "fra", "che",
                "mio", "mia", "nostro", "nostra", "suo", "sua", "questo", "questa", "questi", "queste",
                "sono", "io", "lui", "lei", "noi");
    }

    @Override
    public LinkedHashMap<String, String> buildSynonymMap() {
        LinkedHashMap<String, String> m = new LinkedHashMap<>();
        // Add Italian synonym rules here as they are identified during testing.
        return m;
    }
}
