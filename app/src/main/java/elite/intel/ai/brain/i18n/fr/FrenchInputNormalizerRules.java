package elite.intel.ai.brain.i18n.fr;

import elite.intel.ai.brain.i18n.InputNormalizerProvider;

import java.util.Set;

/** French input filters; acoustic corrections can be added when French STT mishears are characterised. */
public class FrenchInputNormalizerRules implements InputNormalizerProvider {

    @Override
    public Set<String> stopWords() {
        return Set.of(
                "le", "la", "les", "un", "une", "des", "du", "de", "et", "ou", "mais", "avec", "pour",
                "sur", "dans", "par", "vers", "sous", "chez", "est", "sont", "mon", "ma", "mes",
                "notre", "nos", "votre", "vos", "son", "sa", "ses", "ce", "cet", "cette", "ces",
                "que", "qui", "quoi", "je", "tu", "il", "elle", "nous", "vous");
    }
}
