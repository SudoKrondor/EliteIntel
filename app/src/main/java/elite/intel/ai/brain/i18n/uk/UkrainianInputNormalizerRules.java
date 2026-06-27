package elite.intel.ai.brain.i18n.uk;

import elite.intel.ai.brain.i18n.InputNormalizerProvider;
import elite.intel.ai.brain.i18n.ru.RussianInputNormalizerRules;

import java.util.LinkedHashMap;

/**
 * Ukrainian synonym substitution rules for the InputNormalizer.
 * <p>
 * See {@link RussianInputNormalizerRules} for the morphology warning that applies
 * equally to Ukrainian: the normalizer does plain substring replacement without
 * word-boundary awareness, so only add complete, standalone phrases where no
 * common word contains them as a substring. Longer phrases must be registered
 * before shorter ones that they contain (substring-safe ordering).
 * <p>
 * When in doubt, add the synonym as a comma-separated variant in
 * {@link UkrainianAiActionAliases}'s properties instead — the Reducer handles
 * that correctly.
 */
public class UkrainianInputNormalizerRules implements InputNormalizerProvider {

    @Override
    public LinkedHashMap<String, String> buildSynonymMap() {
        LinkedHashMap<String, String> m = new LinkedHashMap<>();
        loadScanning(m);
        loadHudModes(m);
        loadHyperspace(m);
        loadNavigation(m);
        loadCarrierFuelStatus(m);
        loadSquadronCarrierDestination(m);
        loadPhonetics(m);
        return m;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // System scan (honk) vs full-spectrum scan (FSS)
    //
    // The bare verb "скануй" is shared by the honk alias ("скануй систему") and
    // the FSS alias ("відкрий FSS і скануй"), so the small model drifts to FSS.
    // Collapse the honk phrasings onto the canonical honk alias "досліджуй систему"
    // so the Reducer direct-matches and FSS drops out of the candidate set.
    // "відскануй систему" is registered first because it contains "скануй систему".
    // ─────────────────────────────────────────────────────────────────────────
    private void loadScanning(LinkedHashMap<String, String> m) {
        m.put("відскануй систему", "досліджуй систему");
        m.put("скануй систему", "досліджуй систему");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HUD modes / target selection
    //
    // Collapse the colloquial synonyms onto the canonical alias phrase so the
    // small command model never has to reason about them.
    // ─────────────────────────────────────────────────────────────────────────
    private void loadHudModes(LinkedHashMap<String, String> m) {
        m.put("режим дослідника", "перемкнись у режим аналізу");
        m.put("наступний ворог", "пріоритетна ціль");
        m.put("вибрати ворога", "пріоритетна ціль");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Hyperspace / supercruise
    //
    // Short jump/supercruise colloquialisms that are easy to mishear as a
    // different intent. "відходимо" and "поїхали" both mean "let's jump".
    // ─────────────────────────────────────────────────────────────────────────
    private void loadHyperspace(LinkedHashMap<String, String> m) {
        m.put("відходимо", "стрибок у гіперпростір");
        m.put("погнали", "стрибок у гіперпростір");
        m.put("поїхали", "стрибок у гіперпростір");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Navigation / fighter commands
    //
    // "курс на авіаносець ескадрильї" (accusative: navigate TO the carrier) is
    // distinct from "курс авіаносця ескадрильї" (genitive: the carrier's
    // heading/destination). The former is a navigation command; the latter is
    // handled by loadSquadronCarrierDestination below. Also fold the verbose FSD
    // next-jump phrasing onto the canonical query alias.
    // ─────────────────────────────────────────────────────────────────────────
    private void loadNavigation(LinkedHashMap<String, String> m) {
        m.put("курс на авіаносець ескадрильї", "лети до авіаносця ескадрильї");
        m.put("інформація про наступний стрибок", "інформація про ціль fsd");
        m.put("зосередься", "фокус");
        // "вимкни" collides with toggle_lights ("вимкни світло"); collapse the
        // cancel-navigation phrasing onto the unambiguous "скасуй" verb so the
        // lights action never enters the candidate set.
        m.put("вимкни навігацію", "скасуй навігацію");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fleet carrier fuel / status normalization
    //
    // Question-form phrasings of carrier fuel status confuse the small command
    // model. Map them to the canonical alias phrase so the Reducer direct-matches,
    // removing the LLM ambiguity entirely. Longer phrases appear before shorter
    // ones so substring replacement stays safe. The squadron variants survive
    // because "паливо авіаносця ескадрильї" → "статус авіаносця ескадрильї",
    // which is itself a squadron-status alias.
    // ─────────────────────────────────────────────────────────────────────────
    private void loadCarrierFuelStatus(LinkedHashMap<String, String> m) {
        m.put("який статус палива флотського авіаносця", "статус авіаносця");
        m.put("рівень палива авіаносця", "статус авіаносця");
        m.put("паливо авіаносця", "статус авіаносця");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Squadron carrier final destination
    //
    // "курс авіаносця ескадрильї" (the carrier's heading/course) means final
    // destination, not route, but the small model conflates курс with маршрут.
    // Normalize to the unambiguous canonical phrase so the Reducer direct-matches.
    // ─────────────────────────────────────────────────────────────────────────
    private void loadSquadronCarrierDestination(LinkedHashMap<String, String> m) {
        m.put("курс авіаносця ескадрильї", "кінцевий пункт призначення авіаносця ескадрильї");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phonetic corrections
    // Add Ukrainian STT acoustic confusions here as they are discovered in testing.
    // ─────────────────────────────────────────────────────────────────────────
    private void loadPhonetics(LinkedHashMap<String, String> m) {
        // Populate as Ukrainian STT mishears are characterised.
    }
}
