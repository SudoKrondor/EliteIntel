package elite.intel.ai.brain.i18n.ptbz;

import elite.intel.ai.brain.i18n.InputNormalizerProvider;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Brazilian Portuguese synonym substitution rules for the InputNormalizer.
 * <p>
 * Portuguese is moderately inflected. The InputNormalizer does plain substring
 * replacement without word-boundary awareness, so add only complete, standalone
 * phrases where you are certain no common word contains them as a substring, and
 * register longer phrases before shorter ones they contain (substring-safe ordering).
 * When in doubt, add the synonym as a comma-separated variant in
 * {@link BrazilianPortugueseAiActionAliases}'s properties instead; the Reducer handles those correctly.
 * <p>
 * <strong>Phonetic corrections</strong> belong here too, once the Portuguese STT
 * engine is characterised and common mishears are known.
 */
public class BrazilianPortugueseInputNormalizerRules implements InputNormalizerProvider {

    @Override
    public java.util.Set<String> stopWords() {
        return java.util.Set.of(
                "os", "as", "um", "uma", "uns", "umas", "de", "do", "da", "dos", "das",
                "no", "na", "nos", "nas", "em", "com", "por", "para", "pra", "sem", "sobre", "entre",
                "mas", "que", "meu", "minha", "nosso", "nossa", "seu", "sua",
                "este", "esta", "estes", "estas", "são", "ele", "ela", "aqui");
    }

    @Override
    public LinkedHashMap<String, String> buildSynonymMap() {
        LinkedHashMap<String, String> m = new LinkedHashMap<>();
        loadScanning(m);
        loadHudModes(m);
        loadHyperspace(m);
        loadNavigation(m);
        loadCarrierFuelStatus(m);
        loadPhonetics(m);
        return m;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Discovery scanner (honk) vs full-spectrum scan (FSS)
    //
    // In Portuguese "escanear/varrer o sistema" is the discovery-scanner honk,
    // while the FSS is referred to with explicit terms (FSS, varredura espectral).
    // Collapse the honk phrasings onto the single canonical alias "explore o
    // sistema" so the Reducer direct-matches the discovery action.
    // ─────────────────────────────────────────────────────────────────────────
    private void loadScanning(LinkedHashMap<String, String> m) {
        m.put("escaneie o sistema", "explore o sistema");
        m.put("escanear o sistema", "explore o sistema");
        m.put("varrer o sistema", "explore o sistema");
        m.put("varredura do sistema", "explore o sistema");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HUD modes
    // ─────────────────────────────────────────────────────────────────────────
    private void loadHudModes(LinkedHashMap<String, String> m) {
        m.put("modo de exploração", "mudar para o modo de análise");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Hyperspace / supercruise
    //
    // "vamos nessa" / "bora" are casual "let's jump" calls. Collapse onto the
    // canonical hyperspace alias so they never drift toward supercruise.
    // ─────────────────────────────────────────────────────────────────────────
    private void loadHyperspace(LinkedHashMap<String, String> m) {
        m.put("vamos nessa", "salto para o hiperespaço");
        m.put("bora", "salto para o hiperespaço");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Navigation / FSD target / cancel
    //
    // "desligar navegação" collides with toggle_lights; collapse the
    // cancel-navigation phrasing onto "cancelar navegação". Also fold the verbose
    // FSD next-jump phrasing onto the canonical query alias.
    // ─────────────────────────────────────────────────────────────────────────
    private void loadNavigation(LinkedHashMap<String, String> m) {
        m.put("desligar navegação", "cancelar navegação");
        m.put("informação sobre o próximo salto", "informação do alvo fsd");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fleet carrier fuel / status normalization
    //
    // Question-form fuel/tritium/range phrasings confuse the small command model.
    // Map them to the canonical "status do carrier" alias so the Reducer
    // direct-matches. Longer phrases first so substring replacement stays safe.
    // ─────────────────────────────────────────────────────────────────────────
    private void loadCarrierFuelStatus(LinkedHashMap<String, String> m) {
        m.put("status de combustível do fleet carrier", "status do carrier");
        m.put("nível de combustível do carrier", "status do carrier");
        m.put("combustível do carrier", "status do carrier");
        m.put("trítio do carrier", "status do carrier");
        // "alcance" (jump range) is a status query, but the small model reads it as
        // "how far away" and drifts to query_distance_to_carrier. Collapse the range
        // phrasings onto the canonical status alias.
        m.put("alcance do nosso carrier", "status do carrier");
        m.put("alcance do carrier", "status do carrier");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phonetic corrections
    // Add Portuguese STT acoustic confusions here as they are characterised.
    // ─────────────────────────────────────────────────────────────────────────
    private void loadPhonetics(LinkedHashMap<String, String> m) {
        // Populate as Portuguese STT mishears are discovered during testing.
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Trash phrases — filler / noise utterances the STT emits as standalone output
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public List<String> trashPhrases() {
        return List.of(
                "hã", "hum", "hmm", "ãh", "eh", "ah", "oh",
                "sim", "não", "tá", "ok", "okay", "certo", "beleza", "tá bom",
                "olá", "oi", "ei", "tchau", "até logo",
                "então", "bem", "agora", "enfim", "na verdade", "tipo",
                "obrigado", "obrigada", "desculpa", "desculpe", "com licença",
                "sabe", "entendi", "quer dizer", "claro", "sem problema");
    }
}
