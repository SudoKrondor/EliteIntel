# BindForge — l'onglet BindForge

<img src="images/keyboard.png" class="inline" height="20" alt="BindForge"> **BindForge** modifie et
protège vos commandes Elite Dangerous depuis l'extérieur du jeu : clavier, souris, joystick, HOTAS et
palonniers. Il s'occupe de tous les fichiers qui se trouvent derrière : vos fichiers `.binds`, le fichier de
preset qui indique lequel est chargé, ainsi que les fichiers `DeviceMappings.xml` et `.buttonMap` qui nomment
vos contrôleurs et libellent leurs boutons.

Il a son propre onglet, **BindForge**, avec quatre sous-onglets et une barre au-dessus d'eux :

| Partie | À quoi elle sert |
|--------|------------------|
| **[Barre Appliquer](#the-apply-bar)** | Votre dossier de configuration utilisateur, et le seul **Appliquer** et **Abandonner** pour tout cet onglet |
| **[Éditeur de raccourcis](#binding-profile)** | Toutes les commandes du jeu, par section, avec les vues **Anomalies** et **Zones mortes et sensibilité** |
| **[Éditeur de presets](#preset-editor)** | Quel fichier de raccourcis le jeu charge pour les commandes Général, Vaisseau, SRV et À pied |
| **[Gestion des raccourcis](#binding-management)** | Vos installations du jeu, vos fichiers de raccourcis et vos sauvegardes |
| **[Concepteur d'alias](#alias-designer)** | Donnez de vrais noms à vos contrôleurs et libellez leurs boutons, axes et chapeaux |

> **Vega n'appuie toujours que sur des touches du clavier.** Vous pouvez assigner n'importe quel périphérique
> à n'importe quel emplacement, mais une commande a besoin d'une touche clavier pour que Vega puisse
> l'utiliser. La colonne **Accès IA** vous montre lesquelles en ont une.

---

## À la première ouverture

Avant de changer quoi que ce soit, BindForge se configure une fois. **Tout est d'abord sauvegardé, et cette
sauvegarde est conservée pour toujours.**

1. **Confirmez vos installations.** BindForge liste chaque copie d'Elite Dangerous trouvée sur ce PC.
   Corrigez la liste avec **Ajouter une manquante** ou **Supprimer**, puis cliquez sur **Confirmer**.
2. **Rapprochez, si besoin.** Si vos installations contiennent des fichiers de périphérique différents,
   utilisez ceux d'une installation pour toutes, ou cliquez sur **Fusionner** et répondez à chaque ligne
   marquée *choisir*.
3. **Créez votre copie de travail.** BindForge fait sa propre copie de vos fichiers de commandes. C'est cette
   copie que vous modifiez, et les fichiers du jeu restent intacts jusqu'à ce que vous cliquiez sur **Appliquer**.
4. **Nommez vos contrôleurs.** Chaque contrôleur connecté sans nom se voit proposer
   [un nom](#naming-a-controller). Vous pouvez **Passer** cette étape.

Tant que ce n'est pas terminé, la configuration initiale est reproposée à chaque ouverture de l'onglet.

### Nouveau dans Elite Dangerous ?

Un tout nouveau commandant n'a aucun fichier de commandes, et Vega vous le dit. Terminez votre entraînement
de commandant, puis dans *Options → Commandes* du jeu, réglez le déplacement de l'interface de la carte de la
galaxie sur les touches fléchées et appliquez. Cela crée les fichiers, et Vega peut alors les lire.

Si vous avez les fichiers du jeu mais aucun fichier de raccourcis à vous, un bouton **Créer** apparaît sur la
barre Appliquer et les onglets d'édition restent grisés. Choisissez dans **À partir de** l'un des presets de
Frontier, donnez un **Nom** au vôtre, puis cliquez sur **Appliquer** pour l'écrire dans le jeu. Si vous revenez
au jeu, restaurez plutôt une sauvegarde depuis [Sauvegardes du joueur](#player-backups).

---

<a id="the-apply-bar"></a>

## La barre Appliquer

La barre au-dessus des sous-onglets contient le **Dossier de configuration utilisateur** et les boutons
**Abandonner** et **Appliquer**.

**Dossier de configuration utilisateur** est l'endroit où se trouvent vos fichiers `.binds` et le fichier de
preset, et toutes les installations le partagent. Il est détecté automatiquement. Utilisez le sélecteur **⋮** si
le vôtre est dans un endroit inhabituel.

> **« Raccourcis introuvables » avec le bon dossier ?** Elite Dangerous n'écrit un fichier `.binds` qu'une fois
> que vous avez personnalisé quelque chose. Ouvrez *Options → Commandes* dans le jeu, modifiez n'importe quel
> raccourci, et Elite Intel détecte le fichier.

Rien de ce que vous faites dans ces onglets ne touche le jeu avant que vous cliquiez sur **Appliquer**. Vos
modifications s'accumulent, et **Appliquer** et **Abandonner** s'allument dès qu'il y en a.

| Bouton | Ce qu'il fait |
|--------|---------------|
| **Appliquer** | Sauvegarde tout, écrit vos modifications dans les fichiers du jeu et indique ce qui a été écrit pour chaque installation |
| **Abandonner** | Jette toutes les modifications non appliquées. Les fichiers du jeu ne sont pas touchés |

> **Fermez l'écran Commandes du jeu avant d'appliquer.** Les modifications faites en même temps dans BindForge
> et dans le jeu peuvent être perdues. Pour que le jeu charge un fichier de raccourcis modifié, ouvrez puis
> fermez son écran Commandes après avoir appliqué. Vega vous le rappelle.

Si vous quittez l'onglet ou fermez l'application avec des modifications non appliquées, on vous demande de
choisir entre **Appliquer**, **Abandonner** et **Revenir**.

<a id="read-only"></a>

### Lecture seule tant qu'une installation manque

Si l'une de vos installations du jeu est introuvable (en général parce que son disque est débranché),
BindForge passe en **lecture seule**. La barre indique quelle installation manque, Vega vous le dit au
démarrage, et rien dans l'onglet ne peut être modifié, pour que cette installation ne se désynchronise pas
des autres.

Rebranchez le disque et redémarrez Elite Intel, ou **Supprimez** l'installation dans
[Emplacements du jeu](#game-install-locations) si elle a disparu pour de bon. **Outrepasser**, sur la barre,
réactive la modification jusqu'à la fermeture d'Elite Intel : Appliquer écrit alors dans les installations
présentes, et quand celle qui manque revient, elle est comparée au démarrage suivant et vous choisissez ce que
vous gardez.

### Quand le jeu a modifié vos fichiers

Le jeu peut modifier ses propres fichiers, et une mise à jour peut les réinitialiser. BindForge les compare à
sa copie de travail à la première ouverture de l'onglet dans chaque session, puis à chaque Appliquer. S'ils
diffèrent, Vega vous le dit au démarrage, et l'onglet liste ce qui a changé et vous laisse choisir :

| Choix | Ce qui se passe |
|-------|-----------------|
| **Remettre les miens** | Votre copie de travail retourne dans le jeu. Souvent le bon choix quand un fichier semble réinitialisé par une mise à jour |
| **Fusionner...** | Chaque ligne différente devient une rangée. Cliquez sur la valeur à conserver, la vôtre ou celle du jeu |
| **Utiliser les fichiers du jeu** | Les fichiers du jeu remplacent votre copie de travail |

Tout est sauvegardé avant chacune de ces écritures, et vous pouvez fermer l'avertissement pour décider plus
tard. Le fermer pendant un Appliquer l'annule et conserve vos modifications.

**Une nouvelle version de votre fichier de raccourcis.** Les mises à jour enregistrent parfois votre fichier
sous un nouveau nom avec de nouvelles commandes. BindForge n'écrit jamais votre ancien fichier par-dessus. Si
tous vos raccourcis ont été repris, il suffit de cliquer sur **Approuver**. Si certains diffèrent,
**Rapprocher...** garde les nouvelles commandes et vous interroge sur chaque ligne différente.

**Des fichiers qui n'ont rien à faire là.** Si le dossier de raccourcis du jeu contient des fichiers que le jeu
n'utilise pas (un fichier de raccourcis copié à la main sans version dans son nom, par exemple, ou un fichier
étranger), Vega le signale et la vérification propose **Ranger**. Cela les déplace dans **Leftover User
Files**, dans le dossier propre d'Elite Intel. Rien n'est supprimé, et vous pouvez ouvrir ce dossier vous-même.
Un fichier de raccourcis sans version est d'abord copié parmi vos fichiers avec une version dans son nom, sauf
si vous en avez déjà un de ce nom.

---

<a id="binding-profile"></a>

## Éditeur de raccourcis

![Éditeur de raccourcis](images/ui-tab-bindings-profile.png)

La ligne **Modification** en haut indique le fichier de raccourcis affiché. Pour en ouvrir un autre, utilisez
[Fichier de raccourcis](#bindings-file) dans *Gestion des raccourcis*.

**Cadence des saisies clavier** est un curseur **Rapide ↔ Lent** pour la pause qu'Elite Intel observe après
chaque touche envoyée au jeu. Si le jeu perd des touches sur une machine plus lente (une macro à moitié
exécutée, un panneau ouvert sur le mauvais onglet), déplacez-le vers **Lent**.

Une seule rangée d'onglets parcourt l'éditeur : **Tout**, **Général**, **Vaisseau**, **SRV** et **À pied**,
chacun avec son nombre, puis **[Anomalies](#anomalies)** et **[Zones mortes et
sensibilité](#dead-zones-and-sensitivity)**. Les onglets de section montrent toutes les commandes, groupées sous
les titres du jeu et dans le même ordre que son écran Commandes, pour lire les deux côte à côte.

| Colonne | Signification |
|---------|---------------|
| **Commande** | La commande |
| **Primaire** / **Secondaire** | Les deux emplacements qu'Elite Dangerous donne à chaque commande, avec vos propres noms de contrôleurs et de boutons. Sur une ligne d'axe, Primaire est l'axe et Secondaire son sens et sa zone morte |
| **Accès IA** | Si Vega peut utiliser cette commande (voir plus bas) |
| **Action** | **Effacer** retire la touche clavier (ou l'axe) et laisse intacts les raccourcis du contrôleur et du HOTAS. **Corriger** apparaît sur une commande dont Vega a besoin et qui n'a pas de touche, et lui en donne une libre et sûre |

| Accès IA | Signification |
|----------|---------------|
| **Utilisé par Vega** | Vega actionne cette commande avec sa touche. Gardez-y une touche |
| **Inaccessible à Vega** | Vega a besoin de cette commande, mais elle n'a pas de touche. Mettez-en une dans l'emplacement vide : votre assignation au contrôleur continue de fonctionner |
| **Vega a besoin d'une touche** | Vega a besoin de cette commande et rien n'y est assigné |

En bas se trouvent la recherche, trois filtres et le bouton d'attribution automatique :

- **Rechercher des raccourcis...** porte sur la section, le groupe, le nom de la commande et la balise `.binds`
  brute, ainsi que sur les noms de contrôleurs et de boutons d'un raccourci. Elle fonctionne dans tous les
  onglets, et les nombres sur les onglets de section montrent où se trouvent les résultats.
- **Conflits seulement** : uniquement les lignes en conflit.
- **Non assignées** : uniquement les commandes sur lesquelles rien n'est assigné, ni au clavier ni sur un
  contrôleur.
- **Requis par Vega** : uniquement les commandes que Vega actionne, pour ses commandes intégrées et vos
  commandes personnalisées.
- **[Attribuer automatiquement les raccourcis requis par VEGA](#auto-assign-vega-required-binds)** : voir plus bas.

### Assigner un bouton ou une touche

Cliquez sur l'un des deux emplacements d'une ligne pour ouvrir **Assigner une touche clavier**. La fenêtre
montre les deux emplacements sous forme de cartes, et la carte en surbrillance reçoit votre prochaine saisie.
Cliquez sur l'autre carte pour la remplir à la place, ou utilisez le bouton entre elles pour échanger Primaire
et Secondaire.

![Assigner une commande](images/ui-bindings-assign.png)

Sous les cartes se trouvent un onglet **Clavier**, un onglet **Souris** et un onglet par contrôleur connecté.

- **Clavier** : cliquez dans le champ, puis appuyez sur les touches avec leurs modificateurs, jusqu'à trois.
  Échap annule. Maintenez Ctrl, Maj ou Alt pour voir la carte du clavier pour cette combinaison : **vert est
  libre, rouge est déjà utilisé.** Les touches réservées, comme celle du menu du jeu et Alt+F4, ne peuvent pas
  être assignées.
- **Souris** : pointez sur la case et appuyez sur le bouton de souris voulu. Le bouton gauche compte aussi, et
  les clics ailleurs dans la fenêtre fonctionnent comme d'habitude. La molette n'est jamais captée par
  accident : cliquez sur **Molette haut** ou **Molette bas**.
- **Un contrôleur** : appuyez sur un bouton ou un chapeau du contrôleur, ou cliquez sur un élément de la liste
  **Boutons** ou **Chapeaux**. Un bouton ou un chapeau est choisi **quand vous le relâchez**, si bien qu'une
  combinaison avec un bouton de shift est toujours captée en entier. Les boutons déjà maintenus deviennent des
  modificateurs : gardez le bouton de shift enfoncé et appuyez sur d'autres boutons l'un après l'autre pour
  essayer chaque combinaison. La page **Directions d'axe** a un **−** et un **+** pour chaque axe, pour les
  commandes qui agissent sur un sens d'un axe ; bougez l'axe ou cliquez sur celui que vous voulez.
- **Avec Maj**, **Avec Ctrl** et **Avec Alt**, sur chaque onglet de contrôleur, assignent un bouton du manche ou
  de la manette des gaz avec cette touche clavier. Seules ces trois-là peuvent être combinées avec un
  contrôleur, pour ne jamais entrer en collision avec les touches que Vega actionne.

Vert est libre, rouge est utilisé par une autre commande, et bleu est ce que cette commande détient. Un
contrôleur a d'abord besoin d'un nom du [Concepteur d'alias](#alias-designer). Un contrôleur débranché peut
quand même être assigné en cliquant sur ses entrées, et celui que vous branchez pendant que la fenêtre est
ouverte s'y anime aussitôt. Un emplacement contient au plus trois modificateurs, boutons maintenus et touches
confondus.

Une saisie détenue par une autre commande ne peut pas être assignée. La carte nomme cette commande, et
**Enregistrer** reste grisé tant que vous ne l'avez pas libérée. Si un changement retire la dernière touche
d'une commande utilisée par Vega, la fenêtre vous avertit. **Enregistrer** garde vos modifications, prêtes pour
**Appliquer**.

> **Les manettes ne sont que partiellement prises en charge pour l'instant.** Leurs sticks ne peuvent pas encore
> être assignés ici.

### Assigner un axe

Les commandes d'axe, comme le tangage, le lacet et les gaz, se trouvent parmi les autres lignes et ouvrent leur
propre fenêtre.

![Assigner un axe](images/ui-bindings-axis.png)

Bougez un axe du contrôleur, ou cliquez sur un axe de la liste. Réglez ensuite son **Sens** (*Normal* ou
*Inversé*), sa **Zone morte** et, sur les commandes qui en ont une, sa **Plage des gaz** (*Plage complète* ou
*Avant uniquement*). Plusieurs commandes peuvent partager un axe, et c'est normal : la fenêtre vous indique ce
qui s'y trouve déjà, et vous décidez.

<a id="anomalies"></a>

### Anomalies

![Anomalies](images/ui-bindings-anomalies.png)

Les lignes en conflit réunies au même endroit, avec leur nombre sur l'onglet. Ce sont les mêmes lignes que les
onglets de section dessinent en rouge, et un clic ouvre la même fenêtre. **Quand Vega vous signale un conflit de
raccourcis, regardez ici.**

Les conflits sont recherchés sur tous les périphériques : clavier, souris, joystick et HOTAS. Le même numéro de
bouton sur deux contrôleurs désigne deux boutons différents : `Joy_1` sur votre manche gauche n'entre jamais en
conflit avec `Joy_1` sur le droit.

Elite Dangerous ne considère un conflit que si une combinaison est *exactement* la même : `G` et `Maj+G`
cohabitent sans problème. Elite Intel suit la même règle. Les commandes qui ne peuvent jamais être actives en
même temps ne sont pas signalées : une commande de vaisseau et une de SRV, ou une commande de placement de la
Colonisation de systèmes et une commande de vol (le jeu coupe le vol pendant que vous placez une installation).
Survolez une ligne pour voir **Partage *touche* avec :** et la liste, pour chaque emplacement en conflit.

**Boutons de shift.** Si vous maintenez un bouton du manche comme modificateur pour d'autres raccourcis et que
vous assignez aussi ce bouton seul, l'actionner déclenche aussi cette commande. BindForge vous en avertit.

Vous pouvez aussi voir **Équivalent vaisseau/SRV** sur une ligne. Ce n'est pas un conflit mais une suggestion :
certaines commandes de vaisseau et de SRV sont habituellement assignées à la même touche.

Vega **parle** aussi des raccourcis qui cassent vraiment quelque chose, et nomme les touches dans le journal de
diagnostics :

- **Déplacement de la carte de la galaxie et navigation de l'interface sur la même touche.** Le tracé
  d'itinéraire ne peut pas fonctionner tant que la carte et l'interface n'ont pas des touches séparées.
- **Une commande sur votre touche de menu du jeu.** Elite ouvre le menu sur toute combinaison finissant par
  cette touche, donc la commande ne peut jamais être actionnée. La solution est d'effacer ce raccourci dans le
  jeu : Échap ouvre ce menu de toute façon.
- **Une commande sur une combinaison que le système d'exploitation prend en premier** (comme Alt+F4).
  L'actionner ferme le jeu ou quitte votre session.

<a id="dead-zones-and-sensitivity"></a>

### Zones mortes et sensibilité

![Zones mortes et sensibilité](images/ui-bindings-settings.png)

Les sensibilités, zones mortes et courbes de votre fichier de raccourcis, groupées comme dans le jeu. Chacune a
un curseur et un nombre à côté. Faites glisser le curseur, ou tapez un nombre et appuyez sur Entrée.

Le curseur couvre la même plage que celui du jeu. Le nombre est la valeur exacte, et le seul moyen de dépasser
les limites du curseur. Une valeur déjà en dehors reste telle quelle jusqu'à ce que vous la déplaciez.

<a id="auto-assign-vega-required-binds"></a>

### Attribuer automatiquement les raccourcis requis par VEGA

Un bouton qui donne une touche sûre et adaptée à votre disposition à chaque commande que **Vega actionne** et
qui n'en a pas, y compris celles qu'utilisent vos commandes personnalisées. Les autres commandes restent telles
quelles ; pour en assigner une, cliquez sur sa cellule Primaire ou Secondaire.

- Les raccourcis existants sur n'importe quel périphérique ne sont jamais modifiés. Seul un emplacement vide est
  rempli.
- Aucune touche n'est réutilisée.
- Les modifications restent en attente, pour que vous les vérifiiez avant d'**Appliquer**.

Il indique ce qu'il a fait, ce qu'il a ignoré et pourquoi : les deux emplacements déjà sur un contrôleur, plus
de touche sûre libre, ou aucun emplacement modifiable en sécurité. Deux commandes restent **sans raccourci
volontairement** : le menu du jeu (Échap l'ouvre déjà) et *éjecter toute la cargaison* (cela vide la soute dans
l'espace, et aucune commande d'Elite Intel ne l'actionne).

---

<a id="preset-editor"></a>

## Éditeur de presets

![Éditeur de presets](images/ui-bindings-preset-editor.png)

Le jeu lit ses commandes en quatre sections (**Général**, **Vaisseau**, **SRV** et **À pied**), et chaque
section charge le fichier de raccourcis que nomme sa ligne dans le fichier de preset. L'Éditeur de presets
affiche ces quatre lignes sous forme de listes déroulantes et vous laisse diriger chacune vers n'importe lequel
de vos fichiers de raccourcis. Les presets de Frontier figurent aussi dans les listes, marqués *(usine)*. Les
listes montrent le nom que montre le jeu : `Custom.4.2.binds` apparaît donc comme *Custom*.

- **Tout mettre pareil** dirige les quatre sections vers le fichier choisi à côté, en une seule étape.
- Une section qui nomme un fichier que BindForge ne trouve pas, ou aucun, s'affiche en **rouge**. Le jeu y
  chargerait autre chose, donc **Appliquer** refuse tant que vous n'avez pas choisi un vrai fichier.
- Une note apparaît quand les quatre sections utilisent des fichiers différents. Le jeu le permet, donc elle ne
  vous bloque jamais.

Comme partout dans l'onglet, rien n'est écrit avant que vous cliquiez sur **Appliquer**, et **Abandonner** remet
les listes comme avant.

<a id="which-binds-file"></a>

### Quel fichier de raccourcis Vega lit-elle ?

Vega lit les touches qu'elle actionne dans **un seul** fichier de raccourcis. Si vos quatre sections utilisent
des fichiers différents, à la première ouverture de l'onglet BindForge dans une session, elle demande **Quel
fichier de raccourcis ?** et les liste. Choisissez-en un, et elle demande s'il doit être **le seul fichier de
raccourcis** utilisé par le jeu :

- **Oui** : BindForge sauvegarde vos fichiers et y règle aussitôt les quatre sections. C'est le choix sûr : les
  touches que Vega actionne sont alors celles qu'utilise le jeu.
- **Non** : Vega lit le fichier choisi pour cette session. Les commandes d'une section qui utilise un autre
  fichier risquent de ne pas fonctionner quand Vega les actionne. La question revient au démarrage suivant, tant
  que les sections restent sur des fichiers différents.

Si vos commandes Général sont sur un autre fichier que celui que lit Vega, Vega vous avertit aussi au
démarrage : c'est généralement le tracé d'itinéraire qui en pâtit, car les touches de la carte de la galaxie ne
sont peut-être pas celles qu'utilise le jeu.

---

<a id="binding-management"></a>

## Gestion des raccourcis

Vos fichiers, avec deux onglets à eux : **Emplacements du jeu** et **Sauvegardes du joueur**.

<a id="game-install-locations"></a>

### Emplacements du jeu

![Emplacements du jeu](images/ui-bindings-install-locations.png)

<a id="bindings-file"></a>

**Fichier de raccourcis** liste vos fichiers de raccourcis, y compris les modifications non appliquées.
Choisissez-en un et cliquez sur **Charger** pour l'ouvrir dans l'Éditeur de raccourcis, ou tapez un nouveau nom
et cliquez sur **Renommer**. Un renommage est une modification comme une autre, et il atteint le jeu quand vous
cliquez sur **Appliquer**.

**Installations du jeu** liste chaque copie du jeu que BindForge connaît. Chacune a ses propres fichiers
`DeviceMappings.xml` et `.buttonMap`, et BindForge les maintient identiques.

| Colonne | Signification |
|---------|---------------|
| **Plateforme** | D'où vient cette copie : Steam, Epic, Frontier ou Oculus |
| **Emplacement** | Son dossier |
| **Origine** | *Détectée* ou *Ajoutée à la main* |
| **Fichiers de périphérique** | Combien d'entrées de périphérique et de button maps elle contient, et combien sont orphelins |
| **État** | *Correspond* à votre copie de travail, *Diffère* ou *Non trouvée* |

**Ajouter une manquante** ajoute une installation que BindForge n'a pas détectée. **Supprimer** empêche
BindForge de la sauvegarder ou d'y écrire. Si vous déplacez le jeu, cliquez sur **Supprimer**, puis sur
**Ajouter une manquante**.

<a id="player-backups"></a>

### Sauvegardes du joueur

![Sauvegardes du joueur](images/ui-tab-bindings-management.png)

Protéger vos commandes est la raison principale d'être de BindForge. Une configuration complexe demande des
heures, et une mise à jour du jeu peut la réinitialiser. BindForge sauvegarde tout de lui-même, et chaque
sauvegarde est un `.zip` daté contenant vos fichiers de raccourcis, le fichier de preset et les fichiers de
périphérique de chaque installation.

La colonne **Motif** indique pourquoi chaque sauvegarde existe, et cela détermine sa durée de conservation :

| Motif | Quand | Conservation |
|-------|-------|--------------|
| **Quotidienne** | Au premier démarrage d'Elite Intel chaque jour | Selon l'âge : 30 jours par défaut, ou 60, 90 ou 180 |
| **Avant APPLY**, **Avant RESTORE**, **Avant la vérification**, **Avant modifications** | Automatiquement, avant que BindForge n'écrive quoi que ce soit | Les 100 plus récentes par défaut, jusqu'à 1000 |
| **Utilisateur** | Quand vous cliquez sur **Sauvegarder maintenant** | Jusqu'à ce que vous la supprimiez |
| **Première configuration** | Avant la configuration initiale | Pour toujours |

| Bouton | Ce qu'il fait |
|--------|---------------|
| **Sauvegarder maintenant** | Faites vous-même une sauvegarde, avant de commencer à expérimenter |
| **Restaurer** | Remet la sauvegarde sélectionnée : chaque fichier de raccourcis, le fichier de preset et les fichiers de périphérique de chaque installation. Vos fichiers actuels sont d'abord sauvegardés |
| **Parcourir pour un fichier de sauvegarde** | Restaure une sauvegarde absente de la liste, par exemple venue d'un autre ordinateur |
| **Supprimer** | Supprime définitivement une de vos sauvegardes *Utilisateur* |

> **Ne restaurez que vos propres sauvegardes.** La sauvegarde d'un autre joueur contient ses contrôleurs, pas
> les vôtres.

**Paramètres de sauvegarde** définit où vont les sauvegardes (**Enregistrer les sauvegardes dans**, avec
**Parcourir...** et **Utiliser le dossier par défaut**) et les deux règles de conservation. Un dossier situé dans
une installation du jeu est refusé, car une mise à jour supprimerait vos sauvegardes avec lui.

---

<a id="alias-designer"></a>

## Concepteur d'alias

![Concepteur d'alias](images/ui-bindings-alias-designer.png)

D'origine, le jeu ne connaît beaucoup de contrôleurs que par un identifiant matériel, et son écran Commandes
affiche leurs boutons sous forme de numéros anonymes. Le Concepteur d'alias donne à chaque contrôleur un vrai
nom, et à chaque bouton, axe et chapeau un libellé de votre choix. Les libellés apparaissent dans l'écran
Commandes du jeu et dans l'Éditeur de raccourcis.

**Mes périphériques** liste vos contrôleurs : **Périphérique**, **VID / PID**, **État** (*Connecté* ou
*Absent*) et **Alias**. **Périphériques intégrés** est une liste en lecture seule des contrôleurs fournis par
Frontier avec le jeu. **Vérifier à nouveau** relit ce qui est connecté.

<a id="naming-a-controller"></a>

### Nommer un contrôleur

Quand un contrôleur n'a pas de nom, **Nommez vos contrôleurs** vous guide. Cela se lance après la configuration
initiale et avec **Vérifier à nouveau**, et vous pouvez le démarrer pour un seul contrôleur depuis son
engrenage. **Passer** prend le nom par défaut.

Un nom utilise les lettres de A à Z, des chiffres, `-` et `_`, et commence par une lettre, sans espaces ni
accents. Il ne peut pas être trop proche d'un nom déjà utilisé. Les raccourcis qui ne connaissent le contrôleur
que par son identifiant matériel passent au nouveau nom quand vous appliquez.

### L'Éditeur de périphérique

Cliquez sur l'**engrenage** de la ligne d'un périphérique pour l'ouvrir.

![Éditeur de périphérique](images/ui-bindings-device-editor.png)

- **Périphérique** : le nom, le VID, le PID, le nom matériel et l'état. Changez le nom ici, et **Appliquer** le
  renomme dans chaque installation et déplace les raccourcis qui l'utilisent.
- **Axes**, **Boutons**, **Chapeaux** : tapez un libellé pour chaque entrée. Les onglets n'apparaissent que pour
  les types d'entrée du contrôleur. Utilisez une entrée et sa case **s'allume**, pour voir laquelle est
  laquelle, et les axes affichent une jauge en direct.

**Enregistrer** garde vos modifications pour **Appliquer** ; rien n'atteint le jeu avant cela.

- **Effacer** retire l'entrée et les libellés du périphérique de vos fichiers du jeu quand vous appliquez. Si
  vos raccourcis utilisent ce contrôleur, ils ne sont pas perdus : ils passent à son identifiant matériel, donc
  ils continuent de fonctionner tant qu'il est branché, mais perdent le nom que vous lui aviez donné. (Sur un
  contrôleur intégré de Frontier que vous aviez renommé, ils reviennent sous le nom propre du jeu.) La question
  vous dit combien de raccourcis sont déplacés et où. **Annuler l'effacement** les remet en place.
- **Réinitialiser les libellés** remet les libellés par défaut.

Sur un périphérique intégré de Frontier, vous pouvez changer les libellés. Pour utiliser un nom à vous,
branchez-le et tapez le nom dans la case : votre entrée passe avant celle de Frontier, et retaper le nom de
Frontier vous ramène en arrière.

### Quand votre matériel change

S'il manque un contrôleur nommé utilisé par vos raccourcis, Vega vous le dit : ses raccourcis ne seront pas
chargés tant qu'il n'est pas connecté. Une mise à jour de firmware peut aussi changer l'identifiant produit d'un
contrôleur, et un remplacement en a un nouveau. BindForge peut le repérer, et **Est-ce le même contrôleur ?**
demande lequel des contrôleurs connectés c'est. Répondez, et ses raccourcis le suivront quand vous appliquerez.

> Les raccourcis désignent les boutons par leur numéro. Si le remplacement est un autre modèle, certains
> raccourcis peuvent se retrouver sur des boutons inattendus. **Vérifier dans le Bind Editor** vous y mène
> directement.

---

Community 👉[**Matrix**](https://app.element.io/#/room/#krondor:matrix.org)👈
