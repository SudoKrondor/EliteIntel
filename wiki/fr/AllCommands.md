# Commandes et requêtes d'Elite Intel

Salut, Commandant ! Voici une référence de ce que vous pouvez demander ou ordonner à **Vega**, l'IA
de votre vaisseau. **Inutile de mémoriser quoi que ce soit** — parlez naturellement et Vega
comprend ce que vous voulez dire. Les phrases ci-dessous sont des exemples, pas des scripts : ce
qui compte, c'est le sens, pas les mots exacts.

> **La liste de référence, toujours à jour, est dans l'application.** L'[onglet Actions →
> Commandes intégrées](UI-Actions-Tab) liste chaque commande et requête de cette version, filtrées
> sur ce que vous pouvez utiliser là où vous êtes, avec les phrases d'entraînement dans votre
> langue. Vous pouvez y lancer n'importe laquelle d'un clic.

---

## Parler à Vega

**Parlez naturellement.** *« Sors le train d'atterrissage »*, *« déploie le train d'atterrissage »*
et *« sors le train »* font la même chose. Plus vous dites clairement ce que vous voulez, plus
c'est fiable.

**L'appeler par son nom est facultatif.** *« Vega, sors le train »* fonctionne exactement comme
*« sors le train »*.

**Veille et réveil.**

- *« Mise en veille »* / *« désactive les commandes vocales »* — Vega n'écoute plus.
- *« Réveille-toi »* — elle écoute de nouveau.
- *« Écoute-moi, … »* — pendant la veille, fait passer **un** ordre sans la réveiller :
  *« Écoute-moi, saute en hyperespace. »*

**Interrompre.** Dites *« arrête de parler »* ou *« interromps »* pour couper Vega en pleine
phrase. Avec le [push-to-talk](UI-Settings-Tab) activé, appuyer sur le bouton suffit.

**Les commandes destructrices demandent confirmation.** Effacer les rappels, les cibles de minage
ou les missions actives, effacer une route commerciale, neutronique ou de porte-vaisseau, supprimer
une entrée du codex, oublier un terrain de chasse, écarter un chantier, exclure un système des
recherches ou définir un nouveau système de base — Vega vous demande de confirmer. Répondez *oui*
pour continuer ; *non*, ou toute autre réponse, annule.

---

## Taper des commandes dans le chat du jeu

La reconnaissance vocale écorchera toujours certains mots — noms de systèmes, de marchandises,
*tritium*. Dans ce cas, **tapez l'ordre dans le chat du jeu**. Commencez la ligne par `@Vega` :

```
@Vega trouve une marchandise tritium
@Vega ajoute une cible de minage painite
@vega, sors le train
```

- La casse n'a pas d'importance, et une virgule ou deux-points après le nom sont acceptés.
- La ligne est traitée exactement comme si vous l'aviez dite, et interrompt Vega si elle parle.
- Utilisez le canal **local**. Seules vos propres lignes envoyées sont lues — rien de ce qu'un autre
  commandant tape ne peut donner d'ordre à Vega.

---

## ⚙️ Application et session

- Veille / réveille-toi / *« écoute-moi, [ordre] »* — voir plus haut.
- *« Arrête de parler »* — stoppe la parole.
- *« Lance un diagnostic »* — Vega se vérifie et fait son rapport.
- *« Quelle heure est-il ? »* — l'heure UTC réelle.
- *« Crée un rappel [texte] »* — une note permanente ; *« quel est le rappel actif ? »* la relit.
- *« Rappelle-moi dans 10 minutes de vérifier le porte-vaisseau »* — un minuteur.
- *« Efface les rappels. »*

### Annonces on/off

- *« Désactive les annonces radar »* / *« active les annonces de contact radar »*
- *« Active / désactive les annonces de découverte »*
- *« Active / désactive les annonces de route »*
- *« Active / désactive les annonces d'approche planétaire »*
- *« Active / désactive les annonces de minage »*
- *« Annonce les récupérations de la trappe »*
- *« Active / désactive la radio »* — transmissions radio
- *« Désactive toutes les annonces »*

Chacune est aussi un interrupteur de l'[onglet Commandant → Annonces](UI-Commander-Tab).

---

## 🎮 Commandes du vaisseau

- **Train d'atterrissage :** *« sors le train »* / *« rentre le train »*
- **Armes :** *« points d'emport »*, *« sors les armes »* / *« range les armes »*
- **Trappe :** *« ouvre / ferme la trappe »*
- **Lumières / vision nocturne :** *« allume les phares »*, *« éteins les phares »*, *« vision
  nocturne »*
- **Mode du HUD :** *« mode combat »* / *« mode analyse »*
- **Défense :** *« lance un dissipateur thermique »*, *« utilise une cellule de bouclier »*,
  *« lance les paillettes »*
- **Groupes de tir :** *« groupe de tir bravo »*, *« sélectionne le groupe de tir 3 »*
- **Puissance :** *« priorité aux boucliers / moteurs / armes »*, *« puissance aux systèmes »*,
  *« équilibre le distributeur »*
- **Vue tête :** *« réinitialise la vue tête »*
- **Activer :** *« active »* — valide ce qui est sélectionné dans le panneau ouvert.

### Poussée

- *« Arrête le vaisseau »* / *« coupe les gaz »*
- *« Quart de poussée »*, *« demi poussée »*, *« trois quarts de poussée »*, *« plein gaz »*
- *« Augmente la vitesse de 2 »* / *« ralentis de 1 »*
- *« Règle la vitesse optimale »* — vitesse d'approche en supercroisière.

### Vol

- *« Décolle »* / *« lance le vaisseau »* — quitter la plateforme.
- *« Demande d'appontage »* / *« demande l'atterrissage »*
- *« Pilotage automatique »* — laisser l'appontage à l'ordinateur.
- *« Entre en super navigation »*
- *« Saute en hyperespace »* / *« lance le saut FSD »* — le saut lui-même.
- *« Sors de la super navigation »*
- *« Cible la prochaine destination »* — sélectionne le système suivant de votre route tracée.
- *« Scanne le système »* / *« scan de découverte »* — déclenche le scanner de découverte (voir le
  réglage par vaisseau dans l'[onglet Commandant](UI-Commander-Tab)).
- *« Ouvre l'analyseur de système »* / *« lance l'analyse complète du système »* — ouvre le FSS et
  scanne.

---

## 🚙 SRV, chasseur et à pied

- *« Déploie le VRS »* — ouvre la bonne baie du hangar (réglez vos baies dans les réglages ⚙ de
  chaque vaisseau, [onglet Commandant](UI-Commander-Tab)).
- *« Déploie le nomad »*
- *« Récupère le VRS »* / *« remonte à bord »* — depuis le SRV.
- *« Active / désactive l'assistance de conduite »*
- *« Je débarque »* / *« sors du vaisseau »*
- *« Renvoie le vaisseau en orbite »*
- *« Viens me chercher »* / *« retour à la surface »* — le rappeler.
- *« Services station »* — le panneau des services, amarré en SRV.

### Ordres au chasseur

- *« Déploie le chasseur »*
- *« Chasseur en défense »* · *« Attaque ma cible »* · *« Chasseur feu à volonté »* · *« Chasseur
  cesse le feu »* · *« Rappelle le chasseur »*

---

## ⚔️ Combat et missions

- **Cibles :** *« cible la menace prioritaire »*, *« sélectionne l'ennemi le plus dangereux »*
- **Sous-systèmes :** *« cible le FSD »*, *« cible les moteurs »*, *« cible la centrale
  électrique »*, et distributeur, survie, bouclier
- **Escadre :** *« cible l'ailier un / deux / trois »* (ou *ailier alpha / bravo / charlie*),
  *« suis l'ailier »*
- *« Missions actives »* / *« liste les missions en cours »* — tout votre tableau.
- *« Navigue vers la mission active »*
- *« Trouve la marchandise de la mission »* — où acheter ce qu'une mission active demande encore,
  avec la route.
- *« Efface toutes les missions actives »*
- *« Total des primes »* — primes accumulées.

### Empiler les massacres de pirates

- *« Trouve des missions de massacre de pirates »*
- *« Navigue vers le fournisseur de missions pirates »*
- *« Navigue vers la cible de mission pirate »*
- *« Combien de kills restants ? »* / *« progression de la mission pirate »*

### Chasse aux primes et zones de conflit

- *« Trouve un terrain de chasse à 100 années-lumière »* — un système avec des sites d'extraction
  de ressources, parmi ceux que vous avez déjà traversés.
- *« Analyse les journaux pour les terrains de chasse »* — apprend d'un coup les systèmes à RES et
  les donneurs de massacres à partir de vos anciens journaux. À faire une fois après l'installation.
- *« Oublie ce terrain de chasse »*
- *« Trouve une zone de conflit »* / *« où est la guerre la plus proche ? »*

Voir [Missions pirates](Pirate-Massacre-Mission-Tracking).

---

## 🧭 Navigation

Vega trace des routes vers **le résultat d'une recherche**, vers des lieux qu'elle connaît déjà ou
vers des coordonnées de surface. Elle ne peut **pas** naviguer vers un système que vous nommez
simplement à voix haute — les noms sont ce que la reconnaissance vocale rate le plus, et une erreur
vous envoie à l'autre bout de la Bulle. Pour cela, utilisez *naviguer depuis la mémoire* ou — pour
un lieu fréquent — une [commande perso qui trace la route pour vous](UI-Actions-Tab).

- *« Navigue depuis la mémoire »* / *« colle depuis la mémoire »* — copiez d'abord un nom de
  système (depuis INARA, Spansh, un message de chat…) avec Ctrl+C ; Vega ouvre la carte galactique
  et trace la route.
- *« Ramène-moi à la base »* / *« définis le système de base »*
- *« Navigue vers le porte-vaisseau »* / *« navigue vers le porte-vaisseau d'escadron »*
- *« Navigue vers le prochain arrêt commercial »*
- *« Annule la navigation »*

### Sur une planète

- *« Navigue vers les coordonnées latitude 12,5 longitude -40,2 »* — guidage de l'orbite jusqu'au
  point.
- *« Navigue vers la zone d'atterrissage »* — retour là où votre vaisseau s'est posé.
- *« Navigue vers le prochain échantillon biologique »* — l'organique marqué le plus proche.
- *« Supprime cette entrée codex »*

### Autoroute neutronique

- *« Calcule l'itinéraire des étoiles à neutrons »* — copiez d'abord le nom de la destination depuis
  la carte galactique. Options : *« …avec efficacité 60 »*, *« …avec suralimentation »*.
- *« Va à la prochaine étoile à neutrons »* — trace vers le prochain point neutronique (ou laissez
  faire le réglage *Tracer le prochain saut neutronique* de l'[onglet Commandant](UI-Commander-Tab)).
- *« Efface la route des étoiles à neutrons »*

### Trouver des lieux

Chaque commande *trouve* trace une route vers ce qu'elle trouve et l'affiche sur
l'[overlay HUD](UI-HUD-Overlay).

- *« Trouve un marchand de matériaux bruts / de données codées / de matériaux manufacturés »*
- *« Trouve un courtier de technologies humaines / gardiennes »*
- *« Trouve Vista Genomics le plus proche »*
- *« Trouve l'Interstellar Factor le plus proche »* / *« où payer mes amendes »*
- *« Trouve une station de ravitaillement »*
- *« Trouve le porte-vaisseau le plus proche »*
- *« Trouve des brain trees dans un rayon de 500 années-lumière »*
- *« Où miner de la painite »* / *« trouve un site de minage »*
- *« Exclure ce système des recherches »* (ou *« le système de destination »*) — quand une recherche
  vous renvoie sans cesse vers un endroit qui ne fonctionne pas. *« Débannis ce système »*, dit
  depuis ce système, annule l'exclusion.

---

## 💰 Commerce et marchés

- *« Trouve une marchandise [nom] »* — fonctionne aussi pour les modules de vaisseau ; précisez *la
  plus proche* ou *le meilleur prix*.
- *« Où puis-je vendre [marchandise] ? »*
- *« Calcule une route commerciale »* — utilise le profil commercial de ce vaisseau.
- *« Monétise la route »* — une cargaison rentable pour le trajet que vous faites déjà.
- *« Route commerciale »* / *« quel est notre plan commercial actuel ? »*
- *« Navigue vers le prochain arrêt commercial »*
- *« Annule la route commerciale »*
- *« Quels sont les marchés locaux »* · *« Détails de la station »* / *« quels services propose
  cette station »* · *« Modules en vente »* · *« Quels vaisseaux sont à vendre »*
- *« Que contient la soute ? »*

### Profil commercial

Modifiable aussi par vaisseau dans les réglages ⚙ de l'[onglet Commandant](UI-Commander-Tab).

- *« Profil commercial »* — décrit le profil actuel.
- *« Change le budget de départ du profil commercial à 5 millions »*
- *« Change le nombre maximum d'arrêts du profil commercial à 4 »*
- *« Change la distance maximum du profil commercial à 1000 »*
- *« Autorise / interdis les marchandises interdites »*
- *« Autorise / interdis les ports planétaires »*
- *« Autorise / interdis les systèmes à permis »*
- *« Autorise / interdis les bastions »*

Voir [Commerce et profit](TradeRoutePlotting) et [Explorer la galaxie](Search-galaxy-with-EliteIntel).

---

## 🏗️ Colonisation

- *« Trouve la marchandise pour le chantier »* — ce dont le chantier a encore besoin, où l'acheter et
  comment remplir la soute.
- *« Avancement du chantier »* / *« comment avance la construction ? »*
- *« Ramène-moi au chantier »*
- *« Oublie le chantier »* — arrêter de le suivre.

---

## 🛰️ Porte-vaisseau

Dites *porte-vaisseau* — ou *porte-vaisseau d'escadron* — sinon Vega peut croire que vous parlez du
vaisseau.

- *« Statut du porte-vaisseau »* — carburant, portée avec le tritium actuel, finances.
- *« Définis la réserve de tritium à 200 »*
- *« Calcule la route du porte-vaisseau »* — copiez d'abord le nom de la destination depuis la carte
  galactique.
- *« Entre la destination du porte-vaisseau »* — carte galactique du porte-vaisseau ouverte, Vega
  saisit l'étape suivante et la confirme.
- *« Quel est l'itinéraire de mon porte-vaisseau »* / *« combien de sauts pour le porte-vaisseau »*
- *« ETA du porte-vaisseau »* / *« quand arrive mon porte-vaisseau ? »*
- *« Distance jusqu'au porte-vaisseau »*
- *« Annule la route du porte-vaisseau »*
- *« Combien de porte-vaisseaux dans le système »*

---

## 🌠 Exploration et exobiologie

- *« Où sommes-nous ? »* — position actuelle.
- *« Distance depuis Sol »* / *« à quelle distance de la Bulle sommes-nous ? »*
- *« Quelle est la distance jusqu'à cette planète »*
- *« Quel a été le dernier scan »*
- *« Quels sont les corps du système »* / *« y a-t-il des planètes atterrissables »*
- *« Quels signaux sont détectés »* · *« Y a-t-il des signaux géologiques »*
- *« Qui contrôle ce système »* / *« niveau de sécurité du système »*
- *« Quelle est la cible FSD actuelle »* — analyse le système vers lequel vous allez sauter.
- *« Route tracée »* / *« carburant au prochain arrêt »*
- *« Combien rapportent les scans d'exploration »*
- *« Quels matériaux trouve-t-on sur cette planète »*
- *« Organiques scannés dans le système »* · *« Liste les échantillons exobiologiques »* · *« Quel
  est le biome de cette planète »*
- *« Distance depuis le dernier échantillon biologique »*
- *« Nous avons déjà prélevé ce corps »* — à marquer comme fait si vous l'avez prélevé avant
  l'installation.

### Exo-Maîtrise

Une fois le catalogue activé dans l'[onglet Commandant](UI-Commander-Tab) :

- *« Emmène-moi au prochain site d'exobiologie »* — le système le plus riche que vous n'avez pas
  encore épuisé.
- *« Marque ce système comme récolté »* — déclarer fait tout un système.

Voir [Découverte et exobiologie](Discovery-Assistance).

---

## ⛏️ Minage

- *« Ajoute une cible de minage painite »* / *« retire une cible de minage painite »* / *« efface
  toutes les cibles de minage »*
- *« Active les annonces de minage »* — touches du prospecteur pour vos cibles.
- *« Où miner [matériau] »*

Avec des cibles définies et une raffinerie montée, l'[overlay HUD](UI-HUD-Overlay) affiche une
carte d'extraction.

---

## 👤 Commandant et vaisseau

- *« Quel est mon profil de commandant »* — grades et progression.
- *« Configuration du vaisseau »* / *« modules du vaisseau »*
- *« Inventaire des matériaux »* / *« combien de [matériau] avons-nous ? »*

---

## 📺 Panneaux et cartes

Dites le nom du panneau, avec *montre* si vous voulez :

- *Navigation* · *Transactions* · *Contacts* · *Chat / communications* · *Boîte de réception* ·
  *Panneau social* · *Historique* · *Escadron* · *Statut* · *Radar*
- *Panneau commandant* · *Équipage* · *Panneau accueil* · *Modules* · *Groupes de tir* ·
  *Inventaire* · *Stockage* · *Panneau chasseur*
- *Gestion du porte-vaisseau*
- *Carte galactique* · *Carte du système*
- *« Panneau suivant / précédent »*, *« page suivante / précédente »* — parcourir les onglets d'un
  panneau.
- *« Ferme le panneau »* / *« quitte l'écran actuel »* — retour au HUD.

---

## 🎵 Musique

*« Jouer la musique »*, *« mettre la musique en pause »*, *« piste suivante »*, *« piste
précédente »*, *« redémarrer la playlist »*, *« mélanger la musique »*, *« mets le morceau
[titre] »*. Voir l'[onglet Jukebox](UI-Jukebox-Tab).

---

## Vos propres commandes

Ce qui manque, vous le construisez vous-même : [Actions → Commandes perso](UI-Actions-Tab). Les
commandes perso se déclenchent avec vos propres phrases, dites ou tapées dans le chat, exactement
comme les commandes intégrées.

---

Fly Dangerous, Commandant ! o7

----
Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈 | Open Source [**GitHub**](https://github.com/SudoKrondor/EliteIntel) | [YouTube](https://www.youtube.com/@SudoKrondor) | [Twitch](https://www.twitch.tv/sudokrondor) | Creative Commons License |
