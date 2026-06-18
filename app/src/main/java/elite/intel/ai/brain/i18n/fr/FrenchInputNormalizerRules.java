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
        m.put("squadron-carrier", "porte-vaisseaux d'escadron");
        m.put("squadron carrier", "porte-vaisseaux d'escadron");
        m.put("carrier d'escadron", "porte-vaisseaux d'escadron");
        m.put("carrier de l'escadron", "porte-vaisseaux d'escadron");
        m.put("porte-vaisseau de l'escadron", "porte-vaisseaux d'escadron");
        m.put("rapport sur le porte-vaisseaux d'escadron", "statut du porte-vaisseaux d'escadron");
        m.put("rapport sur le porte vaisseaux d'escadron", "statut du porte-vaisseauxx d'escadron");
        m.put("rapport sur le porte-vaisseau d'escadron", "statut du porte-vaisseaux d'escadron");
        m.put("fleet-carrier", "porte-vaisseaux");
        m.put("fleet carrier", "porte-vaisseaux");
        m.put("porte-vaisseaux", "porte-vaisseaux");
        m.put("porte vaisseaux", "porte-vaisseaux");

        //power distribution
        m.put("puissance dans les boucliers", "redirige la puissance vers les boucliers");
        m.put("puissance aux boucliers", "redirige la puissance vers les boucliers");
        m.put("puissance dans les systèmes", "redirige la puissance vers les systèmes");
        m.put("puissance aux systèmes", "redirige la puissance vers les systèmes");
        m.put("puissance dans les moteurs", "redirige la puissance vers les moteurs");
        m.put("puissance aux moteurs", "redirige la puissance vers les moteurs");
        m.put("puissance dans les armes", "redirige la puissance vers les armes");
        m.put("puissance aux armes", "redirige la puissance vers les armes");
        m.put("équilibre le distributeur", "réinitialise la puissance");

        // ship status panel vs loadout query
        m.put("ouvre l'état général du vaisseau", "ouvre le panneau de statut");
        m.put("affiche l'état général du vaisseau", "affiche le panneau de statut");
        m.put("montre l'état général du vaisseau", "montre le panneau de statut");

        // cargo scoop: "trappe" always identifies the physical scoop/hatch
        m.put("ouvre la trappe du cargo", "ouvre la trappe");
        m.put("ferme la trappe du cargo", "ferme la trappe");
        m.put("ouvre la trappe de la soute", "ouvre la trappe");
        m.put("ferme la trappe de la soute", "ferme la trappe");
        m.put("ouvre la trappe de cargaison", "ouvre la trappe");
        m.put("ferme la trappe de cargaison", "ferme la trappe");

        //scannerFFS

        // biology
        m.put("combien d'espèces biologique reste-t-il à analyser dans le système", "progression biologique système");
        m.put("combien d'espèces biologiques reste-t-il à analyser dans le système", "progression biologique système");
        m.put("combien d'especes biologique reste-t-il a analyser dans le systeme", "progression biologique système");
        m.put("combien d'especes biologiques reste-t-il a analyser dans le systeme", "progression biologique système");

        // geology
        m.put("y a-t-il des signaux géologiques", "signaux géologiques");
        m.put("y a t il des signaux géologiques", "signaux géologiques");
        m.put("y a-t-il des signaux geologiques", "signaux géologiques");
        m.put("y a t il des signaux geologiques", "signaux géologiques");

        // route / itinerary queries
        m.put("rapport sur notre itinéraire", "rapport d'itinéraire");
        m.put("rapport sur notre itineraire", "rapport d'itinéraire");
        m.put("rapport sur la prochaine étape de l'itinéraire", "informations sur la prochaine destination");
        m.put("rapport sur la prochaine etape de l'itineraire", "informations sur la prochaine destination");

        // stations / landing places
        m.put("où puis-je me poser ici", "quelles stations dans le système");
        m.put("où puis je me poser ici", "quelles stations dans le système");
        m.put("ou puis-je me poser ici", "quelles stations dans le système");
        m.put("ou puis je me poser ici", "quelles stations dans le système");

        // announcements
        m.put("plus d'annonces", "désactive toutes les annonces");
        m.put("plus d annonces", "désactive toutes les annonces");

        // markets vs generic stations / commodity search
        m.put("matériels", "matériaux");
        m.put("materiels", "matériaux");
        m.put("materiaux", "matériaux");
        m.put("inventaire de bord", "inventaire du vaisseau");
        m.put("inventaire vaisseau", "inventaire du vaisseau");
        m.put("inventaire du bord", "inventaire du vaisseau");
        m.put("matériaux codés", "matériaux encodés");
        m.put("materiaux codes", "matériaux encodés");
        m.put("données encodées", "matériaux encodés");
        m.put("donnees encodees", "matériaux encodés");
        m.put("liste les stations avec commerce", "quels sont les marchés locaux");
        m.put("cherche une station avec commerce", "quels sont les marchés locaux");
        m.put("trouve moi le marché le plus proche", "quels sont les marchés locaux");
        m.put("trouve-moi le marché le plus proche", "quels sont les marchés locaux");

        return m;
    }
}
