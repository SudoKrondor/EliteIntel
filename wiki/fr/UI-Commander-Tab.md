# Onglet Commandant

<img src="images/controller.png" class="inline" height="20" alt="Commandant"> Qui vous êtes, avec
quelle voix parle chaque coque de votre flotte, ce que votre vaisseau fait automatiquement pour
vous, ce que Vega vous dit sans qu'on le lui demande, et le catalogue Exo-Maîtrise.

![Onglet Commandant](images/ui-tab-commander.png)

La bande **Profil commandant** est en haut ; en dessous se trouvent quatre sous-onglets :
**Gestion de la flotte**, **Réglages globaux du vaisseau**, **Annonces** et **Exo-Maîtrise**.

---

## Profil commandant

**Nom du commandant** — le nom que Vega utilise de temps en temps pour vous. Utilisez-le si Vega
écorche votre nom en jeu, ou si vous préférez simplement qu'on vous appelle autrement. Enregistré
avec Entrée ou en cliquant ailleurs.

**M'interpeller** — désactivé, Vega ne s'adresse jamais à vous : ni nom, ni grade, ni titre.

> Le **dossier du journal** se trouve dans [Paramètres → Général](UI-Settings-Tab), et le
> **dossier des raccourcis** dans l'[onglet Bindings](UI-Bindings-Tab).

---

## Gestion de la flotte

Une ligne par vaisseau que vous possédez, suivie de votre **vaisseau-mère** et de votre
**vaisseau-mère d'escadron** si vous en avez. Elite Intel découvre votre flotte dans le journal du
jeu ; vous n'ajoutez jamais de vaisseau à la main. Si vous jouez plusieurs commandants sur un même
PC, la liste suit le commandant chargé dans le jeu.

| Colonne | Remarques |
|--------|-------|
| **Vaisseau** | Le nom de votre vaisseau (pour un vaisseau-mère, son nom et son indicatif) |
| **Modèle de vaisseau** | Le type de coque, ou *Vaisseau-mère* / *Vaisseau-mère d'escadron* |
| **Voix** | Cliquez pour choisir. Un changement joue aussitôt une phrase avec cette voix pour l'écouter |
| **Personnalité** | `Professionnel` · `Décontracté` · `Amical` · `Instable` · `Seven of Nine` · `Mercenaire grande gueule` · `Ton ex-copain` · `Ton ex-copine` · `Fripon` |
| **⚙** | Ouvre les réglages de ce vaisseau (voir plus bas) |

**À propos de la liste des voix.** Toutes les voix du moteur choisi dans
[Paramètres → Services IA](UI-Settings-Tab) sont proposées, masculines comme féminines — la voix
choisie détermine aussi si Vega parle d'elle-même au masculin ou au féminin sur ce vaisseau.

- **Kokoro** et **Supertonic 3** (locales) — libellées `Nom - accent`.
- **Google** (cloud) — libellées `Nom - accent · HD` ou `· Standard`. En anglais, l'accent
  distingue les voix. Dans toute autre langue, chaque voix est synthétisée dans cette langue, donc
  le libellé indique le genre et le niveau de qualité plutôt qu'un accent anglais trompeur.
- **Microsoft Edge** (cloud) — libellées `Nom - accent`.

> Changer de moteur de voix remet la voix de chaque vaisseau à celle par défaut du nouveau moteur.
> Les **personnalités sont conservées**. L'application demande avant de le faire.

**Les lignes de vaisseau-mère** n'ont qu'une voix — un vaisseau-mère n'est pas équipé de Vega, il
n'a donc ni personnalité ni réglages. La voix est celle avec laquelle son **contrôle du trafic**
répond à la radio. Laissez-la sur **Aléatoire** et un contrôleur différent répond à chaque fois ;
choisissez-en une et l'écoute est jouée comme une transmission radio, car c'est la seule façon
dont vous l'entendrez.

---

## Réglages du vaisseau (le bouton ⚙)

Des réglages par vaisseau, car un Python de minage et une Corvette de combat ne veulent pas le
même comportement. Les modifications sont enregistrées en fermant la fenêtre avec **Retour**.

![Réglages du vaisseau](images/ui-ship-settings.png)

**Scanner le système à l'entrée** — lance un scan de découverte à l'arrivée dans un système.
Choisissez le **Groupe de tir** (A–H) et le **Déclencheur** (1 ou 2) sur lesquels votre scanner de
découverte est monté. Si votre HUD est en mode combat, Elite Intel passe en mode analyse, scanne et
revient.

**Baies de véhicules** — ce que vous rangez dans chaque baie du hangar à véhicules (**Baie 1–4** :
*Vide*, *Scarab*, *Scorpion* ou *Rhino*). Le journal du jeu nomme le hangar mais jamais son
contenu ; c'est ainsi que *« déploie le VRS »* ouvre la bonne baie — et sait si le vaisseau doit
d'abord se poser (Scarab, Scorpion) ou peut rester en vol stationnaire (Rhino). Les quatre baies
sont toujours affichées, quel que soit le hangar monté, pour que vos choix survivent à un
changement d'équipement.

**Alerte matériaux sur émissions haute qualité** — vous prévient quand un signal d'émissions de
haute qualité dans le système contient des matériaux qui valent un arrêt.

**Profil commercial** — les contraintes qu'Elite Intel respecte quand il trace une route
commerciale pour ce vaisseau. Chacune peut aussi se régler à la voix :
*« change le nombre maximum d'arrêts du profil commercial à quatre »*.

| Réglage | Signification |
|---------|---------|
| **Autoriser les ports planétaires** | Inclure les ports de surface dans les routes |
| **Autoriser les marchandises prohibées** | Inclure des marchandises illégales quelque part sur la route |
| **Autoriser les systèmes à permis** | Inclure les systèmes qui exigent un permis |
| **Autoriser les transporteurs de flotte** | Inclure les vaisseaux-mères des joueurs comme marchés |
| **Autoriser les systèmes forteresse** | Inclure les systèmes bastions thargoïdes / de puissance |
| **Dist. max à l'arrivée (Ls)** | À quelle distance de l'étoile d'arrivée une station peut se trouver |
| **Arrêts max.** | Nombre d'étapes de la route |
| **Capital de départ** | Crédits que le planificateur peut dépenser |

Comment les routes se volent : [Commerce et profit](TradeRoutePlotting).

---

## Réglages globaux du vaisseau

Des automatisations que Vega effectue pour vous, sur tous les vaisseaux. Chacune est un simple
interrupteur enregistré immédiatement. Utiles à tous, et une vraie aide pour les commandants en
situation de handicap.

| Interrupteur | Ce qu'il fait |
|--------|--------------|
| **Accélérer automatiquement pour le FTL** | Met les gaz avant un saut |
| **Éteindre les lumières pour le FTL** | Éteint les lumières du vaisseau avant un saut |
| **Éteindre la vision nocturne pour le FTL** | Coupe la vision nocturne avant un saut |
| **Rentrer les points d'emport pour le FTL** | Rentre les armes avant un saut |
| **Rentrer le train d'atterrissage pour le FTL** | Rentre le train avant un saut |
| **Rentrer le cargo scoop pour le FTL** | Rentre la trappe avant un saut |
| **Rentrer le train au décollage** | Rentre le train après le décollage |
| **Éteindre les lumières au déploiement du SRV** | Éteint les lumières quand vous déployez le SRV |
| **Tracer le prochain saut neutronique lors du boost dans le cône** | Sur une [route neutronique](AllCommands), trace le prochain point neutronique dès que vous avez traversé le cône en boost |

---

## Annonces

Tout ce que Vega dit sans qu'on le lui demande, au même endroit — un seul écran à vérifier quand
quelque chose parle trop, ou pas assez.

![Annonces](images/ui-commander-announcements.png)

| Interrupteur | Ce que vous entendez |
|--------|---------------|
| **Annoncer les découvertes** | Corps notables, premières découvertes, signaux biologiques. Pilote aussi la carte Exobiologie de l'[overlay HUD](UI-HUD-Overlay) |
| **Annoncer l'approche planétaire** | Des faits sur le corps dont vous vous approchez |
| **Annoncer les contacts radar** | Les vaisseaux qui apparaissent au scanner |
| **Annoncer le minage** | Les touches du prospecteur et les trouvailles pour vos cibles de minage |
| **Annoncer les récupérations de la trappe** | Ce que vous venez de ramasser |
| **Annoncer la navigation** | Événements de navigation et arrivées |
| **Transmissions radio** | Les échanges radio du jeu, avec des voix radio distinctes |
| **Annonces de route** | Interrupteur principal de tout ce qui est dit autour d'un saut. Les interrupteurs en dessous ne fonctionnent que s'il est activé |
| &nbsp;&nbsp;↳ **Annoncer la destination du saut** | Quel est le prochain système |
| &nbsp;&nbsp;↳ **Annoncer le trafic de la destination** | Rapports de trafic de votre destination |
| &nbsp;&nbsp;↳ **Annoncer les pertes de la destination** | Décès récents dans le système de destination |
| &nbsp;&nbsp;↳ **Annoncer l'arrivée** | Une phrase à l'arrivée |
| &nbsp;&nbsp;↳ **Annoncer les sauts restants** | Sauts restants sur la route |
| &nbsp;&nbsp;&nbsp;&nbsp;↳ **Annoncer la disponibilité d'étoile à carburant** | Si la destination a une étoile récoltable — dit dans la phrase des sauts restants |

La plupart se basculent aussi à la voix (*« désactive les annonces radar »*, *« désactive toutes
les annonces »*), c'est pourquoi la page les relit à chaque ouverture de l'onglet.

---

## Exo-Maîtrise

Un catalogue de systèmes stellaires proches de la Bulle dont les planètes abritent une exobiologie
de grande valeur, constitué par la communauté via Spansh.

1. Cliquez sur **Activer Exo-Maîtrise**. Le catalogue est téléchargé puis importé, chaque moitié
   avec sa barre de progression.
2. Une fois chargé, la page affiche les **Systèmes stellaires**, les **Planètes et lunes**, la
   **Valeur prévue** de tout le catalogue et ce que vous avez **Récolté**.
3. En vol, dites *« emmène-moi au prochain site d'exobiologie »* et Vega trace une route vers le
   système le plus riche que vous n'avez pas encore épuisé.

Les planètes terminées sont cochées au fil des scans. Ce que vous avez prélevé avant d'installer
Elite Intel peut être déclaré à la voix — *« nous avons déjà prélevé ce corps »*, ou pour tout un
système, *« marque ce système comme récolté »*.

**Désactiver Exo-Maîtrise** supprime le catalogue de votre ordinateur, après confirmation. Les
planètes déjà prélevées restent enregistrées, donc le réactiver plus tard ne vous y renverra pas.

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
