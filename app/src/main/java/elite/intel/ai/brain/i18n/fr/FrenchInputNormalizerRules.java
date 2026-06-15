package elite.intel.ai.brain.i18n.fr;

import elite.intel.ai.brain.i18n.InputNormalizerProvider;

import java.util.LinkedHashMap;

/**
 * French synonym substitution rules for the InputNormalizer.
 * <p>
 * French uses liaison and elision (e.g. "l'", "d'")  plain substring replacement
 * can match across word boundaries unexpectedly. Keep entries to complete,
 * unambiguous phrases. Prefer adding variants to {@link FrenchAiActionAliases}.
 */
public class FrenchInputNormalizerRules implements InputNormalizerProvider {

    @Override
    public LinkedHashMap<String, String> buildSynonymMap() {
        LinkedHashMap<String, String> m = new LinkedHashMap<>();
        // Add French synonym rules here as they are identified during testing.

        // always available
        m.put("désactive les commandes vocales","passe en mode veille");
        // docking

        // speed /throttle

        // fleet carrier

        //powerdistribution
        m.put("priorité aux systèmes","priorité aux boucliers");
        m.put("puissance dans les systèmes","puissance dans les boucliers");
        m.put("redirige la puissance vers les systèmes","redirige la puissance vers les boucliers");

        //scannerFFS

        // biology

        return m;
    }
}
