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
                "sullo", "sulla", "sugli", "sulle", "quello", "quella", "quelli", "quelle", "sono",
                "io", "lui", "lei", "noi");
    }

    @Override
    public LinkedHashMap<String, String> buildSynonymMap() {
        LinkedHashMap<String, String> m = new LinkedHashMap<>();

        m.put("mappa galattica", "mappa della galassia");
        m.put("mappa stellare", "mappa del sistema");

        m.put("codex", "campione");

        return m;
    }
}
