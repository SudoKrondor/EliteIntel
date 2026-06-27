package elite.intel.ai.brain.i18n.de;

import elite.intel.ai.brain.i18n.InputNormalizerProvider;

import java.util.LinkedHashMap;

/**
 * German synonym substitution rules for the InputNormalizer.
 * <p>
 * German is a compounding language — a normalizer rule that matches a short word
 * can corrupt a longer compound word that contains it. Keep entries to complete,
 * unambiguous phrases, and register longer phrases before shorter ones they
 * contain (substring-safe ordering). Prefer adding variants to
 * {@link GermanAiActionAliases}'s properties; the Reducer handles those correctly.
 */
public class GermanInputNormalizerRules implements InputNormalizerProvider {

    @Override
    public java.util.Set<String> stopWords() {
        return java.util.Set.of(
                "der", "die", "das", "den", "dem", "des", "ein", "eine", "einen", "einem", "einer",
                "und", "oder", "aber", "mit", "von", "zum", "zur", "für", "aus", "bei", "nach",
                "über", "unter", "vor", "ist", "sind", "wird", "werden", "mir", "mich", "uns", "dich", "dir",
                "sein", "ihre", "doch", "noch", "mal", "bitte", "ich", "wir");
    }

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
    // Discovery scanner (honk) vs full-spectrum scan (FSS)
    //
    // In German "das System scannen/erkunden" is the discovery-scanner honk, while
    // the FSS is referred to with explicit terms (FSS, Spektralscan, Systemscan).
    // Collapse the honk phrasings onto the single canonical alias "erkunde das
    // system" so the Reducer direct-matches the discovery action. The open_fss
    // alias deliberately carries no bare "system"/"scanne" token, so FSS never
    // enters the candidate set for these inputs.
    // ─────────────────────────────────────────────────────────────────────────
    private void loadScanning(LinkedHashMap<String, String> m) {
        m.put("scanne das system", "erkunde das system");
        m.put("system erkunden", "erkunde das system");
        m.put("system abtasten", "erkunde das system");
        m.put("system scannen", "erkunde das system");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HUD modes
    // ─────────────────────────────────────────────────────────────────────────
    private void loadHudModes(LinkedHashMap<String, String> m) {
        m.put("erkundungsmodus", "in den analysemodus wechseln");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Hyperspace / supercruise
    //
    // "auf gehts" / "los gehts" are casual "let's jump" calls. Collapse onto the
    // canonical hyperspace alias so they never drift toward supercruise.
    // ─────────────────────────────────────────────────────────────────────────
    private void loadHyperspace(LinkedHashMap<String, String> m) {
        m.put("auf gehts", "sprung in den hyperraum");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Navigation / FSD target / cancel
    //
    // "ausschalten" collides with toggle_lights ("licht ausschalten"); collapse
    // the cancel-navigation phrasing onto "navigation abbrechen" so the lights
    // action never enters the candidate set. Also fold the verbose FSD next-jump
    // phrasing onto the canonical query alias.
    // ─────────────────────────────────────────────────────────────────────────
    private void loadNavigation(LinkedHashMap<String, String> m) {
        m.put("navigation ausschalten", "navigation abbrechen");
        m.put("info zum nächsten sprung", "fsd ziel info");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fleet carrier fuel / status normalization
    //
    // Question-form fuel/tritium phrasings confuse the small command model. Map
    // them to the canonical "carrier status" alias so the Reducer direct-matches.
    // Longer phrases first so substring replacement stays safe. The squadron
    // variants survive because "squadron carrier treibstoff" → "squadron carrier
    // status", which is itself a squadron-status alias phrase.
    // ─────────────────────────────────────────────────────────────────────────
    private void loadCarrierFuelStatus(LinkedHashMap<String, String> m) {
        m.put("treibstoffstatus des fleet carriers", "carrier status");
        m.put("treibstoffstand des carriers", "carrier status");
        m.put("carrier treibstoff", "carrier status");
        m.put("carrier tritium", "carrier status");
        // "Reichweite" (jump range) is a status query, but the small model reads it
        // as "how far away" and drifts to query_distance_to_carrier. Collapse the
        // range phrasings onto the canonical status alias.
        m.put("reichweite unseres carriers", "carrier status");
        m.put("reichweite des carriers", "carrier status");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Squadron carrier final destination
    //
    // "squadron carrier kurs" (the carrier's heading/course) means final
    // destination, not route, but the small model conflates Kurs with Route.
    // Normalize to the unambiguous canonical phrase so the Reducer direct-matches.
    // ─────────────────────────────────────────────────────────────────────────
    private void loadSquadronCarrierDestination(LinkedHashMap<String, String> m) {
        m.put("squadron carrier kurs", "endziel des squadron carriers");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phonetic corrections
    // Add German STT acoustic confusions here as they are characterised.
    // ─────────────────────────────────────────────────────────────────────────
    private void loadPhonetics(LinkedHashMap<String, String> m) {
        // Populate as German STT mishears are discovered during testing.
    }
}
