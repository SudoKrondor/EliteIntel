package elite.intel.ai.brain.i18n.fr;

import elite.intel.ai.brain.i18n.InputNormalizerProvider;

import java.util.LinkedHashMap;
import java.util.Set;

/**
 * French input filters and acoustic corrections.
 */
public class FrenchInputNormalizerRules implements InputNormalizerProvider {

    /**
     * "Porte-vaisseau" and "porte-vaisseaux" are homophones, so which one reaches us is decided by the STT engine
     * rather than by the commander. Collapsing the plural onto the singular lets one authored alias serve both,
     * instead of every carrier phrase needing a twin: the fleet carrier is the single most-discussed object in the
     * French command set, and its aliases are written in the singular.
     */
    @Override
    public LinkedHashMap<String, String> buildPhoneticMap() {
        LinkedHashMap<String, String> corrections = new LinkedHashMap<>();
        corrections.put("porte-vaisseaux", "porte-vaisseau");
        corrections.put("porte vaisseaux", "porte vaisseau");
        return corrections;
    }

    @Override
    public Set<String> stopWords() {
        return Set.of(
                "le", "la", "les", "un", "une", "des", "du", "de", "et", "ou", "mais", "avec", "pour",
                "sur", "dans", "par", "vers", "sous", "chez", "est", "sont", "mon", "ma", "mes",
                "notre", "nos", "votre", "vos", "son", "sa", "ses", "ce", "cet", "cette", "ces",
                "que", "qui", "quoi", "je", "tu", "il", "elle", "nous", "vous");
    }
}
