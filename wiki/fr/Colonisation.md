# Colonisation

EliteIntel transforme un chantier de colonisation en liste de courses en temps réel. Il suit ce dont la
construction a encore besoin, trouve où l'acheter et trace la route jusque-là. Fini les allers-retours entre
les sites de marché et les tableurs.

Il ne décide **pas** quoi construire ni où. Ça, c'est votre affaire, architecte. Il vous aide à rassembler la
montagne de marchandises dont la construction a besoin.

[[youtube:PnIlVZRdhKE]]

## Commencer le suivi d'une construction

**Posez-vous sur le chantier.** C'est le déclencheur. Quand vous vous posez au dépôt, le jeu envoie le
manifeste complet du chantier et ce chantier devient celui qu'EliteIntel suit. Ouvrez l'écran de construction
et l'[overlay HUD](UI-HUD-Overlay) affiche la carte **CHANTIER** : l'avancement, ce qui reste, et quoi
charger au prochain voyage.

Aucun suivi de toute la galaxie ne se fait dans votre dos. L'application ne connaît l'état de la construction
qu'**à votre dernier atterrissage**. D'autres commandants peuvent livrer au même dépôt pendant votre absence,
donc la carte et Vega précisent l'âge des données dès qu'elles ont plus d'une heure.

## Trouver la prochaine marchandise

Demandez ``Trouve la marchandise pour le chantier`` (ou *trouve le fret pour le chantier*, *trouve le fret de
colonisation*, etc.).

1. **Votre porte-vaisseaux d'abord.** Si votre porte-vaisseaux (de flotte ou d'escadron) contient des
   marchandises dont la construction a besoin, Vega vous y envoie avant tout marché. Un porte-vaisseaux dans
   votre système actuel l'emporte toujours. S'il est plus loin, il doit valoir les sauts.
2. **Puis le marché le plus proche.** EliteIntel cherche des marchés à deux sauts de votre vaisseau (selon sa
   portée de saut), puis à quatre sauts si rien ne se présente. Les stations spatiales passent en premier, les
   installations planétaires ensuite. Certaines marchandises, comme le composite CMM, ne se vendent que dans
   les installations planétaires. Oui, il faudra vous poser.
3. **Le plus gros manque d'abord.** La recherche part de la marchandise qui vous manque le plus (acier, titane,
   aluminium, peu importe le plus gros trou) et préfère le marché qui remplit le plus votre soute avec d'autres
   marchandises dont la construction a besoin.

Une fois le marché trouvé, EliteIntel ouvre la carte de la galaxie sur l'étoile. Il ne **valide pas** la
route. C'est voulu : à vous de décider de la verrouiller, ou de passer à la carte du système pour trouver
l'installation exacte.

**Nouvelle installation ?** Les recherches Spansh ont besoin d'un point de départ. L'application cherche
d'abord dans sa base de données locale les stations où vous vous êtes amarré. S'il n'y en a aucune, elle
demande à Spansh la station la plus proche, ce qui nécessite vos coordonnées galactiques, et l'application ne
les apprend qu'après un saut FSD. Volez donc un peu avec l'application lancée et amarrez-vous à quelques
stations avant de commencer un projet de construction. Plus vous l'utilisez, plus elle en sait.

## Au marché

Quand vous vous amarrez, l'overlay réordonne la liste pour placer en tête les marchandises **que cette station
vend**.

- Une marchandise partiellement chargée s'affiche en vert avec le tonnage à bord (par exemple ``16 T +44``).
- Dès que vous en avez assez, elle disparaît de la liste et la marchandise suivante prend sa ligne.
- Quand vous avez acheté tout ce que cette station peut fournir, le nom de la station disparaît de la carte.
  Il reste des marchandises à acheter, mais pas ici. Redemandez ``Trouve la marchandise pour le chantier`` et
  Vega vous envoie au marché suivant.

Livrez, ou stockez sur votre porte-vaisseaux, puis recommencez jusqu'à la fin de la construction.

## Retour au chantier

Dites ``Ramène-moi au chantier``. Si la construction est dans un autre système, la route y est tracée. Si
vous êtes déjà dans son système, aucune route n'est nécessaire.

Demandez ``Comment avance la construction ?`` ou ``Combien devons-nous encore acheter ?`` pour un point
d'avancement à voix haute.

## Le cas du porte-vaisseaux

Frontier n'expose pas la cargaison des porte-vaisseaux aux outils tiers. La seule façon pour EliteIntel de
voir votre stock passe par le **marché de marchandises** du porte-vaisseaux. Pour le rendre visible :

1. **Fermez l'accès au porte-vaisseaux** pour que personne ne puisse s'amarrer et acheter votre stock de
   construction.
2. **Mettez les marchandises en vente** sur le marché du porte-vaisseaux.
3. **Ouvrez le marché du porte-vaisseaux** depuis le panneau. C'est à ce moment-là que l'application le lit.

Ensuite, EliteIntel tient le compte quand vous transférez de la cargaison entre le porte-vaisseaux et votre
vaisseau, et quand vous achetez ou vendez sur votre propre porte-vaisseaux. C'est un contournement, mais le
jeu ne permet guère mieux.

## Plusieurs chantiers

Le dernier chantier où vous vous êtes posé est le chantier actuel. EliteIntel suit une seule construction à
la fois sur l'overlay. Posez-vous sur un autre dépôt et c'est lui qui prend le relais. Reposez-vous sur le
premier et il redevient actuel.

Envie d'une pause dans la construction ? Dites ``Oublie le chantier``. Vega vous demande de confirmer, puis la
carte se tait. Rien n'est supprimé. Se poser sur le chantier le fait revenir.

## Ce que fait l'IA (et ce qu'elle ne fait pas)

L'IA transforme ce que vous dites en actions et décide de ce que Vega vous répond. Le suivi, la recherche et
le tracé de route sont du code ordinaire qui lit le journal du jeu. Elle ne joue pas la boucle de
colonisation à votre place, et elle ne voit rien que le jeu n'écrive pas. C'est toujours vous qui pilotez.
L'application vous épargne la navigation sur le web.

Voir [Toutes les commandes](AllCommands) pour la liste complète des phrases de colonisation.
