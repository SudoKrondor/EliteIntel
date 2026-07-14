package elite.intel.ai.brain.i18n.de;

import elite.intel.ai.brain.i18n.InputNormalizerProvider;

import java.util.Set;

/** German input filters; add acoustic corrections here when the German STT model is characterised. */
public class GermanInputNormalizerRules implements InputNormalizerProvider {

    @Override
    public Set<String> stopWords() {
        return Set.of(
                "der", "die", "das", "den", "dem", "des", "ein", "eine", "einen", "einem", "einer",
                "und", "oder", "aber", "mit", "von", "zum", "zur", "für", "aus", "bei", "nach",
                "über", "unter", "vor", "ist", "sind", "wird", "werden", "mir", "mich", "uns", "dich", "dir",
                "sein", "ihre", "doch", "noch", "mal", "bitte", "ich", "wir");
    }
}
