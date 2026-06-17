package elite.intel.ai.brain.i18n.ru;

import elite.intel.ai.brain.i18n.InputNormalizerProvider;

import java.util.LinkedHashMap;

/**
 * Russian synonym substitution rules for the InputNormalizer.
 * <p>
 * <strong>Morphology warning:</strong> Russian is a heavily inflected language.
 * The InputNormalizer does plain substring replacement without word-boundary
 * awareness, so a rule like {@code "лети" → "навигация"} would corrupt words
 * that contain "лети" as a suffix. Add only complete, standalone phrases where
 * you are certain no common word contains them as a substring.
 * <p>
 * When in doubt, add the synonym as a comma-separated variant in
 * {@link RussianAiActionAliases} instead  the Reducer handles that correctly.
 * <p>
 * <strong>Phonetic corrections</strong> belong here too, once the Russian STT
 * engine (Whisper, etc.) is characterised and common mishears are known.
 */
public class RussianInputNormalizerRules implements InputNormalizerProvider {

    @Override
    public LinkedHashMap<String, String> buildSynonymMap() {
        LinkedHashMap<String, String> m = new LinkedHashMap<>();
        loadHudModes(m);
        loadHyperspace(m);
        colloquialTerms(m);
        loadPhonetics(m);
        return m;
    }

    /// Slang, synonyms
    private void colloquialTerms(LinkedHashMap<String, String> m) {
        m.put("открыть дверь грузового отсека", "грузовой люк");
        m.put("применить деполи", "выбросить помехи");
        m.put("выстрелить помехами", "выбросить помехи");
        m.put("четверть тяги", "малый ход");
        m.put("дроп", "выйти из суперкруиза");
        m.put("сканируй", "исследуй систему");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HUD modes
    // ─────────────────────────────────────────────────────────────────────────
    private void loadHudModes(LinkedHashMap<String, String> m) {
        m.put("боевой режим", "переключись в боевой режим");
        m.put("режим анализа", "переключись в режим анализа");
        m.put("режим исследователя", "переключись в режим анализа");
        m.put("следующий враг", "приоритетная цель");
        m.put("выбрать врага", "приоритетная цель");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Hyperspace / supercruise
    //
    // Russian morphology reduces substring collision risk compared to English
    // (e.g. "прыжок" vs "прыжков" are different forms), but the most common
    // short jump synonym "уходим" is not covered by the aliases, so map it here.
    // ─────────────────────────────────────────────────────────────────────────
    private void loadHyperspace(LinkedHashMap<String, String> m) {
        m.put("уходим", "прыжок в гиперпространство");
        m.put("погнали", "прыжок в гиперпространство");
        m.put("давай прыгнем", "прыжок в гиперпространство");
        m.put("суперкруиз", "войти в суперкруиз");
        m.put("поехали", "войти в суперкруиз");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phonetic corrections
    // Add STT mishears specific to the Russian voice model here.
    // ─────────────────────────────────────────────────────────────────────────
    private void loadPhonetics(LinkedHashMap<String, String> m) {
        // Populate as Russian STT acoustic confusions are discovered during testing.
    }
}
