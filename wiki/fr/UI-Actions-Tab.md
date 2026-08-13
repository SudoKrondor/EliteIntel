# Onglet Actions

<img src="images/keys-binding.png" class="inline" height="20" alt="Actions"> Tout ce qu'Elite Intel
sait faire, et tout ce que vous lui avez appris. Deux sous-onglets : **Commandes intégrées** et
**Commandes perso**.

---

## Commandes intégrées

![Commandes intégrées](images/ui-tab-actions-builtin.png)

C'est la réponse à *« que puis-je dire à cet instant ? »* — et pas seulement à *« que sait faire
cette version ? »*.

### Le sélecteur de situation

Le sélecteur en haut à gauche contient **TOUTES**, ainsi que chaque situation physique où vous
pouvez vous trouver : en vaisseau, en SRV, en chasseur, en taxi, à pied ; amarré, posé, en vol
plané, en supercroisière, à un anneau, en orbite, dans l'espace profond.

- Il **suit le jeu en direct** — sortez de votre vaisseau et le sélecteur passe tout seul sur *À
  pied*, et la liste en dessous change avec lui.
- Dès que vous choisissez une situation à la main, il **cesse de suivre** et reste où vous l'avez
  mis.
- **TOUTES** liste toutes les actions de cette version, y compris celles que vous ne pouvez pas
  utiliser là où vous êtes. Une situation précise ne liste **que ce qui y est utilisable**.

À côté, un champ en lecture seule **Lieu** affiche l'emplacement concret que rapporte le jeu :
station, corps ou système.

### Recherche

Un filtre textuel simple et littéral sur les actions listées : leurs noms, leurs clés d'action et
les phrases parlées qui les déclenchent. Ce que vous tapez est ce qui est cherché.

> Ce n'est délibérément **pas** le routage du compagnon. Le dispatch de Vega classe par *sens* :
> taper « trouver » y ferait remonter des commandes ne partageant aucun mot avec vous, sans moyen de
> comprendre pourquoi. Quand on lit une liste, c'est la recherche littérale qu'on veut.

### Commandes et requêtes disponibles

Une liste unique combinée, triée alphabétiquement sur trois colonnes, réunissant les actions
intégrées, vos macros personnelles et les requêtes pour la situation choisie. Elle se met à jour en
direct à partir des événements du jeu tant que l'onglet est ouvert.

**Double-cliquez sur une entrée** pour ouvrir son détail.

### Détail de la commande

| Champ | Signification |
|-------|---------|
| **Nom de la commande** | Le nom lisible par un humain |
| **Clé d'action** | L'identifiant interne — c'est le nom que voit le modèle de langage |
| **Type de commande** | `Raccourci intégré` (appuie sur une touche) · `Action intégrée` (fait quelque chose dans l'application) · `Requête intégrée` (répond à une question) · `Commande personnalisée` (la vôtre) |
| **Description** | Ce qu'elle fait |
| **Phrases d'entraînement** | Les phrases parlées qui y mènent, dans votre langue actuelle |

Trois boutons :

- **Exécuter** — l'exécute immédiatement depuis l'application, sans parler. Si la commande prend des
  paramètres, un petit formulaire apparaît d'abord.
- **Suggérer une meilleure traduction** — ouvre un ticket GitHub prérempli avec l'identifiant de la
  commande, votre langue et les phrases actuelles, pour que vous proposiez une meilleure formulation
  pour votre locale. C'est ainsi que les jeux de phrases non anglais s'améliorent ; utilisez-le.
- **Fermer**

Voir aussi : [Toutes les commandes](AllCommands).

---

## Commandes perso

![Commandes personnalisées](images/ui-tab-actions-custom.png)

Vos propres macros — une séquence d'étapes nommée, déclenchée par ce que vous dites. Proche dans
l'esprit de VoiceAttack, mais appariée par le sens plutôt que par une phrase exacte.

Le tableau liste le **Nom** de chaque commande et ses **Phrases d'entraînement**, avec un champ de
recherche au-dessus.

| Bouton | Ce qu'il fait |
|--------|--------------|
| **Nouveau** | Créer une commande |
| **Modifier** | Modifier la commande sélectionnée |
| **Supprimer** | Supprimer la commande sélectionnée (avec confirmation) |
| **Exporter** | Écrire les commandes sélectionnées dans un fichier partageable |
| **Importer** | Lire des commandes depuis un fichier. Votre jeu actuel est sauvegardé au préalable |
| **Restaurer depuis une sauvegarde** | Récupérer le jeu qu'un import a remplacé |
| **Ouvrir le dossier des sauvegardes** | Ouvre le dossier sur le disque |

> Si le fichier des commandes personnalisées est un jour trouvé corrompu au démarrage, Elite Intel
> charge automatiquement depuis la sauvegarde et vous le signale.

### L'éditeur de commande

![Éditeur de commande personnalisée](images/ui-custom-command-editor.png)

**Identité de commande**

| Champ | Remarques |
|-------|-------|
| **Nom** | Comment vous l'appelez |
| **Description** | Ce qu'elle fait |
| **Ce que vous direz** | Les phrases que vous emploieriez pour la lancer — **une par ligne** |
| **Clé d'action** | L'identifiant interne. Appuyez sur **Générer** et le modèle de langage en écrit un à partir de vos phrases. Il doit être en snake_case ASCII, car il devient un nom d'outil que le modèle voit : laissez donc faire le bouton Générer. Ajoutez au moins une phrase avant de générer |

**Étapes** — la séquence, dans l'ordre. Ajoutez, modifiez, retirez et déplacez les étapes vers le
haut ou le bas.

| Type d'étape | Champs | À utiliser pour |
|-----------|--------|------------|
| **Appui sur raccourci** | Commande | Appuyer une fois sur un contrôle assigné |
| **Maintien de raccourci** | Commande, durée en ms | Maintenir un contrôle assigné |
| **Délai** | Durée en ms | Attendre entre deux étapes |
| **Parler** | Texte | Faire dire quelque chose à Vega |
| **Pression de touche** | Pression de touche, modificateur | Appuyer sur une touche qui n'est assignée à rien dans le jeu |

Préférez les étapes de type **Commande** à **Pression de touche** quand vous le pouvez : les
assignations suivent les touches que le jeu utilise réellement, elles survivent donc à une
réassignation.

### Les utiliser

Parlez normalement. Vous n'avez pas à reproduire une phrase d'entraînement mot pour mot — vous devez
en transmettre le sens. Plus vos phrases se distinguent de celles des autres commandes, plus la
vôtre sera choisie de façon fiable.

Vega vous indique au démarrage combien de commandes personnalisées ont été chargées, et combien ont
échoué à la validation.

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
