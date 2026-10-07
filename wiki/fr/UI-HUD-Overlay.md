# Overlay HUD

Un overlay toujours au premier plan qui affiche votre objectif actuel et votre conversation avec
Vega — dans la fenêtre du jeu ou dans un casque VR.

![Overlay HUD en jeu](images/ui-overlay-ingame.png)

L'overlay tourne dans son propre processus, il n'entre donc pas en concurrence avec le jeu ni avec
l'application pour le fil de l'interface.

La carte est dessinée selon la géométrie du cockpit plutôt qu'à angle droit avec l'écran : elle
s'incline comme les panneaux du vaisseau à cet endroit de l'écran — déplacez-la et l'inclinaison
change. Ses lignes sont obliques, c'est pourquoi une valeur peut se trouver nettement plus bas que
son libellé : lisez chaque ligne le long de la pente, comme les affichages du jeu à côté. La
hauteur apparente de cette chute dépend aussi de la **TAILLE DU TEXTE** — l'inclinaison est fixée
par le cockpit, donc un texte plus petit donne des lignes plus courtes et la même chute en
traverse davantage.

Activez-le avec **AFFICHER OVERLAY** dans l'[onglet Vega](UI-Vega-Tab), et configurez-le avec
**RÉGLAGES DE L'OVERLAY** juste à côté. L'application se souvient si vous l'avez laissé activé et
le rétablit au lancement suivant.

> Si le binaire de l'overlay manque dans la distribution, l'interrupteur le signale dans le journal
> de diagnostics. Il ne prétend pas afficher un overlay inexistant.

---

## Ce qu'il affiche

### Une carte d'objectif

Une seule carte tient à l'écran, donc l'overlay montre **la chose la plus importante que vous
faites en ce moment** et change tout seul quand cela change. Il n'y a rien à configurer.

Le travail que vous avez accepté passe toujours avant celui que l'application propose, dans cet
ordre :

| Rang | Carte | Apparaît quand |
|------|------|--------------|
| 1 | **CONTRAT EXTERMINATION** | Vous menez des missions de massacre — éliminations requises, pile, récompense |
| 1 | **MISSION** | Vous avez accepté des missions — cible, fret ou passagers, échéance et récompense de la mission mise en avant, plus la valeur du reste de la pile |
| 2 | **ROUTE COMMERCIALE** | Une route commerciale est tracée — marchandise, achat, vente, marge, étape *n* sur *m* |
| 2 | **OPPORTUNITÉ FRET** | Vega a trouvé une paire achat/vente rentable pour l'espace libre de votre trajet |
| 2 | **CHANTIER** | Vous transportez pour une construction de colonisation — avancement, ce qui reste, et quoi charger au prochain voyage |
| 2 | **MARCHANDISE TROUVEE** / **LISTE D'ACHATS** / **VENDRE CARGO** | Une recherche de marchandise a trouvé un marché et une route y est tracée — quoi acheter (ou vendre), stock et prix |
| 3 | **EXTRACTION** | Vous avez des cibles de minage, une raffinerie montée et n'êtes pas en supercroisière — soute, drones, cibles |
| 3 | **EXOBIOLOGIE** | Il reste des genres à prélever dans ce système — seulement si *Annoncer les découvertes* est activé |
| 3 | **CHASSE AUX PRIMES** | Vous êtes dans un site d'extraction de ressources — type de site, primes en soute, éliminations |
| 3 | **ZONE DE CONFLIT** | Vous êtes dans une zone de conflit — intensité, votre camp, obligations de combat en main |
| 3 | **ITINÉRAIRE TRACÉ** | Une route est définie — destination, système suivant, sauts restants |

La carte **itinéraire tracé** prend un titre plus précis quand Vega a déterminé la destination pour
vous et que la route y mène toujours : **MARCHAND DE MATÉRIAUX**, **COURTIER EN TECHNOLOGIE**,
**AGENTS INTERSTELLAIRES**, **VISTA GENOMICS**, **RAVITAILLEMENT** ou **ÉQUIPEMENT**, avec la
station et le type. Un tracé ailleurs efface ce détail, donc une vieille course ne peut jamais
s'accaparer la carte.

La carte **mission** met en avant la mission dont la destination est le bout de votre route
tracée ; sinon celle qui expire le plus tôt.

### La conversation

Sous la carte, l'overlay tape l'échange au fil de l'eau — ce que vous avez dit, la réponse de Vega
et le trafic radio —, chacun dans sa couleur. Utile si vous jouez la voix baissée.

---

## Réglages de l'overlay

![Réglages de l'overlay](images/ui-overlay-settings.png)

**TRANSPARENCE DU FOND** (0–100 %) et **TAILLE DU TEXTE** (75–200 %) sont deux contrôles séparés
exprès. Un seul curseur d'« opacité » estomperait le texte avec le fond — exactement ce qui rend un
overlay atténué illisible au-dessus d'une surface planétaire claire. Estompez le fond ; laissez le
texte tranquille.

### Couleurs du texte

Un sélecteur de couleur par rôle, pour adapter l'overlay aux couleurs de votre cockpit ou à vos
yeux :

**Titre de l'objectif** · **Bon** · **Avertissement** · **Critique** · **Libellés** · **Vos
paroles** · **IA du vaisseau** · **Trafic radio**

**Réinitialiser les couleurs** remet chaque couleur à celle livrée avec l'overlay.

### AFFICHER SUR

| Mode | Ce qu'il fait |
|------|--------------|
| **Ecran** | Une fenêtre de bureau. Le mode par défaut. La carte s'incline selon le cockpit, et l'inclinaison change selon l'endroit où vous la placez |
| **Casque VR** | Un overlay SteamVR. Nécessite SteamVR lancé. Si la VR est indisponible, il se rabat sur une fenêtre de bureau, pour ne jamais vous laisser sans rien |
| **Ecran et casque** | Les deux à la fois, avec les mêmes données. Utile si vous volez en VR mais diffusez ou enregistrez depuis l'écran |
| **Fenêtre de capture VR** | Une fenêtre simple, plate et opaque qu'un outil de capture peut épingler |

### À propos de la fenêtre de capture VR

Ce mode ne communique **pas** avec SteamVR. Lancez votre outil de capture — Desktop+, OVR Toolkit
ou Virtual Desktop — et choisissez la fenêtre nommée **« EliteIntel HUD (VR capture) »**.

Pourquoi il existe : le mode SteamVR transmet au compositeur une texture complète à chaque
caractère tapé, et sur un casque en streaming cela a été signalé comme un vrai coût en images par
seconde. Un outil de capture récupère la fenêtre sur le GPU à son propre rythme, et vous offre des
contrôles de placement et de courbure que cette application n'a pas.

C'est un mode à part plutôt que « pointez votre outil sur la fenêtre Écran », parce que cette
fenêtre est inclinée, transparente et de type fenêtre d'outil — que les sélecteurs de capture
filtrent entièrement.

### POSITION DANS LE CASQUE

Huit positions : **En haut, En haut à droite, À droite, En bas à droite, En bas, En bas à gauche,
À gauche, En haut à gauche.**

> **Le HUD est fixé devant votre siège et ne suit pas votre tête.** La direction choisie est
> mesurée depuis votre regard après le *Reset Seated Position* de SteamVR — recentrer la vue déplace
> donc le HUD avec le cockpit, ce qui est le but. Regardez ailleurs et le HUD reste où vous l'avez
> laissé, exactement comme un panneau physique.

---

## Le lire dans une autre langue

Les libellés des cartes suivent la langue de l'application, et les nombres sont groupés comme le
fait cette langue. Les noms fournis par le jeu — systèmes, stations, marchandises — passent tels
quels.

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
