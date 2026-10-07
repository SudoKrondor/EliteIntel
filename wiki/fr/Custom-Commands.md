# Créer vos propres commandes

Une commande perso est une macro : une liste d'étapes — appuis de touches, pauses, texte saisi,
phrases prononcées — qui s'exécute quand vous dites l'une de vos phrases. Utilisez-en une quand les
commandes intégrées ne font pas ce que vous voulez, ou quand vous répétez sans cesse la même suite
de touches.

Les commandes perso se trouvent dans [Actions → Commandes perso](UI-Actions-Tab). Cette page en
construit deux de A à Z.

---

## Avant de commencer

- **Un modèle de langage doit tourner.** Il génère la clé d'action et associe vos phrases.
  Démarrez d'abord les services dans l'[onglet Vega](UI-Vega-Tab).
- **Chaque commande utilisée doit avoir un raccourci clavier.** Elite Intel appuie sur des touches ;
  une commande assignée uniquement à votre HOTAS ne peut pas être déclenchée par une macro.
  L'[onglet Bindings](UI-Bindings-Tab) montre ce qui manque, et **Attribuer automatiquement les
  raccourcis manquants** le corrige d'un coup.

---

## Exemple 1 : contre-mesures en une phrase

Objectif : dire *« lance les contre-mesures »* et obtenir dissipateur thermique, paillettes et
cellule de bouclier, avec confirmation de Vega.

### 1. Créer la commande

Ouvrez **Actions → Commandes perso** et cliquez sur **Nouveau**.

### 2. Nom et phrases

- **Nom :** `Contre-mesures`
- **Ce que vous direz :** une phrase par ligne (les virgules marchent aussi) :

  ```
  lance les contre-mesures
  toutes les contre-mesures
  paquet défensif
  ```

Écrivez les phrases comme vous les diriez vraiment sous pression. Elles doivent être **différentes
des commandes intégrées** — une phrase qui appartient déjà à une commande intégrée (*« chaff »*,
*« lance un dissipateur thermique »*) est refusée, car Vega ne pourrait jamais les distinguer.

### 3. Générer la clé d'action

Cliquez sur **Générer**. Le modèle de langage transforme vos phrases en clé `snake_case` anglaise,
comme `deploy_all_countermeasures`. Impossible de la saisir à la main — elle devient le nom que voit
le modèle et doit donc avoir une forme qu'il peut toujours restituer. Elle est en anglais même si vos
phrases ne le sont pas.

### 4. Ajouter les étapes

Cliquez sur **Ajouter une étape** pour chacune. Dans la fenêtre, choisissez le **Type** puis
remplissez ce qu'il demande. La liste **Raccourci** est libellée comme l'écran Commandes du jeu —
*Commandes du vaisseau / Cooling / Deploy Heatsink* — et vous pouvez y taper pour filtrer (essayez
`heat`, `srv`, `chaff`). Les noms des commandes y apparaissent en anglais.

| # | Type | Valeur | Durée ms |
|---|------|-------|-------------|
| 1 | Appui sur raccourci | Deploy Heatsink | |
| 2 | Délai | | 150 |
| 3 | Appui sur raccourci | Use Chaff Launcher | |
| 4 | Délai | | 150 |
| 5 | Appui sur raccourci | Use Shield Cell | |
| 6 | Parler | Contre-mesures lancées. | |

Utilisez **Modifier l'étape**, **Supprimer l'étape** et les boutons **▲ ▼** pour ajuster l'ordre.

### 5. Enregistrer et tester

Cliquez sur **Enregistrer**. Si quelque chose cloche, un message de validation vous dit quoi — voir
*Dépannage* en bas de cette page. La commande apparaît maintenant dans la liste.

Cliquez sur sa ligne puis sur **Exécuter** pour la tester sans parler. Quand elle fonctionne,
dites-la : *« Lance les contre-mesures. »* Vous pouvez aussi la taper dans le chat du jeu :
`@Vega lance les contre-mesures`.

---

## Exemple 2 : une route vers un lieu que vous visitez souvent

Vega ne trace pas de route vers un système nommé à voix haute — les noms sont ce que la
reconnaissance vocale rate le plus. Pour un lieu fréquent, une macro pilote la carte galactique pour
vous, toujours de la même façon.

![Commande perso qui trace une route vers Jameson Memorial](images/ui-custom-command-navigation.png)

**Ce que vous direz :** *route vers jameson memorial*, *emmène-moi à jameson memorial*, *jameson
memorial route*. Puis **Générer**.

| # | Type | Valeur | Durée ms | Ce qu'elle fait |
|---|------|-------|-------------|--------------|
| 1 | Appui sur raccourci | GALAXYMAPOPEN | | Ouvrir la carte galactique |
| 2 | Délai | | 1000 | Laisser la carte se charger |
| 3 | Maintien de raccourci | CAMZOOMIN | 500 | Zoomer la caméra de la carte |
| 4 | Appui sur raccourci | UI_LEFT | | Aller au champ de recherche |
| 5 | Appui sur raccourci | UI_RIGHT | | |
| 6 | Appui sur raccourci | UI_SELECT | | Ouvrir le champ de recherche |
| 7 | Saisir du texte | SHINRARTA DEZHRA | | Taper le nom du système |
| 8 | Pression de touche | ENTER | 50 | Rechercher |
| 9 | Délai | | 500 | Attendre le résultat |
| 10 | Appui sur raccourci | UI_RIGHT | | Entrer dans le panneau de résultat |
| 11 | Maintien de raccourci | UI_UP | 500 | Remonter jusqu'au bouton du haut |
| 12 | Appui sur raccourci | UI_SELECT | | Tracer la route |
| 13 | Délai | | 3000 | Attendre que la route soit tracée |
| 14 | Appui sur raccourci | CAMYAWLEFT | | |

Pour un autre lieu, sélectionnez cette commande, cliquez sur **Dupliquer**, changez le nom, les
phrases et le système de l'étape 7, puis **Générer** une nouvelle clé. Dupliquer laisse exprès les
phrases et la clé vides, pour que les deux commandes ne se ressemblent jamais.

Lancez une telle macro **en vol**, carte galactique fermée — la première étape l'ouvre, et sur une
carte déjà ouverte elle la fermerait.

---

## Les types d'étape

| Type | Champs | Utile pour |
|------|--------|------------|
| **Appui sur raccourci** | Raccourci | Appuyer une fois sur une commande du jeu |
| **Maintien de raccourci** | Raccourci, Durée ms | Maintenir une commande — poussée, caméra, défilement d'un menu |
| **Délai** | Durée ms | Laisser du temps au jeu : un panneau qui s'ouvre, une carte qui charge, une recherche |
| **Parler** | Texte | Faire dire quelque chose à Vega — une confirmation, ou ce que la macro vient de faire |
| **Pression de touche** | Touche brute, Modificateur, Durée ms | Appuyer sur une touche qui n'est pas un raccourci du jeu : Entrée, Échap, une lettre. La durée est le temps de maintien |
| **Saisir du texte** | Texte | Taper des caractères dans le champ qui a le focus — une recherche, la ligne de chat |

**Préférez les étapes de raccourci à Pression de touche.** Un raccourci suit la touche que le jeu
utilise vraiment, donc la macro continue de fonctionner si vous réassignez la commande. Réservez
Pression de touche aux touches sans raccourci, comme Entrée dans un champ de texte.

**Saisir du texte va là où se trouve le focus clavier.** Ouvrez d'abord le champ — avec une étape de
raccourci qui ouvre une recherche, ou **Pression de touche : Entrée** pour le chat des
communications. Sinon les lettres arrivent aux commandes du vaisseau comme des appuis de touches.

---

## Conseils

- **Les délais sont la solution habituelle.** Si une étape semble sautée, le jeu n'était pas prêt.
  Allongez le délai qui la précède, ou poussez **Cadence des saisies clavier** dans
  l'[onglet Bindings](UI-Bindings-Tab) vers Lent — cela ajoute une pause après chaque frappe envoyée.
- **Plusieurs phrases, un sens clair.** Inutile de dire une phrase mot pour mot ; Vega associe par
  le sens. Plus vos phrases se distinguent des autres commandes, plus la vôtre sera choisie de façon
  fiable.
- **Les étapes Parler intègrent vos macros au vaisseau**, et confirment qu'elles ont bien tourné.
- **Partagez-les.** **Exporter** écrit les commandes choisies dans un fichier ; **Importer** en lit
  un. L'import remplace votre jeu actuel, mais il est sauvegardé d'abord — **Restaurer depuis une
  sauvegarde** le récupère.
- **Écoutez le message de démarrage.** Au démarrage des services, Vega indique combien de commandes
  perso ont été chargées et combien ont échoué à la validation.

---

## Dépannage

Les messages de validation à l'enregistrement s'affichent en anglais.

| Message ou symptôme | Que faire |
|--------------------|------------|
| *Ajoutez au moins une phrase ci-dessus avant de générer une clé.* | Écrivez d'abord au moins une phrase |
| *Impossible de générer une clé d'action.* | Le modèle de langage n'a pas répondu. Vérifiez l'indicateur IA de l'onglet Vega et réessayez |
| *Phrase collides with a built-in action alias* | Cette phrase lance déjà une commande intégrée. Reformulez |
| *Phrase collides with another custom command* / *Duplicate phrase* | Deux commandes — ou la même deux fois — utilisent cette phrase. Changez-en une |
| *Action key collides with a built-in command* / *must be unique* | Reformulez vos phrases et **Générer** à nouveau |
| *At least one step is required* / *… is required* / *durationMs must be positive* | Il manque à une étape son raccourci, son texte, sa touche ou sa durée |
| La macro tourne mais une étape est sautée | Ajoutez ou allongez un Délai avant cette étape, ou ralentissez la cadence des saisies |
| Vega répond autre chose | Rendez vos phrases plus distinctives, ou lancez-la tapée : `@Vega <phrase>` |
| Une étape de raccourci ne fait rien | Cette commande n'a pas de raccourci clavier — voir l'[onglet Bindings](UI-Bindings-Tab) |

----
Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
