package elite.intel.ai.brain.i18n.it;

import elite.intel.ai.brain.i18n.InputNormalizerProvider;

import java.util.LinkedHashMap;

public class ItalianInputNormalizerRules implements InputNormalizerProvider {

    @Override
    public java.util.Set<String> stopWords() {
        return java.util.Set.of(
                "il", "lo", "la", "gli", "le", "un", "uno", "una", "di", "del", "della", "dei", "delle",
                "dello", "degli", "da", "in", "con", "per", "su", "tra", "fra", "che",
                "mio", "mia", "nostro", "nostra", "suo", "sua", "questo", "questa", "questi", "queste",
                "sono", "io", "lui", "lei", "noi");
    }

    @Override
    public LinkedHashMap<String, String> buildSynonymMap() {
        LinkedHashMap<String, String> m = new LinkedHashMap<>();
        m.put("squadron carrier", "squadron");
        m.put("squadron fleet carrier", "squadron");
        m.put("fleet carrier", "carrier");
        m.put("carrier di squadriglia", "squadron");
        m.put("carrier della squadriglia", "squadron");
        m.put("portanavi di squadriglia", "squadron");
        m.put("portanavi della squadriglia", "squadron");
        m.put("carrier di squadrone", "squadron");
        m.put("carrier dello squadrone", "squadron");
        m.put("portanavi di squadrone", "squadron");
        m.put("portanavi dello squadrone", "squadron");
        m.put("portanavi", "carrier");
        m.put("carrier", "fleet carrier");
        m.put("squadron", "squadron carrier");

        m.put("mappa galattica", "mappa della galassia");
        m.put("mappa stellare", "mappa del sistema");

        m.put("codex", "campione");

        return m;
    }
}
