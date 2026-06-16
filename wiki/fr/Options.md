# Options d'interface et de configuration

### Onglet IA <img src="images/ai.png" class="inline" height="20" alt="AI">

Il s'agit de l'onglet principal par défaut.

|                                                  |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
|--------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ![tab-ai-buttons.png](images/tab-ai-buttons.png) | - **Démarrer / Arrêter les services** : Active ou désactive la pile IA.<br/>- **Veille/Réveil** : En mode Réveil, l'application écoute en permanence. En mode Veille, elle ignore les entrées sauf si le bouton PTT est pressé, le mot de contournement "Listen" est prononcé ou la commande "Wake up!" est émise.<br/>- **Overlay OBS** : Affiche une fenêtre de superposition noire avec les interactions Commandant / IA. À ajouter dans OBS en masquant l'arrière-plan noir.<br/>- **Périphériques audio** : Sélectionner le périphérique audio d'entrée/sortie. **Calibrer l'audio** : Lancer la calibration audio pour de meilleures performances. |
|                                                  |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |


---

### Onglet Joueur <img src="images/controller.png" class="inline" height="20" alt="Player">

![playertab](images/tab-player.png)

- **Nom du Commandant** : Utilisez ce champ pour remplacer votre nom en jeu pour la synthèse vocale.
- **Options du vaisseau** : Vous pouvez activer ou désactiver ces automatisations. Utile pour les commandants en situation de handicap.
- **Gestion de la flotte** : Assignez des voix, des personnalités et une cadence aux vaisseaux individuels. La personnalité ne fonctionne qu'avec les LLMs cloud. L'icône engrenage ouvre les propriétés du vaisseau, telles que le honk automatique et le profil de commerce.

![popup-ship-properties.png](images/popup-ship-properties.png)

- **Honk automatique à l'entrée du système** : Sélectionnez le groupe de feu et la gâchette. Si cette option est cochée, le vaisseau effectuera un scan de découverte à l'entrée. Si le HUD est en mode Combat, il basculera en mode Analyse, effectuera le scan, puis rebasculera.
- **Personnaliser votre profil de commerce** : Ces paramètres peuvent être définis depuis l'interface utilisateur, ou via des commandes vocales : "alter/change trade profile set [paramètre] to [valeur]"

---



### Onglet Actions <img src="images/keys-binding.png" class="inline" height="20" alt="Actions">

![tab-actions.png](images/tab-actions.png)

L'onglet **Actions / Liaisons** comporte trois sections : Liaisons, Commandes intégrées et Commandes personnalisées.

- **Liaisons** : répertoire où se trouve votre fichier de liaisons de jeu. Sans ce fichier, l'application ne peut pas contrôler le jeu.
- **Profil** : votre profil de liaisons actuel en jeu.
- **Fichier** : le fichier contenant les liaisons que vous utilisez actuellement.

Vous pouvez modifier vos liaisons depuis cet écran et les enregistrer comme nouveau profil.

__REMARQUE  Les HOTAS/CONTRÔLEURS sont affichés mais ne peuvent pas être configurés depuis cet écran. Liaisons clavier uniquement (susceptible d'évoluer).__


**Actions / Commandes intégrées**

![tab-action-build-in-commands.png](images/tab-action-build-in-commands.png)


Fournit une liste des commandes intégrées. Un double-clic sur l'une d'elles affiche une boîte de dialogue avec des informations sur la commande et permet de proposer une meilleure traduction pour la localisation.

**Commandes personnalisées**

![acttions cuystom commands](images/tab-actions-custom-commands.png)

Cet écran vous permet de définir une action personnalisée que l'application exécutera sur votre commande.

- Cliquez sur le bouton NOUVEAU pour ouvrir une fenêtre contextuelle dans laquelle vous pouvez définir votre action personnalisée.

![popup-custom-action.png](images/popup-custom-action.png)

- Saisissez le nom de l'action. REMARQUE : Le nom de l'action doit contenir des mots (tokens) séparés par des underscores _
- Fournissez un nom pour votre action personnalisée.
- Fournissez une description pour votre action personnalisée.
- Saisissez des mots d'entraînement, qui sont des tokens de signification. Le LLM tentera de faire correspondre la commande prononcée à l'action en utilisant la probabilité la plus élevée. Plus vos tokens sont susceptibles de correspondre à l'action, plus elle sera retournée.

Pour utiliser les actions personnalisées, parlez normalement. Vous n'avez pas besoin de mémoriser les mots exacts, mais vous devez transmettre un sens précis pour que le LLM associe votre commande à l'action avec la probabilité la plus élevée.

---


Pour tout problème, contactez-nous via Matrix. Les rapports de bugs et les pull requests sont les bienvenus.

Community 👉[**Matrix**](https://matrix.to/#/#krondor:matrix.org)👈
