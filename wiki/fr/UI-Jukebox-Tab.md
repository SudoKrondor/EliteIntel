# Onglet Jukebox

<img src="images/speaker.png" class="inline" height="20" alt="Jukebox"> Votre propre musique, à
partir de vos propres fichiers, jouée sous Vega plutôt que par-dessus. Chaque fois que Vega parle,
la musique baisse automatiquement et remonte quand elle a fini — vous ne ratez aucune annonce.

Rien à installer, pas de compte, pas de service de streaming. L'onglet se lit de haut en bas :
**Bibliothèque musicale**, **Liste de lecture**, **Lecture**.

---

## Bibliothèque musicale

D'où vient la musique.

- **Parcourir...** — choisissez un dossier de musique. Elite Intel le parcourt, sous-dossiers
  compris, et ajoute à la liste chaque fichier lisible trouvé. Les fichiers arrivent dans l'ordre
  des dossiers, donc les chapitres d'un livre audio arrivent dans l'ordre.
- **Réanalyser** — reparcourir le dossier à la recherche de fichiers ajoutés depuis.

**Formats pris en charge :** MP3, FLAC, M4A / M4B (AAC), OGG / OGA (Vorbis) et WAV. Les fichiers
que le Jukebox ne sait pas lire (WMA, Apple Lossless, achats protégés par DRM, Opus) sont ignorés.

---

## Liste de lecture

La liste est la file d'attente : ce que vous voyez est l'ordre de lecture.

| Colonne | Signification |
|--------|---------|
| **#** | Position. Un ▶ marque la piste en cours |
| **Titre** · **Artiste** · **Album** | Lus dans les tags des fichiers. Une grande bibliothèque se remplit en quelques secondes |
| **Durée** | Durée de la piste |

- **Double-cliquez** sur une piste pour la lire.
- **Faites glisser** les lignes pour les réordonner. L'ordre est enregistré.
- Cliquer sur un en-tête de colonne ne trie **pas** — cela détruirait un ordre arrangé à la main.
  Le tri se trouve dans le menu contextuel.

Un fichier disparu du disque est marqué **manquant**.

### Menu contextuel

| Élément | Ce qu'il fait |
|------|--------------|
| **Lire maintenant** | Lit la piste sélectionnée |
| **Lire ensuite** | Place les pistes sélectionnées juste après la piste en cours |
| **Retirer de la liste** | Les retire de la liste (les fichiers sur le disque ne sont pas touchés) |
| **Afficher dans le gestionnaire de fichiers** | Ouvre le dossier qui contient le fichier |
| **Copier artiste et titre** | Dans le presse-papiers |
| **Ajouter un dossier...** | Ajoute la musique d'un autre dossier |
| **Importer une liste de lecture...** | Ajoute les pistes nommées par une liste `.m3u` / `.m3u8` |
| **Retirer les fichiers manquants** | Supprime chaque entrée dont le fichier a disparu |
| **Vider la liste** | Retire toutes les pistes (demande confirmation ; les fichiers sur le disque ne sont pas supprimés) |
| **Trier par** → Titre / Artiste / Dossier | Un tri ponctuel qui réécrit l'ordre de la liste |

---

## Lecture

- **La tête de lecture** — où vous en êtes dans la piste. Faites-la glisser pour vous déplacer ; la
  lecture saute quand vous relâchez.
- **Précédent · Lecture/Pause · Arrêt · Suivant** — les commandes de transport. **Arrêt** rembobine
  la piste au début ; **Pause** garde votre position.
- **Ordre** — *Séquentiel* ou *Aléatoire*.
- **Volume** — le niveau propre de la musique. Il est ici et non dans les paramètres Audio, pour ne
  jamais baisser Vega par erreur.

Votre position dans une piste est mémorisée d'une session à l'autre — pratique pour les livres
audio —, mais le Jukebox ne se met jamais à jouer tout seul au lancement de l'application.

---

## Commandes vocales

Chaque commande musicale nomme la *musique*, une *piste* ou un *morceau*, pour ne jamais entrer en
collision avec les commandes du vaisseau.

| Dites | Ce qui se passe |
|-----|--------------|
| *« Jouer la musique »* / *« lancer la musique »* | Démarrer ou reprendre |
| *« Mettre la musique en pause »* / *« arrêter la musique »* | Pause — « jouer la musique » reprend là où elle s'est arrêtée |
| *« Piste suivante »* / *« passer ce morceau »* | Piste suivante |
| *« Piste précédente »* | Piste précédente |
| *« Redémarrer la playlist »* | Retour à la première piste |
| *« Mélanger la musique »* / *« musique aléatoire »* | Ordre aléatoire |
| *« Mets le morceau Rocket Man »* | Cherche une piste par titre ou artiste et la joue. Si rien ne correspond vraiment, Vega le dit plutôt que de jouer la mauvaise |

Elles fonctionnent aussi tapées dans le chat du jeu — voir [Toutes les commandes](AllCommands).

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
