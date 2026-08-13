# Onglet Commandant

<img src="images/controller.png" class="inline" height="20" alt="Commandant"> Qui vous êtes, ce que
votre vaisseau fait pour vous automatiquement, ce dont Vega vous informe sans qu'on le lui demande,
et avec quelle voix parle chaque coque de votre flotte.

![Onglet Commandant](images/ui-tab-commander.png)

---

## Profil du commandant

**Nom du commandant** — remplace votre nom en jeu pour la synthèse vocale. À utiliser si Vega
écorche votre pseudo, ou si vous voulez simplement qu'on vous appelle autrement. Enregistré quand
vous appuyez sur Entrée ou cliquez ailleurs.

> Le **dossier du journal** a été déplacé vers [Paramètres → Commun](UI-Settings-Tab) en V1.1, et le
> **dossier des assignations** vers l'[onglet Bindings](UI-Bindings-Tab).

---

## Options de vaisseau

Des automatisations que Vega exécute à votre place. Chacune est un simple interrupteur écrit
immédiatement. Utile pour tout le monde, et véritablement libérateur pour les commandants en
situation de handicap.

| Interrupteur | Ce qu'il fait |
|--------|--------------|
| **Accélérer automatiquement pour le FTL** | Met les gaz avant un saut |
| **Éteindre les lumières pour le FTL** | Coupe les feux du vaisseau avant un saut |
| **Éteindre la vision nocturne pour le FTL** | Désactive la vision nocturne avant un saut |
| **Rentrer les points d'emport pour le FTL** | Rentre les points d'emport avant un saut |
| **Rentrer le train d'atterrissage pour le FTL** | Rentre le train avant un saut |
| **Rentrer le cargo scoop pour le FTL** | Rentre le collecteur avant un saut |
| **Rentrer le train au décollage** | Rentre le train après le décollage |
| **Quitter l'interface avant d'ouvrir un autre panneau** | Ferme le panneau ouvert avant d'ouvrir le suivant, pour que les commandes de panneau n'entrent pas en collision |
| **Éteindre les lumières au déploiement du SRV** | Coupe les feux quand vous déployez le SRV |
| **Demander l'amarrage du chasseur au FTL / annuler si hors portée** | *Actuellement désactivé* — en attente d'un correctif Frontier pour un bug lié au Nomad |

---

## Annonces

Tout ce que Vega dit spontanément. Les onze interrupteurs sont désormais au même endroit : il n'y a
donc qu'un seul écran à vérifier quand quelque chose parle trop — ou pas assez.

![Annonces](images/ui-commander-announcements.png)

| Interrupteur | Ce que vous entendez |
|--------|---------------|
| **Annoncer les découvertes** | Corps notables, premières découvertes, signaux biologiques |
| **Annoncer la progression de la route** | Où vous en êtes sur un itinéraire tracé |
| **Annoncer les contacts radar** | Vaisseaux apparaissant au scanner |
| **Annoncer le minage** | Événements de minage et rendements |
| **Annoncer la navigation** | Événements de navigation et arrivées |
| **Transmissions radio** | Échanges radio dans le personnage, énoncés avec une voix radio distincte |
| **Annoncer la destination du saut** | Quel est le système suivant |
| **Annoncer le trafic de la destination** | Rapports de trafic pour votre destination |
| **Annoncer les pertes de la destination** | Morts récentes dans le système de destination |
| **Annoncer les sauts restants** | Sauts restants sur la route |
| **Annoncer la disponibilité d'étoile à carburant** | Si la destination possède une étoile récoltable |

Les six premières peuvent aussi être basculées à la voix : cet écran les relit donc à chaque
ouverture de l'onglet — un `toggle all announcements` prononcé y sera reflété.

---

## Configuration des voix de la flotte

Une ligne par vaisseau que vous possédez. Elite Intel découvre votre flotte à partir du journal du
jeu ; vous n'ajoutez pas de vaisseaux à la main.

| Colonne | Remarques |
|--------|-------|
| **Vaisseau** | Le nom que vous avez donné à votre vaisseau |
| **Modèle de vaisseau** | Le type de coque |
| **Voix** | Cliquez pour choisir. La modifier joue aussitôt une réplique de démonstration avec cette voix, pour que vous puissiez l'auditionner |
| **Personnalité** | `Professionnel` · `Décontracté` · `Amical` · `Instable` · `Fripon` |
| **⚙** | Ouvre les réglages de ce vaisseau |

**À propos de la liste des voix.** Les voix de vaisseau sont féminines. Les voix proposées dépendent
du moteur vocal sélectionné dans [Paramètres → Services IA](UI-Settings-Tab) :

- **Local (Kokoro)** — 53 voix, étiquetées `Nom - accent`. Pas de clé, pas de téléchargement, pas de
  configuration.
- **Cloud (Google)** — étiquetées `Nom - accent · HD` ou `· Standard`. En anglais, l'accent
  distingue les voix. Dans toutes les autres langues, chaque voix est synthétisée dans cette langue :
  l'étiquette affiche donc le genre et le niveau de qualité plutôt qu'un accent anglais trompeur.

> Changer de moteur vocal réinitialise la voix de chaque vaisseau à la valeur par défaut du nouveau
> moteur. Les **personnalités de vos vaisseaux sont conservées**. L'application vous prévient avant.

---

## Réglages de vaisseau (le bouton ⚙)

Des réglages par vaisseau, parce qu'un Python de minage et une Corvette de combat n'attendent pas le
même comportement.

![Réglages de vaisseau](images/ui-ship-settings.png)

**Scanner le système à l'entrée** — effectue un scan de découverte à votre arrivée dans un système.
Choisissez le **Groupe de tir** (A–H) et le **Déclencheur** (1 ou 2) sur lequel votre scanner de
découverte est monté. Si votre HUD est en mode Combat, Elite Intel bascule en Analyse, scanne, puis
rebascule.

**Alerte matériaux sur émissions haute qualité** — vous signale lorsqu'un signal d'émissions haute
qualité du système transporte des matériaux qui valent l'arrêt.

**Profil commercial** — les contraintes qu'Elite Intel respecte lorsqu'il trace une route commerciale
pour ce vaisseau. Chacune peut aussi être définie à la voix :
*« alter trade profile, set max stops to four »*.

| Réglage | Signification |
|---------|---------|
| **Autoriser les ports planétaires** | Inclure les ports de surface dans les routes |
| **Autoriser les marchandises prohibées** | Inclure des marchandises illégales quelque part sur la route |
| **Autoriser les systèmes à permis** | Inclure les systèmes nécessitant un permis |
| **Autoriser les transporteurs de flotte** | Inclure les transporteurs de flotte de joueurs comme marchés |
| **Autoriser les systèmes forteresse** | Inclure les systèmes forteresse thargoïdes ou de puissance |
| **Dist. max à l'arrivée (Ls)** | À quelle distance de l'étoile d'arrivée une station peut se trouver |
| **Arrêts max.** | Nombre d'étapes de la route |
| **Capital de départ** | Crédits que le planificateur de route peut dépenser |

Voir [Commerce et profit](TradeRoutePlotting) pour la manière dont les routes se volent.

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
