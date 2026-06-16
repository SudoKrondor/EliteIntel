# Suivi des Missions de Massacre de Pirates

EliteIntel suit les missions de massacre de pirates, notamment les comptages de kills, les factions cibles et les calculs de récompenses. Il surveille les kills et les paiements tout au long de la session.

## Pour commencer

Pour accomplir efficacement les missions de massacre de pirates :

1. **Trouvez un Hotspot** : Utilisez [INARA](https://inara.cz) pour trouver un système avec un Site d'Extraction de Ressources Dangereux (Haz RES) où plusieurs factions proposent des missions contre la même faction de pirates. Acceptez plusieurs missions contre la même faction. EliteIntel suit la faction cible, le nombre de kills et les détails des paiements.
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

- **Cumul de missions** : Accepter plusieurs missions contre la même faction augmente l'efficacité en crédits. Utilisez INARA pour identifier les systèmes avec plusieurs fournisseurs de missions éligibles.
- **Requêtes en langage naturel** : La parole naturelle est acceptée. ``Combien de pirates restants à tuer ?`` et ``Score de mission de pirates ?`` sont toutes deux valides. Précisez que la requête concerne les pirates pour éviter toute confusion avec d'autres missions actives.
- **Bascule de personnalité** : Changez de mode de personnalité selon vos besoins pour correspondre à votre style de jeu actuel.


---

# Trouver des missions de massacre de pirates
## Fonctionnalité expérimentale

Pour le moment, la recherche manuelle dans INARA pour le cumul de missions de pirates reste la méthode la plus efficace pour la chasse aux primes. Cependant, l'application dispose d'une fonctionnalité expérimentale basée sur les données d'IN**T**RA (et non d'IN**A**RA). C'est une méthode moins efficace mais plus immersive pour jouer à la chasse aux primes, bien qu'elle dépende de la disponibilité des données dans IN**T**RA et que leur site soit opérationnel.

Pour utiliser cette fonctionnalité, montez à bord de votre vaisseau de chasseur de primes et demandez ``Trouve-nous des terrains de chasse (dans un rayon de X années-lumière)``  le vaisseau tentera d'établir une connexion avec IN**T**RA et récupérera les paires système cible / système fournisseur de missions. Le succès ou l'échec dépend du retour des données par l'API IN**T**RA et du nombre d'autres chasseurs de primes utilisant EDMC avec le plugin IN**T**RA.

Si et quand ces données vous sont retournées, l'ordinateur de bord vous indiquera qu'il a trouvé X terrains de chasse. Demandez ``trace une route vers un terrain de chasse pour reconnaissance``. Elite Intel tracera la route vers le terrain de chasse le plus proche de la liste. Vous devez vous y rendre et confirmer la présence du site de ressources. S'il y en a un et que le taux d'apparition vous convient, confirmez ou rejetez ce système comme terrain de chasse potentiel par commande vocale.

Elite Intel peut le confirmer automatiquement comme terrain de chasse s'il détecte les sites RES dans le journal à l'entrée ou lors du scan de la balise de navigation. Il arrive que cela ne se produise pas car il n'y a aucun enregistrement de sites RES enregistré par le jeu dans le journal. Une confirmation manuelle est alors requise.

Une fois satisfait du terrain de chasse, demandez ``navigue vers le système fournisseur de missions de massacre de pirates`` ou quelque chose d'approchant. L'application tracera une route vers le système fournisseur de missions. Volez jusqu'à là-bas, posez-vous dans les ports et prenez des missions contre les pirates pour la même faction et le même lieu que le système de terrain de chasse. Lorsque vous prenez votre première mission de massacre, cette paire de systèmes sera confirmée comme terrain de chasse / fournisseur de missions. L'application vous indiquera s'il y a d'autres systèmes qu'elle connaît ayant des missions contre la même faction dans le système cible  si, bien sûr, IN**T**RA dispose de ces données.

Une fois vos missions cumulées, demandez ``trace une route vers la mission active`` et faites feu sur les pirates comme d'habitude. Lorsque vous aurez terminé les missions, la même demande ``trace une route vers la mission active`` vous mènera là où se trouve l'objectif, cette fois le port où vous avez pris la mission.

----
Communauté 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
