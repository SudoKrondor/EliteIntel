package elite.intel.ai.brain.i18n.uk;

import elite.intel.ai.brain.i18n.InputNormalizerProvider;

import java.util.Set;

/** Ukrainian input filters; acoustic corrections can be added when Ukrainian STT mishears are characterised. */
public class UkrainianInputNormalizerRules implements InputNormalizerProvider {

    @Override
    public Set<String> stopWords() {
        return Set.of(
                "на", "із", "від", "об",
                "для", "або", "при", "над", "під", "про", "без", "біля", "через", "між", "після", "перед",
                "що", "як", "це", "цей", "ця", "ці", "щоб", "теж", "також", "якщо", "тому",
                "його", "її", "їх", "мені", "нам", "вам", "нас", "вас", "мене", "тебе", "свій", "наш",
                "вже", "ось", "навіть", "лише");
    }
}
