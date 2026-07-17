package elite.intel.ai.brain.i18n.it;

import elite.intel.ai.brain.i18n.InputNormalizerProvider;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/** Italian input filters; acoustic corrections can be added when Italian STT mishears are characterised. */
public class ItalianInputNormalizerRules implements InputNormalizerProvider {

    @Override
    public Set<String> stopWords() {
        return Set.of(
                "il", "lo", "la", "gli", "le", "un", "uno", "una", "di", "del", "della",
                "dei", "delle", "dello", "degli", "da", "in", "con", "per", "su", "tra", "fra", "che",
                "mio", "mia", "nostro", "nostra", "suo", "sua", "questo", "questa", "questi", "queste",
                "sullo", "sulla", "sugli", "sulle", "quello", "quella", "quelli", "quelle", "sono",
                "io", "lui", "lei", "noi", "cazzo", "merda", "stronzo", "vaffanculo", "coglione",
                "bastardo", "puttana", "coglioni", "culo", "figa");
    }

    @Override
    public LinkedHashMap<String, String> buildPhoneticMap() {
        LinkedHashMap<String, String> m = new LinkedHashMap<>();
        m.put("hard points", "hardpoints");
        m.put("pro memoria", "promemoria");
        return m;
    }

    @Override
    public List<String> trashPhrases() {
        return List.of(
                "--", "mm-hmm", "uh-huh", "hmm", "mm", "uh", "um", "ah", "oh",
                "huh", "eh", "yeah", "yep", "yup", "nope", "it", "now", "ah ah ah ah ah",
                "okay", "ok", "got it", "thank you", "capito", "va bene", "d'accordo", "chiaro", "capito",
                "ciao", "grazie", "salve", "hey", "bye", "arrivederci", "addio",
                "per favore", "scusa", "scusami", "mi dispiace");
    }
}
