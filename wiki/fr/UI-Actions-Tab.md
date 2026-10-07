# Onglet Actions

<img src="images/keys-binding.png" class="inline" height="20" alt="Actions"> Tout ce qu'Elite Intel
sait faire, et tout ce que vous lui avez appris. Deux sous-onglets : **Commandes intégrées** et
**Commandes perso**.

---

## Commandes intégrées

![Commandes intégrées](images/ui-tab-actions-builtin.png)

C'est la réponse à *« que puis-je dire maintenant ? »* — pas seulement à *« que sait faire cette
version ? »*

### Le sélecteur de portée

Le sélecteur en haut à gauche contient **TOUTES**, plus chaque situation physique possible : dans
le vaisseau, dans le SRV, dans le chasseur, dans le taxi ; à pied (station, hangar, espace social,
planète) ; amarré, posé, en vol plané, en supercroisière, à un anneau, en orbite, dans l'espace
profond.

- Il **suit le jeu en direct** — sortez du vaisseau et le sélecteur passe tout seul sur *À pied*,
  et la liste en dessous change avec lui.
- Dès que vous choisissez une portée à la main, il **cesse de suivre** et reste où vous l'avez mis.
- **TOUTES** liste chaque action de cette version, y compris celles inutilisables là où vous êtes.
  Une situation précise ne liste **que ce qui y est utilisable**.
- Si le jeu ne tourne pas, le sélecteur affiche **TOUTES**.

À côté, un champ en lecture seule **Lieu** montre l'emplacement concret signalé par le jeu —
station, corps ou système.

### Rechercher

Un filtre texte simple et littéral sur les actions listées : leurs noms, leurs clés d'action et les
phrases parlées qui les déclenchent. On cherche exactement ce que vous tapez.

> Ce n'est volontairement **pas** le routage de Vega. Vega associe par *sens*, donc taper
> « trouve » y ferait remonter des commandes sans aucun mot en commun, sans moyen de savoir
> pourquoi. Pour lire une liste, vous voulez une recherche littérale.

### Commandes et requêtes disponibles

Une liste unique, triée par ordre alphabétique sur trois colonnes, avec les actions intégrées, vos
macros personnalisées et les requêtes de la portée choisie. Elle se met à jour en direct avec les
événements du jeu tant que l'onglet est ouvert — y compris une commande perso créée entre-temps.

**Cliquez sur une entrée** (ou sélectionnez-la et appuyez sur Entrée) pour ouvrir ses détails.

### Détails d'une commande

| Champ | Signification |
|-------|---------|
| **Nom de la commande** | Le nom lisible |
| **Clé d'action** | L'identifiant interne — c'est le nom que voit le modèle de langage |
| **Type de commande** | `Raccourci intégré` (appuie sur une touche) · `Action intégrée` (fait quelque chose dans l'application) · `Requête intégrée` (répond à une question) · `Commande personnalisée` (la vôtre) |
| **Description** | Ce qu'elle fait |
| **Phrases d'entraînement** | Les phrases parlées qui y mènent, dans votre langue actuelle |

Boutons :

- **Exécuter** — l'exécute tout de suite depuis l'application, sans parler. Si la commande prend
  des paramètres, un petit formulaire apparaît d'abord.
- **Suggérer une meilleure traduction** — ouvre un ticket GitHub prérempli avec l'id de la
  commande, votre langue, les phrases actuelles et vos suggestions, pour proposer une meilleure
  formulation dans votre langue. C'est ainsi que les phrases non anglaises s'améliorent ;
  utilisez-le, s'il vous plaît.
- **Retour** — ferme la fenêtre.

Voir aussi : [Toutes les commandes et requêtes](AllCommands).

---

## Commandes perso

![Commandes perso](images/ui-tab-actions-custom.png)

> Guide pas à pas : [Créer vos propres commandes](Custom-Commands).

Vos propres macros — une suite d'étapes nommée, déclenchée par ce que vous dites (ou tapez dans le
chat du jeu). Dans l'esprit de VoiceAttack, mais associée par le sens plutôt que par une phrase
exacte.

Le tableau affiche le **Nom** et les **Phrases d'entraînement** de chaque commande, avec un champ
de recherche au-dessus. **Cliquez sur une ligne** pour ouvrir ses détails, qui montrent la
**Séquence** d'étapes et proposent **Exécuter**, **Modifier**, **Dupliquer** et **Supprimer**.

En haut :

| Bouton | Ce qu'il fait |
|--------|--------------|
| **Nouveau** | Créer une commande |
| **Exporter** | Choisir des commandes et les écrire dans un fichier à partager |
| **Importer** | Lire des commandes depuis un fichier. La fenêtre d'import marque chaque entrée *Prête*, *Conflit* (sa clé d'action existe déjà et sera écrasée) ou *Invalide*. L'import **remplace** votre jeu actuel — il est sauvegardé d'abord, et on vous propose ensuite **Ouvrir le dossier des sauvegardes** |
| **Restaurer depuis une sauvegarde** | Récupère le jeu de commandes remplacé par un import |

**Dupliquer** ouvre l'éditeur avec le nom et les étapes d'une copie, mais **sans phrases ni clé
d'action** — écrivez-lui de nouvelles phrases, pour que les deux commandes restent faciles à
distinguer.

> Si le fichier des commandes perso est trouvé corrompu au démarrage, Elite Intel charge
> automatiquement la sauvegarde et vous le signale.

### L'éditeur de commande

![Éditeur de commande perso](images/ui-custom-command-editor.png)

**Identité de commande**

| Champ | Remarques |
|-------|-------|
| **Nom** | Comment vous l'appelez |
| **Ce que vous direz** | Les phrases que vous diriez pour la lancer — **une par ligne** |
| **Clé d'action** | L'identifiant interne. Cliquez sur **Générer** et le modèle de langage l'écrit à partir de vos phrases. Elle est toujours en snake_case anglais, quelle que soit la langue de vos phrases, car elle devient un nom d'outil vu par le modèle — elle ne peut donc pas être saisie à la main. Ajoutez au moins une phrase avant de générer |

**Étapes** — la séquence, dans l'ordre. Ajoutez, modifiez, retirez et déplacez des étapes.

| Type d'étape | Champs | Utile pour |
|-----------|--------|------------|
| **Appui sur raccourci** | Raccourci | Appuyer une fois sur une commande assignée |
| **Maintien de raccourci** | Raccourci, Durée ms | Maintenir une commande assignée |
| **Délai** | Durée ms | Attendre entre deux étapes |
| **Parler** | Texte | Faire dire quelque chose à Vega |
| **Pression de touche** | Touche brute, Modificateur, Durée ms | Appuyer sur une touche assignée à rien dans le jeu |
| **Saisir du texte** | Texte | Taper du texte dans le champ qui a le focus |

**Saisir du texte** va là où se trouve le focus clavier. Ouvrez le champ de texte avec une étape
précédente — par exemple **Pression de touche : Entrée** pour ouvrir le chat des communications —
sinon le texte arrive aux commandes du vaisseau sous forme d'appuis de touches.

Préférez les étapes de **raccourci** à **Pression de touche** quand c'est possible — les raccourcis
suivent les touches que le jeu utilise vraiment, donc ils survivent à une réassignation.

### Exemple : une route vers un lieu que vous visitez souvent

La navigation intégrée de Vega ne trace une route que vers le **résultat d'une recherche** (un
marchand, un marché, un terrain de chasse…), vers des lieux qu'elle connaît déjà (base, votre
vaisseau-mère, une mission) ou vers le système de votre presse-papiers. Elle ne trace pas de route
vers un système que vous nommez simplement à voix haute — les noms sont justement ce que la
reconnaissance vocale rate le plus. Pour un lieu où vous allez régulièrement, une commande perso le
fait exactement, à chaque fois :

![Commande perso qui trace une route vers Jameson Memorial](images/ui-custom-command-navigation.png)

**Ce que vous direz :** *route vers jameson memorial*, *emmène-moi à jameson memorial*, *jameson
memorial route* — puis **Générer** la clé d'action.

| # | Étape | Valeur | Durée ms | Ce qu'elle fait |
|---|------|-------|-------------|--------------|
| 1 | Appui sur raccourci | GALAXYMAPOPEN | | Ouvrir la carte galactique |
| 2 | Délai | | 1000 | Laisser la carte se charger |
| 3 | Maintien de raccourci | CAMZOOMIN | 500 | Zoomer la caméra de la carte |
| 4 | Appui sur raccourci | UI_LEFT | | Aller au champ de recherche |
| 5 | Appui sur raccourci | UI_RIGHT | | |
| 6 | Appui sur raccourci | UI_SELECT | | Ouvrir le champ de recherche |
| 7 | Saisir du texte | SHINRARTA DEZHRA | | Taper le nom du système — sans reconnaissance vocale |
| 8 | Pression de touche | ENTER | 50 | Rechercher |
| 9 | Délai | | 500 | Attendre le résultat |
| 10 | Appui sur raccourci | UI_RIGHT | | Entrer dans le panneau de résultat |
| 11 | Maintien de raccourci | UI_UP | 500 | Remonter jusqu'au bouton du haut |
| 12 | Appui sur raccourci | UI_SELECT | | Tracer la route |
| 13 | Délai | | 3000 | Attendre que la route soit tracée |
| 14 | Appui sur raccourci | CAMYAWLEFT | | |

Pour créer la vôtre, copiez-la avec **Dupliquer**, changez le nom, les phrases et le système de
l'étape **Saisir du texte**, puis générez une nouvelle clé.

Conseils :

- **Les phrases peuvent être séparées par des virgules** aussi bien que par des retours à la ligne.
- **Les délais dépendent de votre PC.** Si la carte n'est pas prête quand l'étape suivante arrive,
  augmentez les délais — ou poussez **Cadence des saisies clavier** de l'[onglet Bindings](UI-Bindings-Tab)
  vers Lent.
- **Chaque étape de raccourci exige une assignation clavier** dans le jeu. Si une commande n'est
  pas assignée, l'[onglet Bindings](UI-Bindings-Tab) l'affiche sous *Commandes manquantes*.
- Vega ouvre et lit la carte galactique de la même façon pour ses propres commandes de route ; une
  commande qui part d'une carte **fraîchement ouverte** est donc le modèle fiable — ne partez pas
  d'une carte déjà ouverte.

### Les utiliser

Parlez normalement. Vous n'avez pas à reproduire une phrase d'entraînement mot pour mot — vous
devez transmettre le même sens. Plus vos phrases se distinguent de celles des autres commandes,
plus la vôtre sera choisie de façon fiable.

Vega vous indique au démarrage combien de commandes perso ont été chargées, et combien ont échoué
à la validation.

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
