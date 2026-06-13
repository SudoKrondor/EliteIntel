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
        m.put("ecoute", "commande vocale");

        // docking
        m.put("remonte", "relève");
        m.put("sors les trains d'atterrissages","déploie les trains d'atterrissages");

        // speed /throttle
        m.put("mets les gaz", "pleine poussée");
        m.put("plein gaz", "pleine poussée");

        // fleet carrier
        m.put("quand arrive le porte-vaisseaux", "ETA porte-vaisseaux");
        m.put("heure d'arrivée du porte-vaisseaux", "ETA porte-vaisseaux");
        m.put("où se déplace le porte-vaisseau", "route porte-vaisseau");

        //powerdistribution
        m.put("energie dans les armes", "mets la puissance dans les armes");
        m.put("priorité aux armes", "mets la puissance dans les armes");
        m.put("energie dans les moteurs", "mets la puissance dans les moteurs");
        m.put("priorité aux moteurs", "mets la puissance dans les moteurs");
        m.put("distance du porte-vaisseaux", "distance au porte-vaisseaux");

        m.put("où est notre porte-vaisseaux", "distance au porte-vaisseaux");
        //scannerFFS
        m.put("outils d'analyse du système", "ouvre scanner système");
        // biology
        m.put("navigue vers entree codex", "navigue vers prochain échantillon biologique");
        return m;
    }
}
