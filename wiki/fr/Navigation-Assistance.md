# Assistance à la navigation

EliteIntel (EI) fournit une assistance à la navigation dans *Elite Dangerous*. Il prend en charge le calcul d'itinéraires galactiques pour les Porteurs de flotte, la navigation par coordonnées de surface sur les planètes et les lunes, ainsi que le suivi des échantillons d'exobiologie.

## Planification d'itinéraire pour Porteur de flotte

EliteIntel utilise [Spansh](https://spansh.co.uk) pour planifier les routes de saut des Porteurs de flotte. Pour calculer un itinéraire :

1. **Ouvrir la carte galactique** : N'importe quelle vue de la carte galactique convient. La carte spécifique au Porteur de flotte n'est pas nécessaire à cette étape.
2. **Copier le nom de la destination** : Sélectionnez le système stellaire cible. Cliquez sur le bouton de copie (le dernier bouton à droite dans l'interface de la carte galactique). Le nom du système est maintenant dans votre presse-papiers.
3. **Demander le calcul de l'itinéraire** : Dites « Calcule l'itinéraire du Porteur de flotte. » EliteIntel lit le nom du système depuis le presse-papiers, vérifie la position actuelle du porteur et interroge Spansh pour générer un itinéraire. L'itinéraire est sauvegardé dans la session en cours.

**Remarque** : La base de données de Spansh peut ne pas inclure tous les systèmes. Si le système actuel du porteur est inconnu de Spansh, EliteIntel sélectionne l'étoile connue la plus proche dans la portée de saut comme point de départ. Si le système de destination n'est pas dans Spansh, une alternative à proximité peut être nécessaire.

### Exécution de l'itinéraire

Une fois l'itinéraire calculé :

1. **Ouvrir la carte galactique du Porteur de flotte** : Accédez à la carte depuis le menu de gestion du porteur et cliquez sur le champ de texte en haut.
2. **Récupérer la prochaine destination** : Dites « Entrer la prochaine destination du Porteur de flotte. » EliteIntel colle le nom du prochain système dans le champ.
3. **Planifier le saut** : Confirmez le saut en jeu. Répétez pour chaque étape de l'itinéraire.

## Navigation par coordonnées de surface

EliteIntel fournit une navigation guidée par la voix vers des coordonnées spécifiques sur une planète ou une lune. Aucun marqueur de navigation n'est nécessaire.

1. **Démarrer la navigation** : Dites « Naviguer vers la latitude 41.4325 longitude -75.2309 » (remplacez par vos coordonnées cibles). EliteIntel vous guide de l'orbite jusqu'à la surface.
2. **Approche orbitale** : Maintenez une vitesse modérée en supercruise. Il y a un léger délai de synthèse vocale, évitez donc une vitesse excessive pour pouvoir suivre les instructions avec précision. Trop lent, et le vaisseau peut sortir prématurément du supercruise. La navigation du côté sombre d'une planète nécessite un vol aux instruments.
3. **Phase de glissade** : À environ 400 km de la surface, EliteIntel fournit des mises à jour sur l'angle de glissade telles que « Angle de glissade prononcé, -40 degrés. » La décision de glisser ou de se repositionner pour un meilleur angle appartient au pilote. EliteIntel ne fournit que des indications.
4. **Navigation en surface** : Après l'atterrissage, EliteIntel vous guide à moins de 1 000 mètres de la cible et vous invite à trouver un endroit pour vous poser. À partir de ce point, EliteIntel continue de vous diriger dans le SRV ou à pied jusqu'à ce que vous soyez à moins de 50 mètres de la cible.
5. **Annuler la navigation** : Dites « Annuler la navigation » à tout moment pour arrêter. Les formulations naturelles sont acceptées.

## Notes d'utilisation

- **Langage naturel** : La syntaxe de commande formelle n'est pas requise. La parole naturelle est acceptée.
- **Navigation côté sombre** : La navigation vers des coordonnées sur le côté sombre d'une planète nécessite un vol aux instruments.
- **Limitations de Spansh** : Si Spansh ne reconnaît pas votre système actuel, essayez une étoile adjacente pour initier l'itinéraire.

----
Communauté 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
