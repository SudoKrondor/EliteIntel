package elite.intel.ai.brain.i18n.pt;

import elite.intel.ai.brain.i18n.InputNormalizerProvider;

import java.util.List;
import java.util.Set;

/** Portuguese input filters; acoustic corrections can be added when Portuguese STT mishears are characterised. */
public class PortugueseInputNormalizerRules implements InputNormalizerProvider {

    @Override
    public Set<String> stopWords() {
        return Set.of(
                "os", "as", "um", "uma", "uns", "umas", "de", "do", "da", "dos", "das",
                "no", "na", "nos", "nas", "em", "com", "por", "para", "pra", "sem", "sobre", "entre",
                "mas", "que", "meu", "minha", "nosso", "nossa", "seu", "sua",
                "este", "esta", "estes", "estas", "são", "ele", "ela", "aqui");
    }

    @Override
    public List<String> trashPhrases() {
        return List.of(
                "hã", "hum", "hmm", "ãh", "eh", "ah", "oh",
                "sim", "não", "tá", "ok", "okay", "certo", "beleza", "tá bom",
                "olá", "oi", "tchau", "adeus", "obrigado", "obrigada", "desculpa", "desculpe", "com licença",
                "sabe", "entendi", "quer dizer", "claro", "sem problema");
    }
}
