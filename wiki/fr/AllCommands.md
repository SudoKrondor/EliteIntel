# Guide des requêtes et commandes IA d'EliteIntel

Salut Commander ! Voici une référence des types de choses que vous pouvez demander à votre compagnon **Elite Intel**.
**Idéalement, vous n'avez pas besoin de mémoriser quoi que ce soit**  parlez naturellement et l'application comprend ce que vous voulez dire. Cette liste existe pour vous montrer ce qui est possible, pas pour vous faire réciter des scripts.

---

## Application

- Vérifier les raccourcis clavier manquants ou non assignés.
- Veille / Réveil (En mode veille, l'entrée vocale est ignorée. Dites 'Listen Up' pour transmettre une commande, ou 'Wake up' pour revenir en mode normal)

## Exploration et localisation

- Activer / Désactiver les annonces de découverte.
- Où sommes-nous en ce moment ?
- Quelle est la distance depuis la Bulle / notre Fleet Carrier / le dernier échantillon bio ?
- Quelle est la distance jusqu'à une planète, une lune ou une station spécifique ?
- Quels matériaux sont disponibles sur cette planète / lune ?
- Analyser le scan le plus récent / les données du corps.
- Quel est l'ETA pour le saut de notre Fleet Carrier ?
- Quelle heure est-il / heure UTC actuelle ? (requête d'horloge en temps réel)
- Analyser le biome de ce système stellaire.
- Quelles planètes nécessitent encore des scans bio ou organiques ?
- Quels scans bio avons-nous effectués ?
- Quels organismes nous reste-t-il à scanner ?
- Quelles planètes ou lunes sont atterrissables dans ce système ?
- Quels signaux y a-t-il dans ce système ?
- Quelles planètes ont des signaux géologiques ?
- Ouvrir le FSS et scanner. / Effectuer un scan à spectre filtré.
- Sécurité du système, qui contrôle ce système, contrôle factionnaire ?
- Quel est notre profil joueur / grades / statistiques ?

## Exobiologie

- Quels scans bio avons-nous effectués ? / système stellaire actuel (complétés, partiels, restants).
- Potentiel de profit d'exploration dans ce système.
- Localisation et distance du dernier échantillon bio. (nécessite un scan d'entrée au codex avec l'analyseur de composition)
- Naviguer vers le prochain échantillon bio / organique / entrée du codex. (nécessite un scan d'entrée au codex avec l'analyseur de composition)
- Quels organismes ou biologie y a-t-il sur cette planète / lune ?
- Naviguer vers le prochain organique / échantillon / entrée du codex.
- Analyse du biome pour [nom du système stellaire / planète / lune].

## Fleet Carrier

Mentionnez bien "carrier", sinon l'IA pourrait penser que vous parlez du vaisseau !

- Quelle est la portée de notre carrier (inclut le carburant de réserve si défini manuellement) / statistiques / carburant.
- Définir la réserve de carburant du carrier [quantité] (définit la réserve de tritium du Fleet Carrier)
- Où notre carrier saute-t-il ensuite ?
- Dans combien de temps le carrier arrive-t-il ? (ETA du Fleet Carrier)
- Quel est le statut du carburant / la portée de saut / la réserve de carburant de mon Fleet Carrier ?
- Combien de temps pouvons-nous opérer avec les fonds actuels ?
- Jusqu'où le carrier peut-il sauter avec le tritium actuel ?
- Quelle est la route du carrier ? / Combien de sauts restants sur la route du carrier ?
- Distance depuis le Fleet Carrier ?
- Des Fleet Carriers dans ce système ?

## Vaisseau et systèmes

- Activer / Désactiver les annonces de route.
- L'étoile suivante est-elle scoopable ?
- Analyser la cible FSD (allégeance, trafic, sécurité, données géo/bio, etc.).
- Qu'est-ce qu'il y a dans ma soute ?
- Quelle est votre configuration ?
- Analyser notre route. (résumé de la route, disponibilité du carburant)
- Disponibilité du carburant sur la route / prochain ravitaillement.
- Avons-nous [matériau] ? / Quelle quantité de [matériau] avons-nous ? / Inventaire des matériaux.
- Quel est notre grade / profil joueur ?

## Stations et marchés

- Quels services sont disponibles dans les stations locales ?
- Équipement / pièces de vaisseau / modules en vente ici ?
- Des vaisseaux à vendre dans cette station ?
- Monétiser ma route (nécessite un profil de commerce configuré, voir Configuration du profil de commerce)
- Calculer une route commerciale (nécessite un profil de commerce configuré, voir Configuration du profil de commerce)
- Où dois-je aller pour acheter/vendre [marchandise] ?
- Qu'y a-t-il sur le marché local ?
- Données sur les stations, ports et établissements du système.
- Rappelle-moi [texte] (lit les rappels définis pendant un parcours commercial, vous indique quelle station rejoindre, etc.)
- Quel est notre plan commercial / route commerciale / étapes de la route actuelle ?

## Configuration du profil de commerce

- Changer le capital de départ du profil commercial [montant]. (définit le capital initial pour la route commerciale)
- Changer la distance maximale du profil commercial [X]. (filtre les stations selon cette distance depuis l'entrée)
- Changer le nombre maximum d'escales du profil commercial [N]. (la route n'aura pas plus de N escales)
- Changer le profil commercial pour autoriser les cargaisons prohibées [on/off].
- Changer le profil commercial pour autoriser les ports planétaires [on/off].
- Changer le profil commercial pour autoriser les systèmes à permis [on/off].
- Changer le profil commercial pour autoriser les forteresses [on/off].
- Profil commercial / paramètres de commerce / configuration commerciale. (décrit le profil actuel)
- Lister les paramètres de la route commerciale.

## ⚔️ Combat et missions

- Annonces de contact radar on/off (active ou désactive les annonces radar)
- Trouver des zones de chasse (Trouvera des paires cible/fournisseur de mission. Vous indiquera de voler vers un système cible et confirmera la présence du site d'extraction de ressources)
- Reconnaître la zone de chasse / naviguer vers la zone de chasse (naviguer vers le système stellaire cible pour l'explorer)
- Ignorer la zone de chasse (passer la zone de chasse candidate actuelle)
- Confirmer la zone de chasse (confirmera manuellement la présence du site d'extraction de ressources, peut être nécessaire si nous ne l'avons pas détecté depuis la balise ou par découverte automatique)
- Naviguer vers le système du fournisseur de mission. / Naviguer vers le fournisseur de mission pirate.
- Naviguer vers une mission active / tracer une route vers une mission active. (naviguera vers le système stellaire de la mission active)
- Combien de kills pirates restants ? (calcule la pile)
- Progression de la mission massacre / kills restants / progression de la chasse aux primes.
- Quelles missions actives ai-je ? (toutes missions, pas seulement les pirates)
- Total des primes collectées cette session.
- Commandes de ciblage des sous-systèmes : 'target drive', 'target fsd', 'target power distributor', 'target powerplant', 'target life support'. NOTE : les sous-systèmes optionnels ne sont pas inclus dans la liste.
- Cibler équipier 1 (wingman alpha) / équipier 2 (wingman bravo) / équipier 3 (wingman charlie).
- Cible prioritaire / cible la plus grande menace / ennemi suivant / sélectionner hostile.

## 🧭 Commandes de navigation

- Naviguer vers les coordonnées [latitude] [longitude]. (guidera de l'orbite vers la localisation dans un rayon de 1 km, puis à 100m une fois atterri)
- Naviguer vers la zone d'atterrissage. (donnera un guidage GPS vers l'endroit où votre vaisseau a atterri la dernière fois)
- Naviguer vers le prochain échantillon bio / entrée du codex.
- Naviguer vers le Fleet Carrier le plus proche.
- Naviguer vers le carrier / rejoindre le carrier / retourner au carrier.
- Naviguer vers / aller à / se diriger vers / voler vers / m'emmener à / mettre cap sur / me guider vers / tracer une route vers [destination].
- Naviguer vers la prochaine escale commerciale / aller à la prochaine escale commerciale.
- Calculer la route du Fleet Carrier. *(Ouvrez la carte galactique, sélectionnez une étoile, copiez le nom dans le presse-papiers d'abord.)*
- Entrer la destination du carrier / définir la destination du carrier. *(Ouvrez d'abord la carte galactique du Fleet Carrier.)*
- Calculer une route commerciale / tracer une route commerciale rentable.
- Trouver le Vista Genomics le plus proche.
- Trouver le courtier technologique humain / guardian le plus proche.
- Trouver le négociant en matériaux bruts / encodés / manufacturés le plus proche.
- Trouver / localiser / rechercher des brain trees [dans un rayon de X années-lumière].
- Trouver un site minier pour [matériau] [dans un rayon de X années-lumière].
- Trouver où miner du Tritium [dans un rayon de X années-lumière].
- Trouver où acheter [marchandise] [dans un rayon de X années-lumière].
- Trouver des zones de chasse.
- Annuler la navigation.
- Tracer une route vers le Fleet Carrier.
- Me ramener à la maison / naviguer vers le système d'origine.
- Définir comme système d'origine.
- Vitesse optimale. *(Règle les gaz à 75% pour l'approche en supercruise.)*
- Augmenter / Diminuer la vitesse de [quantité]. 1-10
- Définir la réserve de carburant du carrier à [quantité]. (définit la réserve de tritium du Fleet Carrier)
- Sélectionner la destination FDS / sélectionner la destination. (sélectionne le prochain système sur la route tracée)
- Verrouillage de navigation en aile / verrouiller la navigation de l'équipier.
- Sélectionner / cibler la plus grande menace / prochain hostile / cibler ennemi.
- Naviguer depuis la mémoire. (Ouvre la carte galactique et navigue vers la localisation que vous avez copiée via Ctrl+C.)

## 🎮 Contrôles du vaisseau

- Train d'atterrissage rentré / sorti | déployer / rétracter le train d'atterrissage.
- Déployer / rétracter les points d'ancrage / armes.
- Armes prêtes ! (identique à déployer les points d'ancrage)
- Déployer le dissipateur thermique.
- Déployer / récupérer le SRV / la voiture / le buggy.
- Monter à bord du vaisseau / récupérer le SRV / amarrer le SRV. (récupère le SRV en mode SRV)
- Débarquer. (sortir du vaisseau à pied)
- Ouvrir / fermer la trappe à cargaison, la soute, l'écoutille cargo.
- Demander l'amarrage. / Contacter la tour, obtenir un pas d'atterrissage, une place de parking, etc.
- Lancer le vaisseau / quitter la station / se détacher de la station.
- Taxi vers l'atterrissage / atterrissage automatique / pilote automatique d'atterrissage.
- Activer le supercruise / entrer en supercruise / aller en supercruise.
- Saut hyperspatial / entrer en hyperespace / on y va / prochain waypoint. (saut réel, pas le supercruise)
- Sortir / sortir ici / sortir du FTL / quitter le supercruise / sortir du supercruise.
- Stop complet / arrêt moteur / vitesse zéro / couper les moteurs.
- Régler la vitesse à : un quart / moitié / trois quarts / pleine puissance.
- Vitesse plus / moins [quantité].
- Toute puissance sur les boucliers / moteurs / armes. Égaliser la puissance. / boucliers max, moteurs max, armes max.
- Mode combat / passer en mode combat → passe à l'affichage de combat.
- Mode analyse / passer en mode analyse / mode explorateur → passe à l'affichage d'analyse.
- Vision nocturne (on / off).
- Phares (on / off).
- Assistance de conduite on / off. (mode SRV)
- Renvoyer le vaisseau / aller en orbite / aller jouer.
- Retourner à la surface / venez me chercher.
- Ouvrir le FSS et scanner / honk / scanner le système / effectuer un scan / scan de découverte / scan du système / scan à spectre complet. (Ouvrira l'interface FSS complète et effectuera le honk)
- Afficher / ouvrir - carte galactique / carte locale / fermer la carte.
- Quitter / fermer (Quitte les menus / onglets / sous-menus et retourne à l'affichage principal).
- Interrompre / tais-toi / silence / annuler [arrête la synthèse vocale en cours].
- Activer (active ce qui est sélectionné dans l'interface).

## 🎙️ Commandes du chasseur

- Déployer le chasseur / lancer le chasseur / envoyer le chasseur.
- Ordonner aux chasseurs de défendre le vaisseau.
- Ordonner au chasseur de se concentrer sur ma cible / attaquer ma cible / tir à volonté / ordres libres pour le chasseur.
- Ordonner au chasseur de cesser le feu / chasseur cessez le feu.
- Ordonner au chasseur de retourner au vaisseau mère / rappeler le chasseur / chasseur amarre.

## 📺 Panneaux de l'interface

Dites **afficher**, **ouvrir**, ou **afficher** suivi du nom du panneau :

- Panneau de navigation
- Panneau des transactions
- Panneau des contacts
- Panneau de chat / panneau de communications
- Panneau de la boîte mail
- Panneau social
- Panneau d'historique
- Panneau d'escadron
- Panneau d'état
- Panneau Commander / panneau de rôle / carnet de bord
- Panneau d'équipage
- Panneau d'accueil (panneau interne)
- Panneau des modules
- Groupes de feu
- Panneau d'inventaire
- Panneau de stockage
- Panneau du chasseur
- Panneau de gestion du carrier
- Carte galactique
- Carte locale / du système
- Panneau des services (SRV amarré à une station)

Autres commandes de panneau :
- Quitter / fermer le panneau (quitte les menus et retourne à l'affichage principal).

## ⚙️ Commandes de l'application et de session

- **"Ignore me" / "do not monitor" / "sleep"** → met l'application en veille (ignore toutes les entrées).
- **"Wake up"** → reprend l'écoute normale.
- **"Listen up [commande]"** → contournement : transmet une seule commande ou requête pendant que l'application reste en veille. Le préfixe "listen up" est supprimé avant que la commande atteigne l'IA. Exemple : *"Listen up, jump to hyperspace"* exécute le saut sans réveiller l'application.
- Définir un rappel [texte]. / Rappelle-moi [texte].
- Effacer les rappels.
- Activer / désactiver les annonces de route.
- Activer / désactiver les annonces de découverte.
- Activer / désactiver les annonces d'exploitation minière et de matériaux.
- Activer / désactiver les bavardages radio / trafic radio.
- Désactiver toutes les annonces.
- Annonces de contact radar on / off.
- Ajouter une cible minière [nom du matériau].
- Supprimer une cible minière [nom du matériau].
- Effacer les cibles minières.
- Supprimer l'entrée du codex. (Supprime l'entrée du codex que vous suivez en GPS)

## 💬 Chat général (le mode conversation doit être ACTIVÉ)

Par défaut, l'application fonctionne en **Mode strict** : si une entrée ne correspond pas à une commande ou requête connue, elle est silencieusement ignorée.
C'est intentionnel  cela empêche le bruit du STT et les bavardages en arrière-plan de déclencher des actions aléatoires en plein vol.

Activez le **Mode conversation** dans l'onglet Paramètres pour activer le chat libre. Lorsqu'il est activé, tout ce qui ne correspond pas à une commande bascule vers la conversation générale  lore du jeu, sujets du monde réel, configurations de vaisseaux, tout ce que vous voulez. L'IA n'est pas seulement un analyseur de commandes quand vous voulez qu'elle soit davantage.

Les LLMs locaux répondront mais seront rigides. Les LLMs cloud (Claude, OpenAI, xAI, Mistral, Deepseek) sont recommandés pour la conversation.

---

[Plus de commandes ici](https://github.com/SudoKrondor/EliteIntel/wiki/Obscure-System-Commands)

Volez dangereusement, Commander ! o7

----
Communauté 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈 | Open Source [**GitHub**](https://github.com/SudoKrondor/EliteIntel) | [YouTube](https://www.youtube.com/@SudoKrondor) | [Twitch](https://www.twitch.tv/sudokrondor) | Licence Creative Commons |
