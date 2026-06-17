# EliteIntel IA – Guide des Requêtes & Commandes

Bienvenue, Commandant ! Voici une référence des choses que vous pouvez demander ou dire à votre assistant **Elite Intel**.
**Idéalement, vous n'avez rien à mémoriser** — parlez naturellement, et l'appli comprend ce que vous voulez. Cette liste existe pour vous montrer ce qui est possible, pas pour réciter des scripts.

## Avant tout : Avez-vous un problème ?

- Si l'appli déclenche des commandes au hasard : problème de cerveau (le LLM est trop faible ou mal configuré).
- Si l'appli déclenche des commandes, mais qu'elles ne s'activent pas : problème de mains (raccourcis clavier).
- Si l'appli vous entend mal : problème d'oreilles (pièce bruyante, ratio bruit/signal trop faible, audio non calibré, etc.).
- Si l'appli ne parle pas : problème de bouche. La connexion audio n'est pas correctement acheminée — vérifie le routage audio au niveau du système d'exploitation.

### [Wiki complet disponible ici](https://github.com/SudoKrondor/EliteIntel/wiki)

## Entrée audio

**Calibrez l'audio dans l'appli**. Si la différence entre le plancher de bruit et le RMS est trop faible (ex. moins de 400), l'appli aura du mal à vous comprendre. Un bon ratio est d'au moins 800–1000. Les haut-parleurs et le micro ne font pas bon ménage — un casque avec micro est recommandé.

## Application

- Vérifie les raccourcis clavier manquants ou non assignés.
- Veille / Réveil (en mode veille, les commandes vocales sont ignorées — dites **« Écoute »** pour passer une commande sans quitter le mode veille, ou **« Réveille-toi »** pour reprendre normalement)

## Exploration & Localisation

- Active / Désactive les annonces de découverte.
- Où sommes-nous / quelle est notre position actuelle ?
- À quelle distance sommes-nous de la Bulle / de notre porte-vaisseau / du dernier échantillon biologique ?
- Quelle est la distance jusqu'à une planète, une lune ou une station précise ?
- Quels matériaux sont disponibles sur cette planète / lune ?
- Analyse le dernier scan / les données du corps céleste.
- Quelle est l'ETA du saut de notre porte-vaisseau ?
- Quelle heure est-il / heure UTC actuelle ? (horloge en temps réel)
- Analyse le biome de ce système stellaire.
- Quelles planètes nécessitent encore des scans biologiques ou organiques ?
- Quels scans biologiques avons-nous effectués ?
- Quels organismes devons-nous encore scanner ?
- Quelles planètes ou lunes sont atterrissables dans ce système ?
- Quels signaux sont détectés dans ce système ?
- Quelles planètes ont des signaux géologiques ?
- Ouvre l'analyseur de système et scanne. / Lance l'analyse spectrale complète.
- Sécurité du système, qui contrôle ce système, contrôle de faction ?
- Quel est votre profil joueur / vos grades / vos statistiques ?

## Exobiologie

- Quels scans biologiques avons-nous effectués ? / Dans le système actuel (complétés, partiels, restants).
- Potentiel de profit d'exploration dans ce système.
- Localisation et distance du dernier échantillon biologique. (nécessite un scan d'entrée codex avec l'analyseur de composition)
- Navigue vers le prochain échantillon biologique / organique / entrée codex. (nécessite un scan d'entrée codex avec l'analyseur de composition)
- Quels organismes ou biologie se trouvent sur cette planète / lune ?
- Navigue vers le prochain organique / échantillon / entrée codex.
- Analyse de biome pour [nom du système / planète / lune].

## Porte-vaisseau

Pensez à mentionner « porte-vaisseau », sinon l'IA pourrait croire que vous parlez de votre vaisseau !

- Quelle est la portée de notre porte-vaisseau (inclut la réserve de carburant si définie manuellement) / statut / carburant ?
- Définis la réserve de tritium du porte-vaisseau à [montant].
- Où saute notre porte-vaisseau ensuite ?
- Combien de temps avant l'arrivée du porte-vaisseau ? (ETA du porte-vaisseau)
- Quel est le statut en carburant / la portée de saut / la réserve de carburant de mon porte-vaisseau ?
- Combien de temps pouvons-nous opérer avec les fonds actuels ?
- Jusqu'où peut sauter le porte-vaisseau avec le tritium actuel ?
- Quelle est la route du porte-vaisseau ? / Combien de sauts restent sur la route du porte-vaisseau ?
- Distance depuis le porte-vaisseau ?
- Y a-t-il des porte-vaisseaux dans ce système ?

## Vaisseau & Systèmes

- Active / Désactive les annonces d'itinéraire.
- La prochaine étoile est-elle scoopable ?
- Analyse la cible FSD (allégeance, trafic, sécurité, données géo/bio, etc.).
- Que contient votre soute / quel est le contenu du cargo ?
- Quelle est votre configuration / quel équipement avons-nous ?
- Analyse notre route. (résumé de la route, disponibilité du carburant)
- Disponibilité du carburant sur la route / prochain arrêt carburant.
- Avons-nous du [matériau] ? / Combien de [matériau] avons-nous ? / Inventaire des matériaux.
- Quel est votre grade / votre profil joueur ?

## Stations & Marchés

- Quels services propose cette station ?
- Équipements / pièces de vaisseau / modules en vente ici ?
- Y a-t-il des vaisseaux à vendre à cette station ?
- Monétise la route (nécessite un profil commercial configuré — voir Configuration du profil commercial)
- Calcule une route commerciale (nécessite un profil commercial configuré — voir Configuration du profil commercial)
- Où devez-vous aller pour acheter/vendre [marchandise] ?
- Quels sont les marchés locaux ?
- Données sur les stations, ports et colonies dans le système.
- Rappelle-moi [texte] (lit les rappels définis pendant une route commerciale, vous indique la station à rejoindre, etc.)
- Quel est notre plan commercial actuel / notre route commerciale / nos étapes commerciales ?

## Configuration du profil commercial

- Change le budget de départ du profil commercial à [montant].
- Change la distance maximum du profil commercial à [X].
- Change le nombre maximum d'arrêts du profil commercial à [N].
- Autorise les marchandises interdites dans le profil commercial [activé/désactivé].
- Autorise les ports planétaires dans le profil commercial [activé/désactivé].
- Autorise les systèmes à permis dans le profil commercial [activé/désactivé].
- Autorise les bastions dans le profil commercial [activé/désactivé].
- Profil commercial / paramètres commerciaux / configuration commerciale. (décrit le profil actuel)
- Liste les paramètres de la route commerciale.

## ⚔️ Combat & Missions

- Annonce contact radar activée/désactivée (active ou désactive les annonces radar)
- Trouve un terrain de chasse (trouvera des paires système cible/fournisseur de missions. Vous dira de voler vers le système cible et confirmera la présence du site d'extraction de ressources)
- Reconnaître le terrain de chasse / navigue vers le terrain de chasse (naviguer vers le système stellaire cible pour le reconnaître)
- Ignore ce terrain de chasse (passer au terrain de chasse candidat suivant)
- Confirme ce terrain de chasse (confirmera manuellement la présence du site d'extraction de ressources, peut être nécessaire si on ne l'a pas détecté depuis la balise ou l'auto-découverte)
- Navigue vers le système fournisseur de missions. / Navigue vers le fournisseur de missions pirates.
- Navigue vers une mission active / trace l'itinéraire vers la mission active. (naviguera vers le système stellaire de la mission active)
- Combien de kills pirates restants ? (calcule le cumul)
- Progression des missions de massacre / kills restants / progression de la chasse aux primes.
- Quelles missions actives avez-vous ? (toutes les missions, pas seulement pirates)
- Total des primes collectées cette session.
- Commandes de ciblage de sous-systèmes : « cible le FSD », « cible les moteurs », « cible le distributeur d'énergie », « cible la centrale électrique », « cible le système de survie ». NOTE : les sous-systèmes optionnels ne sont pas inclus.
- Cible l'ailier un (ailier alpha) / ailier deux (ailier bravo) / ailier trois (ailier charlie).
- Cible la menace prioritaire / sélectionne l'ennemi le plus dangereux / prochain ennemi / sélectionne un hostile.

## 🧭 Commandes de Navigation

- Navigue vers les coordonnées [latitude] [longitude]. (vous guidera depuis l'orbite jusqu'à moins d'1 km, puis une fois atterri à moins de 100 m)
- Navigue vers la zone d'atterrissage. (vous donne le GPS de retour là où votre vaisseau s'est posé la dernière fois)
- Navigue vers le prochain échantillon biologique / entrée codex.
- Trouve le porte-vaisseau le plus proche.
- Navigue vers le porte-vaisseau / direction le porte-vaisseau / retourne au porte-vaisseau.
- Navigue vers / va à / cap sur / direction / emmène-moi à / trace l'itinéraire vers [destination].
- Navigue vers le prochain arrêt commercial / va au prochain point de commerce.
- Calcule la route du porte-vaisseau. *(Ouvre d'abord la carte galactique, sélectionne une étoile et copie son nom dans le presse-papiers.)*
- Entre la destination du porte-vaisseau / définis la destination du porte-vaisseau. *(Ouvre d'abord la carte galactique du porte-vaisseau.)*
- Calcule une route commerciale / trace une route commerciale rentable.
- Trouve le Vista Genomics le plus proche.
- Trouve le courtier de technologies humaines / gardien le plus proche.
- Trouve le marchand de matériaux bruts / codés / manufacturés le plus proche.
- Trouve / localise / cherche des arbres cérébraux [dans un rayon de X années-lumière].
- Trouve un site de minage pour [matériau] [dans un rayon de X années-lumière].
- Trouve où on peut miner du tritium [dans un rayon de X années-lumière].
- Trouve où on peut acheter [marchandise] [dans un rayon de X années-lumière].
- Trouve un terrain de chasse.
- Annule la navigation.
- Trace l'itinéraire vers le porte-vaisseau.
- Ramène-moi à la base / navigue vers le système de base.
- Définis le système actuel comme base.
- Règle la vitesse optimale. *(Règle les gaz à 75 % pour l'approche en super navigation.)*
- Augmente / Réduis la vitesse de [montant]. 1–10
- Définis la réserve de tritium du porte-vaisseau à [montant].
- Sélectionne la destination FSD / cible la prochaine destination. (sélectionne le prochain système dans la route tracée)
- Verrouille la navigation sur l'ailier / lock navigation escadre.
- Sélectionne / cible la menace prioritaire / prochain hostile / cible un ennemi.
- Navigue depuis la mémoire. (Ouvre la carte galactique et navigue vers l'adresse copiée en mémoire via Ctrl+C.)

## 🎮 Commandes du Vaisseau

- Train d'atterrissage sorti / rentré | déploie / rentre le train d'atterrissage.
- Déploie / rentre les points d'emport / les armes.
- Armes au clair ! (identique à déployer les points d'emport)
- Déploie un dissipateur thermique.
- Déploie / récupère le VRS / véhicule / buggy.
- Remonte à bord / récupère le VRS / dock le VRS. (récupère le VRS en mode VRS)
- Débarque. (quitte le vaisseau à pied)
- Ouvre / ferme la trappe à cargaison / soute.
- Demande l'autorisation d'appontage. / Contacte la tour, obtiens un quai d'atterrissage, etc.
- Lance le vaisseau / quitte la station / décolle.
- Mode taxi / pilote automatique / atterrissage automatique.
- Entre en super navigation / active la super navigation.
- Saute en hyperespace / lance le saut FSD / allons-y / prochain waypoint. (saut effectif, pas la super navigation)
- Sors de la super navigation / quitte la super navigation / drop.
- Arrêt complet / coupe les gaz / vitesse zéro / halte.
- Vitesse à : quart de poussée / demi poussée / trois quarts / pleine poussée.
- Augmente / Réduis la vitesse de [montant].
- Toute la puissance dans les boucliers / moteurs / armes. Équilibre le distributeur. / Max boucliers, max moteurs, max armes.
- Mode combat / passe en mode combat → bascule en HUD combat.
- Mode analyse / passe en mode analyse / mode explorateur → bascule en HUD analyse.
- Vision nocturne (activée / désactivée).
- Phares / lumières (activés / désactivés).
- Assistance de conduite activée / désactivée. (mode VRS)
- Renvoie le vaisseau en orbite / envoie le vaisseau se mettre en orbite.
- Retour à la surface / viens me chercher.
- Ouvre l'analyseur de système / honk / scanne le système / lance un scan / scan de découverte / scan système / analyse spectrale complète. (Ouvre l'interface FSS complète et honk)
- Affiche / ouvre - carte galactique / carte système / ferme la carte.
- Ferme / quitte (Quitte les menus / onglets / sous-menus et revient au HUD).
- Interromps / tais-toi / silence / arrête de parler [stoppe la synthèse vocale en cours].
- Active (active ce qui est sélectionné dans l'interface).

## 🎙️ Commandes Chasseur

- Déploie le chasseur / lance le chasseur / sors le chasseur.
- Chasseur défends le vaisseau.
- Chasseur attaque ma cible / concentre-toi sur ma cible / feu à volonté / ordres ouverts chasseur.
- Chasseur cesse le feu / hold fire chasseur.
- Chasseur rentre au vaisseau / rappelle le chasseur / chasseur dock.

## 📺 Panneaux UI

Dis **montre**, **ouvre** ou **affiche** suivi du nom du panneau :

- Panneau de navigation
- Panneau de transactions
- Panneau de contacts
- Panneau de chat / communications
- Panneau boîte de réception
- Panneau social
- Panneau historique
- Panneau escadron
- Panneau de statut
- Panneau commandant / panneau central / tableau de bord
- Panneau équipage
- Panneau accueil (panneau interne)
- Panneau modules
- Groupes de tir
- Panneau inventaire
- Panneau stockage
- Panneau chasseur
- Panneau de gestion du porte-vaisseau
- Carte galactique
- Carte locale / système
- Panneau services (VRS amarré à une station)

Autres commandes de panneaux :
- Ferme / quitte le panneau (quitte les menus et retourne au HUD).

## ⚙️ Commandes Appli & Session

- **« Dors » / « Passe en mode veille » / « Tu peux disposer »** → met l'appli en veille (ignore toutes les entrées).
- **« Réveille-toi »** → reprend l'écoute normale.
- **« Écoute-moi [commande] »** → contournement : passe une seule commande ou requête pendant que l'appli reste en veille. Le préfixe « Écoute-moi » est retiré avant que la commande atteigne l'IA. Exemple : *« Écoute-moi, saute en hyperespace »* exécute le saut sans réveiller l'appli.
- Définis un rappel [texte]. / Rappelle-moi [texte].
- Efface les rappels.
- Active / Désactive les annonces d'itinéraire.
- Active / Désactive les annonces de découverte.
- Active / Désactive les annonces de minage et de matériaux.
- Active / Désactive la radio / le trafic radio.
- Désactive toutes les annonces.
- Annonce contact radar activée / désactivée.
- Ajoute une cible de minage [nom du matériau].
- Retire une cible de minage [nom du matériau].
- Efface toutes les cibles de minage.
- Supprime l'entrée codex. (Supprime l'entrée codex que vous suivez en GPS)

## 💬 Chat Libre (le Mode Conversation doit être ACTIVÉ)

Par défaut, l'appli fonctionne en **Mode Strict** : si l'entrée ne correspond pas à une commande ou requête connue, elle est silencieusement ignorée.
C'est intentionnel — cela évite que le bruit de la reconnaissance vocale et les conversations de fond ne déclenchent des actions aléatoires en plein vol.

Activez le **Mode Conversation** dans l'onglet Paramètres pour activer le chat libre. Une fois activé, tout ce qui ne correspond pas à une commande bascule en conversation générale — lore du jeu, sujets du monde réel, configurations de vaisseaux, peu importe. L'IA n'est pas uniquement un analyseur de commandes quand vous le souhaitez.

Les LLM locaux répondront mais seront rigides. Les LLM cloud (Claude, OpenAI, xAI, Mistral, Deepseek) sont recommandés pour la conversation.

---

[Plus de commandes ici](https://github.com/SudoKrondor/EliteIntel/wiki/Obscure-System-Commands)

Volez Dangereusement, Commandant ! o7

---

Communauté 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈 | Open Source [**GitHub**](https://github.com/SudoKrondor/EliteIntel) | [YouTube](https://www.youtube.com/@SudoKrondor) | [Twitch](https://www.twitch.tv/sudokrondor) | Licence Creative Commons |