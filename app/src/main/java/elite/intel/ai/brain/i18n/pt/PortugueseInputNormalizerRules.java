package elite.intel.ai.brain.i18n.pt;

import elite.intel.ai.brain.i18n.InputNormalizerProvider;

import java.util.LinkedHashMap;

public class PortugueseInputNormalizerRules implements InputNormalizerProvider {

    @Override
    public java.util.Set<String> stopWords() {
        return java.util.Set.of(
                "os", "as", "um", "uma", "uns", "umas", "de", "do", "da", "dos", "das",
                "no", "na", "nos", "nas", "em", "com", "por", "para", "sem", "sobre", "entre",
                "mas", "que", "meu", "minha", "nosso", "nossa", "seu", "sua",
                "este", "esta", "estes", "estas", "são", "ele", "ela");
    }

    @Override
    public LinkedHashMap<String, String> buildSynonymMap() {
        return new LinkedHashMap<>();
    }
}
