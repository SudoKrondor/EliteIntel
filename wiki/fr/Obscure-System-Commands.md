# Commandes système en détail

[Toutes les commandes](AllCommands) liste ce que vous pouvez dire. Cette page couvre les commandes
qui demandent un peu plus d'explications : ce qu'il faut faire *avant* de les dire, ce qu'elles
font vraiment, et les pièges.

Tout ceci fonctionne à la voix, tapé dans le chat du jeu sous la forme `@Vega …`, ou d'un clic
dans l'[onglet Actions](UI-Actions-Tab).

---

## Réglages que vous basculez à la voix

Chaque annonce vocale a son interrupteur vocal, et chacun correspond à un interrupteur de
l'[onglet Commandant → Annonces](UI-Commander-Tab), où vous voyez d'un coup d'œil ce qui est actif.

- **Annonces de route** : *« Désactive les annonces de route. »* Coupe tout ce qui est dit autour
  d'un saut — système suivant, trafic, pertes, arrivée, sauts restants, étoile récoltable. Les
  requêtes manuelles fonctionnent toujours.
- **Annonces de découverte** : *« Active les annonces de découverte. »* Premières découvertes,
  corps de valeur, signaux biologiques. Pilote aussi la carte Exobiologie de
  l'[overlay HUD](UI-HUD-Overlay).
- **Approche planétaire** : *« Active les annonces d'approche planétaire. »*
- **Annonces de minage** : *« Active les annonces de minage. »* Les touches du prospecteur pour vos
  cibles. Définissez d'abord des cibles : *« Ajoute une cible de minage painite. »* Sans cible, il
  n'y a rien à annoncer.
- **Récupérations de la trappe** : *« Annonce les récupérations de la trappe. »*
- **Contacts radar** : *« Désactive les annonces radar. »*
- **Radio** : *« Active la radio. »* Le trafic radio du jeu — menaces pirates, contrôle du trafic —
  avec ses propres voix radio. Volume et effets dans [Paramètres → Audio](UI-Settings-Tab).
- **Tout d'un coup** : *« Désactive toutes les annonces. »*
- **Vision nocturne / lumières / assistance de conduite** : *« Active la vision nocturne. »*
  *« Éteins les phares. »* *« Désactive l'assistance de conduite. »*

> **En stream ou en escadre ?** Il n'existe pas de « mode streaming ». Pour que Vega ne réagisse
> pas aux autres voix, mettez-la en veille (*« Mise en veille »*) et faites précéder l'ordre
> occasionnel de *« Écoute-moi, … »* — ou utilisez le [push-to-talk](UI-Settings-Tab), qui ignore
> tout tant que le bouton n'est pas maintenu.

---

## Navigation et recherche de lieux

Vega trace des routes vers le résultat d'une recherche, vers des lieux qu'elle connaît ou vers des
coordonnées de surface. Elle ne trace pas de route vers un système nommé à voix haute — voir
[une commande perso pour les lieux fréquents](UI-Actions-Tab).

- **Navigation vers des coordonnées** : *« Navigue vers les coordonnées latitude 41,43 longitude
  -75,23. »* Guidage de l'orbite jusqu'au point sur le corps actuel ou approché. Côté nuit, vous
  volez aux instruments.
- **Prochain échantillon / entrée codex** : *« Navigue vers le prochain échantillon biologique. »*
  Vous guide vers l'organisme enregistré le plus proche sur cette planète. *« Supprime cette entrée
  codex »* abandonne celle que vous suivez.
- **Zone d'atterrissage** : *« Navigue vers la zone d'atterrissage. »* Retour là où votre vaisseau
  s'est posé.
- **Votre porte-vaisseau** : *« Navigue vers le porte-vaisseau »* / *« navigue vers le
  porte-vaisseau d'escadron. »* Trace vers sa dernière position connue — ou vers votre système de
  base si aucun n'est connu.
- **Base** : *« Définis le système de base »* marque votre position actuelle (avec confirmation) ;
  *« ramène-moi à la base »* trace le retour.
- **Navigation depuis la mémoire** : copiez un nom de système (Ctrl+C) depuis INARA, Spansh ou un
  message de chat, puis dites *« navigue depuis la mémoire. »* Vega ouvre la carte galactique et
  trace la route.
- **Route du porte-vaisseau** : ouvrez la carte galactique, sélectionnez la destination, copiez son
  nom, puis dites *« calcule la route du porte-vaisseau. »* La route vient de Spansh, le système
  doit donc y être connu.
- **Prochaine destination du porte-vaisseau** : ouvrez la carte galactique *du porte-vaisseau* et
  dites *« entre la destination du porte-vaisseau. »* Vega saisit l'étape suivante de la route
  enregistrée et la confirme — à répéter après chaque saut.
- **Route neutronique** : copiez le nom de la destination depuis la carte galactique et dites
  *« calcule l'itinéraire des étoiles à neutrons »* (option *« …avec efficacité 60 »*). Après
  chaque boost, *« va à la prochaine étoile à neutrons »* — ou activez *Tracer le prochain saut
  neutronique lors du boost dans le cône* dans l'[onglet Commandant](UI-Commander-Tab).
- **Marchands et courtiers** : *« Trouve un marchand de matériaux bruts / de données codées / de
  matériaux manufacturés »*, *« trouve un courtier de technologies humaines / gardiennes »*,
  *« trouve Vista Genomics le plus proche »*, *« trouve l'Interstellar Factor le plus proche. »* Vega
  trace la route et laisse un rappel avec la station ; à l'arrivée, demandez *« quel est le rappel
  actif ? »*
- **Brain trees** : *« Trouve des brain trees pour [matériau] dans un rayon de 500 années-lumière. »*
  Trouve un brain tree gardien qui fournit ce matériau brut.
- **Sites de minage** : *« Où miner de l'osmium dans 200 années-lumière ? »* Fonctionne aussi pour
  le tritium.
- **Acheter et vendre** : *« Trouve une marchandise bromellite »* — précisez *la plus proche* ou
  *le meilleur prix* ; marche aussi pour les modules. *« Où puis-je vendre de l'or ? »*
- **Carburant** : *« Trouve une station de ravitaillement. »*
- **Mauvais résultats de recherche** : si une recherche vous renvoie sans cesse vers un endroit qui
  ne fonctionne pas, dites *« exclure ce système des recherches »* (ou *« exclure le système de
  destination des recherches »*). Annulation depuis le système : *« débannis ce système. »*

---

## Combat et missions

- **Apprendre d'abord de votre historique** : *« Analyse les journaux pour les terrains de
  chasse. »* Lit vos journaux de jeu enregistrés et apprend chaque système à sites d'extraction de
  ressources et chaque donneur de massacres pirates que vous avez croisé. À faire une fois après
  l'installation.
- **Terrains de chasse** : *« Trouve un terrain de chasse à 100 années-lumière. »* Un système avec
  des sites d'extraction de ressources, parmi ceux que vous avez traversés. *« Oublie ce terrain de
  chasse »* en retire un.
- **Empiler les massacres** : *« Trouve des missions de massacre de pirates »*, *« navigue vers le
  fournisseur de missions pirates »*, *« navigue vers la cible de mission pirate »*, *« combien de
  kills restants ? »*
- **Zones de conflit** : *« Trouve une zone de conflit. »*
- **Missions** : *« Navigue vers la mission active. »* *« Trouve la marchandise de la mission »*
  cherche où acheter ce qu'une mission active demande encore — celle qui expire le plus tôt et dont
  vous n'avez pas déjà la cargaison.
- **Sous-systèmes** : *« Cible la centrale électrique »* (aussi moteurs, FSD, distributeur, survie,
  bouclier).

---

## Raccourcis de pilotage

- **Distribution de puissance** : *« Priorité aux boucliers »*, *« priorité aux moteurs »*,
  *« équilibre le distributeur. »* Un seul ordre règle tous les pips.
- **Fermer / quitter** : *« Ferme le panneau »* ou *« quitte l'écran actuel »* sort du panneau ou de
  la carte ouverte.
- **Scan** : *« Scanne le système »* déclenche le scanner de découverte sur le groupe de tir défini
  par vaisseau (onglet Commandant → ⚙). *« Ouvre l'analyseur de système »* ouvre le FSS.
- **Vitesse optimale** : *« Règle la vitesse optimale. »* Met les gaz à 75 % — le point idéal en
  supercroisière. Dites-le environ 20 secondes avant la cible pour ne pas tourner autour.
- **Cibler le prochain système de la route** : *« Cible la prochaine destination. »*
- **Wing nav lock** : *« Suis l'ailier. »*
- **Groupes de tir** : *« Groupe de tir bravo »* — lettres OTAN ou chiffres.
- **Renvoyer / rappeler** : *« Renvoie le vaisseau en orbite »* ; *« viens me chercher »* le fait
  revenir.

---

## Utilitaires et session

- **Rappels** : *« Crée un rappel, récupérer la painite à Hutton Orbital. »* Conservé jusqu'à ce
  que vous l'effaciez ; *« quel est le rappel actif ? »* le relit. *« Efface les rappels »*
  demande confirmation.
- **Minuteurs** : *« Rappelle-moi dans 20 minutes de vérifier le porte-vaisseau. »*
- **Monétiser la route** : *« Monétise la route. »* Trouve une paire achat/vente rentable le long
  de la route tracée et l'enregistre comme rappel — du commerce, pas de l'exploration. Elle apparaît
  sur l'[overlay HUD](UI-HUD-Overlay) comme *Opportunité fret*.
- **Autodiagnostic** : *« Lance un diagnostic. »* Teste la connexion au modèle de langage et
  indique s'il répond et à quelle vitesse.
- **Interrompre** : *« Arrête de parler. »* Coupe Vega en pleine phrase. En push-to-talk, appuyer
  sur le bouton fait de même.
- **Analyse du biome** : *« Quel est le biome de cette planète ? »* Indique ce qui pousse
  probablement là avant l'atterrissage.
- **Finances et portée du porte-vaisseau** : *« Statut du porte-vaisseau. »* Carburant, réserve,
  portée de saut, solde et autonomie.
- **Chantiers** : *« Avancement du chantier »*, *« trouve la marchandise pour le chantier »*,
  *« ramène-moi au chantier »*, *« oublie le chantier. »*

---

## Notes d'utilisation

- **Langage naturel** : pas de syntaxe imposée. Dites ce que vous voulez dire.
- **Les noms dans le chat** : les noms de systèmes, de stations et de marchandises sont ce que la
  reconnaissance vocale rate le plus. Tapez ces ordres : `@Vega trouve une marchandise tritium`.
- **Les commandes destructrices demandent confirmation** : effacer rappels, cibles de minage,
  missions ou une route, supprimer une entrée codex, oublier un terrain de chasse, exclure un
  système ou définir une nouvelle base. Répondez *oui* ; tout le reste annule.
- **VR** : la distribution de puissance et fermer/quitter vous évitent de fouiller les menus dans
  le casque.

----
Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
