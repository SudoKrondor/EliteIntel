# Installation

Elite Intel **V1.2** est la version actuelle.

---

## <img src="images/linux.png" class="inline" height="20" alt="Linux"> Linux <img src="images/windows.png" class="inline" height="20" alt="Windows"> Windows

1. Téléchargez le [👉**programme d'installation**👈](https://github.com/stone-alex/EliteIntel/releases).
2. Exécutez le programme d'installation et suivez les instructions à l'écran.
3. Configurer un modèle de langage. **Elite Intel ne peut pas fonctionner sans lui.** Deux options :
   - **LLM local** (gratuit, hors ligne) : voir le [**guide LLM local**](installing-local-llms).
     Nécessite un GPU performant.
   - **LLM cloud** (payant, et se configure plus facilement) : voir
     [**Options de LLM cloud**](cloud-llm-options) pour obtenir une clé API, puis saisissez-la dans
     [**Services → Services IA**](UI-Settings-Tab).

Configuration terminée. Ensuite : [**l'interface, onglet par onglet**](UI).

### Liste de vérification au premier lancement

Elite Intel énonce ces avertissements à voix haute au démarrage des services : vous serez donc
informé de tout ce qui manque — mais autant le faire d'emblée :

| Étape | Où |
|------|-------|
| Le pointer vers un modèle de langage | [Services → Services IA](UI-Settings-Tab) |
| Vérifier le dossier du journal | [Onglet Commandant](UI-Commander-Tab) |
| Laisser BindForge se configurer et donner à Vega les touches nécessaires | [Onglet BindForge](UI-Bindings-Tab) |
| Calibrer l'audio | [Onglet Vega](UI-Vega-Tab) → **CALIBRER L'AUDIO** |

---

## Ce qu'Elite Intel configure pour vous

Trois choses à connaître avant de commencer sur une nouvelle installation.

### LM Studio, installé pour vous

Si LM Studio ne sert qu'à Elite Intel, **laissez Elite Intel l'installer. C'est la méthode
recommandée.** Appuyez sur **Installer LM Studio** dans [Services → Services IA](UI-Settings-Tab) →
**LMStudio**. Elite Intel installe LM Studio, télécharge et charge le modèle, le teste, puis remplit
**Adresse** et **Modèle** pour vous. Ensuite, il démarre LM Studio avec ses services et l'arrête quand
vous les arrêtez ou fermez l'application : le modèle n'occupe la mémoire de votre carte graphique que
pendant qu'Elite Intel tourne. **Désinstaller LM Studio** le retire.

Le bouton n'apparaît que si vos cartes graphiques peuvent contenir le jeu et le modèle ensemble : une
NVIDIA RTX 3090 de 24 Go ou mieux, deux cartes NVIDIA récentes totalisant environ 20 Go, ou des cartes
AMD avec la même mémoire, en bêta. Le [guide LLM local](installing-local-llms) donne les détails. Si
vous utilisez LM Studio pour autre chose, installez-le vous-même : Elite Intel se contente alors
d'ajouter le modèle et de remplir les champs, et ne démarre, n'arrête ni ne supprime jamais votre LM
Studio.

### Votre HOTAS, vos joysticks et vos palonniers

La première fois que vous ouvrez l'[onglet BindForge](UI-Bindings-Tab), il sauvegarde vos fichiers de
commandes, trouve chaque copie du jeu sur ce PC et regarde ce qui est branché. D'origine, le jeu ne
connaît beaucoup de contrôleurs que par un identifiant matériel. BindForge propose un vrai nom, comme
*Throttle* ou *Pedals*, à chaque contrôleur qui n'en a pas, et vous pouvez **Passer**.

![Contrôleurs nommés dans le Concepteur d'alias](images/ui-bindings-alias-designer.png)

Ensuite, dans le [Concepteur d'alias](UI-Bindings-Tab#alias-designer), cliquez sur l'**engrenage**
d'un contrôleur pour libeller ses boutons, axes et chapeaux. Appuyez sur un bouton et sa case
s'allume : vous savez lequel est lequel. Les libellés apparaissent dans l'écran Commandes du jeu et
dans l'Éditeur de raccourcis.

![Libellés des boutons dans l'Éditeur de périphérique](images/ui-bindings-device-editor.png)

### Le jeu acheté sur plusieurs boutiques

Vous avez Elite Dangerous sur Steam et sur Epic ? Toutes les copies partagent un seul jeu de
commandes, mais chacune garde sa propre liste de vos contrôleurs et des noms de boutons. Quand ces
listes divergent, vos commandes peuvent marcher dans une copie et retomber sur clavier et souris dans
l'autre. BindForge le repère à la première ouverture et propose **Accorder tes copies du jeu** :

![Accorder tes copies du jeu](images/install-reconcile-welcome.png)

1. **Brancher** — branchez les contrôleurs avec lesquels vous volez. Appuyez sur un bouton et sa
   ligne s'allume.
2. **Choisir** — répondez à quelques questions courtes, par exemple quel nom garder pour un contrôleur
   que chaque copie appelle différemment. Vos commandes suivent le nom choisi. Trop de questions ?
   **Tout faire comme dans une de mes copies...** règle tout comme dans la copie où vous jouez le plus.
3. **Vérifier** — voyez exactement ce que chaque copie aura, et pourquoi, avant tout changement.

![Vérifier le résultat](images/install-reconcile-check.png)

**Rien ne change avant la dernière étape, et tout est sauvegardé d'abord.** **Pas maintenant** laisse
vos fichiers tels quels. Si les fichiers de commandes d'une copie changent plus tard, par exemple après
une mise à jour du jeu, BindForge demande seulement **Quelle configuration est actuelle ?** et aligne
les autres.

---

### Nouveau dans Elite Dangerous ?

Le jeu ne crée ses fichiers de commandes qu'une fois que vous avez modifié quelque chose, et sans eux Elite Intel ne peut
pas piloter votre vaisseau. Terminez votre entraînement de commandant, puis dans *Options → Commandes* du jeu, réglez le
déplacement de l'interface de la carte de la galaxie sur les touches fléchées et appliquez.

### Passer à un autre ordinateur

[**Sauvegarder paramètres et données**](UI-Settings-Tab), dans l'onglet Services, regroupe vos réglages, vos données et vos
commandes personnalisées dans un seul fichier que vous pouvez restaurer sur le nouvel ordinateur.

---

### Désinstallation (Linux)

```shell
~/.var/app/elite.intel.app/uninstall
```

----
Pour tout problème, utilisez **Signaler un bug** dans l'onglet Vega : votre description part directement à l'équipe, sans compte. Pour discuter avec nous, rejoignez la communauté sur Matrix avec un compte gratuit. L'application Element est le meilleur choix, mais n'importe quel navigateur récent suffit pour une discussion rapide. Les rapports de bugs et les pull requests sont les bienvenus.

Communauté 👉[**Matrix**](https://app.element.io/#/room/#krondor:matrix.org)👈
