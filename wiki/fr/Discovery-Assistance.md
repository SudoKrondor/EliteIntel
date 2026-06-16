# Assistance à la découverte

Ce guide décrit les fonctionnalités de découverte et d'exploration dans EliteIntel. 
Les sections ci-dessous couvrent la détection des systèmes non découverts, l'exploration de surface, la navigation en exobiologie,
le guidage par coordonnées, et le suivi à l'échelle du système.

## Explorer l'inconnu

Demandez à l'application d'``activer les annonces de découverte``. Comme toujours, parlez naturellement, mais soyez précis sur ce que 
vous voulez dire. 

En entrant dans un système non découvert :

- **Systèmes non découverts** : En sortant de l'hyperespace, EliteIntel vérifie si le système a déjà été
  découvert. S'il est inexploré et que les annonces de découverte sont activées, EliteIntel annonce la nouvelle découverte.
- **Honk** : Si l'auto-honk est activé, votre vaisseau effectuera le balayage de fréquence préliminaire à la sortie de l'hyperespace. 
- **Scan FSS** : Volez à une distance sûre de l'étoile parente. Dites ``Open FSS`` ou ``Scan the system`` ou quelque chose dans ce sens, et l'écran FSS s'ouvrira pour vous. Effectuez vos scans habituels. L'application annoncera si vous trouvez quelque chose digne d'intérêt, comme des mondes de grande valeur ou des signaux bio/géo. 
- **Analyse post-scan** : Après avoir identifié tous les corps du système, EliteIntel peut analyser les données sur votre demande. 
  - Demandez ``Quelles planètes ont des signaux bio ?`` ou ``Des sites géologiques ?`` ou ``La planète X est-elle atterrissable ?``. Pour les noms de planètes, vous pouvez omettre le préfixe du nom d'étoile. Par exemple, `NomEtoile-23` a une planète `NomEtoile-23-2b`, vous pouvez simplement demander ``bravo deux est-elle atterrissable ?``. 
  - Si des signaux bio sont présents, vous pouvez demander à l'application d'effectuer une analyse préliminaire du biome : ``analyser le biome pour ce système stellaire``. L'application tentera de deviner quels exo-organismes pourraient être présents en se basant sur ce que nous savons des organismes et de leurs habitats préférés. C'est une estimation éclairée, pas une certitude, donc effectuez un scan détaillé de la planète et réalisez des scans de surface des colonies d'exo-organismes si vous le souhaitez.


## Exploration de surface

[[youtube:C9IcRAqY6ww]]

## Navigation en exobiologie

Lors de la collecte d'échantillons biologiques en terrain difficile, EliteIntel suit les échantillons balisés et vous guide entre eux.

1. **Baliser les échantillons** : Volez bas et lentement au-dessus du terrain pour localiser les colonies biologiques. Approchez chaque colonie et scannez-la avec
   l'Analyseur de composition pour créer une Entrée du codex. Restez proche de l'échantillon lors du scan. Cela enregistre les coordonnées du vaisseau dans EliteIntel et peut rapporter une récompense en crédits. Balisez toutes les espèces visibles avant d'atterrir.
2. **Atterrir** : Trouvez un endroit d'atterrissage dans un rayon de 2 km des échantillons si possible. Si aucun endroit approprié n'est disponible dans ce rayon, EliteIntel guidera votre SRV via GPS.
3. **Naviguer vers les échantillons** : Dites ``Navigate to nearest codex entry`` ou ``Navigate to next bio sample``. EliteIntel
   vous guide vers la localisation balisée la plus proche. Après avoir scanné une espèce (par exemple, Frutexa), EliteIntel enregistre
   le type. Les demandes de navigation suivantes priorisent cette espèce si d'autres balises du même type existent.
4. **Continuer** : Répétez jusqu'à ce que tous les scans soient terminés. EliteIntel fournit un guidage vocal continu lors des longues traversées en SRV.

**Note** : EliteIntel utilise les coordonnées des balises du codex. Rester proche de l'échantillon lors du scan améliore la précision des coordonnées. Plus de balises offrent plus d'options de navigation.

Lorsqu'une planète avec des signaux bio est identifiée, EliteIntel fournit un support d'exploration de surface :

- **Scan de surface détaillé** : Après un scan de surface détaillé, EliteIntel recueille des données incluant la gravité, la température et les genres disponibles.
- **Repérage et scan** : Volez bas au-dessus de la surface. Localisez des spécimens biologiques tels que Fungoida ou Frutexa,
  qui peuvent se trouver dans des terrains comme des chaînes de montagnes. Scannez un spécimen depuis le vaisseau pour créer une entrée du codex et
  potentiellement obtenir une récompense en crédits. EliteIntel enregistre les coordonnées de ce genre et de cette variante.
- **Navigation en SRV** : Trouvez un endroit d'atterrissage proche des points de scan. Entrez dans le SRV et dites ``Navigate to the nearest bio
sample``. EliteIntel sélectionne l'échantillon balisé le plus proche et fournit le cap et la distance. Il fournit des
  indications vocales jusqu'à une courte distance de l'échantillon.
- **Gestion intelligente du codex** : Scannez un échantillon avec le scanner bio. EliteIntel supprime l'entrée du codex correspondante
  pour ce genre dans la zone de la colonie. Balisez les colonies éloignées les unes des autres pour éviter d'effacer plusieurs entrées avec un seul
  scan. EliteIntel indique la distance minimale jusqu'au prochain échantillon et estime le paiement Vista Genomics pour
  un ensemble complet de trois.
- **Persistance entre les sessions** : La progression est préservée entre les sessions. EliteIntel conserve les échantillons collectés,
  les cibles restantes et les coordonnées même après avoir quitté le système. La progression de l'exploration est sauvegardée jusqu'à une
  réinitialisation manuelle ou l'achèvement de la collecte de données du système.

## Revenir pour terminer le travail

Si vous avez quitté une planète ou le système stellaire, la progression est conservée et peut être reprise à tout moment.

- **Navigation orbitale** : En vous approchant de la planète, demandez à EliteIntel de vous guider. Il vous guide depuis l'orbite
  vers la Zone de Glissade, à environ 300 à 400 km des coordonnées cibles. EliteIntel fournit le cap et la
  distance jusqu'au seuil de la Zone de Glissade, pas jusqu'à la cible de surface. Le vol aux instruments est requis pour les approches sur
  le côté sombre d'une planète.
- **Assistance à la glissade** : En atteignant la Zone de Glissade, EliteIntel vous invite à initier la glissade et suggère un
  angle de descente sûr. L'angle maximum sûr est de -35 degrés. Si un repositionnement est nécessaire pour un meilleur angle de glissade,
  EliteIntel vous conseille en conséquence. Après la glissade, EliteIntel vous guide jusqu'à 1 km de la cible pour l'atterrissage.
- **Navigation en surface** : Dans le SRV, EliteIntel vous guide jusqu'à 25 mètres des coordonnées cibles.

## Naviguer par coordonnées

Pour naviguer vers des coordonnées spécifiques, dites ``Navigate to 23.456 latitude and -43.87 longitude``. EliteIntel active
la navigation depuis le survol orbital ou depuis la surface.

## Informations à l'échelle du système

EliteIntel fournit un suivi et une analyse à l'échelle du système :

- **Suivi des échantillons** : Demandez ``What bio samples have we collected?`` EliteIntel signale les échantillons collectés, les
  cibles restantes et les emplacements par planète. Ces données persistent entre les retours dans le système.
- **Estimations de profit** : Demandez ``What is the potential profit from this system?`` EliteIntel estime les gains basés
  sur les données de scan disponibles.
- **Intégration EDSM** : Pour les systèmes découverts, EliteIntel interroge EDSM pour les données disponibles. Si des données sont trouvées, vous
  pouvez interroger EliteIntel sur les détails du système.
- **Analyse de route** : EliteIntel peut analyser une route planifiée, en indiquant le nombre de sauts restants, les
  étoiles scoopables et la distance de trajet réelle par rapport à la distance en ligne droite.




----
Communauté 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
