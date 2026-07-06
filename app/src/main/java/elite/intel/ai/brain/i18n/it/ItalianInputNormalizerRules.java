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
        m.put("carrier di squadriglia", "squadron carrier");
        m.put("carrier della squadriglia", "squadron carrier");
        m.put("portanavi di squadriglia", "squadron carrier");
        m.put("portanavi della squadriglia", "squadron carrier");
        m.put("carrier di squadrone", "squadron carrier");
        m.put("carrier dello squadrone", "squadron carrier");
        m.put("portanavi di squadrone", "squadron carrier");
        m.put("portanavi dello squadrone", "squadron carrier");
        m.put("portanavi", "fleet carrier");
        m.put("carrier", "fleet carrier");
        m.put("mappa galattica", "mappa della galassia");
        m.put("mappa stellare", "mappa del sistema");

        return m;
    }
}
