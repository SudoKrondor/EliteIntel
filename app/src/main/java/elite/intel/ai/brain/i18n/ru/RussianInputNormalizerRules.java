package elite.intel.ai.brain.i18n.ru;

import elite.intel.ai.brain.i18n.InputNormalizerProvider;

import java.util.LinkedHashMap;
import java.util.Set;

/** Russian input filters; acoustic corrections can be added when Russian STT mishears are characterised. */
public class RussianInputNormalizerRules implements InputNormalizerProvider {

    @Override
    public LinkedHashMap<String, String> buildPhoneticMap() {
        LinkedHashMap<String, String> corrections = new LinkedHashMap<>();
        corrections.put("деполи", "диполи");
        return corrections;
    }

    @Override
    public Set<String> stopWords() {
        return Set.of(
                "на", "во", "со", "ко", "за", "по", "от", "до", "из", "об",
                "для", "или", "при", "над", "под", "про", "без", "около", "через", "между", "после", "перед",
                "что", "как", "это", "этот", "эта", "эти", "чтобы", "тоже", "также", "если", "либо", "потому",
                "его", "ее", "её", "их", "мне", "нам", "вам", "нас", "вас", "меня", "тебя", "свой", "наш",
                "уже", "ещё", "еще", "вот", "даже", "лишь");
    }
}
