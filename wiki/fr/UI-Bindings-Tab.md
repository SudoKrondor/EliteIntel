# Onglet Bindings

<img src="images/keys-binding.png" class="inline" height="20" alt="Bindings"> **Nouveau en V1.1.**
Les assignations occupaient un coin de l'onglet Actions ; elles ont désormais leur propre onglet,
avec un éditeur complet.

Elite Intel pilote votre vaisseau en appuyant sur les touches auxquelles Elite Dangerous est
assigné. Si un contrôle n'a pas d'assignation clavier, Elite Intel ne peut pas s'en servir — c'est
dans cet onglet que vous le découvrez et que vous le corrigez.

Deux sous-onglets : **Profil de raccourcis** et **Gestion des raccourcis**.

---

## Profil de raccourcis

![Profil de raccourcis](images/ui-tab-bindings-profile.png)

### Quel fichier est utilisé

**Profil** — détecté automatiquement. Elite Intel lit l'entrée `StartPreset` active et se rabat au
besoin sur le fichier `.binds` le plus récent.

**Fichier** — le fichier `.binds` actuellement utilisé pour le diagnostic et l'assignation.

**Dossier** — facultatif. Laissez-le vide et l'emplacement standard d'Elite Dangerous est utilisé ;
renseignez-le si votre installation se trouve à un endroit inhabituel.

Les deux champs disposent d'un bouton ⓘ expliquant exactement comment la valeur a été choisie.

### Les tableaux d'assignations

Deux tableaux : **Assignations utilisées** et **Assignations manquantes**, chacun avec son décompte.
Les assignations sont regroupées par catégorie :

Vaisseau / vol · Combat · Panneaux d'interface · Cartes · Exploration · Caméra · SRV · À pied ·
Divers

| Colonne | Signification |
|--------|---------|
| **Commande** | Le contrôle |
| **Primaire** / **Secondaire** | Les deux emplacements qu'Elite Dangerous donne à chaque contrôle |
| **Statut** | `Manquante` · `Pas de clavier` (assignée, mais uniquement à une manette) · `Non définie` |
| **Correction rapide** | Assigne une touche libre et sûre à ce seul contrôle |
| **Effacer** | Retire l'assignation clavier, en laissant intactes celles de manette et de HOTAS |

> **Les HOTAS et manettes sont affichés mais non modifiables.** Elite Intel exécute via les
> assignations clavier ; les autres périphériques n'apparaissent qu'à titre de diagnostic.

**Afficher uniquement les conflits** filtre les tableaux sur les problèmes.

### Conflits

Elite Dangerous ne considère un accord comme conflictuel que s'il s'agit *exactement* du même
accord — `G` et `Shift+G` cohabitent sans souci. Elite Intel applique la même règle, et signale donc
ce que le jeu signale réellement.

Les lignes en conflit sont colorées, et le survol affiche **Partage *touche* avec :** suivi de la
liste.

Vous pouvez aussi voir **Jumeau vaisseau/SRV — beaucoup l'assignent comme :** — non pas un conflit,
mais une suggestion. Certains contrôles de vaisseau et de SRV sont par convention assignés à la même
touche.

### Modifier une assignation

Cliquez sur un emplacement pour ouvrir la boîte de dialogue d'assignation.

![Assigner une touche](images/ui-bindings-assign.png)

Elle affiche l'assignation sélectionnée, l'emplacement et la valeur actuelle. Ensuite **cliquez dans
le champ et appuyez sur les touches voulues** — modificateurs et touche ensemble. Échap annule. Les
accords à plusieurs modificateurs sont pris en charge.

Une carte clavier en direct montre ce qui est disponible : **maintenez Ctrl/Maj/Alt pour voir les
touches libres pour cette combinaison — vert : libre, rouge : déjà utilisée.** Les touches réservées
par le système d'exploitation sont signalées et ne peuvent pas être assignées.

**Effacer l'assignation** supprime l'attribution.

### Corriger les manquantes

Un bouton qui attribue des touches clavier sûres et adaptées à votre disposition à **tous** les
contrôles dépourvus d'assignation clavier.

- Les assignations existantes ne sont jamais modifiées.
- Aucune touche n'est jamais réutilisée.
- Les changements ne vont **que dans votre brouillon**.

Il rapporte ce qu'il a fait, ainsi que ce qu'il a ignoré et pourquoi : les deux emplacements déjà sur
une manette, plus aucune touche sûre disponible, ou aucun emplacement modifiable en toute sécurité.

### Brouillon, Appliquer, Réinitialiser

Les modifications ne partent **pas** directement vers Elite Dangerous. Elles s'accumulent dans un
brouillon, et le badge d'état affiche **Brouillon — non appliqué au jeu** ou **Synchronisé**. Le même
état apparaît sur l'indicateur *Touches* de l'onglet Vega.

| Bouton | Ce qu'il fait |
|--------|--------------|
| **Appliquer au jeu** | Écrit le brouillon dans votre fichier `.binds`, après avoir pris une sauvegarde |
| **Réinitialiser depuis le jeu** | Jette le brouillon et recharge depuis le fichier du jeu |

> **Après application, ouvrez puis refermez l'écran Contrôles dans Elite Dangerous.** Le jeu ne
> relit ses assignations qu'à l'ouverture de cet écran. Elite Intel le dit aussi à voix haute.

Si le fichier d'assignations du jeu a changé après la création de votre brouillon, Appliquer refuse
et vous demande de recharger ou d'abandonner d'abord, plutôt que d'écraser silencieusement la
modification de quelqu'un d'autre.

Si vous fermez l'application avec un brouillon non appliqué, il vous est demandé si vous voulez
**Appliquer au jeu**, **Conserver le brouillon** ou **Annuler**.

---

## Gestion des raccourcis

![Gestion des raccourcis](images/ui-tab-bindings-management.png)

Vos sauvegardes d'assignations, listées par date de **Créée** et par les **Fichiers** que chacune
contient. Elite Intel en prend une automatiquement avant chaque application ; **Sauvegarder
maintenant** en prend une à la demande.

| Bouton | Ce qu'il fait |
|--------|--------------|
| **Restaurer dans le brouillon** | Charge la sauvegarde dans votre brouillon, pour que vous puissiez la relire avant qu'elle ne touche le jeu |
| **Restaurer en direct** | La charge et l'applique directement au jeu. Les contrôles habituels d'application sûre s'exécutent toujours |

L'une comme l'autre remplace les modifications non enregistrées du brouillon en cours, et toutes deux
demandent confirmation.

---

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
