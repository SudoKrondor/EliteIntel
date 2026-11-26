# Suivi des Missions de Massacre de Pirates

EliteIntel suit les missions de massacre de pirates, notamment les comptages de kills, les factions cibles et les calculs de récompenses. Il surveille les kills et les paiements tout au long de la session.

## Pour commencer

Pour accomplir efficacement les missions de massacre de pirates :

1. **Trouvez un ensemble** : Demandez ``trouve des missions de massacre de pirates`` (voir *Trouver des ensembles de
   massacre* plus bas), ou cherchez-en un sur [INARA](https://inara.cz) : un système où plusieurs factions proposent
   des missions contre la même faction de pirates, à côté d'un système doté d'un Site d'Extraction de Ressources (RES).
   Acceptez autant de missions que possible contre cette faction. EliteIntel suit la faction cible, le nombre de kills
   et les détails des paiements.
2. **Interrogez le statut des missions** : Après avoir accepté des missions, posez des questions à EliteIntel telles que ``Combien de kills restants sur les missions de pirates ?`` ou ``Quel est le paiement du massacre de pirates ?`` EliteIntel retourne le total des kills requis, les crédits potentiels et une ventilation par faction. Précisez que la requête concerne les pirates pour éviter toute confusion avec d'autres types de missions.

## Engager les pirates dans le Haz RES

1. **Rejoignez le site** : Volez vers le système cible et entrez dans le Haz RES. Scannez les vaisseaux à proximité.
2. **Identification des cibles** : EliteIntel annonce les informations suivantes lorsque des vaisseaux sont scannés :
    - **Pirates non liés aux missions** : ``Cible légale, [type de vaisseau], [rang du pilote], [récompense de prime].`` Ces vaisseaux sont des cibles valides mais ne comptent pas pour les objectifs de mission.
    - **Pirates de mission** : ``Cible de mission ! [type de vaisseau], [rang du pilote], [récompense de prime].`` Ce sont les cibles principales.
    - EliteIntel n'annonce pas les vaisseaux propres ou amis, gardant la sortie audio pertinente.
3. **Confirmation de kill** : Lorsqu'un pirate est éliminé, EliteIntel annonce ``Kill confirmé`` ainsi que la prime gagnée. Les kills de cibles de mission sont annoncés comme ``Kill de mission confirmé`` avec le paiement associé.
4. **Suivi de progression** : Demandez ``Combien de kills restants ?`` à tout moment. EliteIntel retourne le total des kills restants et une ventilation par faction.


## Meilleures pratiques

_**Rachetez toutes les primes avant de prendre vos missions de massacre privées**_  un kill confirmé est un bon de prime contre la faction

- **Cumul de missions** : Accepter plusieurs missions contre la même faction augmente l'efficacité en crédits.
  Demandez à EliteIntel la pile la plus profonde à proximité (voir *Trouver des ensembles de massacre* plus bas).
- **Requêtes en langage naturel** : La parole naturelle est acceptée. ``Combien de pirates restants à tuer ?`` et ``Score de mission de pirates ?`` sont toutes deux valides. Précisez que la requête concerne les pirates pour éviter toute confusion avec d'autres missions actives.
- **Bascule de personnalité** : Changez de mode de personnalité selon vos besoins pour correspondre à votre style de jeu actuel.


---

# Trouver des ensembles de massacre

Un **ensemble de massacre**, c'est un fournisseur de missions et un terrain de chasse : des factions d'un système
proposent des missions de massacre de pirates contre une faction de pirates d'un système voisin, et ce système possède
un Site d'Extraction de Ressources (RES) où chasser. Plus il y a de factions qui proposent des missions dans le système
fournisseur, plus la pile est profonde : leurs missions tournent en parallèle, si bien que les mêmes kills paient
plusieurs fois.

## Comment EliteIntel apprend les ensembles

EliteIntel apprend les ensembles à partir de vos propres vols. Aucun site web ni compte n'est nécessaire.

- **Les missions que vous acceptez.** Quand vous acceptez une mission de massacre de pirates, EliteIntel enregistre le
  système fournisseur, la station, la faction et la cible.
- **Les sites de ressources que vous voyez.** À votre arrivée dans un système, le jeu liste ses sources de signal, Sites
  d'Extraction de Ressources compris. EliteIntel apprend aussi les sites de ressources que les jeux des autres
  commandants signalent à EDDN, le réseau de données public d'Elite Dangerous, et connaît donc des systèmes que vous
  n'avez jamais visités.

Un ensemble a besoin de ses deux moitiés. Une mission contre un système sans site de ressources connu est gardée, mais
elle n'est proposée qu'une fois un site de ressources aperçu là-bas.

**Commencez avec votre historique.** Vos anciens journaux en contiennent des années. Utilisez **Importer d'anciens
journaux** dans l'[onglet Commandant](UI-Commander-Tab.html), ou dites ``analyse les journaux pour les terrains de
chasse``.

## Demander un ensemble

| Dites | Ce qui se passe |
|---|---|
| ``trouve des missions de massacre de pirates dans un rayon de 50 années-lumière`` | Vega nomme le meilleur ensemble à portée : le système fournisseur et le nombre de factions qui y proposent des missions, la faction de pirates et le système cible, ses sites de ressources, et les stations où aller. Les piles les plus profondes d'abord, puis les plus proches. Sans rayon, elle cherche à 100 années-lumière. |
| ``navigue vers le fournisseur de missions pirates`` | Trace une route vers un fournisseur : d'abord celui des missions que vous avez déjà, puis celui du terrain de chasse où vous êtes, puis le meilleur ensemble à proximité. Un rappel nomme le système cible. |
| ``navigue vers la cible de mission pirate`` | Trace une route vers le terrain de chasse de vos missions. |
| ``trouve un terrain de chasse`` | Le Site d'Extraction de Ressources le plus proche qui vaut la peine pour les seules primes, sans mission. |
| ``oublie ce terrain de chasse`` | À dire dans un terrain de chasse qui ne vous plaît pas (trop facile, trop difficile, trop loin de l'étoile, la mauvaise puissance). Vega demande d'abord, puis ne le propose plus jamais, ni aucun ensemble qui vous y envoie. |

## Partager des ensembles avec d'autres commandants

Le partage est désactivé tant que vous ne l'activez pas, sur la page **Ensembles de massacre** de
l'[onglet Commandant](UI-Commander-Tab.html).

Il fonctionne dans les deux sens : **tant que vous partagez, vous recevez aussi les ensembles partagés par les autres
commandants.** Plus il y a de commandants qui partagent, plus il y a d'ensembles pour tout le monde.

**Ce qui est partagé :** pour chaque ensemble, les noms des systèmes stellaires, des factions et des stations, le
nombre de sites de ressources de chaque niveau dans la cible, et le jour où l'ensemble a été vu pour la dernière fois.
Chaque commandant de votre ordinateur compte, y compris ceux que vous avez importés.

**Ce qui n'est jamais partagé :** votre nom de commandant, vos missions, les récompenses, ni rien d'autre qui dise qui
vous êtes. Voir la [Politique de confidentialité](Privacy.html).

Ce que deviennent les ensembles :

1. Vos ensembles sont envoyés quand vous activez le partage, puis au plus une fois par jour, quand vous en avez appris
   de nouveaux.
2. Chaque nuit, les ensembles de tous les commandants sont vérifiés avec des données galactiques. Les systèmes doivent
   exister, la cible doit être à 20 années-lumière au plus du fournisseur, et la faction de pirates doit vraiment se
   trouver dans le système cible et être une Anarchie. Les ensembles qui échouent sont écartés. Les autres sont publiés,
   sans dire qui les a envoyés.
3. Une fois par jour, EliteIntel télécharge les ensembles publiés. Quand vous demandez un ensemble, ils sont proposés à
   côté des vôtres.

**Un ensemble partagé n'est proposé que si vos propres données montrent un site de ressources dans sa cible**, grâce à
vos visites ou à EDDN. C'est la seule chose que les données galactiques ne peuvent pas vérifier. Si un ensemble partagé
n'apparaît pas, allez dans son système cible ou attendez qu'EDDN le signale.

**Vega vous prévient quand un ensemble vient d'autres commandants.** Considérez-le comme une piste à vérifier, pas
comme quelque chose que vous avez parcouru vous-même. S'il ne vous convient pas, ``oublie ce terrain de chasse``
fonctionne comme pour vos propres ensembles.

**Vos propres données passent en premier.** Quand vous connaissez un ensemble vous-même, votre version est utilisée,
et à profondeur égale, les vôtres sont proposés avant ceux signalés par d'autres.

Si vous ne partagez pas et qu'EliteIntel ne connaît aucun ensemble à proximité, Vega vous signale quand d'autres
commandants en ont rapporté dans la zone.

Désactiver le partage retire vos ensembles des données partagées, et EliteIntel cesse d'utiliser ceux des autres.

----
Communauté 👉[**Matrix**](https://app.element.io/#/room/#krondor:matrix.org)👈
