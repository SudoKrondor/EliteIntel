package elite.intel.ai.brain.i18n.es;

import elite.intel.ai.brain.i18n.InputNormalizerProvider;

import java.util.Set;

/** Spanish input filters; acoustic corrections can be added when Spanish STT mishears are characterised. */
public class SpanishInputNormalizerRules implements InputNormalizerProvider {

    @Override
    public Set<String> stopWords() {
        return Set.of(
                "el", "la", "los", "las", "un", "una", "unos", "unas", "de", "del", "al", "en", "con",
                "por", "para", "sin", "sobre", "bajo", "entre", "pero", "que",
                "mi", "mis", "nuestro", "nuestra", "este", "esta", "estos", "estas",
                "son", "yo", "él", "ella", "nosotros");
    }
}
