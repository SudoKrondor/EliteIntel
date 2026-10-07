# Onglet Bindings

<img src="images/keys-binding.png" class="inline" height="20" alt="Bindings"> Elite Intel pilote
votre vaisseau en appuyant sur les touches qu'Elite Dangerous a assignées. Si une commande n'a pas
de raccourci clavier, Elite Intel ne peut pas l'utiliser — c'est ici que vous le découvrez et que
vous le corrigez.

Deux sous-onglets : **Profil de raccourcis** et **Gestion des raccourcis**.

---

## Profil de raccourcis

![Profil de raccourcis](images/ui-tab-bindings-profile.png)

### Quel fichier est utilisé

**Dossier** (des raccourcis) — facultatif. Laissé vide, l'emplacement standard d'Elite Dangerous
est utilisé ; utilisez le sélecteur **⋮** si votre installation est ailleurs. Un dossier
inutilisable est refusé et le réglage précédent est conservé.

**Profil** — détecté automatiquement. Elite Intel lit l'entrée `StartPreset` active et se rabat,
si besoin, sur le fichier `.binds` le plus récent.

**Fichier** — le fichier `.binds` actuellement utilisé pour le diagnostic et l'assignation.

Profil et Fichier portent chacun un **ⓘ** qui explique précisément comment la valeur a été choisie.

> **« Raccourcis introuvables » avec le bon dossier ?** Elite Dangerous n'écrit un fichier
> `.binds` qu'après une personnalisation. Ouvrez *Options → Commandes* dans le jeu, modifiez
> n'importe quel raccourci, et Elite Intel trouvera le fichier.

### Cadence des saisies clavier

Un curseur **Rapide ↔ Lent** pour la pause qu'Elite Intel observe après chaque frappe envoyée au
jeu. Rapide est la valeur par défaut. Si sur une machine plus lente le jeu perd des frappes dans une
séquence — une macro qui ne s'exécute qu'à moitié, un panneau qui s'ouvre sur le mauvais onglet —,
poussez-le vers **Lent**.

### Les tableaux de raccourcis

Deux onglets : **Commandes utilisées** et **Commandes manquantes**, chacun avec son nombre. Les
lignes sont groupées sous les intitulés du jeu — **Commandes générales**, **Commandes du
vaisseau**, **Commandes du SRV**, **Commandes à pied**, **Autres commandes** — dans le même ordre
que l'écran Commandes du jeu, pour lire les deux côte à côte.

**Rechercher** filtre les deux tableaux pendant la saisie. Elle porte sur la section, le groupe, le
nom de la commande et la balise `.binds` brute. **Afficher uniquement les conflits** filtre sur les
problèmes.

| Colonne | Signification |
|--------|---------|
| **Commande** | La commande |
| **Primaire** / **Secondaire** | Les deux emplacements qu'Elite Dangerous donne à chaque commande |
| **Statut** | `Manquante` · `Pas de clavier` (assignée, mais seulement à une manette) · `Non définie` |
| **Correction rapide** | Onglet *manquantes* : assigne une touche clavier libre et sûre à cette commande |
| **Effacer** | Onglet *utilisées* : retire le raccourci clavier (primaire, secondaire ou les deux), sans toucher aux raccourcis manette et HOTAS |

> **HOTAS et manettes sont affichés mais pas modifiables.** Elite Intel exécute via les raccourcis
> clavier ; les autres périphériques n'apparaissent qu'à titre de diagnostic.

### Conflits

Elite Dangerous ne considère une combinaison en conflit que si elle est *exactement* identique —
`G` et `Maj+G` cohabitent sans problème. Elite Intel applique la même règle et signale donc ce que
le jeu signale vraiment.

Les lignes en conflit sont colorées en rouge, et le survol affiche **Partage *touche* avec :** et
la liste — pour chaque emplacement en conflit, pas seulement le premier.

Vous verrez peut-être aussi **Équivalent vaisseau/SRV - beaucoup l'assignent à la même touche
que :** sur une ligne cyan. Ce n'est pas un conflit, c'est une suggestion : certaines commandes du
vaisseau et du SRV sont habituellement sur la même touche.

Vega **parle** aussi des raccourcis qui cassent vraiment quelque chose, et nomme les touches dans
le journal de diagnostics :

- **Déplacement sur la carte galactique et navigation dans l'interface sur la même touche.** Le
  tracé de route ne fonctionnera pas tant que la carte et l'interface n'ont pas des touches
  distinctes.
- **Une commande sur votre touche de menu du jeu.** Elite ouvre le menu du jeu avec toute
  combinaison se terminant par cette touche, donc la commande ne peut jamais être déclenchée. La
  solution : effacer le raccourci du menu du jeu dans le jeu — Échap ouvre ce menu de toute façon.
- **Une commande sur une combinaison que le système d'exploitation intercepte d'abord** (comme
  Alt+F4). L'utiliser ferme le jeu ou vous fait quitter la session.

### Modifier un raccourci

Cliquez sur un emplacement pour ouvrir la fenêtre d'assignation.

![Assigner une touche](images/ui-bindings-assign.png)

Elle affiche le raccourci choisi, l'emplacement et la valeur actuelle. Ensuite, **cliquez dans le
champ et appuyez sur les touches voulues** — modificateurs et touche ensemble. Échap annule. Les
combinaisons jusqu'à trois modificateurs sont prises en charge.

Une carte du clavier en direct montre ce qui est libre : **maintenez Ctrl/Maj/Alt pour voir les
touches libres pour cette combinaison — vert est libre, rouge est déjà utilisé.** Les touches
réservées (celle du menu du jeu, Alt+F4, Ctrl+Alt+F sous Linux) sont marquées et ne peuvent pas
être assignées.

**Effacer le raccourci** retire l'assignation.

### Attribuer automatiquement les raccourcis manquants

Un bouton qui assigne des touches clavier sûres et adaptées à votre disposition à **toutes** les
commandes sans raccourci clavier.

- Les raccourcis existants ne sont jamais modifiés.
- Aucune touche n'est réutilisée.
- Les changements vont **dans le brouillon uniquement** — vérifiez-les, puis Appliquer.

Il indique ce qu'il a fait, et ce qu'il a ignoré et pourquoi : les deux emplacements déjà sur une
manette, plus de touche sûre libre, ou aucun emplacement modifiable sans risque. Deux commandes
sont **laissées sans raccourci exprès** : le menu du jeu (Échap l'ouvre déjà) et *larguer toute la
cargaison* (cela vide la soute dans l'espace et aucune commande d'Elite Intel ne l'utilise —
assignez-la à la main si vous la voulez).

### Brouillon, Appliquer, Restaurer

Les modifications ne vont **pas** directement dans Elite Dangerous. Elles s'accumulent dans un
brouillon, et le badge indique **Brouillon** ou **Synchronisé**. Le même état apparaît dans
l'indicateur *Mappage* de l'onglet Vega.

| Bouton | Ce qu'il fait |
|--------|--------------|
| **Appliquer** | Écrit le brouillon dans votre fichier `.binds`, en sauvegardant d'abord une copie de l'ancien |
| **Restaurer** | Abandonne le brouillon et recharge depuis le fichier du jeu |

> **Après avoir appliqué, ouvrez puis fermez l'écran Commandes dans Elite Dangerous.** Le jeu ne
> relit ses raccourcis qu'à l'ouverture de cet écran. Vega le dit aussi à voix haute.

Si le fichier de raccourcis du jeu a changé après la création de votre brouillon, Appliquer refuse
et vous demande de recharger ou d'abandonner d'abord, plutôt que d'écraser en silence la
modification de quelqu'un d'autre.

Si vous fermez l'application avec un brouillon non appliqué, on vous propose **Appliquer au jeu**,
**Conserver le brouillon** ou **Annuler**.

---

## Gestion des raccourcis

![Gestion des raccourcis](images/ui-tab-bindings-management.png)

**Sauvegardes du joueur** — des instantanés que vous prenez vous-même avec **Sauvegarder
maintenant**, listés par date (**Créée**) et par les **Fichiers** qu'ils contiennent. Faites-en un
avant d'expérimenter.

(Par ailleurs, chaque **Appliquer** enregistre d'abord discrètement une copie du fichier du jeu
dans `elite-intel/bindings/backups/`. C'est un filet de sécurité, non listé ici.)

| Bouton | Ce qu'il fait |
|--------|--------------|
| **Restaurer dans le brouillon** | Charge la sauvegarde dans votre brouillon pour la vérifier avant qu'elle ne touche le jeu |
| **Restaurer en direct** | La charge et l'applique directement au jeu. Les vérifications de sécurité habituelles s'exécutent quand même |
| **Supprimer la sauvegarde** | Supprime la sauvegarde définitivement |

Chacune de ces actions demande confirmation. Les deux restaurations remplacent les modifications
non enregistrées du brouillon actuel.

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
